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
 * 🌐 API LANGUAGETOOL - Correction orthographique réelle
 * Utilise l'API gratuite de LanguageTool (https://languagetool.org)
 *
 * ✅ GRATUIT
 * ✅ Supporte +30 langues
 * ✅ Correction orthographe, grammaire, style
 * ✅ Suggère des améliorations
 *
 * @author Votre Nom
 * @version 2.0 - API Réelle
 */
public class LanguageToolAPI {

    // API gratuite de LanguageTool
    private static final String API_URL = "https://api.languagetool.org/v2/check";

    // Limite gratuite : 20 requêtes/minute
    private static final int MAX_TEXT_LENGTH = 10000; // 10K caractères max par requête

    /**
     * 🔍 Vérifier un texte avec l'API LanguageTool
     *
     * @param text Texte à vérifier
     * @param language Code langue ("fr" ou "en-US")
     * @return Liste d'erreurs détectées
     */
    public static List<GrammarError> checkText(String text, String language) {
        List<GrammarError> errors = new ArrayList<>();

        if (text == null || text.trim().isEmpty()) {
            return errors;
        }

        if (text.length() > MAX_TEXT_LENGTH) {
            text = text.substring(0, MAX_TEXT_LENGTH);
        }

        try {
            // Préparer les paramètres
            String params = "text=" + URLEncoder.encode(text, StandardCharsets.UTF_8) +
                           "&language=" + URLEncoder.encode(language, StandardCharsets.UTF_8) +
                           "&enabledOnly=false";

            // Créer la connexion
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            // Envoyer la requête
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = params.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Lire la réponse
            int responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));

                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                br.close();

                // Parser la réponse JSON
                errors = parseResponse(response.toString());
            } else {
                System.err.println("Erreur API LanguageTool: " + responseCode);
                // Retourner liste vide si API échoue (fallback géré dans le contrôleur)
                return new ArrayList<>();
            }

            conn.disconnect();

        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification: " + e.getMessage());
            e.printStackTrace();
            // Retourner liste vide (fallback géré dans le contrôleur)
            return new ArrayList<>();
        }

        return errors;
    }

    /**
     * 📊 Parser la réponse JSON de LanguageTool
     */
    private static List<GrammarError> parseResponse(String jsonResponse) {
        List<GrammarError> errors = new ArrayList<>();

        try {
            JSONObject json = new JSONObject(jsonResponse);
            JSONArray matches = json.getJSONArray("matches");

            for (int i = 0; i < matches.length(); i++) {
                JSONObject match = matches.getJSONObject(i);

                String message = match.getString("message");
                String shortMessage = match.optString("shortMessage", "");

                int offset = match.getInt("offset");
                int length = match.getInt("length");

                // Récupérer les suggestions
                List<String> suggestions = new ArrayList<>();
                if (match.has("replacements")) {
                    JSONArray replacements = match.getJSONArray("replacements");
                    for (int j = 0; j < Math.min(3, replacements.length()); j++) {
                        suggestions.add(replacements.getJSONObject(j).getString("value"));
                    }
                }

                // Type d'erreur
                JSONObject rule = match.getJSONObject("rule");
                String category = rule.getJSONObject("category").getString("name");
                String ruleId = rule.getString("id");

                GrammarError error = new GrammarError(
                    message,
                    shortMessage,
                    offset,
                    length,
                    suggestions,
                    category,
                    ruleId
                );

                errors.add(error);
            }

        } catch (Exception e) {
            System.err.println("Erreur parsing JSON: " + e.getMessage());
        }

        return errors;
    }

    /**
     * 🔧 Corriger automatiquement un texte
     *
     * @param text Texte original
     * @param errors Liste d'erreurs
     * @return Texte corrigé
     */
    public static String applyCorrections(String text, List<GrammarError> errors) {
        if (errors.isEmpty()) {
            return text;
        }

        // Trier les erreurs par position (du plus loin au plus proche)
        errors.sort((a, b) -> Integer.compare(b.offset, a.offset));

        StringBuilder correctedText = new StringBuilder(text);

        for (GrammarError error : errors) {
            if (!error.suggestions.isEmpty()) {
                // Prendre la première suggestion
                String replacement = error.suggestions.get(0);

                // Remplacer dans le texte
                int start = error.offset;
                int end = error.offset + error.length;

                if (end <= correctedText.length()) {
                    correctedText.replace(start, end, replacement);
                }
            }
        }

        return correctedText.toString();
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
            if (lowerText.contains(" " + word + " ") || lowerText.startsWith(word + " ")) {
                frenchCount++;
            }
        }

        for (String word : englishWords) {
            if (lowerText.contains(" " + word + " ") || lowerText.startsWith(word + " ")) {
                englishCount++;
            }
        }

        return frenchCount > englishCount ? "fr" : "en-US";
    }

    /**
     * ✨ Améliorer un texte (suggestions de style)
     */
    public static List<String> getStyleSuggestions(String text) {
        List<String> suggestions = new ArrayList<>();

        if (text == null || text.trim().isEmpty()) {
            return suggestions;
        }

        // Vérifier avec l'API
        String language = detectLanguage(text);
        List<GrammarError> errors = checkText(text, language);

        // Filtrer uniquement les suggestions de style
        for (GrammarError error : errors) {
            if (error.category.equalsIgnoreCase("style") ||
                error.category.equalsIgnoreCase("redundancy") ||
                error.category.equalsIgnoreCase("clarity")) {

                suggestions.add("💡 " + error.message);
            }
        }

        return suggestions;
    }

    /**
     * 📝 Classe représentant une erreur grammaticale
     */
    public static class GrammarError {
        public final String message;
        public final String shortMessage;
        public final int offset;
        public final int length;
        public final List<String> suggestions;
        public final String category;
        public final String ruleId;

        public GrammarError(String message, String shortMessage, int offset, int length,
                           List<String> suggestions, String category, String ruleId) {
            this.message = message;
            this.shortMessage = shortMessage;
            this.offset = offset;
            this.length = length;
            this.suggestions = suggestions;
            this.category = category;
            this.ruleId = ruleId;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s → %s",
                category,
                shortMessage.isEmpty() ? message : shortMessage,
                suggestions.isEmpty() ? "Aucune suggestion" : String.join(", ", suggestions));
        }
    }
}

