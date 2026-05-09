package controller.formations;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import entities.formation.formation;
import entities.formation.inscription_formation;
import entities.formation.StatutInscription;
import entities.employers.session;
import entities.employers.employe;
import service.formation.inscription_formationCRUD;
import utils.MyDB;
import utils.RaisonAnalyzer;
import utils.TextAssistant;
import utils.LanguageToolAPI;
import utils.TextGeneratorAPI;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class inscription_formationController {

    @FXML
    private Label lblTitre;
    @FXML
    private Label lblOrganisme;
    @FXML
    private Label lblDates;
    @FXML
    private Label lblLieu;
    @FXML
    private Label lblCapacite;

    @FXML
    private Label lblEmployeConnecte;
    @FXML
    private ComboBox<StatutInscription> comboStatut;
    @FXML
    private javafx.scene.control.TextArea txtRaison;

    // 🤖 Éléments UI pour l'analyse IA
    @FXML
    private HBox aiAnalysisBox;
    @FXML
    private Label lblAiScore;
    @FXML
    private Label lblAiFeedback;
    @FXML
    private Label lblAiCategory;

    // ✨ Éléments UI pour l'assistant de texte
    @FXML
    private Button btnCorrectSpelling;
    @FXML
    private Button btnGenerateText;
    @FXML
    private VBox suggestionsBox;
    @FXML
    private Label lblSuggestions;

    @FXML
    private Button btnInscrire;
    @FXML
    private Button btnAnnuler;

    private formation currentFormation;
    private final inscription_formationCRUD inscriptionService = new inscription_formationCRUD();
    private Runnable onSuccessCallback;

    // 🤖 Stocker le dernier résultat d'analyse IA
    private RaisonAnalyzer.AnalysisResult lastAnalysisResult;

    @FXML
    private void initialize() {
        // Initialiser les valeurs du statut
        comboStatut.getItems().addAll(StatutInscription.values());
        comboStatut.setValue(StatutInscription.EN_ATTENTE);

        // Afficher l'employé connecté
        updateEmployeConnecteLabel();

        // 🤖 ANALYSE IA EN TEMPS RÉEL lors de la saisie de la raison
        txtRaison.textProperty().addListener((observable, oldValue, newValue) -> {
            analyzeRaisonInRealTime(newValue);
            showSuggestions(newValue);
        });
    }

    /**
     * 💡 Afficher les suggestions en temps réel
     */
    private void showSuggestions(String text) {
        if (text == null || text.trim().isEmpty()) {
            suggestionsBox.setVisible(false);
            suggestionsBox.setManaged(false);
            return;
        }

        List<String> suggestions = TextAssistant.getSuggestions(text);

        if (!suggestions.isEmpty()) {
            suggestionsBox.setVisible(true);
            suggestionsBox.setManaged(true);
            lblSuggestions.setText(String.join("\n", suggestions));
        } else {
            suggestionsBox.setVisible(false);
            suggestionsBox.setManaged(false);
        }
    }

    /**
     * 🤖 ANALYSE IA EN TEMPS RÉEL
     * Analyse la raison pendant que l'employé tape
     */
    private void analyzeRaisonInRealTime(String raison) {
        if (raison == null || raison.trim().isEmpty()) {
            aiAnalysisBox.setVisible(false);
            aiAnalysisBox.setManaged(false);
            lastAnalysisResult = null;
            return;
        }

        // Analyser avec l'IA
        lastAnalysisResult = RaisonAnalyzer.analyzeRaison(raison);

        // Afficher le résultat
        aiAnalysisBox.setVisible(true);
        aiAnalysisBox.setManaged(true);

        lblAiScore.setText(lastAnalysisResult.getScoreEmoji() + " " + lastAnalysisResult.getRelevanceScore() + "%");
        lblAiFeedback.setText(lastAnalysisResult.getFeedback());
        lblAiCategory.setText("Catégorie détectée: " + lastAnalysisResult.getCategoryDisplayName());

        // Changer la couleur selon le score
        String backgroundColor;
        String borderColor;
        if (lastAnalysisResult.getRelevanceScore() >= 80) {
            backgroundColor = "linear-gradient(from 0% 0% to 100% 100%, #e8f5e9 0%, #c8e6c9 100%)";
            borderColor = "#4caf50";
        } else if (lastAnalysisResult.getRelevanceScore() >= 60) {
            backgroundColor = "linear-gradient(from 0% 0% to 100% 100%, #fff3e0 0%, #ffe0b2 100%)";
            borderColor = "#ff9800";
        } else if (lastAnalysisResult.getRelevanceScore() >= 40) {
            backgroundColor = "linear-gradient(from 0% 0% to 100% 100%, #fff3e0 0%, #ffccbc 100%)";
            borderColor = "#ff5722";
        } else {
            backgroundColor = "linear-gradient(from 0% 0% to 100% 100%, #ffebee 0%, #ffcdd2 100%)";
            borderColor = "#f44336";
        }

        aiAnalysisBox.setStyle(String.format(
            "-fx-background-color: %s; -fx-background-radius: 8; -fx-padding: 12; -fx-border-color: %s; -fx-border-width: 1.5; -fx-border-radius: 8;",
            backgroundColor, borderColor
        ));
    }

    /**
     * Mettre à jour le label affichant l'employé connecté
     */
    private void updateEmployeConnecteLabel() {
        employe emp = session.getEmploye();

        if (emp == null) {
            lblEmployeConnecte.setText("Aucun employé connecté");
            lblEmployeConnecte.setStyle("-fx-text-fill: #c74a4a;");
            return;
        }

        String employeInfo = emp.getPrenom() + " " + emp.getNom();

        if (employeInfo != null) {
            lblEmployeConnecte.setText(employeInfo);
            lblEmployeConnecte.setStyle("-fx-text-fill: #2d7a3e; -fx-font-weight: bold;");
        } else {
                lblEmployeConnecte.setText("Employé connecté");
            lblEmployeConnecte.setStyle("-fx-text-fill: #2d7a3e; -fx-font-weight: bold;");
        }
    }

    /**
     * Récupérer les informations de l'employé depuis la base de données
     */
    private String getEmployeInfo(int idEmploye) {
        String sql = "SELECT nom, prenom FROM employé WHERE id_employe = ?";

        try {
            Connection conn = MyDB.getInstance().getConn();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idEmploye);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String nom = rs.getString("nom");
                String prenom = rs.getString("prenom");
                return prenom + " " + nom + " (ID: " + idEmploye + ")";
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Définir la formation pour laquelle l'inscription est faite
     */
    public void setFormation(formation f, Runnable onSuccess) {
        this.currentFormation = f;
        this.onSuccessCallback = onSuccess;
        loadFormationDetails();
    }

    /**
     * Charger les détails de la formation dans l'interface
     */
    private void loadFormationDetails() {
        if (currentFormation != null) {
            lblTitre.setText(currentFormation.getTitre());
            lblOrganisme.setText(safeText(currentFormation.getOrganisme()));

            String dates = formatDateRange(
                currentFormation.getDate_debut() != null ? currentFormation.getDate_debut().toString() : "-",
                currentFormation.getDate_fin() != null ? currentFormation.getDate_fin().toString() : "-"
            );
            lblDates.setText(dates);

            lblLieu.setText(safeText(currentFormation.getLieu()));
            lblCapacite.setText(safeText(currentFormation.getCapacite()));
        }
    }

    /**
     * Gérer l'inscription de l'employé à la formation
     */
    @FXML
    private void handleInscription() {
        // Vérifier qu'un employé est connecté
        employe emp = session.getEmploye();
        if (emp == null) {
            showAlert(AlertType.ERROR, "Aucun employé connecté",
                "Veuillez d'abord vous connecter en tant qu'employé dans l'écran principal.");
            return;
        }

        // Valider la raison
        String raison = txtRaison.getText();
        if (raison == null || raison.trim().isEmpty()) {
            showAlert(AlertType.WARNING, "Champ requis",
                "Veuillez expliquer pourquoi vous souhaitez suivre cette formation.");
            txtRaison.requestFocus();
            return;
        }

        if (raison.trim().length() < 10) {
            showAlert(AlertType.WARNING, "Raison trop courte",
                "Veuillez fournir une raison plus détaillée (au moins 10 caractères).");
            txtRaison.requestFocus();
            return;
        }

        // 🤖 VALIDATION IA - Avertir si le score est trop faible
        if (lastAnalysisResult != null && lastAnalysisResult.getRelevanceScore() < 40) {
            Alert confirmAlert = new Alert(AlertType.WARNING);
            confirmAlert.setTitle("⚠️ Analyse IA - Raison faible");
            confirmAlert.setHeaderText("Notre IA a détecté que votre raison pourrait être améliorée");
            confirmAlert.setContentText(
                "Score de pertinence: " + lastAnalysisResult.getRelevanceScore() + "%\n" +
                lastAnalysisResult.getFeedback() + "\n\n" +
                "💡 Suggestions pour améliorer votre raison:\n" +
                "• Soyez plus spécifique sur vos objectifs\n" +
                "• Expliquez comment cette formation s'inscrit dans votre projet professionnel\n" +
                "• Mentionnez les compétences que vous souhaitez acquérir\n" +
                "• Reliez-la à vos missions actuelles ou futures\n\n" +
                "Voulez-vous quand même soumettre votre inscription?"
            );

            javafx.scene.control.ButtonType btnContinue = new javafx.scene.control.ButtonType("Continuer quand même");
            javafx.scene.control.ButtonType btnImprove = new javafx.scene.control.ButtonType("Améliorer ma raison");

            confirmAlert.getButtonTypes().setAll(btnContinue, btnImprove);

            var result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == btnImprove) {
                txtRaison.requestFocus();
                return; // L'utilisateur veut améliorer sa raison
            }
        }

        if (!isFormValid()) {
            showAlert(AlertType.WARNING, "Erreur", "Impossible de procéder à l'inscription.");
            return;
        }

        Integer employeId = emp.getId_employé();

        // Vérifier si l'employé n'est pas déjà inscrit
        try {
            inscription_formation existingInscription = inscriptionService.getInscriptionByFormationAndEmploye(
                currentFormation.getId_formation(),
                employeId
            );

            if (existingInscription != null) {
                // Déjà inscrit - Afficher un message approprié selon le statut
                String message;
                if (existingInscription.getStatut() == StatutInscription.ACCEPTEE) {
                    message = "Vous êtes déjà inscrit et votre inscription a été ACCEPTÉE.\n" +
                             "Vous ne pouvez pas vous réinscrire.";
                } else if (existingInscription.getStatut() == StatutInscription.EN_ATTENTE) {
                    message = "Vous êtes déjà inscrit à cette formation.\n" +
                             "Votre inscription est EN ATTENTE de validation.";
                } else {
                    message = "Vous êtes déjà inscrit à cette formation.\n" +
                             "Statut: " + existingInscription.getStatut();
                }

                showAlert(AlertType.WARNING, "Inscription existante", message);
                return;
            }

            // Créer l'inscription avec l'ID de l'employé connecté et la raison
            inscription_formation inscription = new inscription_formation(
                currentFormation.getId_formation(),
                employeId,
                comboStatut.getValue(),
                raison.trim()
            );

            inscriptionService.ajouter(inscription);

            // 🤖 Message de succès avec le score IA
            String successMessage = "Inscription effectuée avec succès !";
            if (lastAnalysisResult != null) {
                successMessage += "\n\n🤖 Analyse IA de votre motivation:\n" +
                    "Score: " + lastAnalysisResult.getScoreEmoji() + " " + lastAnalysisResult.getRelevanceScore() + "%\n" +
                    "Catégorie: " + lastAnalysisResult.getCategoryDisplayName() + "\n" +
                    lastAnalysisResult.getFeedback();
            }

            showAlert(AlertType.INFORMATION, "Succès", successMessage);

            // Appeler le callback si défini
            if (onSuccessCallback != null) {
                onSuccessCallback.run();
            }

            // Fermer la fenêtre
            closeWindow();
        } catch (SQLException e) {
            showAlert(AlertType.ERROR, "Erreur", "Échec de l'inscription: " + e.getMessage());
        }
    }

    /**
     * Annuler et fermer la fenêtre
     */
    @FXML
    private void handleAnnuler() {
        closeWindow();
    }

    /**
     * 🔤 Corriger l'orthographe du texte avec API réelle
     */
    @FXML
    private void handleCorrectSpelling() {
        String currentText = txtRaison.getText();

        if (currentText == null || currentText.trim().isEmpty()) {
            showAlert(AlertType.INFORMATION, "Aucun texte",
                "Veuillez d'abord écrire votre raison pour la correction orthographique.");
            return;
        }

        // Afficher message de chargement
        Alert loading = new Alert(AlertType.INFORMATION);
        loading.setTitle("🔍 Analyse en cours...");
        loading.setHeaderText("Vérification avec LanguageTool API");
        loading.setContentText("Analyse de votre texte en cours...\nVeuillez patienter.");

        // Créer un thread pour ne pas bloquer l'interface
        new Thread(() -> {
            try {
                // Détecter la langue automatiquement
                String language = LanguageToolAPI.detectLanguage(currentText);

                // Vérifier le texte avec l'API LanguageTool
                List<LanguageToolAPI.GrammarError> errors =
                    LanguageToolAPI.checkText(currentText, language);

                // Retour sur le thread JavaFX
                javafx.application.Platform.runLater(() -> {
                    loading.close();

                    if (errors.isEmpty()) {
                        showAlert(AlertType.INFORMATION, "✅ Aucune erreur",
                            "Félicitations ! Aucune erreur détectée par LanguageTool.\n" +
                            "Votre texte semble parfait ! 🎉");
                        return;
                    }

                    // Afficher les corrections proposées
                    StringBuilder corrections = new StringBuilder();
                    corrections.append("🤖 Analyse LanguageTool (")
                              .append(language.equals("fr") ? "Français" : "Anglais")
                              .append(") :\n\n");

                    corrections.append(errors.size()).append(" problème(s) détecté(s) :\n\n");

                    for (int i = 0; i < Math.min(10, errors.size()); i++) {
                        LanguageToolAPI.GrammarError error = errors.get(i);
                        corrections.append("• ").append(error.shortMessage.isEmpty() ?
                            error.message : error.shortMessage).append("\n");

                        if (!error.suggestions.isEmpty()) {
                            corrections.append("  → Suggestion : ")
                                     .append(String.join(", ", error.suggestions.subList(0,
                                         Math.min(3, error.suggestions.size()))))
                                     .append("\n");
                        }
                        corrections.append("\n");
                    }

                    if (errors.size() > 10) {
                        corrections.append("... et ").append(errors.size() - 10)
                                 .append(" autre(s) problème(s)\n\n");
                    }

                    corrections.append("Voulez-vous appliquer toutes les corrections automatiquement ?");

                    Alert confirm = new Alert(AlertType.CONFIRMATION);
                    confirm.setTitle("🔤 Correction avec LanguageTool");
                    confirm.setHeaderText(errors.size() + " erreur(s) détectée(s)");
                    confirm.setContentText(corrections.toString());

                    Optional<javafx.scene.control.ButtonType> result = confirm.showAndWait();
                    if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
                        // Appliquer les corrections
                        String correctedText = LanguageToolAPI.applyCorrections(currentText, errors);
                        txtRaison.setText(correctedText);

                        showAlert(AlertType.INFORMATION, "✅ Corrections appliquées",
                            errors.size() + " correction(s) appliquée(s) avec succès !\n\n" +
                            "🤖 Propulsé par LanguageTool API");
                    }
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    loading.close();

                    // Fallback vers correction locale
                    Alert fallback = new Alert(AlertType.WARNING);
                    fallback.setTitle("⚠️ API non disponible");
                    fallback.setHeaderText("Erreur de connexion à LanguageTool");
                    fallback.setContentText("Impossible de contacter l'API.\n" +
                        "Utilisation de la correction locale basique.\n\n" +
                        "Erreur : " + e.getMessage());
                    fallback.showAndWait();

                    // Utiliser la correction locale
                    List<TextAssistant.SpellingError> localErrors =
                        TextAssistant.detectSpellingErrors(currentText);

                    if (!localErrors.isEmpty()) {
                        StringBuilder localCorrections = new StringBuilder("Corrections locales :\n\n");
                        for (TextAssistant.SpellingError error : localErrors) {
                            localCorrections.append("• ").append(error).append("\n");
                        }
                        localCorrections.append("\nAppliquer ces corrections ?");

                        Alert confirmLocal = new Alert(AlertType.CONFIRMATION);
                        confirmLocal.setContentText(localCorrections.toString());

                        Optional<javafx.scene.control.ButtonType> res = confirmLocal.showAndWait();
                        if (res.isPresent() && res.get() == javafx.scene.control.ButtonType.OK) {
                            String corrected = TextAssistant.correctSpelling(currentText);
                            txtRaison.setText(corrected);
                        }
                    }
                });
            }
        }).start();

        loading.show();
    }

    /**
     * ✨ Générer un paragraphe avec IA réelle (Hugging Face API)
     */
    @FXML
    private void handleGenerateText() {
        // Demander les mots-clés à l'utilisateur
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("✨ Générateur de Texte IA");
        dialog.setHeaderText("Assistant de rédaction professionnel avec IA réelle");
        dialog.setContentText("Entrez vos mots-clés ou votre intention :\n" +
            "(Ex: communication, technical skills, career development, certification, etc.)");

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(keywords -> {
            if (keywords.trim().isEmpty()) {
                showAlert(AlertType.WARNING, "Mots-clés requis",
                    "Veuillez fournir au moins un mot-clé pour générer le texte.");
                return;
            }

            // Afficher message de chargement
            Alert loading = new Alert(AlertType.INFORMATION);
            loading.setTitle("🤖 Génération en cours...");
            loading.setHeaderText("IA en train de générer votre texte");
            loading.setContentText("Veuillez patienter...\nL'IA génère un texte unique et personnalisé...");

            // Générer dans un thread séparé (l'API prend du temps)
            new Thread(() -> {
                try {
                    // 🤖 Appeler l'IA réelle (Hugging Face API)
                    String context = currentFormation != null ? currentFormation.getTitre() : "";
                    String generatedText = TextGeneratorAPI.generateProfessionalText(keywords, context);

                    // Retour au thread JavaFX
                    javafx.application.Platform.runLater(() -> {
                        loading.close();

                        if (generatedText == null || generatedText.trim().isEmpty()) {
                            showAlert(AlertType.WARNING, "⚠️ Génération échouée",
                                "L'IA n'a pas pu générer de texte.\n" +
                                "Utilisation du template de secours...");

                            // Fallback vers templates
                            String fallbackText = TextAssistant.generateProfessionalParagraph(keywords);
                            displayGeneratedText(fallbackText);
                            return;
                        }

                        // Afficher le texte généré avec option de remplacement
                        displayGeneratedText(generatedText);
                    });

                } catch (Exception e) {
                    javafx.application.Platform.runLater(() -> {
                        loading.close();

                        // Fallback
                        String fallbackText = TextAssistant.generateProfessionalParagraph(keywords);
                        showAlert(AlertType.INFORMATION, "ℹ️ Mode template",
                            "L'IA n'est pas disponible.\n" +
                            "Utilisation d'un template professionnel à la place.");
                        displayGeneratedText(fallbackText);
                    });
                }
            }).start();

            loading.show();
        });
    }

    /**
     * 📝 Afficher le texte généré et demander le remplacement/ajout
     */
    private void displayGeneratedText(String generatedText) {
        String currentText = txtRaison.getText();

        if (currentText != null && !currentText.trim().isEmpty()) {
            Alert confirmReplace = new Alert(AlertType.CONFIRMATION);
            confirmReplace.setTitle("✨ Texte Généré");
            confirmReplace.setHeaderText("Votre IA a généré un texte unique");
            confirmReplace.setContentText("Texte généré :\n\n" + generatedText +
                "\n\nVoulez-vous remplacer votre texte actuel ?");

            javafx.scene.control.ButtonType btnReplace = new javafx.scene.control.ButtonType("Remplacer");
            javafx.scene.control.ButtonType btnAppend = new javafx.scene.control.ButtonType("Ajouter à la fin");
            javafx.scene.control.ButtonType btnCancel = javafx.scene.control.ButtonType.CANCEL;

            confirmReplace.getButtonTypes().setAll(btnReplace, btnAppend, btnCancel);

            Optional<javafx.scene.control.ButtonType> choice = confirmReplace.showAndWait();

            if (choice.isPresent()) {
                if (choice.get() == btnReplace) {
                    txtRaison.setText(generatedText);
                } else if (choice.get() == btnAppend) {
                    txtRaison.setText(currentText + "\n\n" + generatedText);
                }
            }
        } else {
            // Pas de texte existant
            txtRaison.setText(generatedText);

            showAlert(AlertType.INFORMATION, "✅ Texte généré par IA",
                "Un paragraphe professionnel unique a été généré !\n\n" +
                "Vous pouvez le personnaliser selon vos besoins.");
        }
    }

    /**
     * Valider le formulaire
     */
    private boolean isFormValid() {
        return session.getEmploye() != null && currentFormation != null;
    }

    /**
     * Afficher une alerte
     */
    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Fermer la fenêtre actuelle
     */
    private void closeWindow() {
        Stage stage = (Stage) btnAnnuler.getScene().getWindow();
        stage.close();
    }

    /**
     * Formater une plage de dates
     */
    private String formatDateRange(String start, String end) {
        if (start.equals("-") && end.equals("-")) {
            return "Non défini";
        }
        return start + " → " + end;
    }

    /**
     * Retourner un texte sûr (éviter null)
     */
    private String safeText(String value) {
        return value == null || value.trim().isEmpty() ? "Non spécifié" : value;
    }
}
