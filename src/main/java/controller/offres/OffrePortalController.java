package controller.offres;

import entities.offres.Offre;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.*;
import service.OffreCRUD;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OffrePortalController {

    private final OffreCRUD crudOffre = new OffreCRUD();

    @FXML private FlowPane offersGrid;
    @FXML private TextField txtSearch;
    @FXML private Label lblCount;

    private List<Offre> offresListCached = new ArrayList<>();

    @FXML ComboBox<String> comboFilterCategorie, comboFilterTypeContrat;

    @FXML
    public void initialize() {
        loadDataFromDatabase();

        comboFilterTypeContrat.setItems(FXCollections.observableArrayList("Tous", "CDI", "CDD", "CVP", "Stage"));
        comboFilterCategorie.setItems(FXCollections.observableArrayList("Tous", "Informatique", "Marketing", "Vente", "Finance", "Ressources Humaines", "Santé", "Education", "Art et Design", "Autre"));

        // Set default values
        comboFilterTypeContrat.setValue("Tous");
        comboFilterCategorie.setValue("Tous");

        // Attach listeners to all filter controls to trigger applyFilters
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        comboFilterTypeContrat.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        comboFilterCategorie.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private void loadDataFromDatabase() {
        try {
            offresListCached = crudOffre.afficher();
            renderOffersGrid(offresListCached);
        } catch (SQLException e) {
            System.err.println("Erreur chargement portail: " + e.getMessage());
        }
    }

    @FXML
    private void applyFilters() {
        String query = txtSearch.getText() == null ? "" : txtSearch.getText().toLowerCase().trim();
        String selectedType = comboFilterTypeContrat.getValue() == null ? "Tous" : comboFilterTypeContrat.getValue();
        String selectedCategorie = comboFilterCategorie.getValue() == null ? "Tous" : comboFilterCategorie.getValue();

        List<Offre> filteredList = offresListCached.stream()
                .filter(o -> {
                    String title = o.getTitrePoste().toLowerCase();
                    String type = o.getTypeContrat().getDisplayName();
                    String categorie = o.getOffreCategorie().getDisplayName();

                    boolean matchesSearch = query.isEmpty() || title.contains(query);
                    boolean matchesType = "Tous".equals(selectedType) || type.equalsIgnoreCase(selectedType);
                    boolean matchesCategorie = "Tous".equals(selectedCategorie) || categorie.equalsIgnoreCase(selectedCategorie);

                    return matchesSearch && matchesType && matchesCategorie;
                })
                .toList();

        renderOffersGrid(filteredList);
    }

    @FXML
    private void filterOffers() {
        applyFilters();
    }

    private void renderOffersGrid(List<Offre> list) {
        offersGrid.getChildren().clear();
        for (Offre o : list) {
            offersGrid.getChildren().add(createOfferCard(o));
        }
        lblCount.setText(list.size() + " Offres trouvées");
    }

    private VBox createOfferCard(Offre o) {
        VBox card = new VBox(10);
        card.setPrefWidth(320);
        card.setPadding(new Insets(20));
        card.getStyleClass().add("job-card");
        card.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-muted; -fx-background-radius: 10;");

        // Labels
        Label lblTitle = new Label(o.getTitrePoste());
        lblTitle.setStyle("-fx-text-fill: -color-accent-fg; -fx-font-weight: bold; -fx-font-size: 15;");
        lblTitle.setWrapText(true);

        Label lblInfo = new Label(o.getTypeContrat().getDisplayName());
        lblInfo.setStyle("-fx-font-weight: bold; -fx-font-size: 13;");

        Label lblDate = new Label("Expire le: " + o.getDateLimite().toString());
        lblDate.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 12;");

        card.getChildren().addAll(lblTitle, lblInfo, lblDate);

        // Hover Effects
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + "-fx-border-color: -color-accent-emphasis; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);"));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().split("-fx-border-color")[0] + "-fx-border-color: -color-border-muted;"));

        // Click Logic
        card.setOnMouseClicked(e -> openCandidatureOverlay(o));

        return card;
    }

    private void openCandidatureOverlay(Offre o) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/offres/candidature.fxml"));
            Parent candidatureOverlay = loader.load();

            // Injection des données dans le controller de destination
            CandidatureController controller = loader.getController();
            controller.setOfferId(o.getId());

            StackPane contentArea = (StackPane) offersGrid.getScene().lookup("#contentArea");

            if (contentArea != null) {
                if (!contentArea.getChildren().isEmpty()) {
                    Node portalViewNode = contentArea.getChildren().getFirst();
                    portalViewNode.setEffect(new GaussianBlur(40));
                    portalViewNode.setOpacity(0.7);
                }

                contentArea.getChildren().add(candidatureOverlay);
            }

        } catch (IOException ex) {
            System.err.println("Impossible d'ouvrir la postulation: " + ex.getMessage());
        }
    }

}