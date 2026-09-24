package com.my.televip.features.ui;

import android.content.Context;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.Configs.ConfigManager;
import com.my.televip.application.ApplicationLoaderHook;
import com.my.televip.base.BaseMethodHook;
import com.my.televip.hooks.HMethod;
import com.my.televip.logging.Logger;
import com.my.televip.obfuscate.ArgsResolver;
import com.my.televip.obfuscate.Obfuscate;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.robv.android.xposed.XposedHelpers;

/**
 * Shows the full date and time (with seconds) of a user's last connection,
 * instead of Telegram's rounded "today at 14:32" / "yesterday" / date-only text.
 * Only works when the user shares their last seen: if it is hidden, the server
 * never sends a timestamp ("recently", "within a week"...), and those are left untouched.
 */
public class ExactLastSeen {

    public static boolean isEnable = false;

    public static void init() {
        try {
            if (!isEnable) {
                isEnable = true;

                final Class<?> localeController = ClassLoad.getClass(ClassNames.LOCALE_CONTROLLER);
                if (localeController == null) return;

                HMethod.hookMethod(localeController, Obfuscate.getMethodName("LocaleController", "formatDateOnline"),
                        ArgsResolver.merge("formatDateOnline", new Class[]{long.class, boolean[].class}, new BaseMethodHook() {
                            @Override
                            protected void afterMethod(MethodHookParam param) {
                                if (ConfigManager.exactLastSeen == null || !ConfigManager.exactLastSeen.isEnable()) return;
                                try {
                                    long seconds = (long) param.args[0];
                                    if (seconds <= 0) return;

                                    String exact = format(seconds * 1000L);

                                    // Keep Telegram's own translated "last seen %s" wrapper
                                    String result = null;
                                    Context ctx = ApplicationLoaderHook.getApplicationContext();
                                    if (ctx != null) {
                                        int res = ctx.getResources().getIdentifier("LastSeenDateFormatted", "string", ctx.getPackageName());
                                        if (res != 0) {
                                            result = (String) XposedHelpers.callStaticMethod(localeController,
                                                    Obfuscate.getMethodName("LocaleController", "formatString"),
                                                    new Class[]{String.class, int.class, Object[].class},
                                                    "LastSeenDateFormatted", res, new Object[]{exact});
                                        }
                                    }
                                    if (result == null || result.isEmpty() || result.startsWith("LOC_ERR")) {
                                        result = exact;
                                    }

                                    boolean[] madeShorter = (boolean[]) param.args[1];
                                    if (madeShorter != null && madeShorter.length > 0) madeShorter[0] = false;

                                    param.setResult(result);
                                } catch (Throwable t) {
                                    Logger.e(t);
                                }
                            }
                        }));
            }
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    private static String format(long millis) {
        boolean is24h = true;
        try {
            Context ctx = ApplicationLoaderHook.getApplicationContext();
            if (ctx != null) is24h = android.text.format.DateFormat.is24HourFormat(ctx);
        } catch (Throwable ignored) {}
        String pattern = is24h ? "dd/MM/yyyy HH:mm:ss" : "dd/MM/yyyy hh:mm:ss a";
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date(millis));
    }
}
