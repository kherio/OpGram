#!/usr/bin/env python3
"""
OpGram mapping porter.

Ports a known obfuscation mapping (e.g. Telegram Web 12.10.4) to a NEW Telegram
Web build, automatically. Telegram ships R8-obfuscated: between builds most
*class* names change, while method/field names inside a class mostly stay.

Two sub-commands:

  fingerprint   Build a compact, compressed "reference fingerprint" from a
                known-good APK + its mapping (needed once per reference version):
      python3 portmap.py fingerprint --apk Telegram.apk \
          --map ../../app/src/main/assets/clients/TelegramWeb-70999.json \
          --out refs/TelegramWeb-70999.fp.json.gz

  port          Port a reference fingerprint to a new APK and write
                TelegramWeb-<versionCode>.json (+ alias) plus a review report.
                It also writes the fingerprint of the NEW version, so the next
                port can chain from the closest version:
      python3 portmap.py port --ref refs/TelegramWeb-70999.fp.json.gz \
          --apk Telegram-new.apk

Anything it is not confident about is listed under REVIEW instead of guessed.
Requires: pip install androguard
"""
import argparse, collections, gzip, json, os, re, sys, zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
CLIENTS = os.path.join(ROOT, "app/src/main/assets/clients")

# Owners that are NOT obfuscated by R8 (their name is the same in every build).
UNOBF_OWNERS = {
    "PhotoViewer": "org.telegram.ui.PhotoViewer",
    "SecretMediaViewer": "org.telegram.ui.SecretMediaViewer",
    "ProfileActivity": "org.telegram.ui.ProfileActivity",
    "LaunchActivity": "org.telegram.ui.LaunchActivity",
    "Switch": "org.telegram.ui.Components.Switch",
    "AlertDialog$Builder": "org.telegram.ui.ActionBar.AlertDialog$Builder",
}
# Inner/anonymous classes found through their constructor's reference to the outer class.
INNER_OF = {
    "ChatActivity$MenuItemClick": "ChatActivity",
    "ProfileActivity$MenuItemClick": "ProfileActivity",
    "ChatActivity$ChatMessageCellDelegate": "ChatActivity",
}
# Classes (typically tiny interfaces/lambdas whose shape is too generic to fingerprint) that
# are located through the parameter type of a method of an already-resolved class:
# key -> (owner key, method name in the reference, parameter index)
DERIVED_FROM_PARAM = {
    "AlertDialog$OnButtonClickListener": ("AlertDialog$Builder", "setPositiveButton", 1),
}
# Members whose real owner is a different class than their key suggests.
MEMBER_OWNER_OVERRIDE = {
    "ChatActivity#this$0": "ChatActivity$MenuItemClick",
    "ProfileActivity#this$0": "ProfileActivity$MenuItemClick",
}

TYPE = re.compile(r"L([\w/$]+);")


# ----------------------------------------------------------------- indexing
def index_apk(path):
    from androguard.core.dex import DEX
    try:
        from loguru import logger
        logger.remove()
    except Exception:
        pass
    out = {}
    z = zipfile.ZipFile(path)
    for n in z.namelist():
        if not (n.startswith("classes") and n.endswith(".dex")):
            continue
        d = DEX(z.read(n))
        for c in d.get_classes():
            name = c.get_name()[1:-1].replace("/", ".")
            out[name] = {
                "s": c.get_superclassname(),
                "i": c.get_interfaces(),
                "af": c.get_access_flags(),
                "f": [(f.get_name(), f.get_descriptor(), f.get_access_flags()) for f in c.get_fields()],
                "m": [(m.get_name(), m.get_descriptor().replace(" ", ""), m.get_access_flags()) for m in c.get_methods()],
            }
        del d
    return out


def apk_version(path):
    from androguard.core.apk import APK
    a = APK(path)
    return int(a.get_androidversion_code()), a.get_androidversion_name(), a.get_package()


