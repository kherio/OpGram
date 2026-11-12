package com.my.televip.features.extra;

import android.app.Activity;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.language.Keys;
import com.my.televip.language.Translator;
import com.my.televip.logging.Logger;
import com.my.televip.obfuscate.Obfuscate;
import com.my.televip.utils.Utils;

import java.lang.reflect.Method;

/** Compatibility screen: which features can find what they need in the installed Telegram. */
public class Compat {

    private static final class Check {
        final String labelKey;
        final String[][] parts; // {classConstant, objectKeyOrNull, methodNameOrNull}

        Check(String labelKey, String[]... parts) {
            this.labelKey = labelKey;
            this.parts = parts;
        }
    }

    private static String[] p(String cls, String key, String method) { return new String[]{cls, key, method}; }

    private static Check[] checks() {
        return new Check[]{
                new Check(Keys.GhostMode, p(ClassNames.CONNECTIONS_MANAGER, "ConnectionsManager", "sendRequestInternal")),
                new Check(Keys.ShowDeletedMessages,
                        p(ClassNames.MESSAGES_STORAGE, "MessagesStorage", "markMessagesAsDeleted"),
                        p(ClassNames.MESSAGES_CONTROLLER, "MessagesController", "processUpdateArray")),
                new Check(Keys.SaveEditsHistory, p(ClassNames.MESSAGES_STORAGE, "MessagesStorage", "putMessages")),
                new Check(Keys.BatchDownload,
                        p(ClassNames.FILE_LOADER, null, null),
                        p(ClassNames.MEDIA_CONTROLLER, "MediaController", "saveFile")),
                new Check(Keys.TelegramPremium, p(ClassNames.USER_CONFIG, "UserConfig", "isPremium")),
                new Check(Keys.OpGramInChat, p(ClassNames.CHAT_ACTIVITY, "ChatActivity", "createView")),
                new Check(Keys.SecretMediaSave, p(ClassNames.SECRET_MEDIA_VIEWER, "SecretMediaViewer", "openMedia")),
                new Check(Keys.DisableNumberRounding, p(ClassNames.LOCALE_CONTROLLER, "LocaleController", "formatShortNumber")),
                new Check(Keys.ExactLastSeen, p(ClassNames.LOCALE_CONTROLLER, "LocaleController", "formatDateOnline")),
                new Check(Keys.DownloadSpeed, p(ClassNames.FILE_LOAD_OPERATION, "FileLoadOperation", "updateParams")),
                new Check(Keys.HideUpdateApp,
                        p(ClassNames.SHARED_CONFIG, "SharedConfig", "isAppUpdateAvailable"),
                        p(ClassNames.SHARED_CONFIG, "SharedConfig", "setNewAppVersionAvailable")),
                new Check(Keys.HideStoriesBar,
                        p(ClassNames.DOWNLOAD_CONTROLLER, "DownloadController", "canPreloadStories"),
                        p(ClassNames.MESSAGES_CONTROLLER, "MessagesController", "storiesEnabled")),
        };
    }

    private static boolean hasMethod(Class<?> c, String name) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            for (Method m : k.getDeclaredMethods()) if (m.getName().equals(name)) return true;
        }
        return false;
    }

    /** null = fine, otherwise a short description of what is missing. */
    private static String problem(Check c) {
        StringBuilder sb = new StringBuilder();
        for (String[] part : c.parts) {
            Class<?> cls = ClassLoad.getClass(part[0], Utils.classLoader, false);
            String shortName = part[0].substring(part[0].lastIndexOf('.') + 1);
            if (cls == null) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(shortName);
                continue;
            }
            if (part[1] != null && part[2] != null) {
                String name = Obfuscate.getMethodName(part[1], part[2]);
                if (!hasMethod(cls, name)) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(shortName).append('.').append(part[2]);
                }
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    public static void show(Activity activity) {
        try {
            StringBuilder body = new StringBuilder();
            int ok = 0, total = 0;
            for (Check c : checks()) {
                total++;
                String pr = problem(c);
                String label = Translator.get(c.labelKey);
                if (pr == null) {
                    ok++;
                    body.append("✅  ").append(label).append('\n');
                } else {
                    body.append("⚠️  ").append(label).append("\n      ").append(pr).append('\n');
                }
            }
            String head = "Telegram " + Utils.versionCode + "\n" + Translator.get(Keys.CompatSummary, ok, total) + "\n\n";
            new android.app.AlertDialog.Builder(activity)
                    .setTitle(Translator.get(Keys.Compatibility))
                    .setMessage(head + body + "\n" + Translator.get(Keys.CompatNote))
                    .setPositiveButton(Translator.get(Keys.Done), null)
                    .show();
        } catch (Throwable t) {
            Logger.e(t);
        }
    }
}
