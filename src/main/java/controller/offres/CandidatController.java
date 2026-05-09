package controller.offres;

import entities.offres.Candidat;
import entities.offres.Offre;
import entities.employers.employe;
import entities.employers.role;
import entities.employers.session;
import entities.employers.visiteur;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.sql.Date;

import javafx.stage.FileChooser;
import service.CandidatCRUD;
import service.GoogleMeetService;
import service.MatchingService;
import service.OffreCRUD;
import service.employers.employeCRUD;
import service.employers.visiteurCRUD;
import utils.BadgeFactory;

import java.io.FileOutputStream;
import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class CandidatController {

    //Side panel labels and inputs
    @FXML
    private Label lblFullname, lblEmail, lblPhone;
    @FXML
    private ComboBox<String> comboUpdatePhase;
    @FXML
    private TextArea txtNotes;
    @FXML
    private Button btnScheduleMeet;

    //Table selection
    @FXML
    private TableView<Candidat> tableCandidats;

    @FXML
    private TableColumn<Candidat, String> colIdentite;
    @FXML
    private TableColumn<Candidat, String> colEmail;
    @FXML
    private TableColumn<Candidat, String> colScore;
    @FXML
    private TableColumn<Candidat, String> colEtat;
    @FXML
    private TableColumn<Candidat, Date> colDate;

    private Candidat selectedCandidat;

    //comboFilterOffre
    @FXML
    private ComboBox<String> comboFilterOffre;

    //Stats labels
    @FXML
    private Label lblEnAttente, lblPreselectionne, lblEntretien, lblAccepte;

    //Preselection inputs
    private Offre offreActuelle;
    @FXML
    private TextField txtNbrPreselection;

    @FXML
    public void initialize() {
        comboUpdatePhase.setItems(FXCollections.observableArrayList("En attente", "Présélectionné", "Entretien", "Accepté", "Refusé"));

        // Fill table on selection change
        tableCandidats.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                this.selectedCandidat = newSelection;
                fillSidePanel(newSelection);
            }
        });

        setupTable();
        loadCandidatData();
    }

    public void loadOffresIntoCombo() {
        try {
            OffreCRUD offreCRUD = new OffreCRUD();
            List<Offre> offres = offreCRUD.afficher();
            ObservableList<String> offreNames = offres.stream()
                    .map(Offre::getTitrePoste)
                    .collect(Collectors.toCollection(FXCollections::observableArrayList)
                    );

            comboFilterOffre.setItems(offreNames);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    } // LOAD OFFER NAMES IN COMBOBOX

    private void setupTable() {
        colIdentite.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getVisiteurPrenom() + " " + cellData.getValue().getVisiteurNom()));

        colEmail.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getVisiteurEmail()));

        colScore.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("%.2f", cellData.getValue().getScore())));

        colDate.setCellValueFactory(new PropertyValueFactory<>("dateCandidature"));

        colEtat.setCellValueFactory(new PropertyValueFactory<>("etat"));
        colEtat.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(BadgeFactory.createBadge(item));
                }
            }
        });
    } // MAP DATA TO TABLE COLUMNS

    private void updateStatistics(List<Candidat> list) {
        long pending = list.stream().filter(c -> c.getEtat().equalsIgnoreCase("En attente")).count();
        long preselected = list.stream().filter(c -> c.getEtat().equalsIgnoreCase("Présélectionné")).count();
        long interview = list.stream().filter(c -> c.getEtat().equalsIgnoreCase("Entretien")).count();
        long accepted = list.stream().filter(c -> c.getEtat().equalsIgnoreCase("Accepté")).count();

        lblEnAttente.setText(String.valueOf(pending));
        lblPreselectionne.setText(String.valueOf(preselected));
        lblEntretien.setText(String.valueOf(interview));
        lblAccepte.setText(String.valueOf(accepted));
    } //SET STATS FROM DATABASE

    private void fillSidePanel(Candidat candidat) {
        lblFullname.setText(candidat.getVisiteurPrenom() + " " + candidat.getVisiteurNom());
        lblEmail.setText(candidat.getVisiteurEmail());
        lblPhone.setText("📞 " + candidat.getVisiteurTelephone());

        txtNotes.setText(candidat.getNote());
        comboUpdatePhase.setValue(candidat.getEtat());

        // Disable ComboBox if candidat is accepted, otherwise enable it
        comboUpdatePhase.setDisable("Accepté".equalsIgnoreCase(candidat.getEtat()));
    } //FILL SIDE PANEL ON TABLE ROW SELECTION

    @FXML
    private void updateCandidatStatus() {
        if (selectedCandidat == null) {
            System.out.println("Aucun candidat sélectionné");
            return;
        }
        String newEtat = comboUpdatePhase.getValue();
        String oldEtat = selectedCandidat.getEtat();

        // Show confirmation if status is being changed to "Accepté"
        if ("Accepté".equalsIgnoreCase(newEtat) && !"Accepté".equalsIgnoreCase(oldEtat)) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation");
            alert.setHeaderText("Accepter le candidat");
            alert.setContentText("Êtes-vous sûr de vouloir accepter ce candidat?\nUn employé sera créé automatiquement.\nCette action ne peut pas être annulée.");

            if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                comboUpdatePhase.setValue(oldEtat);
                return;
            }
        }

        try {
            selectedCandidat.setEtat(newEtat);
            selectedCandidat.setNote(txtNotes.getText());

            CandidatCRUD candidatCRUD = new CandidatCRUD();
            candidatCRUD.modifier(selectedCandidat);
            if ("Accepté".equalsIgnoreCase(newEtat) && !"Accepté".equalsIgnoreCase(oldEtat)) {
                creerEmployeDepuisCandidat(selectedCandidat);
            }tableCandidats.refresh();
            updateStatistics(tableCandidats.getItems());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // UPDATE CANDIDAT STATUS AND NOTES IN DB

    private void saveFile(byte[] data, String defaultName) {
        if (data == null || data.length == 0) {
            System.out.println("Aucun fichier disponible.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le document");
        fileChooser.setInitialFileName(defaultName);

        // Let user choose where to save
        File destination = fileChooser.showSaveDialog(lblFullname.getScene().getWindow());

        if (destination != null) {
            try (FileOutputStream fos = new FileOutputStream(destination)) {
                fos.write(data);
                System.out.println("Fichier enregistré : " + destination.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleDownloadCV() {
        saveFile(selectedCandidat.getCvData(),
                selectedCandidat.getCvNom());
    } // DOWNLOAD CV FILE OF SELECTED CANDIDAT

    @FXML
    private void handleDownloadLettre() {
        saveFile(selectedCandidat.getLettreMotivationData(), selectedCandidat.getLettreMotivationNom());
    } // DOWNLOAD COVER LETTER FILE OF SELECTED CANDIDAT

    @FXML
    private void loadCandidatData() {
        String selectedOffreTitle = comboFilterOffre.getSelectionModel().getSelectedItem();
        if (selectedOffreTitle == null) return;

        OffreCRUD offreCRUD = new OffreCRUD();
        try {
            // Fetch the full list and find the specific offer
            this.offreActuelle = offreCRUD.afficher().stream()
                    .filter(o -> o.getTitrePoste().equals(selectedOffreTitle))
                    .findFirst()
                    .orElse(null);

            if (this.offreActuelle == null) return;

            // Load the candidates for the offer ID
            int idOffre = this.offreActuelle.getId();
            List<Candidat> filteredList = new service.CandidatCRUD().afficher().stream()
                    .filter(c -> c.getIdOffre() == idOffre)
                    .toList();

            tableCandidats.setItems(FXCollections.observableArrayList(filteredList));
            updateStatistics(filteredList);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    } //LOAD CANDIDATS BASED ON SELECTED OFFER IN COMBOBOX

    @FXML
    private void handleAIPreslection() {
        if (offreActuelle == null || offreActuelle.getDescription() == null) {
            System.out.println("Erreur: Aucune offre sélectionnée ou description vide.");
            return;
        }

        MatchingService preselectionService = new MatchingService();

        // Clean the HTML description
        String cleanJobDesc = preselectionService.extractHtml(offreActuelle.getDescription());

        int nbrCandidats = tableCandidats.getItems().size();
        int nbrPreselection = txtNbrPreselection.getText().isEmpty() ? nbrCandidats : Integer.parseInt(txtNbrPreselection.getText());
        if (nbrPreselection > nbrCandidats) {
            nbrPreselection = nbrCandidats;
            txtNbrPreselection.setText(String.valueOf(nbrCandidats));
        }

        int preselectionCounter = 0;

        for (Candidat c : tableCandidats.getItems()) {
            if (c.getCvData() != null) {
                String cvText = preselectionService.extractTextFromPDF(c.getCvData());
                String response = preselectionService.getMatchScore(cvText, cleanJobDesc);

                System.out.println(response);

                // Score extraction
                double realScore = response.contains("score") ? 0.0 : Double.parseDouble(response.replaceAll("[^0-9.]", ""));

                double formattedScore = Math.round(realScore * 100.0) / 100.0;
                c.setScore(formattedScore);

                if (formattedScore >= 0.4 && preselectionCounter < nbrPreselection) {
                    c.setEtat("Présélectionné");
                    preselectionCounter++;
                } else if (formattedScore < 0.2) c.setEtat("Refusé");

                try {
                    new service.CandidatCRUD().modifier(c);
                    new service.CandidatCRUD().updateScore(c.getId(), formattedScore);
                    System.out.println(c.getScore() + " - " + c.getEtat());
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        tableCandidats.refresh();
        updateStatistics(tableCandidats.getItems());
    } // AUTOMATIC PRESELECTION BASED ON MATCHING SCORE

    // CREATE EMPLOYE FROM CANDIDAT IF ACCEPTED
    private void creerEmployeDepuisCandidat(Candidat candidat) {
        try {
            // Récupérer les infos du visiteur
            visiteurCRUD vCrud = new visiteurCRUD();
            visiteur v = vCrud.getById(candidat.getIdVisiteur());

            // Vérifier si l'employé existe déjà (éviter les doublons)
            employeCRUD empCrud = new employeCRUD();

            // Récupérer l'offre pour le poste
            int idOffre = candidat.getIdOffre();
            OffreCRUD offreCRUD = new OffreCRUD();
            Offre offre = offreCRUD.getById(idOffre);
            int idEntreprise = session.getIdEntreprise();
            employe.setIdEntreprise(idEntreprise);
            // 4. Créer l'employé avec les bons setters
            employe nouvelEmploye = new employe(
                    v.getNom(),                                              // nom
                    v.getPrenom(),                                           // prenom
                    v.getE_mail(),                                           // e_mail
                    v.getTelephone(),                                        // telephone
                    (offre != null ? offre.getTitrePoste() : "Non défini"),  // poste
                    role.EMPLOYE,                                            // role
                    LocalDate.now(),                                         // date_embauche
                    candidat.getCvNom(),                                     // cv_nom (du candidat)
                    candidat.getCvData()                                     // cv_data (du candidat)
            );
            nouvelEmploye.setIdCandidat(candidat.getId());
            empCrud.add(nouvelEmploye);
            System.out.println("Employé créé avec succès pour le candidat: " + candidat.getVisiteurNom());
        } catch (SQLException e) {
            System.err.println("Erreur lors de la création de l'employé: " + e.getMessage());
            e.printStackTrace();

        }
    }

    @FXML
    private void handleScheduleInterview() {
        if (selectedCandidat == null) {
            showAlert(Alert.AlertType.WARNING, "Aucun candidat", "Veuillez sélectionner un candidat.");
            return;
        }

        // Create a dialog to pick date and time
        Dialog<java.time.LocalDateTime> dialog = new Dialog<>();
        dialog.setTitle("Planifier un Entretien");
        dialog.setHeaderText("Planifier une réunion Google Meet pour " +
                           selectedCandidat.getVisiteurPrenom() + " " +
                           selectedCandidat.getVisiteurNom());

        // Add OK and Cancel buttons
        ButtonType scheduleButtonType = new ButtonType("Planifier", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(scheduleButtonType, ButtonType.CANCEL);

        // Create date and time pickers
        DatePicker datePicker = new DatePicker(LocalDate.now().plusDays(1));
        Spinner<Integer> hourSpinner = new Spinner<>(8, 18, 10);
        Spinner<Integer> minuteSpinner = new Spinner<>(0, 45, 0, 15);

        hourSpinner.setEditable(true);
        minuteSpinner.setEditable(true);
        hourSpinner.setPrefWidth(80);
        minuteSpinner.setPrefWidth(80);

        VBox content = new VBox(15);
        content.getChildren().addAll(
            new Label("Date de l'entretien:"),
            datePicker,
            new Label("Heure de l'entretien:"),
            new HBox(10,
                new Label("Heure:"), hourSpinner,
                new Label("Minutes:"), minuteSpinner
            )
        );

        dialog.getDialogPane().setContent(content);

        // Convert result to LocalDateTime
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == scheduleButtonType) {
                LocalDate date = datePicker.getValue();
                if (date == null) {
                    return null;
                }
                int hour = hourSpinner.getValue();
                int minute = minuteSpinner.getValue();
                return date.atTime(hour, minute);
            }
            return null;
        });

        // Show dialog and handle result
        dialog.showAndWait().ifPresent(interviewTime -> {
            if (interviewTime == null || interviewTime.isBefore(java.time.LocalDateTime.now())) {
                showAlert(Alert.AlertType.ERROR, "Date invalide",
                         "La date de l'entretien doit être dans le futur.");
                return;
            }

            // Show loading indicator
            btnScheduleMeet.setDisable(true);
            btnScheduleMeet.setText("⏳ Création en cours...");

            // Run API call in background thread to avoid freezing UI
            new Thread(() -> {
                try {
                    GoogleMeetService meetService = new GoogleMeetService();
                    String meetLink = meetService.createMeeting(
                        "Entretien Momentum - " + selectedCandidat.getVisiteurNom() +
                        " " + selectedCandidat.getVisiteurPrenom(),
                        selectedCandidat.getVisiteurEmail(),
                        interviewTime
                    );

                    // Update UI on JavaFX thread
                    javafx.application.Platform.runLater(() -> {
                        btnScheduleMeet.setDisable(false);
                        btnScheduleMeet.setText("📅 Planifier Meet");

                        if (meetLink != null && !meetLink.isEmpty()) {
                            // Append link to notes
                            String dateFormatted = interviewTime.format(
                                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                            );
                            txtNotes.appendText("\n\n--- Entretien Planifié ---\n" +
                                              "Date: " + dateFormatted + "\n" +
                                              "Lien Meet: " + meetLink + "\n");

                            // Update status to "Entretien" if not already
                            if (!"Entretien".equalsIgnoreCase(selectedCandidat.getEtat())) {
                                comboUpdatePhase.setValue("Entretien");
                            }

                            // Save changes
                            updateCandidatStatus();

                            // Show success message
                            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                            successAlert.setTitle("Succès");
                            successAlert.setHeaderText("Entretien planifié avec succès!");
                            successAlert.setContentText("Un e-mail a été envoyé au candidat.\n\n" +
                                                       "Lien Meet: " + meetLink);

                            ButtonType openLinkButton = new ButtonType("Ouvrir le lien", ButtonBar.ButtonData.OK_DONE);
                            ButtonType closeButton = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
                            successAlert.getButtonTypes().setAll(openLinkButton, closeButton);

                            successAlert.showAndWait().ifPresent(response -> {
                                if (response == openLinkButton) {
                                    openBrowserLink(meetLink);
                                }
                            });
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Erreur",
                                     "Impossible de créer le lien Google Meet.");
                        }
                    });

                } catch (Exception e) {
                    javafx.application.Platform.runLater(() -> {
                        btnScheduleMeet.setDisable(false);
                        btnScheduleMeet.setText("📅 Planifier Meet");

                        String errorMsg = "Erreur lors de la création de la réunion: " + e.getMessage();
                        System.err.println(errorMsg);
                        e.printStackTrace();

                        showAlert(Alert.AlertType.ERROR, "Erreur Google Calendar",
                                 errorMsg + "\n\nAssurez-vous que:\n" +
                                 "1. Le fichier credentials.json est présent\n" +
                                 "2. Vous avez autorisé l'application\n" +
                                 "3. Votre connexion internet fonctionne");
                    });
                }
            }).start();
        });
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void openBrowserLink(String url) {
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            } else {
                // Fallback for systems without Desktop support
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
            }
        } catch (Exception e) {
            System.err.println("Impossible d'ouvrir le navigateur: " + e.getMessage());
            showAlert(Alert.AlertType.WARNING, "Lien copié",
                     "Impossible d'ouvrir automatiquement. Copiez ce lien: " + url);
        }
    }
}