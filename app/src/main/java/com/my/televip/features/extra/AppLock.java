package com.my.televip.features.extra;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.my.televip.Configs.ConfigManager;
import com.my.televip.application.ApplicationLoaderHook;
import com.my.televip.language.Keys;
import com.my.televip.language.Translator;
import com.my.televip.logging.Logger;
import com.my.televip.utils.Utils;

/**
 * Locks the whole app behind the device biometric / screen lock.
 * The app is locked when it starts and again after it spent longer than the chosen
 * timeout in the background. A black cover hides the UI until authentication succeeds.
 * Needs no Telegram internals, so it works on every supported version.
 */
public class AppLock {

    public static boolean isEnable = false;

    private static final long[] TIMEOUTS_MS = {0L, 30_000L, 60_000L, 300_000L};

    private static int started = 0;
    private static long leftAt = 0;
    private static boolean locked = true;
    private static boolean prompting = false;
    private static View cover;

    private static boolean enabled() {
        return ConfigManager.appLock != null && ConfigManager.appLock.isEnable();
    }

    private static long timeoutMs() {
        try {
            int i = ConfigManager.appLockTimeout.getCustomCalendar();
            return TIMEOUTS_MS[Math.max(0, Math.min(i, TIMEOUTS_MS.length - 1))];
        } catch (Throwable t) {
            return 0L;
        }
    }

    public static void init() {
        try {
            if (isEnable) return;
            isEnable = true;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return;

            // switched on from the settings while the app is already open: do not lock right away
            if (Utils.getCurrentActivity() != null) locked = false;

            Context ctx = ApplicationLoaderHook.getApplicationContext();
            Application app = (Application) ctx.getApplicationContext();
            app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                @Override public void onActivityCreated(Activity a, Bundle b) {}

                @Override
                public void onActivityStarted(Activity a) {
                    if (started == 0 && leftAt > 0 && System.currentTimeMillis() - leftAt >= timeoutMs()) locked = true;
                    started++;
                }

                @Override
                public void onActivityResumed(Activity a) {
                    try {
                        if (!enabled()) { removeCover(); return; }
                        if (locked) showCover(a);
                    } catch (Throwable t) {
                        Logger.e(t);
                    }
                }

                @Override public void onActivityPaused(Activity a) {}

                @Override
                public void onActivityStopped(Activity a) {
                    started = Math.max(0, started - 1);
                    if (started == 0) leftAt = System.currentTimeMillis();
                }

                @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}

                @Override
                public void onActivityDestroyed(Activity a) {
                    if (cover != null && cover.getContext() == a) { cover = null; prompting = false; }
                }
            });
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    private static void showCover(final Activity a) {
        if (cover != null && cover.getParent() != null && cover.getContext() == a) {
            if (!prompting) prompt(a);
            return;
        }
        removeCover();
        FrameLayout root = new FrameLayout(a);
        root.setBackgroundColor(Color.BLACK);
        root.setClickable(true);
        root.setFocusable(true);

        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);

        TextView title = new TextView(a);
        title.setText(Translator.get(Keys.AppLockTitle));
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);
        box.addView(title);

        Button unlock = new Button(a);
        unlock.setText(Translator.get(Keys.AppLockUnlock));
        unlock.setOnClickListener(v -> { if (!prompting) prompt(a); });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 48;
        box.addView(unlock, lp);

        root.addView(box, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        ((ViewGroup) a.getWindow().getDecorView()).addView(root,
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        cover = root;
        prompt(a);
    }

    private static void removeCover() {
        try {
            if (cover != null && cover.getParent() instanceof ViewGroup) ((ViewGroup) cover.getParent()).removeView(cover);
        } catch (Throwable ignored) {
        }
        cover = null;
        prompting = false;
    }

    private static void unlockNow() {
        locked = false;
        removeCover();
    }

    private static void prompt(final Activity a) {
        try {
            prompting = true;
            BiometricPrompt.Builder b = new BiometricPrompt.Builder(a)
                    .setTitle(Translator.get(Keys.AppLockTitle))
                    .setDescription(Translator.get(Keys.AppLockDesc));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                b.setAllowedAuthenticators(
                        android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK
                                | android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL);
            } else {
                b.setDeviceCredentialAllowed(true);
            }
            b.build().authenticate(new CancellationSignal(), a.getMainExecutor(),
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                            unlockNow();
                        }

                        @Override
                        public void onAuthenticationError(int code, CharSequence err) {
                            prompting = false;
                            // no lock screen / no hardware: never lock the user out of the app
                            if (code == 11 || code == 12 || code == 14) unlockNow();
                        }
                    });
        } catch (Throwable t) {
            Logger.e(t);
            unlockNow();
        }
    }
}
