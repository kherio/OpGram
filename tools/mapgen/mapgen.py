#!/usr/bin/env python3
"""
OpGram mapping generator.

Locates the obfuscated names that Telegram's R8 build gives to the classes,
methods and fields OpGram hooks, and writes them to
  app/src/main/assets/clients/TelegramWeb-<versionCode>.json
plus its alias file, so OpGram works on a Telegram build without hand-mapping.

It works by:
  1. Indexing every class/method/field of the APK's dex files (androguard).
  2. Matching each wanted class by the string constants it references
     (robust to renaming), with superclass / member-count tie-breakers.
  3. Matching each wanted member by descriptor within the resolved class.

Symbols it cannot resolve are listed so you can adjust fingerprints.json.
Members in the org.telegram.messenger / tgnet / SQLite packages are NOT
obfuscated, so they are emitted unchanged.

Usage:
  python3 tools/mapgen/mapgen.py path/to/Telegram.apk
Requires: pip install androguard
"""
import json, os, sys, collections

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
FP = json.load(open(os.path.join(HERE, "fingerprints.json")))


def load_apk(path):
    from androguard.core.apk import APK
    from androguard.core.dex import DEX
    try:
        from loguru import logger; logger.remove()
    except Exception:
        pass
    apk = APK(path)
    version = int(apk.get_androidversion_code())
    pkg = apk.get_package()
    classes = {}           # name -> {super, interfaces, methods:[(n,desc)], fields:[(n,desc)]}
    strings_by_class = collections.defaultdict(set)
    import zipfile
    z = zipfile.ZipFile(path)
    for n in z.namelist():
        if not (n.startswith("classes") and n.endswith(".dex")):
            continue
        d = DEX(z.read(n))
        for c in d.get_classes():
            name = c.get_name()[1:-1].replace("/", ".")
            classes[name] = {
                "super": c.get_superclassname()[1:-1].replace("/", "."),
                "interfaces": [i[1:-1].replace("/", ".") for i in c.get_interfaces()],
                "methods": [(m.get_name(), m.get_descriptor().replace(" ", "")) for m in c.get_methods()],
                "fields": [(f.get_name(), f.get_descriptor()) for f in c.get_fields()],
            }
        # per-class string refs (const-string operands survive obfuscation)
        for c in d.get_classes():
            owner = c.get_name()[1:-1].replace("/", ".")
            for m in c.get_methods():
                code = m.get_code()
                if not code:
                    continue
                try:
                    instructions = code.get_bc().get_instructions()
                except Exception:
                    continue
                for ins in instructions:
                    if ins.get_name().startswith("const-string"):
                        out = ins.get_output()
                        if '"' in out:
                            strings_by_class[owner].add(out.split('"', 1)[1].rsplit('"', 1)[0])
        del d
    return version, pkg, classes, strings_by_class


def resolve_class(fp, classes, strings_by_class, resolved=None):
    """Resolve one class. `resolved` maps already-known original->obfuscated names,
    used to match by the *obfuscated* superclass and to break string ties."""
    resolved = resolved or {}
    want = set(fp.get("strings", []))
    # obfuscated name expected for the declared super, if we already resolved it
    want_super_ob = resolved.get(fp.get("super")) if fp.get("super") else None
    want_descs = set(fp.get("methodDescriptors", []))
    cands = []
    for name, info in classes.items():
        score = 0
        if want:
            hit = len(want & strings_by_class.get(name, set()))
            if hit:
                score += hit * 10
        if want_super_ob and info["super"] == want_super_ob:
            score += 6
        elif fp.get("super") and info["super"] == fp["super"]:
            score += 3
        if want_descs:
            have = {m[1] for m in info["methods"]}
            matched = len(want_descs & have)
            if matched:
                score += matched * 4
        if fp.get("hasMethod"):
            mn, _ = _split_sig(fp["hasMethod"])
            if any(m[0] == mn for m in info["methods"]):
                score += 2
        if score > 0:
            cands.append((score, name))
    if not cands:
        return None
    cands.sort(reverse=True)
    return cands[0][1] if (len(cands) == 1 or cands[0][0] > cands[1][0]) else None


def _split_sig(sig):
    # "setChecked(boolean)" -> ("setChecked", None); descriptor matching is done separately
    return sig.split("(", 1)[0], None


def main():
    if len(sys.argv) < 2:
        print(__doc__); sys.exit(1)
    version, pkg, classes, sbc = load_apk(sys.argv[1])
    print("Telegram package %s, versionCode %d, %d classes" % (pkg, version, len(classes)))

    resolved = {}
    # Pass 1: classes anchored by strings (unambiguous first).
    pending = list(FP["classes"])
    for _ in range(3):  # a few passes let newly-resolved supers help the rest
        still = []
        for fp in pending:
            r = resolve_class(fp, classes, sbc, resolved)
            if r and fp["orig"] not in resolved:
                resolved[fp["orig"]] = r
            elif fp["orig"] not in resolved:
                still.append(fp)
        if len(still) == len(pending):
            break
        pending = still
    missing = [fp["orig"] for fp in pending if fp["orig"] not in resolved]

    out_classes = [{"o": o, "r": r} for o, r in resolved.items()]
    out_methods, out_fields = [], []
    for m in FP.get("members", []):
        cls = m["class"]
        if not cls.startswith(("org.telegram.messenger", "org.telegram.tgnet", "org.telegram.SQLite")):
            cls_ob = resolved.get(cls)
            if not cls_ob:
                missing.append(cls + "#" + m["orig"]); continue
        else:
            cls_ob = cls  # not obfuscated
        info = classes.get(cls_ob)
        found = None
        if info:
            for (n, desc) in info["methods" if m["kind"] == "method" else "fields"]:
                if desc == m["descriptor"]:
                    found = n; break
        if found:
            simple = cls.split(".")[-1]
            (out_methods if m["kind"] == "method" else out_fields).append(
                {"c": simple, "o": m["orig"], "r": found})
        else:
            missing.append(cls + "#" + m["orig"])

    out = {"classes": out_classes, "methods": out_methods, "fields": out_fields}
    dest = os.path.join(ROOT, "app/src/main/assets/clients/TelegramWeb-%d.json" % version)
    if os.path.exists(dest) and "--force" not in sys.argv:
        dest = dest[:-5] + ".generated.json"
        print("Reference mapping exists; writing to %s (use --force to overwrite)." % os.path.basename(dest))
    json.dump(out, open(dest, "w"), indent=2)
    alias = os.path.join(ROOT, "app/src/main/assets/clients/Alias/TelegramWeb-%d.json" % version)
    json.dump({"methodAlias": {}}, open(alias, "w"), indent=2)
    print("Wrote %s (%d classes, %d methods, %d fields)" % (dest, len(out_classes), len(out_methods), len(out_fields)))
    if missing:
        print("\nCould NOT resolve %d symbols — refine tools/mapgen/fingerprints.json:" % len(missing))
        for x in missing:
            print("  -", x)
    print("\nThis generator is a scaffold: it covers the symbols with fingerprints defined. "
          "Extend fingerprints.json with the remaining hooked members for full coverage.")


if __name__ == "__main__":
    main()
