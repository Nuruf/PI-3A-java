package controller.employers;

import entities.employers.competences_employe;
import entities.employers.employe;
import entities.employers.entreprise;
import entities.employers.role;
import entities.employers.session;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import service.employers.competence_employeCRUD;
import service.employers.employeCRUD;
import service.employers.entrepriseCRUD;
import utils.employers.json;
import utils.employers.UI;
import utils.employers.tagField;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class profil_employe implements Initializable {

    @FXML private VBox profileCard;
    @FXML private HBox profileRow;
    @FXML private StackPane avatarContainer;
    @FXML private Circle cercleArriere;
    @FXML private ImageView imgProfil;
    @FXML private VBox overlayPhoto;
    @FXML private Label lblNomComplet, lblEmailDisplay, lblRoleBadge;

    @FXML private TextField txtNomProfil, txtPrenomProfil, txtEmailProfil, txtTelProfil;
    @FXML private Label lblInfoMessage;

    @FXML private Label lblRoleInfo, lblPosteReadonly, lblDateReadonly;
    @FXML private Label lblEntrepriseNom, lblEntrepriseVille, lblEntreprisePays;

    @FXML private VBox competencesSection;
    @FXML private VBox containerSkills, containerFormations, containerExperience;
    @FXML private Label lblCompetencesStatus, lblCompSaveMsg;

    private tagField tagFieldSkills, tagFieldFormations, tagFieldExperience;

    private employeCRUD employeCRUD;
    private entrepriseCRUD entrepriseCrud;
    private competence_employeCRUD competenceCRUD;
    private employe employeConnecte;

    private static final String PROFILES_DIR = System.getProperty("user.home");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            employeCRUD = new employeCRUD();
            entrepriseCrud = new entrepriseCRUD();
            competenceCRUD = new competence_employeCRUD();
            chargerProfil();
            setupHoverEffects();
        } catch (Exception e) {
            UI.afficherErreur("Erreur d'initialisation", e.getMessage());
        }
    }

    private void chargerProfil() throws SQLException {
        if (session.getEmploye() != null) {
            employeConnecte = session.getEmploye();
        } else if (session.getCompte() != null) {
            employeConnecte = employeCRUD.getById(session.getCompte().getId_employe());
            session.setEmploye(employeConnecte);
        }
        if (employeConnecte == null) return;

        employeConnecte = employeCRUD.getById(employeConnecte.getId_employé());
        session.setEmploye(employeConnecte);

        remplirProfileRow();
        remplirFormulaire();
        remplirInfoPro();

        if (employeConnecte.getRole() == role.ADMINISTRATEUR_ENTREPRISE) {
            competencesSection.setVisible(false);
            competencesSection.setManaged(false);
        } else {
            competencesSection.setVisible(true);
            competencesSection.setManaged(true);
            initTagFields();
            chargerCompetences();
        }
    }
    private void initTagFields() {
        tagFieldSkills = new tagField("#EEF2FF", "#4338CA", "#C7D2FE", "Ajouter une compétence...");
        tagFieldFormations = new tagField("#F0FDF4", "#15803D", "#BBF7D0", "Ajouter une formation...");
        tagFieldExperience = new tagField("#FFF7ED", "#C2410C", "#FED7AA", "Ajouter une expérience...");

        tagFieldSkills.setOnTagsChanged(tags -> sauvegarderEnBDD());
        tagFieldFormations.setOnTagsChanged(tags -> sauvegarderEnBDD());
        tagFieldExperience.setOnTagsChanged(tags -> sauvegarderEnBDD());

        containerSkills.getChildren().setAll(tagFieldSkills);
        containerFormations.getChildren().setAll(tagFieldFormations);
        containerExperience.getChildren().setAll(tagFieldExperience);
    }

    private void sauvegarderEnBDD() {
        if (employeConnecte == null) return;

        try {
            String skillsJson = json.toJson(tagFieldSkills.getTags());
            String formationsJson = json.toJson(tagFieldFormations.getTags());
            String experienceJson = json.toJson(tagFieldExperience.getTags());

            competences_employe comp = new competences_employe(
                    employeConnecte.getId_employé(),
                    skillsJson,
                    formationsJson,
                    experienceJson
            );

            competenceCRUD.ajouter(comp);
            UI.afficherMessage(lblCompSaveMsg, "✓ Sauvegardé", false);

        } catch (SQLException e) {
            UI.afficherMessage(lblCompSaveMsg, "Erreur sauvegarde", true);
            e.printStackTrace();
        }
    }

    private void chargerCompetences() {
        try {
            competences_employe comp = competenceCRUD.getByEmploye(employeConnecte.getId_employé());

            if (comp == null) {
                lblCompetencesStatus.setText("Aucun CV analysé");
                lblCompetencesStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #868686;");
                return;
            }

            lblCompetencesStatus.setText("✓ Extraites du CV");
            lblCompetencesStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #16a34a;");

            tagFieldSkills.setTags(json.parseJsonArray(comp.getSkills()));
            tagFieldFormations.setTags(json.parseJsonArrayField(comp.getFormations(), "degree"));
            tagFieldExperience.setTags(json.parseJsonArrayField(comp.getExperience(), "job_title"));

        } catch (SQLException e) {
            lblCompetencesStatus.setText("Erreur de chargement");
            lblCompetencesStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #DC2626;");
        }
    }
    private void remplirProfileRow() {
        lblNomComplet.setText(employeConnecte.getPrenom() + " " + employeConnecte.getNom());
        lblEmailDisplay.setText(employeConnecte.getE_mail());
        styliserBadgeRole(lblRoleBadge, employeConnecte.getRole());
        chargerImageProfil();
    }
    private void remplirFormulaire() {
        txtNomProfil.setText(employeConnecte.getNom());
        txtPrenomProfil.setText(employeConnecte.getPrenom());
        txtEmailProfil.setText(employeConnecte.getE_mail());
        txtTelProfil.setText(String.valueOf(employeConnecte.getTelephone()));
    }

    private void remplirInfoPro() {
        lblRoleInfo.setText(employeConnecte.getRole().getLibelle());
        lblPosteReadonly.setText(employeConnecte.getPoste() != null ? employeConnecte.getPoste() : "—");
        lblDateReadonly.setText(employeConnecte.getDate_embauche() != null ? UI.formatDate(employeConnecte.getDate_embauche()) : "—");
        chargerEntreprise();
    }

    private void chargerEntreprise() {
        try {
            int idEntreprise = employeConnecte.getIdEntreprise();
            if (idEntreprise > 0) {
                entreprise ent = entrepriseCrud.getById(idEntreprise);
                if (ent != null) {
                    lblEntrepriseNom.setText(ent.getNom_entreprise());
                    lblEntrepriseVille.setText(ent.getVille() != null ? ent.getVille() : "—");
                    lblEntreprisePays.setText(ent.getPays() != null ? ent.getPays() : "—");
                    return;
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement entreprise: " + e.getMessage());
        }
        lblEntrepriseNom.setText("—");
        lblEntrepriseVille.setText("—");
        lblEntreprisePays.setText("—");
    }
    private void chargerImageProfil() {
        Image image = null;
        String imagePath = employeConnecte.getImageProfil();

        if (imagePath != null && !imagePath.isBlank()) {
            File testFile = new File(imagePath);
            if (testFile.isAbsolute() && testFile.exists()) {
                try {
                    image = new Image(testFile.toURI().toString(), 180, 180, true, true);
                } catch (Exception ignored) {}
            }
            if (image == null) {
                try {
                    URL resource = getClass().getResource(imagePath);
                    if (resource != null) image = new Image(resource.toExternalForm(), 180, 180, true, true);
                } catch (Exception ignored) {}
            }
        }

        if (image == null) {
            try {
                URL defaultUrl = getClass().getResource(employe.DEFAULT_IMAGE);
                if (defaultUrl != null) image = new Image(defaultUrl.toExternalForm(), 180, 180, true, true);
            } catch (Exception ignored) {}
        }

        if (image != null) {
            imgProfil.setImage(image);
            imgProfil.setSmooth(true);
            Circle clip = new Circle(45, 45, 45);
            imgProfil.setClip(clip);

            double w = image.getWidth(), h = image.getHeight(), side = Math.min(w, h);
            imgProfil.setViewport(new javafx.geometry.Rectangle2D((w - side) / 2, (h - side) / 2, side, side));
            imgProfil.setPreserveRatio(false);
        }
    }

    @FXML
    private void changerPhotoProfil() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une photo de profil");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File file = fc.showOpenDialog(avatarContainer.getScene().getWindow());
        if (file == null) return;

        try {
            String destPath = sauvegarderImage(file, employeConnecte.getId_employé());
            employeConnecte.setImageProfil(destPath);
            employeCRUD.modifier(employeConnecte);
            session.setEmploye(employeConnecte);
            chargerImageProfil();
            UI.afficherMessage(lblInfoMessage, "✓ Photo mise à jour", false);
        } catch (Exception e) {
            UI.afficherMessage(lblInfoMessage, "Erreur : " + e.getMessage(), true);
        }
    }

    private String sauvegarderImage(File source, int idEmploye) throws Exception {
        File dir = new File(PROFILES_DIR);
        if (!dir.exists()) dir.mkdirs();

        String extension = source.getName().substring(source.getName().lastIndexOf("."));
        File dest = new File(dir, "profile_" + idEmploye + "_" + System.currentTimeMillis() + extension);
        Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

        if (employeConnecte.hasCustomImage()) {
            try {
                File oldFile = new File(employeConnecte.getImageProfil());
                if (oldFile.exists() && !oldFile.getAbsolutePath().equals(dest.getAbsolutePath()))
                    oldFile.delete();
            } catch (Exception ignored) {}
        }
        return dest.getAbsolutePath();
    }

    private void setupHoverEffects() {
        avatarContainer.setOnMouseEntered(e -> {
            overlayPhoto.setVisible(true);
            overlayPhoto.setManaged(true);
            FadeTransition ft = new FadeTransition(Duration.millis(200), overlayPhoto);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        });
        avatarContainer.setOnMouseExited(e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(200), overlayPhoto);
            ft.setFromValue(1);
            ft.setToValue(0);
            ft.setOnFinished(ev -> {
                overlayPhoto.setVisible(false);
                overlayPhoto.setManaged(false);
            });
            ft.play();
        });
    }
    @FXML
    private void sauvegarderInfos() {
        lblInfoMessage.setText("");

        String nom = txtNomProfil.getText().trim();
        String prenom = txtPrenomProfil.getText().trim();
        String email = txtEmailProfil.getText().trim();
        String tel = txtTelProfil.getText().trim();

        UI.effacerErreur(txtNomProfil);
        UI.effacerErreur(txtPrenomProfil);
        UI.effacerErreur(txtEmailProfil);
        UI.effacerErreur(txtTelProfil);

        boolean valid = true;

        if (nom.isEmpty()) {
            UI.marquerErreur(txtNomProfil);
            valid = false;
        }
        if (prenom.isEmpty()) {
            UI.marquerErreur(txtPrenomProfil);
            valid = false;
        }
        if (!UI.validerEmail(email)) {
            UI.marquerErreur(txtEmailProfil);
            valid = false;
        }
        if (tel.isEmpty()) {
            UI.marquerErreur(txtTelProfil);
            valid = false;
        } else {
            try {
                Integer.parseInt(tel);
            } catch (NumberFormatException e) {
                UI.marquerErreur(txtTelProfil);
                valid = false;
            }
        }

        if (!valid) {
            UI.afficherMessage(lblInfoMessage, "Champ vide ou email invalide ou numéro téléphone invalide", true);
            return;
        }

        try {
            employeConnecte.setNom(nom);
            employeConnecte.setPrenom(prenom);
            employeConnecte.setE_mail(email);
            employeConnecte.setTelephone(Integer.parseInt(tel));
            employeCRUD.modifier(employeConnecte);
            session.setEmploye(employeConnecte);
            remplirProfileRow();
            UI.afficherMessage(lblInfoMessage, "Informations mises à jour", false);
        } catch (SQLException e) {
            UI.afficherMessage(lblInfoMessage, "Erreur : " + e.getMessage(), true);
        }
    }

    @FXML
    private void changerMotDePasse() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/emp/changer_mot_de_passe.fxml"));
            VBox content = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(content);
            scene.getStylesheets().addAll(avatarContainer.getScene().getStylesheets());

            javafx.stage.Stage dialog = new javafx.stage.Stage();
            dialog.setTitle("Changer le mot de passe");
            dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialog.initOwner(avatarContainer.getScene().getWindow());
            dialog.setScene(scene);
            dialog.setResizable(false);
            dialog.centerOnScreen();
            dialog.showAndWait();
        } catch (Exception e) {
            UI.afficherErreur("Erreur", "Impossible d'ouvrir la fenêtre : " + e.getMessage());
        }
    }
    private void styliserBadgeRole(Label badge, role r) {
        badge.setText(r.getLibelle());
        String base = "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 12; -fx-background-radius: 15;";
        switch (r) {
            case ADMINISTRATEUR_ENTREPRISE -> badge.setStyle(base + "-fx-background-color: -color-accent-muted; -fx-text-fill: -color-accent-fg;");
            case RH -> badge.setStyle(base + "-fx-background-color: -color-danger-muted; -fx-text-fill: -color-danger-fg;");
            case CHEF_PROJET -> badge.setStyle(base + "-fx-background-color: -color-success-muted; -fx-text-fill: -color-success-fg;");
            default -> badge.setStyle(base + "-fx-background-color: -color-bg-subtle; -fx-text-fill: -color-fg-muted; -fx-border-color: -color-border-muted; -fx-border-radius: 15;");
        }
    }
}