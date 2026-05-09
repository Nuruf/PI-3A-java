package service.api;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LieuEvenement {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    public static class GeoResult {
        public final String displayName;
        public final double lat;
        public final double lon;
        public GeoResult(String displayName, double lat, double lon) {
            this.displayName = displayName; this.lat = lat; this.lon = lon;
        }
        @Override public String toString() { return displayName; }
    }

    public static List<GeoResult> suggererLieux(String query) {
        List<GeoResult> suggestions = new ArrayList<>();
        if (query == null || query.trim().length() < 2) return suggestions;
        try {
            String encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
            String url = "https://nominatim.openstreetmap.org/search?q=" + encoded
                    + "&format=json&limit=6&addressdetails=1";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "MomentumHR/1.0 (academic project)")
                    .timeout(java.time.Duration.ofSeconds(5))
                    .GET().build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return suggestions;
            JSONArray results = new JSONArray(response.body());
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < results.length(); i++) {
                JSONObject obj = results.getJSONObject(i);
                JSONObject address = obj.optJSONObject("address");
                double lat = obj.getDouble("lat");
                double lon = obj.getDouble("lon");
                String city = "", country = "";
                if (address != null) {
                    if (address.has("city"))        city = address.getString("city");
                    else if (address.has("town"))    city = address.getString("town");
                    else if (address.has("village")) city = address.getString("village");
                    else if (address.has("state"))   city = address.getString("state");
                    if (address.has("country"))      country = address.getString("country");
                }
                if (city.isEmpty()) {
                    String[] parts = obj.getString("display_name").split(",");
                    city = parts[0].trim();
                    country = parts.length > 1 ? parts[parts.length - 1].trim() : "";
                }
                String formatted = city + (country.isEmpty() ? "" : ", " + country);
                if (!seen.contains(formatted) && !formatted.isBlank()) {
                    seen.add(formatted);
                    suggestions.add(new GeoResult(formatted, lat, lon));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return suggestions;
    }
}
