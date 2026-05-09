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
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement config : " + e.getMessage());
        }
    }

    public static String get(String key) {
        load();
        return properties.getProperty(key, "");
    }

    public static String getApiToken() {
        return get("huggingface.api.token");
    }

    public static String getApiUrl() {
        return get("huggingface.api.url");
    }
}