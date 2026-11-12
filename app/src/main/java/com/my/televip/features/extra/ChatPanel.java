package com.my.televip.features.extra;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.my.televip.Configs.ConfigManager;
import com.my.televip.features.ghostMode.GhostExceptions;
import com.my.televip.language.Keys;
import com.my.televip.language.Translator;
import com.my.televip.logging.Logger;
import com.my.televip.ui.ThemeColors;
import com.my.televip.utils.Utils;

/** Single per-chat OpGram panel (bottom sheet): switches for this chat + quick actions. */
public class ChatPanel {

    private interface Toggle { boolean get(); void set(boolean on); }

    private static int dp(Activity a, int v) {
        return Math.round(v * a.getResources().getDisplayMetrics().density);
    }

    private static boolean on(com.my.televip.Configs.ConfigItem i) {
        return i != null && i.isEnable();
    }

    /** True if at least one row would be shown. */
    public static boolean hasContent(long did) {
        return ConfigManager.isGhostMode() || on(ConfigManager.chatLock) || on(ConfigManager.secureChats)
                || (on(ConfigManager.totalDelete) && did > 0) || on(ConfigManager.reminders) || on(ConfigManager.batchDownload);
    }

    public static void show(final long did) {
        try {
            final Activity act = Utils.getCurrentActivity();
            if (act == null || did == 0) return;
            final boolean light = com.my.televip.virtuals.ActionBar.Theme.isLight();
            final int bg = ThemeColors.getBackgroundWhiteOrBlueColor();
            final int text = ThemeColors.getTextColor();
            final int gray = ThemeColors.getTextGrayColor();
            final int accent = ThemeColors.getTextBlueColor();
            final int line = light ? 0x14000000 : 0x1FFFFFFF;

            final Dialog dialog = new Dialog(act);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            LinearLayout root = new LinearLayout(act);
            root.setOrientation(LinearLayout.VERTICAL);
            GradientDrawable shape = new GradientDrawable();
            shape.setColor(bg);
            float r = dp(act, 20);
            shape.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
            root.setBackground(shape);
            root.setPadding(0, dp(act, 8), 0, dp(act, 16));

            View handle = new View(act);
            GradientDrawable hd = new GradientDrawable();
            hd.setColor(line == 0 ? gray : (light ? 0x33000000 : 0x55FFFFFF));
            hd.setCornerRadius(dp(act, 3));
            handle.setBackground(hd);
            LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(dp(act, 36), dp(act, 4));
            hp.gravity = Gravity.CENTER_HORIZONTAL;
            hp.bottomMargin = dp(act, 8);
            root.addView(handle, hp);

            TextView title = new TextView(act);
            title.setText(Translator.get(Keys.OpGramInChat));
            title.setTextColor(text);
            title.setTextSize(17);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            title.setPadding(dp(act, 20), dp(act, 6), dp(act, 20), dp(act, 10));
            root.addView(title);

            boolean any = false;

            if (ConfigManager.isGhostMode()) {
                any = true;
                addSwitch(act, root, dialog, "ghost", Keys.HideSeenHere, text, accent, new Toggle() {
                    public boolean get() { return !GhostExceptions.isExcluded(did); }
                    public void set(boolean on) { if (on != get()) GhostExceptions.toggle(did); }
                });
            }
            if (on(ConfigManager.chatLock)) {
                any = true;
                addSwitch(act, root, dialog, "msg_secret", Keys.BlockThisChat, text, accent, new Toggle() {
                    public boolean get() { return ChatLock.isLocked(did); }
                    public void set(boolean on) { if (on != get()) ChatLock.toggle(did); }
                });
            }
            if (on(ConfigManager.secureChats)) {
                any = true;
                addSwitch(act, root, dialog, "msg_secret", Keys.BlockScreenshotsHere, text, accent, new Toggle() {
                    public boolean get() { return SecureScreen.isSecure(did); }
                    public void set(boolean on) { if (on != get()) SecureScreen.toggle(did); }
                });
            }
            if (on(ConfigManager.totalDelete) && did > 0) {
                any = true;
                addSwitch(act, root, dialog, "msg_delete", Keys.TotalDeleteShort, text, accent, new Toggle() {
                    public boolean get() { return TotalDelete.isMarked(did); }
                    public void set(boolean on) { if (on != get()) TotalDelete.toggle(did); }
                });
            }

            boolean hasActions = ConfigManager.isGhostMode() || on(ConfigManager.reminders) || on(ConfigManager.batchDownload);
            if (hasActions) {
                if (any) {
                    View div = new View(act);
                    div.setBackgroundColor(line);
                    LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
                    dlp.topMargin = dp(act, 6);
                    dlp.bottomMargin = dp(act, 6);
                    root.addView(div, dlp);
                }
                TextView sub = new TextView(act);
                sub.setText(Translator.get(Keys.ChatActions));
                sub.setTextColor(accent);
                sub.setTextSize(13);
                sub.setTypeface(Typeface.DEFAULT_BOLD);
                sub.setPadding(dp(act, 20), dp(act, 8), dp(act, 20), dp(act, 4));
                root.addView(sub);

                if (ConfigManager.isGhostMode()) {
                    addAction(act, root, dialog, "msg_markread", Keys.MarkAsReadNow, text, accent,
                            () -> GhostExceptions.markAsReadNow(did));
                }
                if (on(ConfigManager.reminders)) {
                    addAction(act, root, dialog, "msg_calendar2", Keys.RemindMe, text, accent,
                            () -> Reminders.offer(did, Translator.get(Keys.RemindMe)));
                }
                if (on(ConfigManager.batchDownload)) {
                    addAction(act, root, dialog, "msg_download", Keys.BatchDownload, text, accent,
                            () -> BatchDownload.offer(did));
                }
            }

            dialog.setContentView(root);
            Window w = dialog.getWindow();
            if (w != null) {
                w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
                w.setGravity(Gravity.BOTTOM);
                w.setDimAmount(0.45f);
            }
            dialog.show();
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    private static Drawable icon(Activity act, String name, int tint) {
        try {
            int id = Utils.drawableId(name);
            if (id == 0) return null;
            Drawable d = act.getResources().getDrawable(id, act.getTheme()).mutate();
            d.setTint(tint);
            return d;
        } catch (Throwable t) {
            return null;
        }
    }

    private static LinearLayout row(Activity act, String iconName, String labelKey, int text, int accent) {
        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(act, 20), 0, dp(act, 20), 0);
        row.setMinimumHeight(dp(act, 52));
        android.util.TypedValue tv = new android.util.TypedValue();
        if (act.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true)) {
            row.setBackgroundResource(tv.resourceId);
        }
        ImageView iv = new ImageView(act);
        Drawable d = icon(act, iconName, accent);
        if (d != null) iv.setImageDrawable(d);
        row.addView(iv, new LinearLayout.LayoutParams(dp(act, 24), dp(act, 24)));
        TextView tx = new TextView(act);
        tx.setText(Translator.get(labelKey));
        tx.setTextColor(text);
        tx.setTextSize(16);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.leftMargin = dp(act, 16);
        row.addView(tx, lp);
        return row;
    }

