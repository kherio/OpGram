package com.my.televip.features.extra;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import com.my.televip.Branding;
import com.my.televip.language.Keys;
import com.my.televip.language.Translator;
import com.my.televip.logging.Logger;
import com.my.televip.utils.Utils;

/**
 * Simple message reminders. Uses Android's AlarmManager + a notification, so it
 * does not depend on any Telegram internals. A reminder fires even if Telegram
 * is closed. Offered from the chat header menu ("Remind me about this chat").
 */
public class Reminders {

    private static final String CHANNEL_ID = "opgram_reminders";
    public static final String ACTION = "com.my.televip.REMINDER";
    public static final String EXTRA_TEXT = "text";

    private static boolean receiverRegistered = false;

    /** Registers the alarm receiver in the current (Telegram) process. */
    public static void ensureReceiver(Context ctx) {
        if (receiverRegistered) return;
        try {
            android.content.IntentFilter f = new android.content.IntentFilter(ACTION);
            ctx.getApplicationContext().registerReceiver(new ReminderReceiver(), f,
                    Build.VERSION.SDK_INT >= 34 ? Context.RECEIVER_NOT_EXPORTED : 0);
            receiverRegistered = true;
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    public static void offer(final long dialogId, final String chatTitle) {
        final Activity activity = Utils.getCurrentActivity();
        if (activity == null) return;
        final String[] labels = {
                Translator.get(Keys.RemindIn1h),
                Translator.get(Keys.RemindTonight),
                Translator.get(Keys.RemindTomorrow),
        };
        final long[] delays = {
                60L * 60 * 1000,                 // 1 hour
                tonightMillis(),                 // 20:00 today (or +1h if past)
                tomorrowMillis(),                // 09:00 tomorrow
        };
        try {
            new AlertDialog.Builder(activity)
                    .setTitle(Translator.get(Keys.RemindMe))
                    .setItems(labels, (d, which) -> schedule(activity,
                            System.currentTimeMillis() + delays[which] > System.currentTimeMillis() && which == 0
                                    ? System.currentTimeMillis() + delays[0] : delays[which],
                            which == 0 ? System.currentTimeMillis() + delays[0] : delays[which],
                            chatTitle))
                    .setNegativeButton(Translator.get(Keys.Cancel), null)
                    .show();
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    private static void schedule(Context ctx, long ignored, long triggerAt, String chatTitle) {
        try {
            ensureChannel(ctx);
            ensureReceiver(ctx);
            Intent i = new Intent(ACTION);
            i.setPackage(ctx.getPackageName());
            i.putExtra(EXTRA_TEXT, chatTitle);
            int id = (int) (System.currentTimeMillis() & 0x7fffffff);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT
                    | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
            PendingIntent pi = PendingIntent.getBroadcast(ctx, id, i, flags);

            android.app.AlarmManager am = (android.app.AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (Build.VERSION.SDK_INT >= 23) {
                am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else {
                am.setExact(android.app.AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
            Toast.makeText(ctx, Translator.get(Keys.ReminderSet), Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    static void ensureChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(new NotificationChannel(
                        CHANNEL_ID, Branding.NAME + " reminders", NotificationManager.IMPORTANCE_HIGH));
            }
        }
    }

    static void notify(Context ctx, String text) {
        try {
            ensureChannel(ctx);
            Intent open = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
            int flags = PendingIntent.FLAG_UPDATE_CURRENT
                    | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
            PendingIntent pi = open != null ? PendingIntent.getActivity(ctx, 0, open, flags) : null;

            Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? new Notification.Builder(ctx, CHANNEL_ID)
                    : new Notification.Builder(ctx);
            b.setContentTitle(Branding.NAME)
                    .setContentText(text != null ? text : "")
                    .setSmallIcon(ctx.getApplicationInfo().icon)
                    .setAutoCancel(true);
            if (pi != null) b.setContentIntent(pi);

            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify((int) (System.currentTimeMillis() & 0x7fffffff), b.build());
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    private static long tonightMillis() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.set(java.util.Calendar.HOUR_OF_DAY, 20);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        long t = c.getTimeInMillis();
        if (t <= System.currentTimeMillis()) t = System.currentTimeMillis() + 60L * 60 * 1000;
        return t;
    }

    private static long tomorrowMillis() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.add(java.util.Calendar.DAY_OF_YEAR, 1);
        c.set(java.util.Calendar.HOUR_OF_DAY, 9);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        return c.getTimeInMillis();
    }
}
