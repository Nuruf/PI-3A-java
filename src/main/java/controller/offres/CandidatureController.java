package controller.offres;

import entities.offres.Candidat;
import entities.offres.Offre;
import entities.employers.session;
import entities.employers.visiteur;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import service.CandidatCRUD;
import service.OffreCRUD;
import utils.FilePickerUtil;
import java.io.File;
import java.nio.file.Files;
import java.sql.SQLException;

public class CandidatureController {

    @FXML private StackPane rootNode;
    @FXML private VBox postulationFormView, succesEnvoiView;

    @FXML private WebView detailsWebView;
    private WebEngine descriptionEngine;

    @FXML private Button btnUploadCV, btnUploadLettre;
    @FXML private Label lblTrackingCode;

    private File fileCV, fileLettre;
    private boolean cvUploaded = false;
    private boolean lettreUploaded = false;
    private int idOffreSelectionnee;

    private visiteur visiteurConnecte;

    @FXML
    public void initialize() {
        descriptionEngine = detailsWebView.getEngine();
        modifierVisibiliteVues(postulationFormView);

        visiteurConnecte = session.getVisiteur();
    }

    private boolean isPostulationValide() {
        boolean isValide = true;

        String errorStyle = "-fx-border-color: -color-danger-emphasis; -fx-border-width: 1; -fx-border-radius: 5;";

        resetStylesFormulaire();

        // Validation CV
        if (!cvUploaded) {
            btnUploadCV.setStyle(errorStyle);
            isValide = false;
        }

        return isValide;
    }

    private void resetStylesFormulaire() {
        String defaultBorder = "-fx-border-color: -color-border-default; -fx-border-width: 1; -fx-border-radius: 5;";

        if (!cvUploaded) btnUploadCV.setStyle("");
        if (!lettreUploaded) btnUploadLettre.setStyle("");
    }

    @FXML
    private void handlePostuler() {
        if (!isPostulationValide()) return;

        try {
            String trackingCode = Long.toString(System.currentTimeMillis(), 36).toUpperCase();
            byte[] cvBytes = Files.readAllBytes(fileCV.toPath());
            byte[] lettreBytes = (fileLettre != null) ? Files.readAllBytes(fileLettre.toPath()) : null;

            Candidat nouveauCandidat = new Candidat(
                    trackingCode,
                    fileCV.getName(),
                    cvBytes, (fileLettre != null ? fileLettre.getName() : ""),
                    lettreBytes, "En attente", "", new java.sql.Date(System.currentTimeMillis()),
                    idOffreSelectionnee,
                    visiteurConnecte.getId_visiteur()
            );

            new CandidatCRUD().ajouter(nouveauCandidat);

            lblTrackingCode.setText(trackingCode);
            modifierVisibiliteVues(succesEnvoiView);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void pickCV() {
        File file = FilePickerUtil.pickPdf(btnUploadCV.getScene().getWindow(), "Upload CV");
        if (file != null) {
            this.fileCV = file;
            this.cvUploaded = true;
            btnUploadCV.setText("✔ " + file.getName());
            btnUploadCV.setStyle("-fx-border-color: -color-success-emphasis; -fx-text-fill: -color-success-emphasis;");
        }
    }

    @FXML
    private void pickLettre() {
        File file = FilePickerUtil.pickPdf(btnUploadLettre.getScene().getWindow(), "Upload Lettre");
        if (file != null) {
            this.fileLettre = file;
            this.lettreUploaded = true;
            btnUploadLettre.setText("✔ " + file.getName());
            btnUploadLettre.setStyle("-fx-border-color: -color-success-emphasis; -fx-text-fill: -color-success-emphasis;");
        }
    }

    private void modifierVisibiliteVues(VBox vueActive) {
        postulationFormView.setVisible(false); postulationFormView.setManaged(false);
        succesEnvoiView.setVisible(false); succesEnvoiView.setManaged(false);

        vueActive.setVisible(true);
        vueActive.setManaged(true);
    }

    @FXML
    private void handleClose() {
        StackPane parent = (StackPane) rootNode.getParent();
        if (parent != null) {
            parent.getChildren().remove(rootNode);
            if (!parent.getChildren().isEmpty()) {
                Node contentPortal = parent.getChildren().get(0);
                contentPortal.setEffect(null);
                contentPortal.setOpacity(1.0);
            }
        }
    }

    public void setOfferId(int offerId) {
        this.idOffreSelectionnee = offerId;
        try {
            Offre offreActuelle = new OffreCRUD().getById(offerId);
            if (offreActuelle != null) {
                descriptionEngine.loadContent(offreActuelle.getDescription());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}