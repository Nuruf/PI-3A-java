package service.employers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import utils.employers.configLoader;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

public class extract_CV_data {
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private static final Gson gson = new Gson();

    public static Map<String, Object> extractCVData(byte[] pdfBytes) throws Exception {
        return extractAndProcess(pdfBytes);
    }

    private static Map<String, Object> extractAndProcess(byte[] pdfBytes) throws Exception {
        // 1. Validate configuration
        String groqApiUrl = configLoader.getGroqApiUrl();
        String groqApiKey = configLoader.getGroqApiKey();
        String groqModel = configLoader.getGroqModel();

        if (groqApiUrl == null || groqApiKey == null || groqModel == null || groqApiUrl.isEmpty() || groqApiKey.isEmpty() || groqModel.isEmpty()) {
            return createErrorResponse("Configuration Groq manquante (GROQ_API_URL, GROQ_API_KEY ou GROQ_MODEL).");
        }

        // 2. Extract text from CV binary
        String cvText = extractTextFromCvBinary(pdfBytes);
        if (cvText == null || cvText.trim().isEmpty()) {
            return createErrorResponse("Impossible d'extraire le texte du CV.");
        }

        // 3. Extract with Groq API
        Map<String, Object> extracted = extractWithGroq(cvText, groqApiUrl, groqApiKey, groqModel);
        if (extracted.containsKey("error")) {
            return extracted;
        }

        // 4. Process extracted data
        List<String> skills = (List<String>) extracted.getOrDefault("skills", new ArrayList<>());
        List<Map<String, String>> formations = (List<Map<String, String>>) extracted.getOrDefault("formations", new ArrayList<>());
        List<Map<String, Object>> experience = (List<Map<String, Object>>) extracted.getOrDefault("experience", new ArrayList<>());

        // 5. Return success response
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", Map.of(
                "skills", skills,
                "formations", formations,
                "experience", experience,
                "skills_count", skills.size(),
                "formations_count", formations.size(),
                "experience_count", experience.size()
        ));

        return result;
    }

    /**
     * Build and call Groq API, with simple fallback strategy and robust parsing.
     */
    private static Map<String, Object> extractWithGroq(String cvText, String groqApiUrl, String groqApiKey, String groqModel) {
        String systemMessage = "You are a strict data extraction tool. Return only a valid JSON object with the exact required structure and keys. No markdown, no explanations, no extra fields.";

        String userMessage = "Extract structured data from this CV and return ONLY a JSON object with this exact structure:\n"
                + "{\n"
                + "  \"skills\": [\"string\"],\n"
                + "  \"formations\": [\n"
                + "    {\"degree\": \"string\", \"institution\": \"string\", \"year\": \"string\"}\n"
                + "  ],\n"
                + "  \"experience\": [\n"
                + "    {\"job_title\": \"string\", \"company\": \"string\", \"duration\": \"string\", \"responsibilities\": [\"string\"]}\n"
                + "  ]\n"
                + "}\n\n"
                + "Rules:\n"
                + "- Keep exactly these top-level keys: skills, formations, experience\n"
                + "- Keep exactly these nested keys\n"
                + "- If information is missing, use empty arrays []\n"
                + "- Return only JSON\n\n"
                + "CV:\n\n"
                + cvText
                + "\n";

        // Build base messages
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", systemMessage);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);

        JsonArray messages = new JsonArray();
        messages.add(systemMsg);
        messages.add(userMsg);

        // Candidate models: configured then fallbacks
        List<String> candidateModels = new ArrayList<>();
        if (groqModel != null && !groqModel.isEmpty()) candidateModels.add(groqModel);
        candidateModels.add("llama-3.1-8b-instant");
        candidateModels.add("mixtral-8x7b-instruct");

        Exception lastException = null;
        for (String modelToTry : candidateModels) {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("model", modelToTry);
                payload.add("messages", messages);
                payload.addProperty("temperature", 0.0);

                JsonObject responseFormat = new JsonObject();
                responseFormat.addProperty("type", "json_object");
                payload.add("response_format", responseFormat);

                System.out.println("🔁 Trying Groq model: " + modelToTry);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(groqApiUrl))
                        .header("Authorization", "Bearer " + groqApiKey)
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(90))
                        .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();
                String body = response.body();
                System.out.println("📥 Groq response status=" + status + " (model=" + modelToTry + ")");
                String preview = body == null ? "(null)" : (body.length() > 1200 ? body.substring(0, 1200) + "...[truncated]" : body);
                System.out.println("📄 Groq response preview: " + preview);

                if (status != 200) {
                    if (body != null && (body.contains("model_decommissioned") || body.contains("decommissioned"))) {
                        System.err.println("⚠️ Model decommissioned: " + modelToTry + ". Trying next model...");
                        continue;
                    }
                    return createErrorResponse("Groq API error: " + status + " - " + preview);
                }

                JsonObject responseJson = gson.fromJson(body, JsonObject.class);

                // Try to extract model output from different possible fields
                String rawText = null;
                try {
                    if (responseJson.has("choices")) {
                        JsonArray choices = responseJson.getAsJsonArray("choices");
                        if (choices.size() > 0) {
                            JsonObject first = choices.get(0).getAsJsonObject();
                            if (first.has("message") && first.getAsJsonObject("message").has("content")) {
                                rawText = first.getAsJsonObject("message").get("content").getAsString();
                            } else if (first.has("content")) {
                                rawText = first.get("content").getAsString();
                            } else if (first.has("text")) {
                                rawText = first.get("text").getAsString();
                            }
                        }
                    } else if (responseJson.has("output")) {
                        rawText = responseJson.get("output").toString();
                    }
                } catch (Exception ex) {
                    System.err.println("⚠️ Impossible d'extraire le contenu depuis la réponse Groq: " + ex.getMessage());
                    ex.printStackTrace();
                    return createErrorResponse("Réponse Groq inattendue. Aperçu: " + preview);
                }

                if (rawText == null || rawText.trim().isEmpty()) {
                    return createErrorResponse("Réponse vide du modèle.");
                }

                System.out.println("🧾 Raw model output preview: " + (rawText.length() > 1200 ? rawText.substring(0, 1200) + "...[truncated]" : rawText));

                // Decode JSON response
                Map<String, Object> decoded = decodeModelJson(rawText);
                if (decoded == null) {
                    String previewRaw = rawText.length() > 250 ? rawText.substring(0, 250) : rawText;
                    return createErrorResponse("Le modèle n'a pas retourné un JSON valide. Réponse: " + previewRaw);
                }

                return decoded;

            } catch (Exception ex) {
                lastException = ex;
                System.err.println("Erreur lors de l'appel à l'API Groq avec le modèle " + modelToTry + ": " + ex.getMessage());
                ex.printStackTrace();
                // try next model
            }
        }

        if (lastException != null) {
            return createErrorResponse("Erreur lors de l'appel à l'API Groq: " + lastException.getMessage());
        }

        return createErrorResponse("Aucun modèle Groq disponible ou tous ont échoué.");
    }

    private static String extractTextFromCvBinary(byte[] cvBinary) {
        if (cvBinary == null || cvBinary.length == 0) return null;
        try {
            String sig = new String(Arrays.copyOf(cvBinary, Math.min(5, cvBinary.length)), StandardCharsets.UTF_8);
            if (sig.startsWith("%PDF-")) {
                return extractPdfTextWithPDFBox(cvBinary);
            }
        } catch (Exception ignored) {
        }
        return new String(cvBinary, StandardCharsets.UTF_8).trim();
    }

    private static String extractPdfTextWithPDFBox(byte[] cvBinary) {
        try (PDDocument doc = PDDocument.load(new ByteArrayInputStream(cvBinary))) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(doc);
            if (text == null || text.trim().isEmpty()) return null;
            return text.trim();
        } catch (Exception e) {
            System.err.println("Error extracting PDF text: " + e.getMessage());
            return null;
        }
    }

    private static Map<String, Object> decodeModelJson(String rawText) {
        // Try 1: Direct decode
        try {
            Map<String, Object> decoded = gson.fromJson(rawText, Map.class);
            if (decoded != null && !decoded.isEmpty()) {
                return decoded;
            }
        } catch (Exception e) {
            // Continue to next strategy
        }

        // Try 2: Clean markdown blocks
        String cleanJson = rawText.trim()
                .replace("```json", "")
                .replace("```", "");

        try {
            Map<String, Object> decoded = gson.fromJson(cleanJson, Map.class);
            if (decoded != null && !decoded.isEmpty()) {
                return decoded;
            }
        } catch (Exception e) {
            // Continue to next strategy
        }

        // Try 3: Extract JSON block
        int firstBrace = cleanJson.indexOf("{");
        int lastBrace = cleanJson.lastIndexOf("}");

        if (firstBrace >= 0 && lastBrace > firstBrace) {
            String jsonBlock = cleanJson.substring(firstBrace, lastBrace + 1);
            try {
                Map<String, Object> decoded = gson.fromJson(jsonBlock, Map.class);
                if (decoded != null && !decoded.isEmpty()) {
                    return decoded;
                }
            } catch (Exception e) {
                // Fall through to return null
            }
        }

        return null;
    }

    /**
     * Create error response map
     */
    private static Map<String, Object> createErrorResponse(String error) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", error);
        return result;
    }

    @Deprecated
    public static String extraireDepuisCV(byte[] pdfBytes) throws Exception {
        Map<String, Object> result = extractCVData(pdfBytes);

        if ((boolean) result.getOrDefault("success", false)) {
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            if (data != null) {
                return gson.toJson(data);
            }
        }

        String error = (String) result.get("error");
        throw new Exception(error != null ? error : "Unknown error");
    }
}

