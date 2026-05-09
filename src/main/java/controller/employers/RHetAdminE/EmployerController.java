package controller.employers.RHetAdminE;

import entities.employers.employe;
import entities.employers.role;
import entities.employers.session;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import service.employers.employeCRUD;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class EmployerController implements Initializable {

    @FXML private TabPane tabPane;
    @FXML private Tab tabListe, tabAjouter;
    @FXML private VBox employesContainer;
    @FXML private Label lblNombreEmployes;
    @FXML private Label lblTotalEmployes, lblTotalRH, lblTotalChefs, lblTotalSimple;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterRole;

    @FXML private VBox sidePanel, panelAvatarBlock;
    @FXML private Label lblPanelTitle, lblError;
    @FXML private TextField txtNom, txtPrenom, txtEmail, txtTelephone, txtPoste;
    @FXML private ComboBox<String> comboRole;
    @FXML private DatePicker dateEmbauche;
    @FXML private Button btnSave, btnDelete, btnClosePanel;
    @FXML private Button btnChoisirCVPanel;
    @FXML private HBox cvPreviewPanel;
    @FXML private Label lblCVNamePanel;
    @FXML private VBox formCardAjout;
    @FXML private Label lblErrorAjout;
    @FXML private TextField txtNomAjout, txtPrenomAjout, txtEmailAjout, txtTelephoneAjout, txtPosteAjout;
    @FXML private ComboBox<String> comboRoleAjout;
    @FXML private DatePicker dateEmbaucheAjout;
    @FXML private Button btnImportCSV, btnChoisirCV;
    @FXML private HBox cvPreview;
    @FXML private Label lblCVName;

    private employeCRUD employeCRUD;
    private employe employeSelectionne;
    private int idEntrepriseConnecte;
    private List<employe> tousEmployes;
    private VBox carteSelectionnee;
    private byte[] cvBytesTemp = null;
    private String cvNomTemp = null;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            employeCRUD = new employeCRUD();
            initSession();
            initComboBoxRoles();
            setupFilters();
            chargerEmployes();
        } catch (SQLException e) {
            afficherErreur("Erreur Init", e.getMessage());
        }
    }

    @FXML
    private void choisirCV() {
        File file = choisirFichierCV();
        if (file == null) return;

        try {
            cvBytesTemp = Files.readAllBytes(file.toPath());
            cvNomTemp = file.getName();

            lblCVName.setText(file.getName());
            cvPreview.setVisible(true);
            cvPreview.setManaged(true);

            btnChoisirCV.setText("CV sélectionné");
            btnChoisirCV.setStyle("-fx-background-color: #f0fdf4;-fx-background-radius: 5;-fx-border-color: #16a34a;-fx-border-radius: 5;-fx-text-fill: #16a34a;-fx-cursor: hand;");
        } catch (IOException e) {
            lblErrorAjout.setText("Erreur lors de la lecture du CV.");
            lblErrorAjout.setStyle("-fx-text-fill: -color-danger-fg;");
        }
    }

    @FXML
    private void supprimerCVAjout() {
        cvBytesTemp = null;
        cvNomTemp = null;
        cvPreview.setVisible(false);
        cvPreview.setManaged(false);
        btnChoisirCV.setText("📄 Télécharger un CV");
        btnChoisirCV.setStyle("-fx-background-color: #f9fafb;-fx-background-radius: 5;-fx-border-color: #e5e7eb;-fx-border-radius: 5;-fx-text-fill: #303030;-fx-cursor: hand;"
        );
    }

    @FXML
    private void choisirCVPanel() {
        if (employeSelectionne == null) return;
        File file = choisirFichierCV();
        if (file == null) return;

        try {
            employeSelectionne.setCv_data(Files.readAllBytes(file.toPath()));
            employeSelectionne.setCv_nom(file.getName());
            employeCRUD.modifier(employeSelectionne);

            lblError.setText(" CV mis à jour");
            lblError.setStyle("-fx-text-fill: -color-success-fg;-fx-font-weight: bold;");
            majPanelCV(employeSelectionne);
        } catch (Exception e) {
            lblError.setText("Erreur modification CV.");
            lblError.setStyle("-fx-text-fill: -color-danger-fg;");
        }
    }

    @FXML
    private void ouvrirCV() {
        if (employeSelectionne == null || !employeSelectionne.hasCv()) return;
        try {
            String nom = employeSelectionne.getCv_nom();
            if (nom == null || nom.isEmpty()) nom = "cv.pdf";

            File temp = File.createTempFile("cv_", "_" + nom);
            Files.write(temp.toPath(), employeSelectionne.getCv_data());
            temp.deleteOnExit();

            Desktop.getDesktop().open(temp);
        } catch (IOException e) {
            afficherErreur("Erreur", "Impossible d'ouvrir le CV.");
        }
    }

    @FXML
    private void supprimerCVPanel() {
        if (employeSelectionne == null) return;
        if (!confirmer("Supprimer le CV", "Voulez-vous supprimer le CV ?")) return;

        try {
            employeSelectionne.setCv_data(null);
            employeSelectionne.setCv_nom(null);
            employeCRUD.modifier(employeSelectionne);

            lblError.setText(" CV supprimé");
            lblError.setStyle("-fx-text-fill: -color-success-fg;-fx-font-weight: bold;");
            majPanelCV(employeSelectionne);
        } catch (SQLException e) {
            lblError.setText("Erreur suppression CV.");
            lblError.setStyle("-fx-text-fill: -color-danger-fg;");
        }
    }

    private File choisirFichierCV() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir un CV (PDF)");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        File file = fc.showOpenDialog(tabPane.getScene().getWindow());
        if (file != null && file.length() > 10 * 1024 * 1024) {
            afficherErreur("Fichier trop gros", "Le CV ne doit pas dépasser 10 Mo.");
            return null;
        }
        return file;
    }

    private void majPanelCV(employe emp) {
        boolean isAdmin = emp.getRole() == role.ADMINISTRATEUR_ENTREPRISE;

        if (emp.hasCv()) {
            lblCVNamePanel.setText(emp.getCv_nom() != null ? emp.getCv_nom() : "CV");
            cvPreviewPanel.setVisible(true);
            cvPreviewPanel.setManaged(true);

            btnChoisirCVPanel.setText(" 📄 Modifier le CV");
            btnChoisirCVPanel.setStyle("-fx-background-color: #f9fafb;-fx-background-radius: 5;-fx-border-color: #e5e7eb;-fx-border-radius: 5;-fx-text-fill: #303030;-fx-cursor: hand;");
        } else {
            cvPreviewPanel.setVisible(false);
            cvPreviewPanel.setManaged(false);

            btnChoisirCVPanel.setText("📄 Télécharger un CV");
            btnChoisirCVPanel.setStyle("-fx-background-color: #f9fafb;-fx-background-radius: 5;-fx-border-color: #e5e7eb;-fx-border-radius: 5;-fx-text-fill: #303030;-fx-cursor: hand;");
        }

        btnChoisirCVPanel.setDisable(isAdmin);
    }
    @FXML
    private void ajouterEmploye() {
        if (!validerFormulaire(txtNomAjout, txtPrenomAjout, txtEmailAjout,
                txtTelephoneAjout, txtPosteAjout, comboRoleAjout, lblErrorAjout))
            return;

        try {
            employe nv = new employe();
            nv.setNom(txtNomAjout.getText().trim());
            nv.setPrenom(txtPrenomAjout.getText().trim());
            nv.setE_mail(txtEmailAjout.getText().trim());
            nv.setTelephone(Integer.parseInt(txtTelephoneAjout.getText().trim()));
            nv.setPoste(txtPosteAjout.getText().trim());
            nv.setRole(role.fromString(comboRoleAjout.getValue()));
            nv.setDate_embauche(dateEmbaucheAjout.getValue());
            nv.setIdEntreprise(idEntrepriseConnecte);
            nv.setCv_data(cvBytesTemp);
            nv.setCv_nom(cvNomTemp);

            employeCRUD.add(nv);
            String message = "L'employé a été ajouté avec succès.";
            if (cvBytesTemp != null) {
                message += "\n\nLe CV est en cours d'analyse.\nLes compétences seront extraites automatiquement.";
            }
            afficherSucces("Succès", "L'employé a été ajouté.");
            retourListe();

        } catch (SQLException e) {
            System.err.println("❌ Erreur SQL ajout employé : " + e.getMessage());
            e.printStackTrace();

            lblErrorAjout.setText("Erreur lors de l'ajout : " + e.getMessage());
            lblErrorAjout.setStyle("-fx-text-fill: -color-danger-fg;");
        } catch (NumberFormatException e) {
            lblErrorAjout.setText("Format téléphone invalide.");
            lblErrorAjout.setStyle("-fx-text-fill: -color-danger-fg;");
        }
    }
    private void remplirPanel(employe emp) {
        panelAvatarBlock.getChildren().clear();
        StackPane avatar = creerAvatar(emp, 60);
        Label fullName = new Label(emp.getPrenom() + " " + emp.getNom());
        fullName.setStyle("-fx-font-weight: bold;-fx-font-size: 16px;");
        Label roleBadge = creerBadgeRole(emp.getRole());
        panelAvatarBlock.getChildren().addAll(avatar, fullName, roleBadge);

        txtNom.setText(emp.getNom());
        txtPrenom.setText(emp.getPrenom());
        txtEmail.setText(emp.getE_mail());
        txtTelephone.setText(String.valueOf(emp.getTelephone()));
        txtPoste.setText(emp.getPoste());
        comboRole.setValue(emp.getRole().getLibelle());
        dateEmbauche.setValue(emp.getDate_embauche());
        lblError.setText("");

        boolean isAdmin = emp.getRole() == role.ADMINISTRATEUR_ENTREPRISE;
        verrouillerChamps(isAdmin);
        lblPanelTitle.setText(isAdmin ? "Fiche Administrateur" : "Fiche Employé");

        majPanelCV(emp);
    }

    @FXML
    private void sauvegarderModifications() {
        if (employeSelectionne == null) return;
        if (!validerFormulaire(txtNom, txtPrenom, txtEmail, txtTelephone, txtPoste, comboRole, lblError)) return;

        try {
            employeSelectionne.setNom(txtNom.getText().trim());
            employeSelectionne.setPrenom(txtPrenom.getText().trim());
            employeSelectionne.setE_mail(txtEmail.getText().trim());
            employeSelectionne.setTelephone(Integer.parseInt(txtTelephone.getText().trim()));
            employeSelectionne.setPoste(txtPoste.getText().trim());
            employeSelectionne.setRole(role.fromString(comboRole.getValue()));
            employeSelectionne.setDate_embauche(dateEmbauche.getValue());
            employeCRUD.modifier(employeSelectionne);
            lblError.setText(" Modifications enregistrées");
            lblError.setStyle("-fx-text-fill: -color-success-fg;-fx-font-weight: bold;");
            chargerEmployes();
            remplirPanel(employeSelectionne);
        } catch (SQLException e) {
            lblError.setText("Erreur : " + e.getMessage());
            lblError.setStyle("-fx-text-fill: -color-danger-fg;");
        } catch (NumberFormatException e) {
            lblError.setText("Téléphone invalide.");
            lblError.setStyle("-fx-text-fill: -color-danger-fg;");
        }
    }

    @FXML
    private void supprimerDepuisPanel() {
        if (employeSelectionne == null) return;
        if (confirmer("Suppression", "Supprimer " + employeSelectionne.getPrenom() + " " + employeSelectionne.getNom() + " ?")) {
            try {
                employeCRUD.supprimer(employeSelectionne.getId_employé());
                fermerPanel();
                chargerEmployes();
            } catch (SQLException e) {
                afficherErreur("Erreur", e.getMessage());
            }
        }
    }
    @FXML
    private void ouvrirAjoutEmploye() {
        fermerPanel();
        resetFormulaireAjout();
        tabAjouter.setDisable(false);
        tabPane.getSelectionModel().select(tabAjouter);
    }

    @FXML
    public void retourListe() {
        tabAjouter.setDisable(true);
        tabPane.getSelectionModel().select(tabListe);
        chargerEmployes();
    }

    private void resetFormulaireAjout() {
        txtNomAjout.clear();
        txtPrenomAjout.clear();
        txtEmailAjout.clear();
        txtTelephoneAjout.clear();
        txtPosteAjout.clear();
        comboRoleAjout.setValue(null);
        dateEmbaucheAjout.setValue(LocalDate.now());
        lblErrorAjout.setText("");
        supprimerCVAjout();
    }
    private void selectionnerEmploye(employe emp, VBox card) {
        if (carteSelectionnee != null) appliquerStyleCarte(carteSelectionnee, false);
        employeSelectionne = emp;
        carteSelectionnee = card;
        appliquerStyleCarte(card, true);
        remplirPanel(emp);
        if (!sidePanel.isVisible()) ouvrirPanel();
    }

    private void ouvrirPanel() {
        sidePanel.setVisible(true);
        sidePanel.setManaged(true);
        TranslateTransition tt = new TranslateTransition(Duration.millis(250), sidePanel);
        tt.setFromX(380); tt.setToX(0); tt.play();
        FadeTransition ft = new FadeTransition(Duration.millis(250), sidePanel);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    @FXML
    private void fermerPanel() {
        if (carteSelectionnee != null) {
            appliquerStyleCarte(carteSelectionnee, false);
            carteSelectionnee = null;
        }
        employeSelectionne = null;
        TranslateTransition tt = new TranslateTransition(Duration.millis(200), sidePanel);
        tt.setFromX(0); tt.setToX(380);
        FadeTransition ft = new FadeTransition(Duration.millis(200), sidePanel);
        ft.setFromValue(1); ft.setToValue(0);
        ft.setOnFinished(e -> {
            sidePanel.setVisible(false);
            sidePanel.setManaged(false);
            sidePanel.setTranslateX(0);
        });
        tt.play(); ft.play();
    }

    private void verrouillerChamps(boolean verrouiller) {
        Control[] champs = {txtNom, txtPrenom, txtEmail, txtTelephone, txtPoste, comboRole, dateEmbauche};
        for (Control champ : champs) {
            champ.setDisable(verrouiller);
            champ.setStyle(verrouiller ? "-fx-opacity: 1.0;-fx-background-color: -color-bg-subtle;" + "-fx-text-fill: -color-fg-muted;-fx-border-color: -color-border-muted;" + "-fx-border-radius: 5;-fx-background-radius: 5;" : "");
        }
        btnSave.setVisible(!verrouiller); btnSave.setManaged(!verrouiller);
        btnDelete.setVisible(!verrouiller); btnDelete.setManaged(!verrouiller);
        btnChoisirCVPanel.setDisable(verrouiller);

        if (verrouiller) {
            lblError.setText("🔒 Compte administrateur — modification non autorisée");
            lblError.setStyle("-fx-text-fill: #F59E0B;-fx-font-size: 11px;-fx-font-style: italic;");
        } else {
            lblError.setText("");
            lblError.setStyle("");
        }
    }
    private void initSession() throws SQLException {
        if (session.getEmploye() != null) {
            idEntrepriseConnecte = session.getEmploye().getIdEntreprise();
        } else if (session.getCompte() != null) {
            employe emp = employeCRUD.getById(session.getCompte().getId_employe());
            if (emp != null) {
                session.setEmploye(emp);
                idEntrepriseConnecte = emp.getIdEntreprise();
            }
        }
    }

    private void initComboBoxRoles() {
        List<String> roles = List.of(role.RH.getLibelle(), role.EMPLOYE.getLibelle(), role.CHEF_PROJET.getLibelle());
        comboRole.setItems(FXCollections.observableArrayList(roles));
        comboRoleAjout.setItems(FXCollections.observableArrayList(roles));
    }

    private void setupFilters() {
        filterRole.setItems(FXCollections.observableArrayList("Tous", "RH", "Chef de projet", "Employé", "Admin entreprise"));
        filterRole.setValue("Tous");
        filterRole.setOnAction(e -> appliquerFiltres());
        searchField.textProperty().addListener((obs, o, n) -> appliquerFiltres());
    }

    private void chargerEmployes() {
        try {
            tousEmployes = employeCRUD.afficher(idEntrepriseConnecte);
            mettreAJourStats(tousEmployes);
            appliquerFiltres();
        } catch (SQLException e) {
            afficherErreur("Erreur de chargement", e.getMessage());
        }
    }

    private void appliquerFiltres() {
        if (tousEmployes == null) return;
        String recherche = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
        String filtreRole = filterRole.getValue();
        List<employe> filtres = tousEmployes.stream().filter(e -> filtrerParRole(e, filtreRole)).filter(e -> filtrerParRecherche(e, recherche)).collect(Collectors.toList());
        afficherEmployes(filtres);
    }

    private boolean filtrerParRole(employe e, String filtre) {
        if (filtre == null || "Tous".equals(filtre)) return true;
        return switch (filtre) {
            case "RH" -> e.getRole() == role.RH;
            case "Chef de projet" -> e.getRole() == role.CHEF_PROJET;
            case "Employé" -> e.getRole() == role.EMPLOYE;
            case "Admin entreprise" -> e.getRole() == role.ADMINISTRATEUR_ENTREPRISE;
            default -> true;
        };
    }

    private boolean filtrerParRecherche(employe e, String r) {
        if (r.isEmpty()) return true;
        return e.getNom().toLowerCase().contains(r) || e.getPrenom().toLowerCase().contains(r) || e.getPoste().toLowerCase().contains(r) || e.getE_mail().toLowerCase().contains(r);
    }

    private void afficherEmployes(List<employe> employes) {
        employesContainer.getChildren().clear();
        lblNombreEmployes.setText(employes.size() + " employé" + (employes.size() > 1 ? "s" : ""));
        if (employes.isEmpty()) {
            employesContainer.getChildren().add(creerMessageVide());
            return;
        }
        for (employe emp : employes) {
            employesContainer.getChildren().add(creerCarteEmploye(emp));
        }
    }

    private void mettreAJourStats(List<employe> employes) {
        lblTotalEmployes.setText(String.valueOf(employes.size()));
        lblTotalRH.setText(String.valueOf(employes.stream().filter(e -> e.getRole() == role.RH).count()));
        lblTotalChefs.setText(String.valueOf(employes.stream().filter(e -> e.getRole() == role.CHEF_PROJET).count()));
        lblTotalSimple.setText(String.valueOf(employes.stream().filter(e -> e.getRole() == role.EMPLOYE).count()));
    }
    @FXML
    private void importerDepuisCSV() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Importer CSV Employés");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers CSV", "*.csv"));
        File file = fc.showOpenDialog(tabPane.getScene().getWindow());
        if (file == null) return;
        int ok = 0, err = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line; int i = 0;
            while ((line = br.readLine()) != null) {
                if (i++ == 0 && line.toLowerCase().contains("nom")) continue;
                String[] p = line.split(";", -1);
                if (p.length < 6) { err++; continue; }
                try {
                    employe e = new employe();
                    e.setNom(p[0].trim()); e.setPrenom(p[1].trim());
                    e.setE_mail(p[2].trim());
                    e.setTelephone(Integer.parseInt(p[3].trim()));
                    e.setPoste(p[4].trim()); e.setRole(parseRole(p[5].trim()));
                    e.setDate_embauche(p.length > 6 && !p[6].isEmpty() ? LocalDate.parse(p[6].trim()) : null);
                    e.setIdEntreprise(idEntrepriseConnecte);
                    employeCRUD.add(e); ok++;
                } catch (Exception ex) { err++; }
            }
            chargerEmployes();
            afficherSucces("Import CSV", ok + " importé" + (ok > 1 ? "s" : "") + " avec succès." + (err > 0 ? "\n" + err + " erreur(s)." : ""));
        } catch (Exception e) {
            afficherErreur("Erreur CSV", e.getMessage());
        }
    }
    private VBox creerCarteEmploye(employe emp) {
        VBox card = new VBox();
        card.getStyleClass().add("glass-card");
        appliquerStyleCarte(card, false);
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(5, 20, 5, 20));
        row.setStyle("-fx-cursor: hand;");
        row.setOnMouseEntered(e -> {
            if (card != carteSelectionnee)
                card.setStyle("-fx-background-color: -color-bg-default; -fx-background-radius: 10; -fx-padding: 15; -fx-border-color: -color-accent-muted; -fx-border-radius: 10; -fx-border-width: 1;");
        });
        row.setOnMouseExited(e -> {
            if (card != carteSelectionnee) appliquerStyleCarte(card, false);
        });
        row.setOnMouseClicked(e -> selectionnerEmploye(emp, card));

        StackPane av = creerAvatar(emp, 38);
        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label lblNomCard = new Label(emp.getPrenom() + " " + emp.getNom());
        lblNomCard.setStyle("-fx-font-weight: bold;-fx-font-size: 13px;");
        HBox meta = new HBox(8);
        meta.setAlignment(Pos.CENTER_LEFT);
        meta.getChildren().addAll(creerIconLabel(emp.getPoste()), creerSeparateurVertical(), creerIconLabel(emp.getE_mail()));
        info.getChildren().addAll(lblNomCard, meta);
        Label badge = creerBadgeRole(emp.getRole());
        row.getChildren().addAll(av, info, badge);
        card.getChildren().add(row);
        return card;
    }

    private void appliquerStyleCarte(VBox card, boolean selected) {
        card.setStyle(selected ? "-fx-background-color: -color-bg-default; -fx-background-radius: 10; -fx-padding: 15; -fx-border-color: -color-accent-emphasis; -fx-border-radius: 10; -fx-border-width: 1.5;" : "-fx-background-color: -color-bg-default; -fx-background-radius: 10; -fx-padding: 15; -fx-border-color: -color-border-muted; -fx-border-radius: 10; -fx-border-width: 1;");
    }

    private boolean validerFormulaire(TextField nom, TextField prenom, TextField email, TextField tel, TextField poste, ComboBox<String> roleCB, Label errLbl) {
        boolean valid = true;
        errLbl.setText(""); errLbl.setStyle("-fx-text-fill: -color-danger-fg;");
        String es = "-fx-border-color: #DC2626;-fx-border-width: 1;-fx-border-radius: 3;";
        nom.setStyle(""); prenom.setStyle(""); email.setStyle("");
        tel.setStyle(""); poste.setStyle(""); roleCB.setStyle("");
        if (nom.getText().trim().isEmpty()) { nom.setStyle(es); valid = false; }
        if (prenom.getText().trim().isEmpty()) { prenom.setStyle(es); valid = false; }
        if (poste.getText().trim().isEmpty()) { poste.setStyle(es); valid = false; }
        if (roleCB.getValue() == null) { roleCB.setStyle(es); valid = false; }
        if (email.getText().trim().isEmpty() || !email.getText().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            email.setStyle(es); valid = false; }
        try { if (tel.getText().trim().isEmpty()) throw new NumberFormatException();
            Integer.parseInt(tel.getText().trim());
        } catch (NumberFormatException e) { tel.setStyle(es); valid = false; }
        if (!valid) errLbl.setText("Veuillez corriger les champs en rouge.");
        return valid;
    }

    private StackPane creerAvatar(employe emp, double size) {
        StackPane av = new StackPane();
        av.setPrefSize(size, size); av.setMinSize(size, size); av.setMaxSize(size, size);
        Image image = null;
        double loadSize = size * 2;
        if (emp.hasCustomImage()) {
            try { File imgFile = new File(emp.getImageProfil());
                if (imgFile.exists())
                    image = new Image(imgFile.toURI().toString(), loadSize, loadSize, true, true);
            } catch (Exception ignored) {}
        }
        if (image == null) {
            try { URL resource = getClass().getResource(employe.DEFAULT_IMAGE);
                if (resource != null)
                    image = new Image(resource.toExternalForm(), loadSize, loadSize, true, true);
            } catch (Exception ignored) {}
        }
        if (image != null) {
            ImageView iv = new ImageView(image); iv.setFitWidth(size); iv.setFitHeight(size);
            iv.setSmooth(true);
            double w = image.getWidth(), h = image.getHeight(), side = Math.min(w, h);
            double x = (w - side) / 2, y = (h - side) / 2;
            iv.setViewport(new javafx.geometry.Rectangle2D(x, y, side, side));
            iv.setPreserveRatio(false);
            iv.setClip(new Circle(size/2, size/2, size/2));
            av.getChildren().add(iv); return av;
        }
        String initials = "";
        if (emp.getPrenom() != null && !emp.getPrenom().isEmpty()) initials += emp.getPrenom().charAt(0);
        if (emp.getNom() != null && !emp.getNom().isEmpty()) initials += emp.getNom().charAt(0);
        Label il = new Label(initials.toUpperCase());
        il.setStyle("-fx-text-fill: white;-fx-font-weight: bold;-fx-font-size: " + (size*0.35) + "px;");
        av.setStyle("-fx-background-color: #4A5DEF;-fx-background-radius: " + (size/2) + ";");
        av.getChildren().add(il); return av;
    }

    private Label creerIconLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: -color-fg-muted;-fx-font-size: 11px;"); return l;
    }

    private Region creerSeparateurVertical() {
        Region sep = new Region(); sep.setPrefWidth(1); sep.setPrefHeight(12);
        sep.setStyle("-fx-background-color: -color-border-muted;"); return sep;
    }

    private Label creerBadgeRole(role r) {
        Label badge = new Label(r.getLibelle());
        String base = "-fx-font-size: 10px;-fx-font-weight: bold;-fx-padding: 3 10;-fx-background-radius: 12;";
        switch (r) {
            case ADMINISTRATEUR_ENTREPRISE -> badge.setStyle(base +
                    "-fx-background-color: -color-accent-muted;-fx-text-fill: -color-accent-fg;");
            case RH -> badge.setStyle(base +
                    "-fx-background-color: -color-danger-muted;-fx-text-fill: -color-danger-fg;");
            case CHEF_PROJET -> badge.setStyle(base +
                    "-fx-background-color: -color-success-muted;-fx-text-fill: -color-success-fg;");
            default -> badge.setStyle(base +"-fx-background-color: -color-bg-subtle;-fx-text-fill: -color-fg-muted;-fx-border-color: -color-border-muted;-fx-border-radius: 12;");
        }
        return badge;
    }

    private VBox creerMessageVide() {
        VBox box = new VBox(10); box.setAlignment(Pos.CENTER); box.setPadding(new Insets(60));
        Label icon = new Label("👥"); icon.setStyle("-fx-font-size: 36px;");
        Label msg = new Label("Aucun employé trouvé");
        msg.setStyle("-fx-text-fill: -color-fg-muted;-fx-font-size: 16px;");
        Label sub = new Label("Modifiez vos filtres ou ajoutez un collaborateur");
        sub.setStyle("-fx-text-fill: -color-fg-subtle;-fx-font-size: 13px;");
        box.getChildren().addAll(icon, msg, sub); return box;
    }
    private boolean confirmer(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(titre); alert.setHeaderText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private role parseRole(String t) {
        if (t == null) return role.EMPLOYE;
        String v = t.trim().toLowerCase();
        if (v.contains("rh")) return role.RH;
        if (v.contains("chef")) return role.CHEF_PROJET;
        if (v.contains("admin")) return role.ADMINISTRATEUR_ENTREPRISE;
        return role.EMPLOYE;
    }

    private void afficherSucces(String t, String c) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(t); a.setHeaderText(null); a.setContentText(c); a.showAndWait();
    }

    private void afficherErreur(String t, String c) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(t); a.setHeaderText(null); a.setContentText(c); a.showAndWait();
    }
}