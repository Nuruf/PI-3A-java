package entities.formation;

import java.time.LocalDate;

public class formation {
    private int id_formation;
    private String titre;
    private String organisme;
    private LocalDate date_debut;
    private LocalDate date_fin;
    private String lieu;
    private String capacite;

    public formation() {}
    public formation( String titre, String organisme, LocalDate date_debut, LocalDate date_fin, String lieu, String capacite) {
        this.titre = titre;
        this.organisme = organisme;
        this.date_debut = date_debut;
        this.date_fin = date_fin;
        this.lieu = lieu;
        this.capacite = capacite;
    }
    public formation(int id_formation, String titre, String organisme, LocalDate date_debut, LocalDate date_fin, String lieu, String capacite) {
        this.id_formation = id_formation;
        this.titre = titre;
        this.organisme = organisme;
        this.date_debut = date_debut;
        this.date_fin = date_fin;
        this.lieu = lieu;
        this.capacite = capacite;
    }

    public int getId_formation() {
        return id_formation;
    }

    public String getTitre() {
        return titre;
    }

    public String getOrganisme() {
        return organisme;
    }

    public LocalDate getDate_debut() {
        return date_debut;
    }

    public LocalDate getDate_fin() {
        return date_fin;
    }

    public String getLieu() {
        return lieu;
    }

    public String getCapacite() {
        return capacite;
    }

    public void setId_formation(int id_formation) {
        this.id_formation = id_formation;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public void setOrganisme(String organisme) {
        this.organisme = organisme;
    }

    public void setDate_debut(LocalDate date_debut) {
        this.date_debut = date_debut;
    }

    public void setDate_fin(LocalDate date_fin) {
        this.date_fin = date_fin;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public void setCapacite(String capacite) {
        this.capacite = capacite;
    }

    @Override
    public String toString() {
        return "j{" +
                "id_formation=" + id_formation +
                ", titre='" + titre + '\'' +
                ", organisme='" + organisme + '\'' +
                ", date_debut=" + date_debut +
                ", date_fin=" + date_fin +
                ", lieu='" + lieu + '\'' +
                ", capacite='" + capacite + '\'' +
                '}';
    }
}
