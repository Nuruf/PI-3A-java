package service.projet;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import com.google.api.services.calendar.model.Events;

import entities.projet.Tache;
import entities.projet.Projet;
import entities.projet.priority;
import entities.projet.statut;
import entities.projet.statut_t;

import java.io.*;
import java.net.ServerSocket;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Service class to integrate with Google Calendar API.
 * Allows creating, updating, and deleting calendar events for tasks and projects.
 */
public class GoogleCalendarService {

    private static final String APPLICATION_NAME = "Momentum Project Manager";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.apps.googleusercontent.com.json";

    private static GoogleCalendarService instance;
    private Calendar calendarService;
    private boolean isConnected = false;

    private GoogleCalendarService() {
        // Private constructor for singleton
    }

    public static GoogleCalendarService getInstance() {
        if (instance == null) {
            instance = new GoogleCalendarService();
        }
        return instance;
    }

    /**
     * Check if the service is connected to Google Calendar
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * Initialize connection to Google Calendar API
     * This will open a browser window for OAuth authentication if needed
     */
    public boolean connect() throws IOException, GeneralSecurityException {
        if (isConnected && calendarService != null) {
            return true;
        }

        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = getCredentials(HTTP_TRANSPORT);

        if (credential == null) {
            return false;
        }

        calendarService = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        isConnected = true;
        return true;
    }

    /**
     * Disconnect and clear stored credentials
     */
    public void disconnect() {
        // Delete stored tokens to force re-authentication
        File tokensDir = new File(TOKENS_DIRECTORY_PATH);
        if (tokensDir.exists()) {
            File[] files = tokensDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            tokensDir.delete();
        }
        calendarService = null;
        isConnected = false;
    }

