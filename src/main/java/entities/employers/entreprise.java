package entities.employers;

import entities.employers.statut;

import java.time.LocalDate;

public class entreprise {
    private int id;
    private String nom_entreprise;
    private String nom;
    private String prenom;
    private String pays;
    private String ville;
    private String matricule_fiscale;
    private int telephone;
    private String e_mail;
    private LocalDate date_demande;
    private statut statut;
    private String logo;
    private String siteWeb;
    public  entreprise() {}
    public entreprise(String nom_entreprise,String pays,String ville,String nom, String prenom, String matricule_fiscale,int telephone,String e_mail, String siteWeb,String logo) {
        this.nom_entreprise = nom_entreprise;
        this.pays = pays;
        this.ville = ville;
        this.nom = nom;
        this.prenom = prenom;
        this.matricule_fiscale = matricule_fiscale;
        this.telephone = telephone;
        this.e_mail = e_mail;
        this.date_demande = LocalDate.now();
        this.statut = statut.enattende;
        this.siteWeb = siteWeb;
        this.logo = logo;
    }

    public String getE_mail() {
        return e_mail;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setE_mail(String e_mail) {
        this.e_mail = e_mail;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPays() {
        return pays;
    }

    public void setPays(String pays) {
        this.pays = pays;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public String getMatricule_fiscale() {
        return matricule_fiscale;
    }

    public void setMatricule_fiscale(String matricule_fiscale) {
        this.matricule_fiscale = matricule_fiscale;
    }

    public int getTelephone() {
        return telephone;
    }

    public void setTelephone(int telephone) {
        this.telephone = telephone;
    }

    public LocalDate getDate_demande() {
        return date_demande;
    }

    public void setDate_demande(LocalDate date_demande) {
        this.date_demande = date_demande;
    }

    public statut getStatut() {
        return statut;
    }

    public void setStatut(statut statut) {
        this.statut = statut;
    }

    public String getNom_entreprise() {
        return nom_entreprise;
    }

    public void setNom_entreprise(String nom_entreprise) {
        this.nom_entreprise = nom_entreprise;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }
    public String getLogo() {
        return logo;
    }
    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getSiteWeb() {
        return siteWeb;
    }
    public void setSiteWeb(String siteWeb) {
        this.siteWeb = siteWeb; }

    @Override
    public String toString() {
        return "entreprise{" +
                "nom='" + nom + '\'' +
                ", pays='" + pays + '\'' +
                ", ville='" + ville + '\'' +
                ", matricule_fiscale='" + matricule_fiscale + '\'' +
                ", telephone=" + telephone +
                ", date_demande=" + date_demande +
                ", statut=" + statut +
                '}';
    }
}
