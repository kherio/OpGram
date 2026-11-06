package com.my.televip.language;

import com.my.televip.logging.Logger;
import com.my.televip.utils.JsonAssetReader;
import com.my.televip.virtuals.messenger.LocaleController;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Translator {

    private static final Map<String, JSONObject> langMap = new HashMap<>();

    private static LocaleController localeController;

    private static java.util.Locale locale() {
        try {
            if (localeController != null) {
                java.util.Locale l = localeController.getCurrentLocale();
                if (l != null) return l;
            }
        } catch (Throwable ignored) { }
        return java.util.Locale.getDefault();
    }

    public static void init() {
        try {
            loadAllLanguages();
        } catch (Throwable e) {
            Logger.e(e);
        }
        try {
            localeController = new LocaleController();
        } catch (Throwable e) {
            // LocaleController is obfuscated on newer Telegram builds: fall back to the system language
            localeController = null;
        }
    }
    private static void loadAllLanguages() {
        List<String> files = JsonAssetReader.listFiles("assets/lang/", ".json");

        for (String fileName : files) {
            String langCode = fileName.substring(0, fileName.lastIndexOf('.'));

            JSONObject json = JsonAssetReader.readObject("assets/lang/" + fileName);
            if (json != null) {
                langMap.put(langCode, json);
            }
        }
    }

    public static String get(String key) {
        try {
            if (locale() != null) {
                String lang = locale().getLanguage();
                JSONObject langJson = langMap.get(lang);

                if (langJson != null && langJson.has(key)) {
                    return langJson.optString(key);
                }
                JSONObject enJson = langMap.get("en");
                String text;
                if (enJson != null && enJson.has(key)) {
                    text = enJson.optString(key);
                } else {
                    text = key;
                }

                return text;
            }

        } catch (Throwable e) {
            Logger.e(e);
        }

        return key;
    }

    /** Like get(), but returns null when the key has no translation at all (used for optional texts). */
    public static String getOrNull(String key) {
        try {
            if (locale() != null) {
                JSONObject langJson = langMap.get(locale().getLanguage());
                if (langJson != null && langJson.has(key)) return langJson.optString(key);
                JSONObject enJson = langMap.get("en");
                if (enJson != null && enJson.has(key)) return enJson.optString(key);
            }
        } catch (Throwable e) {
            Logger.e(e);
        }
        return null;
    }

    public static String get(String key, Object... args) {
        try {
            if (locale() != null) {
                String lang = locale().getLanguage();

                String text = null;

                JSONObject langJson = langMap.get(lang);

                if (langJson != null && langJson.has(key)) {
                    text = langJson.optString(key);
                }

                if (text == null) {
                    JSONObject enJson = langMap.get("en");
                    if (enJson != null && enJson.has(key)) {
                        text = enJson.optString(key);
                    } else {
                        text = key;
                    }
                }

                if (args != null && args.length > 0) {
                    return String.format(text, args);
                }

                return text;
            }
        } catch (Throwable e) {
            Logger.e(e);
        }

        return key;
    }

}
