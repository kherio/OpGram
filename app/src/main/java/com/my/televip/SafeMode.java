package com.my.televip;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Crash-loop protection. Every start of the main Telegram process increments a counter that is
 * reset once the app has been running for a while. If Telegram dies during startup several times
 * in a row (typically after a Telegram update breaks a hook), OpGram stops installing its hooks
 * and offers to re-enable them.
 */
public class SafeMode {

    private static final int MAX_FAILED_STARTS = 3;
    private static final long STABLE_AFTER_MS = 20_000L;

    private static File counterFile;
    public static boolean active = false;
    private static boolean dialogShown = false;

    public static void onProcessStart(String dataDir) {
        try {
            File dir = new File(dataDir, "files");
            if (!dir.exists()) dir.mkdirs();
            counterFile = new File(dir, "opgram_failed_starts");
            int n = read();
            if (n >= MAX_FAILED_STARTS) {
                active = true;
            } else {
                write(n + 1);
            }
        } catch (Throwable ignored) {
        }
    }

    /** Called once the hooks are installed: if the app keeps running, the start counts as good. */
    public static void scheduleStableReset() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> write(0), STABLE_AFTER_MS);
    }

    public static void showDialog(final Activity activity) {
        if (dialogShown || activity == null) return;
        dialogShown = true;
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                new AlertDialog.Builder(activity)
                        .setTitle("OpGram")
                        .setMessage("OpGram se ha desactivado porque Telegram se cerró varias veces al arrancar.\n\n" +
                                "OpGram was disabled because Telegram crashed several times on startup.")
                        .setPositiveButton("Reactivar / Re-enable", (d, w) -> {
                            write(0);
                            Intent intent = activity.getPackageManager().getLaunchIntentForPackage(activity.getPackageName());
                            if (intent != null) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                activity.startActivity(intent);
                            }
                            activity.finishAffinity();
                            android.os.Process.killProcess(android.os.Process.myPid());
                        })
                        .setNegativeButton("Seguir sin OpGram / Continue", null)
                        .show();
            } catch (Throwable ignored) {
            }
        });
    }

    private static int read() {
        try {
            if (counterFile == null || !counterFile.exists()) return 0;
            byte[] b = new byte[16];
            int len;
            try (FileInputStream in = new FileInputStream(counterFile)) { len = in.read(b); }
            return len > 0 ? Integer.parseInt(new String(b, 0, len, StandardCharsets.US_ASCII).trim()) : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    private static void write(int n) {
        try {
            if (counterFile == null) return;
            try (FileOutputStream out = new FileOutputStream(counterFile, false)) {
                out.write(String.valueOf(n).getBytes(StandardCharsets.US_ASCII));
            }
        } catch (Throwable ignored) {
        }
    }
}
