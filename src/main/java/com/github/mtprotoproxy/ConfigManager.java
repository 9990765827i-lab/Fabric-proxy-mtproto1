package com.github.mtprotoproxy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("mtproto-proxy-mod.json");
    private static Config config = new Config();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void loadConfig() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                config = GSON.fromJson(json, Config.class);
                MTProtoProxyMod.LOGGER.info("Loaded config from {}", CONFIG_PATH);
            } catch (IOException e) {
                MTProtoProxyMod.LOGGER.error("Failed to load config", e);
            }
        } else {
            saveConfig();
        }
    }

    public static void saveConfig() {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(config));
            MTProtoProxyMod.LOGGER.info("Saved default config to {}", CONFIG_PATH);
        } catch (IOException e) {
            MTProtoProxyMod.LOGGER.error("Failed to save config", e);
        }
    }

    public static boolean isProxyEnabled() { return config.enabled; }
    public static String getProxyHost() { return config.proxyHost; }
    public static int getProxyPort() { return config.proxyPort; }
    public static byte[] getSecret() {
        if (config.secret == null || config.secret.isEmpty()) return null;
        try {
            if (config.secret.startsWith("hex:")) {
                String hex = config.secret.substring(4).replaceAll("\\s", "");
                byte[] bytes = new byte[hex.length() / 2];
                for (int i = 0; i < bytes.length; i++) {
                    bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
                }
                return bytes;
            } else {
                return config.secret.getBytes();
            }
        } catch (Exception e) {
            MTProtoProxyMod.LOGGER.error("Invalid secret format", e);
            return null;
        }
    }

    static class Config {
        boolean enabled = false;
        String proxyHost = "127.0.0.1";
        int proxyPort = 443;
        String secret = ""; // plain text or hex:ABCDEF...
    }
}