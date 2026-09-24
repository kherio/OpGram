package com.my.televip.features.extra;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.Configs.ConfigManager;
import com.my.televip.base.BaseMethodHook;
import com.my.televip.hooks.HMethod;
import com.my.televip.logging.Logger;

import de.robv.android.xposed.XposedHelpers;

/**
 * Applies a default playback speed to voice notes and round video messages.
 * MediaController lives in org.telegram.messenger (not obfuscated), so playMessage
 * and setPlaybackSpeed keep their names across builds.
 */
public class VoicePlaybackSpeed {

    public static boolean isEnable = false;

    public static void init() {
        try {
            if (isEnable) return;
            isEnable = true;

            final Class<?> mediaController = ClassLoad.getClass(ClassNames.MEDIA_CONTROLLER);
            if (mediaController == null) return;

            HMethod.hookMethod(mediaController, "playMessage", ClassLoad.getClass(ClassNames.MESSAGE_OBJECT), new BaseMethodHook() {
                @Override
                protected void afterMethod(MethodHookParam param) {
                    applyIfVoice(param.thisObject, param.args[0]);
                }
            });
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    private static void applyIfVoice(Object controller, Object messageObject) {
        try {
            if (ConfigManager.voicePlaybackSpeed == null || !ConfigManager.voicePlaybackSpeed.isEnable()) return;
            if (messageObject == null || controller == null) return;

            boolean isVoice = (boolean) XposedHelpers.callMethod(messageObject, "isVoice");
            boolean isRound = (boolean) XposedHelpers.callMethod(messageObject, "isRoundVideo");
            if (!isVoice && !isRound) return;

            float speed = ConfigManager.voicePlaybackSpeed.getFloatValue(1.5f);
            // setPlaybackSpeed(boolean isMusic, float speed); false = voice/video path
            XposedHelpers.callMethod(controller, "setPlaybackSpeed", false, speed);
        } catch (Throwable t) {
            Logger.e(t);
        }
    }
}
