package controller.employers;

import entities.employers.entreprise;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import service.employers.entrepriseCRUD;
import utils.employers.UI;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;

public class compteEntreprise {

    @FXML private TextField e_mailField;
    @FXML private TextField matriculeFiescaleField;
    @FXML private TextField nomEField;
    @FXML private TextField prenomField;
    @FXML private TextField nomField;
    @FXML private TextField paysField;
    @FXML private TextField telephoneField;
    @FXML private TextField villeField;
    @FXML private TextField siteWebField;
    @FXML private Button btnChoisirLogo;
    @FXML private HBox logoPreview;
    @FXML private ImageView imgLogoPreview;
    @FXML private Label lblLogoName;
    @FXML private Label msgError;

    private entrepriseCRUD entrepriseCRUD;
    private String logoPath = null;

    private static final String LOGOS_DIR = System.getProperty("user.home");

    @FXML
    void initialize() throws SQLException {
        entrepriseCRUD = new entrepriseCRUD();
        msgError.setText("");
    }

    @FXML
    private void choisirLogo() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir le logo de l'entreprise");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));

        File file = fc.showOpenDialog(btnChoisirLogo.getScene().getWindow());
        if (file == null) return;

        if (file.length() > 5 * 1024 * 1024) {
            msgError.setText("Logo trop volumineux (max 5 Mo)");
            return;
        }

        try {
            Path dirPath = Path.of(LOGOS_DIR);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            String extension = file.getName().substring(file.getName().lastIndexOf("."));
            String fileName = "logo_" + System.currentTimeMillis() + extension;
            Path dest = dirPath.resolve(fileName);
            Files.copy(file.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
            logoPath = dest.toAbsolutePath().toString();

            Image img = new Image(dest.toUri().toString(), 30, 30, true, true);
            imgLogoPreview.setImage(img);
            lblLogoName.setText(file.getName());
            logoPreview.setVisible(true);
            logoPreview.setManaged(true);

            btnChoisirLogo.setText("Logo sélectionné");
            btnChoisirLogo.setStyle(" -fx-background-radius: 10;-fx-border-color: #16a34a; -fx-border-radius: 10; -fx-text-fill: #16a34a; -fx-cursor: hand;");

        } catch (Exception e) {
            msgError.setText("Erreur lors du chargement du logo.");
        }
    }

    @FXML
    private void supprimerLogo() {
        if (logoPath != null) {
            try {
                Files.deleteIfExists(Path.of(logoPath));
            } catch (IOException ignored) {}
        }
        resetLogoUI();
    }

    private void resetLogoUI() {
        logoPath = null;
        imgLogoPreview.setImage(null);
        lblLogoName.setText("");
        logoPreview.setVisible(false);
        logoPreview.setManaged(false);

        btnChoisirLogo.setText("Logo (optionnel)");
        btnChoisirLogo.setStyle(" -fx-background-radius: 10;-fx-border-color: #e5e7eb; -fx-border-radius: 10; -fx-text-fill: #6b7280; -fx-cursor: hand;");
    }

    @FXML
    public void ajouterCompteEntreprise() {
        msgError.setText("");
        msgError.setStyle("-fx-text-fill: #DC2626;");

        if (!validerFormulaire()) return;

        try {
            int telephone = Integer.parseInt(telephoneField.getText().trim());
            String siteWeb = siteWebField.getText().trim();

            entreprise ent = new entreprise(
                    nomEField.getText().trim(),
                    paysField.getText().trim(),
                    villeField.getText().trim(),
                    nomField.getText().trim(),
                    prenomField.getText().trim(),
                    matriculeFiescaleField.getText().trim(),
                    telephone,
                    e_mailField.getText().trim(),
                    siteWeb.isEmpty() ? null : siteWeb,
                    logoPath
            );

            entrepriseCRUD.ajouter(ent);
            UI.afficherSucces("Succès", "Votre demande a été envoyée avec succès.");
            viderFormulaire();

        } catch (NumberFormatException e) {
            msgError.setText("Le téléphone ne doit contenir que des chiffres.");
        } catch (SQLException e) {
            msgError.setText("Erreur technique : " + e.getMessage());
        }
    }

    private boolean validerFormulaire() {
        // Reset erreurs
        UI.effacerErreur(nomEField);
        UI.effacerErreur(paysField);
        UI.effacerErreur(villeField);
        UI.effacerErreur(nomField);
        UI.effacerErreur(prenomField);
        UI.effacerErreur(matriculeFiescaleField);
        UI.effacerErreur(telephoneField);
        UI.effacerErreur(e_mailField);
        UI.effacerErreur(siteWebField);

        boolean valid = true;

        if (nomEField.getText().trim().isEmpty()) {
            UI.marquerErreur(nomEField);
            valid = false;
        }
        if (paysField.getText().trim().isEmpty()) {
            UI.marquerErreur(paysField);
            valid = false;
        }
        if (villeField.getText().trim().isEmpty()) {
            UI.marquerErreur(villeField);
            valid = false;
        }
        if (nomField.getText().trim().isEmpty()) {
            UI.marquerErreur(nomField);
            valid = false;
        }
        if (prenomField.getText().trim().isEmpty()) {
            UI.marquerErreur(prenomField);
            valid = false;
        }
        if (matriculeFiescaleField.getText().trim().isEmpty()) {
            UI.marquerErreur(matriculeFiescaleField);
            valid = false;
        }
        if (telephoneField.getText().trim().isEmpty()) {
            UI.marquerErreur(telephoneField);
            valid = false;
        }

        if (!UI.validerEmail(e_mailField.getText().trim())) {
            UI.marquerErreur(e_mailField);
            valid = false;
        }

        // Site web (optionnel mais validé si rempli)
        String siteWeb = siteWebField.getText().trim();
        if (!siteWeb.isEmpty() && !siteWeb.matches("^(https?://)?[\\w.-]+\\.[a-zA-Z]{2,}(/.*)?$")) {
            UI.marquerErreur(siteWebField);
            msgError.setText("Format du site web invalide.");
            return false;
        }

        if (!valid) {
            msgError.setText("Veuillez remplir tous les champs obligatoires.");
        }

        return valid;
    }

    private void viderFormulaire() {
        nomEField.clear();
        paysField.clear();
        villeField.clear();
        nomField.clear();
        prenomField.clear();
        e_mailField.clear();
        matriculeFiescaleField.clear();
        telephoneField.clear();
        siteWebField.clear();
        resetLogoUI();
        msgError.setText("");
    }

    @FXML
    void naviguerVersLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/emp/login.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Connexion");
            stage.show();

            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.close();
        } catch (IOException e) {
            UI.afficherErreur("Erreur", "Impossible d'ouvrir la page de connexion.");
        }
    }
}