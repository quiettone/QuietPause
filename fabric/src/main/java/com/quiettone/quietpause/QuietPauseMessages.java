package com.quiettone.quietpause;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class QuietPauseMessages {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<String> DEFAULT_LANGUAGES = List.of("en_us", "tr_tr");
    private static final Map<String, Map<String, String>> DEFAULT_MESSAGES = new HashMap<>();
    private static final Map<String, Map<String, String>> OVERRIDE_MESSAGES = new HashMap<>();
    private static final Set<String> WARNED_MISSING_KEYS = ConcurrentHashMap.newKeySet();
    private static Path configDir;
    private static Config config = new Config();

    private QuietPauseMessages() {
    }

    public static void init() {
        configDir = FabricLoader.getInstance().getConfigDir().resolve(QuietPause.MOD_ID);

        try {
            Files.createDirectories(configDir.resolve("lang"));
            copyDefaultConfig();
            copyDefaultLanguages();
            loadConfig();
            migrateOldDefaultLanguageConfig();
            loadLanguages();
        } catch (IOException exception) {
            System.err.println("[QuietPause] Failed to load message config: " + exception.getMessage());
        }
    }

    public static Text text(ServerCommandSource source, String key, Map<String, String> placeholders) {
        ServerPlayerEntity player = source.getPlayer();
        return Text.literal(resolve(player, key, placeholders));
    }

    public static Text text(ServerPlayerEntity player, String key, Map<String, String> placeholders) {
        return Text.literal(resolve(player, key, placeholders));
    }

    public static void broadcast(MinecraftServer server, String key, Map<String, String> placeholders) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.sendMessage(text(player, key, placeholders), false);
        }
    }

    public static Map<String, String> placeholders(String key, Object value) {
        return Map.of(key, String.valueOf(value));
    }

    public static Map<String, String> noPlaceholders() {
        return Map.of();
    }

    private static String resolve(ServerPlayerEntity player, String key, Map<String, String> placeholders) {
        String language = languageFor(player);
        String message = lookup(language, key);
        if (message == null) {
            message = lookup(config.fallbackLanguage, key);
        }
        if (message == null) {
            warnMissing(key, language);
            return key;
        }

        for (Map.Entry<String, String> placeholder : placeholders.entrySet()) {
            message = message.replace("{" + placeholder.getKey() + "}", placeholder.getValue());
        }
        return message;
    }

    private static String languageFor(ServerPlayerEntity player) {
        if ("server".equalsIgnoreCase(config.languageMode)) {
            return normalizeLanguage(config.serverLanguage);
        }
        if (player == null) {
            return normalizeLanguage(config.serverLanguage);
        }
        return normalizeLanguage(player.getClientOptions().language());
    }

    private static String lookup(String language, String key) {
        String normalized = normalizeLanguage(language);

        Map<String, String> overrides = OVERRIDE_MESSAGES.get(normalized);
        if (overrides != null && overrides.containsKey(key)) {
            return overrides.get(key);
        }

        Map<String, String> defaults = DEFAULT_MESSAGES.get(normalized);
        if (defaults != null && defaults.containsKey(key)) {
            return defaults.get(key);
        }

        return null;
    }

    private static String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "en_us";
        }
        return language.toLowerCase(Locale.ROOT);
    }

    private static void copyDefaultConfig() throws IOException {
        Path configPath = configDir.resolve("config.json");
        if (Files.exists(configPath)) {
            return;
        }

        try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
            GSON.toJson(new Config(), writer);
        }
    }

    private static void copyDefaultLanguages() throws IOException {
        for (String language : DEFAULT_LANGUAGES) {
            Path target = configDir.resolve("lang").resolve(language + ".json");
            if (Files.exists(target)) {
                continue;
            }

            String resource = "assets/" + QuietPause.MOD_ID + "/lang/" + language + ".json";
            try (InputStream input = QuietPauseMessages.class.getClassLoader().getResourceAsStream(resource)) {
                if (input != null) {
                    Files.copy(input, target);
                }
            }
        }
    }

    private static void loadConfig() throws IOException {
        Path configPath = configDir.resolve("config.json");
        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            Config loaded = GSON.fromJson(reader, Config.class);
            if (loaded != null) {
                config = loaded;
            }
        }
    }

    private static void migrateOldDefaultLanguageConfig() throws IOException {
        boolean isTurkishServerMode = "server".equalsIgnoreCase(config.languageMode) && "tr_tr".equalsIgnoreCase(config.serverLanguage);
        boolean isTurkishClientMode = "client".equalsIgnoreCase(config.languageMode) && "tr_tr".equalsIgnoreCase(config.serverLanguage);

        if (isTurkishServerMode || isTurkishClientMode) {
            config.languageMode = "client";
            config.serverLanguage = "en_us";

            Path configPath = configDir.resolve("config.json");
            try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        }
    }

    private static void loadLanguages() throws IOException {
        DEFAULT_MESSAGES.clear();
        OVERRIDE_MESSAGES.clear();

        for (String language : DEFAULT_LANGUAGES) {
            String resource = "assets/" + QuietPause.MOD_ID + "/lang/" + language + ".json";
            try (InputStream input = QuietPauseMessages.class.getClassLoader().getResourceAsStream(resource)) {
                if (input != null) {
                    DEFAULT_MESSAGES.put(language, parseJson(input));
                }
            }
        }

        Path langDir = configDir.resolve("lang");
        try (var paths = Files.list(langDir)) {
            paths.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .forEach(path -> loadOverrideLanguage(path));
        }
    }

    private static void loadOverrideLanguage(Path path) {
        String fileName = path.getFileName().toString();
        String language = normalizeLanguage(fileName.substring(0, fileName.length() - ".json".length()));

        try (InputStream input = Files.newInputStream(path)) {
            OVERRIDE_MESSAGES.put(language, parseJson(input));
        } catch (IOException exception) {
            System.err.println("[QuietPause] Failed to load language override " + path + ": " + exception.getMessage());
        }
    }

    private static Map<String, String> parseJson(InputStream input) throws IOException {
        JsonObject object = JsonParser.parseReader(new java.io.InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        Map<String, String> messages = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                messages.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        return messages;
    }

    private static void warnMissing(String key, String language) {
        String warning = language + ":" + key;
        if (WARNED_MISSING_KEYS.add(warning)) {
            System.err.println("[QuietPause] Missing translation key '" + key + "' for language '" + language + "'.");
        }
    }

    private static final class Config {
        @SuppressWarnings("unused")
        String languageMode = "client";

        @SuppressWarnings("unused")
        String serverLanguage = "en_us";

        @SuppressWarnings("unused")
        String fallbackLanguage = "en_us";
    }
}
