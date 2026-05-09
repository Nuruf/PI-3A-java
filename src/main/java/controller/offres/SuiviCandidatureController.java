package controller.offres;

import entities.offres.Candidat;
import entities.offres.Offre;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import javafx.scene.shape.Circle;
import service.CandidatCRUD;
import service.OffreCRUD;

import java.sql.SQLException;

public class SuiviCandidatureController {

    @FXML private TextField txtTrackingCode;
    @FXML private VBox resultContainer;
    @FXML private Label lblPoste, lblEntreprise, lblStatusTitle, lblStatusDesc;

    @FXML private Circle step1Circle, step2Circle, step3Circle, step4Circle;
    @FXML private Region line1, line2, line3;

    @FXML private VBox statusMessageBox;

    OffreCRUD offreCRUD = new OffreCRUD();

    @FXML
    private void handleSearch() {
        String code = txtTrackingCode.getText().trim();
        if (code.isEmpty()) return;

        try {
            CandidatCRUD crud = new CandidatCRUD();
            Candidat c = crud.afficher().stream()
                    .filter(cand -> code.equalsIgnoreCase(cand.getCodeCandidature()))
                    .findFirst()
                    .orElse(null);

            if (c != null) {
                displayStatus(c);
            } else {
                resultContainer.setVisible(false);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void displayStatus(Candidat c) {
        resultContainer.setVisible(true);
        try {
            Offre offre = offreCRUD.getById(c.getIdOffre());
            if (offre != null) {
                lblPoste.setText("Candidature pour le poste: " + offre.getTitrePoste());
            } else {
                lblPoste.setText("Candidature pour le poste: Information non disponible");
            }
        }catch (SQLException e){
            e.printStackTrace();
            lblPoste.setText("Erreur lors du chargement du poste");
        }

        resetTimeline();
        String etat = c.getEtat().toLowerCase();

        updateStatusTheme("info");
        highlightStep(step1Circle, null);

        if (etat.contains("présélection")) {
            highlightStep(step2Circle, line1);
            lblStatusTitle.setText("Phase de Présélection");
            lblStatusDesc.setText("Félicitations ! Votre profil a passé le premier tri. " +
                    "Notre IA Momentum vous a classé parmi les meilleurs profils.");
        }
        else if (etat.contains("entretien")) {
            highlightStep(step2Circle, line1);
            highlightStep(step3Circle, line2);
            lblStatusTitle.setText("En attente d'entretien");
            lblStatusDesc.setText("Nous souhaitons vous rencontrer ! Un recruteur vous contactera sous peu pour fixer une date.");
        }
        else if (etat.contains("accepté")) {
            updateStatusTheme("success");
            highlightStep(step2Circle, line1);
            highlightStep(step3Circle, line2);
            highlightStep(step4Circle, line3);
            lblStatusTitle.setText("Candidature Acceptée !");
            lblStatusDesc.setText("Bienvenue chez nous ! Consultez vos emails pour les détails de l'embauche.");
        }
        else if (etat.contains("refusé")) {
            updateStatusTheme("danger");

            // We show exactly where it was refused
            step1Circle.setStyle("-fx-fill: -color-danger-emphasis;");
            lblStatusTitle.setText("Candidature Non Retenue");
            lblStatusDesc.setText("Nous sommes désolés de vous informer que votre candidature n'a pas été retenue pour ce poste." +
                    "Nous garderons votre profil pour de futures opportunités.");
        }
    }

    private void updateStatusTheme(String type) {
        switch (type) {
            case "danger" -> {
                statusMessageBox.setStyle("-fx-background-color: -color-danger-subtle; -fx-padding: 25; -fx-background-radius: 15;");
                lblStatusTitle.setStyle("-fx-text-fill: -color-danger-fg; -fx-font-weight: bold;");
            }
            case "success" -> {
                statusMessageBox.setStyle("-fx-background-color: -color-success-subtle; -fx-padding: 25; -fx-background-radius: 15;");
                lblStatusTitle.setStyle("-fx-text-fill: -color-success-fg; -fx-font-weight: bold;");
            }
            default -> { // Info / Pending
                statusMessageBox.setStyle("-fx-background-color: -color-accent-subtle; -fx-padding: 25; -fx-background-radius: 15;");
                lblStatusTitle.setStyle("-fx-text-fill: -color-accent-fg; -fx-font-weight: bold;");
            }
        }
    }

    private void highlightStep(Circle circle, Region line) {
        circle.setStyle("-fx-fill: -color-success-emphasis;");
        if (line != null) line.setStyle("-fx-background-color: -color-success-emphasis;");
    }

    private void resetTimeline() {
        Circle[] circles = {step1Circle, step2Circle, step3Circle, step4Circle};
        Region[] lines = {line1, line2, line3};
        for (Circle c : circles) c.setStyle("-fx-fill: -color-base-3;");
        for (Region r : lines) r.setStyle("-fx-background-color: -color-base-3;");
    }
}