package com.github.mtprotoproxy;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MTProtoProxyMod implements ModInitializer {
    public static final String MOD_ID = "mtproto-proxy-mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("MTProto Proxy Mod initialized");
        ConfigManager.loadConfig();
        LOGGER.info("Proxy config: enabled={}, host={}, port={}, secret={}",
                ConfigManager.isProxyEnabled(),
                ConfigManager.getProxyHost(),
                ConfigManager.getProxyPort(),
                ConfigManager.getSecret() != null ? "[set]" : "[not set]");
    }
}