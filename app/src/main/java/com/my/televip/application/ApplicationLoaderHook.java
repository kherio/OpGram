package com.my.televip.application;

import android.content.Context;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.obfuscate.Obfuscate;

import de.robv.android.xposed.XposedHelpers;

public class ApplicationLoaderHook {

    private static Context applicationContext;

    public static Context getApplicationContext() {
        if (applicationContext == null) {
            try {
                applicationContext = (Context) XposedHelpers.getStaticObjectField(
                        ClassLoad.getClass(ClassNames.APPLICATION_LOADER),
                        Obfuscate.getFieldName("ApplicationLoader", "applicationContext")
                );
            } catch (Throwable ignored) {
                // ApplicationLoader is obfuscated on newer Telegram builds: ask Android instead
            }
        }
        if (applicationContext == null) {
            try {
                applicationContext = (Context) Class.forName("android.app.ActivityThread")
                        .getMethod("currentApplication").invoke(null);
            } catch (Throwable ignored) { }
        }
        return applicationContext;
    }
}