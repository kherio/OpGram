package com.my.televip.utils;

import android.app.Activity;

import com.google.gson.Gson;
import com.my.televip.logging.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;

public class Utils {
    public static String pkgName = null;
    public static String modulePath = null;
    public static ClassLoader classLoader = null;
    public static long versionCode = 0;
    public static boolean versionedMapping = false;
    public static final String issue = "Your Telegram client may be an incompatible version with OpGram. Please download the latest version that is compatible with OpGram.";

    private static WeakReference<Activity> currentActivity;

    public static void setCurrentActivity(Activity activity) {
        currentActivity = new WeakReference<>(activity);
    }

    public static Activity getCurrentActivity() {
        return currentActivity != null ? currentActivity.get() : null;
    }

    /** Resource id of a Telegram drawable by name (R$drawable is obfuscated in newer builds). */
    public static int drawableId(String name) {
        try {
            android.content.Context c = (android.content.Context) Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication").invoke(null);
            if (c != null) {
                int id = c.getResources().getIdentifier(name, "drawable", c.getPackageName());
                if (id != 0) return id;
            }
        } catch (Throwable ignored) { }
        try {
            return de.robv.android.xposed.XposedHelpers.getStaticIntField(
                    com.my.televip.Class.ClassLoad.getClass(com.my.televip.Class.ClassNames.DRAWABLE), name);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static String getFieldAsString(Object value) {

        if (value == null) {
            return null;
        }

        if (value instanceof CharSequence) {
            return value.toString();
        }

        return String.valueOf(value);
    }

    public static void objectToJson(Object object) {
        try {
            Gson gson = new Gson();

            String json = gson.toJson(object);

            File dir = new File(Utils.getCurrentActivity().getFilesDir(), "backup");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File file = new File(
                    dir,
                    object.getClass().getSimpleName() + "_" + System.currentTimeMillis() + ".json"
            );

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(json.getBytes(StandardCharsets.UTF_8));
            fos.close();
        } catch (Throwable throwable) {
            Logger.e(throwable);
        }
    }

}