def load_index(apk=None, index=None):
    if index:
        ix = json.load(open(index))
    else:
        ix = index_apk(apk)
    for v in ix.values():
        v["m"] = [(a, b.replace(" ", ""), c) for a, b, c in v["m"]]
    return ix


def dot(desc):  # 'Lfoo/Bar;' -> 'foo.Bar'
    return desc[1:-1].replace("/", ".")


def is_obf(type_name):
    simple = type_name.split("/")[-1]
    return len(simple) <= 3 and simple.isalnum() and not simple[0].isupper()


def norm(desc, keep=frozenset(), translate=None):
    """Normalise a descriptor: translate known classes, wildcard other obfuscated types."""
    def sub(m):
        t = m.group(1)
        dotted = t.replace("/", ".")
        if translate and dotted in translate:
            return "L" + translate[dotted].replace(".", "/") + ";"
        if dotted in keep and not translate:   # reference side: its own names are not "kept" names
            return m.group(0)
        return "?" if is_obf(t) else m.group(0)
    return TYPE.sub(sub, desc)


def jac(a, b):
    i = sum((a & b).values())
    u = sum((a | b).values()) or 1
    return i / u


# --------------------------------------------------------------- fingerprint
def owner_table(mapping):
    """simple name -> reference (obfuscated) class, for every owner we know."""
    t = {}
    for c in mapping["classes"]:
        t[c["o"].split(".")[-1]] = c["r"]
        t[c["o"]] = c["r"]
    t["AlertDialog$Builder"] = UNOBF_OWNERS["AlertDialog$Builder"]
    for k, v in UNOBF_OWNERS.items():
        t.setdefault(k, v)
    return t


def cmd_fingerprint(args):
    ix = load_index(args.apk, args.index)
    mapping = json.load(open(args.map))
    version = apk_version(args.apk)[0] if args.apk else args.version
    owners = owner_table(mapping)
    classes = {}
    wanted = {c["r"] for c in mapping["classes"]} | set(UNOBF_OWNERS.values())
    # also keep the listener owners
    for orig, ref in owners.items():
        wanted.add(ref)
    for r in wanted:
        if r in ix:
            v = ix[r]
            classes[r] = {"s": v["s"], "i": v["i"], "af": v["af"], "m": v["m"], "f": v["f"]}
    fp = {
        "version": version,
        "mapping": mapping,
        "classes": classes,
        "alias": _read_alias(version),
    }
    os.makedirs(os.path.dirname(os.path.abspath(args.out)), exist_ok=True)
    with gzip.open(args.out, "wt", encoding="utf-8") as f:
        json.dump(fp, f)
    print("fingerprint written: %s (%d classes, %d bytes)" % (args.out, len(classes), os.path.getsize(args.out)))


def _read_alias(version):
    for name in ("TelegramWeb-%s.json" % version, "TelegramWeb.json"):
        p = os.path.join(CLIENTS, "Alias", name)
        if os.path.exists(p):
            return json.load(open(p))
    return {"methodAlias": {}}


