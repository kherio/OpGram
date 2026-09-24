package com.my.televip.features.ghostMode;

import android.widget.Toast;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.Configs.ConfigPreferences;
import com.my.televip.language.Keys;
import com.my.televip.language.Translator;
import com.my.televip.logging.Logger;
import com.my.televip.utils.Utils;
import com.my.televip.virtuals.messenger.MessagesStorage;
import com.my.televip.virtuals.tgnet.TLRPC;

import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.XposedHelpers;

/**
 * Per-chat exceptions for Ghost Mode, plus a manual "mark as read" action.
 * Excluded chats behave like normal Telegram (read receipts, typing...), the rest stay hidden.
 */
public class GhostExceptions {

    private static final String PREF = "GhostExceptions";
    private static Set<Long> excluded;

    private static synchronized Set<Long> set() {
        if (excluded == null) {
            excluded = new HashSet<>();
            String raw = ConfigPreferences.getString(PREF);
            if (raw != null && !raw.isEmpty()) {
                for (String s : raw.split(",")) {
                    try { excluded.add(Long.parseLong(s.trim())); } catch (Throwable ignored) {}
                }
            }
        }
        return excluded;
    }

    private static synchronized void save() {
        StringBuilder sb = new StringBuilder();
        for (Long id : set()) {
            if (sb.length() > 0) sb.append(',');
            sb.append(id);
        }
        ConfigPreferences.putString(PREF, sb.toString());
    }

    public static boolean isExcluded(long dialogId) {
        return dialogId != 0 && set().contains(dialogId);
    }

    public static void toggle(long dialogId) {
        if (dialogId == 0) return;
        boolean nowExcluded;
        synchronized (GhostExceptions.class) {
            if (set().contains(dialogId)) { set().remove(dialogId); nowExcluded = false; }
            else { set().add(dialogId); nowExcluded = true; }
            save();
        }
        try {
            Toast.makeText(Utils.getCurrentActivity(),
                    Translator.get(nowExcluded ? Keys.GhostExcludedToast : Keys.GhostIncludedToast), Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {}
    }

    /** Dialog id targeted by a TL request (peer / channel field), or 0 if unknown. */
    public static long dialogIdOf(Object request) {
        if (request == null) return 0;
        try {
            Object peer = XposedHelpers.getObjectField(request, "peer");
            if (peer != null) {
                Long id = HideSeen.getDialogId(new TLRPC.InputPeer(peer));
                return id != null ? id : 0;
            }
        } catch (Throwable ignored) {}
        try {
            Object channel = XposedHelpers.getObjectField(request, "channel");
            if (channel != null) return -XposedHelpers.getLongField(channel, "channel_id");
        } catch (Throwable ignored) {}
        return 0;
    }

    public static boolean isExcludedRequest(Object request) {
        if (set().isEmpty()) return false;
        return isExcluded(dialogIdOf(request));
    }

    /** Sends the read receipt for the whole chat now, even with Ghost Mode on. */
    public static void markAsReadNow(long dialogId) {
        try {
            if (dialogId == 0) return;
            Class<?> mcClass = ClassLoad.getClass(ClassNames.MESSAGES_CONTROLLER);
            int account = XposedHelpers.getStaticIntField(ClassLoad.getClass(ClassNames.USER_CONFIG), "selectedAccount");
            Object mc = XposedHelpers.callStaticMethod(mcClass, "getInstance", account);
            Object inputPeer = XposedHelpers.callMethod(mc, "getInputPeer", dialogId);
            if (inputPeer == null) return;
            final TLRPC.InputPeer peer = new TLRPC.InputPeer(inputPeer);
            MessagesStorage storage = MessagesStorage.getMessagesStorage();
            HideSeen.getDialogMaxMessageId(storage, dialogId, maxId -> {
                if (maxId > 0) HideSeen.markReadOnServer(maxId, peer);
                try {
                    Toast.makeText(Utils.getCurrentActivity(), Translator.get(Keys.MarkedAsRead), Toast.LENGTH_SHORT).show();
                } catch (Throwable ignored) {}
            });
        } catch (Throwable t) {
            Logger.e(t);
        }
    }
}
