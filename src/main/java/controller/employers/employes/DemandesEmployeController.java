package controller.employers.employes;

import controller.demandes.DemandeFormHelper;
import controller.demandes.NavigationHelper;
import entities.demande.Demande;
import entities.demande.DemandeDetails;
import entities.demande.HistoriqueDemande;
import entities.employers.employe;
import entities.employers.session;
import service.api.EmailService;
import service.api.WeatherService;
import service.demande.DemandeCRUD;
import service.demande.DemandeDetailsCRUD;
import service.demande.HistoriqueDemandeCRUD;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

public class DemandesEmployeController implements Initializable {

    // ══════════════════════════════════════════════════════════════════════════
    // FXML FIELDS - DEMANDES
    // ══════════════════════════════════════════════════════════════════════════

    @FXML private TabPane mainTabPane;
    @FXML private TextField rechercheField;
    @FXML private ComboBox<String> filterStatutCombo;
    @FXML private ComboBox<String> filterPrioriteCombo;
    @FXML private VBox cardsContainer;

    @FXML private Label lblSummaryTotal;
    @FXML private Label lblSummaryNouvelle;
    @FXML private Label lblSummaryEnCours;
    @FXML private Label lblSummaryEnAttente;
    @FXML private Label lblSummaryResolue;
    @FXML private Label lblSummaryFermee;
    @FXML private Label lblSummaryAnnulee;

    @FXML private VBox placeholderBox;
    @FXML private VBox detailsContent;
    @FXML private Label detailTitreLabel;
    @FXML private Label detailStatusBadge;
    @FXML private Label detailCategorieLabel;
    @FXML private Label detailTypeLabel;
    @FXML private Label detailPrioriteLabel;
    @FXML private Label detailStatusLabel;
    @FXML private Label detailDateLabel;
    @FXML private Label detailDescriptionLabel;
    @FXML private VBox detailSpecificContainer;
    @FXML private HBox statusProgressBar;
    @FXML private VBox historiqueContainer;

    // ══════════════════════════════════════════════════════════════════════════
    // FXML FIELDS - CANCEL SECTION
    // ══════════════════════════════════════════════════════════════════════════

    @FXML private VBox cancelSection;
    @FXML private Button cancelDemandeBtn;

    // ══════════════════════════════════════════════════════════════════════════
    // FXML FIELDS - WEATHER
    // ══════════════════════════════════════════════════════════════════════════

    @FXML private VBox weatherWidgetContainer;
    @FXML private ImageView weatherIcon;
    @FXML private Label cityLabel;
    @FXML private Label temperatureLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label humidityLabel;
    @FXML private Label windLabel;
    @FXML private Label feelsLikeLabel;
    @FXML private Label recommendationLabel;
    @FXML private Label lastUpdateLabel;
    @FXML private Label alertLabel;
    @FXML private HBox alertBox;
    @FXML private VBox forecastSection;
    @FXML private HBox forecastContainer;
    @FXML private Button toggleForecastBtn;
    @FXML private VBox weatherTipsBox;
    @FXML private Label weatherTipLabel;

    // ══════════════════════════════════════════════════════════════════════════
    // INSTANCE VARIABLES
    // ══════════════════════════════════════════════════════════════════════════

    private ObservableList<Demande> allDemandes = FXCollections.observableArrayList();
    private ObservableList<Demande> filteredDemandes = FXCollections.observableArrayList();
    private DemandeCRUD demandeCRUD;
    private DemandeDetailsCRUD detailsCRUD;
    private HistoriqueDemandeCRUD historiqueCRUD;
    private DemandeFormHelper formHelper;
    private WeatherService weatherService;
    private EmailService emailService;  // Added EmailService
    private Demande selectedDemande;
    private VBox selectedCard;
    private Timer weatherRefreshTimer;
    private boolean forecastVisible = false;

    private static final String[] STATUS_ORDER = {"Nouvelle", "En cours", "En attente", "Résolue", "Fermée"};

    // ══════════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("=== DemandesEmployeController.initialize() ===");

        // Initialize services
        demandeCRUD = new DemandeCRUD();
        detailsCRUD = new DemandeDetailsCRUD();
        historiqueCRUD = new HistoriqueDemandeCRUD();
        formHelper = new DemandeFormHelper();
        emailService = new EmailService();  // Initialize EmailService

        System.out.println("═══════════════════════════════════════════════");
        System.out.println("📧 Email Service Status: " + (emailService.isEnabled() ? "✅ ENABLED" : "❌ DISABLED"));
        System.out.println("═══════════════════════════════════════════════");

        // Initialize filters first
        initializeFilters();

        // Setup filter listeners
        setupFilterListeners();

        // Load demandes
        loadDemandesAsync();

        // Initialize weather (with try-catch to prevent crash)
        try {
            weatherService = new WeatherService();
            initializeWeatherWidget();
        } catch (Exception e) {
            System.err.println("Weather service initialization failed: " + e.getMessage());
            showWeatherError();
        }

