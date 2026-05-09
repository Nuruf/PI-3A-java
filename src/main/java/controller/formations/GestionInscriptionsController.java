package controller.formations;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import entities.formation.formation;
import entities.formation.inscription_formation;
import entities.formation.StatutInscription;
import service.formation.inscription_formationCRUD;
import utils.RaisonAnalyzer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import utils.MyDB;

public class GestionInscriptionsController {

    @FXML
    private Label lblFormationTitle;
    @FXML
    private VBox inscriptionsContainer;
    @FXML
    private Button btnFermer;

    private formation currentFormation;
    private final inscription_formationCRUD inscriptionService = new inscription_formationCRUD();
    private Runnable onSuccessCallback;

    /**
     * Définir la formation et charger les inscriptions
     */
    public void setFormation(formation f, Runnable onSuccess) {
        this.currentFormation = f;
        this.onSuccessCallback = onSuccess;
        lblFormationTitle.setText("Formation: " + f.getTitre());
        loadInscriptions();
    }

    /**
     * Charger et afficher les inscriptions
     */
    private void loadInscriptions() {
        inscriptionsContainer.getChildren().clear();

        try {
            List<inscription_formation> inscriptions = inscriptionService.getInscriptionsByFormation(
                currentFormation.getId_formation()
            );

            if (inscriptions.isEmpty()) {
                Label emptyLabel = new Label("Aucune inscription pour cette formation.");
                emptyLabel.setStyle("-fx-text-fill: #868686; -fx-font-size: 14px;");
                inscriptionsContainer.getChildren().add(emptyLabel);
                return;
            }

            // Créer une carte pour chaque inscription
            for (inscription_formation insc : inscriptions) {
                VBox card = createInscriptionCard(insc);
                inscriptionsContainer.getChildren().add(card);
            }

        } catch (SQLException e) {
            showAlert(AlertType.ERROR, "Erreur", "Échec du chargement des inscriptions: " + e.getMessage());
        }
    }

