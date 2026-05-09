package controller.employers;

import entities.employers.compte;
import entities.employers.session;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import service.employers.compteCRUD;
import service.employers.hachageMotDePasse;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class changer_mot_de_passe implements Initializable {

    @FXML private PasswordField txtMotDePasseActuel;
    @FXML private PasswordField txtNouveauMotDePasse;
    @FXML private PasswordField txtConfirmationMotDePasse;
    @FXML private Label lblMessage;
    @FXML private Button btnConfirmer;

    @FXML private HBox strengthBar;
    @FXML private Region bar1;
    @FXML private Region bar2;
    @FXML private Region bar3;
    @FXML private Label lblStrength;

    private compteCRUD compteCrud;

    private static final String ERROR_STYLE = "-fx-border-color: #DC2626; -fx-border-width: 1; -fx-border-radius: 3;";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            compteCrud = new compteCRUD();
        } catch (SQLException e) {
            afficherMessage("Erreur de connexion à la base de données.", true);
        }
        txtNouveauMotDePasse.textProperty().addListener((obs, oldVal, newVal) -> {
            updateStrengthBar(newVal);
        });
    }

    @FXML
    private void confirmerChangement() {
        resetStyles();
        lblMessage.setText("");

        String actuel = txtMotDePasseActuel.getText();
        String nouveau = txtNouveauMotDePasse.getText();
        String confirmation = txtConfirmationMotDePasse.getText();
        boolean valid = true;

        if (actuel == null || actuel.trim().isEmpty()) {
            txtMotDePasseActuel.setStyle(ERROR_STYLE);
            valid = false;
        }

        if (nouveau == null || nouveau.trim().isEmpty()) {
            txtNouveauMotDePasse.setStyle(ERROR_STYLE);
            valid = false;
        }

        if (confirmation == null || confirmation.trim().isEmpty()) {
            txtConfirmationMotDePasse.setStyle(ERROR_STYLE);
            valid = false;
        }

        if (!valid) {
            afficherMessage("Veuillez remplir tous les champs.", true);
            return;
        }
        if (!nouveau.equals(confirmation)) {
            txtNouveauMotDePasse.setStyle(ERROR_STYLE);
            txtConfirmationMotDePasse.setStyle(ERROR_STYLE);
            afficherMessage("Les mots de passe ne correspondent pas.", true);
            return;
        }
        if (actuel.equals(nouveau)) {
            txtNouveauMotDePasse.setStyle(ERROR_STYLE);
            afficherMessage("Le nouveau mot de passe doit être différent de l'actuel.", true);
            return;
        }
        try {
            compte compteConnecte = session.getCompte();
            if (compteConnecte == null) {
                afficherMessage("Session expirée. Veuillez vous reconnecter.", true);
                return;
            }
            String hashedActuel = hachageMotDePasse.hashPassword(actuel);
            String hashedEnBase = compteConnecte.getPassword();

            if (!hashedActuel.equals(hashedEnBase)) {
                txtMotDePasseActuel.setStyle(ERROR_STYLE);
                afficherMessage("Le mot de passe actuel est incorrect.", true);
                return;
            }
            String hashedNouveau = hachageMotDePasse.hashPassword(nouveau);
            compteConnecte.setPassword(hashedNouveau);
            compteCrud.modifierMotDePasse(compteConnecte.getId(), hashedNouveau);
            session.setCompte(compteConnecte);

            afficherMessage("Mot de passe modifié avec succès !", false);
            txtMotDePasseActuel.setDisable(true);
            txtNouveauMotDePasse.setDisable(true);
            txtConfirmationMotDePasse.setDisable(true);
            btnConfirmer.setDisable(true);
            PauseTransition pause = new PauseTransition(Duration.seconds(2));
            pause.play();

        } catch (SQLException e) {
            afficherMessage("Erreur lors de la modification : " + e.getMessage(), true);
        }
    }
    private void resetStyles() {
        txtMotDePasseActuel.setStyle("");
        txtNouveauMotDePasse.setStyle("");
        txtConfirmationMotDePasse.setStyle("");
    }

    private void updateStrengthBar(String password) {
        if (password == null || password.isEmpty()) {
            strengthBar.setVisible(false);
            strengthBar.setManaged(false);
            return;
        }

        strengthBar.setVisible(true);
        strengthBar.setManaged(true);

    }

    private void afficherMessage(String message, boolean isError) {
        lblMessage.setText(message);
        lblMessage.setStyle(isError ? "-fx-text-fill: #DC2626;" : "-fx-text-fill: -color-success-fg;;");
        FadeTransition ft = new FadeTransition(Duration.millis(300), lblMessage);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }
}