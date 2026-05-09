package entities.projet;

import java.time.LocalDate;

public class Projet {
    private int projet_id;
    private int responsable_id;
    private String nom;
    private String description;
    private LocalDate date_debut;
    private LocalDate date_fin_prevue;
    private LocalDate date_fin_reelle;
    private statut statut;
    private priority priority;
    public Projet(){}
    public Projet(int projet_id,int responsable_id, String nom, String description, LocalDate date_debut, LocalDate date_fin_prevue,LocalDate date_fin_reelle ,statut statut, priority priority) {
        this.projet_id = projet_id;
        this.responsable_id = responsable_id;
        this.nom = nom;
        this.description = description;
        this.date_debut = date_debut;
        this.date_fin_prevue = date_fin_prevue;
        this.date_fin_reelle = date_fin_reelle;
        this.statut = statut;
        this.priority = priority;
    }
    public Projet(int responsable_id,String nom, String description, LocalDate date_debut, LocalDate date_fin_prevue,LocalDate date_fin_reelle , statut statut, priority priority) {
        this.responsable_id = responsable_id;
        this.nom = nom;
        this.description = description;
        this.date_debut = date_debut;
        this.date_fin_prevue = date_fin_prevue;
        this.date_fin_reelle = date_fin_reelle;
        this.statut = statut;
        this.priority = priority;
    }

    public int getProjet_id() {
        return projet_id;
    }

    public void setProjet_id(int projet_id) {
        this.projet_id = projet_id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public LocalDate getDate_fin_prevue() {
        return date_fin_prevue;
    }

    public void setDate_fin_prevue(LocalDate date_fin_prevue) {
        this.date_fin_prevue = date_fin_prevue;
    }

    public LocalDate getDate_fin_reelle() {
        return date_fin_reelle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDate_debut() {
        return date_debut;
    }

    public void setDate_debut(LocalDate date_debut) {
        this.date_debut = date_debut;
    }

    public LocalDate getDate_fin() {
        return date_fin_prevue;
    }

    public void setDate_fin(LocalDate date_fin) {
        this.date_fin_prevue = date_fin_prevue;
    }
    public void setDate_fin_reelle(LocalDate date_fin_reelle) {
        this.date_fin_reelle = date_fin_reelle;
    }

    public int getResponsable_id() {
        return responsable_id;
    }

    public void setResponsable_id(int responsable_id) {
        this.responsable_id = responsable_id;
    }

    public statut getStatut() {
        return statut;
    }

    public void setStatut(statut statut) {
        this.statut = statut;
    }

    public priority getPriority() {
        return priority;
    }

    public void setPriority(priority priority) {
        this.priority = priority;
    }

    @Override
    public String toString() {
        return "Projets{" +
                "projet_id=" + projet_id +
                ", nom='" + nom + '\'' +
                ", description='" + description + '\'' +
                ", date_debut='" + date_debut + '\'' +
                ", date_fin_prevue='" + date_fin_prevue + '\'' +
                ", date_fin_reelle='" + date_fin_reelle + '\'' +
                ", responsable_id=" + responsable_id +
                ", statut=" + statut +
                ", priority=" + priority +
                '}';
    }
}
