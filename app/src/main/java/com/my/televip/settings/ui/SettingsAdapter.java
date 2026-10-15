package com.my.televip.settings.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.my.televip.Configs.ConfigItem;
import com.my.televip.Configs.ConfigManager;
import com.my.televip.Audio;
import com.my.televip.language.Keys;
import com.my.televip.language.Translator;
import com.my.televip.logging.Logger;
import com.my.televip.settings.controller.SettingsController;
import com.my.televip.ui.ThemeColors;
import com.my.televip.utils.DialogUtils;
import com.my.televip.utils.Utils;
import com.my.televip.virtuals.androidx.ViewHolder;
import com.my.televip.virtuals.messenger.browser.Browser;
import com.my.televip.ui.Cells.ExpandableTextCheckCell;
import com.my.televip.virtuals.ui.Cells.HeaderCell;
import com.my.televip.virtuals.ui.Cells.TextCheckCell;
import com.my.televip.ui.Cells.TextInfoCell;
import com.my.televip.virtuals.ui.Cells.TextSettingsCell;

import de.robv.android.xposed.XposedHelpers;

public class SettingsAdapter {

    private static boolean isLongText = false;

    private static final String[] ACCENT_KEYS = {Keys.AccentDefault, Keys.AccentBlue, Keys.AccentGreen,
            Keys.AccentOrange, Keys.AccentPink, Keys.AccentPurple, Keys.AccentCyan};

    /** Recolours the title text of a header cell with the chosen accent (no-op for "default"). */
    private static void tintAccent(View v) {
        int accent = ThemeColors.ACCENTS[ThemeColors.getAccentIndex()];
        if (accent == 0 || v == null) return;
        try {
            if (v instanceof TextView) { ((TextView) v).setTextColor(accent); return; }
            if (v instanceof android.view.ViewGroup) {
                android.view.ViewGroup g = (android.view.ViewGroup) v;
                for (int i = 0; i < g.getChildCount(); i++) {
                    View c = g.getChildAt(i);
                    if (c instanceof TextView) ((TextView) c).setTextColor(accent);
                    else XposedHelpers.callMethod(c, "setTextColor", accent);
                }
            }
        } catch (Throwable ignored) { }
    }

    public static int getRow(int position) { return ConfigManager.getItems().get(position).getType(); }

    public static int getRowCount() {
        return ConfigManager.getItems().size();
    }

