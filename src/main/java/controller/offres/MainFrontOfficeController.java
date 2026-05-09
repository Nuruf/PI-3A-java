package controller.offres;

import entities.employers.session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

public class MainFrontOfficeController {

    @FXML private StackPane contentArea;
    @FXML private Button btnNavOffres, btnNavEntreprises, btnNavSuivi;
    @FXML private Label lblUserInfo;

    @FXML
    public void initialize() {
        // Display logged-in user's name
        displayLoggedInUser();

        showOffres();
    }

    private void displayLoggedInUser() {
        try {
            // Get the current session visitor
            entities.employers.visiteur visitor = session.getVisiteur();
            if (visitor != null) {
                String fullName = visitor.getPrenom() + " " + visitor.getNom();
                lblUserInfo.setText("👤 " + fullName);
            }
        } catch (Exception e) {
            lblUserInfo.setText("👤 Visiteur");
        }
    }

    @FXML
    private void showOffres() {
        loadView("/offres/offres-portal.fxml");
        updateActiveLink(btnNavOffres);
    }

    @FXML
    private void showEntreprises() {
        loadView("/offres/entreprises-portal.fxml");
        updateActiveLink(btnNavEntreprises);
    }

    @FXML
    private void showSuivi() {
        loadView("/offres/suivi-candidature.fxml");
        updateActiveLink(btnNavSuivi);
    }

    @FXML
    private void handleDeonnexion() {
        try {
            session.logout();

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Déconnexion");
            alert.setHeaderText(null);
            alert.setContentText("Vous voulez déconnecter?");
            alert.showAndWait();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/emp/Login.fxml"));
            Parent root = loader.load();
            Stage loginStage = new Stage();
            loginStage.setTitle("Connexion");
            loginStage.setScene(new Scene(root));
            loginStage.show();
                // Fermer la fenêtre actuelle
            Stage currentStage = (Stage) contentArea.getScene().getWindow();
            currentStage.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadView(String fxmlPath) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("Error loading: " + fxmlPath);
            e.printStackTrace();
        }
    }

    private void updateActiveLink(Button activeBtn) {
        // Reset styles for all
        btnNavOffres.setStyle("-fx-text-fill: -color-fg-default; -fx-font-weight: normal;");
        btnNavEntreprises.setStyle("-fx-text-fill: -color-fg-default; -fx-font-weight: normal;");
        btnNavSuivi.setStyle("-fx-text-fill: -color-fg-default; -fx-font-weight: normal;");

        // Highlight the active one
        activeBtn.setStyle("-fx-text-fill: -color-accent-fg; -fx-font-weight: bold; -fx-border-color: -color-accent-fg; -fx-border-width: 0 0 2 0;");
    }
}