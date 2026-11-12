package com.my.televip.features.otherFeatures;

import com.my.televip.Configs.ConfigManager;

import com.my.televip.features.ghostMode.GhostExceptions;

import android.content.Context;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.Clients.ClientManager;
import com.my.televip.base.BaseMethodHook;
import com.my.televip.hooks.HMethod;
import com.my.televip.language.Keys;
import com.my.televip.language.Translator;
import com.my.televip.logging.Logger;
import com.my.televip.obfuscate.ArgsResolver;
import com.my.televip.obfuscate.Obfuscate;
import com.my.televip.utils.Utils;
import com.my.televip.virtuals.ActionBar.ActionBarMenuItem;
import com.my.televip.virtuals.ActionBar.AlertDialog;
import com.my.televip.virtuals.ActionBar.Theme;
import com.my.televip.virtuals.ui.ChatActivity;

import de.robv.android.xposed.XposedHelpers;

public class ChatHook {

    private static boolean initialized = false;

    public static void init(String className) {
        if (initialized || ClientManager.is(ClientManager.Client.Nagram) || ClientManager.is(ClientManager.Client.TelegramPlus)) return;

        Class<?> clazz = ClassLoad.getClass(className);
        if (clazz == null) FeatureStateManager.reset();
        try {
            initialized = true;
            HMethod.hookMethod(ClassLoad.getClass(ClassNames.CHAT_ACTIVITY), Obfuscate.getMethodName("ChatActivity", "createView"), ArgsResolver.merge("createView", new Class[]{Context.class}, new BaseMethodHook() {
                @Override
                protected void afterMethod(MethodHookParam param) {
                    try {
                        ChatActivity chatActivity = new ChatActivity(param.thisObject);

                        ActionBarMenuItem headerItem = chatActivity.getHeaderItem();
                        if (headerItem.getActionBarMenuItem() != null) {

                            int drawableResource = Utils.drawableId("msg_go_up");

                            if (!ClientManager.is(ClientManager.Client.iMe) && !ClientManager.is(ClientManager.Client.iMeWeb) && !ClientManager.is(ClientManager.Client.TelegramPlus) && !ClientManager.is(ClientManager.Client.XPlus) && !ClientManager.is(ClientManager.Client.forkgram) && !ClientManager.is(ClientManager.Client.forkgramBeta)) {
                                headerItem.lazilyAddSubItem(8353847, drawableResource, Translator.get(Keys.ToTheBeginning));
                            }
                            drawableResource = Utils.drawableId("player_new_order");

                            headerItem.lazilyAddSubItem(8353848, drawableResource, Translator.get(Keys.ToTheMessage));

                            if (ConfigManager.reminders != null && ConfigManager.reminders.isEnable()) {
                                int remIcon = Utils.drawableId("msg_calendar2");
                                headerItem.lazilyAddSubItem(8353852, remIcon, Translator.get(Keys.RemindMe));
                            }
                            if (ConfigManager.batchDownload != null && ConfigManager.batchDownload.isEnable()) {
                                int dlIcon = Utils.drawableId("msg_download");
                                headerItem.lazilyAddSubItem(8353853, dlIcon, Translator.get(Keys.BatchDownload));
                            }
                            if (ConfigManager.chatLock != null && ConfigManager.chatLock.isEnable()) {
                                long lockDid = chatActivity.getDialogId();
                                int lockIcon = Utils.drawableId(
                                        com.my.televip.features.extra.ChatLock.isLocked(lockDid) ? "menu_unlock" : "msg_secret");
                                headerItem.lazilyAddSubItem(8353851, lockIcon, Translator.get(
                                        com.my.televip.features.extra.ChatLock.isLocked(lockDid) ? Keys.UnlockThisChat : Keys.LockThisChat));
                            }
                            if (ConfigManager.totalDelete != null && ConfigManager.totalDelete.isEnable() && chatActivity.getDialogId() > 0) {
                                boolean marked = com.my.televip.features.extra.TotalDelete.isMarked(chatActivity.getDialogId());
                                headerItem.lazilyAddSubItem(8353855, Utils.drawableId("msg_delete"),
                                        Translator.get(marked ? Keys.TotalDeleteDisableChat : Keys.TotalDeleteEnableChat));
                            }
                            if (ConfigManager.secureChats != null && ConfigManager.secureChats.isEnable()) {
                                long secDid = chatActivity.getDialogId();
                                boolean sec = com.my.televip.features.extra.SecureScreen.isSecure(secDid);
                                headerItem.lazilyAddSubItem(8353854, Utils.drawableId("msg_secret"),
                                        Translator.get(sec ? Keys.UnsecureThisChat : Keys.SecureThisChat));
                            }
                            if (ConfigManager.isGhostMode()) {
                                long did = chatActivity.getDialogId();
                                int icon = Utils.drawableId("msg_markread");
                                headerItem.lazilyAddSubItem(8353849, icon, Translator.get(Keys.MarkAsReadNow));
                                icon = Utils.drawableId("ghost");
                                headerItem.lazilyAddSubItem(8353850, icon, Translator.get(
                                        GhostExceptions.isExcluded(did) ? Keys.GhostIncludeChat : Keys.GhostExcludeChat));
                            }

                        }
                    } catch (Throwable t){
                        Logger.e(t);
                    }

                }
            }));

            XposedHelpers.findAndHookMethod(clazz, Obfuscate.getMethodName("ActionBar$ActionBarMenuOnItemClick", "onItemClick"), int.class, new BaseMethodHook() {
                @Override
                protected void afterMethod(MethodHookParam param) {
                    try {
                        int id = (int) param.args[0];

                        final Object thisClass = XposedHelpers.getObjectField(param.thisObject, Obfuscate.getFieldName("ChatActivity", "this$0"));
                        ChatActivity chat = new ChatActivity(thisClass);

                        if (id == 8353847) {
                            chat.scrollToMessageId(1, 0, true, 0, true, 0);
                        } else if (id == 8353852) {
                            com.my.televip.features.extra.Reminders.offer(chat.getDialogId(), Translator.get(Keys.RemindMe));
                        } else if (id == 8353853) {
                            com.my.televip.features.extra.BatchDownload.offer(chat.getDialogId());
                        } else if (id == 8353855) {
                            com.my.televip.features.extra.TotalDelete.toggle(chat.getDialogId());
                        } else if (id == 8353854) {
                            com.my.televip.features.extra.SecureScreen.toggle(chat.getDialogId());
                        } else if (id == 8353851) {
                            com.my.televip.features.extra.ChatLock.toggle(chat.getDialogId());
                        } else if (id == 8353849) {
                            GhostExceptions.markAsReadNow(chat.getDialogId());
                        } else if (id == 8353850) {
                            GhostExceptions.toggle(chat.getDialogId());
                        } else if (id == 8353848) {

                            AlertDialog dialog = new AlertDialog(Utils.getCurrentActivity());
                            dialog.setTitle(Translator.get(Keys.InputMessageId));

                            EditText input = new EditText(Utils.getCurrentActivity());
                            input.setInputType(InputType.TYPE_CLASS_NUMBER);
                            if (Theme.isLight()) {
                                input.setTextColor(0xFF000000);
                                input.setHintTextColor(0xFF424242);
                            } else {
                                input.setTextColor(0xFFFFFFFF);
                                input.setHintTextColor(0xFFBDBDBD);
                            }
                            input.setTextSize(18);
                            input.setPadding(20, 20, 20, 20);

                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            );
                            params.setMargins(20, 20, 20, 20);
                            input.setLayoutParams(params);

                            LinearLayout layout = new LinearLayout(Utils.getCurrentActivity());
                            layout.setOrientation(LinearLayout.VERTICAL);
                            layout.addView(input);

                            dialog.setView(layout);

                            dialog.setPositiveButton(Translator.get(Keys.Done), AlertDialog.click(() -> {
                                String text = input.getText().toString().trim();
                                if (!text.isEmpty()) {
                                    int msgId = Integer.parseInt(text);
                                    chat.scrollToMessageId(msgId, 0, true, 0, true, 0);
                                }
                            }));

                            dialog.show();
                        }
                    } catch (Throwable t) {
                        Logger.e(t);
                    }
                }
            });
        } catch (Throwable t){
            FeatureStateManager.reset();
        }
    }
}