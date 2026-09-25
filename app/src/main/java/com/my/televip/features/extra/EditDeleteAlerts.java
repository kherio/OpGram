package com.my.televip.features.extra;

import android.app.Activity;
import android.widget.Toast;

import com.my.televip.Configs.ConfigManager;
import com.my.televip.language.Keys;
import com.my.televip.language.Translator;
import com.my.televip.utils.Utils;

/**
 * Lightweight in-app alerts when a message is deleted or edited. Both call sites
 * (ShowDeletedMessages.stampDeleted and SaveEditsHistory) already run in the
 * messenger path, so no obfuscated hook is needed here. Alerts are shown as a
 * toast to stay non-intrusive and avoid depending on Telegram's notification UI.
 */
public class EditDeleteAlerts {

    public static void onDeleted() {
        if (ConfigManager.alertDeleted == null || !ConfigManager.alertDeleted.isEnable()) return;
        toast(Translator.get(Keys.AlertDeletedToast));
    }

    public static void onEdited() {
        if (ConfigManager.alertEdited == null || !ConfigManager.alertEdited.isEnable()) return;
        toast(Translator.get(Keys.AlertEditedToast));
    }

    private static void toast(final String text) {
        try {
            final Activity a = Utils.getCurrentActivity();
            if (a != null) a.runOnUiThread(() -> Toast.makeText(a, text, Toast.LENGTH_SHORT).show());
        } catch (Throwable ignored) {
        }
    }
}
