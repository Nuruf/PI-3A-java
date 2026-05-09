package service.api;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Pure JavaFX map picker — no WebView!
 * Downloads OSM tiles directly as JavaFX Images.
 * Free, no API key needed.
 */
public class MapPickerDialog {

    public static class LocationResult {
        public String cityName;
        public double lat;
        public double lon;
        public LocationResult(String cityName, double lat, double lon) {
            this.cityName = cityName;
            this.lat = lat;
            this.lon = lon;
        }
    }

    public interface LocationCallback {
        void onLocationSelected(LocationResult result);
    }

    // ── Map state ────────────────────────────────────────────────
    private int zoom = 4;
    private double centerLat = 34.0;
    private double centerLon = 9.0;
    private static final int TILE_SIZE = 256;
    private static final int MAP_W = 700;
    private static final int MAP_H = 420;

    private Pane mapPane;
    private Label infoLabel;
    private Button confirmBtn;
    private LocationCallback callback;
    private Stage stage;

    private double selectedLat = 0, selectedLon = 0;
    private String selectedCity = null;

    // Tile cache to avoid re-downloading
    private final Map<String, Image> tileCache = new HashMap<>();

    // Drag state
    private double dragStartX, dragStartY;
    private double dragStartLon, dragStartLat;

    public void show(LocationCallback callback) {
        this.callback = callback;

        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Choisir une destination");
        stage.setResizable(false);

        // ── Header ───────────────────────────────────────────────
        Label title = new Label("Cliquez sur la carte pour choisir votre destination");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #4A5DEF;");
        Label hint = new Label("Faites glisser pour naviguer • Molette pour zoomer");
        hint.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
        VBox header = new VBox(3, title, hint);
        header.setPadding(new Insets(12, 15, 12, 15));
        header.setStyle("-fx-background-color: #F0F5FF; -fx-border-color: #C0E0FF;" +
                "-fx-border-width: 0 0 1 0;");

        // ── Search bar ───────────────────────────────────────────
        TextField searchField = new TextField();
        searchField.setPromptText("Rechercher une ville...");
        searchField.setPrefWidth(300);
        searchField.setStyle("-fx-background-radius: 20; -fx-border-radius: 20;" +
                "-fx-background-color: white; -fx-border-color: #DDD; -fx-padding: 8 15;");

        Button searchBtn = new Button("Rechercher");
        searchBtn.setStyle("-fx-background-color: #4A5DEF; -fx-text-fill: white;" +
                "-fx-background-radius: 20; -fx-padding: 8 18; -fx-cursor: hand;");
        searchBtn.setOnAction(e -> searchCity(searchField.getText()));
        searchField.setOnAction(e -> searchCity(searchField.getText()));

        HBox searchBar = new HBox(8, searchField, searchBtn);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.setPadding(new Insets(8, 15, 8, 15));
        searchBar.setStyle("-fx-background-color: white;");

        // ── Map pane ─────────────────────────────────────────────
        mapPane = new Pane();
        mapPane.setPrefSize(MAP_W, MAP_H);
        mapPane.setStyle("-fx-background-color: #AAD3DF; -fx-cursor: crosshair;");
        mapPane.setClip(new javafx.scene.shape.Rectangle(MAP_W, MAP_H));

        // ── Zoom buttons ─────────────────────────────────────────
        Button zoomIn  = zoomButton("+");
        Button zoomOut = zoomButton("−");
        zoomIn.setOnAction(e  -> { if (zoom < 17) { zoom++; renderTiles(); } });
        zoomOut.setOnAction(e -> { if (zoom > 2)  { zoom--; renderTiles(); } });
        VBox zoomBox = new VBox(4, zoomIn, zoomOut);
        zoomBox.setStyle("-fx-padding: 8; -fx-background-color: white;" +
                "-fx-background-radius: 8; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.2),6,0,0,2);");
        zoomBox.setTranslateX(10);
        zoomBox.setTranslateY(10);

        // ── Info label on map ─────────────────────────────────────
        infoLabel = new Label("Cliquez pour choisir un lieu");
        infoLabel.setStyle("-fx-background-color: white; -fx-text-fill: #444;" +
                "-fx-padding: 6 14; -fx-background-radius: 20;" +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.2),6,0,0,2);" +
                "-fx-font-size: 12px; -fx-font-weight: bold;");
        infoLabel.setTranslateX(MAP_W / 2.0 - 120);
        infoLabel.setTranslateY(10);

        mapPane.getChildren().addAll(zoomBox, infoLabel);

        // ── Map interactions ──────────────────────────────────────
        // Click to pick location
        mapPane.setOnMouseClicked(e -> {
            if (e.isStillSincePress()) {
                double[] latLon = pixelToLatLon(e.getX(), e.getY());
                reverseGeocode(latLon[0], latLon[1]);
            }
        });

        // Drag to pan
        mapPane.setOnMousePressed(e -> {
            dragStartX   = e.getX();
            dragStartY   = e.getY();
            dragStartLon = centerLon;
            dragStartLat = centerLat;
            mapPane.setCursor(Cursor.CLOSED_HAND);
        });

        mapPane.setOnMouseDragged(e -> {
            double dx = e.getX() - dragStartX;
            double dy = e.getY() - dragStartY;
            double tilesX = dx / TILE_SIZE;
            double tilesY = dy / TILE_SIZE;
            int n = (int) Math.pow(2, zoom);
            centerLon = dragStartLon - tilesX * 360.0 / n;
            double yTile = latToTileY(dragStartLat, zoom) - tilesY;
            centerLat = tileYToLat(yTile, zoom);
            renderTiles();
        });

        mapPane.setOnMouseReleased(e -> mapPane.setCursor(Cursor.CROSSHAIR));

        // Scroll to zoom
        mapPane.setOnScroll(e -> {
            if (e.getDeltaY() > 0 && zoom < 17) zoom++;
            else if (e.getDeltaY() < 0 && zoom > 2) zoom--;
            renderTiles();
        });

        // ── Info + confirm row ────────────────────────────────────
        confirmBtn = new Button("✓  Confirmer cette destination");
        confirmBtn.setStyle("-fx-background-color: #4A5DEF; -fx-text-fill: white;" +
                "-fx-font-weight: bold; -fx-background-radius: 25;" +
                "-fx-padding: 10 30; -fx-cursor: hand; -fx-font-size: 13px;");
        confirmBtn.setDisable(true);
        confirmBtn.setOnAction(e -> {
            if (selectedCity != null) {
                callback.onLocationSelected(
                        new LocationResult(selectedCity, selectedLat, selectedLon));
                stage.close();
            }
        });

        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle("-fx-background-color: #888; -fx-text-fill: white;" +
                "-fx-background-radius: 25; -fx-padding: 10 25; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> stage.close());

        HBox footer = new HBox(12, cancelBtn, confirmBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12, 15, 12, 15));
        footer.setStyle("-fx-background-color: #F8F8F8; -fx-border-color: #E0E0E0;" +
                "-fx-border-width: 1 0 0 0;");

        // ── Assemble ──────────────────────────────────────────────
        VBox root = new VBox(header, searchBar, mapPane, footer);
        Scene scene = new Scene(root, MAP_W, MAP_H + 170);
        stage.setScene(scene);

        // Render tiles after showing
        stage.setOnShown(e -> renderTiles());
        stage.show();
    }

    // ── Tile rendering ───────────────────────────────────────────
    private void renderTiles() {
        // Remove old tile images (keep zoom buttons and info label)
        mapPane.getChildren().removeIf(n -> n instanceof ImageView);

        int n = (int) Math.pow(2, zoom);

        // Center tile
        double centerTileX = lonToTileX(centerLon, zoom);
        double centerTileY = latToTileY(centerLat, zoom);

        // How many tiles fit on screen
        int tilesX = (int) Math.ceil((double) MAP_W / TILE_SIZE) + 2;
        int tilesY = (int) Math.ceil((double) MAP_H / TILE_SIZE) + 2;

        int startTileX = (int) Math.floor(centerTileX) - tilesX / 2;
        int startTileY = (int) Math.floor(centerTileY) - tilesY / 2;

        // Pixel offset of top-left tile
        double offsetX = MAP_W / 2.0 - (centerTileX - startTileX) * TILE_SIZE;
        double offsetY = MAP_H / 2.0 - (centerTileY - startTileY) * TILE_SIZE;

        for (int tx = 0; tx <= tilesX; tx++) {
            for (int ty = 0; ty <= tilesY; ty++) {
                int tileX = ((startTileX + tx) % n + n) % n;
                int tileY = startTileY + ty;
                if (tileY < 0 || tileY >= n) continue;

                double px = offsetX + tx * TILE_SIZE;
                double py = offsetY + ty * TILE_SIZE;

                loadTile(tileX, tileY, zoom, px, py);
            }
        }

        // Re-add marker if location selected
        if (selectedCity != null) {
            addMarker(selectedLat, selectedLon);
        }
    }

    private final HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();

    private void loadTile(int tileX, int tileY, int z, double px, double py) {
        String key = z + "/" + tileX + "/" + tileY;

        ImageView iv = new ImageView();
        iv.setFitWidth(TILE_SIZE);
        iv.setFitHeight(TILE_SIZE);
        iv.setLayoutX(px);
        iv.setLayoutY(py);
        iv.setSmooth(true);
        mapPane.getChildren().add(0, iv); // add behind controls

        if (tileCache.containsKey(key)) {
            iv.setImage(tileCache.get(key));
            return;
        }

        // Download tile in background
        new Thread(() -> {
            try {
                String url = "https://tile.openstreetmap.org/" + z + "/" + tileX + "/" + tileY + ".png";
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "MomentumHRApp/1.0 (educational)")
                        .GET().build();
                HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
                if (resp.statusCode() == 200) {
                    Image img = new Image(new java.io.ByteArrayInputStream(resp.body()));
                    tileCache.put(key, img);
                    Platform.runLater(() -> iv.setImage(img));
                }
            } catch (Exception e) {
                // Tile failed — leave gray
            }
        }).start();
    }

    // ── Marker ───────────────────────────────────────────────────
    private void addMarker(double lat, double lon) {
        mapPane.getChildren().removeIf(n -> "marker".equals(n.getUserData()));

        double[] px = latLonToPixel(lat, lon);

        // Drop pin shape
        Circle circle = new Circle(10, Color.web("#4A5DEF"));
        circle.setStroke(Color.WHITE);
        circle.setStrokeWidth(3);
        circle.setEffect(new javafx.scene.effect.DropShadow(8, Color.rgb(0, 0, 0, 0.4)));
        circle.setLayoutX(px[0]);
        circle.setLayoutY(px[1]);
        circle.setUserData("marker");

        mapPane.getChildren().add(circle);
    }

    // ── Reverse geocoding ────────────────────────────────────────
    private void reverseGeocode(double lat, double lon) {
        infoLabel.setText("Recherche en cours...");
        infoLabel.setStyle(infoLabel.getStyle());

        new Thread(() -> {
            try {
                String url = "https://nominatim.openstreetmap.org/reverse?lat=" + lat
                        + "&lon=" + lon + "&format=json&accept-language=fr";
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "MomentumHRApp/1.0")
                        .GET().build();
                HttpResponse<String> resp = client.send(req,
                        HttpResponse.BodyHandlers.ofString());

                String body = resp.body();
                String city = extractField(body, "city");
                if (city == null) city = extractField(body, "town");
                if (city == null) city = extractField(body, "village");
                if (city == null) city = extractField(body, "municipality");
                if (city == null) city = extractField(body, "county");
                if (city == null) city = extractField(body, "state");
                if (city == null) city = "Lieu inconnu";

                final String finalCity = city;
                Platform.runLater(() -> {
                    selectedLat  = lat;
                    selectedLon  = lon;
                    selectedCity = finalCity;

                    infoLabel.setText("Destination: " + finalCity);
                    confirmBtn.setDisable(false);

                    addMarker(lat, lon);
                    centerLat = lat;
                    centerLon = lon;
                    renderTiles();
                });
            } catch (Exception e) {
                Platform.runLater(() -> infoLabel.setText("Erreur reseau"));
            }
        }).start();
    }

    // ── City search ───────────────────────────────────────────────
    private void searchCity(String query) {
        if (query == null || query.trim().isEmpty()) return;
        infoLabel.setText("Recherche: " + query + "...");

        new Thread(() -> {
            try {
                String encoded = java.net.URLEncoder.encode(query.trim(), "UTF-8");
                String url = "https://nominatim.openstreetmap.org/search?q=" + encoded
                        + "&format=json&limit=1&accept-language=fr";
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "MomentumHRApp/1.0")
                        .GET().build();
                HttpResponse<String> resp = client.send(req,
                        HttpResponse.BodyHandlers.ofString());

                String body = resp.body();
                String latStr = extractField(body, "lat");
                String lonStr = extractField(body, "lon");

                if (latStr != null && lonStr != null) {
                    double lat = Double.parseDouble(latStr);
                    double lon = Double.parseDouble(lonStr);
                    Platform.runLater(() -> {
                        centerLat = lat;
                        centerLon = lon;
                        zoom = 10;
                        renderTiles();
                        reverseGeocode(lat, lon);
                    });
                } else {
                    Platform.runLater(() -> infoLabel.setText("Ville non trouvee: " + query));
                }
            } catch (Exception e) {
                Platform.runLater(() -> infoLabel.setText("Erreur de recherche"));
            }
        }).start();
    }

    // ── Tile math ────────────────────────────────────────────────
    private double lonToTileX(double lon, int z) {
        return (lon + 180.0) / 360.0 * Math.pow(2, z);
    }

    private double latToTileY(double lat, int z) {
        double latRad = Math.toRadians(lat);
        return (1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI)
                / 2.0 * Math.pow(2, z);
    }

    private double tileYToLat(double tileY, int z) {
        double n = Math.PI - 2.0 * Math.PI * tileY / Math.pow(2, z);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }

    private double[] pixelToLatLon(double px, double py) {
        double centerTileX = lonToTileX(centerLon, zoom);
        double centerTileY = latToTileY(centerLat, zoom);
        double tileX = centerTileX + (px - MAP_W / 2.0) / TILE_SIZE;
        double tileY = centerTileY + (py - MAP_H / 2.0) / TILE_SIZE;
        double lon = tileX / Math.pow(2, zoom) * 360.0 - 180.0;
        double lat = tileYToLat(tileY, zoom);
        return new double[]{lat, lon};
    }

    private double[] latLonToPixel(double lat, double lon) {
        double centerTileX = lonToTileX(centerLon, zoom);
        double centerTileY = latToTileY(centerLat, zoom);
        double tileX = lonToTileX(lon, zoom);
        double tileY = latToTileY(lat, zoom);
        double px = MAP_W / 2.0 + (tileX - centerTileX) * TILE_SIZE;
        double py = MAP_H / 2.0 + (tileY - centerTileY) * TILE_SIZE;
        return new double[]{px, py};
    }

    // ── Helpers ───────────────────────────────────────────────────
    private String extractField(String json, String key) {
        String search = "\"" + key + "\":\"";
        int idx = json.indexOf(search);
        if (idx == -1) return null;
        int start = idx + search.length();
        int end = json.indexOf("\"", start);
        return end > start ? json.substring(start, end) : null;
    }

    private Button zoomButton(String text) {
        Button btn = new Button(text);
        btn.setPrefSize(30, 30);
        btn.setStyle("-fx-background-color: white; -fx-font-size: 16px; -fx-font-weight: bold;" +
                "-fx-background-radius: 4; -fx-cursor: hand; -fx-border-color: #DDD;" +
                "-fx-border-width: 1; -fx-text-fill: #333;");
        return btn;
    }

    // ── Read-only mode: view a location without selection ────────
    public void showReadOnly(double lat, double lon, String locationName) {
        this.centerLat = lat;
        this.centerLon = lon;
        this.zoom = 14;

        // Reuse show() with a dummy callback, but disable interaction
        show(result -> {}); // callback ignored

        // After stage opens, add marker and update label
        javafx.application.Platform.runLater(() -> {
            addMarker(lat, lon);
            infoLabel.setText("📍 " + locationName);
            confirmBtn.setVisible(false);
            confirmBtn.setManaged(false);
            // Update title
            stage.setTitle("Localisation : " + locationName);
        });
    }
}