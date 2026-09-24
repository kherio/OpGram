package com.my.televip.settings.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

import com.my.televip.Configs.ConfigPreferences;
import com.my.televip.Database.MessageDatabase;
import com.my.televip.language.Keys;
import com.my.televip.language.Translator;
import com.my.televip.logging.Logger;

import java.io.File;

/** Export / import of TeleVip settings (clipboard + share sheet) and cleanup of the edits-history database. */
public class BackupActions {

    public static void exportSettings(Activity activity) {
        try {
            String json = ConfigPreferences.exportJson();
            if (json == null) return;
            ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("TeleVip backup", json));

            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_TEXT, json);
            activity.startActivity(Intent.createChooser(send, Translator.get(Keys.ExportSettings)));
            Toast.makeText(activity, Translator.get(Keys.SettingsExported), Toast.LENGTH_LONG).show();
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    public static void importSettings(Activity activity) {
        try {
            ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            CharSequence text = null;
            if (cm.hasPrimaryClip() && cm.getPrimaryClip() != null && cm.getPrimaryClip().getItemCount() > 0) {
                text = cm.getPrimaryClip().getItemAt(0).coerceToText(activity);
            }
            boolean ok = text != null && ConfigPreferences.importJson(text.toString());
            Toast.makeText(activity, Translator.get(ok ? Keys.SettingsImported : Keys.ImportInvalid), Toast.LENGTH_LONG).show();
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    public static void clearEditsHistory(Activity activity) {
        try {
            new AlertDialog.Builder(activity)
                    .setTitle(Translator.get(Keys.ClearEditsHistory))
                    .setMessage(Translator.get(Keys.ClearEditsHistoryConfirm))
                    .setPositiveButton(Translator.get(Keys.Done), (d, w) -> {
                        try {
                            File db = new File(MessageDatabase.getDataBasePath());
                            if (db.exists()) {
                                SQLiteDatabase sql = SQLiteDatabase.openDatabase(db.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE);
                                sql.delete("messages", null, null);
                                sql.execSQL("VACUUM");
                                sql.close();
                            }
                            Toast.makeText(activity, Translator.get(Keys.EditsHistoryCleared), Toast.LENGTH_SHORT).show();
                        } catch (Throwable t) {
                            Logger.e(t);
                        }
                    })
                    .setNegativeButton(Translator.get(Keys.Cancel), null)
                    .show();
        } catch (Throwable t) {
            Logger.e(t);
        }
    }
}
