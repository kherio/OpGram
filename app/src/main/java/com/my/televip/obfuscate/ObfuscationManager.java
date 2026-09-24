package com.my.televip.obfuscate;

import com.my.televip.Clients.ClientManager;
import com.my.televip.obfuscate.struct.ClientObfuscationData;
import com.my.televip.utils.JsonAssetReader;

import java.util.EnumMap;
import java.util.Map;

public class ObfuscationManager {

    private static final Map<ClientManager.Client, ClientObfuscationData> cache =
            new EnumMap<>(ClientManager.Client.class);

    private static volatile ClientObfuscationData currentClientData;
    private static volatile boolean attemptedLoad = false;

    public static ClientObfuscationData current() {
        if (currentClientData != null) return currentClientData;
        if (attemptedLoad) return null;

        synchronized (ObfuscationManager.class) {
            if (currentClientData != null) return currentClientData;
            attemptedLoad = true;

            ClientManager.Client type = ClientManager.getCurrent();
            if (type == null) return null;

            currentClientData = load(type);
            return currentClientData;
        }
    }

    private static ClientObfuscationData load(ClientManager.Client type) {
        if (cache.containsKey(type)) return cache.get(type);

        ClientObfuscationData data = null;
        String clientJson = null;
        String clientAliasJson = null;

        // Per-version mapping first (e.g. TelegramWeb-70999.json), then the generic one.
        if (com.my.televip.utils.Utils.versionCode > 0) {
            String suffix = "-" + com.my.televip.utils.Utils.versionCode + ".json";
            clientJson = JsonAssetReader.readRaw("assets/clients/" + type.name() + suffix, false);
            if (clientJson != null) {
                com.my.televip.utils.Utils.versionedMapping = true;
                clientAliasJson = JsonAssetReader.readRaw("assets/clients/Alias/" + type.name() + suffix, false);
            }
        }
        if (clientJson == null) {
            clientJson = JsonAssetReader.readRaw("assets/clients/" + type.name() + ".json", false);
        }
        if (clientAliasJson == null) {
            clientAliasJson = JsonAssetReader.readRaw("assets/clients/Alias/" + type.name() + ".json", false);
        }

        if (clientJson != null && clientAliasJson != null)
            data = ClientObfuscationData.parse(clientJson, clientAliasJson);

        if (data != null)
            cache.put(type, data);
        return data;
    }

}