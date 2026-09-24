package com.my.televip.virtuals.ui;

import android.view.View;

import com.my.televip.obfuscate.Obfuscate;
import com.my.televip.virtuals.ActionBar.ActionBarMenuItem;
import com.my.televip.virtuals.messenger.MessageObject;

import de.robv.android.xposed.XposedHelpers;

public class ChatActivity {

    final Object chatActivity;

    public ChatActivity(Object obj){
        chatActivity = obj;
    }

    public MessageObject getSelectedObject(){
        return new MessageObject(XposedHelpers.getObjectField(chatActivity, Obfuscate.getFieldName("ChatActivity","selectedObject")));
    }

    public ActionBarMenuItem getHeaderItem(){
        return new ActionBarMenuItem(XposedHelpers.getObjectField(chatActivity, Obfuscate.getFieldName("ChatActivity", "headerItem")));
    }
    public View getPinnedMessageView(){
        return (View) XposedHelpers.getObjectField(chatActivity, Obfuscate.getFieldName("ChatActivity", "pinnedMessageView"));
    }

    public void scrollToMessageId(int id, int fromMessageId, boolean select, int loadIndex, boolean forceScroll, int forcePinnedMessageId){
        if (com.my.televip.Clients.ClientManager.is(com.my.televip.Clients.ClientManager.Client.TelegramWeb)) {
            // R8 build only keeps the 9-parameter variant
            XposedHelpers.callMethod(chatActivity, Obfuscate.getMethodName("ChatActivity", "scrollToMessageId"),
                    new Class[]{int.class, int.class, boolean.class, int.class, boolean.class, int.class, Integer.class, byte[].class, Runnable.class},
                    id, fromMessageId, select, loadIndex, forceScroll, forcePinnedMessageId, null, null, null);
            return;
        }
        XposedHelpers.callMethod(chatActivity, Obfuscate.getMethodName("ChatActivity", "scrollToMessageId"), id, fromMessageId, select, loadIndex, forceScroll, forcePinnedMessageId);
    }

}
