package com.my.televip.features.extra;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;

import com.my.televip.Branding;
import com.my.televip.Configs.ConfigPreferences;
import com.my.televip.language.Keys;
import com.my.televip.language.Translator;
import com.my.televip.logging.Logger;
import com.my.televip.utils.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Checks the GitHub releases of the OpGram repo for a newer version and offers to
 * download and install it. Requires that every release is signed with the SAME key,
 * otherwise Android refuses to update in place.
 *
 * Runs inside Telegram's process, so it uses Telegram's INTERNET permission.
 */
public class Updater {

    private static final String LATEST_API = "https://api.github.com/repos/kherio/OpGram/releases/latest";
    private static final String LAST_CHECK = "OpGramLastUpdateCheck";
    private static final String SKIP_TAG = "OpGramSkipTag";
    private static final long CHECK_INTERVAL_MS = 12 * 60 * 60 * 1000L; // twice a day at most

    public static void checkAsync(final boolean manual) {
        new Thread(() -> {
            try {
                if (!manual) {
                    long last = ConfigPreferences.getLong(LAST_CHECK);
                    if (System.currentTimeMillis() - last < CHECK_INTERVAL_MS) return;
                }
                ConfigPreferences.putLong(LAST_CHECK, System.currentTimeMillis());

                Release r = fetchLatest();
                if (r == null) {
                    if (manual) toast(Translator.get(Keys.UpdateCheckFailed));
                    return;
                }

                String current = Branding.version();
                if (!isNewer(r.version, current)) {
                    if (manual) toast(Translator.get(Keys.UpdateUpToDate));
                    return;
                }
                if (!manual && r.tag.equals(ConfigPreferences.getString(SKIP_TAG))) return;

                promptOnUi(r);
            } catch (Throwable t) {
                Logger.e(t);
                if (manual) toast(Translator.get(Keys.UpdateCheckFailed));
            }
        }).start();
    }

    private static Release fetchLatest() throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(LATEST_API).openConnection();
        c.setRequestProperty("Accept", "application/vnd.github+json");
        c.setRequestProperty("User-Agent", "OpGram");
        c.setConnectTimeout(10000);
        c.setReadTimeout(10000);
        if (c.getResponseCode() != 200) return null;

