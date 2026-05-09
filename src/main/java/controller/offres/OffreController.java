package controller.offres;

import entities.offres.CategorieOffre;
import entities.offres.EtatOffre;
import entities.offres.Offre;
import entities.offres.TypeContrat;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;

import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import service.OffreCRUD;
import utils.BadgeFactory;
import utils.LayoutAnimator;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;

public class OffreController {

    //Form components
    @FXML private TextField txtTitre;
    @FXML private ComboBox<TypeContrat> comboType;
    @FXML private DatePicker dpDate;
    @FXML private ComboBox<EtatOffre> comboEtat;
    @FXML private Button btnSave, btnUpdate, btnDelete,btnDetail;
    @FXML private ComboBox<CategorieOffre> comboCategorie;

    //List container
    @FXML private VBox offersContainer;

    //Variables
    private final Set<Offre> selectedOffres = new java.util.HashSet<>();

    private String currentDescription = "";
    OffreCRUD crud = new OffreCRUD();

    //Stats components
    @FXML private Label lblTotal, lblOuvert, lblFerme;
    @FXML private FlowPane StatisticsPane;

    //Search components
    @FXML private TextField txtSearch;

    //Filter checkbox
    @FXML private ComboBox<String> filterTypeCB;
    @FXML private ComboBox<String> filterEtatCB;
    @FXML private ComboBox<String> filterCategorieCB;

    @FXML
    public void initialize() {
        //Animations
        new LayoutAnimator(offersContainer);
        new LayoutAnimator(StatisticsPane);

        //Fill form combo boxes
        comboType.setItems(FXCollections.observableArrayList(TypeContrat.values()));
        comboEtat.setItems(FXCollections.observableArrayList(EtatOffre.values()));
        comboCategorie.setItems(FXCollections.observableArrayList(CategorieOffre.values()));

        filterTypeCB.setItems(FXCollections.observableArrayList("Tous", "CDI", "CDD", "CVP", "Stage"));
        filterEtatCB.setItems(FXCollections.observableArrayList("Tous", "Ouvert", "Fermé"));
        filterCategorieCB.setItems(FXCollections.observableArrayList("Tous", "Informatique", "Marketing", "Vente", "Finance", "Ressources Humaines", "Santé", "Education", "Art et Design", "Autre"));

        filterTypeCB.setValue("Tous");
        filterEtatCB.setValue("Tous");
        filterCategorieCB.setValue("Tous");

        loadOffersList();
    }

    private void loadOffersList() {
        offersContainer.getChildren().clear();

        List<Offre> offres;

        try {
            offres = crud.afficher();
            for (Offre o : offres) {
                addOfferCard(o);
            }

            lblTotal.setText(String.valueOf(offres.size()));
            lblOuvert.setText(String.valueOf(offres.stream().filter(o -> o.getEtat() == EtatOffre.OUVERT).count()));
            lblFerme.setText(String.valueOf(offres.stream().filter(o -> o.getEtat() == EtatOffre.FERME).count()));

            offersContainer.layout();
        } catch (SQLException e) {
            System.out.println("Erreur lors du chargement des offres: " + e.getMessage());
        }
    } //LOAD CARDS AND BASIC STATS

