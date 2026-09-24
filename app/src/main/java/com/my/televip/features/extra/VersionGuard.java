package com.my.televip.features.extra;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.widget.Toast;

import com.my.televip.Clients.ClientManager;
import com.my.televip.language.Keys;
import com.my.televip.language.Translator;
import com.my.televip.logging.Logger;
import com.my.televip.utils.Utils;

/**
 * The Telegram Web mapping is made for one exact build (R8 renames everything on each release).
 * Warn clearly if the installed Telegram Web is a different build instead of failing silently.
 */
public class VersionGuard {

    public static final long TELEGRAM_WEB_VERSION_CODE = 70999L;
    public static final String TELEGRAM_WEB_VERSION_NAME = "12.10.4";

    public static void check() {
        try {
            if (!ClientManager.is(ClientManager.Client.TelegramWeb)) return;
            final Activity activity = Utils.getCurrentActivity();
            if (activity == null) return;
            PackageInfo info = activity.getPackageManager().getPackageInfo(Utils.pkgName, 0);
            long code = Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
            if (code == TELEGRAM_WEB_VERSION_CODE) return;

            final String msg = Translator.get(Keys.VersionMismatch, TELEGRAM_WEB_VERSION_NAME, info.versionName);
            Logger.w(msg);
            activity.runOnUiThread(() -> Toast.makeText(activity, msg, Toast.LENGTH_LONG).show());
        } catch (Throwable t) {
            Logger.e(t);
        }
    }
}
