package com.my.televip.features.extra;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Fires when a scheduled reminder is due and posts a notification. */
public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String text = intent != null ? intent.getStringExtra(Reminders.EXTRA_TEXT) : null;
        Reminders.notify(context, text);
    }
}
