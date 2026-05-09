package controller.components;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import service.api.AIDocumentGeneratorService;
import service.api.AIDocumentGeneratorService.GeneratedDocument;
import service.api.AIDocumentGeneratorService.ExportResult;

import java.io.File;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DocumentGeneratorPanel extends VBox {

    private AIDocumentGeneratorService docService;
    private TextArea documentPreview;
    private Label statusLabel;
    private Label folderLabel;
    private ProgressIndicator loadingIndicator;
    private Button exportPDFButton;
    private Button exportWordButton;
    private Button copyButton;
    private GeneratedDocument currentDocument;
    private File selectedFolder;

    public DocumentGeneratorPanel() {
        try {
            docService = new AIDocumentGeneratorService();

            String userHome = System.getProperty("user.home");
            selectedFolder = new File(userHome, "Downloads");
            if (!selectedFolder.exists()) selectedFolder = new File(userHome, "Documents");
            if (!selectedFolder.exists()) selectedFolder = new File(userHome);

            setupUI();

            System.out.println("✅ DocumentGeneratorPanel initialized");
            System.out.println("📁 Default folder: " + selectedFolder.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("❌ DocumentGeneratorPanel init error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupUI() {
        setSpacing(15);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                "-fx-border-color: #e0e0e0; -fx-border-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("📄");
        icon.setStyle("-fx-font-size: 28;");

        VBox titleBox = new VBox(2);
        Label title = new Label("Générateur de Documents");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        statusLabel = new Label("Prêt à générer");
        statusLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11;");
        titleBox.getChildren().addAll(title, statusLabel);

        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setPrefSize(24, 24);
        loadingIndicator.setVisible(false);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(icon, titleBox, spacer, loadingIndicator);

        // Folder selection
        HBox folderBox = new HBox(10);
        folderBox.setAlignment(Pos.CENTER_LEFT);
        folderBox.setPadding(new Insets(12));
        folderBox.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-border-color: #dee2e6; -fx-border-radius: 8;");

        Button chooseFolderBtn = createStyledButton("📁 Choisir dossier", "#4A5DEF", "#3944D5");
        chooseFolderBtn.setOnAction(e -> chooseFolder());

        folderLabel = new Label(selectedFolder != null ? "📂 " + selectedFolder.getName() : "Aucun dossier");
        folderLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 12;");
        folderLabel.setWrapText(true);
        HBox.setHgrow(folderLabel, Priority.ALWAYS);

        folderBox.getChildren().addAll(chooseFolderBtn, folderLabel);

        // Separator
        Separator sep1 = new Separator();

        // Preview
        Label previewLabel = new Label("📋 Aperçu du document");
        previewLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #555; -fx-font-size: 13;");

        documentPreview = new TextArea();
        documentPreview.setWrapText(true);
        documentPreview.setPrefRowCount(15);
        documentPreview.setEditable(false);
        documentPreview.setStyle("-fx-font-family: 'Consolas', 'Monaco', 'Courier New', monospace; " +
                "-fx-font-size: 11; -fx-control-inner-background: #f8f9fa;");
        documentPreview.setPromptText("Le document généré apparaîtra ici...");
        VBox.setVgrow(documentPreview, Priority.ALWAYS);

        // Buttons
        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER);
        buttons.setPadding(new Insets(10, 0, 0, 0));

        exportPDFButton = createStyledButton("📥 Exporter PDF", "#e74c3c", "#c0392b");
        exportPDFButton.setDisable(true);
        exportPDFButton.setOnAction(e -> exportPDF());

        exportWordButton = createStyledButton("📄 Exporter Word", "#2185d0", "#1678c2");
        exportWordButton.setDisable(true);
        exportWordButton.setOnAction(e -> exportWord());

        copyButton = createStyledButton("📋 Copier", "#95a5a6", "#7f8c8d");
        copyButton.setDisable(true);
        copyButton.setOnAction(e -> copyText());

        buttons.getChildren().addAll(exportPDFButton, exportWordButton, copyButton);

        getChildren().addAll(header, folderBox, sep1, previewLabel, documentPreview, buttons);
    }

    private Button createStyledButton(String text, String normalColor, String hoverColor) {
        Button btn = new Button(text);
        String normalStyle = "-fx-background-color: " + normalColor + "; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 10 20; -fx-font-weight: bold; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 2);";
        String hoverStyle = "-fx-background-color: " + hoverColor + "; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 10 20; -fx-font-weight: bold; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 6, 0, 0, 3);";

        btn.setStyle(normalStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(normalStyle));

        return btn;
    }

    private void chooseFolder() {
        try {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Choisir le dossier de destination");

            if (selectedFolder != null && selectedFolder.exists()) {
                chooser.setInitialDirectory(selectedFolder);
            }

            File folder = chooser.showDialog(getScene().getWindow());

            if (folder != null && folder.exists() && folder.isDirectory()) {
                selectedFolder = folder;
                folderLabel.setText("📂 " + folder.getName());
                folderLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 12;");
                updateStatus("📁 Dossier: " + folder.getName(), "#27ae60");
                System.out.println("✅ Folder selected: " + folder.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
            updateStatus("❌ Erreur lors de la sélection du dossier", "#e74c3c");
        }
    }

    /**
     * Main method called to generate and export document
     */
    public void generateDocument(String type, String name, String position, String id, Date date, String info) {
        System.out.println("╔═══════════════════════════════════════════════════════╗");
        System.out.println("║ 📄 DOCUMENT GENERATION REQUEST                        ║");
        System.out.println("╠═══════════════════════════════════════════════════════╣");
        System.out.println("║ Type: " + type);
        System.out.println("║ Employee: " + name);
        System.out.println("║ Folder: " + (selectedFolder != null ? selectedFolder.getAbsolutePath() : "null"));
        System.out.println("╚═══════════════════════════════════════════════════════╝");

        if (selectedFolder == null || !selectedFolder.exists()) {
            updateStatus("⚠️ Dossier invalide", "#e74c3c");
            showAlert(Alert.AlertType.WARNING, "Dossier requis",
                    "Aucun dossier sélectionné",
                    "Veuillez choisir un dossier de destination avant de générer le document.");
            return;
        }

        if (docService == null) {
            updateStatus("❌ Service non initialisé", "#e74c3c");
            return;
        }

        updateStatus("🔄 Génération en cours...", "#f39c12");
        loadingIndicator.setVisible(true);
        disableButtons(true);

        // Generate document async
        docService.generateDocumentAsync(type, name, position, id, date, info)
                .thenAccept(doc -> {
                    if (doc != null && doc.isValid) {
                        // Document generated, now export
                        Platform.runLater(() -> {
                            currentDocument = doc;
                            documentPreview.setText(doc.getFormattedText());
                            updateStatus("✅ Généré! Export en cours...", "#27ae60");
                        });

                        // Build file paths
                        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                        String baseName = sanitizeFilename(type) + "_" + sanitizeFilename(name) + "_" + timestamp;
                        String pdfPath = Paths.get(selectedFolder.getPath(), baseName + ".pdf").toString();
                        String wordPath = Paths.get(selectedFolder.getPath(), baseName + ".docx").toString();

                        // Export both formats (this is synchronous within the async context)
                        ExportResult result = docService.exportBothFormats(doc, pdfPath, wordPath);

                        // Update UI with results
                        Platform.runLater(() -> {
                            loadingIndicator.setVisible(false);
                            disableButtons(false);
                            handleExportResult(result);
                        });

                    } else {
                        Platform.runLater(() -> {
                            loadingIndicator.setVisible(false);
                            disableButtons(false);
                            updateStatus("❌ Échec de la génération", "#e74c3c");
                        });
                    }
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        loadingIndicator.setVisible(false);
                        disableButtons(false);
                        updateStatus("❌ Erreur: " + ex.getMessage(), "#e74c3c");
                        ex.printStackTrace();
                    });
                    return null;
                });
    }

    private void handleExportResult(ExportResult result) {
        System.out.println("╔═══════════════════════════════════════════════════════╗");
        System.out.println("║ 📊 HANDLING EXPORT RESULT                             ║");
        System.out.println("║ " + result);
        System.out.println("╚═══════════════════════════════════════════════════════╝");

        if (result.isFullySuccessful()) {
            updateStatus("✅ PDF + Word exportés!", "#27ae60");

            StringBuilder content = new StringBuilder();
            content.append("Les fichiers ont été créés dans:\n");
            content.append(selectedFolder.getAbsolutePath()).append("\n\n");

            if (result.pdfFile != null) {
                content.append("📄 ").append(result.pdfFile.getName()).append("\n");
            }
            if (result.wordFile != null) {
                content.append("📝 ").append(result.wordFile.getName()).append("\n");
            }

            showAlert(Alert.AlertType.INFORMATION, "Export Réussi",
                    "Documents exportés avec succès!", content.toString());

        } else if (result.pdfSuccess && !result.wordSuccess) {
            updateStatus("⚠️ PDF OK, Word échoué: " + result.wordError, "#f39c12");
            showAlert(Alert.AlertType.WARNING, "Export Partiel",
                    "Seul le PDF a été exporté",
                    "PDF: ✅ Créé\nWord: ❌ " + result.wordError);

        } else if (!result.pdfSuccess && result.wordSuccess) {
            updateStatus("⚠️ Word OK, PDF échoué: " + result.pdfError, "#f39c12");
            showAlert(Alert.AlertType.WARNING, "Export Partiel",
                    "Seul le Word a été exporté",
                    "PDF: ❌ " + result.pdfError + "\nWord: ✅ Créé");

        } else {
            updateStatus("❌ Échec de l'export", "#e74c3c");
            showAlert(Alert.AlertType.ERROR, "Export Échoué",
                    "Aucun fichier n'a été créé",
                    "PDF: " + result.pdfError + "\nWord: " + result.wordError);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void exportPDF() {
        if (currentDocument == null) {
            updateStatus("❌ Aucun document", "#e74c3c");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter en PDF");
        fileChooser.setInitialFileName(sanitizeFilename(currentDocument.title) + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        if (selectedFolder != null && selectedFolder.exists()) {
            fileChooser.setInitialDirectory(selectedFolder);
        }

        File file = fileChooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            updateStatus("🔄 Export PDF...", "#f39c12");
            loadingIndicator.setVisible(true);

            docService.exportToPDFAsync(currentDocument, file.getAbsolutePath())
                    .thenAccept(f -> Platform.runLater(() -> {
                        loadingIndicator.setVisible(false);
                        if (f != null && f.exists()) {
                            updateStatus("✅ PDF: " + f.getName(), "#27ae60");
                        } else {
                            updateStatus("❌ Échec export PDF", "#e74c3c");
                        }
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            loadingIndicator.setVisible(false);
                            updateStatus("❌ Erreur: " + ex.getMessage(), "#e74c3c");
                        });
                        return null;
                    });
        }
    }

    private void exportWord() {
        if (currentDocument == null) {
            updateStatus("❌ Aucun document", "#e74c3c");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter en Word");
        fileChooser.setInitialFileName(sanitizeFilename(currentDocument.title) + ".docx");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Word", "*.docx"));
        if (selectedFolder != null && selectedFolder.exists()) {
            fileChooser.setInitialDirectory(selectedFolder);
        }

        File file = fileChooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            updateStatus("🔄 Export Word...", "#f39c12");
            loadingIndicator.setVisible(true);

            docService.exportToWordAsync(currentDocument, file.getAbsolutePath())
                    .thenAccept(f -> Platform.runLater(() -> {
                        loadingIndicator.setVisible(false);
                        if (f != null && f.exists()) {
                            updateStatus("✅ Word: " + f.getName(), "#27ae60");
                        } else {
                            updateStatus("❌ Échec export Word", "#e74c3c");
                        }
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            loadingIndicator.setVisible(false);
                            updateStatus("❌ Erreur: " + ex.getMessage(), "#e74c3c");
                        });
                        return null;
                    });
        }
    }

    private void copyText() {
        if (currentDocument == null || documentPreview.getText().isEmpty()) {
            updateStatus("❌ Aucun texte", "#e74c3c");
            return;
        }

        try {
            ClipboardContent content = new ClipboardContent();
            content.putString(documentPreview.getText());
            Clipboard.getSystemClipboard().setContent(content);
            updateStatus("✅ Texte copié!", "#27ae60");
        } catch (Exception ex) {
            updateStatus("❌ Erreur copie", "#e74c3c");
        }
    }

    private void updateStatus(String message, String color) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11;");
        }
    }

    private void disableButtons(boolean disable) {
        if (!disable && currentDocument != null) {
            exportPDFButton.setDisable(false);
            exportWordButton.setDisable(false);
            copyButton.setDisable(false);
        } else {
            exportPDFButton.setDisable(disable);
            exportWordButton.setDisable(disable);
            copyButton.setDisable(disable);
        }
    }

    private String sanitizeFilename(String name) {
        if (name == null || name.isEmpty()) return "document";
        return name.replaceAll("[^a-zA-Z0-9_-]", "_")
                .replaceAll("_{2,}", "_")
                .replaceAll("^_|_$", "")
                .toLowerCase();
    }
}