        System.out.println("Initialization complete!");
    }

    private void initializeFilters() {
        if (filterStatutCombo != null) {
            filterStatutCombo.setItems(FXCollections.observableArrayList(
                    "Tous", "Nouvelle", "En cours", "En attente", "Résolue", "Fermée", "Annulée"));
            filterStatutCombo.setValue("Tous");
        }

        if (filterPrioriteCombo != null) {
            filterPrioriteCombo.setItems(FXCollections.observableArrayList(
                    "Toutes", "HAUTE", "NORMALE", "BASSE"));
            filterPrioriteCombo.setValue("Toutes");
        }
    }

    private void setupFilterListeners() {
        if (rechercheField != null) {
            rechercheField.textProperty().addListener((o, ov, nv) -> applyFilters());
        }
        if (filterStatutCombo != null) {
            filterStatutCombo.valueProperty().addListener((o, ov, nv) -> applyFilters());
        }
        if (filterPrioriteCombo != null) {
            filterPrioriteCombo.valueProperty().addListener((o, ov, nv) -> applyFilters());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // WEATHER WIDGET
    // ══════════════════════════════════════════════════════════════════════════

    private void initializeWeatherWidget() {
        System.out.println("Initializing weather widget...");

        if (alertBox != null) {
            alertBox.setVisible(false);
            alertBox.setManaged(false);
        }

        if (forecastSection != null) {
            forecastSection.setVisible(false);
            forecastSection.setManaged(false);
        }

        refreshWeather();
        startWeatherAutoRefresh();
    }

    @FXML
    public void refreshWeather() {
        System.out.println("Refreshing weather...");

        if (temperatureLabel != null) {
            temperatureLabel.setText("...");
        }

        if (weatherService == null) {
            System.err.println("WeatherService is null!");
            showWeatherError();
            return;
        }

        try {
            weatherService.getCurrentWeatherAsync().thenAccept(weather -> {
                Platform.runLater(() -> {
                    if (weather != null && weather.isValid) {
                        updateWeatherUI(weather);
                    } else {
                        showWeatherError();
                    }
                });
            }).exceptionally(ex -> {
                System.err.println("Weather fetch error: " + ex.getMessage());
                Platform.runLater(this::showWeatherError);
                return null;
            });
        } catch (Exception e) {
            System.err.println("Weather refresh error: " + e.getMessage());
            showWeatherError();
        }

        try {
            weatherService.getWeeklyForecastAsync().thenAccept(forecast -> {
                Platform.runLater(() -> buildForecastCards(forecast));
            }).exceptionally(ex -> {
                System.err.println("Forecast fetch error: " + ex.getMessage());
                Platform.runLater(() -> buildForecastCards(null));
                return null;
            });
        } catch (Exception e) {
            System.err.println("Forecast refresh error: " + e.getMessage());
            buildForecastCards(null);
        }
    }

    @FXML
    public void toggleForecast() {
        forecastVisible = !forecastVisible;

        if (forecastSection != null) {
            forecastSection.setVisible(forecastVisible);
            forecastSection.setManaged(forecastVisible);
        }

        if (toggleForecastBtn != null) {
            if (forecastVisible) {
                toggleForecastBtn.setText("📅 Masquer prévisions ▲");
            } else {
                toggleForecastBtn.setText("📅 Voir la semaine ▼");
            }
        }
    }

    private void buildForecastCards(List<WeatherService.DailyForecast> forecast) {
        if (forecastContainer == null) return;
        forecastContainer.getChildren().clear();

        if (forecast == null || forecast.isEmpty()) {
            Label err = new Label("Prévisions non disponibles");
            err.setStyle("-fx-text-fill: rgba(255,255,255,0.5); -fx-font-size: 10;");
            forecastContainer.getChildren().add(err);
            return;
        }

        for (WeatherService.DailyForecast day : forecast) {
            VBox card = new VBox(3);
            card.setAlignment(Pos.CENTER);
            card.setPadding(new Insets(8, 5, 8, 5));
            card.setMinWidth(52);
            card.setPrefWidth(54);
            card.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 8;");

            Label dayLbl = new Label(day.dayName.length() > 3 ? day.dayName.substring(0, 3) : day.dayName);
            dayLbl.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-weight: bold; -fx-font-size: 9;");

            Label dateLbl = new Label(day.dateDisplay);
            dateLbl.setStyle("-fx-text-fill: rgba(255,255,255,0.4); -fx-font-size: 8;");

            Label emoji = new Label(day.getWeatherEmoji());
            emoji.setStyle("-fx-font-size: 18;");

            Label tempH = new Label(String.format("%.0f°", day.tempMax));
            tempH.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11;");

            Label tempL = new Label(String.format("%.0f°", day.tempMin));
            tempL.setStyle("-fx-text-fill: rgba(255,255,255,0.5); -fx-font-size: 9;");

            card.getChildren().addAll(dayLbl, dateLbl, emoji, tempH, tempL);
            forecastContainer.getChildren().add(card);
        }
    }

    private void updateWeatherUI(WeatherService.WeatherData weather) {
        if (cityLabel != null) cityLabel.setText(weather.cityName + ", " + weather.country);
        if (temperatureLabel != null) temperatureLabel.setText(String.format("%.0f°C", weather.temperature));
        if (descriptionLabel != null) descriptionLabel.setText(weather.description);
        if (humidityLabel != null) humidityLabel.setText(weather.humidity + "%");
        if (windLabel != null) windLabel.setText(String.format("%.1f m/s", weather.windSpeed));
        if (feelsLikeLabel != null) feelsLikeLabel.setText(String.format("%.0f°C", weather.feelsLike));

        if (weatherIcon != null) {
            try {
                String iconUrl = weather.getIconUrl();
                if (iconUrl != null && !iconUrl.isEmpty()) {
                    weatherIcon.setImage(new Image(iconUrl, true));
                }
            } catch (Exception e) {
                System.err.println("Error loading weather icon: " + e.getMessage());
            }
        }

        if (weatherService != null && recommendationLabel != null) {
            try {
                WeatherService.WorkRecommendation rec = weatherService.getWorkRecommendation();
                if (rec != null) {
                    recommendationLabel.setText(rec.title);
                    recommendationLabel.setStyle("-fx-text-fill: white; -fx-font-size: 10; " +
                            "-fx-background-color: " + rec.getColor() + "; " +
                            "-fx-background-radius: 8; -fx-padding: 6;");

                    updateWeatherTips(weather, rec);
                }
            } catch (Exception e) {
                System.err.println("Error getting recommendation: " + e.getMessage());
            }
        }

        if (lastUpdateLabel != null) {
            lastUpdateLabel.setText("Mis à jour: " + new SimpleDateFormat("HH:mm").format(new Date()));
        }
    }

    private void updateWeatherTips(WeatherService.WeatherData weather, WeatherService.WorkRecommendation rec) {
        if (weatherTipLabel == null) return;
        String tip = "Bonne journée de travail!";
        try {
            switch (rec.type) {
                case STAY_HOME:
                    tip = "🏠 Conditions difficiles. Restez prudent si vous devez sortir.";
                    break;
                case FLEXIBLE:
                    tip = "⏰ Pensez à adapter vos horaires de trajet si possible.";
                    break;
                case COMMUTE_OK:
                    if (weather.temperature > 25) {
                        tip = "☀️ Belle journée! N'oubliez pas de vous hydrater.";
                    } else if (weather.temperature < 10) {
                        tip = "🧥 Temps frais, pensez à bien vous couvrir.";
                    } else {
                        tip = "✨ Conditions idéales pour travailler!";
                    }
                    break;
            }
        } catch (Exception e) {
            tip = "Données météo chargées.";
        }
        weatherTipLabel.setText(tip);
    }

    private void showWeatherError() {
        if (temperatureLabel != null) temperatureLabel.setText("--°C");
        if (descriptionLabel != null) descriptionLabel.setText("Non disponible");
        if (cityLabel != null) cityLabel.setText("--");
        if (humidityLabel != null) humidityLabel.setText("--%");
        if (windLabel != null) windLabel.setText("--");
        if (feelsLikeLabel != null) feelsLikeLabel.setText("--°C");

        if (recommendationLabel != null) {
            recommendationLabel.setText("⚠️ Météo indisponible");
            recommendationLabel.setStyle("-fx-text-fill: white; -fx-font-size: 10; " +
                    "-fx-background-color: #95a5a6; -fx-background-radius: 8; -fx-padding: 6;");
        }
        if (lastUpdateLabel != null) lastUpdateLabel.setText("Échec de mise à jour");
        if (weatherTipLabel != null) weatherTipLabel.setText("Impossible de charger les données météo.");

        if (alertBox != null) {
            alertBox.setVisible(false);
            alertBox.setManaged(false);
        }
    }

    private void startWeatherAutoRefresh() {
        if (weatherRefreshTimer != null) {
            weatherRefreshTimer.cancel();
        }

        weatherRefreshTimer = new Timer(true);
        weatherRefreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    try {
                        refreshWeather();
                    } catch (Exception e) {
                        System.err.println("Auto-refresh error: " + e.getMessage());
                    }
                });
            }
        }, 15 * 60 * 1000, 15 * 60 * 1000);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DEMANDES LOADING
    // ══════════════════════════════════════════════════════════════════════════

    private void loadDemandesAsync() {
        System.out.println("Loading demandes...");

        Task<List<Demande>> loadTask = new Task<List<Demande>>() {
            @Override
            protected List<Demande> call() throws Exception {
                employe emp = session.getEmploye();
                int empId = emp != null ? emp.getId_employé() : -1;
                System.out.println("Employee ID: " + empId);

                if (empId > 0) {
                    return demandeCRUD.getByEmploye(empId);
                }
                return new ArrayList<>();
            }
        };

        loadTask.setOnSucceeded(event -> {
            List<Demande> result = loadTask.getValue();
            System.out.println("Loaded " + (result != null ? result.size() : 0) + " demandes");
            allDemandes.clear();
            if (result != null) {
                allDemandes.addAll(result);
            }
            applyFilters();
            updateSummary();
        });

        loadTask.setOnFailed(event -> {
            Throwable ex = loadTask.getException();
            System.err.println("Error loading demandes: " + (ex != null ? ex.getMessage() : "Unknown"));
            if (ex != null) ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Erreur chargement: " + (ex != null ? ex.getMessage() : "Erreur inconnue"));
        });

        Thread loadThread = new Thread(loadTask);
        loadThread.setDaemon(true);
        loadThread.start();
    }

    public void loadDemandes() {
        loadDemandesAsync();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FILTERING
    // ══════════════════════════════════════════════════════════════════════════

    private void applyFilters() {
        filteredDemandes.clear();

        String search = (rechercheField != null && rechercheField.getText() != null)
                ? rechercheField.getText().toLowerCase().trim() : "";
        String sf = (filterStatutCombo != null) ? filterStatutCombo.getValue() : "Tous";
        String pf = (filterPrioriteCombo != null) ? filterPrioriteCombo.getValue() : "Toutes";

        for (Demande d : allDemandes) {
            boolean matchSearch = search.isEmpty() ||
                    (d.getTitre() != null && d.getTitre().toLowerCase().contains(search));
            boolean matchStatus = sf == null || sf.equals("Tous") ||
                    (d.getStatus() != null && d.getStatus().equals(sf));
            boolean matchPriority = pf == null || pf.equals("Toutes") ||
                    (d.getPriorite() != null && d.getPriorite().equals(pf));

            if (matchSearch && matchStatus && matchPriority) {
                filteredDemandes.add(d);
            }
        }

        buildCards();
        resetDetails();
    }

    private void resetDetails() {
        if (placeholderBox != null) {
            placeholderBox.setVisible(true);
            placeholderBox.setManaged(true);
        }
        if (detailsContent != null) {
            detailsContent.setVisible(false);
            detailsContent.setManaged(false);
        }
        if (cancelSection != null) {
            cancelSection.setVisible(false);
            cancelSection.setManaged(false);
        }
        selectedDemande = null;
        selectedCard = null;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SUMMARY
    // ══════════════════════════════════════════════════════════════════════════

    private void updateSummary() {
        int total = allDemandes.size();
        int n = 0, ec = 0, ea = 0, r = 0, f = 0, a = 0;

        for (Demande d : allDemandes) {
            if (d.getStatus() == null) continue;
            switch (d.getStatus()) {
                case "Nouvelle": n++; break;
                case "En cours": ec++; break;
                case "En attente": ea++; break;
                case "Résolue": r++; break;
                case "Fermée": f++; break;
                case "Annulée": a++; break;
            }
        }

        if (lblSummaryTotal != null) lblSummaryTotal.setText("Total: " + total);
        if (lblSummaryNouvelle != null) lblSummaryNouvelle.setText("🔵 Nouvelle: " + n);
        if (lblSummaryEnCours != null) lblSummaryEnCours.setText("🟡 En cours: " + ec);
        if (lblSummaryEnAttente != null) lblSummaryEnAttente.setText("🟠 En attente: " + ea);
        if (lblSummaryResolue != null) lblSummaryResolue.setText("🟢 Résolue: " + r);
        if (lblSummaryFermee != null) lblSummaryFermee.setText("⚫ Fermée: " + f);
        if (lblSummaryAnnulee != null) lblSummaryAnnulee.setText("🔴 Annulée: " + a);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CARDS
    // ══════════════════════════════════════════════════════════════════════════

    private void buildCards() {
        if (cardsContainer == null) return;
        cardsContainer.getChildren().clear();

        if (filteredDemandes.isEmpty()) {
            VBox empty = new VBox(10);
            empty.setAlignment(Pos.CENTER);
            empty.setStyle("-fx-padding: 60;");

            Label icon = new Label("📭");
            icon.setStyle("-fx-font-size: 48;");

            Label title = new Label("Aucune demande");
            title.setStyle("-fx-font-size: 18; -fx-text-fill: #999; -fx-font-weight: bold;");

            Label sub = new Label("Cliquez sur '+ Nouvelle Demande' pour en créer une");
            sub.setStyle("-fx-font-size: 12; -fx-text-fill: #bbb;");
            sub.setWrapText(true);
            sub.setAlignment(Pos.CENTER);

            empty.getChildren().addAll(icon, title, sub);
            cardsContainer.getChildren().add(empty);
            return;
        }

        for (Demande d : filteredDemandes) {
            cardsContainer.getChildren().add(createCard(d));
        }
    }

    private VBox createCard(Demande d) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12, 15, 12, 15));
        card.setStyle(cardStyle(false));
        card.setCursor(Cursor.HAND);

        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(d.getTitre() != null ? d.getTitre() : "Sans titre");
        title.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        title.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(title, Priority.ALWAYS);

        Label badge = new Label(d.getStatus() != null ? d.getStatus() : "N/A");
        badge.setStyle(badgeStyle(d.getStatus()));
        badge.setMinWidth(Region.USE_PREF_SIZE);

        top.getChildren().addAll(title, badge);

        HBox mid = new HBox(15);
        mid.setAlignment(Pos.CENTER_LEFT);

        Label cat = new Label("🏷 " + (d.getCategorie() != null ? d.getCategorie() : "N/A"));
        cat.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11;");

        Label type = new Label("📂 " + (d.getTypeDemande() != null ? d.getTypeDemande() : "N/A"));
        type.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11;");

        Label prio = new Label("⚡ " + (d.getPriorite() != null ? d.getPriorite() : "N/A"));
        prio.setStyle(prioStyle(d.getPriorite()));

        mid.getChildren().addAll(cat, type, prio);

        HBox bot = new HBox(10);
        bot.setAlignment(Pos.CENTER_LEFT);

        Label dt = new Label("📅 " + (d.getDateCreation() != null ? d.getDateCreation().toString() : "N/A"));
        dt.setStyle("-fx-text-fill: #bdc3c7; -fx-font-size: 10;");

        if (canCancelDemande(d)) {
            Label cancelIndicator = new Label("🔴 Annulable");
            cancelIndicator.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 10; -fx-font-weight: bold;");
            bot.getChildren().addAll(dt, new Region(), cancelIndicator);
            HBox.setHgrow(bot.getChildren().get(1), Priority.ALWAYS);
        } else {
            bot.getChildren().add(dt);
        }

        card.getChildren().addAll(top, mid, bot);

        card.setOnMouseClicked(e -> {
            if (selectedCard != null) selectedCard.setStyle(cardStyle(false));
            selectedCard = card;
            card.setStyle(cardStyle(true));
            selectedDemande = d;
            showDetails(d);
        });

        card.setOnMouseEntered(e -> {
            if (card != selectedCard) card.setStyle(hoverStyle());
        });
        card.setOnMouseExited(e -> {
            if (card != selectedCard) card.setStyle(cardStyle(false));
        });

        return card;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // STYLES
    // ══════════════════════════════════════════════════════════════════════════

    private String cardStyle(boolean selected) {
        if (selected) {
            return "-fx-background-color: #ebf5fb; -fx-background-radius: 10; " +
                    "-fx-border-color: #3498db; -fx-border-radius: 10; -fx-border-width: 2; " +
                    "-fx-effect: dropshadow(gaussian,rgba(52,152,219,0.3),10,0,0,2);";
        }
        return "-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-border-color: #e0e0e0; -fx-border-radius: 10; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.05),5,0,0,1);";
    }

    private String hoverStyle() {
        return "-fx-background-color: #fafafa; -fx-background-radius: 10; " +
                "-fx-border-color: #bdc3c7; -fx-border-radius: 10; -fx-border-width: 1;";
    }

    private String badgeStyle(String status) {
        String base = "-fx-padding: 3 10; -fx-background-radius: 12; -fx-font-weight: bold; -fx-font-size: 10;";
        if (status == null) return base;

        switch (status) {
            case "Nouvelle": return base + " -fx-background-color: #d4edfc; -fx-text-fill: #2980b9;";
            case "En cours": return base + " -fx-background-color: #fdebd0; -fx-text-fill: #e67e22;";
            case "En attente": return base + " -fx-background-color: #fadbd8; -fx-text-fill: #c0392b;";
            case "Résolue": return base + " -fx-background-color: #d5f5e3; -fx-text-fill: #27ae60;";
            case "Fermée": return base + " -fx-background-color: #e5e8e8; -fx-text-fill: #7f8c8d;";
            case "Annulée": return base + " -fx-background-color: #f5b7b1; -fx-text-fill: #922b21;";
            default: return base;
        }
    }

    private String prioStyle(String priority) {
        if (priority == null) return "-fx-font-size: 11;";
        switch (priority) {
            case "HAUTE": return "-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 11;";
            case "NORMALE": return "-fx-text-fill: #3498db; -fx-font-weight: bold; -fx-font-size: 11;";
            case "BASSE": return "-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 11;";
            default: return "-fx-font-size: 11;";
        }
    }

    private String statusTagStyle(String status) {
        if (status == null) return "-fx-text-fill: black;";
        switch (status) {
            case "Nouvelle": return "-fx-background-color: #d4edfc; -fx-text-fill: #2980b9; -fx-font-weight: bold;";
            case "En cours": return "-fx-background-color: #fdebd0; -fx-text-fill: #e67e22; -fx-font-weight: bold;";
            case "En attente": return "-fx-background-color: #fadbd8; -fx-text-fill: #c0392b; -fx-font-weight: bold;";
            case "Résolue": return "-fx-background-color: #d5f5e3; -fx-text-fill: #27ae60; -fx-font-weight: bold;";
            case "Fermée": return "-fx-background-color: #e5e8e8; -fx-text-fill: #7f8c8d; -fx-font-weight: bold;";
            case "Annulée": return "-fx-background-color: #f5b7b1; -fx-text-fill: #922b21; -fx-font-weight: bold;";
            default: return "-fx-text-fill: black;";
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DETAILS PANEL
    // ══════════════════════════════════════════════════════════════════════════

    private void showDetails(Demande d) {
        if (placeholderBox != null) {
            placeholderBox.setVisible(false);
            placeholderBox.setManaged(false);
        }
        if (detailsContent != null) {
            detailsContent.setVisible(true);
            detailsContent.setManaged(true);
        }

        if (detailTitreLabel != null)
            detailTitreLabel.setText(d.getTitre() != null ? d.getTitre() : "Sans titre");

        if (detailStatusBadge != null) {
            detailStatusBadge.setText(d.getStatus() != null ? d.getStatus() : "N/A");
            detailStatusBadge.setStyle(badgeStyle(d.getStatus()) + " -fx-font-size: 12;");
        }

        if (detailCategorieLabel != null)
            detailCategorieLabel.setText(d.getCategorie() != null ? d.getCategorie() : "N/A");
        if (detailTypeLabel != null)
            detailTypeLabel.setText(d.getTypeDemande() != null ? d.getTypeDemande() : "N/A");
        if (detailDescriptionLabel != null)
            detailDescriptionLabel.setText(d.getDescription() != null ? d.getDescription() : "Aucune description");

        if (detailPrioriteLabel != null) {
            detailPrioriteLabel.setText(d.getPriorite() != null ? d.getPriorite() : "N/A");
            detailPrioriteLabel.setStyle(prioStyle(d.getPriorite()) + " -fx-font-size: 13;");
        }

        if (detailStatusLabel != null)
            detailStatusLabel.setText(d.getStatus() != null ? d.getStatus() : "N/A");
        if (detailDateLabel != null)
            detailDateLabel.setText(d.getDateCreation() != null ? d.getDateCreation().toString() : "N/A");

        loadSpecificDetails(d);
        buildProgress(d);
        loadHistorique(d);
        updateCancelButtonVisibility(d);
    }

    private void loadSpecificDetails(Demande d) {
        if (detailSpecificContainer == null) return;
        detailSpecificContainer.getChildren().clear();

        try {
            DemandeDetails det = detailsCRUD.getByDemande(d.getIdDemande());
            if (det != null && det.getDetails() != null && !det.getDetails().equals("{}")) {
                Map<String, String> parsed = formHelper.parseDetailsJson(det.getDetails());
                GridPane grid = new GridPane();
                grid.setHgap(10);
                grid.setVgap(5);
                int row = 0;

                for (Map.Entry<String, String> entry : parsed.entrySet()) {
                    Label key = new Label(entry.getKey() + ":");
                    key.setStyle("-fx-font-weight: bold; -fx-text-fill: #555;");
                    key.setMinWidth(130);

                    Label value = new Label(entry.getValue());
                    value.setWrapText(true);

                    grid.add(key, 0, row);
                    grid.add(value, 1, row);
                    row++;
                }
                detailSpecificContainer.getChildren().add(grid);
            } else {
                Label noDetails = new Label("Aucun détail spécifique");
                noDetails.setStyle("-fx-text-fill: #999; -fx-font-style: italic;");
                detailSpecificContainer.getChildren().add(noDetails);
            }
        } catch (SQLException e) {
            System.err.println("Error loading specific details: " + e.getMessage());
        }
    }

    private void buildProgress(Demande d) {
        if (statusProgressBar == null) return;
        statusProgressBar.getChildren().clear();

        if ("Annulée".equals(d.getStatus())) {
            VBox cancelledBox = new VBox(8);
            cancelledBox.setAlignment(Pos.CENTER);
            cancelledBox.setStyle("-fx-background-color: #fadbd8; -fx-background-radius: 10; -fx-padding: 15;");

            Label icon = new Label("🚫");
            icon.setStyle("-fx-font-size: 32;");
            Label text = new Label("Demande Annulée");
            text.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #922b21;");

            cancelledBox.getChildren().addAll(icon, text);
            statusProgressBar.getChildren().add(cancelledBox);
            return;
        }

        int currentIndex = -1;
        for (int i = 0; i < STATUS_ORDER.length; i++) {
            if (STATUS_ORDER[i].equals(d.getStatus())) {
                currentIndex = i;
                break;
            }
        }

        for (int i = 0; i < STATUS_ORDER.length; i++) {
            VBox step = new VBox(4);
            step.setAlignment(Pos.CENTER);
            step.setPrefWidth(80);

            Label circle = new Label();
            circle.setPrefSize(28, 28);
            circle.setMinSize(28, 28);
            circle.setMaxSize(28, 28);
            circle.setAlignment(Pos.CENTER);

            if (i < currentIndex) {
                circle.setText("✓");
                circle.setStyle("-fx-background-color: #27ae60; -fx-background-radius: 14; " +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12; -fx-alignment: center;");
            } else if (i == currentIndex) {
                circle.setText("●");
                circle.setStyle("-fx-background-color: #3498db; -fx-background-radius: 14; " +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14; -fx-alignment: center;");
            } else {
                circle.setText(String.valueOf(i + 1));
                circle.setStyle("-fx-background-color: #ecf0f1; -fx-background-radius: 14; " +
                        "-fx-text-fill: #bdc3c7; -fx-font-weight: bold; -fx-font-size: 11; -fx-alignment: center;");
            }

            Label name = new Label(STATUS_ORDER[i]);
            String color = i <= currentIndex ? "#2c3e50" : "#bdc3c7";
            String weight = i == currentIndex ? "bold" : "normal";
            name.setStyle("-fx-font-size: 9; -fx-text-fill: " + color + "; -fx-font-weight: " + weight + ";");

            step.getChildren().addAll(circle, name);
            statusProgressBar.getChildren().add(step);

            if (i < STATUS_ORDER.length - 1) {
                Region line = new Region();
                line.setPrefHeight(2);
                line.setMinHeight(2);
                line.setPrefWidth(30);
                HBox.setHgrow(line, Priority.ALWAYS);

                String lineColor = i < currentIndex ? "#27ae60" : "#ecf0f1";
                line.setStyle("-fx-background-color: " + lineColor + ";");

                HBox lineBox = new HBox(line);
                lineBox.setAlignment(Pos.CENTER);
                lineBox.setPadding(new Insets(0, 0, 15, 0));

                statusProgressBar.getChildren().add(lineBox);
            }
        }
    }

    private void loadHistorique(Demande d) {
        if (historiqueContainer == null) return;
        historiqueContainer.getChildren().clear();

        try {
            List<HistoriqueDemande> list = historiqueCRUD.getByDemande(d.getIdDemande());

            if (list == null || list.isEmpty()) {
                VBox noHistory = new VBox(8);
                noHistory.setAlignment(Pos.CENTER);
                noHistory.setStyle("-fx-padding: 20; -fx-background-color: white; " +
                        "-fx-background-radius: 8; -fx-border-color: #e0e0e0; -fx-border-radius: 8;");

                Label icon = new Label("⏳");
                icon.setStyle("-fx-font-size: 24;");

                String message;
                if ("Annulée".equals(d.getStatus())) {
                    message = "Cette demande a été annulée";
                } else {
                    message = "Votre demande est en attente de traitement par le service RH";
                }

                Label messageLabel = new Label(message);
                messageLabel.setStyle("-fx-text-fill: #999; -fx-font-style: italic; -fx-font-size: 12;");
                messageLabel.setWrapText(true);

                noHistory.getChildren().addAll(icon, messageLabel);
                historiqueContainer.getChildren().add(noHistory);
                return;
            }

            for (HistoriqueDemande h : list) {
                VBox card = new VBox(6);
                card.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                        "-fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-padding: 12;");

                HBox statusLine = new HBox(5);
                statusLine.setAlignment(Pos.CENTER_LEFT);

                Label ancien = new Label(h.getAncienStatut() != null ? h.getAncienStatut() : "N/A");
                ancien.setStyle(statusTagStyle(h.getAncienStatut()) + " -fx-padding: 3 8; -fx-background-radius: 10; -fx-font-size: 11;");

                Label arrow = new Label("→");
                arrow.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

                Label nouveau = new Label(h.getNouveauStatut() != null ? h.getNouveauStatut() : "N/A");
                nouveau.setStyle(statusTagStyle(h.getNouveauStatut()) + " -fx-padding: 3 8; -fx-background-radius: 10; -fx-font-size: 11;");

                statusLine.getChildren().addAll(ancien, arrow, nouveau);

                HBox meta = new HBox(15);
                meta.setAlignment(Pos.CENTER_LEFT);

                Label acteur = new Label("👤 " + (h.getActeur() != null ? h.getActeur() : "Système"));
                acteur.setStyle("-fx-text-fill: #8e44ad; -fx-font-weight: bold; -fx-font-size: 11;");

                Label date = new Label("📅 " + (h.getDateAction() != null ? h.getDateAction().toString() : "N/A"));
                date.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");

                meta.getChildren().addAll(acteur, date);

                VBox commentBox = new VBox(3);
                commentBox.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 6; -fx-padding: 8;");

                Label commentLabel = new Label("💬 Commentaire:");
                commentLabel.setStyle("-fx-text-fill: #666; -fx-font-weight: bold; -fx-font-size: 11;");

                Label commentText = new Label(h.getCommentaire() != null ? h.getCommentaire() : "Aucun commentaire");
                commentText.setWrapText(true);
                commentText.setStyle("-fx-text-fill: #333; -fx-font-size: 12;");

                commentBox.getChildren().addAll(commentLabel, commentText);
                card.getChildren().addAll(statusLine, meta, commentBox);
                historiqueContainer.getChildren().add(card);
            }
        } catch (SQLException e) {
            System.err.println("Error loading historique: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CANCEL FUNCTIONALITY WITH EMAIL
    // ══════════════════════════════════════════════════════════════════════════

    private void updateCancelButtonVisibility(Demande d) {
        boolean canCancel = canCancelDemande(d);

        System.out.println("=== updateCancelButtonVisibility ===");
        System.out.println("Demande: " + (d != null ? d.getTitre() : "null"));
        System.out.println("Status: " + (d != null ? d.getStatus() : "null"));
        System.out.println("Can Cancel: " + canCancel);

        if (cancelSection != null) {
            cancelSection.setVisible(canCancel);
            cancelSection.setManaged(canCancel);
        }

        if (cancelDemandeBtn != null) {
            cancelDemandeBtn.setVisible(canCancel);
            cancelDemandeBtn.setManaged(canCancel);
            cancelDemandeBtn.setDisable(!canCancel);
        }
    }

    private boolean canCancelDemande(Demande d) {
        if (d == null || d.getStatus() == null) return false;
        if (!d.getStatus().equals("Nouvelle")) return false;
        return !hasReceivedResponse(d);
    }

    private boolean hasReceivedResponse(Demande d) {
        try {
            List<HistoriqueDemande> historique = historiqueCRUD.getByDemande(d.getIdDemande());
            return historique != null && !historique.isEmpty();
        } catch (SQLException e) {
            System.err.println("Error checking historique: " + e.getMessage());
            return true;
        }
    }

    @FXML
    private void annulerDemande() {
        System.out.println("=== annulerDemande() called ===");

        if (selectedDemande == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner une demande.");
            return;
        }

        if (!canCancelDemande(selectedDemande)) {
            showAlert(Alert.AlertType.WARNING, "Annulation impossible",
                    "Cette demande ne peut pas être annulée car:\n\n" +
                            "• Soit elle a déjà été traitée par le service RH\n" +
                            "• Soit son statut n'est plus 'Nouvelle'");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmer l'annulation");
        confirmation.setHeaderText("⚠️ Annuler cette demande ?");
        confirmation.setContentText(
                "📋 Titre: " + selectedDemande.getTitre() + "\n" +
                        "📂 Type: " + selectedDemande.getTypeDemande() + "\n\n" +
                        "Cette action est irréversible.\n" +
                        "Une notification sera envoyée au service RH.\n\n" +
                        "Êtes-vous sûr de vouloir annuler cette demande ?");

        ButtonType btnOui = new ButtonType("Oui, annuler", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNon = new ButtonType("Non, conserver", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmation.getButtonTypes().setAll(btnOui, btnNon);

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == btnOui) {
            performCancellation();
        }
    }

    private void performCancellation() {
        System.out.println("=== performCancellation() ===");

        if (cancelDemandeBtn != null) {
            cancelDemandeBtn.setDisable(true);
            cancelDemandeBtn.setText("⏳ Annulation...");
        }

        // Store demande info before async task
        final Demande demandeToCancel = selectedDemande;
        final String demandeTitre = demandeToCancel.getTitre();
        final String demandeType = demandeToCancel.getTypeDemande();
        final String demandePriorite = demandeToCancel.getPriorite();

        Task<Boolean> cancelTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                String ancienStatut = demandeToCancel.getStatus();

                // Update demande status to "Annulée"
                demandeToCancel.setStatus("Annulée");
                demandeCRUD.modifier(demandeToCancel);
                System.out.println("✅ Demande status updated to 'Annulée'");

                // Get employee info
                employe emp = session.getEmploye();
                String empName = "Employé";
                String empEmail = "";

                if (emp != null) {
                    String nom = emp.getNom() != null ? emp.getNom() : "";
                    String prenom = emp.getPrenom() != null ? emp.getPrenom() : "";
                    empName = (prenom + " " + nom).trim();
                    if (empName.isEmpty()) empName = "Employé #" + emp.getId_employé();
                    empEmail = emp.getE_mail() != null ? emp.getE_mail() : "";
                }

                // Create historique entry
                HistoriqueDemande historique = new HistoriqueDemande();
                historique.setIdDemande(demandeToCancel.getIdDemande());
                historique.setAncienStatut(ancienStatut);
                historique.setNouveauStatut("Annulée");
                historique.setDateAction(new java.util.Date());
                historique.setActeur(empName + " (Employé)");
                historique.setCommentaire("Demande annulée par l'employé avant traitement");
                historiqueCRUD.ajouter(historique);
                System.out.println("✅ Historique entry added");

                // Send emails
                final String finalEmpName = empName;
                final String finalEmpEmail = empEmail;

                // Send email to RH
                sendCancellationEmailToRH(finalEmpName, finalEmpEmail, demandeTitre, demandeType, demandePriorite);

                // Send confirmation email to employee
                if (!finalEmpEmail.isEmpty() && finalEmpEmail.contains("@")) {
                    sendCancellationConfirmationToEmployee(finalEmpName, finalEmpEmail, demandeTitre, demandeType);
                }

                return true;
            }
        };

        cancelTask.setOnSucceeded(event -> {
            Platform.runLater(() -> {
                if (cancelDemandeBtn != null) {
                    cancelDemandeBtn.setDisable(false);
                    cancelDemandeBtn.setText("❌ Annuler cette demande");
                }

                showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "✅ Demande annulée avec succès!\n\n" +
                                "📋 \"" + demandeTitre + "\"\n\n" +
                                "📧 Le service RH a été notifié de cette annulation.");

                loadDemandesAsync();
                resetDetails();
            });
        });

        cancelTask.setOnFailed(event -> {
            Platform.runLater(() -> {
                if (cancelDemandeBtn != null) {
                    cancelDemandeBtn.setDisable(false);
                    cancelDemandeBtn.setText("❌ Annuler cette demande");
                }
                Throwable ex = cancelTask.getException();
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "❌ Erreur lors de l'annulation:\n" + (ex != null ? ex.getMessage() : "Erreur inconnue"));
            });
        });

        Thread cancelThread = new Thread(cancelTask);
        cancelThread.setDaemon(true);
        cancelThread.start();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // EMAIL METHODS FOR CANCELLATION
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Send cancellation notification email to RH
     */
    private void sendCancellationEmailToRH(String employeeName, String employeeEmail,
                                           String demandeTitre, String demandeType, String demandePriorite) {
        try {
            System.out.println("═══════════════════════════════════════════════");
            System.out.println("📧 SENDING CANCELLATION EMAIL TO RH");
            System.out.println("   Employee: " + employeeName);
            System.out.println("   Demande: " + demandeTitre);
            System.out.println("═══════════════════════════════════════════════");

            emailService.sendDemandeCancelledToRH(
                    employeeName,
                    employeeEmail,
                    demandeTitre,
                    demandeType,
                    demandePriorite
            ).thenAccept(success -> {
                Platform.runLater(() -> {
                    System.out.println(success ?
                            "✅ Cancellation email sent to RH" :
                            "❌ Failed to send cancellation email to RH");
                });
            }).exceptionally(ex -> {
                System.err.println("❌ Email to RH error: " + ex.getMessage());
                return null;
            });

        } catch (Exception e) {
            System.err.println("❌ Error sending email to RH: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send cancellation confirmation email to employee
     */
    private void sendCancellationConfirmationToEmployee(String employeeName, String employeeEmail,
                                                        String demandeTitre, String demandeType) {
        try {
            System.out.println("═══════════════════════════════════════════════");
            System.out.println("📧 SENDING CANCELLATION CONFIRMATION TO EMPLOYEE");
            System.out.println("   Employee: " + employeeName);
            System.out.println("   Email: " + employeeEmail);
            System.out.println("   Demande: " + demandeTitre);
            System.out.println("═══════════════════════════════════════════════");

            emailService.sendDemandeCancelledConfirmation(
                    employeeName,
                    employeeEmail,
                    demandeTitre,
                    demandeType
            ).thenAccept(success -> {
                Platform.runLater(() -> {
                    System.out.println(success ?
                            "✅ Confirmation email sent to employee: " + employeeEmail :
                            "❌ Failed to send confirmation email to employee");
                });
            }).exceptionally(ex -> {
                System.err.println("❌ Email to employee error: " + ex.getMessage());
                return null;
            });

        } catch (Exception e) {
            System.err.println("❌ Error sending confirmation email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NAVIGATION
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    private void ouvrirAjouter() {
        System.out.println("=== ouvrirAjouter() called ===");

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/emp/employes/ajouter-demande-employe.fxml"));
            Parent formView = loader.load();

            // Get the controller
            AjouterDemandeEmployeController controller = loader.getController();
            controller.setParentController(this);

            if (cardsContainer != null && cardsContainer.getParent() != null) {
                StackPane contentArea = findContentArea(cardsContainer);
                if (contentArea != null) {
                    contentArea.getChildren().setAll(formView);
                    System.out.println("✅ Form loaded in content area");
                } else {
                    System.err.println("❌ Could not find content area!");
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger le formulaire");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger le formulaire: " + e.getMessage());
        }
    }

    private StackPane findContentArea(javafx.scene.Node node) {
        javafx.scene.Parent parent = node.getParent();
        while (parent != null) {
            if (parent instanceof StackPane && parent.getId() != null && parent.getId().equals("contentArea")) {
                return (StackPane) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    private void openAddDemandeInNewWindow() {
        try {
            System.out.println("Opening add demande in new window...");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/emp/employes/ajouter-demande-employe.fxml"));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof AjouterDemandeEmployeController) {
                ((AjouterDemandeEmployeController) controller).setParentController(this);
            }

            Stage stage = new Stage();
            stage.setTitle("➕ Nouvelle Demande");
            stage.setScene(new Scene(root, 950, 750));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(true);
            stage.setMinWidth(800);
            stage.setMinHeight(600);

            stage.setOnHidden(event -> {
                System.out.println("Add demande window closed, refreshing list...");
                loadDemandesAsync();
            });

            stage.show();
            System.out.println("Add demande window opened successfully!");

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger le formulaire: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UTILITY
    // ══════════════════════════════════════════════════════════════════════════

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void shutdown() {
        if (weatherRefreshTimer != null) {
            weatherRefreshTimer.cancel();
            weatherRefreshTimer = null;
        }
    }
}