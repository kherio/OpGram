package com.my.televip;

import static com.my.televip.obfuscate.ArgsResolver.resolverRegistry;

import com.my.televip.Configs.ConfigManager;
import com.my.televip.application.AndroidUtilities;
import com.my.televip.dex.DexInjector;
import com.my.televip.language.Translator;
import com.my.televip.logging.Logger;
import com.my.televip.settings.SettingsManager;
import com.my.televip.settings.controller.SettingsController;
import com.my.televip.utils.Utils;
import com.my.televip.virtuals.TeleVip.Bridge.Bridge;

public class TeleVip {
    
    /** Each start-up step is isolated: one failing step must not prevent the settings entry from appearing. */
    private static void step(String name, Runnable r) {
        try {
            r.run();
        } catch (Throwable e) {
            Logger.w("OpGram startup step failed: " + name);
            Logger.e(e);
        }
    }

    public static void startHook() {
        step("params", () -> resolverRegistry.loadParameter());
        step("translator", () -> Translator.init());
        step("androidUtilities", () -> AndroidUtilities.init());
        step("dex", () -> DexInjector.injectDex(Utils.classLoader));

        final SettingsController settingsController = new SettingsController();

        step("bridge", () -> Bridge.init(settingsController));
        step("config", () -> ConfigManager.loadAndRead());
        step("settings", () -> SettingsManager.init(settingsController));
        step("versionGuard", () -> com.my.televip.features.extra.VersionGuard.check());
        step("updater", () -> com.my.televip.features.extra.Updater.checkAsync(false));
    }
}