    public static void onBindViewHolder(Object holder, SettingsController settingsController, int position, int viewType) {
        try {
            ConfigItem item = ConfigManager.getItems().get(position);

            switch (viewType) {
                case ConfigItem.HEADER:
                    HeaderCellHolder headerCell = new HeaderCellHolder(holder);
                    String hkey = item.getKey();
                    String arrow = ConfigManager.getSearchQuery().isEmpty()
                            ? (ConfigManager.isCollapsed(hkey) ? "  ▸" : "  ▾") : "";
                    headerCell.cell.setText(Translator.get(hkey) + arrow);
                    tintAccent(headerCell.cell.getView());
                    if (ConfigManager.getSearchQuery().isEmpty()) {
                        headerCell.cell.getView().setOnClickListener(v -> {
                            ConfigManager.toggleCollapseByKey(hkey);
                            settingsController.refreshList();
                        });
                        if (ConfigManager.isResettable(hkey)) {
                            headerCell.cell.getView().setOnLongClickListener(v -> {
                                confirmReset(settingsController, hkey);
                                return true;
                            });
                        } else {
                            headerCell.cell.getView().setOnLongClickListener(null);
                        }
                    }
                    break;

                case ConfigItem.SWITCH:
                    TextCheckCellHolder textCheck = new TextCheckCellHolder(holder);
                    String subtitle = buildSubtitle(item);
                    String title = Translator.get(item.getKey());
                    if (ConfigManager.isFavorite(item.getKey())) title = "\u2605 " + title;
                    if (subtitle != null) {
                        textCheck.cell.setTextAndValueAndCheck(title, subtitle, item.isEnable(), true, false);
                    } else {
                        textCheck.cell.setTextAndCheck(title, item.isEnable(), false);
                    }
                    textCheck.cell.getTextView().setLines(0);
                    textCheck.cell.getTextView().setMaxLines(0);
                    textCheck.cell.getTextView().setSingleLine(false);
                    textCheck.cell.getTextView().setEllipsize(null);
                    break;
                case ConfigItem.EXPANDABLE_SWITCH:
                    ExpandableTextCheckCellHolder expandableTextCheck = new ExpandableTextCheckCellHolder(holder);
                    expandableTextCheck.cell.addChildren(item);

                    break;
                case ConfigItem.TEXT:
                    TextSettingsCellHolder settingsCell = new TextSettingsCellHolder(holder);
                    if (item.getKey().equals(Keys.Calendar)) {
                        String value = null;
                        switch (item.getCustomCalendar()) {
                            case 0:
                                value = Translator.get(Keys.Gregorian);
                                break;
                            case 1:
                                value = Translator.get(Keys.Hijri);
                                break;
                            case 2:
                                value = Translator.get(Keys.Persian);
                                break;
                        }
                        settingsCell.cell.setTextAndValue(Translator.get(item.getKey()), value, false, false);
                    } else if (item.getKey().equals(Keys.SettingsTheme)) {
                        settingsCell.cell.setTextAndValue(Translator.get(item.getKey()),
                                Translator.get(item.getCustomCalendar() == 1 ? Keys.ThemeAmoled : Keys.ThemeFollow), false, false);
                    } else if (item.getKey().equals(Keys.SettingsAccent)) {
                        settingsCell.cell.setTextAndValue(Translator.get(item.getKey()),
                                Translator.get(ACCENT_KEYS[Math.max(0, Math.min(item.getCustomCalendar(), ACCENT_KEYS.length - 1))]), false, false);
                    } else {
                        settingsCell.cell.setText(Translator.get(item.getKey()), false);
                        settingsCell.cell.getTextView().setTextColor(ThemeColors.getTextBlueColor());
                    }
                    break;
                case ConfigItem.DIVIDER:
                    ShadowSectionCellHolder shadowSectionCell = new ShadowSectionCellHolder(holder);
                    shadowSectionCell.cell.setBackgroundColor((ThemeColors.getBackgroundGrayColor()));
                    break;
                case ConfigItem.INFO:
                    TextInfoCellHolder textInfoCell = new TextInfoCellHolder(holder);
                    TextView textView = textInfoCell.text.getTextView();
                    if (!item.getKey().equals(Keys.OfflineVisibilityInfo)) {
                        textView.setMaxLines(Integer.MAX_VALUE);
                        textView.setEllipsize(null);
                        textView.setText(Translator.get(item.getKey()));
                        textView.setOnClickListener(null);
                    }
                    if (item.getKey().equals(Keys.OfflineVisibilityInfo)) {
                        textView.setMaxLines(2);
                        textView.setEllipsize(TextUtils.TruncateAt.END);
                        textView.setText(Translator.get(Keys.OfflineVisibilityInfo));
                        if (textView.getMaxLines() == 2) {
                            isLongText = false;
                        }
                        textView.setOnClickListener(v -> {
                            if (!isLongText) {
                                textView.setMaxLines(Integer.MAX_VALUE);
                                textView.setEllipsize(null);
                                isLongText = true;
                            } else {
                                textView.setMaxLines(2);
                                textView.setEllipsize(TextUtils.TruncateAt.END);
                                textView.setText(Translator.get(Keys.OfflineVisibilityInfo));
                                isLongText = false;
                            }
                        });
                    }
                    break;
            }
            // The header cell IS the row view: the generic listeners below would overwrite the
            // collapse / reset listeners set above (headers then ignored taps and long-press
            // played the original TeleVip audio easter egg).
            if (viewType == ConfigItem.HEADER) return;

            ViewHolder viewHolder = new ViewHolder(holder);
            if (ThemeColors.isAmoled() && viewType != ConfigItem.DIVIDER) {
                viewHolder.getItemView().setBackgroundColor(ThemeColors.getBackgroundWhiteOrBlueColor());
            }

            viewHolder.getItemView().setOnLongClickListener(v -> {
                if (viewType == ConfigItem.SWITCH) {
                    boolean now = ConfigManager.toggleFavorite(item.getKey());
                    android.widget.Toast.makeText(settingsController.getContext(),
                            Translator.get(now ? Keys.FavoriteAdded : Keys.FavoriteRemoved),
                            android.widget.Toast.LENGTH_SHORT).show();
                    settingsController.refreshList();
                    return true;
                }
                return true;
            });

            viewHolder.getItemView().setOnClickListener(v -> {

                if (viewType == ConfigItem.SWITCH) {
                    TextCheckCellHolder textCheck = new TextCheckCellHolder(holder);
                    boolean checked = !textCheck.cell.isChecked();
                    textCheck.cell.setChecked(checked);
                    item.setEnable(checked);
                    item.run();
                    if (Keys.CompactMode.equals(item.getKey())) {
                        ConfigManager.rebuildView();
                        settingsController.refreshList();
                        return;
                    }
                    if (item.isRestartRequired() && !ConfigManager.isRestartPending()) {
                        ConfigManager.setRestartPending(true);
                        settingsController.refreshList();
                    }
                } else if (viewType == ConfigItem.TEXT) {
                    switch (item.getKey()) {
                        case Keys.RestartPendingBanner:
                            restartTelegram(settingsController);
                            break;
                        case Keys.DeveloperChannel:
                            settingsController.hide();
                            Browser.openUrl(settingsController.getContext(), com.my.televip.Branding.RELEASES_URL);
                            break;
                        case Keys.RestartApp:
                            restartTelegram(settingsController);
                            break;
                        case Keys.ExportSettings:
                            BackupActions.exportSettings((Activity) settingsController.getContext());
                            break;
                        case Keys.ImportSettings:
                            BackupActions.importSettings((Activity) settingsController.getContext());
                            break;
                        case Keys.CheckForUpdates:
                            com.my.televip.features.extra.Updater.checkAsync(true);
                            break;
                        case Keys.ViewEditsHistory:
                            EditsViewer.show((Activity) settingsController.getContext());
                            break;
                        case Keys.QuickReplies:
                            com.my.televip.features.extra.QuickReplies.show((Activity) settingsController.getContext());
                            break;
                        case Keys.ClearEditsHistory:
                            BackupActions.clearEditsHistory((Activity) settingsController.getContext());
                            break;
                        case Keys.SettingsTheme: {
                            Dialog d = DialogUtils.createSingleChoiceDialog((Activity) settingsController.getContext(),
                                    new String[]{Translator.get(Keys.ThemeFollow), Translator.get(Keys.ThemeAmoled)},
                                    Translator.get(Keys.SettingsTheme), item.getCustomCalendar(), (dialog, which) -> {
                                        item.setCustomCalendar(which);
                                        settingsController.settingsActivity.applyTheme();
                                        settingsController.refreshList();
                                    });
                            d.show();
                            break;
                        }
                        case Keys.SettingsAccent: {
                            String[] names = new String[ACCENT_KEYS.length];
                            for (int i = 0; i < names.length; i++) names[i] = Translator.get(ACCENT_KEYS[i]);
                            Dialog d = DialogUtils.createSingleChoiceDialog((Activity) settingsController.getContext(),
                                    names, Translator.get(Keys.SettingsAccent), item.getCustomCalendar(), (dialog, which) -> {
                                        item.setCustomCalendar(which);
                                        settingsController.settingsActivity.applyTheme();
                                        settingsController.refreshList();
                                    });
                            d.show();
                            break;
                        }
                        case Keys.Calendar:
                            Dialog dlg = DialogUtils.createSingleChoiceDialog((Activity) settingsController.getContext(), new String[]{
                                            Translator.get(Keys.Gregorian), Translator.get(Keys.Hijri), Translator.get(Keys.Persian)},
                                    Translator.get(Keys.Calendar), item.getCustomCalendar(), (dialog, which) -> {
                                        item.setCustomCalendar(which);
                                        item.run();
                                        if (settingsController.settingsActivity.listView.getAdapter() != null) {
                                            settingsController.settingsActivity.listView.getAdapter().notifyItemChanged(position);
                                        }
                                    });
                            dlg.show();
                            break;
                    }
                }
            });

        } catch (Throwable t){
            Logger.e(t);
        }

    }

