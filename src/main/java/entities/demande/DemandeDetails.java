package entities.demande;

public class DemandeDetails {
    private int idDetails;
    private int idDemande;
    private String details;

    public DemandeDetails() {}

    public DemandeDetails(int idDetails, int idDemande, String details) {
        this.idDetails = idDetails;
        this.idDemande = idDemande;
        this.details = details;
    }

    public int getIdDetails() { return idDetails; }
    public void setIdDetails(int idDetails) { this.idDetails = idDetails; }

    public int getIdDemande() { return idDemande; }
    public void setIdDemande(int idDemande) { this.idDemande = idDemande; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    @Override
    public String toString() {
        return "DemandeDetails{" +
                "idDetails=" + idDetails +
                ", idDemande=" + idDemande +
                ", details='" + details + '\'' +
                '}';
    }
}