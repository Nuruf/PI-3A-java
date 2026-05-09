package service.api;

public class ApiConfig {

    // ==================== SMTP EMAIL (replaces EmailJS) ====================
    public static final String SMTP_HOST     = "smtp.gmail.com";
    public static final String SMTP_PORT     = "587";
    public static final String SMTP_USERNAME = "dahen.baheeddine@gmail.com";
    public static final String SMTP_PASSWORD = "cbsg lpgb fwob xqdf";
    public static final String SMTP_FROM_NAME = "RH - Momentum";

    // ==================== OPEN WEATHER MAP ====================
    public static final String WEATHER_API_KEY   = "36f4b94b2bef44d5918cc09ffdcd3d6b";
    public static final String WEATHER_BASE_URL  = "https://api.openweathermap.org/data/2.5/weather";
    public static final String FORECAST_BASE_URL = "https://api.openweathermap.org/data/2.5/forecast";
    public static final String OFFICE_CITY       = "Tunis";
    public static final String COUNTRY_CODE      = "TN";

    // ==================== OPENSTREETMAP / NOMINATIM ====================
    public static final String NOMINATIM_BASE_URL    = "https://nominatim.openstreetmap.org";
    public static final String NOMINATIM_SEARCH_URL  = NOMINATIM_BASE_URL + "/search";
    public static final String NOMINATIM_REVERSE_URL = NOMINATIM_BASE_URL + "/reverse";

    public static final double DEFAULT_LATITUDE  = 36.8065;
    public static final double DEFAULT_LONGITUDE = 10.1815;
    public static final int    DEFAULT_ZOOM      = 13;

    // ==================== COMPANY INFO ====================
    public static final String COMPANY_NAME = "Momentum";
    public static final String RH_EMAIL     = "dahen.baheeddine@gmail.com";
    public static final String ADMIN_EMAIL  = "dahen.baheeddine@gmail.com";
}