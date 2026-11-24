package com.my.televip.features.extra;

import android.app.AndroidAppHelper;
import android.media.ExifInterface;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.Configs.ConfigManager;
import com.my.televip.logging.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/**
 * Removes GPS, camera model, dates and similar EXIF data from JPEG files right before Telegram uploads them.
 * The original file is never touched: a cleaned copy in the app cache is uploaded instead.
 * The upload method is found by its shape, because its name is obfuscated and changes between builds.
 */
public class MediaCleaner {

    private static boolean isEnable = false;

    private static final String[] TAGS = {
            "GPSLatitude", "GPSLatitudeRef", "GPSLongitude", "GPSLongitudeRef", "GPSAltitude", "GPSAltitudeRef",
            "GPSTimeStamp", "GPSDateStamp", "GPSProcessingMethod", "GPSAreaInformation", "GPSSpeed", "GPSSpeedRef",
            "GPSImgDirection", "GPSImgDirectionRef", "GPSDestLatitude", "GPSDestLongitude", "GPSVersionID",
            "GPSMapDatum", "GPSStatus", "GPSMeasureMode", "GPSDOP", "GPSSatellites",
            "Make", "Model", "Software", "Artist", "Copyright", "ImageDescription", "UserComment", "MakerNote",
            "DateTime", "DateTimeOriginal", "DateTimeDigitized", "SubSecTime", "SubSecTimeOriginal", "SubSecTimeDigitized",
            "OffsetTime", "OffsetTimeOriginal", "OffsetTimeDigitized",
            "ImageUniqueID", "BodySerialNumber", "CameraOwnerName", "LensMake", "LensModel", "LensSerialNumber",
            "HostComputer", "DeviceSettingDescription"
    };

    public static void init() {
        try {
            if (isEnable) return;
            Class<?> fl = ClassLoad.getClass(ClassNames.FILE_LOADER);
            if (fl == null) return;
            XC_MethodHook hook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        if (ConfigManager.cleanMetadata == null || !ConfigManager.cleanMetadata.isEnable()) return;
                        if (!(param.args[0] instanceof String)) return;
                        String cleaned = clean((String) param.args[0]);
                        if (cleaned != null) param.args[0] = cleaned;
                    } catch (Throwable t) {
                        Logger.e(t);
                    }
                }
            };
            int n = 0;
            for (Method m : fl.getDeclaredMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (m.getReturnType() == void.class && !Modifier.isStatic(m.getModifiers())
                        && p.length >= 4 && p[0] == String.class && p[1] == boolean.class && p[2] == boolean.class) {
                    XposedBridge.hookMethod(m, hook);
                    n++;
                }
            }
            isEnable = n > 0;
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    /** @return path of a cleaned copy, or null when the file is not a JPEG with metadata (upload it as is). */
    static String clean(String path) {
        try {
            File src = new File(path);
            if (!src.isFile() || src.length() < 4 || src.length() > 200L * 1024 * 1024) return null;
            try (InputStream in = new FileInputStream(src)) {
                if (in.read() != 0xFF || in.read() != 0xD8) return null;
            }
            android.app.Application app = AndroidAppHelper.currentApplication();
            if (app == null) return null;
            File dir = new File(app.getCacheDir(), "opgram_clean");
            if (!dir.isDirectory() && !dir.mkdirs()) return null;
            File dst = new File(dir, Integer.toHexString((path + src.length() + src.lastModified()).hashCode()) + ".jpg");
            if (dst.isFile() && dst.length() > 0) return dst.getAbsolutePath();

            try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dst)) {
                byte[] buf = new byte[64 * 1024];
                int r;
                while ((r = in.read(buf)) > 0) out.write(buf, 0, r);
            }
            ExifInterface exif = new ExifInterface(dst.getAbsolutePath());
            boolean had = false;
            for (String tag : TAGS) {
                if (exif.getAttribute(tag) != null) {
                    had = true;
                    exif.setAttribute(tag, null);
                }
            }
            if (!had) {
                dst.delete();
                return null;
            }
            exif.saveAttributes();
            return dst.getAbsolutePath();
        } catch (Throwable t) {
            Logger.e(t);
            return null;
        }
    }
}
