package service.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple utility to read configuration from .env file
 */
public class EnvConfig {

    private static final Map<String, String> config = new HashMap<>();

    static {
        loadEnv();
    }

    private static void loadEnv() {
        String[] paths = {".env", "./src/main/resources/.env", "src/main/resources/.env"};

        for (String path : paths) {
            try {
                Files.lines(Paths.get(path))
                        .filter(line -> !line.trim().isEmpty() && !line.trim().startsWith("#"))
                        .forEach(line -> {
                            int eqIdx = line.indexOf('=');
                            if (eqIdx > 0) {
                                config.put(line.substring(0, eqIdx).trim(),
                                          line.substring(eqIdx + 1).trim());
                            }
                        });
                return;
            } catch (IOException ignored) {}
        }
    }

    public static String get(String key, String defaultValue) {
        return config.getOrDefault(key, defaultValue);
    }

    public static String get(String key) {
        return config.get(key);
    }
}

