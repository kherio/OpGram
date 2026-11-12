package com.my.televip.settings.controller;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import com.my.televip.Configs.ConfigPreferences;
import com.my.televip.language.Keys;
import com.my.televip.language.Translator;
import com.my.televip.settings.ui.SettingsActivity;
import com.my.televip.logging.Logger;
import com.my.televip.utils.Utils;
import com.my.televip.virtuals.ActionBar.AlertDialog;
import com.my.televip.virtuals.messenger.browser.Browser;
import com.my.televip.virtuals.ui.LaunchActivity;

public class SettingsController {

    public FrameLayout settingsView;

    public SettingsActivity settingsActivity;

    public SettingsController() {}

    public void openView(){
        try {
            if (settingsView == null) {
                settingsView = new FrameLayout(getContext());
            }

            settingsView.removeAllViews();
            settingsActivity = new SettingsActivity(getContext());
            showJoinTeleVip();

            settingsView.addView(settingsActivity.createView(this));

            show(settingsView);
            registerBackCallback();
        } catch (Throwable e) {
            Logger.e(e);
        }
    }

    private void showJoinTeleVip() {
        try {
            if (!ConfigPreferences.getBoolean("JTV")) {
                AlertDialog alertDialog = new AlertDialog(getContext());

                alertDialog.setTitle(com.my.televip.Branding.title());
                alertDialog.setMessage(Translator.get(Keys.JoinTeleVip));

                alertDialog.setPositiveButton(Translator.get(Keys.Join), AlertDialog.click(() -> {
                    Browser.openUrl(getContext(), com.my.televip.Branding.RELEASES_URL);
                    hide();
                }));

                alertDialog.setNegativeButton(Translator.get(Keys.Cancel), null);
                alertDialog.setNeutralButton(Translator.get(Keys.DontShowAgain), AlertDialog.click(() -> ConfigPreferences.putBoolean("JTV", true)));
                alertDialog.show();
            }
        } catch (Throwable e) {
            Logger.e(e);
        }
    }

    public void show(View target) {
        LaunchActivity launchActivity = new LaunchActivity(Utils.getCurrentActivity());
        if (target.getParent() == null) {
            launchActivity.frameLayout.addView(target);
        }

        for (int i = 0; i < launchActivity.frameLayout.getChildCount(); i++) {
            View child = launchActivity.frameLayout.getChildAt(i);
            child.setVisibility(child == target ? View.VISIBLE : View.GONE);
        }

        target.bringToFront();
    }

    /** Rebuilds the settings list after a search/collapse change. */
    public void refreshList() {
        try {
            if (settingsActivity != null && settingsActivity.listView != null) {
                settingsActivity.listView.getAdapter().notifyDataSetChanged();
                settingsActivity.refreshTabs();
            }
        } catch (Throwable e) {
            Logger.e(e);
        }
    }

    private Object backCallback;
    private android.app.Activity backActivity;

    /**
     * Gesture / button back on Android 13+ goes through OnBackInvokedDispatcher, and Telegram's own
     * callback closes the app because the settings page is not one of its fragments. A higher-priority
     * callback makes back return from a section to the section list, then close settings.
     */
    private void registerBackCallback() {
        try {
            unregisterBackCallback();
            if (android.os.Build.VERSION.SDK_INT < 33) return;
            final android.app.Activity act = Utils.getCurrentActivity();
            if (act == null) return;
            android.window.OnBackInvokedCallback cb = () -> {
                try {
                    if (settingsActivity != null && settingsActivity.handleBack()) return;
                    hide();
                    settingsView = null;
                } catch (Throwable t) {
                    Logger.e(t);
                }
            };
            act.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_OVERLAY, cb);
            backCallback = cb;
            backActivity = act;
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    private void unregisterBackCallback() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 33 && backCallback != null && backActivity != null) {
                backActivity.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(
                        (android.window.OnBackInvokedCallback) backCallback);
            }
        } catch (Throwable ignored) {
        }
        backCallback = null;
        backActivity = null;
    }

    public void hide() {
        unregisterBackCallback();
        LaunchActivity launchActivity = new LaunchActivity(settingsView.getContext());
        for (int i = 0; i < launchActivity.frameLayout.getChildCount(); i++) {
            View child = launchActivity.frameLayout.getChildAt(i);
            child.setVisibility(child == settingsView ? View.GONE : View.VISIBLE);
        }

        if (settingsView != null && settingsView.getParent() != null) {
            launchActivity.frameLayout.removeView(settingsView);
        }
        SettingsActivity.isSettings = false;
    }

    public Context getContext() {
        return Utils.getCurrentActivity();
    }

}
