package service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.UUID;

public class GoogleMeetService {
    private static final String APPLICATION_NAME = "Momentum Recruitment";
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    private Calendar getCalendarService() throws Exception {
        // Load credentials with null check
        InputStream credentialsStream = getClass().getResourceAsStream("/credentials.json");
        if (credentialsStream == null) {
            throw new IllegalStateException("Fichier credentials.json introuvable dans /src/main/resources/");
        }

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                new InputStreamReader(credentialsStream));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, clientSecrets,
                Collections.singleton(CalendarScopes.CALENDAR))
                .setDataStoreFactory(new com.google.api.client.util.store.FileDataStoreFactory(
                        new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

        return new Calendar.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public String createMeeting(String summary, String candidateEmail, java.time.LocalDateTime startDateTime) throws Exception {
        Calendar service = getCalendarService();

        Event event = new Event()
                .setSummary(summary)
                .setDescription("Entretien technique via Momentum Recruitment Platform");

        // Convert LocalDateTime to RFC3339 format (ISO 8601)
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        String startTimeStr = startDateTime.format(formatter);
        DateTime start = DateTime.parseRfc3339(startTimeStr + "Z");

        // Set start time
        EventDateTime startEventDateTime = new EventDateTime()
                .setDateTime(start)
                .setTimeZone(ZoneId.systemDefault().getId());
        event.setStart(startEventDateTime);

        // Set end time (1 hour later)
        java.time.LocalDateTime endDateTime = startDateTime.plusHours(1);
        String endTimeStr = endDateTime.format(formatter);
        DateTime end = DateTime.parseRfc3339(endTimeStr + "Z");

        EventDateTime endEventDateTime = new EventDateTime()
                .setDateTime(end)
                .setTimeZone(ZoneId.systemDefault().getId());
        event.setEnd(endEventDateTime);

        // Enable Google Meet
        CreateConferenceRequest createRequest = new CreateConferenceRequest()
                .setRequestId(UUID.randomUUID().toString())
                .setConferenceSolutionKey(new ConferenceSolutionKey().setType("hangoutsMeet"));

        ConferenceData conferenceData = new ConferenceData()
                .setCreateRequest(createRequest);

        event.setConferenceData(conferenceData);

        // Add Candidate as Guest
        EventAttendee attendee = new EventAttendee()
                .setEmail(candidateEmail)
                .setResponseStatus("needsAction");
        event.setAttendees(Collections.singletonList(attendee));

        // Send email notifications
        event.setGuestsCanModify(false);
        event.setGuestsCanInviteOthers(false);

        // Insert event with conference data
        Event createdEvent = service.events().insert("primary", event)
                .setConferenceDataVersion(1)
                .setSendNotifications(true)
                .execute();

        // Return the Meet link
        String meetLink = createdEvent.getHangoutLink();
        if (meetLink == null || meetLink.isEmpty()) {
            // Fallback: try to get it from conference data
            if (createdEvent.getConferenceData() != null &&
                createdEvent.getConferenceData().getEntryPoints() != null) {
                meetLink = createdEvent.getConferenceData().getEntryPoints().stream()
                        .filter(ep -> "video".equals(ep.getEntryPointType()))
                        .map(EntryPoint::getUri)
                        .findFirst()
                        .orElse(createdEvent.getHtmlLink());
            }
        }

        return meetLink != null ? meetLink : createdEvent.getHtmlLink();
    }
}