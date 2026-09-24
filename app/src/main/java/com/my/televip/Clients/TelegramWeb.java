package com.my.televip.Clients;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Parameter overrides for official Telegram Web (org.telegram.messenger.web) 12.10.x,
 * which is R8-optimized: some methods changed their parameter lists.
 */
public class TelegramWeb {

    public static class ParameterResolver {
        static Map<String, Class<?>[]> objectList = new HashMap<>();

        public static void register(String name, Class<?>[] classes) {
            objectList.put(name, classes);
        }

        public static Class<?>[] resolve(String name) {
            return objectList.get(name);
        }

        public static boolean has(String name) {
            return objectList.get(name) != null;
        }
    }

    public static void loadParameter() {
        Class<?> chatActivity = ClassLoad.getClass(ClassNames.CHAT_ACTIVITY);
        Class<?> messageObject = ClassLoad.getClass(ClassNames.MESSAGE_OBJECT);
        Class<?> settingsActivity = ClassLoad.getClass("org.telegram.ui.SettingsActivity");
        Class<?> uItem = ClassLoad.getClass(ClassNames.UITEM);

        // updatePinnedMessageView(boolean, int) -> yc(int, boolean)
        ParameterResolver.register("updatePinnedMessageView", new Class[]{int.class, boolean.class});
        // sendSecretMediaDelete(MessageObject) -> static O4(ChatActivity, MessageObject)
        if (chatActivity != null && messageObject != null)
            ParameterResolver.register("sendSecretMediaDelete", new Class[]{chatActivity, messageObject});
        // SettingsActivity.fillItems(ArrayList, UniversalAdapter) -> static b0(SettingsActivity, ArrayList)
        if (settingsActivity != null)
            ParameterResolver.register("fillItems", new Class[]{settingsActivity, ArrayList.class});
        // SettingsActivity.onClick(UItem, View, int, float, float) -> static f0(SettingsActivity, UItem)
        if (settingsActivity != null && uItem != null)
            ParameterResolver.register("onClick", new Class[]{settingsActivity, uItem});
    }
}
