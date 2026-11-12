package com.my.televip.features.extra;

import android.app.Activity;
import android.widget.Toast;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.Configs.ConfigManager;
import com.my.televip.Configs.ConfigPreferences;
import com.my.televip.Database.MessageDatabase;
import com.my.televip.base.BaseMethodHook;
import com.my.televip.hooks.HMethod;
import com.my.televip.language.Keys;
import com.my.televip.language.Translator;
import com.my.televip.logging.Logger;
import com.my.televip.obfuscate.ArgsResolver;
import com.my.televip.obfuscate.Obfuscate;
import com.my.televip.utils.Utils;

import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.XposedHelpers;

/**
 * "Total delete" for private chats marked from the chat menu: whenever Telegram sends the
 * request that deletes the history of that chat, it is forced to revoke (delete for both sides)
 * regardless of the checkbox, and OpGram's own saved copies for that chat are purged.
 * Hooks the request itself, so it does not depend on obfuscated Telegram classes.
 */
public class TotalDelete {

    public static boolean isEnable = false;
    private static final String PREF = "TotalDeleteChats";
    private static Set<Long> chats;

    private static synchronized Set<Long> set() {
        if (chats == null) {
            chats = new HashSet<>();
            String raw = ConfigPreferences.getString(PREF);
            if (raw != null && !raw.isEmpty()) {
                for (String s : raw.split(",")) {
                    try { chats.add(Long.parseLong(s.trim())); } catch (Throwable ignored) {}
                }
            }
        }
        return chats;
    }

    public static boolean isMarked(long dialogId) {
        return dialogId != 0 && set().contains(dialogId);
    }

    public static void toggle(long dialogId) {
        if (dialogId <= 0) return; // private chats only
        boolean now;
        synchronized (TotalDelete.class) {
            if (set().contains(dialogId)) { set().remove(dialogId); now = false; }
            else { set().add(dialogId); now = true; }
            StringBuilder sb = new StringBuilder();
            for (Long id : set()) { if (sb.length() > 0) sb.append(','); sb.append(id); }
            ConfigPreferences.putString(PREF, sb.toString());
        }
        toast(Translator.get(now ? Keys.TotalDeleteOn : Keys.TotalDeleteOff));
    }

    private static void toast(final String text) {
        final Activity a = Utils.getCurrentActivity();
        if (a == null) return;
        a.runOnUiThread(() -> Toast.makeText(a, text, Toast.LENGTH_LONG).show());
    }

    public static void init() {
        try {
            if (isEnable) return;
            isEnable = true;
            Class<?> cm = ClassLoad.getClass(ClassNames.CONNECTIONS_MANAGER);
            if (cm == null) return;
            HMethod.hookMethod(cm, Obfuscate.getMethodName("ConnectionsManager", "sendRequestInternal"),
                    ArgsResolver.merge("sendRequestInternal", new Class[]{ClassLoad.getClass(ClassNames.TL_OBJECT), ClassLoad.getClass(ClassNames.REQUEST_DELEGATE), ClassLoad.getClass(ClassNames.REQUEST_DELEGATE_TIMESTAMP), ClassLoad.getClass(ClassNames.QUICK_ACK_DELEGATE), ClassLoad.getClass(ClassNames.WRITE_TO_SOCKET_DELEGATE), int.class, int.class, int.class, boolean.class, int.class}, new BaseMethodHook() {
                        @Override
                        protected void beforeMethod(MethodHookParam param) {
                            try {
                                if (ConfigManager.totalDelete == null || !ConfigManager.totalDelete.isEnable()) return;
                                Object req = param.args[0];
                                if (req == null || !req.getClass().getName().endsWith("TL_messages_deleteHistory")) return;
                                Object peer = XposedHelpers.getObjectField(req, Obfuscate.getFieldName("TL_messages_deleteHistory", "peer"));
                                if (peer == null || !peer.getClass().getName().contains("inputPeerUser")) return;
                                final long uid = XposedHelpers.getLongField(peer, Obfuscate.getFieldName("TL_inputPeerUser", "user_id"));
                                if (!isMarked(uid)) return;

                                XposedHelpers.setBooleanField(req, Obfuscate.getFieldName("TL_messages_deleteHistory", "revoke"), true);
                                try {
                                    String ff = Obfuscate.getFieldName("TL_messages_deleteHistory", "flags");
                                    XposedHelpers.setIntField(req, ff, XposedHelpers.getIntField(req, ff) | 2);
                                } catch (Throwable ignored) {
                                }

                                final Activity a = Utils.getCurrentActivity();
                                if (a != null) {
                                    new Thread(() -> {
                                        try {
                                            new MessageDatabase(a).deleteByUser(uid);
                                        } catch (Throwable t) {
                                            Logger.e(t);
                                        }
                                    }).start();
                                }
                                toast(Translator.get(Keys.TotalDeleteSent));
                            } catch (Throwable t) {
                                Logger.e(t);
                            }
                        }
                    }));
        } catch (Throwable t) {
            Logger.e(t);
        }
    }
}
