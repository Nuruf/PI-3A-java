package service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class MatchingService {

    private static final String API_URL;
    private static final String AUTH_TOKEN;

    static {
        Properties props = new Properties();
        try (InputStream input = MatchingService.class.getResourceAsStream("/config.properties")) {
            if (input == null) {
                throw new RuntimeException("Unable to find config.properties");
            }
            props.load(input);
            API_URL = props.getProperty("huggingface.api.url");
            AUTH_TOKEN = props.getProperty("huggingface.api.token");

            if (API_URL == null || AUTH_TOKEN == null) {
                throw new RuntimeException("API credentials not found in config.properties");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties: " + e.getMessage(), e);
        }
    }

    public static String cleanText(String text) {
        if (text == null) return "";
        text = text.replaceAll("\\S+@\\S+", ""); // Remove emails
        text = text.replaceAll("http\\S+", ""); // Remove URLs
        text = text.replaceAll("[^a-zA-Z+#.\\s]", " "); // Keep technical chars like C++, C#, .NET
        text = text.trim().replaceAll("\\s+", " "); // Remove extra whitespaces
        return text;
    }

    public String extractTextFromPDF(byte[] data) {
        try (PDDocument document = PDDocument.load(data)) {
            String temp = new PDFTextStripper().getText(document);
            return cleanText(temp);
        } catch (IOException e) {
            System.err.println("Error reading PDF: " + e.getMessage());
            return "";
        }
    }

    public String extractHtml(String html) {
        if (html == null || html.isEmpty()) return "";
        return cleanText(Jsoup.parse(html).text());
    }

    public String getMatchScore(String cvText, String jobText) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            // Prepare the JSON body
            String jsonBody = "{"
                    + "\"inputs\": {"
                    + "\"source_sentence\": \"" + escapeJson(jobText) + "\","
                    + "\"sentences\": [\"" + escapeJson(cvText) + "\"]"
                    + "}"
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Authorization", "Bearer " + AUTH_TOKEN)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return response.body();
            } else {
                return "[0.0]";
            }
        } catch (Exception e) {
            return "[0.0]";
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}