    /** Subtitle under a switch: description, example value and/or "restart required". */
    private static String buildSubtitle(ConfigItem item) {
        StringBuilder sb = new StringBuilder();
        boolean compact = ConfigManager.isCompact();
        String desc = compact ? null : Translator.getOrNull(item.getKey() + "Desc");
        if (desc != null) sb.append(desc);
        if (item.getValue() != null && !compact) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(item.getValue());
        }
        if (item.isRestartRequired() && !compact) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("\u21BB ").append(Translator.get(Keys.RestartRequired));
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private static void restartTelegram(SettingsController settingsController) {
        Intent intent = settingsController.getContext()
                .getPackageManager()
                .getLaunchIntentForPackage(Utils.pkgName);

        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            settingsController.getContext().startActivity(intent);
        }

        ((Activity) settingsController.getContext()).finishAffinity();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private static void confirmReset(SettingsController settingsController, String headerKey) {
        try {
            final Activity activity = (Activity) settingsController.getContext();
            new android.app.AlertDialog.Builder(activity)
                    .setTitle(Translator.get(Keys.ResetSection))
                    .setMessage(Translator.get(Keys.ResetSectionConfirm, Translator.get(headerKey)))
                    .setPositiveButton(Translator.get(Keys.Reset), (d, w) -> {
                        ConfigManager.resetSection(headerKey);
                        android.widget.Toast.makeText(activity, Translator.get(Keys.ResetDone),
                                android.widget.Toast.LENGTH_SHORT).show();
                        settingsController.refreshList();
                    })
                    .setNegativeButton(Translator.get(Keys.Cancel), null)
                    .show();
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    public static class HeaderCellHolder {
        HeaderCell cell;

        public HeaderCellHolder(Object obj) {
            cell = new HeaderCell(XposedHelpers.getObjectField(obj, "headerCell"));
        }
    }

    public static class TextCheckCellHolder {
        TextCheckCell cell;

        public TextCheckCellHolder(Object obj) {
            cell = new TextCheckCell(XposedHelpers.getObjectField(obj, "textCheckCell"));
        }
    }
    public static class ExpandableTextCheckCellHolder {
        ExpandableTextCheckCell cell;

        public ExpandableTextCheckCellHolder(Object obj) {
            cell = (ExpandableTextCheckCell) XposedHelpers.getObjectField(obj, "expandableTextCheckCell");
        }
    }

    public static class TextSettingsCellHolder {
        TextSettingsCell cell;

        public TextSettingsCellHolder(Object obj) {
            cell = new TextSettingsCell(XposedHelpers.getObjectField(obj, "textSettingsCell"));
        }
    }

    public static class ShadowSectionCellHolder {
        View cell;

        public ShadowSectionCellHolder(Object obj) {
            cell = (View) XposedHelpers.getObjectField(obj, "view");
        }
    }

    public static class TextInfoCellHolder {
        TextInfoCell text;

        public TextInfoCellHolder(Object obj) {
            text = (TextInfoCell) XposedHelpers.getObjectField(obj, "view");
        }
    }

    public static void playAudio(Context context) {
        if (Audio.playing) {
            Audio.stop();
        } else {
            Audio.start();
            DialogUtils.showQuranAlert(context);
        }
    }

}
