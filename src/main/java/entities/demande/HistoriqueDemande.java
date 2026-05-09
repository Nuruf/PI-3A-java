package entities.demande;

import java.util.Date;

public class HistoriqueDemande {
    private int idHistorique;
    private int idDemande;
    private String ancienStatut;
    private String nouveauStatut;
    private Date dateAction;
    private String acteur;
    private String commentaire;

    public HistoriqueDemande() {}

    public HistoriqueDemande(int idHistorique, int idDemande, String ancienStatut,
                             String nouveauStatut, Date dateAction, String acteur,
                             String commentaire) {
        this.idHistorique = idHistorique;
        this.idDemande = idDemande;
        this.ancienStatut = ancienStatut;
        this.nouveauStatut = nouveauStatut;
        this.dateAction = dateAction;
        this.acteur = acteur;
        this.commentaire = commentaire;
    }

    public int getIdHistorique() { return idHistorique; }
    public void setIdHistorique(int idHistorique) { this.idHistorique = idHistorique; }

    public int getIdDemande() { return idDemande; }
    public void setIdDemande(int idDemande) { this.idDemande = idDemande; }

    public String getAncienStatut() { return ancienStatut; }
    public void setAncienStatut(String ancienStatut) { this.ancienStatut = ancienStatut; }

    public String getNouveauStatut() { return nouveauStatut; }
    public void setNouveauStatut(String nouveauStatut) { this.nouveauStatut = nouveauStatut; }

    public Date getDateAction() { return dateAction; }
    public void setDateAction(Date dateAction) { this.dateAction = dateAction; }

    public String getActeur() { return acteur; }
    public void setActeur(String acteur) { this.acteur = acteur; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    @Override
    public String toString() {
        return "HistoriqueDemande{" +
                "idHistorique=" + idHistorique +
                ", idDemande=" + idDemande +
                ", ancienStatut='" + ancienStatut + '\'' +
                ", nouveauStatut='" + nouveauStatut + '\'' +
                ", dateAction=" + dateAction +
                ", acteur='" + acteur + '\'' +
                ", commentaire='" + commentaire + '\'' +
                '}';
    }
}