    /**
     * Get OAuth credentials - will prompt for authentication if needed
     */
    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets from credentials.json file in resources
        InputStream in = GoogleCalendarService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);

        if (in == null) {
            System.err.println("⚠️ Google Calendar credentials file not found: " + CREDENTIALS_FILE_PATH);
            System.err.println("Please place your credentials.json file in src/main/resources/");
            return null;
        }

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

        // Find an available port dynamically to avoid "Address already in use" errors
        int port = findAvailablePort();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(port).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    /**
     * Find an available port for the OAuth callback server
     * Tries port 8888 first, then finds any available port
     */
    private int findAvailablePort() {
        // First try the preferred port 8888
        int[] preferredPorts = {8888, 8889, 8890, 9000, 9001};

        for (int port : preferredPorts) {
            try (ServerSocket socket = new ServerSocket(port)) {
                socket.setReuseAddress(true);
                return port;
            } catch (IOException e) {
                // Port is in use, try next
            }
        }

        // If all preferred ports are in use, let the system find any available port
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            // Fallback to 8888 and let it fail with a clear error
            return 8888;
        }
    }

    /**
     * Create a calendar event for a task
     * @param task The task to create an event for
     * @param projectName The name of the project (for context in the event)
     * @return The created event ID, or null if failed
     */
    public String createTaskEvent(Tache task, String projectName) throws IOException {
        if (!isConnected || calendarService == null) {
            throw new IOException("Not connected to Google Calendar. Please connect first.");
        }

        Event event = new Event()
                .setSummary("📋 " + task.getTitre())
                .setDescription(buildTaskDescription(task, projectName));

        // Set start date
        LocalDate startDate = task.getDate_deb() != null ? task.getDate_deb() : LocalDate.now();
        EventDateTime start = new EventDateTime()
                .setDate(new DateTime(startDate.toString()))
                .setTimeZone("Africa/Tunis");
        event.setStart(start);

        // Set end date (due date)
        LocalDate endDate = task.getDate_limite() != null ? task.getDate_limite() : startDate.plusDays(1);
        EventDateTime end = new EventDateTime()
                .setDate(new DateTime(endDate.toString()))
                .setTimeZone("Africa/Tunis");
        event.setEnd(end);

        // Set color based on priority
        event.setColorId(getColorIdForPriority(task.getPriority_tache()));

        // Add reminders
        EventReminder[] reminderOverrides = new EventReminder[] {
                new EventReminder().setMethod("popup").setMinutes(24 * 60), // 1 day before
                new EventReminder().setMethod("popup").setMinutes(60)       // 1 hour before
        };
        Event.Reminders reminders = new Event.Reminders()
                .setUseDefault(false)
                .setOverrides(Arrays.asList(reminderOverrides));
        event.setReminders(reminders);

        Event createdEvent = calendarService.events().insert("primary", event).execute();
        return createdEvent.getId();
    }

    /**
     * Create a calendar event for a project milestone
     * @param project The project to create an event for
     * @return The created event ID, or null if failed
     */
    public String createProjectEvent(Projet project) throws IOException {
        if (!isConnected || calendarService == null) {
            throw new IOException("Not connected to Google Calendar. Please connect first.");
        }

        Event event = new Event()
                .setSummary("🎯 Projet: " + project.getNom())
                .setDescription(buildProjectDescription(project));

        // Set start date
        LocalDate startDate = project.getDate_debut() != null ? project.getDate_debut() : LocalDate.now();
        EventDateTime start = new EventDateTime()
                .setDate(new DateTime(startDate.toString()))
                .setTimeZone("Africa/Tunis");
        event.setStart(start);

        // Set end date (deadline)
        LocalDate endDate = project.getDate_fin_prevue() != null ? project.getDate_fin_prevue() : startDate.plusDays(30);
        EventDateTime end = new EventDateTime()
                .setDate(new DateTime(endDate.toString()))
                .setTimeZone("Africa/Tunis");
        event.setEnd(end);

        // Set color based on priority
        event.setColorId(getColorIdForPriority(project.getPriority()));

        Event createdEvent = calendarService.events().insert("primary", event).execute();
        return createdEvent.getId();
    }

    /**
     * Update an existing calendar event for a task
     */
    public void updateTaskEvent(String eventId, Tache task, String projectName) throws IOException {
        if (!isConnected || calendarService == null) {
            throw new IOException("Not connected to Google Calendar. Please connect first.");
        }

        Event event = calendarService.events().get("primary", eventId).execute();

        event.setSummary("📋 " + task.getTitre());
        event.setDescription(buildTaskDescription(task, projectName));

        // Update dates
        LocalDate startDate = task.getDate_deb() != null ? task.getDate_deb() : LocalDate.now();
        EventDateTime start = new EventDateTime()
                .setDate(new DateTime(startDate.toString()))
                .setTimeZone("Africa/Tunis");
        event.setStart(start);

        LocalDate endDate = task.getDate_limite() != null ? task.getDate_limite() : startDate.plusDays(1);
        EventDateTime end = new EventDateTime()
                .setDate(new DateTime(endDate.toString()))
                .setTimeZone("Africa/Tunis");
        event.setEnd(end);

        event.setColorId(getColorIdForPriority(task.getPriority_tache()));

        calendarService.events().update("primary", eventId, event).execute();
    }

    /**
     * Delete a calendar event
     */
    public void deleteEvent(String eventId) throws IOException {
        if (!isConnected || calendarService == null) {
            throw new IOException("Not connected to Google Calendar. Please connect first.");
        }

        try {
            calendarService.events().delete("primary", eventId).execute();
        } catch (Exception e) {
            System.err.println("Warning: Could not delete event " + eventId + ": " + e.getMessage());
        }
    }

    /**
     * Get upcoming events from calendar
     */
    public List<Event> getUpcomingEvents(int maxResults) throws IOException {
        if (!isConnected || calendarService == null) {
            throw new IOException("Not connected to Google Calendar. Please connect first.");
        }

        DateTime now = new DateTime(System.currentTimeMillis());
        Events events = calendarService.events().list("primary")
                .setMaxResults(maxResults)
                .setTimeMin(now)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();

        return events.getItems();
    }

    /**
     * Build description for task event
     */
    private String buildTaskDescription(Tache task, String projectName) {
        StringBuilder sb = new StringBuilder();
        sb.append("📁 Projet: ").append(projectName).append("\n\n");

        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            sb.append("📝 Description:\n").append(task.getDescription()).append("\n\n");
        }

        sb.append("📊 Statut: ").append(formatStatus(task.getStatut_tache())).append("\n");
        sb.append("⚡ Priorité: ").append(formatPriority(task.getPriority_tache())).append("\n");
        sb.append("📈 Progression: ").append(task.getProgression()).append("%\n\n");
        sb.append("---\n");
        sb.append("Créé par Momentum Project Manager");

        return sb.toString();
    }

    /**
     * Build description for project event
     */
    private String buildProjectDescription(Projet project) {
        StringBuilder sb = new StringBuilder();

        if (project.getDescription() != null && !project.getDescription().isEmpty()) {
            sb.append("📝 Description:\n").append(project.getDescription()).append("\n\n");
        }

        sb.append("📊 Statut: ").append(formatProjectStatus(project.getStatut())).append("\n");
        sb.append("⚡ Priorité: ").append(formatPriority(project.getPriority())).append("\n\n");
        sb.append("---\n");
        sb.append("Créé par Momentum Project Manager");

        return sb.toString();
    }

    /**
     * Get Google Calendar color ID based on priority
     * Google Calendar color IDs: 1-11
     */
    private String getColorIdForPriority(priority priority) {
        if (priority == null) return "9"; // Blue (default)

        return switch (priority) {
            case HAUTE -> "11";    // Red
            case MOYENNE -> "5";   // Yellow
            case BASSE -> "10";    // Green
        };
    }

    private String formatStatus(statut_t status) {
        if (status == null) return "Non défini";
        return switch (status) {
            case A_FAIRE -> "À faire";
            case EN_COURS -> "En cours";
            case BLOCQUEE -> "Bloquée";
            case TERMINEE -> "Terminée";
        };
    }

    private String formatProjectStatus(statut status) {
        if (status == null) return "Non défini";
        return switch (status) {
            case PLANIFIE -> "Planifié";
            case EN_COURS -> "En cours";
            case TERMINE -> "Terminé";
            case ANNULE -> "Annulé";
        };
    }

    private String formatPriority(priority priority) {
        if (priority == null) return "Non définie";
        return switch (priority) {
            case HAUTE -> "🔴 Haute";
            case MOYENNE -> "🟡 Moyenne";
            case BASSE -> "🟢 Basse";
        };
    }
}


