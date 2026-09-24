package com.my.televip.features.extra;

import android.app.Activity;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.CancellationSignal;
import android.view.View;
import android.widget.Toast;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.Configs.ConfigManager;
import com.my.televip.Configs.ConfigPreferences;
import com.my.televip.base.BaseMethodHook;
import com.my.televip.hooks.HMethod;
import com.my.televip.language.Keys;
import com.my.televip.language.Translator;
import com.my.televip.logging.Logger;
import com.my.televip.utils.Utils;
import com.my.televip.virtuals.ui.ChatActivity;

import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.XposedHelpers;

/**
 * Locks selected chats behind the device biometric/credential. When a locked chat
 * opens, its content view is hidden until authentication succeeds.
 * Requires API 28+ (BiometricPrompt); below that the lock is skipped.
 */
public class ChatLock {

    public static boolean isEnable = false;
    private static final String PREF = "ChatLockList";
    private static Set<Long> locked;
    private static final Set<Long> unlockedThisSession = new HashSet<>();

    private static synchronized Set<Long> set() {
        if (locked == null) {
            locked = new HashSet<>();
            String raw = ConfigPreferences.getString(PREF);
            if (raw != null && !raw.isEmpty()) {
                for (String s : raw.split(",")) {
                    try { locked.add(Long.parseLong(s.trim())); } catch (Throwable ignored) {}
                }
            }
        }
        return locked;
    }

    private static synchronized void save() {
        StringBuilder sb = new StringBuilder();
        for (Long id : set()) { if (sb.length() > 0) sb.append(','); sb.append(id); }
        ConfigPreferences.putString(PREF, sb.toString());
    }

    public static boolean isLocked(long dialogId) {
        return dialogId != 0 && set().contains(dialogId);
    }

    public static void toggle(long dialogId) {
        if (dialogId == 0) return;
        boolean nowLocked;
        synchronized (ChatLock.class) {
            if (set().contains(dialogId)) { set().remove(dialogId); nowLocked = false; }
            else { set().add(dialogId); nowLocked = true; }
            unlockedThisSession.remove(dialogId);
            save();
        }
        try {
            Toast.makeText(Utils.getCurrentActivity(),
                    Translator.get(nowLocked ? Keys.ChatLocked : Keys.ChatUnlocked), Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {}
    }

    public static void init() {
        try {
            if (isEnable) return;
            isEnable = true;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return;

            Class<?> chat = ClassLoad.getClass(ClassNames.CHAT_ACTIVITY);
            if (chat == null) return;

            HMethod.hookMethod(chat, "onResume", new BaseMethodHook() {
                @Override
                protected void afterMethod(MethodHookParam param) {
                    try {
                        if (ConfigManager.chatLock == null || !ConfigManager.chatLock.isEnable()) return;
                        ChatActivity ca = new ChatActivity(param.thisObject);
                        long did = ca.getDialogId();
                        if (!isLocked(did) || unlockedThisSession.contains(did)) return;

                        final View content = (View) XposedHelpers.getObjectField(
                                param.thisObject, com.my.televip.obfuscate.Obfuscate.getFieldName("BaseFragment", "fragmentView"));
                        if (content != null) content.setVisibility(View.INVISIBLE);
                        prompt(did, content);
                    } catch (Throwable t) {
                        Logger.e(t);
                    }
                }
            });
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    private static void prompt(final long dialogId, final View content) {
        final Activity activity = Utils.getCurrentActivity();
        if (activity == null) { reveal(content); return; }
        try {
            BiometricPrompt.Builder b = new BiometricPrompt.Builder(activity)
                    .setTitle(Translator.get(Keys.ChatLock))
                    .setDescription(Translator.get(Keys.ChatLockPrompt));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                b.setAllowedAuthenticators(
                        android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK
                                | android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL);
            } else {
                b.setDeviceCredentialAllowed(true);
            }
            BiometricPrompt prompt = b.build();
            prompt.authenticate(new CancellationSignal(), activity.getMainExecutor(),
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                            unlockedThisSession.add(dialogId);
                            reveal(content);
                        }

                        @Override
                        public void onAuthenticationError(int code, CharSequence errString) {
                            // leave hidden and go back
                            try { activity.onBackPressed(); } catch (Throwable ignored) {}
                        }
                    });
        } catch (Throwable t) {
            Logger.e(t);
            reveal(content);
        }
    }

    private static void reveal(final View content) {
        if (content == null) return;
        content.post(() -> content.setVisibility(View.VISIBLE));
    }
}