# ---------------------------------------------------------------------- port
class Porter:
    def __init__(self, fp, new_ix):
        self.fp = fp
        self.ref = fp["classes"]
        for v in self.ref.values():
            v["m"] = [tuple(x) for x in v["m"]]
            v["f"] = [tuple(x) for x in v["f"]]
        self.mapping = fp["mapping"]
        self.new = new_ix
        self.owners = owner_table(self.mapping)
        self.cm = {}            # ref class -> new class (final)
        self.cls_report = []    # (orig, ref, new, score, margin, status)
        self.mem_report = []
        self._cache = {}

    # --- per-class structures --------------------------------------------
    def _struct(self, ix, name, keep, translate, cacheable):
        key = (id(ix), name, cacheable)
        if cacheable and key in self._cache:
            return self._cache[key]
        v = ix[name]
        m_pairs = collections.Counter((a, norm(b, keep, translate)) for a, b, _ in v["m"])
        m_desc = collections.Counter(norm(b, keep, translate) for _, b, _ in v["m"])
        f_pairs = collections.Counter((a, norm(b, keep, translate)) for a, b, _ in v["f"])
        res = (m_pairs, m_desc, f_pairs, len(v["m"]), len(v["f"]))
        if cacheable:
            self._cache[key] = res
        return res

    def score(self, ref_name, new_name, known):
        keep = frozenset(known.values())
        # reference side: known classes are translated to their new names
        a = self._struct(self.ref, ref_name, keep, known, False)
        b = self._struct(self.new, new_name, keep, None, not known)
        mp, md, fp_, ma, fa = a
        mp2, md2, fp2, mb, fb = b
        size = 1 - abs(ma - mb) / (max(ma, mb) + 1)
        return 0.4 * jac(mp, mp2) + 0.2 * jac(md, md2) + 0.3 * jac(fp_, fp2) + 0.1 * size

    def candidates(self, ref_name, known):
        rv = self.ref[ref_name]
        ms, fs = len(rv["m"]), len(rv["f"])
        sup = dot(rv["s"])
        out = []
        for nc, nv in self.new.items():
            mc, fc = len(nv["m"]), len(nv["f"])
            if abs(mc - ms) > max(6, ms * 0.3) or abs(fc - fs) > max(6, fs * 0.4):
                continue
            nsup = dot(nv["s"])
            if sup in known:                       # super already resolved -> must match
                if nsup != known[sup]:
                    continue
            elif not is_obf(rv["s"][1:-1]):        # framework super -> must match exactly
                if nsup != sup:
                    continue
            out.append(nc)
        return out

    # --- class resolution -------------------------------------------------
    def resolve_classes(self):
        todo = [c for c in self.mapping["classes"]
                if c["o"].split(".")[-1] not in INNER_OF
                and c["o"].split(".")[-1] not in DERIVED_FROM_PARAM and c["r"] in self.ref]
        todo += [{"o": o, "r": r} for o, r in UNOBF_OWNERS.items() if False]
        known = {}
        # unobfuscated owners map to themselves
        for r in UNOBF_OWNERS.values():
            if r in self.new:
                known[r] = r
        # classes that were not obfuscated in the reference keep their name if still present
        results = {}
        for _ in range(4):
            changed = False
            for c in todo:
                rn = c["r"]
                if rn == c["o"] and rn in self.new:           # not obfuscated at all
                    results[rn] = (rn, 1.0, 1.0)
                    known[rn] = rn
                    continue
                scored = sorted(((self.score(rn, nc, known), nc) for nc in self.candidates(rn, known)), reverse=True)
                if not scored:
                    continue
                best, second = scored[0], (scored[1] if len(scored) > 1 else (0.0, None))
                margin = best[0] - second[0]
                prev = results.get(rn)
                if not prev or prev[0] != best[1]:
                    changed = True
                results[rn] = (best[1], best[0], margin)
                known[rn] = best[1]
            if not changed:
                break
        # classes derived from a parameter type of a resolved class' method
        for key, (okey, mname, pidx) in DERIVED_FROM_PARAM.items():
            cdef = next((c for c in self.mapping["classes"] if c["o"].split(".")[-1] == key), None)
            if not cdef or cdef["r"] not in self.ref:
                continue
            o_ref = self.owners.get(okey)
            o_new = known.get(o_ref)
            rm = next((m for m in self.mapping["methods"] if m["c"] == okey and m["o"] == mname), None)
            if not o_new or not rm:
                continue
            want = "L%s;" % cdef["r"].replace(".", "/")
            got = set()
            for nm in self.members_of(self.new, o_new, "m"):
                if nm[0] != rm["r"]:
                    continue
                params = TYPE.findall(nm[1].split(")")[0])
                for rr in self.members_of(self.ref, o_ref, "m"):
                    if rr[0] == rm["r"] and want in rr[1].split(")")[0]:
                        rp = TYPE.findall(rr[1].split(")")[0])
                        if len(rp) > pidx and rp[pidx] == want[1:-1] and len(params) > pidx:
                            got.add(params[pidx].replace("/", "."))
            if len(got) == 1:
                nn = next(iter(got))
                results[cdef["r"]] = (nn, 1.0, 1.0)
                known[cdef["r"]] = nn
        # inner classes: constructor references the resolved outer class
        for inner, outer in INNER_OF.items():
            cdef = next((c for c in self.mapping["classes"] if c["o"].split(".")[-1] == inner), None)
            if not cdef or cdef["r"] not in self.ref:
                continue
            outer_ref = self.owners.get(outer)
            outer_new = known.get(outer_ref)
            if not outer_new:
                continue
            ot = "L%s;" % outer_new.replace(".", "/")
            rv = self.ref[cdef["r"]]
            cands = [nc for nc, nv in self.new.items()
                     if any(m[0] == "<init>" and ot in m[1] for m in nv["m"])
                     and (is_obf(rv["s"][1:-1]) or nv["s"] == rv["s"])]
            if len(cands) > 1 and self.ref.get(cdef["r"]):
                # prefer those whose super matches the (resolved) reference super
                sup = dot(rv["s"])
                cands2 = [c for c in cands if dot(self.new[c]["s"]) == known.get(sup, sup)]
                cands = cands2 or cands
            scored = sorted(((self.score(cdef["r"], nc, known), nc) for nc in cands), reverse=True)
            if scored:
                margin = scored[0][0] - (scored[1][0] if len(scored) > 1 else 0)
                results[cdef["r"]] = (scored[0][1], scored[0][0], margin)
                known[cdef["r"]] = scored[0][1]
        self.cm = known
        for c in self.mapping["classes"]:
            r = c["r"]
            if r in results:
                nn, sc, mg = results[r]
                status = "OK" if (sc >= 0.6 and mg >= 0.08) or nn == r else "REVIEW"
                self.cls_report.append((c["o"], r, nn, round(sc, 3), round(mg, 3), status))
            else:
                self.cls_report.append((c["o"], r, None, 0, 0, "REVIEW"))
        return results

    # --- member resolution ------------------------------------------------
    def members_of(self, ix, cls, kind):
        out = []
        while cls in ix:
            out += [x for x in ix[cls][kind] if not (kind == "m" and x[2] & 0x1000)]
            cls = dot(ix[cls]["s"])
        return out

    def resolve_members(self):
        keep = frozenset(self.cm.values())
        new_mapping = {"methods": [], "fields": []}
        for kind, key in (("m", "methods"), ("f", "fields")):
            for e in self.mapping[key]:
                k = e["c"] + "#" + e["o"]
                owner_key = MEMBER_OWNER_OVERRIDE.get(k, e["c"])
                ref_owner = self.owners.get(owner_key)
                new_owner = self.cm.get(ref_owner) if ref_owner else None
                if not ref_owner or ref_owner not in self.ref or not new_owner:
                    new_mapping[key].append(e)
                    self.mem_report.append((k, e["r"], e["r"], "REVIEW", "propietario sin resolver"))
                    continue
                refm = [x for x in self.members_of(self.ref, ref_owner, kind) if x[0] == e["r"]]
                newm = self.members_of(self.new, new_owner, kind)
                result, status, why = e["r"], "OK", ""
                if not refm:
                    status, why = "REVIEW", "no existe en la referencia"
                else:
                    kept, alt = False, set()
                    for rm in refm:
                        want = norm(rm[1], keep, self.cm)
                        for nm in newm:
                            if (nm[2] & 8) != (rm[2] & 8):
                                continue
                            if norm(nm[1], keep) == want:
                                if nm[0] == e["r"]:
                                    kept = True
                                else:
                                    alt.add(nm[0])
                    if kept:
                        status = "OK"
                    elif len(alt) == 1:
                        result, status, why = next(iter(alt)), "RENAMED", "mismo descriptor, nombre distinto"
                    else:
                        status, why = "REVIEW", "%d candidatos" % len(alt)
                new_mapping[key].append({**e, "r": result})
                self.mem_report.append((k, e["r"], result, status, why))
        return new_mapping


