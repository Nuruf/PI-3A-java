package entities.demande;

import java.util.Date;

public class Demande {
    private int idDemande;
    private int idEmploye;
    private String categorie;
    private String titre;
    private String description;
    private String priorite;
    private String status;
    private Date dateCreation;
    private String typeDemande;

    public Demande() {}

    public Demande(int idDemande, int idEmploye, String categorie,
                   String titre, String description, String priorite,
                   String status, Date dateCreation, String typeDemande) {
        this.idDemande = idDemande;
        this.idEmploye = idEmploye;
        this.categorie = categorie;
        this.titre = titre;
        this.description = description;
        this.priorite = priorite;
        this.status = status;
        this.dateCreation = dateCreation;
        this.typeDemande = typeDemande;
    }

    public int getIdDemande() { return idDemande; }
    public void setIdDemande(int idDemande) { this.idDemande = idDemande; }

    public int getIdEmploye() { return idEmploye; }
    public void setIdEmploye(int idEmploye) { this.idEmploye = idEmploye; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPriorite() { return priorite; }
    public void setPriorite(String priorite) { this.priorite = priorite; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getDateCreation() { return dateCreation; }
    public void setDateCreation(Date dateCreation) { this.dateCreation = dateCreation; }

    public String getTypeDemande() { return typeDemande; }
    public void setTypeDemande(String typeDemande) { this.typeDemande = typeDemande; }

    @Override
    public String toString() {
        return "Demande{id=" + idDemande + ", idEmploye=" + idEmploye
                + ", titre='" + titre + "', status='" + status + "'}";
    }
}