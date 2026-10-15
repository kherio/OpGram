package com.my.televip.ui;

import static com.my.televip.virtuals.ActionBar.Theme.isLight;

import android.graphics.Color;

import com.my.televip.Configs.ConfigPreferences;
import com.my.televip.language.Keys;

public class ThemeColors {

    /** Accent presets: 0 = follow Telegram. */
    public static final int[] ACCENTS = {0, 0xFF3FA9F5, 0xFF4CAF50, 0xFFFF9800, 0xFFE91E63, 0xFF9C27B0, 0xFF26C6DA};

    /** 0 = follow Telegram, 1 = AMOLED black (only applies when Telegram is dark). */
    public static int getThemeMode() { return ConfigPreferences.getInt(Keys.SettingsTheme + "Int"); }

    public static int getAccentIndex() {
        int i = ConfigPreferences.getInt(Keys.SettingsAccent + "Int");
        return i >= 0 && i < ACCENTS.length ? i : 0;
    }

    public static boolean isAmoled() { return getThemeMode() == 1 && !isLight(); }


    public static int getBackgroundGrayColor(){
        if (isAmoled()) return Color.BLACK;
        return isLight() ? Color.rgb(241, 241, 243) : Color.rgb(21, 30, 39);
    }

    public static int getBackgroundWhiteOrBlueColor(){
        if (isAmoled()) return Color.rgb(10, 10, 10);
        return isLight() ? Color.WHITE : Color.rgb(29, 39, 51);
    }

    public static int getToolBarColor(){
        if (isAmoled()) return Color.BLACK;
        return isLight() ? Color.WHITE : Color.rgb(36, 45, 57);
    }

    public static int getToolBarRippleColor(){
        return isLight() ? 0x20000000 : 0x20FFFFFF;
    }

    public static int getTextToolBarColor(){
        return isLight() ? Color.BLACK : Color.WHITE;
    }
    public static int getTextColor(){
        return isLight() ? Color.BLACK : Color.WHITE;
    }

    public static int getTextBlueColor(){
        int a = ACCENTS[getAccentIndex()];
        if (a != 0) return a;
        return isLight() ? Color.rgb(100, 164, 221) : Color.rgb(112, 184, 221);
    }
    public static int getTextGrayColor(){
        return isLight() ? Color.rgb(128, 128, 128) : Color.rgb(103,115,128);
    }

    public static int getArrowDrawableColor(){
        return isLight() ? Color.BLACK : Color.WHITE;
    }

}
