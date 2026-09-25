package com.my.televip.features.messages;

import android.util.SparseArray;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.Clients.ClientManager;
import com.my.televip.Configs.ConfigManager;
import com.my.televip.base.BaseMethodHook;
import com.my.televip.hooks.HMethod;
import com.my.televip.logging.Logger;
import com.my.televip.messages.MessageStorage;
import com.my.televip.obfuscate.ArgsResolver;
import com.my.televip.obfuscate.Obfuscate;
import com.my.televip.virtuals.androidx.LongSparseArray;
import com.my.televip.virtuals.messenger.MessageObject;
import com.my.televip.virtuals.messenger.MessagesController;
import com.my.televip.virtuals.messenger.MessagesStorage;
import com.my.televip.virtuals.messenger.NotificationCenter;
import com.my.televip.virtuals.tgnet.TLRPC;

import java.util.ArrayList;

public class ShowDeletedMessages {

    public static final int FLAG_DELETED = 1 << 31;
    public static final String DELETED_AT = "opgram_deleted_at";

    /** Marks a message owner as deleted and records the wall-clock time of deletion. */
    public static void stampDeleted(TLRPC.Message owner) {
        owner.setFlags(owner.getFlags() | FLAG_DELETED);
        try {
            de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField(owner.get_Message(), DELETED_AT, System.currentTimeMillis());
        } catch (Throwable ignored) {
        }
        com.my.televip.features.extra.EditDeleteAlerts.onDeleted();
    }

    public static long getDeletedAt(TLRPC.Message owner) {
        try {
            Object v = de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField(owner.get_Message(), DELETED_AT);
            return v instanceof Long ? (Long) v : 0L;
        } catch (Throwable t) {
            return 0L;
        }
    }

    private static boolean isDeleteMessage = false;

    public static boolean isEnable = false;

    public static void markMessagesDeletedForController(MessagesStorage messagesStorage, long dialogId, ArrayList<Integer> delMsg) {
        MessageStorage.markMessagesDeleted(messagesStorage, dialogId, delMsg);
    }

    private static void processDeletedMessage(MessagesController messagesController, Object item, boolean updateDeleteChannelMessages, boolean updateDeleteMessages) {

        if (updateDeleteChannelMessages) {
            TLRPC.TL_updateDeleteChannelMessages channelMessages = new TLRPC.TL_updateDeleteChannelMessages(item);

            LongSparseArray dialogMessage = messagesController.getDialogMessage();

            ArrayList<Object> dialogMessages = dialogMessage.get(-channelMessages.getChannelID());
            if (dialogMessages != null) {
                for (final Object msgObj : dialogMessages) {
                    TLRPC.Message owner = new MessageObject(msgObj).getMessageOwner();
                    if (channelMessages.getMessages().contains(owner.getId())) {
                        stampDeleted(owner);
                    }
                }
            }

            markMessagesDeletedForController(messagesController.getMessagesStorage(), -channelMessages.getChannelID(), channelMessages.getMessages());
        }

        if (updateDeleteMessages) {
            ArrayList<Integer> messages = new TLRPC.TL_updateDeleteMessages(item).getMessages();
            SparseArray<Object> dialogMessages = messagesController.getDialogMessagesByIds();
            for (int id : messages) {
                Object msgObj = dialogMessages.get(id);
                if (msgObj == null) {
                    break;
                } else {
                    TLRPC.Message owner = new MessageObject(msgObj).getMessageOwner();
                    stampDeleted(owner);
                }
            }
            markMessagesDeletedForController(messagesController.getMessagesStorage(), 0, messages);
        }
    }

