package entities.annonce;

import java.time.LocalDateTime;

public class Participation {
    private int idParticipation;
    private int utilisateurId;
    private int postId; // événement
    private String statutParticipation; // INTERESTED / GOING / ATTENDED
    private LocalDateTime dateAction;

    public Participation() {}

    public Participation(int idParticipation, int utilisateurId, int postId, String statutParticipation, LocalDateTime dateAction) {
        this.idParticipation = idParticipation;
        this.utilisateurId = utilisateurId;
        this.postId = postId;
        this.statutParticipation = statutParticipation;
        this.dateAction = dateAction;

    }

    public int getIdParticipation() {
        return idParticipation;
    }

    public void setIdParticipation(int idParticipation) {
        this.idParticipation = idParticipation;
    }

    public int getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(int utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    public int getpostId() {
        return postId;
    }

    public void setpostId(int postId) {
        this.postId = postId;
    }

    public String getStatutParticipation() {
        return statutParticipation;
    }

    public void setStatutParticipation(String statutParticipation) {
        this.statutParticipation = statutParticipation;
    }

    public LocalDateTime getDateAction() {
        return dateAction;
    }

    public void setDateAction(LocalDateTime dateAction) {
        this.dateAction = dateAction;
    }

    @Override
    public String toString() {
        return "Participation{" +
                "idParticipation=" + idParticipation +
                ", utilisateurId=" + utilisateurId +
                ", postId=" + postId +
                ", statutParticipation='" + statutParticipation + '\'' +
                ", dateAction=" + dateAction +
                '}';
    }
}