    private static void addSwitch(Activity act, LinearLayout root, Dialog dialog, String iconName, String labelKey,
                                  int text, int accent, final Toggle toggle) {
        final LinearLayout row = row(act, iconName, labelKey, text, accent);
        final Switch sw = new Switch(act);
        sw.setChecked(toggle.get());
        sw.setClickable(false);
        sw.setFocusable(false);
        int[][] states = {{android.R.attr.state_checked}, {}};
        sw.setThumbTintList(new ColorStateList(states, new int[]{accent, 0xFF9AA5B1}));
        sw.setTrackTintList(new ColorStateList(states, new int[]{(accent & 0x00FFFFFF) | 0x66000000, 0x33808890}));
        row.addView(sw);
        row.setOnClickListener(v -> {
            try {
                toggle.set(!toggle.get());
                sw.setChecked(toggle.get());
            } catch (Throwable t) {
                Logger.e(t);
            }
        });
        root.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private static void addAction(Activity act, LinearLayout root, final Dialog dialog, String iconName, String labelKey,
                                  int text, int accent, final Runnable action) {
        LinearLayout row = row(act, iconName, labelKey, text, accent);
        row.setOnClickListener(v -> {
            dialog.dismiss();
            try { action.run(); } catch (Throwable t) { Logger.e(t); }
        });
        root.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }
}
