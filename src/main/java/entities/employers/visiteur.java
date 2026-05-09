package entities.employers;

public class visiteur {
    private int id_visiteur;
    private String nom;
    private String prenom;
    private String e_mail;
    private String mot_de_passe;
    private int telephone;
    public visiteur() {}
    public visiteur(String nom, String prenom, String e_mail,String mot_de_passe, int telephone) {
        this.nom = nom;
        this.prenom = prenom;
        this.e_mail = e_mail;
        this.telephone = telephone;
        this.mot_de_passe = mot_de_passe;
    }

    public int getId_visiteur() {
        return id_visiteur;
    }

    public void setId_visiteur(int id_visiteur) {
        this.id_visiteur = id_visiteur;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getE_mail() {
        return e_mail;
    }

    public void setE_mail(String e_mail) {
        this.e_mail = e_mail;
    }

    public String getMot_de_passe() {
        return mot_de_passe;
    }

    public void setMot_de_passe(String mot_de_passe) {
        this.mot_de_passe = mot_de_passe;
    }

    public int getTelephone() {
        return telephone;
    }

    public void setTelephone(int telephone) {
        this.telephone = telephone;
    }
}
