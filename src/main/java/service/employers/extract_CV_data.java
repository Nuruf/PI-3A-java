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
import java.time.Duration;


public class extract_CV_data {
    private static final String API_URL = configLoader.getApiUrl();
    private static final String API_TOKEN = configLoader.getApiToken();

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private static final Gson gson = new Gson();
    public static String extraireDepuisCV(byte[] pdfBytes) throws Exception {
        String cvText = extraireTextePDF(pdfBytes);
        return extractCvData(cvText);
    }
    private static String extraireTextePDF(byte[] pdfBytes) throws Exception {
        try (PDDocument doc = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(doc);
            if (text == null || text.trim().isEmpty()) {
                throw new Exception("Aucun texte extrait du PDF.");
            }
            return text.trim();
        }
    }
    public static String extractCvData(String cvText) throws Exception {

        String systemMessage =
                "You are a strict data extraction tool. You only output valid JSON. " +
                        "Do not include any conversational text, explanations, or markdown code blocks like ```json. " +
                        "Extract exactly: skills (list of strings), formations (list of objects), and experience (list of objects).";

        String userMessage = "Extract data from this CV:\n\n" +
                cvText + "\n\n" +
                "Return ONLY a JSON object with this exact structure:\n" +
                "{\n" +
                "  \"skills\": [\"string\"],\n" +
                "  \"formations\": [\n" +
                "    {\"degree\": \"string\", \"institution\": \"string\", \"year\": \"string\"}\n" +
                "  ],\n" +
                "  \"experience\": [\n" +
                "    {\"job_title\": \"string\", \"company\": \"string\", \"duration\": \"string\", \"responsibilities\": [\"string\"]}\n" +
                "  ]\n" +
                "}";

        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", systemMessage);
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);
        JsonArray messages = new JsonArray();
        messages.add(systemMsg);
        messages.add(userMsg);

        JsonObject payload = new JsonObject();
        payload.addProperty("model", "meta-llama/Llama-3.2-3B-Instruct");
        payload.add("messages", messages);
        payload.addProperty("temperature", 0.1);
        payload.addProperty("max_tokens", 1000);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(API_URL)).header("Authorization", "Bearer " + API_TOKEN).header("Content-Type", "application/json").timeout(Duration.ofSeconds(60)).POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload))).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
            String rawText = responseJson.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString();
            String cleanJson = rawText.replace("```json", "").replace("```", "").trim();

            try {
                gson.fromJson(cleanJson, JsonObject.class);
                return cleanJson;
            } catch (Exception e) {
                System.out.println("Model failed to return valid JSON. Raw output:");
                System.out.println(cleanJson);
                return cleanJson;
            }

        } else {
            throw new Exception("Error: " + response.statusCode() + " - " + response.body());
        }
    }
}