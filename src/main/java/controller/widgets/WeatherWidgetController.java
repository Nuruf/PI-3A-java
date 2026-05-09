package controller.widgets;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import service.api.WeatherService;
import service.api.WeatherService.*;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

public class WeatherWidgetController implements Initializable {

    @FXML private VBox weatherContainer;
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
    @FXML private HBox forecastContainer;

    private WeatherService weatherService;
    private Timer autoRefreshTimer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        weatherService = new WeatherService();
        if (alertBox != null) { alertBox.setVisible(false); alertBox.setManaged(false); }
        refreshWeather();
        startAutoRefresh();
    }

    @FXML
    public void refreshWeather() {
        if (temperatureLabel != null) temperatureLabel.setText("...");

        weatherService.getCurrentWeatherAsync().thenAccept(weather ->
                Platform.runLater(() -> updateCurrentWeatherUI(weather))
        );

        weatherService.getWeeklyForecastAsync().thenAccept(forecast ->
                Platform.runLater(() -> updateForecastUI(forecast))
        );
    }

    // ── current weather ──────────────────────────────────────────

    private void updateCurrentWeatherUI(WeatherData weather) {
        if (weather == null || !weather.isValid) { showError(); return; }

        if (cityLabel != null) cityLabel.setText(weather.cityName + ", " + weather.country);
        if (temperatureLabel != null) temperatureLabel.setText(String.format("%.0f°C", weather.temperature));
        if (descriptionLabel != null) descriptionLabel.setText(weather.description);
        if (humidityLabel != null) humidityLabel.setText(weather.humidity + "%");
        if (windLabel != null) windLabel.setText(String.format("%.1f m/s", weather.windSpeed));
        if (feelsLikeLabel != null) feelsLikeLabel.setText(String.format("%.0f°C", weather.feelsLike));

        if (weatherIcon != null) {
            try { weatherIcon.setImage(new Image(weather.getIconUrl(), true)); }
            catch (Exception ignored) {}
        }

        WorkRecommendation rec = weatherService.getWorkRecommendation();
        if (recommendationLabel != null) {
            recommendationLabel.setText(rec.title + "\n" + rec.description);
            recommendationLabel.setStyle("-fx-text-fill: white; -fx-font-size: 11;"
                    + " -fx-background-color: " + rec.getColor() + ";"
                    + " -fx-background-radius: 8; -fx-padding: 10;");
        }

        String alert = weatherService.getWeatherAlert();
        if (alertBox != null && alertLabel != null) {
            if (alert != null) {
                alertLabel.setText(alert);
                alertBox.setVisible(true); alertBox.setManaged(true);
            } else {
                alertBox.setVisible(false); alertBox.setManaged(false);
            }
        }

        if (lastUpdateLabel != null) {
            lastUpdateLabel.setText("Mis à jour: " + new SimpleDateFormat("HH:mm").format(new Date()));
        }
    }

    // ── weekly forecast cards ────────────────────────────────────

    private void updateForecastUI(List<DailyForecast> forecast) {
        if (forecastContainer == null) return;
        forecastContainer.getChildren().clear();

        if (forecast == null || forecast.isEmpty()) {
            Label err = new Label("Prévisions non disponibles");
            err.setStyle("-fx-text-fill: rgba(255,255,255,0.6);");
            forecastContainer.getChildren().add(err);
            return;
        }

        for (DailyForecast day : forecast) {
            VBox card = new VBox(4);
            card.setAlignment(Pos.CENTER);
            card.setPadding(new Insets(10, 8, 10, 8));
            card.setMinWidth(80);
            card.setPrefWidth(90);
            card.setStyle("-fx-background-color: rgba(255,255,255,0.15);"
                    + " -fx-background-radius: 10;");

            // Day name
            Label dayName = new Label(day.dayName.substring(0, 3) + ".");
            dayName.setStyle("-fx-text-fill: rgba(255,255,255,0.85); -fx-font-weight: bold; -fx-font-size: 11;");

            // Date
            Label dateLabel = new Label(day.dateDisplay);
            dateLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 9;");

            // Weather emoji
            Label emoji = new Label(day.getWeatherEmoji());
            emoji.setStyle("-fx-font-size: 22;");

            // Try to load icon image
            ImageView icon = new ImageView();
            icon.setFitWidth(35);
            icon.setFitHeight(35);
            icon.setPreserveRatio(true);
            try {
                icon.setImage(new Image(day.getIconUrl(), 35, 35, true, true, true));
            } catch (Exception e) {
                icon = null;
            }

            // Temp range
            Label temps = new Label(String.format("%.0f°/%.0f°", day.tempMax, day.tempMin));
            temps.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12;");

            // Description
            Label desc = new Label(day.description.length() > 12
                    ? day.description.substring(0, 10) + "…" : day.description);
            desc.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 9;");

            // Humidity
            Label hum = new Label("💧 " + day.humidity + "%");
            hum.setStyle("-fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 9;");

            card.getChildren().addAll(dayName, dateLabel);
            if (icon != null) card.getChildren().add(icon);
            else card.getChildren().add(emoji);
            card.getChildren().addAll(temps, desc, hum);

            // Hover effect
            card.setOnMouseEntered(e -> card.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.25); -fx-background-radius: 10;"));
            card.setOnMouseExited(e -> card.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.15); -fx-background-radius: 10;"));

            // Tooltip with full info
            card.setOnMouseClicked(e -> {
                if (descriptionLabel != null) {
                    descriptionLabel.setText(day.dayName + ": " + day.description
                            + " | 💨 " + day.windSpeed + " m/s");
                }
            });

            forecastContainer.getChildren().add(card);
        }
    }

    // ── error / refresh ──────────────────────────────────────────

    private void showError() {
        if (temperatureLabel != null)      temperatureLabel.setText("--°C");
        if (descriptionLabel != null)      descriptionLabel.setText("Données non disponibles");
        if (recommendationLabel != null)   recommendationLabel.setText("⚠️ Météo indisponible");
        if (lastUpdateLabel != null)       lastUpdateLabel.setText("Échec de mise à jour");
    }

    private void startAutoRefresh() {
        autoRefreshTimer = new Timer(true);
        autoRefreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() { Platform.runLater(() -> refreshWeather()); }
        }, 15 * 60 * 1000, 15 * 60 * 1000);
    }

    public void shutdown() {
        if (autoRefreshTimer != null) { autoRefreshTimer.cancel(); autoRefreshTimer = null; }
    }
}