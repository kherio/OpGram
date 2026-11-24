package com.my.televip.features.extra;

import android.app.Activity;
import android.content.Context;
import android.view.ContextThemeWrapper;

import com.my.televip.language.Keys;
import com.my.televip.language.Translator;
import com.my.televip.logging.Logger;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;

/** Lists active devices and website/bot logins so forgotten or suspicious ones can be spotted. */
public class SessionsReview {

    public static void show(final Activity activity) {
        new Thread(() -> {
            String text;
            try {
                text = build();
            } catch (Throwable t) {
                Logger.e(t);
                text = Translator.get(Keys.SRFailed);
            }
            final String shown = text;
            activity.runOnUiThread(() -> dialog(activity, shown));
        }, "OpGram-sessions").start();
    }

    private static void dialog(Activity activity, String text) {
        try {
            boolean light = com.my.televip.virtuals.ActionBar.Theme.isLight();
            Context themed = new ContextThemeWrapper(activity,
                    light ? android.R.style.Theme_DeviceDefault_Light_Dialog_Alert : android.R.style.Theme_DeviceDefault_Dialog_Alert);
            new android.app.AlertDialog.Builder(themed)
                    .setTitle(Translator.get(Keys.SessionsReview))
                    .setMessage(text)
                    .setPositiveButton(Translator.get(Keys.Done), null)
                    .show();
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    private static String date(int unix) {
        if (unix <= 0) return "?";
        return DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(unix * 1000L));
    }

    private static Object field(Object o, String name) {
        try { return XposedHelpers.getObjectField(o, name); } catch (Throwable t) { return null; }
    }

    private static int intField(Object o, String name) {
        try { return XposedHelpers.getIntField(o, name); } catch (Throwable t) { return 0; }
    }

    private static boolean boolField(Object o, String name) {
        try { return XposedHelpers.getBooleanField(o, name); } catch (Throwable t) { return false; }
    }

    private static String str(Object o, String name) {
        Object v = field(o, name);
        return v == null ? "" : String.valueOf(v);
    }

    private static String build() throws Throwable {
        Class<?> devReq = TgRequest.cls("org.telegram.tgnet.tl.TL_account$getAuthorizations", "org.telegram.tgnet.TLRPC$TL_account_getAuthorizations");
        Class<?> webReq = TgRequest.cls("org.telegram.tgnet.tl.TL_account$getWebAuthorizations", "org.telegram.tgnet.TLRPC$TL_account_getWebAuthorizations");
        if (devReq == null && webReq == null) return Translator.get(Keys.SRFailed);

        StringBuilder out = new StringBuilder();
        int warnings = 0;
        long now = System.currentTimeMillis() / 1000L;
        boolean any = false;

        if (devReq != null) {
            Object[] r = TgRequest.call(devReq.newInstance());
            Object list = r[0] == null ? null : field(r[0], "authorizations");
            if (list instanceof List) {
                any = true;
                out.append("📱  ").append(Translator.get(Keys.SRDevices)).append("\n");
                for (Object a : (List<?>) list) {
                    boolean current = boolField(a, "current");
                    boolean official = boolField(a, "official_app");
                    int active = intField(a, "date_active");
                    boolean stale = !current && active > 0 && now - active > 30L * 86400L;
                    boolean warn = stale || (!official && !current);
                    if (warn) warnings++;
                    out.append(current ? "✅  " : warn ? "⚠️  " : "•  ")
                            .append(str(a, "app_name")).append(" · ").append(str(a, "device_model")).append('\n')
                            .append("      ").append(str(a, "platform")).append(' ').append(str(a, "system_version"))
                            .append(" — ").append(str(a, "country")).append(" ").append(str(a, "ip")).append('\n')
                            .append("      ").append(current ? Translator.get(Keys.SRThisDevice) : date(active))
                            .append(!official && !current ? "  · " + Translator.get(Keys.SRUnofficial) : "")
                            .append(stale ? "  · " + Translator.get(Keys.SRInactive) : "").append('\n');
                }
                out.append('\n');
            }
        }

        if (webReq != null) {
            Object[] r = TgRequest.call(webReq.newInstance());
            Object list = r[0] == null ? null : field(r[0], "authorizations");
            if (list instanceof List) {
                any = true;
                out.append("🌐  ").append(Translator.get(Keys.SRWeb)).append("\n");
                if (((List<?>) list).isEmpty()) out.append("✅  ").append(Translator.get(Keys.SRNone)).append('\n');
                Object users = field(r[0], "users");
                for (Object a : (List<?>) list) {
                    long botId = 0;
                    try { botId = XposedHelpers.getLongField(a, "bot_id"); } catch (Throwable ignored) {}
                    String bot = "";
                    if (users instanceof List) {
                        for (Object u : (List<?>) users) {
                            long id = 0;
                            try { id = XposedHelpers.getLongField(u, "id"); } catch (Throwable ignored) {}
                            if (id == botId && id != 0) {
                                String un = str(u, "username");
                                bot = un.isEmpty() ? str(u, "first_name") : "@" + un;
                                break;
                            }
                        }
                    }
                    int active = intField(a, "date_active");
                    boolean stale = active > 0 && now - active > 30L * 86400L;
                    if (stale) warnings++;
                    String domain = str(a, "domain");
                    out.append(stale ? "⚠️  " : "•  ").append(domain.isEmpty() ? bot : domain)
                            .append(bot.isEmpty() || domain.isEmpty() ? "" : " (" + bot + ")").append('\n')
                            .append("      ").append(str(a, "browser")).append(" · ").append(str(a, "platform"))
                            .append(" — ").append(date(active)).append(stale ? "  · " + Translator.get(Keys.SRInactive) : "").append('\n');
                }
                out.append('\n');
            }
        }

        if (!any) return Translator.get(Keys.SRFailed);
        out.append(warnings > 0 ? Translator.get(Keys.SRWarn, warnings) : Translator.get(Keys.SRClean));
        out.append("\n").append(Translator.get(Keys.SRHow));
        return out.toString();
    }
}
