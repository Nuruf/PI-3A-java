package controller.employers.admin_sys;

import entities.employers.entreprise;
import entities.employers.statut;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import service.employers.entrepriseCRUD;
import utils.employers.UI;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class demandes_entreprise implements Initializable {

    @FXML private VBox demandesContainer;
    @FXML private Label lblTotal, lblEnAttente, lblAcceptees, lblRefusees;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatut;

    private entrepriseCRUD entrepriseCRUD;
    private List<entreprise> entreprises;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            entrepriseCRUD = new entrepriseCRUD();
            setupFilters();
            chargerDemandes();
        } catch (SQLException e) {
            UI.afficherErreur("Erreur de connexion", e.getMessage());
        }
    }

    private void setupFilters() {
        filterStatut.setItems(FXCollections.observableArrayList("Toutes", "En attente", "Acceptées", "Refusées"));
        filterStatut.setValue("Toutes");
        filterStatut.setOnAction(e -> appliquerFiltres());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres());
    }

    private void chargerDemandes() {
        try {
            entreprises = entrepriseCRUD.afficher();
            mettreAJourStats(entreprises);
            appliquerFiltres();
        } catch (SQLException e) {
            UI.afficherErreur("Erreur de chargement", e.getMessage());
        }
    }

    private void appliquerFiltres() {
        if (entreprises == null) return;
        String recherche = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
        String filtreStatut = filterStatut.getValue();

        List<entreprise> filtrees = entreprises.stream()
                .filter(e -> filtrerParStatut(e, filtreStatut))
                .filter(e -> filtrerParRecherche(e, recherche))
                .sorted((e1, e2) -> {
                    if (e1.getStatut() == statut.enattende && e2.getStatut() != statut.enattende) return -1;
                    if (e1.getStatut() != statut.enattende && e2.getStatut() == statut.enattende) return 1;
                    return 0;
                })
                .collect(Collectors.toList());

        afficherEntreprises(filtrees);
    }

    private boolean filtrerParStatut(entreprise e, String filtre) {
        if (filtre == null || "Toutes".equals(filtre)) return true;
        return switch (filtre) {
            case "En attente" -> e.getStatut() == statut.enattende;
            case "Acceptées" -> e.getStatut() == statut.acceptee;
            case "Refusées" -> e.getStatut() == statut.refusee;
            default -> true;
        };
    }

    private boolean filtrerParRecherche(entreprise e, String recherche) {
        if (recherche.isEmpty()) return true;
        return e.getNom_entreprise().toLowerCase().contains(recherche)
                || (e.getPrenom() + " " + e.getNom()).toLowerCase().contains(recherche);
    }

    private void afficherEntreprises(List<entreprise> entreprises) {
        demandesContainer.getChildren().clear();
        if (entreprises.isEmpty()) {
            demandesContainer.getChildren().add(
                    UI.creerMessageVide(null, "Aucune demande trouvée", "Attendez de nouvelles inscriptions"));
            return;
        }
        for (entreprise ent : entreprises) {
            demandesContainer.getChildren().add(creerCarte(ent));
        }
    }

    private VBox creerCarte(entreprise ent) {
        VBox card = new VBox();
        card.getStyleClass().add("card");
        UI.appliquerStyleCarte(card, false);

        HBox row = creerLigneEntreprise(ent, card);
        card.getChildren().add(row);
        return card;
    }

    private HBox creerLigneEntreprise(entreprise ent, VBox parentCard) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(15, 25, 15, 25));
        row.setStyle("-fx-cursor: hand;");

        row.setOnMouseEntered(e -> UI.appliquerStyleCarteHover(parentCard, true));
        row.setOnMouseExited(e -> UI.appliquerStyleCarte(parentCard, false));
        row.setOnMouseClicked(e -> toggleDetails(parentCard, ent));

        StackPane miniLogo = UI.creerLogoEntreprise(ent, 36, 10, 13);
        Label nomLabel = UI.creerLabel(ent.getNom_entreprise(), 220, true);
        Label dateLabel = UI.creerLabel(UI.formatDate(ent.getDate_demande()), 130, false);
        Label emailLabel = UI.creerLabel(ent.getE_mail(), 200, false);

        HBox statutBox = new HBox(UI.creerBadgeStatut(ent.getStatut()));
        statutBox.setPrefWidth(130);
        statutBox.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox buttonsBox = creerBoutonsAction(ent);

        row.getChildren().addAll(miniLogo, nomLabel, dateLabel, emailLabel, statutBox, spacer, buttonsBox);
        return row;
    }

    private HBox creerBoutonsAction(entreprise ent) {
        HBox buttonsBox = new HBox(10);
        buttonsBox.setAlignment(Pos.CENTER_RIGHT);
        buttonsBox.setPrefWidth(250);

        Button btnAccepter = UI.creerBouton("Accepter", "success");
        Button btnRefuser = UI.creerBouton("Refuser", "danger");

        btnAccepter.setOnMouseClicked(e -> e.consume());
        btnRefuser.setOnMouseClicked(e -> e.consume());

        btnAccepter.setOnAction(e -> confirmerAcceptation(ent));
        btnRefuser.setOnAction(e -> confirmerRefus(ent));

        if (ent.getStatut() != statut.enattende) {
            UI.desactiverBoutons(btnAccepter, btnRefuser);
        }

        buttonsBox.getChildren().addAll(btnAccepter, btnRefuser);
        return buttonsBox;
    }

    private void toggleDetails(VBox parentCard, entreprise ent) {
        if (parentCard.getChildren().size() > 1) {
            parentCard.getChildren().remove(1);
        } else {
            VBox detailsBox = creerPanneauDetails(ent);
            parentCard.getChildren().add(detailsBox);
        }
    }

    private VBox creerPanneauDetails(entreprise ent) {
        VBox box = new VBox(20);
        box.setPadding(new Insets(25, 35, 25, 35));
        box.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-muted; -fx-border-width: 1 0 0 0;");

        HBox infoGrid = creerInfoGrid(ent);
        box.getChildren().add(infoGrid);

        if (ent.getStatut() == statut.acceptee || ent.getStatut() == statut.refusee) {
            HBox footer = creerDetailsFooter(ent);
            box.getChildren().add(footer);
        }

        return box;
    }

    private HBox creerInfoGrid(entreprise ent) {
        HBox grid = new HBox(30);
        grid.setPadding(new Insets(2, 0, 2, 0));
        VBox col1 = creerInfoSection("Informations entreprise", creerInfoItem("Nom", ent.getNom_entreprise()), creerInfoItem("Ville", ent.getVille()), creerInfoItem("Pays", ent.getPays()), creerInfoItem("Matricule fiscale", ent.getMatricule_fiscale()));
        Region divider = new Region();
        divider.setPrefWidth(1);
        divider.setStyle("-fx-background-color: -color-border-muted;");
        VBox col2 = creerInfoSection("Responsable", creerInfoItem("Nom complet", ent.getPrenom() + " " + ent.getNom()), creerInfoItem("Email", ent.getE_mail()), creerInfoItem("Téléphone", UI.formatTelephone(ent.getTelephone())), creerInfoItem("Date de demande", UI.formatDate(ent.getDate_demande())));
        HBox.setHgrow(col1, Priority.ALWAYS);
        HBox.setHgrow(col2, Priority.ALWAYS);
        grid.getChildren().addAll(col1, divider, col2);
        return grid;
    }

    private VBox creerInfoSection(String titre, HBox... items) {
        VBox section = new VBox(12);
        Label header = new Label(titre);
        header.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 0 0 5 0;");

        VBox itemsBox = new VBox(10);
        itemsBox.getChildren().addAll(items);

        section.getChildren().addAll(header, itemsBox);
        return section;
    }

    private HBox creerInfoItem(String label, String value) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);

        Label keyLabel = new Label(label);
        keyLabel.setMinWidth(120);
        keyLabel.setPrefWidth(120);
        keyLabel.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 12px;");

        Label valueLabel = new Label(value != null && !value.isBlank() ? value : "—");
        valueLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        valueLabel.setWrapText(true);
        HBox.setHgrow(valueLabel, Priority.ALWAYS);

        item.getChildren().addAll(keyLabel, valueLabel);
        return item;
    }

    private HBox creerDetailsFooter(entreprise ent) {
        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(10, 0, 0, 0));
        footer.setStyle("-fx-border-color: -color-border-muted; -fx-border-width: 1 0 0 0; -fx-padding: 15 0 0 0;");

        Label timestamp = new Label("Demande soumise le " + UI.formatDate(ent.getDate_demande()));
        timestamp.setStyle("-fx-text-fill: -color-fg-subtle; -fx-font-size: 11px;");
        HBox.setHgrow(timestamp, Priority.ALWAYS);

        footer.getChildren().add(timestamp);

        switch (ent.getStatut()) {
            case refusee -> {
                Button btnDelete = UI.creerBoutonFooter("Supprimer", "danger");
                btnDelete.setOnAction(e -> confirmerSuppression(ent));
                Button btnReconsider = UI.creerBoutonFooter("Reconsidérer", "accent");
                btnReconsider.setOnAction(e -> {
                    if (UI.confirmer("Reconsidération", "Remettre en attente " + ent.getNom_entreprise() + " ?")) {
                        try {
                            entrepriseCRUD.changerStatut(ent.getId(), statut.enattende);
                            chargerDemandes();
                        } catch (SQLException ex) {
                            UI.afficherErreur("Erreur", ex.getMessage());
                        }
                    }
                });
                footer.getChildren().addAll(btnDelete, btnReconsider);
            }
            case acceptee -> {
                Label acceptedInfo = new Label("Entreprise approuvée");
                acceptedInfo.setStyle("-fx-text-fill: -color-success-fg; -fx-font-weight: bold; -fx-font-size: 12px;");
                footer.getChildren().add(acceptedInfo);
            }
        }
        return footer;
    }

    private void confirmerAcceptation(entreprise ent) {
        if (UI.confirmer("Confirmation", "Accepter " + ent.getNom_entreprise() + " ?")) {
            try {
                entrepriseCRUD.accepterEntreprise(ent);
                chargerDemandes();
            } catch (SQLException e) {
                UI.afficherErreur("Erreur", e.getMessage());
            }
        }
    }

    private void confirmerRefus(entreprise ent) {
        if (UI.confirmer("Refus", "Refuser " + ent.getNom_entreprise() + " ?")) {
            try {
                entrepriseCRUD.changerStatut(ent.getId(), statut.refusee);
                chargerDemandes();
            } catch (SQLException e) {
                UI.afficherErreur("Erreur", e.getMessage());
            }
        }
    }

    private void confirmerSuppression(entreprise ent) {
        if (UI.confirmer("Suppression", "Supprimer cette demande ? Cette action est irréversible.")) {
            try {
                entrepriseCRUD.supprimer(ent.getId());
                chargerDemandes();
            } catch (SQLException e) {
                UI.afficherErreur("Erreur", e.getMessage());
            }
        }
    }

    private void mettreAJourStats(List<entreprise> entreprises) {
        lblTotal.setText(String.valueOf(entreprises.size()));
        lblEnAttente.setText(String.valueOf(entreprises.stream().filter(e -> e.getStatut() == statut.enattende).count()));
        lblAcceptees.setText(String.valueOf(entreprises.stream().filter(e -> e.getStatut() == statut.acceptee).count()));
        lblRefusees.setText(String.valueOf(entreprises.stream().filter(e -> e.getStatut() == statut.refusee).count()));
    }
}