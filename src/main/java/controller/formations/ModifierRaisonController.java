package controller.formations;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import entities.formation.inscription_formation;
import entities.formation.StatutInscription;
import service.formation.inscription_formationCRUD;

import java.sql.SQLException;

public class ModifierRaisonController {

    @FXML
    private Label lblFormationTitre;
    @FXML
    private Label lblStatut;
    @FXML
    private Label lblRaisonActuelle;
    @FXML
    private TextArea txtNouvelleRaison;
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnAnnuler;

    private inscription_formation currentInscription;
    private final inscription_formationCRUD inscriptionService = new inscription_formationCRUD();
    private Runnable onSuccessCallback;

    /**
     * Définir l'inscription à modifier
     */
    public void setInscription(inscription_formation inscription, String formationTitre, Runnable onSuccess) {
        this.currentInscription = inscription;
        this.onSuccessCallback = onSuccess;

        // Afficher les informations
        lblFormationTitre.setText(formationTitre);
        lblStatut.setText(inscription.getStatut().getDisplayName());

        // Appliquer le style selon le statut
        if (inscription.getStatut() == StatutInscription.EN_ATTENTE) {
            lblStatut.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
        } else if (inscription.getStatut() == StatutInscription.ACCEPTEE) {
            lblStatut.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        } else {
            lblStatut.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        }

        lblRaisonActuelle.setText(inscription.getRaison() != null ? inscription.getRaison() : "Aucune raison spécifiée");
        txtNouvelleRaison.setText(inscription.getRaison() != null ? inscription.getRaison() : "");

        // Vérifier si la modification est possible
        if (inscription.getStatut() == StatutInscription.ACCEPTEE) {
            txtNouvelleRaison.setDisable(true);
            btnModifier.setDisable(true);
            showAlert(AlertType.WARNING, "Modification impossible",
                "Vous ne pouvez pas modifier la raison car votre inscription a déjà été ACCEPTÉE.");
        }
    }

    /**
     * Gérer la modification de la raison
     */
    @FXML
    private void handleModifier() {
        // Vérifier que l'inscription n'est pas acceptée
        if (currentInscription.getStatut() == StatutInscription.ACCEPTEE) {
            showAlert(AlertType.ERROR, "Modification impossible",
                "Vous ne pouvez pas modifier la raison car votre inscription a été ACCEPTÉE.");
            return;
        }

        // Valider la nouvelle raison
        String nouvelleRaison = txtNouvelleRaison.getText();
        if (nouvelleRaison == null || nouvelleRaison.trim().isEmpty()) {
            showAlert(AlertType.WARNING, "Champ requis",
                "Veuillez fournir une raison pour votre inscription.");
            txtNouvelleRaison.requestFocus();
            return;
        }

        if (nouvelleRaison.trim().length() < 10) {
            showAlert(AlertType.WARNING, "Raison trop courte",
                "Veuillez fournir une raison plus détaillée (au moins 10 caractères).");
            txtNouvelleRaison.requestFocus();
            return;
        }

        // Vérifier si la raison a changé
        String ancienneRaison = currentInscription.getRaison() != null ? currentInscription.getRaison() : "";
        if (nouvelleRaison.trim().equals(ancienneRaison.trim())) {
            showAlert(AlertType.INFORMATION, "Aucun changement",
                "La raison n'a pas été modifiée.");
            return;
        }

        // Mettre à jour la raison
        try {
            currentInscription.setRaison(nouvelleRaison.trim());
            inscriptionService.modifier(currentInscription);

            showAlert(AlertType.INFORMATION, "Succès",
                "Votre raison a été mise à jour avec succès !");

            // Appeler le callback si défini
            if (onSuccessCallback != null) {
                onSuccessCallback.run();
            }

            // Fermer la fenêtre
            closeWindow();
        } catch (SQLException e) {
            showAlert(AlertType.ERROR, "Erreur",
                "Échec de la modification: " + e.getMessage());
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
}

