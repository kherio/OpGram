package com.my.televip.Clients;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.utils.Utils;

public class ClientManager {

    public enum Client {
        Telegram("org.telegram.messenger"),
        TelegramWeb("org.telegram.messenger.web", com.my.televip.Clients.TelegramWeb.class),
        TelegramPlus("org.telegram.plus"),
        TGConnect("com.tgconnect.android"),
        Nagram("xyz.nextalone.nagram", com.my.televip.Clients.Nagram.class),
        Nicegram("app.nicegram", com.my.televip.Clients.Nicegram.class),
        TelegramBeta("org.telegram.messenger.beta"),
        NagramX("nu.gpu.nagram"),
        XPlus("com.xplus.messenger"),
        iMe("com.iMe.android"),
        iMeWeb("com.iMe.android.web"),
        forkgram("org.forkgram.messenger"),
        forkgramBeta("org.forkclient.messenger.beta"),
        Telegraph("ir.ilmili.telegraph", com.my.televip.Clients.Telegraph.class),
        Telega("ru.dahl.messenger"),
        Momogram("nekox.messenger.broken", com.my.televip.Clients.Momogram.class),
        Nekogram("tw.nekomimi.nekogram", true),
        Cherrygram("uz.unnarsx.cherrygram", true),
        ForkgramClassic("org.forkgram.classic"),
        Turrit("org.telegram.group", com.my.televip.Clients.Turrit.class),
        NagramXF("fork.risin42.nagramx");

        private final boolean tgnetObfuscated;
        private final String pkg;
        private final Class<?> resolverClass;

        Client(String pkg, boolean tgnetObfuscated) {
            this.tgnetObfuscated = tgnetObfuscated;
            this.pkg = pkg;
            resolverClass = null;
        }

        Client(String pkg) {
            this.tgnetObfuscated = false;
            this.pkg = pkg;
            resolverClass = null;
        }

        Client(String pkg, Class<?> resolverClass) {
            this.tgnetObfuscated = false;
            this.pkg = pkg;
            this.resolverClass = resolverClass;
        }

        public boolean isTgnetObfuscated() {
            return tgnetObfuscated;
        }

        public Class<?> getResolverClass() {
            return resolverClass;
        }

        public boolean hasPackage(String pkg) {

            return this.pkg.equals(pkg);
        }
    }

    public static boolean is(Client client) {

        return is(client, Utils.pkgName);

    }

    public static boolean is(Client client, String pkg) {

        return client.hasPackage(pkg);

    }

    public static Client getCurrent() {

        for (Client client : Client.values()) {

            if (client.hasPackage(Utils.pkgName))
                return client;

        }

        return null;
    }

    public static boolean isTgnetObfuscated() {

        Client client = getCurrent();

        return client != null &&
                client.isTgnetObfuscated();

    }

    public static boolean containsPackage(String pkg, ClassLoader classLoader) {

        for (Client client : Client.values()) {

            if (client.hasPackage(pkg))
                return true;

        }

        return ClassLoad.getClass(ClassNames.CONNECTIONS_MANAGER, classLoader, false) != null && ClassLoad.getClass(ClassNames.MESSAGES_CONTROLLER, classLoader, false) != null && ClassLoad.getClass(ClassNames.MESSAGES_STORAGE, classLoader, false) != null;
    }

}