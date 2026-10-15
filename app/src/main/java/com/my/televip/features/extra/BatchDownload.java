package com.my.televip.features.extra;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.widget.Toast;

import com.my.televip.language.Keys;
import com.my.televip.language.Translator;
import com.my.televip.logging.Logger;
import com.my.televip.utils.Utils;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;

/**
 * Downloads the latest photos and videos of the open chat and saves them to the gallery.
 *
 * It only uses Telegram classes that R8 does not rename (TLRPC, FileLoader, MediaController,
 * ConnectionsManager...), reached by reflection, so it survives new Telegram builds. Every step is
 * wrapped: if a signature ever changes the user gets a toast and the reason goes to the log.
 */
public class BatchDownload {

    private static final int[] CHOICES = {20, 50, 100};
    private static final long TIMEOUT_MS = 15 * 60 * 1000L;

    private static boolean running = false;

    /** Asks how many items to fetch and starts the download. */
    public static void offer(final long dialogId) {
        final Activity activity = Utils.getCurrentActivity();
        if (activity == null) return;
        if (running) { toast(Translator.get(Keys.BatchDownloadBusy)); return; }
        String[] labels = new String[CHOICES.length];
        for (int i = 0; i < CHOICES.length; i++) {
            labels[i] = Translator.get(Keys.BatchDownloadLast, CHOICES[i]);
        }
        new AlertDialog.Builder(activity)
                .setTitle(Translator.get(Keys.BatchDownload))
                .setItems(labels, (d, which) -> start(dialogId, CHOICES[which]))
                .setNegativeButton(Translator.get(Keys.Cancel), null)
                .show();
    }

    private static void start(final long dialogId, final int limit) {
        running = true;
        toast(Translator.get(Keys.BatchDownloadSearching));
        new Thread(() -> {
            try {
                run(dialogId, limit);
            } catch (Throwable t) {
                Logger.e(t);
                toast(Translator.get(Keys.BatchDownloadFailed));
            } finally {
                running = false;
            }
        }).start();
    }

    private static Class<?> cls(String name) {
        return XposedHelpers.findClass(name, Utils.classLoader);
    }

