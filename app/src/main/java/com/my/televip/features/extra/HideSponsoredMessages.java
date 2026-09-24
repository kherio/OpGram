package com.my.televip.features.extra;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.Configs.ConfigManager;
import com.my.televip.base.BaseMethodHook;
import com.my.televip.hooks.HMethod;
import com.my.televip.logging.Logger;
import com.my.televip.obfuscate.ArgsResolver;
import com.my.televip.obfuscate.Obfuscate;

/** Hides sponsored messages (ads) shown at the bottom of public channels. */
public class HideSponsoredMessages {

    public static boolean isEnable = false;

    private static boolean on() {
        return ConfigManager.hideSponsoredMessages != null && ConfigManager.hideSponsoredMessages.isEnable();
    }

    public static void init() {
        try {
            if (isEnable) return;
            isEnable = true;
            Class<?> mc = ClassLoad.getClass(ClassNames.MESSAGES_CONTROLLER);
            if (mc == null) return;

            HMethod.hookMethod(mc, Obfuscate.getMethodName("MessagesController", "getSponsoredMessages"),
                    ArgsResolver.merge("getSponsoredMessages", new Class[]{long.class}, new BaseMethodHook() {
                        @Override
                        protected void beforeMethod(MethodHookParam param) {
                            if (on()) param.setResult(null);
                        }
                    }));

            HMethod.hookMethod(mc, Obfuscate.getMethodName("MessagesController", "isSponsoredDisabled"), new BaseMethodHook() {
                @Override
                protected void beforeMethod(MethodHookParam param) {
                    if (on()) param.setResult(true);
                }
            });
        } catch (Throwable t) {
            Logger.e(t);
        }
    }
}
