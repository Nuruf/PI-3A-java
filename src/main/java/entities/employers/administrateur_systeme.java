package entities.employers;

public class administrateur_systeme {
    private int id;
    private String e_mail;
    private String mot_de_passe;
    public administrateur_systeme(){}
    public administrateur_systeme(String e_mail, String mot_de_passe) {
        this.e_mail = e_mail;
        this.mot_de_passe = mot_de_passe;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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
}