    private static void run(long dialogId, int limit) throws Throwable {
        final ClassLoader cl = Utils.classLoader;
        final int account = XposedHelpers.getStaticIntField(cls("org.telegram.messenger.UserConfig"), "selectedAccount");

        Class<?> tlObject = cls("org.telegram.tgnet.TLObject");
        Class<?> delegateC = cls("org.telegram.tgnet.RequestDelegate");
        Class<?> cmC = cls("org.telegram.tgnet.ConnectionsManager");
        Class<?> mcC = cls("org.telegram.messenger.MessagesController");

        // --- messages.search with the photo+video filter ---
        Object req = cls("org.telegram.tgnet.TLRPC$TL_messages_search").newInstance();
        Object mc = XposedHelpers.callStaticMethod(mcC, "getInstance", account);
        Method getInputPeer = mcC.getMethod("getInputPeer", long.class);
        Object peer = getInputPeer.invoke(mc, dialogId);
        XposedHelpers.setObjectField(req, "peer", peer);
        XposedHelpers.setObjectField(req, "q", "");
        XposedHelpers.setObjectField(req, "filter", cls("org.telegram.tgnet.TLRPC$TL_inputMessagesFilterPhotoVideo").newInstance());
        XposedHelpers.setIntField(req, "limit", Math.min(limit, 100));
        XposedHelpers.setIntField(req, "offset_id", 0);
        XposedHelpers.setIntField(req, "add_offset", 0);

        final Object[] holder = new Object[2];
        final Object lock = new Object();
        Object delegate = Proxy.newProxyInstance(cl, new Class[]{delegateC}, (proxy, method, args) -> {
            if ("run".equals(method.getName())) {
                synchronized (lock) {
                    holder[0] = args[0];
                    holder[1] = args[1];
                    lock.notifyAll();
                }
            }
            return null;
        });
        Object cm = XposedHelpers.callStaticMethod(cmC, "getInstance", account);
        cmC.getMethod("sendRequest", tlObject, delegateC).invoke(cm, req, delegate);

        synchronized (lock) {
            long end = System.currentTimeMillis() + 30000;
            while (holder[0] == null && holder[1] == null && System.currentTimeMillis() < end) {
                lock.wait(500);
            }
        }
        if (holder[1] != null || holder[0] == null) {
            Logger.w("BatchDownload: search failed " + holder[1]);
            toast(Translator.get(Keys.BatchDownloadFailed));
            return;
        }
        List<?> messages = (List<?>) XposedHelpers.getObjectField(holder[0], "messages");
        if (messages == null || messages.isEmpty()) {
            toast(Translator.get(Keys.BatchDownloadEmpty));
            return;
        }

        // --- start every download ---
        Class<?> fileLoaderC = cls("org.telegram.messenger.FileLoader");
        Class<?> messageC = cls("org.telegram.tgnet.TLRPC$Message");
        Class<?> documentC = cls("org.telegram.tgnet.TLRPC$Document");
        Class<?> messageObjectC = cls("org.telegram.messenger.MessageObject");
        Object fl = XposedHelpers.callStaticMethod(fileLoaderC, "getInstance", account);
        int prioLow = XposedHelpers.getStaticIntField(fileLoaderC, "PRIORITY_LOW");
        Method getPath = fileLoaderC.getMethod("getPathToMessage", messageC);
        Method loadDoc = fileLoaderC.getMethod("loadFile", documentC, Object.class, int.class, int.class);
        Method getDocument = messageObjectC.getMethod("getDocument", messageC);
        Method isPhoto = messageObjectC.getMethod("isPhoto", messageC);

        final List<File> files = new ArrayList<>();
        final List<Object> items = new ArrayList<>();
        final List<Boolean> video = new ArrayList<>();
        final List<String> names = new ArrayList<>();
        final List<String> mimes = new ArrayList<>();

        for (Object m : messages) {
            try {
                Object doc = getDocument.invoke(null, m);
                File target;
                boolean isVideo;
                String name = null, mime = null;
                if (doc != null) {
                    mime = (String) XposedHelpers.getObjectField(doc, "mime_type");
                    if (mime == null || !mime.startsWith("video/")) continue;
                    target = (File) getPath.invoke(fl, m);
                    if (target == null) continue;
                    name = (String) fileLoaderC.getMethod("getDocumentFileName", documentC).invoke(null, doc);
                    if (!target.exists()) loadDoc.invoke(fl, doc, m, prioLow, 0);
                    isVideo = true;
                } else if ((Boolean) isPhoto.invoke(null, m)) {
                    target = (File) getPath.invoke(fl, m);
                    if (target == null) continue;
                    if (!target.exists()) {
                        Object media = XposedHelpers.getObjectField(m, "media");
                        Object photo = XposedHelpers.getObjectField(media, "photo");
                        Object sizes = XposedHelpers.getObjectField(photo, "sizes");
                        int px = (Integer) XposedHelpers.callStaticMethod(cls("org.telegram.messenger.AndroidUtilities"), "getPhotoSize");
                        Object size = fileLoaderC.getMethod("getClosestPhotoSizeWithSize", ArrayList.class, int.class)
                                .invoke(null, sizes, px);
                        Object loc = cls("org.telegram.messenger.ImageLocation")
                                .getMethod("getForPhoto", cls("org.telegram.tgnet.TLRPC$PhotoSize"), cls("org.telegram.tgnet.TLRPC$Photo"))
                                .invoke(null, size, photo);
                        fileLoaderC.getMethod("loadFile", cls("org.telegram.messenger.ImageLocation"), Object.class, String.class, int.class, int.class)
                                .invoke(fl, loc, m, null, prioLow, 0);
                    }
                    isVideo = false;
                } else {
                    continue;
                }
                // add to every list together so they always stay aligned
                files.add(target);
                items.add(m);
                video.add(isVideo);
                names.add(name);
                mimes.add(mime);
            } catch (Throwable t) {
                Logger.e(t);
            }
        }
        if (files.isEmpty()) {
            toast(Translator.get(Keys.BatchDownloadEmpty));
            return;
        }
        toast(Translator.get(Keys.BatchDownloadStarted, files.size()));

        // --- wait for the files to appear, then save each to the gallery ---
        final Activity activity = Utils.getCurrentActivity();
        Class<?> mediaC = cls("org.telegram.messenger.MediaController");
        Method saveFile = mediaC.getMethod("saveFile", String.class, Context.class, int.class, String.class, String.class);
        boolean[] saved = new boolean[files.size()];
        int done = 0;
        long end = System.currentTimeMillis() + TIMEOUT_MS;
        while (done < files.size() && System.currentTimeMillis() < end) {
            for (int i = 0; i < files.size(); i++) {
                if (saved[i]) continue;
                File f = files.get(i);
                if (f.exists() && f.length() > 0) {
                    try {
                        saveFile.invoke(null, f.getAbsolutePath(), activity, video.get(i) ? 1 : 0, names.get(i), mimes.get(i));
                        saved[i] = true;
                        done++;
                    } catch (Throwable t) {
                        Logger.e(t);
                        saved[i] = true; // do not retry forever
                    }
                }
            }
            if (done < files.size()) Thread.sleep(1000);
        }
        toast(Translator.get(Keys.BatchDownloadDone, done, files.size()));
    }

    private static void toast(final String text) {
        try {
            final Activity a = Utils.getCurrentActivity();
            if (a != null) a.runOnUiThread(() -> Toast.makeText(a, text, Toast.LENGTH_LONG).show());
        } catch (Throwable ignored) {}
    }
}