    private void addOfferCard(Offre o) {
        HBox card = new HBox();
        card.setUserData(o);
        card.setSpacing(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("glass-card");
        card.setStyle("-fx-background-color: -color-bg-default; -fx-background-radius: 10; " +
                "-fx-padding: 15; -fx-border-color: -color-border-muted; -fx-border-radius: 10; -fx-cursor: hand;");

        CheckBox cb = new CheckBox();
        cb.setMouseTransparent(true);

        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(45, 45);
        iconBox.setStyle("-fx-background-color: -color-accent-3; -fx-background-radius: 5;");
        Label iconLabel = new Label("💼");
        iconLabel.setStyle("-fx-text-fill: white;");
        iconBox.getChildren().add(iconLabel);

        VBox details = new VBox();
        HBox.setHgrow(details, Priority.ALWAYS);
        Label titleLabel = new Label(o.getTitrePoste());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        Label subLabel = new Label(o.getTypeContrat() + " • Expire le: " + formatDate(o.getDateLimite().toString()));
        subLabel.getStyleClass().add("text-muted");
        details.getChildren().addAll(titleLabel, subLabel);

        card.getChildren().addAll(cb, iconBox, details, BadgeFactory.createBadge(o.getEtat().getDisplayName().toUpperCase()));

        card.setOnMouseClicked(e -> {
            cb.setSelected(!cb.isSelected());

            if (cb.isSelected()) {
                selectedOffres.add(o);
            } else {
                selectedOffres.remove(o);
            }

            updateButtonStates();
        });

        offersContainer.getChildren().add(card);
    } //CREATE CARDS

    private boolean formOffreValide() {
        if (txtTitre.getText() == null || txtTitre.getText().length() <= 4) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de validation");
            alert.setHeaderText(null);
            alert.setContentText("Le titre de l'offre doit contenir au moins 5 caractères.");
            alert.showAndWait();

            return false;
        }

        if (comboType.getValue() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de validation");
            alert.setHeaderText(null);
            alert.setContentText("Veuillez sélectionner un type de contrat.");
            alert.showAndWait();

            return false;
        }

        if (comboEtat.getValue() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de validation");
            alert.setHeaderText(null);
            alert.setContentText("Veuillez sélectionner un état pour l'offre.");
            alert.showAndWait();

            return false;
        }

        if (comboCategorie.getValue() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de validation");
            alert.setHeaderText(null);
            alert.setContentText("Veuillez sélectionner une catégorie pour l'offre.");
            alert.showAndWait();

            return false;
        }

        return true;
    } //BASIC VALIDATION

    private void updateButtonStates() {
        int count = selectedOffres.size();

        switch (count) {
            case 0 -> {
                btnSave.setDisable(false);
                btnUpdate.setDisable(true);
                btnDelete.setDisable(true);
                btnDelete.setText("Supprimer");
                clearForm(null);
            }
            case 1 -> {
                //enable form fields
                txtTitre.setDisable(false);
                comboType.setDisable(false);
                comboEtat.setDisable(false);
                comboCategorie.setDisable(false);
                dpDate.setDisable(false);

                btnDetail.setDisable(false);

                //enable buttons
                btnSave.setDisable(true);
                btnUpdate.setDisable(false);
                btnDelete.setDisable(false);
                btnDelete.setText("Supprimer");

                //update form on selection
                Offre single = selectedOffres.iterator().next();
                txtTitre.setText(single.getTitrePoste());
                comboType.setValue(single.getTypeContrat());
                comboEtat.setValue(single.getEtat());
                comboCategorie.setValue(single.getOffreCategorie());
                dpDate.setValue(LocalDate.parse(single.getDateLimite().toString()));
                currentDescription = single.getDescription();
            }
            default -> {
                //disable form fields
                txtTitre.setDisable(true);
                comboType.setDisable(true);
                comboEtat.setDisable(true);
                comboCategorie.setDisable(true);
                dpDate.setDisable(true);

                btnDetail.setDisable(true);

                //disable buttons
                btnSave.setDisable(true);
                btnUpdate.setDisable(true);
                btnDelete.setDisable(false);
                btnDelete.setText("Supprimer (" + count + ")");
            }
        }
    } //UPDATE BUTTONS STATE AND FORM FIELDS BASED ON SELECTION

    @FXML
    private void clearForm(ActionEvent event) {
        txtTitre.clear();
        dpDate.setValue(null);
        comboType.getSelectionModel().clearSelection();
        comboEtat.getSelectionModel().clearSelection();
        comboCategorie.getSelectionModel().clearSelection();
        currentDescription = "";

        txtTitre.setDisable(false);
        comboType.setDisable(false);
        comboEtat.setDisable(false);
        comboCategorie.setDisable(false);
        dpDate.setDisable(false);

        btnDetail.setDisable(false);

        //enable buttons
        btnSave.setDisable(false);
        btnUpdate.setDisable(true);
        btnDelete.setDisable(true);
        btnDelete.setText("Supprimer");

        selectedOffres.clear();

        offersContainer.getChildren().forEach(node -> {
            if (node instanceof HBox card) {
                card.getChildren().stream()
                        .filter(child -> child instanceof CheckBox)
                        .forEach(child -> ((CheckBox) child).setSelected(false));
            }
        });
    } //CLEAR FORM AND UNSELECT CARDS

