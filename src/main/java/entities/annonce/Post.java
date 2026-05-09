package entities.annonce;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Post {

    private int idPost;
    private String titre;
    private String contenu;
    private int typePost; // 1 ou 2
    private LocalDateTime dateCreation;
    private int utilisateurId;
    private boolean active;

    // Champs EVENEMENT (NULL si typePost = 1)
    private LocalDate dateEvenement;
    private LocalDate dateFinEvenement;
    private String lieu;
    private Integer capaciteMax;

    private Double latitude;
    private Double longitude;


    public Post() {}

    public Post(int idPost, String titre, String contenu, int typePost, LocalDateTime dateCreation, int utilisateurId, boolean active, LocalDate dateEvenement, LocalDate dateFinEvenement, String lieu, Integer capaciteMax) {
        this.idPost = idPost;
        this.titre = titre;
        this.contenu = contenu;
        this.typePost = typePost;
        this.dateCreation = dateCreation;
        this.utilisateurId = utilisateurId;
        this.active = active;
        this.dateEvenement = dateEvenement;
        this.dateFinEvenement = dateFinEvenement;
        this.lieu = lieu;
        this.capaciteMax = capaciteMax;
    }

    public int getIdPost() {
        return idPost;
    }

    public void setIdPost(int idPost) {
        this.idPost = idPost;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public int getTypePost() {
        return typePost;
    }

    public void setTypePost(int typePost) {
        this.typePost = typePost;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public int getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(int utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDate getDateEvenement() {
        return dateEvenement;
    }

    public void setDateEvenement(LocalDate dateEvenement) {
        this.dateEvenement = dateEvenement;
    }

    public LocalDate getDateFinEvenement() {
        return dateFinEvenement;
    }

    public void setDateFinEvenement(LocalDate dateFinEvenement) {
        this.dateFinEvenement = dateFinEvenement;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public Integer getCapaciteMax() {
        return capaciteMax;
    }

    public void setCapaciteMax(Integer capaciteMax) {
        this.capaciteMax = capaciteMax;
    }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    @Override
    public String toString() {
        return "Post{" +
                "idPost=" + idPost +
                ", titre='" + titre + '\'' +
                ", contenu='" + contenu + '\'' +
                ", typePost=" + typePost +
                ", dateCreation=" + dateCreation +
                ", utilisateurId=" + utilisateurId +
                ", active=" + active +
                ", dateEvenement=" + dateEvenement +
                ", dateFinEvenement=" + dateFinEvenement +
                ", lieu='" + lieu + '\'' +
                ", capaciteMax=" + capaciteMax +
                '}';
    }
}
