package service.api;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class AIDocumentGeneratorService {

    private static final String API_URL = "http://localhost:11434/api/chat";
    private static final String MODEL = "llama3";
    private static final int TIMEOUT_CONNECT = 30000;
    private static final int TIMEOUT_READ = 120000;

    // ═══════════════════════════════════════════════════════════════════════════
    // MOMENTUM COLOR PALETTE
    // ═══════════════════════════════════════════════════════════════════════════

    private static final BaseColor ACCENT_PRIMARY = new BaseColor(74, 93, 239);
    private static final BaseColor ACCENT_LIGHT = new BaseColor(192, 224, 255);
    private static final BaseColor ACCENT_BG = new BaseColor(240, 245, 255);
    private static final BaseColor TEXT_DARK = new BaseColor(44, 62, 80);
    private static final BaseColor TEXT_MUTED = new BaseColor(107, 114, 128);
    private static final BaseColor TEXT_LIGHT = new BaseColor(153, 153, 153);

    // ═══════════════════════════════════════════════════════════════════════════
    // MAIN GENERATION METHOD
    // ═══════════════════════════════════════════════════════════════════════════

    public CompletableFuture<GeneratedDocument> generateDocumentAsync(
            String type, String employeeName, String position,
            String employeeID, Date hireDate, String additionalInfo) {

        return CompletableFuture.supplyAsync(() -> {
            System.out.println("═══════════════════════════════════════════════");
            System.out.println("🚀 Generating document...");
            System.out.println("   Type: " + type);
            System.out.println("   Employee: " + employeeName);
            System.out.println("═══════════════════════════════════════════════");

            if (isOllamaAvailable()) {
                try {
                    String prompt = buildPrompt(type, employeeName, position, employeeID, additionalInfo);
                    String aiResponse = callOllama(prompt);

                    if (aiResponse != null && !aiResponse.trim().isEmpty()) {
                        GeneratedDocument doc = parseAIResponse(aiResponse, type, employeeName);
                        if (doc != null && doc.isValid) {
                            System.out.println("✅ AI document generated!");
                            return doc;
                        }
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ AI failed, using template: " + e.getMessage());
                }
            }

            System.out.println("📝 Using template document...");
            return createTemplateDocument(type, employeeName, position, employeeID, additionalInfo);
        });
    }

    private boolean isOllamaAvailable() {
        try {
            URL url = new URL("http://localhost:11434/api/tags");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            int code = conn.getResponseCode();
            conn.disconnect();
            return code == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private String buildPrompt(String type, String name, String position, String id, String info) {
        String today = new SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH).format(new Date());
        String refDate = new SimpleDateFormat("yyyyMMdd").format(new Date());

        return "Vous êtes un assistant RH professionnel. Générez un document officiel en français.\n\n" +
                "RÈGLES IMPORTANTES:\n" +
                "1. Ne JAMAIS inclure de JSON brut dans le corps du document\n" +
                "2. Formater les informations de manière professionnelle et lisible\n" +
                "3. Utiliser un ton formel et courtois\n" +
                "4. Le document doit être prêt à être imprimé\n\n" +
                "Répondez UNIQUEMENT avec un JSON valide (pas de texte avant ou après):\n\n" +
                "{\n" +
                "  \"title\": \"TITRE DU DOCUMENT EN MAJUSCULES\",\n" +
                "  \"reference\": \"REF/RH/" + refDate + "/001\",\n" +
                "  \"recipient\": \"À qui de droit\",\n" +
                "  \"subject\": \"Objet court et clair\",\n" +
                "  \"body\": \"Corps du document avec paragraphes séparés par \\n\\n. " +
                "Formater les détails en liste avec des tirets. " +
                "Ne PAS inclure de JSON. Écrire de manière professionnelle.\",\n" +
                "  \"closing\": \"Cordialement,\",\n" +
                "  \"signature\": \"Service des Ressources Humaines\",\n" +
                "  \"footer\": \"Document généré électroniquement - Valide sans signature\"\n" +
                "}\n\n" +
                "═══════════════════════════════════════════════════════════\n" +
                "INFORMATIONS POUR LE DOCUMENT:\n" +
                "═══════════════════════════════════════════════════════════\n" +
                "Type de demande: " + type + "\n" +
                "Employé: " + name + "\n" +
                "Poste: " + position + "\n" +
                "ID Employé: " + id + "\n" +
                "Date: " + today + "\n" +
                "Détails supplémentaires:\n" + (info != null && !info.isEmpty() ? info : "Aucun") + "\n" +
                "═══════════════════════════════════════════════════════════\n\n" +
                "Générez le JSON du document:";
    }

    private String callOllama(String prompt) throws Exception {
        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        conn.setConnectTimeout(TIMEOUT_CONNECT);
        conn.setReadTimeout(TIMEOUT_READ);

        JSONObject body = new JSONObject();
        body.put("model", MODEL);
        body.put("stream", false);

        JSONObject options = new JSONObject();
        options.put("temperature", 0.3);
        body.put("options", options);

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system")
                .put("content", "Réponds uniquement en JSON valide."));
        messages.put(new JSONObject().put("role", "user").put("content", prompt));
        body.put("messages", messages);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        if (conn.getResponseCode() != 200) {
            conn.disconnect();
            return null;
        }

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        conn.disconnect();

        JSONObject json = new JSONObject(response.toString());
        return json.getJSONObject("message").getString("content");
    }

    private GeneratedDocument parseAIResponse(String aiResponse, String fallbackTitle, String employeeName) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) return null;

        try {
            String cleaned = aiResponse.trim()
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            int start = cleaned.indexOf("{");
            int end = cleaned.lastIndexOf("}");

            if (start == -1 || end == -1 || end <= start) return null;

            String jsonStr = cleaned.substring(start, end + 1);
            JSONObject json = new JSONObject(jsonStr);

            GeneratedDocument doc = new GeneratedDocument();
            doc.title = json.optString("title", fallbackTitle);
            doc.reference = json.optString("reference",
                    "REF/RH/" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + "/001");
            doc.header = json.optString("header", "").replace("\\n", "\n");
            doc.recipient = json.optString("recipient", "À qui de droit");
            doc.subject = json.optString("subject", doc.title);
            doc.body = json.optString("body", "").replace("\\n", "\n");
            doc.closing = json.optString("closing", "Cordialement,");
            doc.signature = json.optString("signature", "Service RH").replace("\\n", "\n");
            doc.footer = json.optString("footer", "Document généré électroniquement");
            doc.employeeName = employeeName;
            doc.generatedDate = new Date();
            doc.isValid = !doc.body.isEmpty();

            return doc.isValid ? doc : null;

        } catch (Exception e) {
            System.err.println("⚠️ Error parsing AI JSON: " + e.getMessage());
            return null;
        }
    }

    private GeneratedDocument createTemplateDocument(String type, String name,
                                                     String position, String id, String info) {
        String today = new SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH).format(new Date());
        String refDate = new SimpleDateFormat("yyyyMMdd").format(new Date());

        GeneratedDocument doc = new GeneratedDocument();
        doc.employeeName = name;
        doc.generatedDate = new Date();
        doc.reference = "REF/RH/" + refDate + "/001";
        doc.header = "MOMENTUM HR\n123 Avenue Principale\nTunis, Tunisie\nTél: +216 XX XXX XXX";
        doc.recipient = "À qui de droit";
        doc.closing = "Cordialement,";
        doc.signature = "Le Directeur des Ressources Humaines\n\n[Signature]\n\nService RH";
        doc.footer = "Document généré par Momentum HR - Valide sans signature";
        doc.isValid = true;

        // Format the additional info (remove raw JSON)
        String formattedInfo = formatInfoForDocument(info);

        String typeLower = type != null ? type.toLowerCase() : "";

        if (typeLower.contains("attestation") || typeLower.contains("travail")) {
            doc.title = "ATTESTATION DE TRAVAIL";
            doc.subject = "Attestation de Travail";
            doc.body = "Je soussigné(e), Directeur des Ressources Humaines de MOMENTUM HR, " +
                    "atteste par la présente que:\n\n" +
                    "Nom et Prénom: " + name + "\n" +
                    "Poste occupé: " + position + "\n" +
                    "Numéro d'employé: " + id + "\n\n" +
                    "travaille au sein de notre entreprise.\n\n" +
                    (!formattedInfo.isEmpty() ? "Informations complémentaires:\n" + formattedInfo + "\n\n" : "") +
                    "Cette attestation est délivrée à l'intéressé(e) pour servir et valoir ce que de droit.\n\n" +
                    "Fait à Tunis, le " + today;

        } else if (typeLower.contains("congé") || typeLower.contains("conge")) {
            doc.title = "AUTORISATION DE CONGÉ";
            doc.subject = "Autorisation de Congé";
            doc.body = "Suite à la demande de congé soumise par:\n\n" +
                    "Nom et Prénom: " + name + "\n" +
                    "Poste: " + position + "\n" +
                    "Numéro d'employé: " + id + "\n\n" +
                    "Nous avons le plaisir de vous informer que votre demande a été approuvée.\n\n" +
                    (!formattedInfo.isEmpty() ? "Détails du congé:\n" + formattedInfo + "\n\n" : "") +
                    "Nous vous souhaitons un excellent repos.\n\n" +
                    "Fait à Tunis, le " + today;

        } else if (typeLower.contains("logiciel") || typeLower.contains("licence")) {
            doc.title = "DEMANDE DE LICENCE LOGICIELLE";
            doc.subject = "Demande de Licence Logicielle";
            doc.body = "Concernant la demande de licence logicielle soumise par:\n\n" +
                    "Nom et Prénom: " + name + "\n" +
                    "Poste: " + position + "\n" +
                    "Numéro d'employé: " + id + "\n\n" +
                    "Nous accusons réception de votre demande.\n\n" +
                    (!formattedInfo.isEmpty() ? "Détails de la demande:\n" + formattedInfo + "\n\n" : "") +
                    "Votre demande sera examinée et traitée dans les plus brefs délais.\n\n" +
                    "Fait à Tunis, le " + today;

        } else if (typeLower.contains("équipement") || typeLower.contains("equipement") || typeLower.contains("materiel")) {
            doc.title = "DEMANDE D'ÉQUIPEMENT";
            doc.subject = "Demande d'Équipement";
            doc.body = "Concernant la demande d'équipement soumise par:\n\n" +
                    "Nom et Prénom: " + name + "\n" +
                    "Poste: " + position + "\n" +
                    "Numéro d'employé: " + id + "\n\n" +
                    "Nous accusons réception de votre demande.\n\n" +
                    (!formattedInfo.isEmpty() ? "Détails de la demande:\n" + formattedInfo + "\n\n" : "") +
                    "Votre demande sera traitée dans les plus brefs délais.\n\n" +
                    "Fait à Tunis, le " + today;

        } else {
            doc.title = type != null ? type.toUpperCase() : "DOCUMENT RH";
            doc.subject = type != null ? type : "Document RH";
            doc.body = "Document généré pour:\n\n" +
                    "Nom et Prénom: " + name + "\n" +
                    "Poste: " + position + "\n" +
                    "Numéro d'employé: " + id + "\n\n" +
                    "Type de demande: " + (type != null ? type : "Document") + "\n\n" +
                    (!formattedInfo.isEmpty() ? "Informations:\n" + formattedInfo + "\n\n" : "") +
                    "Fait à Tunis, le " + today;
        }

        doc.summary = doc.title + " pour " + name;
        return doc;
    }

    /**
     * Formats info string, removing any raw JSON and making it readable
     */
    private String formatInfoForDocument(String info) {
        if (info == null || info.trim().isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        String[] lines = info.split("\n");

        for (String line : lines) {
            String trimmed = line.trim();

            // Skip empty lines
            if (trimmed.isEmpty()) continue;

            // Skip JSON braces
            if (trimmed.equals("{") || trimmed.equals("}") ||
                    trimmed.equals("[") || trimmed.equals("]")) continue;

            // Check if line contains JSON-like content
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                // Try to parse and format JSON
                try {
                    org.json.JSONObject json = new org.json.JSONObject(trimmed);
                    for (String key : json.keySet()) {
                        String value = json.optString(key, "");
                        if (!value.isEmpty()) {
                            result.append("- ").append(formatKey(key)).append(": ").append(value).append("\n");
                        }
                    }
                    continue;
                } catch (Exception e) {
                    // Not valid JSON, continue
                }
            }

            // Check for JSON key-value pattern like "key":"value"
            if (trimmed.matches("\"[^\"]+\"\\s*:\\s*\"[^\"]*\".*")) {
                // Extract key and value
                try {
                    String cleanLine = trimmed.replaceAll(",$", ""); // Remove trailing comma
                    String jsonObj = "{" + cleanLine + "}";
                    org.json.JSONObject json = new org.json.JSONObject(jsonObj);
                    for (String key : json.keySet()) {
                        String value = json.optString(key, "");
                        if (!value.isEmpty()) {
                            result.append("- ").append(formatKey(key)).append(": ").append(value).append("\n");
                        }
                    }
                    continue;
                } catch (Exception e) {
                    // Continue to normal processing
                }
            }

            // Normal line - add it
            if (!trimmed.startsWith("\"") && !trimmed.contains("\":")) {
                result.append(trimmed).append("\n");
            }
        }

        return result.toString().trim();
    }

    /**
     * Formats a key into readable text
     */
    private String formatKey(String key) {
        if (key == null || key.isEmpty()) return "";

        // Common translations
        switch (key) {
            case "nomLogiciel": return "Nom du logiciel";
            case "version": return "Version";
            case "typeLicence": return "Type de licence";
            case "justificationLogiciel": return "Justification";
            case "dateDebut": return "Date de début";
            case "dateFin": return "Date de fin";
            case "nombreJours": return "Nombre de jours";
            case "typeConge": return "Type de congé";
            case "motif": return "Motif";
            case "description": return "Description";
            case "quantite": return "Quantité";
            case "urgence": return "Urgence";
            case "justification": return "Justification";
            case "nomEquipement": return "Nom de l'équipement";
            case "typeEquipement": return "Type d'équipement";
            case "marque": return "Marque";
            case "modele": return "Modèle";
            case "specifications": return "Spécifications";
            case "raisonDemande": return "Raison de la demande";
            default:
                // Convert camelCase to readable
                return key.replaceAll("([a-z])([A-Z])", "$1 $2")
                        .replaceAll("_", " ")
                        .substring(0, 1).toUpperCase() +
                        key.replaceAll("([a-z])([A-Z])", "$1 $2")
                                .replaceAll("_", " ")
                                .substring(1);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EXPORT BOTH FORMATS - MAIN METHOD (SYNCHRONOUS - SEQUENTIAL)
    // ═══════════════════════════════════════════════════════════════════════════

    public ExportResult exportBothFormats(GeneratedDocument doc, String pdfPath, String wordPath) {
        ExportResult result = new ExportResult();

        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║ 📤 EXPORTING DOCUMENTS                                        ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════╣");
        System.out.println("║ PDF Path:  " + pdfPath);
        System.out.println("║ Word Path: " + wordPath);
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");

        // ═══════════════════════════════════════════════════════════════
        // STEP 1: EXPORT PDF
        // ═══════════════════════════════════════════════════════════════
        System.out.println("\n📄 [STEP 1/2] Exporting PDF...");
        try {
            result.pdfFile = createPDF(doc, pdfPath);
            if (result.pdfFile != null && result.pdfFile.exists() && result.pdfFile.length() > 0) {
                result.pdfSuccess = true;
                System.out.println("✅ PDF SUCCESS: " + result.pdfFile.getAbsolutePath());
                System.out.println("   Size: " + result.pdfFile.length() + " bytes");
            } else {
                result.pdfError = "PDF file was not created or is empty";
                System.err.println("❌ PDF FAILED: File not created or empty");
            }
        } catch (Exception e) {
            result.pdfError = e.getMessage();
            System.err.println("❌ PDF ERROR: " + e.getMessage());
            e.printStackTrace();
        }

        // ═══════════════════════════════════════════════════════════════
        // STEP 2: EXPORT WORD
        // ═══════════════════════════════════════════════════════════════
        System.out.println("\n📝 [STEP 2/2] Exporting Word...");
        try {
            result.wordFile = createWord(doc, wordPath);
            if (result.wordFile != null && result.wordFile.exists() && result.wordFile.length() > 0) {
                result.wordSuccess = true;
                System.out.println("✅ WORD SUCCESS: " + result.wordFile.getAbsolutePath());
                System.out.println("   Size: " + result.wordFile.length() + " bytes");
            } else {
                result.wordError = "Word file was not created or is empty";
                System.err.println("❌ WORD FAILED: File not created or empty");
            }
        } catch (Exception e) {
            result.wordError = e.getMessage();
            System.err.println("❌ WORD ERROR: " + e.getMessage());
            e.printStackTrace();
        }

        // ═══════════════════════════════════════════════════════════════
        // SUMMARY
        // ═══════════════════════════════════════════════════════════════
        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║ 📊 EXPORT SUMMARY                                             ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════╣");
        System.out.println("║ PDF:  " + (result.pdfSuccess ? "✅ SUCCESS" : "❌ FAILED - " + result.pdfError));
        System.out.println("║ Word: " + (result.wordSuccess ? "✅ SUCCESS" : "❌ FAILED - " + result.wordError));
        System.out.println("╚═══════════════════════════════════════════════════════════════╝\n");

        return result;
    }

    public CompletableFuture<ExportResult> exportBothFormatsAsync(GeneratedDocument doc, String pdfPath, String wordPath) {
        return CompletableFuture.supplyAsync(() -> exportBothFormats(doc, pdfPath, wordPath));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CREATE PDF - SYNCHRONOUS
    // ═══════════════════════════════════════════════════════════════════════════

    private File createPDF(GeneratedDocument doc, String path) throws Exception {
        File pdfFile = new File(path);

        // Create parent directories
        if (pdfFile.getParentFile() != null && !pdfFile.getParentFile().exists()) {
            boolean created = pdfFile.getParentFile().mkdirs();
            System.out.println("   Created directories: " + created);
        }

        com.itextpdf.text.Document pdf = new com.itextpdf.text.Document(PageSize.A4, 50, 50, 50, 50);
        FileOutputStream fos = new FileOutputStream(pdfFile);

        try {
            PdfWriter.getInstance(pdf, fos);
            pdf.open();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, ACCENT_PRIMARY);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA, 10, TEXT_MUTED);
            Font subjectFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, TEXT_DARK);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 11, TEXT_DARK);
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, TEXT_LIGHT);
            Font accentFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, ACCENT_PRIMARY);

            // HEADER BAR
            PdfPTable headerBar = new PdfPTable(1);
            headerBar.setWidthPercentage(100);

            PdfPCell headerCell = new PdfPCell();
            headerCell.setBackgroundColor(ACCENT_PRIMARY);
            headerCell.setPadding(15);
            headerCell.setBorder(Rectangle.NO_BORDER);

            Paragraph headerText = new Paragraph();
            Font headerTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.WHITE);
            Font headerSubFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new BaseColor(200, 220, 255));

            headerText.add(new Chunk("MOMENTUM HR\n", headerTitleFont));
            headerText.add(new Chunk("Système de Gestion des Ressources Humaines", headerSubFont));
            headerCell.addElement(headerText);
            headerBar.addCell(headerCell);

            pdf.add(headerBar);
            pdf.add(Chunk.NEWLINE);

            // INFO BOX
            PdfPTable infoBox = new PdfPTable(2);
            infoBox.setWidthPercentage(100);
            infoBox.setWidths(new float[]{1, 1});

            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.setPadding(10);
            leftCell.setBackgroundColor(ACCENT_BG);

            Paragraph refPara = new Paragraph();
            refPara.add(new Chunk("Référence: ", accentFont));
            refPara.add(new Chunk(doc.reference, bodyFont));
            leftCell.addElement(refPara);
            infoBox.addCell(leftCell);

            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setPadding(10);
            rightCell.setBackgroundColor(ACCENT_BG);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

            String dateStr = new SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH).format(doc.generatedDate);
            Paragraph datePara = new Paragraph("Tunis, le " + dateStr, headerFont);
            datePara.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(datePara);
            infoBox.addCell(rightCell);

            pdf.add(infoBox);
            pdf.add(Chunk.NEWLINE);
            pdf.add(Chunk.NEWLINE);

            // TITLE
            Paragraph title = new Paragraph(doc.title, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            pdf.add(title);

            // UNDERLINE
            PdfPTable line = new PdfPTable(1);
            line.setWidthPercentage(40);
            PdfPCell lineCell = new PdfPCell();
            lineCell.setBorder(Rectangle.BOTTOM);
            lineCell.setBorderColor(ACCENT_PRIMARY);
            lineCell.setBorderWidth(2);
            lineCell.setFixedHeight(5);
            line.addCell(lineCell);
            pdf.add(line);
            pdf.add(Chunk.NEWLINE);

            // RECIPIENT
            if (doc.recipient != null && !doc.recipient.isEmpty()) {
                Paragraph recipient = new Paragraph(doc.recipient, subjectFont);
                pdf.add(recipient);
                pdf.add(Chunk.NEWLINE);
            }

            // SUBJECT
            if (doc.subject != null && !doc.subject.isEmpty() && !doc.subject.equals(doc.title)) {
                Paragraph subject = new Paragraph();
                subject.add(new Chunk("Objet: ", accentFont));
                subject.add(new Chunk(doc.subject, bodyFont));
                pdf.add(subject);
                pdf.add(Chunk.NEWLINE);
            }

            // BODY
            if (doc.body != null && !doc.body.isEmpty()) {
                String[] paragraphs = doc.body.split("\n\n");
                for (String para : paragraphs) {
                    String[] lines = para.split("\n");
                    for (String bodyLine : lines) {
                        if (!bodyLine.trim().isEmpty()) {
                            Paragraph p = new Paragraph(bodyLine.trim(), bodyFont);
                            p.setAlignment(Element.ALIGN_JUSTIFIED);
                            p.setSpacingAfter(3);
                            pdf.add(p);
                        }
                    }
                    pdf.add(Chunk.NEWLINE);
                }
            }

            pdf.add(Chunk.NEWLINE);

            // CLOSING
            if (doc.closing != null && !doc.closing.isEmpty()) {
                Paragraph closing = new Paragraph(doc.closing, bodyFont);
                pdf.add(closing);
                pdf.add(Chunk.NEWLINE);
            }

            // SIGNATURE
            if (doc.signature != null && !doc.signature.isEmpty()) {
                pdf.add(Chunk.NEWLINE);
                for (String sigLine : doc.signature.split("\n")) {
                    Paragraph p = new Paragraph(sigLine.trim(), bodyFont);
                    p.setAlignment(Element.ALIGN_RIGHT);
                    pdf.add(p);
                }
            }

            // FOOTER BAR
            pdf.add(Chunk.NEWLINE);
            pdf.add(Chunk.NEWLINE);

            PdfPTable footerBar = new PdfPTable(1);
            footerBar.setWidthPercentage(100);

            PdfPCell footerCell = new PdfPCell();
            footerCell.setBackgroundColor(ACCENT_BG);
            footerCell.setPadding(10);
            footerCell.setBorder(Rectangle.TOP);
            footerCell.setBorderColor(ACCENT_LIGHT);

            Paragraph footerText = new Paragraph(doc.footer, footerFont);
            footerText.setAlignment(Element.ALIGN_CENTER);
            footerCell.addElement(footerText);
            footerBar.addCell(footerCell);

            pdf.add(footerBar);

        } finally {
            if (pdf.isOpen()) {
                pdf.close();
            }
            fos.close();
        }

        return pdfFile;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CREATE WORD - SYNCHRONOUS
    // ═══════════════════════════════════════════════════════════════════════════

    private File createWord(GeneratedDocument doc, String path) throws Exception {
        File wordFile = new File(path);

        // Create parent directories
        if (wordFile.getParentFile() != null && !wordFile.getParentFile().exists()) {
            boolean created = wordFile.getParentFile().mkdirs();
            System.out.println("   Created directories: " + created);
        }

        XWPFDocument word = new XWPFDocument();
        FileOutputStream fos = new FileOutputStream(wordFile);

        try {
            // Header
            XWPFParagraph headerPara = word.createParagraph();
            headerPara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun headerRun = headerPara.createRun();
            headerRun.setText("MOMENTUM HR");
            headerRun.setBold(true);
            headerRun.setFontSize(16);
            headerRun.setColor("4A5DEF");
            headerRun.addBreak();

            XWPFRun subRun = headerPara.createRun();
            subRun.setText("Système de Gestion des Ressources Humaines");
            subRun.setFontSize(10);
            subRun.setColor("6b7280");
            subRun.addBreak();
            subRun.addBreak();

            // Reference
            XWPFParagraph infoPara = word.createParagraph();
            XWPFRun infoRun = infoPara.createRun();
            infoRun.setText("Réf: " + doc.reference);
            infoRun.setFontSize(10);
            infoRun.setColor("4A5DEF");
            infoRun.addBreak();

            // Date
            XWPFParagraph datePara = word.createParagraph();
            datePara.setAlignment(ParagraphAlignment.RIGHT);
            XWPFRun dateRun = datePara.createRun();
            String dateStr = new SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH).format(doc.generatedDate);
            dateRun.setText("Tunis, le " + dateStr);
            dateRun.setFontSize(10);
            dateRun.setColor("6b7280");
            dateRun.addBreak();
            dateRun.addBreak();

            // Title
            XWPFParagraph titlePara = word.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText(doc.title);
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.setColor("4A5DEF");
            titleRun.addBreak();
            titleRun.addBreak();

            // Recipient
            if (doc.recipient != null && !doc.recipient.isEmpty()) {
                XWPFParagraph recipientPara = word.createParagraph();
                XWPFRun recipientRun = recipientPara.createRun();
                recipientRun.setText(doc.recipient);
                recipientRun.setBold(true);
                recipientRun.setFontSize(11);
                recipientRun.addBreak();
            }

            // Subject
            if (doc.subject != null && !doc.subject.isEmpty() && !doc.subject.equals(doc.title)) {
                XWPFParagraph subjectPara = word.createParagraph();
                XWPFRun subjectLabel = subjectPara.createRun();
                subjectLabel.setText("Objet: ");
                subjectLabel.setBold(true);
                subjectLabel.setFontSize(11);
                subjectLabel.setColor("4A5DEF");

                XWPFRun subjectText = subjectPara.createRun();
                subjectText.setText(doc.subject);
                subjectText.setFontSize(11);
                subjectText.addBreak();
            }

            // Body
            if (doc.body != null && !doc.body.isEmpty()) {
                XWPFParagraph bodyPara = word.createParagraph();
                bodyPara.setAlignment(ParagraphAlignment.BOTH);

                String[] lines = doc.body.split("\n");
                for (String line : lines) {
                    XWPFRun bodyRun = bodyPara.createRun();
                    bodyRun.setText(line);
                    bodyRun.setFontSize(11);
                    bodyRun.addBreak();
                }
            }

            // Closing
            if (doc.closing != null && !doc.closing.isEmpty()) {
                word.createParagraph();
                XWPFParagraph closingPara = word.createParagraph();
                XWPFRun closingRun = closingPara.createRun();
                closingRun.setText(doc.closing);
                closingRun.setFontSize(11);
                closingRun.addBreak();
            }

            // Signature
            if (doc.signature != null && !doc.signature.isEmpty()) {
                XWPFParagraph sigPara = word.createParagraph();
                sigPara.setAlignment(ParagraphAlignment.RIGHT);
                String[] sigLines = doc.signature.split("\n");
                for (String sigLine : sigLines) {
                    XWPFRun sigRun = sigPara.createRun();
                    sigRun.setText(sigLine.trim());
                    sigRun.setFontSize(11);
                    sigRun.addBreak();
                }
            }

            // Footer
            if (doc.footer != null && !doc.footer.isEmpty()) {
                word.createParagraph();
                word.createParagraph();
                XWPFParagraph footerPara = word.createParagraph();
                footerPara.setAlignment(ParagraphAlignment.CENTER);
                XWPFRun footerRun = footerPara.createRun();
                footerRun.setText("────────────────────────────────────────");
                footerRun.addBreak();
                footerRun.setText(doc.footer);
                footerRun.setFontSize(9);
                footerRun.setItalic(true);
                footerRun.setColor("999999");
            }

            // Write to file
            word.write(fos);

        } finally {
            fos.close();
            word.close();
        }

        return wordFile;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ASYNC WRAPPERS (for manual export buttons)
    // ═══════════════════════════════════════════════════════════════════════════

    public CompletableFuture<File> exportToPDFAsync(GeneratedDocument doc, String path) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return createPDF(doc, path);
            } catch (Exception e) {
                System.err.println("❌ PDF export error: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        });
    }

    public CompletableFuture<File> exportToWordAsync(GeneratedDocument doc, String path) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return createWord(doc, path);
            } catch (Exception e) {
                System.err.println("❌ Word export error: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EXPORT RESULT CLASS
    // ═══════════════════════════════════════════════════════════════════════════

    public static class ExportResult {
        public boolean pdfSuccess = false;
        public boolean wordSuccess = false;
        public File pdfFile = null;
        public File wordFile = null;
        public String pdfError = null;
        public String wordError = null;

        public boolean isFullySuccessful() {
            return pdfSuccess && wordSuccess;
        }

        public boolean isPartiallySuccessful() {
            return pdfSuccess || wordSuccess;
        }

        @Override
        public String toString() {
            return "ExportResult{PDF=" + (pdfSuccess ? "✅" : "❌") +
                    ", Word=" + (wordSuccess ? "✅" : "❌") + "}";
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GENERATED DOCUMENT CLASS
    // ═══════════════════════════════════════════════════════════════════════════

    public static class GeneratedDocument {
        public boolean isValid = false;
        public String title = "";
        public String reference = "";
        public String header = "";
        public String recipient = "";
        public String subject = "";
        public String body = "";
        public String closing = "";
        public String signature = "";
        public String footer = "";
        public String summary = "";
        public Date generatedDate;
        public String employeeName = "";

        public String getFormattedText() {
            StringBuilder sb = new StringBuilder();

            sb.append("╔══════════════════════════════════════════════════════╗\n");
            sb.append("║             MOMENTUM HR - DOCUMENT OFFICIEL          ║\n");
            sb.append("╚══════════════════════════════════════════════════════╝\n\n");

            if (reference != null && !reference.isEmpty()) {
                sb.append("Réf: ").append(reference).append("\n\n");
            }

            sb.append("═══════════════════════════════════════════════════════\n");
            sb.append("  ").append(title).append("\n");
            sb.append("═══════════════════════════════════════════════════════\n\n");

            if (recipient != null && !recipient.isEmpty()) {
                sb.append(recipient).append("\n\n");
            }

            if (subject != null && !subject.isEmpty() && !subject.equals(title)) {
                sb.append("Objet: ").append(subject).append("\n\n");
            }

            if (body != null && !body.isEmpty()) {
                sb.append(body).append("\n\n");
            }

            if (closing != null && !closing.isEmpty()) {
                sb.append(closing).append("\n\n");
            }

            if (signature != null && !signature.isEmpty()) {
                sb.append(signature).append("\n");
            }

            if (footer != null && !footer.isEmpty()) {
                sb.append("\n───────────────────────────────────────────────────────\n");
                sb.append(footer);
            }

            return sb.toString();
        }
    }
}