def cmd_port(args):
    fp = json.load(gzip.open(args.ref, "rt", encoding="utf-8"))
    new_ix = load_index(args.apk, args.index)
    vc = apk_version(args.apk)[0] if args.apk else args.version
    p = Porter(fp, new_ix)
    p.resolve_classes()
    members = p.resolve_members()

    classes = []
    for c in fp["mapping"]["classes"]:
        nn = p.cm.get(c["r"])
        classes.append({"o": c["o"], "r": nn if nn else c["r"]})
    out = {"classes": classes, "methods": members["methods"], "fields": members["fields"]}

    print("Reference version %s -> new versionCode %s" % (fp["version"], vc))
    print("\nCLASES (clase -> ref -> nueva  puntuación/margen  estado)")
    for o, r, nn, sc, mg, st in p.cls_report:
        print("  %-8s %-34s %-26s -> %-26s %5.3f/%5.3f" % (st, o.split(".")[-1], r, nn, sc, mg))
    review_m = [m for m in p.mem_report if m[3] != "OK"]
    n_ok = len(p.mem_report) - len(review_m)
    print("\nMIEMBROS: %d OK, %d a revisar" % (n_ok, len(review_m)))
    for k, old, new, st, why in review_m:
        print("  %-8s %-45s %s -> %s  (%s)" % (st, k, old, new, why))
    n_rev = sum(1 for r in p.cls_report if r[5] != "OK") + len(review_m)

    dest_dir = args.out_dir or CLIENTS
    dest = os.path.join(dest_dir, "TelegramWeb-%s.json" % vc)
    if os.path.exists(dest) and not args.force:
        dest = dest[:-5] + ".generated.json"
        print("\nYa existe un mapeo de referencia; escribo %s (usa --force para sobrescribir)." % os.path.basename(dest))
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    json.dump(out, open(dest, "w"), indent=2)
    alias_dir = os.path.join(dest_dir, "Alias")
    os.makedirs(alias_dir, exist_ok=True)
    apath = os.path.join(alias_dir, os.path.basename(dest))
    json.dump(fp.get("alias", {"methodAlias": {}}), open(apath, "w"), indent=2)
    print("\nEscrito: %s" % dest)

    if args.save_fp:
        newfp = {"version": vc, "mapping": out, "alias": fp.get("alias", {}), "classes": {}}
        wanted = {c["r"] for c in out["classes"]} | set(UNOBF_OWNERS.values())
        for r in wanted:
            if r in new_ix:
                v = new_ix[r]
                newfp["classes"][r] = {"s": v["s"], "i": v["i"], "af": v["af"], "m": v["m"], "f": v["f"]}
        with gzip.open(args.save_fp, "wt", encoding="utf-8") as f:
            json.dump(newfp, f)
        print("Huella de la versión nueva: %s" % args.save_fp)
    print("\n%s" % ("TODO RESUELTO CON CONFIANZA." if n_rev == 0 else
                    "%d elementos a REVISAR antes de usar este mapeo." % n_rev))
    return 0 if n_rev == 0 else 2


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)
    f = sub.add_parser("fingerprint")
    f.add_argument("--apk"); f.add_argument("--index"); f.add_argument("--version")
    f.add_argument("--map", required=True); f.add_argument("--out", required=True)
    p = sub.add_parser("port")
    p.add_argument("--ref", required=True); p.add_argument("--apk"); p.add_argument("--index")
    p.add_argument("--version"); p.add_argument("--out-dir"); p.add_argument("--save-fp")
    p.add_argument("--force", action="store_true")
    args = ap.parse_args()
    if args.cmd == "fingerprint":
        cmd_fingerprint(args)
    else:
        sys.exit(cmd_port(args))


if __name__ == "__main__":
    main()
