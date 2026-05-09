package controller.offres;

import entities.employers.entreprise;
import entities.employers.statut;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import service.employers.entrepriseCRUD;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class EnterprisePortalController {

    @FXML private FlowPane enterprisesGrid;
    @FXML private Label lblCount;

    private final entrepriseCRUD entrepriseCRUD;

    public EnterprisePortalController() throws SQLException {
        this.entrepriseCRUD = new entrepriseCRUD();
    }

    @FXML
    public void initialize() {

        loadAcceptedEnterprises();
    }

    private void loadAcceptedEnterprises() {
        try {
            // Get all enterprises and filter for accepted ones
            List<entreprise> allEntreprises = entrepriseCRUD.afficher();
            List<entreprise> acceptedEnterprises = allEntreprises.stream()
                    .filter(e -> e.getStatut() == statut.acceptee)
                    .collect(Collectors.toList());

            // Display the enterprises
            displayEnterprises(acceptedEnterprises);

        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement des entreprises: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void displayEnterprises(List<entreprise> enterprises) {
        enterprisesGrid.getChildren().clear();

        if (enterprises.isEmpty()) {
            // Show empty state
            VBox emptyState = createEmptyState();
            enterprisesGrid.getChildren().add(emptyState);
        } else {
            for (entreprise e : enterprises) {
                enterprisesGrid.getChildren().add(createEnterpriseCard(e));
            }
        }

        lblCount.setText(enterprises.size() + " entreprise(s) partenaire(s)");
    }

    private VBox createEmptyState() {
        VBox emptyState = new VBox(15);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPrefWidth(1200);
        emptyState.setPrefHeight(300);
        emptyState.setStyle("-fx-text-alignment: center;");

        Label emptyLabel = new Label("Aucune entreprise acceptée pour le moment");
        emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: -color-fg-muted;");

        emptyState.getChildren().add(emptyLabel);
        return emptyState;
    }

    private VBox createEnterpriseCard(entreprise e) {
        VBox card = new VBox(10);
        card.setPrefWidth(320);
        card.setPadding(new Insets(20));
        card.getStyleClass().add("job-card");
        card.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-muted; -fx-background-radius: 10;");

        // Company name
        Label companyName = new Label(e.getNom_entreprise());
        companyName.setStyle("-fx-text-fill: -color-accent-fg; -fx-font-weight: bold; -fx-font-size: 15;");
        companyName.setWrapText(true);

        // Contact person
        Label contactPerson = new Label(e.getPrenom() + " " + e.getNom());
        contactPerson.setStyle("-fx-font-weight: bold; -fx-font-size: 13;");

        // Location
        Label locationLabel = new Label("📍 " + e.getVille() + ", " + e.getPays());
        locationLabel.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 12;");

        // Email
        Label emailLabel = new Label("✉ " + e.getE_mail());
        emailLabel.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 12;");
        emailLabel.setWrapText(true);

        // Phone
        Label phoneLabel = new Label("📞 " + e.getTelephone());
        phoneLabel.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 12;");

        // Add elements to card
        card.getChildren().addAll(companyName, contactPerson, locationLabel, emailLabel, phoneLabel);

        // Website (if available)
        if (e.getSiteWeb() != null && !e.getSiteWeb().isEmpty()) {
            Label websiteLabel = new Label("🌐 " + e.getSiteWeb());
            websiteLabel.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 12;");
            websiteLabel.setWrapText(true);
            card.getChildren().add(websiteLabel);
        }

        // Hover Effects (matching offres portal style)
        card.setOnMouseEntered(event -> card.setStyle(card.getStyle() + "-fx-border-color: -color-accent-emphasis; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);"));
        card.setOnMouseExited(event -> card.setStyle(card.getStyle().split("-fx-border-color")[0] + "-fx-border-color: -color-border-muted;"));

        return card;
    }
}
