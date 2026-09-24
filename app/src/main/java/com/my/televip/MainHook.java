package com.my.televip;


import android.app.Activity;
import android.os.Bundle;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.Clients.ClientManager;
import com.my.televip.base.BaseMethodHook;
import com.my.televip.hooks.HMethod;
import com.my.televip.utils.Utils;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private boolean isStart;

    @Override
    public void initZygote(StartupParam startupParam){ Utils.modulePath = startupParam.modulePath; }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        if (!ClientManager.containsPackage(lpparam.packageName, lpparam.classLoader)) return;

        Utils.classLoader = lpparam.classLoader;
        Utils.pkgName = lpparam.packageName;
        try {
            if (lpparam.appInfo != null) {
                Utils.versionCode = de.robv.android.xposed.XposedHelpers.getLongField(lpparam.appInfo, "longVersionCode");
            }
        } catch (Throwable ignored) {
        }

        boolean mainProcess = lpparam.processName == null || lpparam.processName.equals(lpparam.packageName);
        if (mainProcess && lpparam.appInfo != null) SafeMode.onProcessStart(lpparam.appInfo.dataDir);

        HMethod.hookMethod(ClassLoad.getClass(ClassNames.LAUNCH_ACTIVITY), "onCreate", Bundle.class, new BaseMethodHook() {
            @Override
            protected void beforeMethod(MethodHookParam param) {
                Utils.setCurrentActivity((Activity) param.thisObject);
                if (SafeMode.active) {
                    SafeMode.showDialog((Activity) param.thisObject);
                    return;
                }
                if (!isStart) {
                    isStart = true;
                    TeleVip.startHook();
                    SafeMode.scheduleStableReset();
                }
            }
        });
    }


}