    //CRUD operations

    @FXML
    private void addOffre(ActionEvent event) {
        if (formOffreValide()) {
            Offre o = new Offre(1,
                    txtTitre.getText(),
                    comboType.getValue(),
                    java.sql.Date.valueOf(dpDate.getValue()),
                    comboEtat.getValue(),
                    currentDescription,
                    comboCategorie.getValue());
            try {
                crud.ajouter(o);

                clearForm(null);
                loadOffersList();
            } catch (Exception e) {
                System.out.println("Erreur lors de l'ajout: " + e.getMessage());
            }
        }
    }

    @FXML
    private void updateOffre(ActionEvent event)  {
        if (formOffreValide()) {
            Offre o = selectedOffres.iterator().next();

            o.setTitrePoste(txtTitre.getText());
            o.setTypeContrat(comboType.getValue());
            o.setDateLimite(java.sql.Date.valueOf(dpDate.getValue()));
            o.setEtat(comboEtat.getValue());
            o.setDescription(currentDescription);
            o.setOffreCategorie(comboCategorie.getValue());

            try {
                crud.modifier(o);

                selectedOffres.clear();
                updateButtonStates();
                clearForm(null);
                loadOffersList();
                resetFiltersToDefault();
                applyFilters();
            } catch (SQLException e) {
                System.out.println("Erreur lors de la modification: " + e.getMessage());
            }
        }
    }

    @FXML
    private void deleteOffre(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setContentText("Supprimer " + selectedOffres.size() + " offre(s) ?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    for (Offre o : selectedOffres) {
                        crud.supprimer(o.getId());
                    }

                    selectedOffres.clear();
                    loadOffersList();
                    updateButtonStates();

                } catch (SQLException e) {
                    System.out.println("Erreur lors de la suppression: " + e.getMessage());
                }
            }
        });
    }

    //Format Date
    private String formatDate(String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        return date.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"));
    }

    //Web view editor for description
    @FXML
    private void handleOpenEditor() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/offres/description-offre.fxml"));
            Parent root = loader.load();

            DescriptionOffreController editorController = loader.getController();

            editorController.setInitialText(currentDescription);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Éditeur de texte riche");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            if (editorController.isSaveClicked()) {
                this.currentDescription = editorController.getHtmlText();
                System.out.println("Description mise à jour !");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Search
    @FXML
    public void searchOffre(KeyEvent keyEvent) {
        applyFilters();
    }

    @FXML private void applyFilters() {
        String query = txtSearch.getText() == null ? "" : txtSearch.getText().toLowerCase().trim();
        String selectedType = filterTypeCB.getValue() == null ? "Tous" : filterTypeCB.getValue();
        String selectedEtat = filterEtatCB.getValue() == null ? "Tous" : filterEtatCB.getValue();
        String selectedCategorie = filterCategorieCB.getValue() == null ? "Tous" : filterCategorieCB.getValue();

        offersContainer.getChildren().forEach(node -> {
            if (node instanceof HBox card) {
                Offre o = card.getUserData() instanceof Offre ? (Offre) card.getUserData() : null;

                String title = o != null ? o.getTitrePoste() : "";
                String type = o != null && o.getTypeContrat() != null ? o.getTypeContrat().getDisplayName() : "";
                String etat = o != null && o.getEtat() != null ? o.getEtat().getDisplayName() : "";
                String categorie = o != null && o.getOffreCategorie() != null ? o.getOffreCategorie().getDisplayName() : "";

                boolean matchesSearch = query.isEmpty() || title.toLowerCase().contains(query);
                boolean matchesType = "Tous".equals(selectedType) || type.equalsIgnoreCase(selectedType);
                boolean matchesEtat = "Tous".equals(selectedEtat) || etat.equalsIgnoreCase(selectedEtat);
                boolean matchesCategorie = "Tous".equals(selectedCategorie) || categorie.equalsIgnoreCase(selectedCategorie);

                boolean matches = matchesSearch && matchesType && matchesEtat && matchesCategorie;
                card.setVisible(matches);
                card.setManaged(matches);
            }
        });
    }

    private void resetFiltersToDefault() {
        filterTypeCB.setValue("Tous");
        filterEtatCB.setValue("Tous");
        filterCategorieCB.setValue("Tous");
    }
}
