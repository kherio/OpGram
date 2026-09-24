package com.my.televip.settings.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.my.televip.Database.MessageDatabase;
import com.my.televip.language.Keys;
import com.my.televip.language.Translator;
import com.my.televip.logging.Logger;

import java.util.Date;
import java.util.List;

/**
 * Lightweight viewer for the locally-saved edited-message history, with a search box.
 * Built with plain Android views so it does not depend on Telegram internals.
 */
public class EditsViewer {

    public static void show(final Activity activity) {
        try {
            final Context ctx = activity;
            LinearLayout root = new LinearLayout(ctx);
            root.setOrientation(LinearLayout.VERTICAL);
            int pad = dp(ctx, 16);
            root.setPadding(pad, pad, pad, 0);

            final EditText search = new EditText(ctx);
            search.setHint(Translator.get(Keys.Search));
            search.setSingleLine(true);
            root.addView(search);

            final ScrollView scroll = new ScrollView(ctx);
            final LinearLayout list = new LinearLayout(ctx);
            list.setOrientation(LinearLayout.VERTICAL);
            scroll.addView(list);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(ctx, 380));
            lp.topMargin = dp(ctx, 8);
            root.addView(scroll, lp);

            final MessageDatabase db = new MessageDatabase(ctx);
            render(ctx, list, db, null);

            search.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                public void onTextChanged(CharSequence s, int a, int b, int c) {}
                public void afterTextChanged(Editable e) {
                    render(ctx, list, db, e.toString().trim());
                }
            });

            new AlertDialog.Builder(activity)
                    .setTitle(Translator.get(Keys.EditsHistory))
                    .setView(root)
                    .setPositiveButton(Translator.get(Keys.Done), null)
                    .show();
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    private static void render(Context ctx, LinearLayout list, MessageDatabase db, String filter) {
        list.removeAllViews();
        List<String[]> rows = db.queryRecent(filter, 200);
        if (rows.isEmpty()) {
            TextView empty = new TextView(ctx);
            empty.setText(Translator.get(Keys.NoEditsSaved));
            empty.setPadding(0, dp(ctx, 24), 0, dp(ctx, 24));
            empty.setGravity(Gravity.CENTER);
            list.addView(empty);
            return;
        }
        for (String[] r : rows) {
            LinearLayout item = new LinearLayout(ctx);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(0, dp(ctx, 8), 0, dp(ctx, 8));

            TextView meta = new TextView(ctx);
            long date = safeLong(r[1]);
            String when = date > 0 ? DateFormat.format("dd/MM/yyyy HH:mm:ss", new Date(date)).toString() : "";
            String version = Translator.get(Keys.Version) + " " + r[2];
            meta.setText(when + "  ·  " + version);
            meta.setTextColor(Color.GRAY);
            meta.setTextSize(12);

            TextView body = new TextView(ctx);
            body.setText(r[0]);
            body.setTextSize(15);
            body.setTextIsSelectable(true);

            item.addView(meta);
            item.addView(body);
            list.addView(item);

            View sep = new View(ctx);
            sep.setBackgroundColor(0x22808080);
            list.addView(sep, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(ctx, 1)));
        }
    }

    private static long safeLong(String s) {
        try { return Long.parseLong(s); } catch (Throwable t) { return 0; }
    }

    private static int dp(Context ctx, float v) {
        return Math.round(v * ctx.getResources().getDisplayMetrics().density);
    }
}
