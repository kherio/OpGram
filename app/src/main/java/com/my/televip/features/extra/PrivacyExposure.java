package com.my.televip.features.extra;

import android.app.Activity;
import android.content.Context;
import android.view.ContextThemeWrapper;

import com.my.televip.Class.ClassLoad;
import com.my.televip.language.Keys;
import com.my.televip.language.Translator;
import com.my.televip.logging.Logger;
import com.my.televip.utils.Utils;
import com.my.televip.virtuals.messenger.UserConfig;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;

/**
 * "What do people see about me": reads the account's privacy rules from Telegram and lists, for each
 * item, who can see it, flagging the ones open to everybody.
 */
public class PrivacyExposure {

    // {TL key class suffix, label key, warn when open to everybody}
    private static final Object[][] KEYS = {
            {"PhoneNumber", Keys.PXPhone, true},
            {"AddedByPhone", Keys.PXFindByPhone, true},
            {"StatusTimestamp", Keys.PXSeen, true},
            {"ProfilePhoto", Keys.PXPhoto, false},
            {"About", Keys.PXAbout, false},
            {"Birthday", Keys.PXBirthday, false},
            {"ChatInvite", Keys.PXGroups, true},
            {"PhoneCall", Keys.PXCalls, false},
            {"Forwards", Keys.PXForwards, true},
            {"VoiceMessages", Keys.PXVoice, false},
    };

    private static Class<?> cls(String... names) {
        for (String n : names) {
            Class<?> c = ClassLoad.getClass(n, Utils.classLoader, false);
            if (c != null) return c;
        }
        return null;
    }

    public static void show(final Activity activity) {
        new Thread(() -> {
            try {
                final String text = build();
                activity.runOnUiThread(() -> dialog(activity, text));
            } catch (Throwable t) {
                Logger.e(t);
                activity.runOnUiThread(() -> dialog(activity, Translator.get(Keys.PXFailed)));
            }
        }, "OpGram-privacy").start();
    }

    private static void dialog(Activity activity, String text) {
        try {
            boolean light = com.my.televip.virtuals.ActionBar.Theme.isLight();
            Context themed = new ContextThemeWrapper(activity,
                    light ? android.R.style.Theme_DeviceDefault_Light_Dialog_Alert : android.R.style.Theme_DeviceDefault_Dialog_Alert);
            new android.app.AlertDialog.Builder(themed)
                    .setTitle(Translator.get(Keys.PrivacyExposure))
                    .setMessage(text)
                    .setPositiveButton(Translator.get(Keys.Done), null)
                    .show();
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    private static String build() throws Throwable {
        Class<?> reqC = cls("org.telegram.tgnet.tl.TL_account$getPrivacy", "org.telegram.tgnet.TLRPC$TL_account_getPrivacy");
        Class<?> tlObject = cls("org.telegram.tgnet.TLObject");
        Class<?> delegateC = cls("org.telegram.tgnet.RequestDelegate");
        Class<?> cmC = cls("org.telegram.tgnet.ConnectionsManager");
        if (reqC == null || tlObject == null || delegateC == null || cmC == null) return Translator.get(Keys.PXFailed);

        int account = UserConfig.getSelectedAccount();
        Object cm = XposedHelpers.callStaticMethod(cmC, "getInstance", account);
        Method send = null;
        for (Method m : cmC.getMethods()) {
            Class<?>[] p = m.getParameterTypes();
            if (p.length == 2 && p[0].isAssignableFrom(reqC) && p[1] == delegateC) { send = m; break; }
        }
        if (send == null) return Translator.get(Keys.PXFailed);

        StringBuilder out = new StringBuilder();
        int open = 0, got = 0;
        for (Object[] k : KEYS) {
            Class<?> keyC = cls("org.telegram.tgnet.TLRPC$TL_inputPrivacyKey" + k[0]);
            if (keyC == null) continue;
            Object req = reqC.newInstance();
            XposedHelpers.setObjectField(req, "key", keyC.newInstance());

            final Object[] holder = new Object[2];
            final Object lock = new Object();
            Object delegate = Proxy.newProxyInstance(Utils.classLoader, new Class[]{delegateC}, (proxy, method, args) -> {
                if ("run".equals(method.getName()) && args != null && args.length == 2) {
                    synchronized (lock) {
                        holder[0] = args[0];
                        holder[1] = args[1];
                        lock.notifyAll();
                    }
                }
                return null;
            });
            send.invoke(cm, req, delegate);
            synchronized (lock) {
                long end = System.currentTimeMillis() + 15000;
                while (holder[0] == null && holder[1] == null && System.currentTimeMillis() < end) lock.wait(300);
            }
            String label = Translator.get((String) k[1]);
            if (holder[0] == null) {
                out.append("❓  ").append(label).append("\n      ").append(Translator.get(Keys.PXUnknown)).append('\n');
                continue;
            }
            got++;
            List<?> rules = (List<?>) XposedHelpers.getObjectField(holder[0], "rules");
            String audience = "";
            boolean everybody = false;
            int allowUsers = 0, denyUsers = 0, chats = 0;
            boolean contacts = false, all = false;
            for (Object r : rules) {
                String n = r.getClass().getSimpleName();
                if (n.contains("AllowAll")) all = true;
                else if (n.contains("AllowContacts")) contacts = true;
                else if (n.contains("AllowUsers")) allowUsers += size(r, "users");
                else if (n.contains("DisallowUsers")) denyUsers += size(r, "users");
                else if (n.contains("ChatParticipants")) chats += size(r, "chats");
            }
            if (all) { audience = Translator.get(Keys.PXAll); everybody = true; }
            else if (contacts) audience = Translator.get(Keys.PXContacts);
            else audience = Translator.get(Keys.PXNobody);
            StringBuilder extra = new StringBuilder();
            if (allowUsers > 0) extra.append(" +").append(allowUsers);
            if (denyUsers > 0) extra.append(" −").append(denyUsers);
            if (chats > 0) extra.append(" (").append(chats).append(" ").append(Translator.get(Keys.PXGroupsWord)).append(")");
            boolean warn = everybody && (Boolean) k[2];
            if (warn) open++;
            out.append(warn ? "⚠️  " : "✅  ").append(label).append("\n      ")
                    .append(audience).append(extra).append('\n');
        }
        if (got == 0) return Translator.get(Keys.PXFailed);
        return out + "\n" + (open > 0 ? Translator.get(Keys.PXOpenSome, open) : Translator.get(Keys.PXOpenNone))
                + "\n" + Translator.get(Keys.PXNote);
    }

    private static int size(Object rule, String field) {
        try {
            Object v = XposedHelpers.getObjectField(rule, field);
            return v instanceof List ? ((List<?>) v).size() : 0;
        } catch (Throwable t) {
            return 0;
        }
    }
}
