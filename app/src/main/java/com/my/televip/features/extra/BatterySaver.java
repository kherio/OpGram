package com.my.televip.features.extra;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.Configs.ConfigManager;
import com.my.televip.base.BaseMethodHook;
import com.my.televip.hooks.HMethod;
import com.my.televip.logging.Logger;

/**
 * Battery / data savers built on non-obfuscated messenger classes:
 *  - stop preloading stories in the background (DownloadController.canPreloadStories)
 *  - hide the stories bar in the chat list (MessagesController.storiesEnabled)
 * Both method names are stable because these classes are not obfuscated.
 */
public class BatterySaver {

    public static boolean isEnable = false;

    private static boolean noStoriesPreload() {
        return ConfigManager.noStoriesPreload != null && ConfigManager.noStoriesPreload.isEnable();
    }

    private static boolean hideStoriesBar() {
        return ConfigManager.hideStoriesBar != null && ConfigManager.hideStoriesBar.isEnable();
    }

    public static void init() {
        try {
            if (isEnable) return;
            isEnable = true;

            Class<?> dc = ClassLoad.getClass(ClassNames.DOWNLOAD_CONTROLLER);
            if (dc != null) {
                HMethod.hookMethod(dc, "canPreloadStories", new BaseMethodHook() {
                    @Override
                    protected void beforeMethod(MethodHookParam param) {
                        if (noStoriesPreload()) param.setResult(false);
                    }
                });
            }

            Class<?> mc = ClassLoad.getClass(ClassNames.MESSAGES_CONTROLLER);
            if (mc != null) {
                HMethod.hookMethod(mc, "storiesEnabled", new BaseMethodHook() {
                    @Override
                    protected void beforeMethod(MethodHookParam param) {
                        if (hideStoriesBar()) param.setResult(false);
                    }
                });
            }
        } catch (Throwable t) {
            Logger.e(t);
        }
    }
}
