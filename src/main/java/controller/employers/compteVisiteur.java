package controller.employers;

import entities.employers.visiteur;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import service.employers.visiteurCRUD;
import utils.employers.UI;

import java.io.IOException;
import java.sql.SQLException;

public class compteVisiteur {

    @FXML private TextField txtNom;
    @FXML private TextField txtPrenom;
    @FXML private TextField txtEmail;
    @FXML private TextField txtTelephone;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Label lblError;

    private visiteurCRUD visiteurService;

    @FXML
    void initialize() {
        try {
            visiteurService = new visiteurCRUD();
            lblError.setText("");
        } catch (SQLException e) {
            lblError.setText("Erreur de connexion à la base de données.");
        }
    }

    @FXML
    private void creerCompte() {
        lblError.setText("");

        if (!validerFormulaire()) return;

        try {
            visiteur v = new visiteur(
                    txtNom.getText().trim(),
                    txtPrenom.getText().trim(),
                    txtEmail.getText().trim(),
                    txtPassword.getText(),
                    Integer.parseInt(txtTelephone.getText().trim())
            );

            visiteurService.ajouter(v);
            UI.afficherSucces("Succès", "Votre compte a été créé avec succès !\nConnectez-vous depuis la page principale.");
            viderFormulaire();
            naviguerVersLogin();

        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("Duplicate")) {
                lblError.setText("Cet email est déjà utilisé.");
            } else {
                lblError.setText("Erreur lors de la création du compte.");
            }
        }
    }

    private boolean validerFormulaire() {
        UI.effacerErreur(txtNom);
        UI.effacerErreur(txtPrenom);
        UI.effacerErreur(txtEmail);
        UI.effacerErreur(txtTelephone);
        UI.effacerErreur(txtPassword);
        UI.effacerErreur(txtConfirmPassword);

        boolean valid = true;
        String nom = txtNom.getText().trim();
        String prenom = txtPrenom.getText().trim();
        String email = txtEmail.getText().trim();
        String tel = txtTelephone.getText().trim();
        String pwd = txtPassword.getText();
        String confirmPwd = txtConfirmPassword.getText();
        if (nom.isEmpty()) {
            UI.marquerErreur(txtNom);
            valid = false;
        }
        if (prenom.isEmpty()) {
            UI.marquerErreur(txtPrenom);
            valid = false;
        }
        if (email.isEmpty()) {
            UI.marquerErreur(txtEmail);
            valid = false;
        }
        if (tel.isEmpty()) {
            UI.marquerErreur(txtTelephone);
            valid = false;
        }
        if (pwd.isEmpty()) {
            UI.marquerErreur(txtPassword);
            valid = false;
        }
        if (confirmPwd.isEmpty()) {
            UI.marquerErreur(txtConfirmPassword);
            valid = false;
        }

        if (!valid) {
            lblError.setText("Veuillez remplir tous les champs.");
            return false;
        }
        if (!nom.matches("[a-zA-ZÀ-ÿ\\s-]+")) {
            UI.marquerErreur(txtNom);
            lblError.setText("Le nom ne doit contenir que des lettres.");
            return false;
        }
        if (!prenom.matches("[a-zA-ZÀ-ÿ\\s-]+")) {
            UI.marquerErreur(txtPrenom);
            lblError.setText("Le prénom ne doit contenir que des lettres.");
            return false;
        }
        if (!UI.validerEmail(email)) {
            UI.marquerErreur(txtEmail);
            lblError.setText("Adresse email invalide.");
            return false;
        }

        if (pwd.length() < 6) {
            UI.marquerErreur(txtPassword);
            lblError.setText("Le mot de passe doit contenir au moins 6 caractères.");
            return false;
        }

        if (!pwd.equals(confirmPwd)) {
            UI.marquerErreur(txtPassword);
            UI.marquerErreur(txtConfirmPassword);
            lblError.setText("Les mots de passe ne correspondent pas.");
            return false;
        }

        return true;
    }

    private void viderFormulaire() {
        txtNom.clear();
        txtPrenom.clear();
        txtEmail.clear();
        txtTelephone.clear();
        txtPassword.clear();
        txtConfirmPassword.clear();
        lblError.setText("");
    }

    private void naviguerVersLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/emp/login.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Connexion");
            stage.show();

            Stage currentStage = (Stage) txtNom.getScene().getWindow();
            currentStage.close();
        } catch (IOException e) {
            UI.afficherErreur("Erreur", "Impossible d'ouvrir la page de connexion.");
        }
    }

    @FXML
    void retourLogin(ActionEvent event) {
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