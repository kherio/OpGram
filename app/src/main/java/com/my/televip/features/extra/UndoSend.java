package com.my.televip.features.extra;

import android.app.Activity;
import android.widget.Toast;

import com.my.televip.Configs.ConfigManager;
import com.my.televip.base.BaseMethodHook;
import com.my.televip.language.Keys;
import com.my.televip.language.Translator;
import com.my.televip.logging.Logger;
import com.my.televip.obfuscate.Obfuscate;
import com.my.televip.utils.Utils;

import java.lang.reflect.Member;
import java.lang.reflect.Method;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Holds an outgoing message for a few seconds so it can be undone. Hooks
 * SendMessagesHelper.sendMessage(SendMessageParams) (not obfuscated); the send is
 * re-issued after the delay unless the user cancels. A "bypass" flag lets our own
 * delayed re-send go through without being intercepted again.
 */
public class UndoSend {

    public static boolean isEnable = false;
    private static volatile boolean bypass = false;

    public static void init() {
        try {
            if (isEnable) return;
            isEnable = true;

            final Class<?> helper = XposedHelpers.findClassIfExists(
                    Obfuscate.getClassName("org.telegram.messenger.SendMessagesHelper"), Utils.classLoader);
            final Class<?> paramsClass = XposedHelpers.findClassIfExists(
                    Obfuscate.getClassName("org.telegram.messenger.SendMessagesHelper$SendMessageParams"), Utils.classLoader);
            if (helper == null || paramsClass == null) return;

            Method target = null;
            for (Method m : helper.getDeclaredMethods()) {
                if (m.getName().equals(Obfuscate.getMethodName("SendMessagesHelper", "sendMessage"))
                        && m.getParameterTypes().length == 1 && m.getParameterTypes()[0] == paramsClass) {
                    target = m;
                    break;
                }
            }
            if (target == null) return;

            XposedBridge.hookMethod(target, new BaseMethodHook() {
                @Override
                protected void beforeMethod(MethodHookParam param) {
                    if (bypass) return;
                    if (ConfigManager.undoSend == null || !ConfigManager.undoSend.isEnable()) return;
                    try {
                        final Activity activity = Utils.getCurrentActivity();
                        if (activity == null) return;

                        final int seconds = 5;
                        final Member method = param.method;
                        final Object thiz = param.thisObject;
                        final Object[] args = param.args.clone();

                        // Cancel the immediate send; we re-issue it after the delay.
                        param.setResult(null);

                        final boolean[] cancelled = {false};
                        final android.widget.Toast toast = Toast.makeText(activity,
                                Translator.get(Keys.UndoSendToast, seconds), Toast.LENGTH_SHORT);

                        // A simple cancel dialog so the user can stop the send.
                        activity.runOnUiThread(() -> {
                            try {
                                android.app.AlertDialog dlg = new android.app.AlertDialog.Builder(activity)
                                        .setMessage(Translator.get(Keys.UndoSendToast, seconds))
                                        .setNegativeButton(Translator.get(Keys.UndoSendCancel), (d, w) -> cancelled[0] = true)
                                        .setCancelable(true)
                                        .create();
                                dlg.show();
                                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                    try { if (dlg.isShowing()) dlg.dismiss(); } catch (Throwable ignored) {}
                                    if (cancelled[0]) {
                                        toastNow(activity, Translator.get(Keys.UndoSendCancelled));
                                        return;
                                    }
                                    resend(method, thiz, args);
                                }, seconds * 1000L);
                            } catch (Throwable t) {
                                Logger.e(t);
                                resend(method, thiz, args);
                            }
                        });
                    } catch (Throwable t) {
                        Logger.e(t);
                    }
                }
            });
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    private static void resend(Member method, Object thiz, Object[] args) {
        bypass = true;
        try {
            XposedBridge.invokeOriginalMethod(method, thiz, args);
        } catch (Throwable t) {
            Logger.e(t);
        } finally {
            bypass = false;
        }
    }

    private static void toastNow(Activity a, String text) {
        try {
            a.runOnUiThread(() -> Toast.makeText(a, text, Toast.LENGTH_SHORT).show());
        } catch (Throwable ignored) {
        }
    }
}
