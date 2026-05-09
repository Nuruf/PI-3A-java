package utils.employers;

import java.io.InputStream;
import java.util.Properties;

public class configLoader {

    private static Properties properties = null;

    private static void load() {
        if (properties != null) return;

        properties = new Properties();
        try (InputStream is = configLoader.class
                .getResourceAsStream("/config.properties")) {
            if (is != null) {
                properties.load(is);
            } else {
                System.err.println("config.properties not found");
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement config : " + e.getMessage());
        }
    }

    public static String get(String key) {
        load();
        String value = properties.getProperty(key, "");
        return value != null ? value.trim() : "";
    }

    // Groq API Configuration
    public static String getGroqApiUrl() {
        return get("groq.api.url");
    }

    public static String getGroqApiKey() {
        return get("groq.api.key");
    }

    public static String getGroqModel() {
        return get("groq.model");
    }

    // Legacy Hugging Face Configuration (deprecated)
    public static String getApiToken() {
        return get("huggingface.api.token");
    }

    public static String getApiUrl() {
        return get("huggingface.api.url");
    }
}