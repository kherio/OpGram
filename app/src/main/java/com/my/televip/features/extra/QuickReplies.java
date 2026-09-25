package com.my.televip.features.extra;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.my.televip.Configs.ConfigPreferences;
import com.my.televip.language.Keys;
import com.my.televip.language.Translator;
import com.my.televip.logging.Logger;
import com.my.televip.utils.Utils;

import org.json.JSONArray;

import java.util.Date;

/**
 * Saved quick-reply templates with simple variables. Templates are stored in prefs
 * as a JSON array. When picked, variables are expanded and the text is copied to
 * the clipboard for pasting into the chat box (robust: no obfuscated input hook).
 *
 * Variables: {date} {time} {datetime}
 */
public class QuickReplies {

    private static final String PREF = "QuickReplies";

    public static void show(final Activity activity) {
        try {
            final JSONArray list = load();
            LinearLayout root = new LinearLayout(activity);
            root.setOrientation(LinearLayout.VERTICAL);
            int pad = dp(activity, 16);
            root.setPadding(pad, pad, pad, 0);

            final ScrollView scroll = new ScrollView(activity);
            final LinearLayout items = new LinearLayout(activity);
            items.setOrientation(LinearLayout.VERTICAL);
            scroll.addView(items);
            root.addView(scroll, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 300)));

            renderList(activity, items, list);

            Button add = new Button(activity);
            add.setText(Translator.get(Keys.QuickReplyAdd));
            add.setOnClickListener(v -> editDialog(activity, items, list, -1));
            root.addView(add);

            new AlertDialog.Builder(activity)
                    .setTitle(Translator.get(Keys.QuickReplies))
                    .setView(root)
                    .setPositiveButton(Translator.get(Keys.Done), null)
                    .show();
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    private static void renderList(Activity activity, LinearLayout items, JSONArray list) {
        items.removeAllViews();
        if (list.length() == 0) {
            TextView empty = new TextView(activity);
            empty.setText(Translator.get(Keys.QuickReplyEmpty));
            empty.setPadding(0, dp(activity, 16), 0, dp(activity, 16));
            empty.setGravity(Gravity.CENTER);
            items.addView(empty);
            return;
        }
        for (int i = 0; i < list.length(); i++) {
            final int index = i;
            final String text = list.optString(i);

            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, dp(activity, 6), 0, dp(activity, 6));

            TextView tv = new TextView(activity);
            tv.setText(text);
            tv.setTextSize(15);
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tv.setLayoutParams(tp);
            // tap = expand variables + copy to clipboard
            tv.setOnClickListener(v -> copyExpanded(activity, text));

            TextView edit = new TextView(activity);
            edit.setText("  ✎  ");
            edit.setTextColor(Color.GRAY);
            edit.setOnClickListener(v -> editDialog(activity, items, list, index));

            TextView del = new TextView(activity);
            del.setText("  🗑  ");
            del.setTextColor(Color.GRAY);
            del.setOnClickListener(v -> { removeAt(list, index); save(list); renderList(activity, items, list); });

            row.addView(tv);
            row.addView(edit);
            row.addView(del);
            items.addView(row);
        }
    }

    private static void editDialog(Activity activity, LinearLayout items, JSONArray list, int index) {
        final EditText input = new EditText(activity);
        input.setHint(Translator.get(Keys.QuickReplyHint));
        if (index >= 0) input.setText(list.optString(index));
        new AlertDialog.Builder(activity)
                .setTitle(Translator.get(index >= 0 ? Keys.QuickReplyEdit : Keys.QuickReplyAdd))
                .setView(input)
                .setPositiveButton(Translator.get(Keys.Save), (d, w) -> {
                    String text = input.getText().toString().trim();
                    if (text.isEmpty()) return;
                    if (index >= 0) setAt(list, index, text); else list.put(text);
                    save(list);
                    renderList(activity, items, list);
                })
                .setNegativeButton(Translator.get(Keys.Cancel), null)
                .show();
    }

    private static void copyExpanded(Context ctx, String template) {
        String out = expand(template);
        ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("OpGram quick reply", out));
        Toast.makeText(ctx, Translator.get(Keys.QuickReplyCopied), Toast.LENGTH_SHORT).show();
    }

    static String expand(String t) {
        if (t == null) return "";
        Date now = new Date();
        String date = DateFormat.format("dd/MM/yyyy", now).toString();
        String time = DateFormat.format("HH:mm", now).toString();
        return t.replace("{date}", date)
                .replace("{time}", time)
                .replace("{datetime}", date + " " + time);
    }

    private static JSONArray load() {
        try {
            String raw = ConfigPreferences.getString(PREF);
            return raw != null && !raw.isEmpty() ? new JSONArray(raw) : new JSONArray();
        } catch (Throwable t) {
            return new JSONArray();
        }
    }

    private static void save(JSONArray list) {
        ConfigPreferences.putString(PREF, list.toString());
    }

    private static void setAt(JSONArray a, int i, String v) {
        try { a.put(i, v); } catch (Throwable ignored) {}
    }

    private static void removeAt(JSONArray a, int i) {
        try { a.remove(i); } catch (Throwable ignored) {}
    }

    private static int dp(Context ctx, float v) {
        return Math.round(v * ctx.getResources().getDisplayMetrics().density);
    }
}
