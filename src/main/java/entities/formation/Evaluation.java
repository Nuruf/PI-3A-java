package entities.formation;

import java.time.LocalDateTime;

/**
 * Classe représentant une évaluation d'une formation
 */
public class Evaluation {
    private int id_evaluation;
    private int id_formation;
    private int id_employe;
    private int note; // 1 à 5 étoiles
    private String commentaire;
    private LocalDateTime date_evaluation;
    private String nom_employe; // Pour affichage uniquement

    // Constructeurs
    public Evaluation() {}

    public Evaluation(int id_formation, int id_employe, int note, String commentaire) {
        this.id_formation = id_formation;
        this.id_employe = id_employe;
        this.note = note;
        this.commentaire = commentaire;
        this.date_evaluation = LocalDateTime.now();
    }

    public Evaluation(int id_evaluation, int id_formation, int id_employe, int note,
                     String commentaire, LocalDateTime date_evaluation) {
        this.id_evaluation = id_evaluation;
        this.id_formation = id_formation;
        this.id_employe = id_employe;
        this.note = note;
        this.commentaire = commentaire;
        this.date_evaluation = date_evaluation;
    }

    // Getters
    public int getId_evaluation() {
        return id_evaluation;
    }

    public int getId_formation() {
        return id_formation;
    }

    public int getId_employe() {
        return id_employe;
    }

    public int getNote() {
        return note;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public LocalDateTime getDate_evaluation() {
        return date_evaluation;
    }

    public String getNom_employe() {
        return nom_employe;
    }

    // Setters
    public void setId_evaluation(int id_evaluation) {
        this.id_evaluation = id_evaluation;
    }

    public void setId_formation(int id_formation) {
        this.id_formation = id_formation;
    }

    public void setId_employe(int id_employe) {
        this.id_employe = id_employe;
    }

    public void setNote(int note) {
        if (note >= 1 && note <= 5) {
            this.note = note;
        } else {
            throw new IllegalArgumentException("La note doit être entre 1 et 5");
        }
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public void setDate_evaluation(LocalDateTime date_evaluation) {
        this.date_evaluation = date_evaluation;
    }

    public void setNom_employe(String nom_employe) {
        this.nom_employe = nom_employe;
    }

    /**
     * Obtenir une représentation visuelle de la note (étoiles)
     */
    public String getStarRepresentation() {
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < note; i++) {
            stars.append("⭐");
        }
        return stars.toString();
    }

    @Override
    public String toString() {
        return "Evaluation{" +
                "id_evaluation=" + id_evaluation +
                ", id_formation=" + id_formation +
                ", id_employe=" + id_employe +
                ", note=" + note +
                ", commentaire='" + commentaire + '\'' +
                ", date_evaluation=" + date_evaluation +
                ", nom_employe='" + nom_employe + '\'' +
                '}';
    }
}