        StringBuilder sb = new StringBuilder();
        java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(c.getInputStream()));
        String line;
        while ((line = in.readLine()) != null) sb.append(line);
        in.close();

        JSONObject o = new JSONObject(sb.toString());
        Release r = new Release();
        r.tag = o.optString("tag_name");
        r.version = r.tag.replaceAll("[^0-9.]", "");
        r.notes = o.optString("body");
        JSONArray assets = o.optJSONArray("assets");
        if (assets != null) {
            for (int i = 0; i < assets.length(); i++) {
                JSONObject a = assets.getJSONObject(i);
                String name = a.optString("name");
                if (name.toLowerCase().endsWith(".apk")) {
                    r.apkUrl = a.optString("browser_download_url");
                    r.apkName = name;
                    break;
                }
            }
        }
        return r.apkUrl != null ? r : null;
    }

    /** True if remote is strictly newer than local (dotted numeric compare). */
    static boolean isNewer(String remote, String local) {
        if (remote == null || remote.isEmpty()) return false;
        // If we cannot determine the local version, do NOT assume an update exists
        // (that caused "update available" to show even on the latest version).
        if (local == null || local.isEmpty()) return false;
        String[] a = remote.split("\\.");
        String[] b = local.split("\\.");
        int n = Math.max(a.length, b.length);
        for (int i = 0; i < n; i++) {
            int x = i < a.length ? parse(a[i]) : 0;
            int y = i < b.length ? parse(b[i]) : 0;
            if (x != y) return x > y;
        }
        return false;
    }

    private static int parse(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Throwable t) { return 0; }
    }

    private static void promptOnUi(final Release r) {
        final Activity activity = Utils.getCurrentActivity();
        if (activity == null) return;
        activity.runOnUiThread(() -> {
            try {
                final float d = activity.getResources().getDisplayMetrics().density;
                int pad = Math.round(20 * d);

                // Scrollable body: version line + the full release notes
                android.widget.ScrollView scroll = new android.widget.ScrollView(activity);
                android.widget.LinearLayout box = new android.widget.LinearLayout(activity);
                box.setOrientation(android.widget.LinearLayout.VERTICAL);
                box.setPadding(pad, pad / 2, pad, 0);

                android.widget.TextView head = new android.widget.TextView(activity);
                head.setText(Translator.get(Keys.UpdateAvailable, r.version));
                head.setTextSize(16);
                box.addView(head);

                if (r.notes != null && !r.notes.trim().isEmpty()) {
                    android.widget.TextView title = new android.widget.TextView(activity);
                    title.setText(Translator.get(Keys.UpdateNotes));
                    title.setTextSize(13);
                    title.setAllCaps(true);
                    title.setPadding(0, Math.round(16 * d), 0, Math.round(4 * d));
                    box.addView(title);

                    android.widget.TextView notes = new android.widget.TextView(activity);
                    notes.setText(cleanNotes(r.notes));
                    notes.setTextSize(14);
                    box.addView(notes);
                }
                scroll.addView(box);

                new AlertDialog.Builder(activity)
                        .setTitle(Branding.NAME)
                        .setView(scroll)
                        .setPositiveButton(Translator.get(Keys.UpdateDownload), (dl, w) -> download(activity, r))
                        .setNegativeButton(Translator.get(Keys.Cancel), null)
                        .setNeutralButton(Translator.get(Keys.UpdateSkip),
                                (dl, w) -> ConfigPreferences.putString(SKIP_TAG, r.tag))
                        .show();
            } catch (Throwable t) {
                Logger.e(t);
            }
        });
    }

    /** GitHub release bodies are Markdown; strip the most common markup so it reads well as plain text. */
    static String cleanNotes(String md) {
        String t = md.replace("\r", "");
        t = t.replaceAll("(?m)^#{1,6}\\s*", "");
        t = t.replaceAll("(?m)^\\s*[-*]\\s+", "\u2022 ");
        t = t.replace("**", "").replace("__", "").replace("`", "");
        t = t.replaceAll("\\[([^\\]]+)\\]\\([^)]*\\)", "$1");
        return t.trim();
    }

    private static void download(final Activity activity, final Release r) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && !activity.getPackageManager().canRequestPackageInstalls()) {
                Intent settings = new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + activity.getPackageName()));
                settings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(settings);
                toast(Translator.get(Keys.UpdateAllowInstall));
                return;
            }

            final DownloadManager dm = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(r.apkUrl));
            req.setTitle(Branding.NAME + " " + r.version);
            req.setMimeType("application/vnd.android.package-archive");
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, r.apkName);
            final long id = dm.enqueue(req);

            // Progress dialog, refreshed by polling the DownloadManager
            final android.widget.ProgressBar bar = new android.widget.ProgressBar(activity, null,
                    android.R.attr.progressBarStyleHorizontal);
            bar.setMax(100);
            final android.widget.TextView label = new android.widget.TextView(activity);
            label.setText(Translator.get(Keys.UpdateDownloadProgress, 0));
            final float d = activity.getResources().getDisplayMetrics().density;
            int pad = Math.round(20 * d);
            android.widget.LinearLayout box = new android.widget.LinearLayout(activity);
            box.setOrientation(android.widget.LinearLayout.VERTICAL);
            box.setPadding(pad, pad, pad, pad / 2);
            box.addView(label);
            box.addView(bar);

            final boolean[] cancelled = {false};
            final AlertDialog[] dialog = new AlertDialog[1];
            activity.runOnUiThread(() -> {
                dialog[0] = new AlertDialog.Builder(activity)
                        .setTitle(Branding.NAME + " " + r.version)
                        .setView(box)
                        .setCancelable(false)
                        .setNegativeButton(Translator.get(Keys.Cancel), (dl, w) -> {
                            cancelled[0] = true;
                            try { dm.remove(id); } catch (Throwable ignored) {}
                        })
                        .show();
            });

            new Thread(() -> {
                try {
                    while (!cancelled[0]) {
                        android.database.Cursor c = dm.query(new DownloadManager.Query().setFilterById(id));
                        if (c == null) break;
                        int status = -1;
                        long got = 0, total = 0;
                        try {
                            if (c.moveToFirst()) {
                                status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                                got = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                                total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                            }
                        } finally {
                            c.close();
                        }
                        final int pct = total > 0 ? (int) (got * 100 / total) : 0;
                        activity.runOnUiThread(() -> {
                            bar.setProgress(pct);
                            label.setText(Translator.get(Keys.UpdateDownloadProgress, pct));
                        });
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            dismiss(activity, dialog[0]);
                            activity.runOnUiThread(() -> install(activity, dm, id));
                            return;
                        }
                        if (status == DownloadManager.STATUS_FAILED || status == -1) {
                            dismiss(activity, dialog[0]);
                            toast(Translator.get(Keys.UpdateDownloadFailed));
                            return;
                        }
                        Thread.sleep(400);
                    }
                    dismiss(activity, dialog[0]);
                } catch (Throwable t) {
                    Logger.e(t);
                    dismiss(activity, dialog[0]);
                    toast(Translator.get(Keys.UpdateDownloadFailed));
                }
            }).start();
        } catch (Throwable t) {
            Logger.e(t);
            toast(Translator.get(Keys.UpdateCheckFailed));
        }
    }

    private static void dismiss(Activity activity, final AlertDialog dialog) {
        activity.runOnUiThread(() -> {
            try { if (dialog != null && dialog.isShowing()) dialog.dismiss(); } catch (Throwable ignored) {}
        });
    }

    private static void install(Activity activity, DownloadManager dm, long id) {
        try {
            // DownloadManager hands back a content:// URI that any installer can read, so
            // OpGram does not need to declare its own FileProvider in the manifest.
            Uri uri = dm.getUriForDownloadedFile(id);
            if (uri == null) { toast(Translator.get(Keys.UpdateCheckFailed)); return; }
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(uri, "application/vnd.android.package-archive");
            i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    private static void toast(final String text) {
        try {
            final Activity a = Utils.getCurrentActivity();
            if (a != null) a.runOnUiThread(() -> Toast.makeText(a, text, Toast.LENGTH_LONG).show());
        } catch (Throwable ignored) {}
    }

    private static class Release {
        String tag, version, notes, apkUrl, apkName;
    }
}