    public static void initProcessUpdateArray() {
        try {
            HMethod.hookMethod(
                    ClassLoad.getClass(ClassNames.MESSAGES_CONTROLLER),
                    Obfuscate.getMethodName("MessagesController", "processUpdateArray"),
                    ArgsResolver.merge("processUpdateArray", new Class[]{ArrayList.class, ArrayList.class, ArrayList.class, boolean.class, int.class},
                            new BaseMethodHook() {
                                @Override
                                protected void beforeMethod(MethodHookParam param) {

                                    try {
                                        ArrayList<Object> updates = (ArrayList<Object>) param.args[0];
                                        MessagesController messagesController = new MessagesController(param.thisObject);
                                        if (updates.isEmpty()) {
                                            return;
                                        }

                                        ArrayList<Object> result = new ArrayList<>();

                                        for (Object update : updates) {

                                            String name = update.getClass().getName();

                                            boolean updateDeleteChannelMessages;
                                            boolean updateDeleteMessages;

                                            if (!ClientManager.isTgnetObfuscated()) {
                                                updateDeleteChannelMessages = name.contains("TL_updateDeleteChannelMessages");
                                                updateDeleteMessages = name.contains("TL_updateDeleteMessages");
                                            } else {
                                                updateDeleteChannelMessages = update.getClass().equals(ClassLoad.getClass(ClassNames.TL_UPDATE_DELETE_CHANNEL_MESSAGES));
                                                updateDeleteMessages = update.getClass().equals(ClassLoad.getClass(ClassNames.TL_UPDATE_DELETE_MESSAGES));
                                            }

                                            if (updateDeleteChannelMessages || updateDeleteMessages) {
                                                processDeletedMessage(messagesController, update,
                                                        updateDeleteChannelMessages, updateDeleteMessages);
                                                continue;
                                            }

                                            result.add(update);
                                        }

                                        param.args[0] = result;

                                    } catch (Throwable e) {
                                        Logger.e(e);
                                    }
                                }
                            }));
        } catch (Throwable e) {
            Logger.e(e);
        }
    }

    public static void init() {
        try {
            if (!isEnable) {
                isEnable = true;

                HMethod.hookMethod(
                        ClassLoad.getClass(ClassNames.MESSAGES_STORAGE),
                        Obfuscate.getMethodName("MessagesStorage", "markMessagesAsDeleted"),
                        ArgsResolver.merge("markMessagesAsDeleted", new Class[]{long.class, java.util.ArrayList.class, boolean.class, boolean.class, int.class, int.class},
                                new BaseMethodHook() {
                                    @Override
                                    protected void beforeMethod(MethodHookParam param) {
                                        if (!isDeleteMessage) {
                                            param.setResult(null);
                                        }
                                    }
                                }
                        ));
            }
            HMethod.hookMethod(ClassLoad.getClass(ClassNames.NOTIFICATIONS_CONTROLLER),Obfuscate.getMethodName("NotificationsController", "removeDeletedMessagesFromNotifications"), ArgsResolver.merge("removeDeletedMessagesFromNotifications", new Class[]{ClassLoad.getClass(ClassNames.LONG_SPARES_ARRAY), boolean.class}, new BaseMethodHook() {
                    @Override
                    protected void beforeMethod(MethodHookParam param) {
                        if (ConfigManager.showDeletedMessages.isEnable()) {
                            param.setResult(null);
                        }
                    }
                }));

            HMethod.hookMethod(
                    ClassLoad.getClass(ClassNames.MESSAGES_CONTROLLER),
                    Obfuscate.getMethodName("MessagesController", "deleteMessages"),
                    ArgsResolver.merge("deleteMessages", new Class[]{java.util.ArrayList.class,
                                    java.util.ArrayList.class,
                                    ClassLoad.getClass(ClassNames.TLRPC_ENCRYPTED_CHAT),
                                    long.class,
                                    boolean.class,
                                    int.class,
                                    boolean.class,
                                    long.class,
                                    ClassLoad.getClass(ClassNames.TL_OBJECT),
                                    int.class,
                                    boolean.class,
                                    int.class},
                            new BaseMethodHook() {
                                @Override
                                protected void beforeMethod(MethodHookParam param) {
                                    isDeleteMessage = true;
                                }
                            }
                    ));

            HMethod.hookMethod(ClassLoad.getClass(ClassNames.NOTIFICATION_CENTER), Obfuscate.getMethodName("NotificationCenter", "postNotificationName"), ArgsResolver.merge("postNotificationName", new Class[]{int.class, Object[].class}, new BaseMethodHook() {
                @Override
                protected void beforeMethod(MethodHookParam param) {
                    if (!isDeleteMessage) {
                        int id = (int) param.args[0];
                        if (id == NotificationCenter.getMessagesDeleted()) {
                            param.setResult(null);
                        }
                    }
                }

                @Override
                protected void afterMethod(MethodHookParam param) {
                    isDeleteMessage = false;
                }
            }));

            ShowDeletedMessages.initProcessUpdateArray();
        } catch (Throwable e) {
            Logger.e(e);
        }

        if (ConfigManager.showDeletedMessages.isEnable() && !MessageTimeModifier.loaded)
            MessageTimeModifier.init();

    }

}