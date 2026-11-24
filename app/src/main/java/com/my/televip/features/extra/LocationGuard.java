package com.my.televip.features.extra;

import android.app.Activity;
import android.content.Context;
import android.view.ContextThemeWrapper;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.Configs.ConfigManager;
import com.my.televip.language.Keys;
import com.my.televip.language.Translator;
import com.my.televip.logging.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Asks for confirmation before a location is sent to a chat, and offers to send an approximate one (about 1 km).
 * The send method is found by shape (first parameter is a MessageMedia, second an int).
 */
public class LocationGuard {

    private static boolean isEnable = false;
    private static boolean passing = false;

    public static void init() {
        try {
            if (isEnable) return;
            Class<?> chat = ClassLoad.getClass(ClassNames.CHAT_ACTIVITY);
            if (chat == null) return;
            int n = 0;
            for (final Method m : chat.getDeclaredMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (m.getReturnType() == void.class && !Modifier.isStatic(m.getModifiers()) && p.length == 2
                        && p[0].getName().endsWith("MessageMedia") && p[1] == int.class) {
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(final MethodHookParam param) {
                            try {
                                if (passing || ConfigManager.locationGuard == null || !ConfigManager.locationGuard.isEnable()) return;
                                Activity act = findActivity(param.thisObject);
                                if (act == null || param.args[0] == null) return;
                                param.setResult(null);
                                ask(act, m, param);
                            } catch (Throwable t) {
                                Logger.e(t);
                            }
                        }
                    });
                    n++;
                }
            }
            isEnable = n > 0;
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    private static Activity findActivity(Object chat) {
        for (Class<?> c = chat.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers()) && Activity.class.isAssignableFrom(f.getType())) {
                    try {
                        f.setAccessible(true);
                        Object v = f.get(chat);
                        if (v instanceof Activity) return (Activity) v;
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        return null;
    }

    private static void ask(Activity act, final Method m, final XC_MethodHook.MethodHookParam param) {
        final Object media = param.args[0];
        final boolean live = media.getClass().getSimpleName().contains("Live") || (int) param.args[1] != 0;
        final Object geo = XposedHelpers.getObjectField(media, "geo");
        boolean light = com.my.televip.virtuals.ActionBar.Theme.isLight();
        Context themed = new ContextThemeWrapper(act,
                light ? android.R.style.Theme_DeviceDefault_Light_Dialog_Alert : android.R.style.Theme_DeviceDefault_Dialog_Alert);
        android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(themed)
                .setTitle(Translator.get(Keys.LGTitle))
                .setMessage(Translator.get(live ? Keys.LGMessageLive : Keys.LGMessage))
                .setNegativeButton(Translator.get(Keys.LGCancel), null)
                .setPositiveButton(Translator.get(Keys.LGSend), (d, w) -> send(m, param));
        if (!live && geo != null) {
            b.setNeutralButton(Translator.get(Keys.LGApprox), (d, w) -> {
                try {
                    double lat = XposedHelpers.getDoubleField(geo, "lat");
                    double lon = XposedHelpers.getDoubleField(geo, "_long");
                    XposedHelpers.setDoubleField(geo, "lat", Math.round(lat * 100.0) / 100.0);
                    XposedHelpers.setDoubleField(geo, "_long", Math.round(lon * 100.0) / 100.0);
                } catch (Throwable t) {
                    Logger.e(t);
                }
                send(m, param);
            });
        }
        b.show();
    }

    private static void send(Method m, XC_MethodHook.MethodHookParam param) {
        try {
            passing = true;
            XposedBridge.invokeOriginalMethod(m, param.thisObject, param.args);
        } catch (Throwable t) {
            Logger.e(t);
        } finally {
            passing = false;
        }
    }
}
