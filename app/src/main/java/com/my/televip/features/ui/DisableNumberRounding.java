package com.my.televip.features.ui;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.Configs.ConfigManager;
import com.my.televip.base.BaseMethodHook;
import com.my.televip.hooks.HMethod;
import com.my.televip.logging.Logger;
import com.my.televip.obfuscate.ArgsResolver;
import com.my.televip.obfuscate.Obfuscate;

public class DisableNumberRounding {

    public static boolean isEnable = false;

    public static void init() {
        try {
            if (!isEnable) {
                isEnable = true;

                if (ClassLoad.getClass(ClassNames.LOCALE_CONTROLLER) != null) {
                    HMethod.hookMethod(ClassLoad.getClass(ClassNames.LOCALE_CONTROLLER), Obfuscate.getMethodName("LocaleController", "formatShortNumber"), ArgsResolver.merge("formatShortNumber", new Class[]{int.class, int[].class},
                            new BaseMethodHook() {
                                @Override
                                protected void beforeMethod(MethodHookParam param) {
                                    if (ConfigManager.disableNumberRounding.isEnable()) {
                                        // argument order differs between builds: (int, int[]) or (int[], int)
                                        boolean arrFirst = param.args[0] instanceof int[];
                                        int[] rounded = (int[]) param.args[arrFirst ? 0 : 1];
                                        int number = (int) param.args[arrFirst ? 1 : 0];
                                        if (rounded != null) {
                                            rounded[0] = number;
                                        }
                                        param.setResult(String.valueOf(number));
                                    }
                                }
                            }));
                }
            }
        } catch (Throwable t) {
            Logger.e(t);
        }
    }
}
