package service.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class WeatherService {

    private final OkHttpClient client;
    private final boolean enabled;

    private WeatherData cachedWeather;
    private List<DailyForecast> cachedForecast;
    private long lastWeatherFetch;
    private long lastForecastFetch;
    private static final long CACHE_MS = 10 * 60 * 1000;

    public WeatherService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
        this.enabled = ApiConfig.WEATHER_API_KEY != null
                && !ApiConfig.WEATHER_API_KEY.contains("xxxxxxx")
                && !ApiConfig.WEATHER_API_KEY.isEmpty();

        System.out.println(enabled
                ? "✅ Weather service configured"
                : "⚠️ Weather API key not set in ApiConfig.java");
    }

    public boolean isEnabled() { return enabled; }

    // ══════════════════════ CURRENT WEATHER ══════════════════════

    public WeatherData getCurrentWeather() {
        if (cachedWeather != null && System.currentTimeMillis() - lastWeatherFetch < CACHE_MS) {
            return cachedWeather;
        }
        if (!enabled) return getDefaultWeather();

        try {
            String url = ApiConfig.WEATHER_BASE_URL
                    + "?q=" + ApiConfig.OFFICE_CITY + "," + ApiConfig.COUNTRY_CODE
                    + "&appid=" + ApiConfig.WEATHER_API_KEY
                    + "&units=metric&lang=fr";

            Request req = new Request.Builder().url(url).build();
            try (Response resp = client.newCall(req).execute()) {
                if (resp.isSuccessful() && resp.body() != null) {
                    cachedWeather = parseWeatherData(resp.body().string());
                    lastWeatherFetch = System.currentTimeMillis();
                    return cachedWeather;
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Weather error: " + e.getMessage());
        }
        return getDefaultWeather();
    }

    public CompletableFuture<WeatherData> getCurrentWeatherAsync() {
        return CompletableFuture.supplyAsync(this::getCurrentWeather);
    }

    // ══════════════════════ WEEKLY FORECAST ══════════════════════

    public List<DailyForecast> getWeeklyForecast() {
        if (cachedForecast != null && System.currentTimeMillis() - lastForecastFetch < CACHE_MS) {
            return cachedForecast;
        }
        if (!enabled) return getDefaultForecast();

        try {
            String url = ApiConfig.FORECAST_BASE_URL
                    + "?q=" + ApiConfig.OFFICE_CITY + "," + ApiConfig.COUNTRY_CODE
                    + "&appid=" + ApiConfig.WEATHER_API_KEY
                    + "&units=metric&lang=fr";

            Request req = new Request.Builder().url(url).build();
            try (Response resp = client.newCall(req).execute()) {
                if (resp.isSuccessful() && resp.body() != null) {
                    cachedForecast = parseForecastData(resp.body().string());
                    lastForecastFetch = System.currentTimeMillis();
                    System.out.println("✅ Weekly forecast fetched: " + cachedForecast.size() + " days");
                    return cachedForecast;
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Forecast error: " + e.getMessage());
        }
        return getDefaultForecast();
    }

    public CompletableFuture<List<DailyForecast>> getWeeklyForecastAsync() {
        return CompletableFuture.supplyAsync(this::getWeeklyForecast);
    }

    private List<DailyForecast> parseForecastData(String json) {
        List<DailyForecast> dailyList = new ArrayList<>();

        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray list = root.getAsJsonArray("list");

            Map<String, List<JsonObject>> byDate = new LinkedHashMap<>();
            SimpleDateFormat keyFmt = new SimpleDateFormat("yyyy-MM-dd");

            for (int i = 0; i < list.size(); i++) {
                JsonObject item = list.get(i).getAsJsonObject();
                long dt = item.get("dt").getAsLong() * 1000;
                String dateKey = keyFmt.format(new Date(dt));
                byDate.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(item);
            }

            SimpleDateFormat dayNameFmt = new SimpleDateFormat("EEEE", Locale.FRENCH);
            SimpleDateFormat displayFmt = new SimpleDateFormat("dd/MM");

            for (Map.Entry<String, List<JsonObject>> entry : byDate.entrySet()) {
                List<JsonObject> slots = entry.getValue();

                double tempMin = Double.MAX_VALUE;
                double tempMax = Double.MIN_VALUE;
                double humiditySum = 0;
                double windSum = 0;
                Map<String, Integer> conditionCount = new HashMap<>();
                String bestIcon = "01d";
                int bestIconCount = 0;

                for (JsonObject slot : slots) {
                    JsonObject main = slot.getAsJsonObject("main");
                    double tMin = main.get("temp_min").getAsDouble();
                    double tMax = main.get("temp_max").getAsDouble();
                    if (tMin < tempMin) tempMin = tMin;
                    if (tMax > tempMax) tempMax = tMax;
                    humiditySum += main.get("humidity").getAsInt();
                    windSum += slot.getAsJsonObject("wind").get("speed").getAsDouble();

                    JsonObject w = slot.getAsJsonArray("weather").get(0).getAsJsonObject();
                    String desc = w.get("description").getAsString();
                    String icon = w.get("icon").getAsString();
                    int count = conditionCount.getOrDefault(desc, 0) + 1;
                    conditionCount.put(desc, count);
                    if (count > bestIconCount) {
                        bestIconCount = count;
                        bestIcon = icon;
                    }
                }

                int n = slots.size();
                long dt = slots.get(0).get("dt").getAsLong() * 1000;
                Date date = new Date(dt);

                DailyForecast df = new DailyForecast();
                df.date = date;
                df.dayName = capitalize(dayNameFmt.format(date));
                df.dateDisplay = displayFmt.format(date);
                df.tempMin = Math.round(tempMin);
                df.tempMax = Math.round(tempMax);
                df.humidity = (int) (humiditySum / n);
                df.windSpeed = Math.round((windSum / n) * 10.0) / 10.0;
                df.icon = bestIcon;

                df.description = conditionCount.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(e -> capitalize(e.getKey()))
                        .orElse("N/A");

                df.weatherId = slots.get(0).getAsJsonArray("weather")
                        .get(0).getAsJsonObject().get("id").getAsInt();

                dailyList.add(df);
            }
        } catch (Exception e) {
            System.out.println("Forecast parse error: " + e.getMessage());
            e.printStackTrace();
        }
        return dailyList;
    }

    // ══════════════════════ RECOMMENDATIONS ══════════════════════

    public WorkRecommendation getWorkRecommendation() {
        WeatherData w = getCurrentWeather();
        if (!w.isValid) {
            return new WorkRecommendation(RecommendationType.UNKNOWN,
                    "❓ Données météo non disponibles",
                    "Impossible de déterminer les conditions");
        }

        int id = w.weatherId;

        if (id >= 200 && id < 300)
            return new WorkRecommendation(RecommendationType.STAY_HOME,
                    "⛈️ Télétravail fortement recommandé",
                    "Orages — " + w.description);

        if ((id >= 502 && id < 600) || (id >= 600 && id < 700))
            return new WorkRecommendation(RecommendationType.STAY_HOME,
                    "🌧️ Télétravail recommandé",
                    w.description + " — Déplacements difficiles");

        if (w.temperature > 40)
            return new WorkRecommendation(RecommendationType.STAY_HOME,
                    "🌡️ Télétravail recommandé",
                    String.format("Chaleur extrême (%.0f°C)", w.temperature));

        if (w.temperature > 35)
            return new WorkRecommendation(RecommendationType.FLEXIBLE,
                    "☀️ Horaires flexibles conseillés",
                    String.format("Forte chaleur (%.0f°C)", w.temperature));

        if (w.temperature < 0)
            return new WorkRecommendation(RecommendationType.STAY_HOME,
                    "❄️ Télétravail recommandé",
                    String.format("Froid extrême (%.0f°C)", w.temperature));

        if ((id >= 300 && id < 400) || (id >= 500 && id < 502))
            return new WorkRecommendation(RecommendationType.COMMUTE_OK,
                    "🌧️ Conditions acceptables",
                    w.description + " — Prévoyez un parapluie");

        return new WorkRecommendation(RecommendationType.COMMUTE_OK,
                "☀️ Bonnes conditions",
                w.description + " — " + String.format("%.0f°C", w.temperature));
    }

    public String getWeatherAlert() {
        WeatherData w = getCurrentWeather();
        if (!w.isValid) return null;
        if (w.weatherId >= 200 && w.weatherId < 300) return "⚠️ ALERTE: Orages en cours!";
        if (w.temperature > 40)  return "⚠️ ALERTE: Canicule — Restez hydraté!";
        if (w.temperature < -5)  return "⚠️ ALERTE: Grand froid!";
        if (w.windSpeed > 20)    return "⚠️ ALERTE: Vents forts!";
        return null;
    }

    // ══════════════════════ PARSING ══════════════════════

    private WeatherData parseWeatherData(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            WeatherData d = new WeatherData();

            JsonObject main = root.getAsJsonObject("main");
            d.temperature = main.get("temp").getAsDouble();
            d.feelsLike   = main.get("feels_like").getAsDouble();
            d.humidity    = main.get("humidity").getAsInt();
            d.pressure    = main.get("pressure").getAsInt();
            d.tempMin     = main.get("temp_min").getAsDouble();
            d.tempMax     = main.get("temp_max").getAsDouble();

            JsonObject w = root.getAsJsonArray("weather").get(0).getAsJsonObject();
            d.description = capitalize(w.get("description").getAsString());
            d.icon        = w.get("icon").getAsString();
            d.main        = w.get("main").getAsString();
            d.weatherId   = w.get("id").getAsInt();

            JsonObject wind = root.getAsJsonObject("wind");
            d.windSpeed = wind.get("speed").getAsDouble();
            if (wind.has("deg")) d.windDeg = wind.get("deg").getAsInt();
            if (root.has("clouds"))     d.cloudiness = root.getAsJsonObject("clouds").get("all").getAsInt();
            if (root.has("visibility")) d.visibility = root.get("visibility").getAsInt();

            d.cityName = root.get("name").getAsString();
            if (root.has("sys")) {
                JsonObject sys = root.getAsJsonObject("sys");
                d.sunrise = sys.get("sunrise").getAsLong() * 1000;
                d.sunset  = sys.get("sunset").getAsLong() * 1000;
                d.country = sys.get("country").getAsString();
            }

            d.timestamp = System.currentTimeMillis();
            d.isValid   = true;
            return d;
        } catch (Exception e) {
            System.out.println("Parse error: " + e.getMessage());
            return getDefaultWeather();
        }
    }

    private WeatherData getDefaultWeather() {
        WeatherData d = new WeatherData();
        d.cityName = ApiConfig.OFFICE_CITY;
        d.country  = ApiConfig.COUNTRY_CODE;
        d.temperature = 25; d.feelsLike = 25; d.humidity = 50;
        d.description = "Données non disponibles";
        d.main = "Unknown"; d.icon = "01d"; d.isValid = false;
        d.timestamp = System.currentTimeMillis();
        return d;
    }

    private List<DailyForecast> getDefaultForecast() {
        List<DailyForecast> list = new ArrayList<>();
        String[] days = {"Lundi","Mardi","Mercredi","Jeudi","Vendredi"};
        for (String day : days) {
            DailyForecast df = new DailyForecast();
            df.dayName = day; df.dateDisplay = "--/--";
            df.tempMin = 0; df.tempMax = 0;
            df.description = "N/A"; df.icon = "01d"; df.humidity = 0;
            df.date = new Date();
            list.add(df);
        }
        return list;
    }

    private String capitalize(String s) {
        return (s == null || s.isEmpty()) ? s : s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    // ══════════════════════ DATA CLASSES ══════════════════════

    public static class WeatherData {
        public String cityName, country, description, main, icon;
        public double temperature, feelsLike, tempMin, tempMax, windSpeed;
        public int humidity, pressure, weatherId, windDeg, cloudiness, visibility;
        public long sunrise, sunset, timestamp;
        public boolean isValid = false;

        public String getIconUrl() {
            return "https://openweathermap.org/img/wn/" + icon + "@2x.png";
        }

        public String getWindDirection() {
            String[] dirs = {"N","NE","E","SE","S","SW","W","NW"};
            return dirs[(int) Math.round(windDeg / 45.0) % 8];
        }

        @Override
        public String toString() {
            return String.format("%s, %s | %.0f°C | %s | 💧%d%% | 💨%.1f m/s",
                    cityName, country, temperature, description, humidity, windSpeed);
        }
    }

    public static class DailyForecast {
        public Date date;
        public String dayName;
        public String dateDisplay;
        public double tempMin;
        public double tempMax;
        public String description;
        public String icon;
        public int humidity;
        public double windSpeed;
        public int weatherId;

        public String getIconUrl() {
            return "https://openweathermap.org/img/wn/" + icon + "@2x.png";
        }

        public String getWeatherEmoji() {
            if (icon == null) return "❓";
            switch (icon.substring(0, 2)) {
                case "01": return "☀️";
                case "02": return "⛅";
                case "03": case "04": return "☁️";
                case "09": return "🌧️";
                case "10": return "🌦️";
                case "11": return "⛈️";
                case "13": return "❄️";
                case "50": return "🌫️";
                default:   return "🌤️";
            }
        }

        @Override
        public String toString() {
            return String.format("%s %s: %.0f°/%.0f° %s",
                    dayName, dateDisplay, tempMin, tempMax, description);
        }
    }

    public enum RecommendationType { STAY_HOME, FLEXIBLE, COMMUTE_OK, UNKNOWN }

    public static class WorkRecommendation {
        public final RecommendationType type;
        public final String title;
        public final String description;

        public WorkRecommendation(RecommendationType type, String title, String description) {
            this.type = type; this.title = title; this.description = description;
        }

        public String getColor() {
            switch (type) {
                case STAY_HOME:  return "#e74c3c";
                case FLEXIBLE:   return "#f39c12";
                case COMMUTE_OK: return "#27ae60";
                default:         return "#95a5a6";
            }
        }
    }
}