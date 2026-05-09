package controller.formations;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import entities.formation.formation;
import entities.formation.inscription_formation;
import entities.formation.StatutInscription;
import entities.employers.session;
import entities.employers.employe;
import service.formation.formationCRUD;
import service.formation.inscription_formationCRUD;
import service.formation.EvaluationCRUD;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class FormationsController {

    @FXML
    private TabPane tabPane;
    @FXML
    private Tab tabFormations;
    @FXML
    private Tab tabListeFormations;

    @FXML
    private FlowPane statisticsPane;
    @FXML
    private Label lblTotal;
    @FXML
    private Label lblUpcoming;
    @FXML
    private Label lblOngoing;
    @FXML
    private Label lblCompleted;

    @FXML
    private TextField txtSearch;
    @FXML
    private ComboBox<String> filterLieu;
    @FXML
    private VBox formationsContainer;

    // Onglet Liste des Formations (pour employés)
    @FXML
    private TextField txtSearchEmploye;
    @FXML
    private ComboBox<String> filterLieuEmploye;
    @FXML
    private VBox availableFormationsContainer;

    @FXML
    private TextField txtTitre;
    @FXML
    private TextField txtOrganisme;
    @FXML
    private DatePicker dpDateDebut;
    @FXML
    private DatePicker dpDateFin;
    @FXML
    private TextField txtLieu;
    @FXML
    private TextField txtCapacite;

    @FXML
    private Button btnSave;

    private final formationCRUD formationService = new formationCRUD();
    private final inscription_formationCRUD inscriptionService = new inscription_formationCRUD();
    private final EvaluationCRUD evaluationService = new EvaluationCRUD();

    // Listes complètes pour la recherche/filtrage
    private List<formation> allFormations = new java.util.ArrayList<>();
    private List<formation> allAvailableFormations = new java.util.ArrayList<>();


    @FXML
    private void initialize() {
        System.out.println("🔍 [FormationsController] Initialisation démarrée...");

        // Configurer les onglets selon le rôle
        configureTabsByRole();

        // Charger toutes les formations depuis la base de données
        try {
            System.out.println("📂 [FormationsController] Chargement des formations depuis la base...");
            allFormations = formationService.afficher();
            System.out.println("✅ [FormationsController] " + allFormations.size() + " formations chargées");

            // Debug: afficher les formations chargées
            for (formation f : allFormations) {
                System.out.println("   - ID: " + f.getId_formation() + ", Titre: " + f.getTitre() + ", Capacite: " + f.getCapacite());
            }

            allAvailableFormations = new java.util.ArrayList<>(allFormations);
        } catch (Exception e) {
            System.err.println("❌ [FormationsController] Erreur lors du chargement des formations: " + e.getMessage());
            e.printStackTrace();
        }

        // Initialiser les ComboBox avec les lieux
        loadLocationsForFilters();

        // Ajouter les écouteurs pour la recherche et le tri - Onglet RH
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((observable, oldValue, newValue) -> filterFormations());
        }
        if (filterLieu != null) {
            filterLieu.valueProperty().addListener((observable, oldValue, newValue) -> filterFormations());
        }

        // Ajouter les écouteurs pour la recherche et le tri - Onglet Employé
        if (txtSearchEmploye != null) {
            txtSearchEmploye.textProperty().addListener((observable, oldValue, newValue) -> filterAvailableFormations());
        }
        if (filterLieuEmploye != null) {
            filterLieuEmploye.valueProperty().addListener((observable, oldValue, newValue) -> filterAvailableFormations());
        }

        // Rafraîchir l'affichage avec les données chargées
        System.out.println("🎨 [FormationsController] Affichage des formations...");
        refreshFormations();
        refreshAvailableFormations();
        System.out.println("✅ [FormationsController] Initialisation terminée");
    }

    /**
     * Charger tous les lieux disponibles dans les ComboBox de filtrage
     */
    private void loadLocationsForFilters() {
        try {
            List<formation> formations = formationService.afficher();
            java.util.Set<String> lieux = new java.util.LinkedHashSet<>();

            for (formation f : formations) {
                if (f.getLieu() != null && !f.getLieu().isEmpty()) {
                    lieux.add(f.getLieu());
                }
            }

            if (filterLieu != null) {
                filterLieu.getItems().clear();
                filterLieu.getItems().addAll(lieux);
            }

            if (filterLieuEmploye != null) {
                filterLieuEmploye.getItems().clear();
                filterLieuEmploye.getItems().addAll(lieux);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement des lieux: " + e.getMessage());
        }
    }

    /**
     * Filtrer les formations RH par titre/organisme et lieu
     */
    private void filterFormations() {
        String searchText = txtSearch != null ? txtSearch.getText().toLowerCase() : "";
        String selectedLieu = filterLieu != null ? filterLieu.getValue() : null;

        List<formation> filtered = allFormations.stream()
                .filter(f -> {
                    // Filtre par texte de recherche (titre ou organisme)
                    boolean matchesSearch = f.getTitre().toLowerCase().contains(searchText) ||
                                          f.getOrganisme().toLowerCase().contains(searchText);

                    // Filtre par lieu
                    boolean matchesLieu = selectedLieu == null ||
                                        (f.getLieu() != null && f.getLieu().equals(selectedLieu));

                    return matchesSearch && matchesLieu;
                })
                .toList();

        renderFormations(filtered);
    }

    /**
     * Filtrer les formations disponibles pour les employés par titre/organisme et lieu
     */
    private void filterAvailableFormations() {
        String searchText = txtSearchEmploye != null ? txtSearchEmploye.getText().toLowerCase() : "";
        String selectedLieu = filterLieuEmploye != null ? filterLieuEmploye.getValue() : null;

        List<formation> filtered = allAvailableFormations.stream()
                .filter(f -> {
                    // Filtre par texte de recherche (titre ou organisme)
                    boolean matchesSearch = f.getTitre().toLowerCase().contains(searchText) ||
                                          f.getOrganisme().toLowerCase().contains(searchText);

                    // Filtre par lieu
                    boolean matchesLieu = selectedLieu == null ||
                                        (f.getLieu() != null && f.getLieu().equals(selectedLieu));

                    return matchesSearch && matchesLieu;
                })
                .toList();

        renderAvailableFormations(filtered);
    }

    /**
     * Configurer les onglets visibles selon le rôle de l'utilisateur
     */
    private void configureTabsByRole() {
        employe emp = session.getEmploye();

        // Vérifier le rôle de l'utilisateur
        if (emp != null) {
            entities.employers.role roleEnum = emp.getRole();
            String role = roleEnum != null ? roleEnum.getLibelle().toLowerCase().trim() : null;
            System.out.println("DEBUG: Rôle de l'utilisateur: '" + role + "'");

            if (role != null) {

                if (role.equals("employé") || role.equals("employe")) {
                    // Les employés simples ne voient que l'onglet "Liste des Formations Disponibles"
                    System.out.println("DEBUG: Employé détecté - Masquage de l'onglet Formations");

                    // Supprimer l'onglet "Formations" (Dashboard RH)
                    if (tabPane != null && tabFormations != null) {
                        tabPane.getTabs().remove(tabFormations);
                        System.out.println("DEBUG: Onglet Formations supprimé avec succès");
                    }

                    // Sélectionner automatiquement l'onglet "Liste des Formations Disponibles"
                    if (tabPane != null && tabListeFormations != null) {
                        tabPane.getSelectionModel().select(tabListeFormations);
                        System.out.println("DEBUG: Onglet Liste des Formations sélectionné");
                    }
                } else {
                    // RH, Admin Entreprise, Chef Projet voient tous les onglets
                    System.out.println("DEBUG: Rôle '" + role + "' - Affichage de tous les onglets");
                }
            }
        } else {
            System.out.println("DEBUG: Aucun employé connecté");
        }
    }

    @FXML
    private void handleNewFormation() {
        clearForm();
    }

    @FXML
    private void handleSave() {
        System.out.println("🔍 [handleSave] Vérification du formulaire...");

        if (!isFormValid()) {
            System.err.println("❌ [handleSave] Formulaire invalide!");
            showAlert(AlertType.WARNING, "Champs requis", "Veuillez remplir le titre, l'organisme et les dates.");
            return;
        }

        System.out.println("✅ [handleSave] Formulaire valide, création de la formation...");

        formation f = new formation(
                txtTitre.getText().trim(),
                txtOrganisme.getText().trim(),
                dpDateDebut.getValue(),
                dpDateFin.getValue(),
                txtLieu.getText().trim(),
                txtCapacite.getText().trim()
        );

        System.out.println("📝 [handleSave] Formation créée: " + f.getTitre());

        try {
            System.out.println("💾 [handleSave] Sauvegarde en base de données...");
            formationService.ajouter(f);
            System.out.println("✅ [handleSave] Formation sauvegardée avec succès!");

            showAlert(AlertType.INFORMATION, "Succes", "Formation ajoutee avec succes.");
            clearForm();
            refreshFormations();
            refreshAvailableFormations(); // Rafraîchir aussi la liste employé

            System.out.println("🎨 [handleSave] Affichage mis à jour");
        } catch (Exception e) {
            System.err.println("❌ [handleSave] Erreur lors de la sauvegarde: " + e.getMessage());
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Erreur", "Echec de l'ajout: " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdate() {
        // TODO: implement update logic
    }

    @FXML
    private void handleDelete() {
        // TODO: implement delete logic
    }

    private void clearForm() {
        txtTitre.clear();
        txtOrganisme.clear();
        dpDateDebut.setValue(null);
        dpDateFin.setValue(null);
        txtLieu.clear();
        txtCapacite.clear();
    }

    private boolean isFormValid() {
        return txtTitre != null && !txtTitre.getText().trim().isEmpty()
                && txtOrganisme != null && !txtOrganisme.getText().trim().isEmpty()
                && dpDateDebut != null && dpDateDebut.getValue() != null
                && dpDateFin != null && dpDateFin.getValue() != null;
    }

    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void refreshFormations() {
        System.out.println("🔄 [refreshFormations] Début du rafraîchissement...");
        try {
            allFormations = formationService.afficher(); // Mettre à jour la liste complète
            System.out.println("📂 [refreshFormations] " + allFormations.size() + " formations chargées");

            loadLocationsForFilters(); // Recharger les lieux de filtrage
            filterFormations(); // Appliquer les filtres actuels
            updateStats(allFormations);

            System.out.println("✅ [refreshFormations] Rafraîchissement terminé");
        } catch (Exception e) {
            System.err.println("❌ [refreshFormations] Erreur: " + e.getMessage());
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Erreur", "Echec du chargement: " + e.getMessage());
        }
    }

    private void renderFormations(List<formation> formations) {
        System.out.println("🎨 [renderFormations] Début du rendu avec " + formations.size() + " formations...");

        if (formationsContainer == null) {
            System.err.println("❌ [renderFormations] formationsContainer est NULL!");
            return;
        }

        formationsContainer.getChildren().clear(); // tfr8 el list ta3na
        System.out.println("📝 [renderFormations] Container nettoyé");

        for (formation f : formations) {
            System.out.println("🏗️ [renderFormations] Création de la carte pour: " + f.getTitre());

            VBox card = new VBox(12);
            card.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                    "-fx-padding: 20; -fx-border-color: #e0e0e0; -fx-border-radius: 12; -fx-border-width: 1; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");

            // Header avec titre et badge
            HBox headerRow = new HBox(10);
            headerRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            Label title = new Label(f.getTitre());
            title.setFont(Font.font(title.getFont().getFamily(), FontWeight.BOLD, 16));
            title.setStyle("-fx-text-fill: #2c3e50;");

            // Badge pour les inscriptions en attente
            try {
                int pendingCount = inscriptionService.countPendingInscriptions(f.getId_formation());
                if (pendingCount > 0) {
                    Label badge = new Label(pendingCount + " en attente");
                    badge.setStyle("-fx-background-color: linear-gradient(to right, #ff9800, #ff5722); " +
                            "-fx-text-fill: white; -fx-padding: 4 12 4 12; -fx-background-radius: 15; " +
                            "-fx-font-size: 11px; -fx-font-weight: 600;");
                    headerRow.getChildren().addAll(title, badge);
                } else {
                    headerRow.getChildren().add(title);
                }
            } catch (SQLException e) {
                headerRow.getChildren().add(title);
            }

            Label organisme = new Label("🏢 Organisme: " + safeText(f.getOrganisme()));
            organisme.setStyle("-fx-text-fill: #555; -fx-font-size: 14px;");

            String dates = formatDateRange(f.getDate_debut(), f.getDate_fin());
            Label dateRange = new Label("📅 Dates: " + dates);
            dateRange.setStyle("-fx-text-fill: #555; -fx-font-size: 14px;");

            Label lieu = new Label("📍 Lieu: " + safeText(f.getLieu()));
            lieu.setStyle("-fx-text-fill: #555; -fx-font-size: 14px;");

            Label capacite = new Label("👥 Capacité: " + safeText(f.getCapacite()) + " places");
            capacite.setStyle("-fx-text-fill: #555; -fx-font-size: 14px;");

            HBox metaRow = new HBox(15, lieu, capacite);
            metaRow.setFillHeight(true);

            // Boutons modernes avec icônes et styles améliorés
            Button btnEdit = new Button("✏️ Modifier");
            btnEdit.setStyle("-fx-background-color: linear-gradient(to right, #667eea, #764ba2); " +
                    "-fx-text-fill: white; -fx-font-weight: 600; -fx-background-radius: 8; " +
                    "-fx-padding: 8 16 8 16; -fx-cursor: hand; -fx-font-size: 12px; " +
                    "-fx-effect: dropshadow(gaussian, rgba(102,126,234,0.4), 6, 0, 0, 2);");
            btnEdit.setOnAction(event -> openEditWindow(event, f));

            Button btnDeleteCard = new Button("🗑️ Supprimer");
            btnDeleteCard.setStyle("-fx-background-color: transparent; -fx-border-color: #e74c3c; " +
                    "-fx-border-width: 1.5; -fx-border-radius: 8; -fx-text-fill: #e74c3c; " +
                    "-fx-font-weight: 600; -fx-background-radius: 8; -fx-padding: 8 16 8 16; " +
                    "-fx-cursor: hand; -fx-font-size: 12px;");
            btnDeleteCard.setOnAction(event -> handleDeleteFromCard(f));

            Button btnAccept = new Button("✓ Gérer Inscriptions");
            btnAccept.setStyle("-fx-background-color: linear-gradient(to right, #43e97b, #38f9d7); " +
                    "-fx-text-fill: white; -fx-font-weight: 600; -fx-background-radius: 8; " +
                    "-fx-padding: 8 16 8 16; -fx-cursor: hand; -fx-font-size: 12px; " +
                    "-fx-effect: dropshadow(gaussian, rgba(67,233,123,0.4), 6, 0, 0, 2);");
            btnAccept.setOnAction(event -> openAcceptInscriptionsWindow(event, f));

            Button btnEvaluations = new Button("⭐ Voir les évaluations");
            btnEvaluations.setStyle("-fx-background-color: linear-gradient(to right, #f093fb, #f5576c); " +
                    "-fx-text-fill: white; -fx-font-weight: 600; -fx-background-radius: 8; " +
                    "-fx-padding: 8 16 8 16; -fx-cursor: hand; -fx-font-size: 12px; " +
                    "-fx-effect: dropshadow(gaussian, rgba(245,87,108,0.4), 6, 0, 0, 2);");
            btnEvaluations.setOnAction(event -> openEvaluationWindow(event, f));

            HBox actionsRow = new HBox(10, btnEdit, btnDeleteCard, btnAccept, btnEvaluations);
            actionsRow.setStyle("-fx-padding: 10 0 0 0;");

            card.getChildren().addAll(headerRow, organisme, dateRange, metaRow, actionsRow);
            formationsContainer.getChildren().add(card);
        }
    }

    private void openEditWindow(javafx.event.ActionEvent event, formation f) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/formation/edit-formation.fxml"));
            Parent root = loader.load();

            EditFormationController controller = loader.getController();
            controller.setFormation(f, () -> {
                refreshFormations();
                refreshAvailableFormations(); // Rafraîchir aussi la liste employé
            });

            Stage stage = new Stage();
            stage.setTitle("Modifier une formation");
            stage.setScene(new Scene(root));
            stage.initOwner(((Node) event.getSource()).getScene().getWindow());
            stage.show();
        } catch (IOException e) {
            showAlert(AlertType.ERROR, "Erreur", "Echec d'ouverture de l'edition: " + e.getMessage());
        }
    }

    private void openInscriptionWindow(javafx.event.ActionEvent event, formation f) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/formation/inscription-formation.fxml"));
            Parent root = loader.load();

            inscription_formationController controller = loader.getController();
            controller.setFormation(f, () -> {
                refreshFormations();
                refreshAvailableFormations();
            });

            Stage stage = new Stage();
            stage.setTitle("Inscription à la formation");
            stage.setScene(new Scene(root));
            stage.initOwner(((Node) event.getSource()).getScene().getWindow());
            stage.show();
        } catch (IOException e) {
            showAlert(AlertType.ERROR, "Erreur", "Echec d'ouverture de l'inscription: " + e.getMessage());
        }
    }

    private void handleDeleteFromCard(formation f) {
        try {
            // Vérifier s'il y a des inscriptions acceptées pour cette formation
            List<inscription_formation> inscriptions = inscriptionService.getInscriptionsByFormation(f.getId_formation());
            long acceptedCount = inscriptions.stream()
                    .filter(i -> i.getStatut() == StatutInscription.ACCEPTEE)
                    .count();

            if (acceptedCount > 0) {
                showAlert(AlertType.ERROR, "Suppression impossible",
                    "Cette formation ne peut pas être supprimée car " + acceptedCount +
                    " employé(s) ont une inscription acceptée.\n\n" +
                    "Vous devez d'abord annuler toutes les inscriptions acceptées avant de supprimer la formation.");
                return;
            }

            // Si aucune inscription acceptée, demander confirmation
            Alert confirm = new Alert(AlertType.CONFIRMATION);
            confirm.setTitle("Confirmer");
            confirm.setHeaderText("Supprimer la formation ?");

            if (inscriptions.isEmpty()) {
                confirm.setContentText("Cette action est irréversible.");
            } else {
                confirm.setContentText("Cette formation a " + inscriptions.size() +
                    " inscription(s) en attente ou refusée(s).\n\n" +
                    "Ces inscriptions seront également supprimées. Continuer ?");
            }

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                formationService.supprimer(f.getId_formation());
                showAlert(AlertType.INFORMATION, "Succès", "Formation supprimée avec succès.");
                refreshFormations();
                refreshAvailableFormations(); // Rafraîchir aussi la liste employé
            }
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erreur", "Echec de la suppression: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void updateStats(List<formation> formations) {
        int total = formations.size();
        int upcoming = 0;
        int ongoing = 0;
        int completed = 0;
        LocalDate today = LocalDate.now();

        for (formation f : formations) {
            LocalDate start = f.getDate_debut();
            LocalDate end = f.getDate_fin();

            if (start != null && start.isAfter(today)) {
                upcoming++;
            } else if (start != null && end != null && !start.isAfter(today) && !end.isBefore(today)) {
                ongoing++;
            } else if (end != null && end.isBefore(today)) {
                completed++;
            }
        }

        lblTotal.setText(String.valueOf(total));
        lblUpcoming.setText(String.valueOf(upcoming));
        lblOngoing.setText(String.valueOf(ongoing));
        lblCompleted.setText(String.valueOf(completed));
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String formatDateRange(LocalDate start, LocalDate end) {
        if (start == null && end == null) {
            return "N/A";
        }
        if (start != null && end == null) {
            return start.toString() + " -";
        }
        if (start == null) {
            return "- " + end;
        }
        return start + " - " + end;
    }

    /**
     * Rafraîchir la liste des formations disponibles (pour l'onglet employé)
     */
    private void refreshAvailableFormations() {
        System.out.println("🔄 [refreshAvailableFormations] Début du rafraîchissement...");
        try {
            allAvailableFormations = formationService.afficher(); // Mettre à jour la liste complète
            System.out.println("📂 [refreshAvailableFormations] " + allAvailableFormations.size() + " formations disponibles");

            loadLocationsForFilters(); // Recharger les lieux de filtrage
            filterAvailableFormations(); // Appliquer les filtres actuels

            System.out.println("✅ [refreshAvailableFormations] Rafraîchissement terminé");
        } catch (Exception e) {
            System.err.println("❌ [refreshAvailableFormations] Erreur: " + e.getMessage());
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Erreur", "Echec du chargement: " + e.getMessage());
        }
    }

    /**
     * Afficher les formations disponibles avec bouton Inscription ou Annulation
     */
    private void renderAvailableFormations(List<formation> formations) {
        System.out.println("🎨 [renderAvailableFormations] Début du rendu avec " + formations.size() + " formations...");

        if (availableFormationsContainer == null) {
            System.err.println("❌ [renderAvailableFormations] availableFormationsContainer est NULL!");
            return;
        }

        availableFormationsContainer.getChildren().clear();
        System.out.println("📝 [renderAvailableFormations] Container nettoyé");

        // Trier les formations par nombre d'évaluations (descendant)
        try {
            formations.sort((f1, f2) -> {
                try {
                    int count1 = evaluationService.getEvaluationCount(f1.getId_formation());
                    int count2 = evaluationService.getEvaluationCount(f2.getId_formation());
                    return Integer.compare(count2, count1); // Descendant (plus d'évaluations en premier)
                } catch (SQLException e) {
                    return 0;
                }
            });
        } catch (Exception e) {
            System.err.println("Erreur lors du tri des formations: " + e.getMessage());
        }

        for (formation f : formations) {
            System.out.println("🏗️ [renderAvailableFormations] Création de la carte pour: " + f.getTitre());

            VBox card = new VBox(12);
            card.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                    "-fx-padding: 20; -fx-border-color: #e0e0e0; -fx-border-radius: 12; -fx-border-width: 1; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");

            // En-tête avec titre et badge d'évaluation
            HBox headerBox = new HBox();
            headerBox.setSpacing(10);
            headerBox.setStyle("-fx-alignment: center-left;");

            Label title = new Label(f.getTitre());
            title.setFont(Font.font(title.getFont().getFamily(), FontWeight.BOLD, 18));
            title.setStyle("-fx-text-fill: #2c3e50;");
            HBox.setHgrow(title, javafx.scene.layout.Priority.ALWAYS);

            // Badge avec le nombre d'évaluations
            try {
                int evaluationCount = evaluationService.getEvaluationCount(f.getId_formation());
                double averageRating = evaluationService.getAverageRating(f.getId_formation());

                if (evaluationCount > 0) {
                    String stars = "⭐".repeat((int) Math.round(averageRating));
                    Label ratingBadge = new Label(stars + " " + evaluationCount);
                    ratingBadge.setStyle("-fx-background-color: linear-gradient(to right, #ffc107, #ff9800); " +
                            "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15 8 15; " +
                            "-fx-background-radius: 20; -fx-font-size: 12px;");
                    headerBox.getChildren().add(ratingBadge);
                } else {
                    Label noBadge = new Label("⭐ 0");
                    noBadge.setStyle("-fx-background-color: #ccc; -fx-text-fill: white; " +
                            "-fx-font-weight: bold; -fx-padding: 8 15 8 15; -fx-background-radius: 20; -fx-font-size: 12px;");
                    headerBox.getChildren().add(noBadge);
                }
            } catch (SQLException e) {
                System.err.println("Erreur lors du chargement des évaluations: " + e.getMessage());
            }

            headerBox.getChildren().add(0, title);


            Label organisme = new Label("🏢 Organisme: " + safeText(f.getOrganisme()));
            organisme.setStyle("-fx-text-fill: #555; -fx-font-size: 14px;");

            String dates = formatDateRange(f.getDate_debut(), f.getDate_fin());
            Label dateRange = new Label("📅 Dates: " + dates);
            dateRange.setStyle("-fx-text-fill: #555; -fx-font-size: 14px;");

            Label lieu = new Label("📍 Lieu: " + safeText(f.getLieu()));
            lieu.setStyle("-fx-text-fill: #555; -fx-font-size: 14px;");

            Label capacite = new Label("👥 Capacité: " + safeText(f.getCapacite()) + " places");
            capacite.setStyle("-fx-text-fill: #555; -fx-font-size: 14px;");

            // Vérifier si l'employé est déjà inscrit
            // Récupérer l'employé connecté depuis la session
            employe currentEmploye = session.getEmploye();
            Integer currentEmployeId = (currentEmploye != null) ? currentEmploye.getId_employé() : null;

            if (currentEmployeId == null) {
                // Aucun employé connecté
                Label warningLabel = new Label("⚠️ Veuillez vous connecter pour vous inscrire");
                warningLabel.setStyle("-fx-text-fill: #ff9800; -fx-font-weight: bold; -fx-font-size: 14px; " +
                        "-fx-background-color: #fff3e0; -fx-padding: 10; -fx-background-radius: 6;");

                card.getChildren().addAll(headerBox, organisme, dateRange, lieu, capacite, warningLabel);
                availableFormationsContainer.getChildren().add(card);
                continue;
            }

            try {
                inscription_formation existingInscription = inscriptionService.getInscriptionByFormationAndEmploye(
                    f.getId_formation(), currentEmployeId
                );

                if (existingInscription != null) {
                    // Déjà inscrit - Vérifier le statut
                    StatutInscription statut = existingInscription.getStatut();

                    Label statutLabel = new Label("📊 " + getStatutText(statut));
                    statutLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " +
                        getStatutColor(statut) + "; -fx-font-size: 15px; -fx-padding: 8 12 8 12; " +
                        "-fx-background-color: " + getStatutBgColor(statut) + "; -fx-background-radius: 8;");

                    if (statut == StatutInscription.ACCEPTEE) {
                        // Inscription acceptée - Ne pas permettre l'annulation
                        Label infoLabel = new Label("✅ Vous êtes inscrit(e) à cette formation");
                        infoLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-weight: 600; -fx-font-size: 14px;");

                        // Bouton pour voir les évaluations et évaluer
                        Button btnEvaluations = new Button("⭐ Évaluer et voir les avis");
                        btnEvaluations.setStyle("-fx-background-color: linear-gradient(to right, #f093fb, #f5576c); " +
                                "-fx-text-fill: white; -fx-font-weight: 600; -fx-background-radius: 10; " +
                                "-fx-padding: 12 20 12 20; -fx-cursor: hand; -fx-font-size: 14px; " +
                                "-fx-effect: dropshadow(gaussian, rgba(245,87,108,0.4), 8, 0, 0, 3);");
                        btnEvaluations.setPrefHeight(45);
                        btnEvaluations.setMaxWidth(Double.MAX_VALUE);
                        btnEvaluations.setOnAction(event -> openEvaluationWindow(event, f));

                        card.getChildren().addAll(headerBox, organisme, dateRange, lieu, capacite, statutLabel, infoLabel, btnEvaluations);

                    } else if (statut == StatutInscription.REFUSEE) {
                        // Inscription refusée - Permettre de se réinscrire
                        Label infoLabel = new Label("❌ Votre inscription a été refusée. Vous pouvez réessayer.");
                        infoLabel.setStyle("-fx-text-fill: #f44336; -fx-font-size: 13px;");

                        Button btnReinscrire = new Button("🔄 Réessayer l'inscription");
                        btnReinscrire.setStyle("-fx-background-color: linear-gradient(to right, #667eea, #764ba2); " +
                                "-fx-text-fill: white; -fx-font-weight: 600; -fx-background-radius: 10; " +
                                "-fx-padding: 12 20 12 20; -fx-cursor: hand; -fx-font-size: 14px; " +
                                "-fx-effect: dropshadow(gaussian, rgba(102,126,234,0.4), 8, 0, 0, 3);");
                        btnReinscrire.setPrefHeight(45);
                        btnReinscrire.setMaxWidth(Double.MAX_VALUE);
                        btnReinscrire.setOnAction(event -> handleReinscrire(existingInscription, f));

                        card.getChildren().addAll(headerBox, organisme, dateRange, lieu, capacite, statutLabel, infoLabel, btnReinscrire);

                    } else {
                        // EN_ATTENTE - Permettre l'annulation et la modification de la raison
                        HBox actionButtons = new HBox(10);
                        actionButtons.setStyle("-fx-alignment: center;");

                        Button btnModifierRaison = new Button("✏️ Modifier ma raison");
                        btnModifierRaison.setStyle("-fx-background-color: linear-gradient(to right, #667eea, #764ba2); " +
                                "-fx-text-fill: white; -fx-font-weight: 600; -fx-background-radius: 10; " +
                                "-fx-padding: 12 20 12 20; -fx-cursor: hand; -fx-font-size: 14px; " +
                                "-fx-effect: dropshadow(gaussian, rgba(102,126,234,0.4), 8, 0, 0, 3);");
                        btnModifierRaison.setPrefHeight(45);
                        btnModifierRaison.setMaxWidth(Double.MAX_VALUE);
                        btnModifierRaison.setOnAction(event -> openModifierRaisonWindow(event, existingInscription, f));
                        HBox.setHgrow(btnModifierRaison, javafx.scene.layout.Priority.ALWAYS);

                        Button btnAnnuler = new Button("🗑️ Annuler");
                        btnAnnuler.setStyle("-fx-background-color: linear-gradient(to right, #e74c3c, #c0392b); " +
                                "-fx-text-fill: white; -fx-font-weight: 600; -fx-background-radius: 10; " +
                                "-fx-padding: 12 20 12 20; -fx-cursor: hand; -fx-font-size: 14px; " +
                                "-fx-effect: dropshadow(gaussian, rgba(231,76,60,0.4), 8, 0, 0, 3);");
                        btnAnnuler.setPrefHeight(45);
                        btnAnnuler.setPrefWidth(120);
                        btnAnnuler.setOnAction(event -> handleAnnulerInscription(existingInscription));

                        actionButtons.getChildren().addAll(btnModifierRaison, btnAnnuler);
                        card.getChildren().addAll(headerBox, organisme, dateRange, lieu, capacite, statutLabel, actionButtons);
                    }
                } else {
                    // Pas encore inscrit - Afficher bouton S'inscrire
                    Button btnInscription = new Button("✨ S'inscrire à cette formation");
                    btnInscription.setStyle("-fx-background-color: linear-gradient(to right, #43e97b, #38f9d7); " +
                            "-fx-text-fill: white; -fx-font-weight: 600; -fx-background-radius: 10; " +
                            "-fx-padding: 12 20 12 20; -fx-cursor: hand; -fx-font-size: 14px; " +
                            "-fx-effect: dropshadow(gaussian, rgba(67,233,123,0.4), 8, 0, 0, 3);");
                    btnInscription.setPrefHeight(45);
                    btnInscription.setMaxWidth(Double.MAX_VALUE);
                    btnInscription.setOnAction(event -> openInscriptionWindow(event, f));

                    // Bouton pour voir les évaluations (lecture seule, pas d'ajout)
                    Button btnEvaluations = new Button("⭐ Voir les évaluations");
                    btnEvaluations.setStyle("-fx-background-color: #9e9e9e; " +
                            "-fx-text-fill: white; -fx-font-weight: 600; -fx-background-radius: 10; " +
                            "-fx-padding: 12 20 12 20; -fx-cursor: not-allowed; -fx-font-size: 14px; " +
                            "-fx-effect: dropshadow(gaussian, rgba(158,158,158,0.4), 8, 0, 0, 3);");
                    btnEvaluations.setPrefHeight(45);
                    btnEvaluations.setMaxWidth(Double.MAX_VALUE);
                    btnEvaluations.setDisable(true);
                    btnEvaluations.setTooltip(new Tooltip("Inscrivez-vous et attendez l'acceptation pour évaluer"));

                    HBox buttonsBox = new HBox(10, btnInscription, btnEvaluations);
                    buttonsBox.setPrefHeight(45);

                    card.getChildren().addAll(headerBox, organisme, dateRange, lieu, capacite, buttonsBox);
                }
            } catch (SQLException e) {
                // En cas d'erreur, afficher le bouton S'inscrire par défaut
                Button btnInscription = new Button("✓ S'inscrire à cette formation");
                btnInscription.getStyleClass().add("accent");
                btnInscription.setPrefHeight(40);
                btnInscription.setMaxWidth(Double.MAX_VALUE);
                btnInscription.setOnAction(event -> openInscriptionWindow(event, f));

                card.getChildren().addAll(headerBox, organisme, dateRange, lieu, capacite, btnInscription);
            }

            availableFormationsContainer.getChildren().add(card);
        }
    }

    /**
     * Réinscrire après un refus
     */
    private void handleReinscrire(inscription_formation oldInscription, formation f) {
        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la réinscription");
        confirm.setHeaderText("Nouvelle inscription");
        confirm.setContentText("Votre précédente inscription a été refusée. Voulez-vous soumettre une nouvelle demande ?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Mettre à jour le statut à EN_ATTENTE
                oldInscription.setStatut(StatutInscription.EN_ATTENTE);
                inscriptionService.modifier(oldInscription);

                showAlert(AlertType.INFORMATION, "Succès", "Votre nouvelle demande d'inscription a été soumise !");
                refreshAvailableFormations();
                refreshFormations();
            } catch (SQLException e) {
                showAlert(AlertType.ERROR, "Erreur", "Échec de la réinscription: " + e.getMessage());
            }
        }
    }

    /**
     * Obtenir le texte du statut en français
     */
    private String getStatutText(StatutInscription statut) {
        switch (statut) {
            case EN_ATTENTE: return "En attente de validation";
            case ACCEPTEE: return "Inscription acceptée ✓";
            case REFUSEE: return "Inscription refusée";
            default: return statut.name();
        }
    }

    /**
     * Obtenir la couleur du statut
     */
    private String getStatutColor(StatutInscription statut) {
        switch (statut) {
            case EN_ATTENTE: return "#ff9800";
            case ACCEPTEE: return "#4caf50";
            case REFUSEE: return "#f44336";
            default: return "#666666";
        }
    }

    /**
     * Obtenir la couleur de fond du badge de statut
     */
    private String getStatutBgColor(StatutInscription statut) {
        switch (statut) {
            case EN_ATTENTE: return "#fff3e0";
            case ACCEPTEE: return "#e8f5e9";
            case REFUSEE: return "#ffebee";
            default: return "#f5f5f5";
        }
    }

    /**
     * Annuler l'inscription d'un employé
     */
    private void handleAnnulerInscription(inscription_formation inscription) {
        // Vérifier que l'inscription n'est pas déjà acceptée
        if (inscription.getStatut() == StatutInscription.ACCEPTEE) {
            showAlert(AlertType.ERROR, "Annulation impossible",
                "Vous ne pouvez pas annuler une inscription qui a été acceptée par le RH.");
            return;
        }

        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer l'annulation");
        confirm.setHeaderText("Annuler votre inscription ?");
        confirm.setContentText("Êtes-vous sûr de vouloir annuler votre inscription à cette formation ?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                inscriptionService.supprimer(inscription.getId_inscription());
                showAlert(AlertType.INFORMATION, "Succès", "Votre inscription a été annulée.");
                refreshAvailableFormations();
                refreshFormations(); // Rafraîchir aussi la vue RH
            } catch (SQLException e) {
                showAlert(AlertType.ERROR, "Erreur", "Échec de l'annulation: " + e.getMessage());
            }
        }
    }

    /**
     * Ouvrir la fenêtre pour gérer les inscriptions (RH)
     */
    private void openAcceptInscriptionsWindow(javafx.event.ActionEvent event, formation f) {
        try {
            List<inscription_formation> inscriptions = inscriptionService.getInscriptionsByFormation(f.getId_formation());

            if (inscriptions.isEmpty()) {
                showAlert(AlertType.INFORMATION, "Information", "Aucune inscription pour cette formation.");
                return;
            }

            // Ouvrir la nouvelle fenêtre de gestion
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/formation/gestion-inscriptions.fxml"));
            Parent root = loader.load();

            GestionInscriptionsController controller = loader.getController();
            controller.setFormation(f, () -> {
                refreshFormations();
                refreshAvailableFormations();
            });

            Stage stage = new Stage();
            stage.setTitle("Gestion des Inscriptions");
            stage.setScene(new Scene(root));
            stage.initOwner(((Node) event.getSource()).getScene().getWindow());
            stage.show();

        } catch (IOException | SQLException e) {
            showAlert(AlertType.ERROR, "Erreur", "Échec d'ouverture: " + e.getMessage());
        }
    }


    /**
     * Ouvrir la fenêtre pour voir et ajouter des évaluations
     */
    private void openEvaluationWindow(javafx.event.ActionEvent event, formation f) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/formation/evaluation-formation.fxml"));
            Parent root = loader.load();

            EvaluationController controller = loader.getController();
            controller.setFormationData(f.getId_formation(), f.getTitre(), () -> {
                refreshFormations();
                refreshAvailableFormations();
            });

            Stage stage = new Stage();
            stage.setTitle("Évaluations - " + f.getTitre());
            stage.setScene(new Scene(root, 700, 800));
            stage.initOwner(((Node) event.getSource()).getScene().getWindow());
            stage.show();

        } catch (IOException e) {
            showAlert(AlertType.ERROR, "Erreur", "Échec d'ouverture: " + e.getMessage());
        }
    }

    /**
     * Ouvrir la fenêtre pour modifier la raison de l'inscription
     */
    private void openModifierRaisonWindow(javafx.event.ActionEvent event, inscription_formation inscription, formation f) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/formation/modifier-raison.fxml"));
            Parent root = loader.load();

            ModifierRaisonController controller = loader.getController();
            controller.setInscription(inscription, f.getTitre(), () -> {
                refreshFormations();
                refreshAvailableFormations();
            });

            Stage stage = new Stage();
            stage.setTitle("Modifier la Raison");
            stage.setScene(new Scene(root));
            stage.initOwner(((Node) event.getSource()).getScene().getWindow());
            stage.show();

        } catch (IOException e) {
            showAlert(AlertType.ERROR, "Erreur", "Échec d'ouverture: " + e.getMessage());
        }
    }
}
