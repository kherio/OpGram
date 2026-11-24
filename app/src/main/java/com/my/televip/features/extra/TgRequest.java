package com.my.televip.features.extra;

import com.my.televip.Class.ClassLoad;
import com.my.televip.utils.Utils;
import com.my.televip.virtuals.messenger.UserConfig;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import de.robv.android.xposed.XposedHelpers;

/** Sends a Telegram TL request from a background thread and waits for the answer (reflection only). */
public class TgRequest {

    public static Class<?> cls(String... names) {
        for (String n : names) {
            Class<?> c = ClassLoad.getClass(n, Utils.classLoader, false);
            if (c != null) return c;
        }
        return null;
    }

    /** @return {response, error}; both null when it timed out or the request could not be sent. */
    public static Object[] call(Object req) throws Throwable {
        Class<?> delegateC = cls("org.telegram.tgnet.RequestDelegate");
        Class<?> cmC = cls("org.telegram.tgnet.ConnectionsManager");
        if (req == null || delegateC == null || cmC == null) return new Object[2];
        Object cm = XposedHelpers.callStaticMethod(cmC, "getInstance", UserConfig.getSelectedAccount());
        Method send = null;
        for (Method m : cmC.getMethods()) {
            Class<?>[] p = m.getParameterTypes();
            if (p.length == 2 && p[0].isAssignableFrom(req.getClass()) && p[1] == delegateC) { send = m; break; }
        }
        if (send == null) return new Object[2];
        final Object[] holder = new Object[2];
        final Object lock = new Object();
        Object delegate = Proxy.newProxyInstance(Utils.classLoader, new Class[]{delegateC}, (proxy, method, args) -> {
            if ("run".equals(method.getName()) && args != null && args.length == 2) {
                synchronized (lock) {
                    holder[0] = args[0];
                    holder[1] = args[1];
                    lock.notifyAll();
                }
            }
            return null;
        });
        send.invoke(cm, req, delegate);
        synchronized (lock) {
            long end = System.currentTimeMillis() + 15000;
            while (holder[0] == null && holder[1] == null && System.currentTimeMillis() < end) lock.wait(300);
        }
        return holder;
    }
}