    /**
     * Créer une carte pour une inscription
     */
    private VBox createInscriptionCard(inscription_formation insc) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 8; " +
                "-fx-padding: 15; -fx-border-color: -color-border-muted; -fx-border-radius: 8;");

        // Récupérer les infos de l'employé
        String employeInfo = getEmployeInfo(insc.getId_user());

        // Header avec ID et nom
        HBox headerRow = new HBox(10);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label idLabel = new Label("ID: " + insc.getId_user());
        idLabel.setFont(Font.font(idLabel.getFont().getFamily(), FontWeight.BOLD, 14));

        Label nameLabel = new Label(employeInfo);
        nameLabel.setStyle("-fx-text-fill: -color-fg-muted;");

        headerRow.getChildren().addAll(idLabel, nameLabel);

        // Statut actuel
        Label statutLabel = new Label("Statut: " + getStatutText(insc.getStatut()));
        statutLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " +
            getStatutColor(insc.getStatut()) + "; -fx-font-size: 13px;");

        // Afficher la raison de l'inscription
        if (insc.getRaison() != null && !insc.getRaison().trim().isEmpty()) {
            VBox raisonBox = new VBox(5);
            raisonBox.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 10; -fx-background-radius: 5;");

            Label raisonTitleLabel = new Label("💬 Raison de l'inscription:");
            raisonTitleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #495057;");

            Label raisonTextLabel = new Label(insc.getRaison());
            raisonTextLabel.setWrapText(true);
            raisonTextLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #495057; -fx-font-style: italic;");

            raisonBox.getChildren().addAll(raisonTitleLabel, raisonTextLabel);

            // 🤖 AJOUT DE L'ANALYSE IA POUR LA RH
            RaisonAnalyzer.AnalysisResult aiAnalysis = RaisonAnalyzer.analyzeRaison(insc.getRaison());
            if (aiAnalysis != null) {
                VBox aiBox = new VBox(3);
                aiBox.setStyle("-fx-padding: 8 0 0 0;");

                // Score et feedback
                HBox scoreRow = new HBox(8);
                scoreRow.setAlignment(Pos.CENTER_LEFT);

                Label aiLabel = new Label("🤖 Analyse IA:");
                aiLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #6c757d;");

                Label scoreLabel = new Label(aiAnalysis.getScoreEmoji() + " " + aiAnalysis.getRelevanceScore() + "%");
                scoreLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: " +
                    getScoreColor(aiAnalysis.getRelevanceScore()) + ";");

                Label categoryLabel = new Label("• " + aiAnalysis.getCategoryDisplayName());
                categoryLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #6c757d;");

                scoreRow.getChildren().addAll(aiLabel, scoreLabel, categoryLabel);

                Label feedbackLabel = new Label(aiAnalysis.getFeedback());
                feedbackLabel.setWrapText(true);
                feedbackLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #6c757d; -fx-font-style: italic;");

                aiBox.getChildren().addAll(scoreRow, feedbackLabel);
                raisonBox.getChildren().add(aiBox);
            }

            card.getChildren().add(raisonBox);
        }

        // Boutons d'action
        HBox actionsRow = new HBox(10);
        actionsRow.setAlignment(Pos.CENTER_LEFT);

        if (insc.getStatut() == StatutInscription.EN_ATTENTE) {
            // Bouton Accepter
            Button btnAccept = new Button("✓ Accepter");
            btnAccept.getStyleClass().add("accent");
            btnAccept.setPrefWidth(120);
            btnAccept.setOnAction(event -> handleAccepter(insc));

            // Bouton Refuser
            Button btnRefuse = new Button("✗ Refuser");
            btnRefuse.getStyleClass().addAll("danger", "outline");
            btnRefuse.setPrefWidth(120);
            btnRefuse.setOnAction(event -> handleRefuser(insc));

            actionsRow.getChildren().addAll(btnAccept, btnRefuse);
        } else {
            // Déjà traité
            Label traitedLabel = new Label("Déjà traité");
            traitedLabel.setStyle("-fx-text-fill: #868686; -fx-font-style: italic;");
            actionsRow.getChildren().add(traitedLabel);
        }

        card.getChildren().addAll(headerRow, statutLabel, actionsRow);
        return card;
    }

    /**
     * Récupérer les informations de l'employé
     */
    private String getEmployeInfo(int idEmploye) {
        try {
            Connection conn = MyDB.getInstance().getConn();
            String query = "SELECT nom, prenom FROM employé WHERE id_employe=?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, idEmploye);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("prenom") + " " + rs.getString("nom");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de l'employé: " + e.getMessage());
        }
        return "Employé #" + idEmploye;
    }

    /**
     * Accepter une inscription
     */
    private void handleAccepter(inscription_formation insc) {
        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer l'acceptation");
        confirm.setHeaderText("Accepter cette inscription ?");
        confirm.setContentText("Employé: " + getEmployeInfo(insc.getId_user()));

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                insc.setStatut(StatutInscription.ACCEPTEE);
                inscriptionService.modifier(insc);

                showAlert(AlertType.INFORMATION, "Succès", "Inscription acceptée avec succès !");
                loadInscriptions(); // Recharger la liste

                if (onSuccessCallback != null) {
                    onSuccessCallback.run();
                }
            } catch (SQLException e) {
                showAlert(AlertType.ERROR, "Erreur", "Échec de l'acceptation: " + e.getMessage());
            }
        }
    }

    /**
     * Refuser une inscription
     */
    private void handleRefuser(inscription_formation insc) {
        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer le refus");
        confirm.setHeaderText("Refuser cette inscription ?");
        confirm.setContentText("Employé: " + getEmployeInfo(insc.getId_user()));

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                insc.setStatut(StatutInscription.REFUSEE);
                inscriptionService.modifier(insc);

                showAlert(AlertType.INFORMATION, "Succès", "Inscription refusée.");
                loadInscriptions(); // Recharger la liste

                if (onSuccessCallback != null) {
                    onSuccessCallback.run();
                }
            } catch (SQLException e) {
                showAlert(AlertType.ERROR, "Erreur", "Échec du refus: " + e.getMessage());
            }
        }
    }

    /**
     * Fermer la fenêtre
     */
    @FXML
    private void handleFermer() {
        Stage stage = (Stage) btnFermer.getScene().getWindow();
        stage.close();
    }

    /**
     * Obtenir le texte du statut
     */
    private String getStatutText(StatutInscription statut) {
        switch (statut) {
            case EN_ATTENTE: return "En attente de validation";
            case ACCEPTEE: return "Acceptée ✓";
            case REFUSEE: return "Refusée ✗";
            default: return statut.name();
        }
    }

    /**
     * Obtenir la couleur du statut
     */
    private String getStatutColor(StatutInscription statut) {
        switch (statut) {
            case EN_ATTENTE: return "#ff9800";
            case ACCEPTEE: return "#4caf50";
            case REFUSEE: return "#f44336";
            default: return "#666666";
        }
    }

    /**
     * 🤖 Obtenir la couleur selon le score IA
     */
    private String getScoreColor(int score) {
        if (score >= 80) return "#4caf50";  // Vert - Excellent
        if (score >= 60) return "#ff9800";  // Orange - Bon
        if (score >= 40) return "#ff5722";  // Orange foncé - Acceptable
        return "#f44336";                    // Rouge - Faible
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
}


