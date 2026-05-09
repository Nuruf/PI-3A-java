package entities.annonce;

import java.time.LocalDateTime;

public class Notification {
    private int idNotification;
    private int utilisateurId;
    private String type;       // LIKE, COMMENT, PARTICIPATION, NEW_EVENT
    private String message;
    private int postId;
    private boolean lu;
    private LocalDateTime dateCreation;

    public Notification() {}

    public Notification(int utilisateurId, String type, String message, int postId) {
        this.utilisateurId = utilisateurId;
        this.type = type;
        this.message = message;
        this.postId = postId;
        this.lu = false;
        this.dateCreation = LocalDateTime.now();
    }

    public int getIdNotification() { return idNotification; }
    public void setIdNotification(int idNotification) { this.idNotification = idNotification; }

    public int getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(int utilisateurId) { this.utilisateurId = utilisateurId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getPostId() { return postId; }
    public void setPostId(int postId) { this.postId = postId; }

    public boolean isLu() { return lu; }
    public void setLu(boolean lu) { this.lu = lu; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}