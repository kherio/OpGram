package com.my.televip.features.extra;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.my.televip.Configs.ConfigManager;
import com.my.televip.logging.Logger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/**
 * Removes tracking parameters (utm_*, fbclid, gclid...) from web links before they open in the browser.
 * Works at Activity.startActivity level, so it does not depend on Telegram's obfuscated link code.
 */
public class AntiTrack {

    private static boolean isEnable = false;

    private static final Set<String> GENERIC = new HashSet<>(Arrays.asList(
            "fbclid", "gclid", "gclsrc", "gbraid", "wbraid", "dclid", "msclkid", "yclid", "ttclid", "twclid",
            "li_fat_id", "mc_cid", "mc_eid", "igshid", "igsh", "_hsenc", "_hsmi", "mkt_tok", "oly_enc_id",
            "oly_anon_id", "vero_id", "spm", "ref_src", "ref_url", "s_kwcid", "cmpid", "ocid"));
    private static final Set<String> HOSTS_SI = new HashSet<>(Arrays.asList(
            "youtube.com", "youtu.be", "music.youtube.com", "m.youtube.com", "open.spotify.com", "spotify.link"));
    private static final List<String> SKIP_HOSTS = Arrays.asList("t.me", "telegram.me", "telegram.org", "telegra.ph", "telesco.pe");

    public static void init() {
        try {
            if (isEnable) return;
            isEnable = true;
            XC_MethodHook hook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        if (ConfigManager.antiTrack == null || !ConfigManager.antiTrack.isEnable()) return;
                        if (param.args.length == 0 || !(param.args[0] instanceof Intent)) return;
                        Intent intent = (Intent) param.args[0];
                        if (!Intent.ACTION_VIEW.equals(intent.getAction())) return;
                        Uri data = intent.getData();
                        if (data == null) return;
                        Uri clean = clean(data);
                        if (clean != null && !clean.equals(data)) {
                            intent.setData(clean);
                        }
                    } catch (Throwable t) {
                        Logger.e(t);
                    }
                }
            };
            XposedBridge.hookAllMethods(Activity.class, "startActivity", hook);
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    /** Returns the link without tracking parameters, or null when it must be left as it is. */
    public static Uri clean(Uri uri) {
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equals("http") || scheme.equals("https"))) return null;
        String host = uri.getHost();
        if (host == null) return null;
        host = host.toLowerCase();
        for (String s : SKIP_HOSTS) {
            if (host.equals(s) || host.endsWith("." + s)) return null;
        }
        if (!uri.isHierarchical() || uri.getQuery() == null) return null;

        boolean siHost = false;
        for (String h : HOSTS_SI) {
            if (host.equals(h) || host.endsWith("." + h)) { siHost = true; break; }
        }
        Uri.Builder b = uri.buildUpon().clearQuery();
        boolean changed = false;
        for (String name : uri.getQueryParameterNames()) {
            String lower = name.toLowerCase();
            boolean drop = lower.startsWith("utm_") || GENERIC.contains(lower)
                    || (siHost && (lower.equals("si") || lower.equals("pp") || lower.equals("feature")));
            if (drop) { changed = true; continue; }
            for (String v : uri.getQueryParameters(name)) b.appendQueryParameter(name, v);
        }
        return changed ? b.build() : null;
    }
}
