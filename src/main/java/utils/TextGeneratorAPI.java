package utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 🤖 API HUGGING FACE - Génération de texte IA réelle (GRATUIT)
 * Utilise le modèle de langage gratuit pour générer du texte contextualisé
 *
 * ✅ GRATUIT
 * ✅ Génère du texte unique
 * ✅ Pas juste des templates
 * ✅ Texte naturel et professionnel
 *
 * @author Votre Nom
 * @version 1.0
 */
public class TextGeneratorAPI {

    // API Hugging Face (gratuit, pas d'authentification requise pour les modèles publics)
    private static final String API_URL = "https://api-inference.huggingface.co/models/gpt2";

    // Tokens limite par requête
    private static final int MAX_TOKENS = 100;

    /**
     * 🤖 Générer un texte professionnel basé sur un prompt
     *
     * @param keywords Les mots-clés de l'utilisateur
     * @param context Le contexte (formation, description)
     * @return Texte généré unique
     */
    public static String generateProfessionalText(String keywords, String context) {
        if (keywords == null || keywords.trim().isEmpty()) {
            return "";
        }

        try {
            // Construire un prompt intelligent
            String language = detectLanguage(keywords);
            String prompt = buildPrompt(keywords, context, language);

            // Appeler l'API de génération
            String generatedText = callHuggingFaceAPI(prompt);

            if (generatedText != null && !generatedText.trim().isEmpty()) {
                // Nettoyer et améliorer le texte généré
                String improvedText = improveGeneratedText(generatedText, language);
                return improvedText;
            }

        } catch (Exception e) {
            System.err.println("Erreur génération IA: " + e.getMessage());
        }

        // Fallback vers templates si API échoue
        return TextAssistant.generateProfessionalParagraph(keywords);
    }

    /**
     * 📝 Construire un prompt intelligent pour la génération
     */
    private static String buildPrompt(String keywords, String context, String language) {
        String prefix = language.equals("fr") ?
            "Rédactif une phrase professionnelle et motivée pour l'inscription à une formation. " +
            "Les mots-clés sont: " :
            "Write a professional and motivated sentence for training registration. " +
            "Keywords are: ";

        String prompt = prefix + keywords;

        if (context != null && !context.trim().isEmpty()) {
            prompt += " Formation: " + context;
        }

        return prompt;
    }

    /**
     * 🌐 Appeler l'API Hugging Face pour générer du texte
     */
    private static String callHuggingFaceAPI(String prompt) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            // Créer le payload JSON
            JSONObject payload = new JSONObject();
            payload.put("inputs", prompt);
            JSONObject options = new JSONObject();
            options.put("max_length", MAX_TOKENS);
            options.put("temperature", 0.7);
            options.put("top_p", 0.9);
            payload.put("options", options);

            // Envoyer la requête
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Lire la réponse
            int responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                br.close();

                // Parser la réponse
                JSONArray jsonArray = new JSONArray(response.toString());
                if (jsonArray.length() > 0) {
                    JSONObject firstResult = jsonArray.getJSONObject(0);
                    String generatedText = firstResult.getString("generated_text");

                    // Extraire seulement la partie générée (sans le prompt)
                    return extractGeneratedPart(generatedText, response.toString());
                }
            } else {
                System.err.println("Erreur API Hugging Face: " + responseCode);
            }

            conn.disconnect();

        } catch (Exception e) {
            System.err.println("Exception API: " + e.getMessage());
        }

        return null;
    }

    /**
     * ✂️ Extraire la partie générée du texte
     */
    private static String extractGeneratedPart(String fullText, String originalPrompt) {
        // Nettoyer et formater
        String cleaned = fullText.trim();

        // Supprimer les caractères spéciaux indésirables
        cleaned = cleaned.replaceAll("[^a-zA-Zàâäéèêëïîôöœûüçñáéíóú\\.\\,\\!\\?\\;\\: ]", "");

        // Limiter à une phrase sensée
        if (cleaned.length() > 200) {
            cleaned = cleaned.substring(0, 200);
            int lastPeriod = cleaned.lastIndexOf(".");
            if (lastPeriod > 50) {
                cleaned = cleaned.substring(0, lastPeriod + 1);
            }
        }

        return cleaned.trim();
    }

    /**
     * 🎨 Améliorer le texte généré
     */
    private static String improveGeneratedText(String text, String language) {
        // Ajouter une structure professionnelle
        if (language.equals("fr")) {
            if (!text.toLowerCase().contains("souhaite") && !text.toLowerCase().contains("veux")) {
                text = "Je souhaite " + text;
            }
            if (!text.endsWith(".")) {
                text += ".";
            }
        } else {
            if (!text.toLowerCase().contains("want") && !text.toLowerCase().contains("would")) {
                text = "I want to " + text;
            }
            if (!text.endsWith(".")) {
                text += ".";
            }
        }

        // Capitaliser la première lettre
        if (text.length() > 0) {
            text = text.substring(0, 1).toUpperCase() + text.substring(1);
        }

        return text;
    }

    /**
     * 🌐 Détecter la langue automatiquement
     */
    public static String detectLanguage(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "en-US";
        }

        String lowerText = text.toLowerCase();

        // Mots français communs
        String[] frenchWords = {"je", "tu", "il", "elle", "nous", "vous", "ils", "de", "le", "la", "les",
            "un", "une", "des", "pour", "dans", "avec", "est", "sont", "avoir", "être"};

        // Mots anglais communs
        String[] englishWords = {"i", "you", "he", "she", "we", "they", "the", "a", "an", "is", "are",
            "have", "has", "with", "for", "to", "of", "in", "on"};

        int frenchCount = 0;
        int englishCount = 0;

        for (String word : frenchWords) {
            if (lowerText.contains(word)) {
                frenchCount++;
            }
        }

        for (String word : englishWords) {
            if (lowerText.contains(word)) {
                englishCount++;
            }
        }

        return frenchCount > englishCount ? "fr" : "en-US";
    }

    /**
     * 🔍 Vérifier si l'API est disponible
     */
    public static boolean isAPIAvailable() {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(5000);
            int responseCode = conn.getResponseCode();
            return responseCode < 500;
        } catch (Exception e) {
            return false;
        }
    }
}

