package com.my.televip.features.extra;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.Configs.ConfigManager;
import com.my.televip.Configs.ConfigPreferences;
import com.my.televip.application.ApplicationLoaderHook;
import com.my.televip.base.BaseMethodHook;
import com.my.televip.hooks.HMethod;
import com.my.televip.language.Keys;
import com.my.televip.language.Translator;
import com.my.televip.logging.Logger;
import com.my.televip.utils.Utils;
import com.my.televip.virtuals.ui.ChatActivity;

import java.util.HashSet;
import java.util.Set;

/**
 * FLAG_SECURE: blocks screenshots / screen recording and blanks the app in the recents
 * screen. Can be applied everywhere, or only while a chat marked from its menu is open.
 */
public class SecureScreen {

    public static boolean isEnable = false;
    private static final String PREF = "SecureChatList";
    private static Set<Long> chats;
    private static boolean inSecureChat = false;

    private static synchronized Set<Long> set() {
        if (chats == null) {
            chats = new HashSet<>();
            String raw = ConfigPreferences.getString(PREF);
            if (raw != null && !raw.isEmpty()) {
                for (String s : raw.split(",")) {
                    try { chats.add(Long.parseLong(s.trim())); } catch (Throwable ignored) {}
                }
            }
        }
        return chats;
    }

    public static boolean isSecure(long dialogId) {
        return dialogId != 0 && set().contains(dialogId);
    }

    public static void toggle(long dialogId) {
        if (dialogId == 0) return;
        boolean now;
        synchronized (SecureScreen.class) {
            if (set().contains(dialogId)) { set().remove(dialogId); now = false; }
            else { set().add(dialogId); now = true; }
            StringBuilder sb = new StringBuilder();
            for (Long id : set()) { if (sb.length() > 0) sb.append(','); sb.append(id); }
            ConfigPreferences.putString(PREF, sb.toString());
        }
        inSecureChat = now;
        apply(Utils.getCurrentActivity());
        try {
            Toast.makeText(Utils.getCurrentActivity(),
                    Translator.get(now ? Keys.SecureChatOn : Keys.SecureChatOff), Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {}
    }

    private static boolean wanted() {
        boolean everywhere = ConfigManager.secureScreen != null && ConfigManager.secureScreen.isEnable();
        boolean perChat = ConfigManager.secureChats != null && ConfigManager.secureChats.isEnable() && inSecureChat;
        return everywhere || perChat;
    }

    static void apply(Activity a) {
        if (a == null) return;
        try {
            final boolean on = wanted();
            a.runOnUiThread(() -> {
                try {
                    if (on) a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
                    else a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
                } catch (Throwable ignored) {}
            });
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    public static void init() {
        try {
            if (isEnable) { apply(Utils.getCurrentActivity()); return; }
            isEnable = true;

            Context ctx = ApplicationLoaderHook.getApplicationContext();
            ((Application) ctx.getApplicationContext()).registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                @Override public void onActivityCreated(Activity a, Bundle b) { apply(a); }
                @Override public void onActivityStarted(Activity a) { apply(a); }
                @Override public void onActivityResumed(Activity a) { apply(a); }
                @Override public void onActivityPaused(Activity a) {}
                @Override public void onActivityStopped(Activity a) {}
                @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
                @Override public void onActivityDestroyed(Activity a) {}
            });

            Class<?> chat = ClassLoad.getClass(ClassNames.CHAT_ACTIVITY);
            if (chat != null) {
                HMethod.hookMethod(chat, "onResume", new BaseMethodHook() {
                    @Override
                    protected void afterMethod(MethodHookParam param) {
                        try {
                            inSecureChat = isSecure(new ChatActivity(param.thisObject).getDialogId());
                            apply(Utils.getCurrentActivity());
                        } catch (Throwable t) { Logger.e(t); }
                    }
                });
                HMethod.hookMethod(chat, "onPause", new BaseMethodHook() {
                    @Override
                    protected void afterMethod(MethodHookParam param) {
                        inSecureChat = false;
                        apply(Utils.getCurrentActivity());
                    }
                });
            }
            apply(Utils.getCurrentActivity());
        } catch (Throwable t) {
            Logger.e(t);
        }
    }
}
