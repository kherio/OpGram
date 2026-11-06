package com.my.televip.settings;

import com.my.televip.Clients.ClientManager;
import com.my.televip.Clients.Turrit;
import com.my.televip.obfuscate.Obfuscate;
import com.my.televip.settings.controller.SettingsController;
import com.my.televip.settings.hook.SettingsHook;
import com.my.televip.settings.ui.SettingsActivity;
import com.my.televip.utils.Utils;

import de.robv.android.xposed.XposedHelpers;

public class SettingsManager {

    /** If the settings entry cannot be installed, say so on screen instead of failing silently. */
    private static void diagnose(Class<?> settingsActivity, Class<?> factory) {
        try {
            if (!ClientManager.is(ClientManager.Client.TelegramWeb)) return;
            String problem = null;
            if (settingsActivity == null || factory == null) {
                problem = "class not found (" + (settingsActivity == null ? "SettingsActivity " : "") + (factory == null ? "Factory" : "") + ")";
            } else {
                Class<?>[] types = com.my.televip.obfuscate.ArgsResolver.resolveObject("fillItems",
                        new Class[]{java.util.ArrayList.class, com.my.televip.Class.ClassLoad.getClass(com.my.televip.Class.ClassNames.UNIVERSAL_ADAPTER)});
                String name = Obfuscate.getMethodName("SettingsActivity", "fillItems");
                if (XposedHelpers.findMethodExactIfExists(settingsActivity, name, (Object[]) types) == null) {
                    problem = "fillItems not found (" + name + ")";
                }
            }
            if (!Utils.versionedMapping) problem = (problem == null ? "" : problem + "; ") + "no mapping for versionCode " + Utils.versionCode;
            if (problem == null) return;
            final String msg = "OpGram " + com.my.televip.Branding.VERSION + ": " + problem;
            com.my.televip.logging.Logger.w(msg);
            final android.app.Activity a = Utils.getCurrentActivity();
            if (a != null) a.runOnUiThread(() -> android.widget.Toast.makeText(a, msg, android.widget.Toast.LENGTH_LONG).show());
        } catch (Throwable t) {
            com.my.televip.logging.Logger.e(t);
        }
    }

    public static void init(SettingsController settingsController) {

        SettingsActivity.init(settingsController);

        Class<?> SettingsActivityClass = XposedHelpers.findClassIfExists(
                Obfuscate.getClassName("org.telegram.ui.SettingsActivity"),
                Utils.classLoader
        );

        Class<?> SettingsActivity$SettingCell$FactoryClass = XposedHelpers.findClassIfExists(
                Obfuscate.getClassName("org.telegram.ui.SettingsActivity$SettingCell$Factory"),
                Utils.classLoader
        );

        SettingsHook settings = new SettingsHook();
        if (SettingsActivityClass != null && SettingsActivity$SettingCell$FactoryClass != null) {
            settings.newSettings(SettingsActivityClass, SettingsActivity$SettingCell$FactoryClass, settingsController);
        } else {
            settings.oldSettings(settingsController);
        }
        if (ClientManager.is(ClientManager.Client.ForkgramClassic)) settings.oldSettings(settingsController);
        diagnose(SettingsActivityClass, SettingsActivity$SettingCell$FactoryClass);
        if (ClientManager.is(ClientManager.Client.Turrit)) Turrit.showGhostModeDialog(settingsController);
    }

}
