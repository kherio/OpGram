package com.my.televip.features.media;

import com.my.televip.Clients.ClientManager;

import com.my.televip.Class.ClassNames;
import com.my.televip.Configs.ConfigManager;
import com.my.televip.base.BaseMethodHook;
import com.my.televip.hooks.HMethod;
import com.my.televip.Class.ClassLoad;
import com.my.televip.obfuscate.ArgsResolver;
import com.my.televip.obfuscate.Obfuscate;
import com.my.televip.logging.Logger;
import com.my.televip.virtuals.ui.SecretMediaViewer;

import de.robv.android.xposed.XposedHelpers;

public class PreventMedia {

    public static boolean isEnable = false;

    public static void init() {
        try {
            if (!isEnable) {
                isEnable = true;

                if (ClassLoad.getClass(ClassNames.CHAT_ACTIVITY) != null) {
                    HMethod.hookMethod(ClassLoad.getClass(ClassNames.CHAT_ACTIVITY), Obfuscate.getMethodName("ChatActivity", "sendSecretMessageRead"), ArgsResolver.merge("sendSecretMessageRead", new Class[]{ClassLoad.getClass(ClassNames.MESSAGE_OBJECT), boolean.class}, new BaseMethodHook() {
                        @Override
                        protected void beforeMethod(MethodHookParam param) {
                            if (ConfigManager.preventMedia.isEnable()) param.setResult(null);
                        }
                    }));
                    HMethod.hookMethod(ClassLoad.getClass(ClassNames.CHAT_ACTIVITY), Obfuscate.getMethodName("ChatActivity", "sendSecretMediaDelete"), ArgsResolver.merge("sendSecretMediaDelete", new Class[]{ClassLoad.getClass(ClassNames.MESSAGE_OBJECT)}, new BaseMethodHook() {
                        @Override
                        protected void beforeMethod(MethodHookParam param) {
                            if (ConfigManager.preventMedia.isEnable()) param.setResult(null);
                        }
                    }));
                }

                if (ClassLoad.getClass(ClassNames.SECRET_MEDIA_VIEWER) != null) {
                    if (!ClientManager.is(ClientManager.Client.TelegramWeb)) SecretMediaViewer.openMedia();
                    HMethod.hookMethod(ClassLoad.getClass(ClassNames.SECRET_MEDIA_VIEWER), Obfuscate.getMethodName("SecretMediaViewer", "closePhoto"), ArgsResolver.merge("closePhoto", new Class[]{boolean.class, boolean.class}, new BaseMethodHook() {
                        @Override
                        protected void beforeMethod(MethodHookParam param) {
                            if (ConfigManager.preventMedia.isEnable()) {
                                Object thisObject = param.thisObject;
                                XposedHelpers.setObjectField(thisObject, Obfuscate.getFieldName("SecretMediaViewer", "onClose"), null);
                            }
                        }
                    }));
                }
            }
        } catch (Throwable t){
            Logger.e(t);
        }
    }

}
