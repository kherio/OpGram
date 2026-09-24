package com.my.televip.features.extra;

import android.app.Activity;
import android.app.AlertDialog;

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
 * Asks for confirmation before sending stickers, GIFs, voice notes and round videos,
 * to avoid accidental sends. Hooks SendMessagesHelper.sendMessage(SendMessageParams),
 * the common path for all of them.
 */
public class ConfirmSending {

    public static boolean isEnable = false;
    private static volatile boolean bypass = false;

    public static void init() {
        try {
            if (isEnable) return;
            isEnable = true;

            final Class<?> helper = XposedHelpers.findClassIfExists(Obfuscate.getClassName("org.telegram.messenger.SendMessagesHelper"), Utils.classLoader);
            final Class<?> paramsClass = XposedHelpers.findClassIfExists(Obfuscate.getClassName("org.telegram.messenger.SendMessagesHelper$SendMessageParams"), Utils.classLoader);
            final Class<?> messageObject = XposedHelpers.findClassIfExists(Obfuscate.getClassName("org.telegram.messenger.MessageObject"), Utils.classLoader);
            if (helper == null || paramsClass == null || messageObject == null) return;

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
                    if (ConfigManager.confirmSending == null || !ConfigManager.confirmSending.isEnable()) return;
                    try {
                        Object params = param.args[0];
                        if (params == null) return;
                        Object doc = XposedHelpers.getObjectField(params, "document");
                        if (doc == null) return;

                        String what = null;
                        if (is(messageObject, "isVoiceDocument", doc)) what = Translator.get(Keys.ConfirmVoice);
                        else if (is(messageObject, "isRoundVideoDocument", doc)) what = Translator.get(Keys.ConfirmRound);
                        else if (is(messageObject, "isGifDocument", doc)) what = Translator.get(Keys.ConfirmGif);
                        else if (is(messageObject, "isStickerDocument", doc) || isAnimatedSticker(messageObject, doc)) what = Translator.get(Keys.ConfirmSticker);
                        if (what == null) return;

                        final Activity activity = Utils.getCurrentActivity();
                        if (activity == null || activity.isFinishing()) return;

                        param.setResult(null);
                        final Member method = param.method;
                        final Object thiz = param.thisObject;
                        final Object[] args = param.args.clone();
                        final String message = what;

                        activity.runOnUiThread(() -> {
                            try {
                                new AlertDialog.Builder(activity)
                                        .setTitle(Translator.get(Keys.ConfirmSendingTitle))
                                        .setMessage(message)
                                        .setPositiveButton(Translator.get(Keys.Send), (d, w) -> {
                                            bypass = true;
                                            try {
                                                XposedBridge.invokeOriginalMethod(method, thiz, args);
                                            } catch (Throwable t) {
                                                Logger.e(t);
                                            } finally {
                                                bypass = false;
                                            }
                                        })
                                        .setNegativeButton(Translator.get(Keys.Cancel), null)
                                        .show();
                            } catch (Throwable t) {
                                Logger.e(t);
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

    private static boolean is(Class<?> mo, String name, Object doc) {
        try {
            return (boolean) XposedHelpers.callStaticMethod(mo, Obfuscate.getMethodName("MessageObject", name), doc);
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean isAnimatedSticker(Class<?> mo, Object doc) {
        try {
            return (boolean) XposedHelpers.callStaticMethod(mo, Obfuscate.getMethodName("MessageObject", "isAnimatedStickerDocument"),
                    new Class[]{doc.getClass().getSuperclass() != null ? findDocumentClass(doc) : doc.getClass(), boolean.class}, doc, true);
        } catch (Throwable t) {
            return false;
        }
    }

    private static Class<?> findDocumentClass(Object doc) {
        Class<?> c = doc.getClass();
        while (c != null && !c.getName().endsWith("TLRPC$Document")) c = c.getSuperclass();
        return c != null ? c : doc.getClass();
    }
}
