package com.my.televip.utils;

import static com.my.televip.application.AndroidUtilities.dp;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.LinearLayout;

import com.my.televip.language.Keys;
import com.my.televip.language.Translator;
import com.my.televip.ui.ThemeColors;
import com.my.televip.virtuals.ActionBar.AlertDialog;
import com.my.televip.ui.Cells.RadioColorCell;

public class DialogUtils {

    /**
     * Single-choice dialog built with the plain Android dialog: Telegram's own AlertDialog/Builder
     * changes between builds and a failure inside a click handler closes the whole app.
     */
    public static Dialog createSingleChoiceDialog(Activity parentActivity, final String[] options, final String title, final int selected, final DialogInterface.OnClickListener listener) {
        boolean light = com.my.televip.virtuals.ActionBar.Theme.isLight();
        android.content.Context themed = new android.view.ContextThemeWrapper(parentActivity,
                light ? android.R.style.Theme_DeviceDefault_Light_Dialog_Alert : android.R.style.Theme_DeviceDefault_Dialog_Alert);
        return new android.app.AlertDialog.Builder(themed)
                .setTitle(title)
                .setSingleChoiceItems(options, selected, (d, which) -> {
                    d.dismiss();
                    try {
                        listener.onClick(d, which);
                    } catch (Throwable t) {
                        com.my.televip.logging.Logger.e(t);
                    }
                })
                .setNegativeButton(Translator.get(Keys.Cancel), null)
                .create();
    }

    public static void showQuranAlert(Context context){
        AlertDialog alertDialog = new AlertDialog(context);
        alertDialog.setTitle(com.my.televip.Branding.NAME);
        alertDialog.setMessage(Translator.get(Keys.QuranNotification));
        alertDialog.setPositiveButton(Translator.get(Keys.Done), null);
        alertDialog.show();
    }
}
