package com.my.televip;

import android.content.Context;
import android.content.pm.PackageInfo;

import com.my.televip.application.ApplicationLoaderHook;
import com.my.televip.utils.Utils;

/**
 * OpGram branding.
 *
 * OpGram is a fork of TeleVip, originally created by @m_1_iq
 * (https://github.com/mustafa1dev/TeleVip-Lsposed). TeleVip is licensed under
 * the GPL, which requires keeping attribution to the original author; the
 * credit below must stay in the module.
 */
public final class Branding {
    public static final String NAME = "OpGram";
    public static final String ORIGINAL_AUTHOR = "@m_1_iq";
    public static final String REPO_URL = "https://github.com/kherio/OpGram";
    public static final String RELEASES_URL = REPO_URL + "/releases";

    /** Reads the module's own versionName from its package (falls back if unavailable). */
    public static String version() {
        try {
            Context ctx = ApplicationLoaderHook.getApplicationContext();
            if (ctx != null) {
                PackageInfo info = ctx.getPackageManager().getPackageInfo("com.my.televip", 0);
                if (info != null && info.versionName != null) return info.versionName;
            }
        } catch (Throwable ignored) {
        }
        return "";
    }

    /** Title shown in the OpGram panel header, e.g. "OpGram 1.1". */
    public static String title() {
        String v = version();
        return v.isEmpty() ? NAME : NAME + " " + v;
    }

    private Branding() {}
}
