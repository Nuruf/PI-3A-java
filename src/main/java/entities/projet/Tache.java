package entities.projet;

import java.time.LocalDate;

public class Tache {
    private int id_tache;
    private int id_projet;
    private int id_employe;
    private String titre;
    private String description;
    private statut_t statut_tache;
    private priority priority_tache;
    private LocalDate date_deb;
    private LocalDate date_limite;
    private int progression;

    public Tache(){}
    public Tache(int id_tache, int id_projet, int id_employe, String titre, String description, statut_t statut_tache, priority priority_tache, LocalDate date_deb, LocalDate date_limite, int progression) {
        this.id_tache = id_tache;
        this.id_projet = id_projet;
        this.id_employe = id_employe;
        this.titre = titre;
        this.description = description;
        this.statut_tache = statut_tache;
        this.priority_tache = priority_tache;
        this.date_deb = date_deb;
        this.date_limite = date_limite;
        this.progression = progression;
    }

    public Tache(int id_projet, int id_employe, String titre, String description, statut_t statut_tache, priority priority_tache, LocalDate date_deb, LocalDate date_limite, int progression) {
        this.id_projet = id_projet;
        this.id_employe = id_employe;
        this.titre = titre;
        this.description = description;
        this.statut_tache = statut_tache;
        this.priority_tache = priority_tache;
        this.date_deb = date_deb;
        this.date_limite = date_limite;
        this.progression = progression;
    }

    public int getId_tache() {
        return id_tache;
    }

    public void setId_tache(int id_tache) {
        this.id_tache = id_tache;
    }

    public int getId_projet() {
        return id_projet;
    }

    public void setId_projet(int id_projet) {
        this.id_projet = id_projet;
    }

    public int getId_employe() {
        return id_employe;
    }

    public void setId_employe(int id_employe) {
        this.id_employe = id_employe;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public statut_t getStatut_tache() {
        return statut_tache;
    }

    public void setStatut_tache(statut_t statut_tache) {
        this.statut_tache = statut_tache;
    }

    public priority getPriority_tache() {
        return priority_tache;
    }

    public void setPriority_tache(priority priority_tache) {
        this.priority_tache = priority_tache;
    }

    public LocalDate getDate_deb() {
        return date_deb;
    }

    public void setDate_deb(LocalDate date_deb) {
        this.date_deb = date_deb;
    }

    public LocalDate getDate_limite() {
        return date_limite;
    }

    public void setDate_limite(LocalDate date_limite) {
        this.date_limite = date_limite;
    }

    public int getProgression() {
        return progression;
    }

    public void setProgression(int progression) {
        this.progression = progression;
    }

    @Override
    public String toString() {
        return "Tache{" +
                "id_tache=" + id_tache +
                ", id_projet=" + id_projet +
                ", id_employe=" + id_employe +
                ", titre='" + titre + '\'' +
                ", description='" + description + '\'' +
                ", statut_tache=" + statut_tache +
                ", priority_tache=" + priority_tache +
                ", date_deb=" + date_deb +
                ", date_limite=" + date_limite +
                ", progression=" + progression +
                '}';
    }
}
