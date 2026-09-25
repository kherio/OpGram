package com.my.televip.Configs;

import android.app.Activity;
import android.content.SharedPreferences;

import com.my.televip.application.ApplicationLoaderHook;
import com.my.televip.logging.Logger;

public class ConfigPreferences {

    private static SharedPreferences sharedPreferences;


    public static void init(){
        sharedPreferences = ApplicationLoaderHook.getApplicationContext().getSharedPreferences("TeleVip", Activity.MODE_PRIVATE);
    }

    public static boolean getBoolean(String key) {
        try {
            return sharedPreferences.getBoolean(key, false);
        } catch (ClassCastException e) {
            sharedPreferences.edit().remove(key).apply();
            return false;
        }
    }

    public static void putBoolean(String key, boolean b) {
        try {
            sharedPreferences.edit().putBoolean(key, b).apply();
        } catch (ClassCastException e) {
            sharedPreferences.edit().remove(key).apply();
        }
    }
    public static long getLong(String key) {
        try {
            return sharedPreferences.getLong(key, 0L);
        } catch (Throwable t) {
            return 0L;
        }
    }

    public static void putLong(String key, long v) {
        try {
            sharedPreferences.edit().putLong(key, v).apply();
        } catch (Throwable ignored) {
        }
    }

    public static int getInt(String key) {
        try {
            return sharedPreferences.getInt(key, 0);
        } catch (ClassCastException e) {
            sharedPreferences.edit().remove(key).apply();
            return 0;
        }
    }

    public static void putInt(String key, int var) {
        try {
            sharedPreferences.edit().putInt(key, var).apply();
        } catch (ClassCastException e) {
            sharedPreferences.edit().remove(key).apply();
        }
    }

    public static String getString(String key) {
        try {
            return sharedPreferences.getString(key, null);
        } catch (ClassCastException e) {

            sharedPreferences.edit().remove(key).apply();
            return null;
        }
    }

    public static void putString(String key, String v) {
        try {
            sharedPreferences.edit().putString(key, v).apply();
        } catch (ClassCastException e) {
            sharedPreferences.edit().remove(key).apply();
        }
    }

    public static void remove(String key){
        try {
            if (key == null) return;
            sharedPreferences.edit().remove(key).apply();
        } catch (Throwable t){
            Logger.e(t);
        }
    }


    /** Exports every TeleVip preference as JSON (with type info) for backup. */
    public static String exportJson() {
        try {
            org.json.JSONObject root = new org.json.JSONObject();
            root.put("televip_backup", 1);
            org.json.JSONObject values = new org.json.JSONObject();
            for (java.util.Map.Entry<String, ?> e : sharedPreferences.getAll().entrySet()) {
                Object v = e.getValue();
                org.json.JSONObject item = new org.json.JSONObject();
                if (v instanceof Boolean) item.put("t", "b");
                else if (v instanceof Integer) item.put("t", "i");
                else if (v instanceof Long) item.put("t", "l");
                else if (v instanceof String) item.put("t", "s");
                else continue;
                item.put("v", v);
                values.put(e.getKey(), item);
            }
            root.put("values", values);
            return root.toString();
        } catch (Throwable t) {
            Logger.e(t);
            return null;
        }
    }

    /** Restores preferences from a backup made with exportJson(). Returns false if the text is not a valid backup. */
    public static boolean importJson(String text) {
        try {
            if (text == null) return false;
            org.json.JSONObject root = new org.json.JSONObject(text.trim());
            if (!root.has("televip_backup")) return false;
            org.json.JSONObject values = root.getJSONObject("values");
            SharedPreferences.Editor ed = sharedPreferences.edit();
            java.util.Iterator<String> keys = values.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                org.json.JSONObject item = values.getJSONObject(k);
                switch (item.optString("t")) {
                    case "b": ed.putBoolean(k, item.getBoolean("v")); break;
                    case "i": ed.putInt(k, item.getInt("v")); break;
                    case "l": ed.putLong(k, item.getLong("v")); break;
                    case "s": ed.putString(k, item.getString("v")); break;
                }
            }
            ed.apply();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

}
