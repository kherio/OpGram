package com.my.televip.virtuals.ActionBar;

import android.text.Layout;
import android.view.View;

import com.my.televip.obfuscate.ArgsResolver;
import com.my.televip.obfuscate.Obfuscate;

import de.robv.android.xposed.XposedHelpers;

public class SimpleTextView {

    Object simpleTextView;

    public SimpleTextView(Object textview){
        simpleTextView = textview;
    }

    public CharSequence getText(){
        return (CharSequence) XposedHelpers.callMethod(simpleTextView, Obfuscate.getMethodName("SimpleTextView", "getText"));
    }

    public void setText(CharSequence text){
        try {
            XposedHelpers.callMethod(simpleTextView, Obfuscate.getMethodName("SimpleTextView", "setText"), text);
        } catch (Throwable t) {
            // R8 builds only keep setText(CharSequence, boolean)
            XposedHelpers.callMethod(simpleTextView, Obfuscate.getMethodName("SimpleTextView", "setText"), text, false);
        }
    }

    public void setText(CharSequence text, boolean force){
        XposedHelpers.callMethod(simpleTextView, Obfuscate.getMethodName("SimpleTextView", "setText"), text, force);
    }

    public void setAlignment(Layout.Alignment alignment){
        XposedHelpers.callMethod(simpleTextView, Obfuscate.getMethodName("SimpleTextView", "setAlignment"), alignment);
    }

    public void setMaxLines(int value){
        XposedHelpers.callMethod(simpleTextView, Obfuscate.getMethodName("SimpleTextView", "setMaxLines"), value);
    }

    public View getSimpleTextView(){
        return (View) simpleTextView;
    }

}
