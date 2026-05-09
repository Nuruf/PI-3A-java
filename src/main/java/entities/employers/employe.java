package entities.employers;

import java.time.LocalDate;

public class employe {
    private int id_employé;
    private String nom;
    private String prenom;
    private String e_mail;
    private int telephone;
    private String poste;
    private role role;
    private LocalDate date_embauche;
    private String imageProfil;
    private String cv_nom;
    private byte[] cv_data;
    public static int idEntreprise;
    private Integer idCandidat;
    public static final String DEFAULT_IMAGE = "/icons/user.png";
    public employe(){}
    public employe(String nom, String prenom, String e_mail, int telephone, String poste, role role, LocalDate date_embauche,String cvNom, byte[] cvData) {
        this.nom=nom;
        this.prenom=prenom;
        this.e_mail=e_mail;
        this.telephone=telephone;
        this.poste=poste;
        this.role=role;
        this.date_embauche=date_embauche;
        this.cv_nom=cvNom;
        this.cv_data=cvData;
    }

    public int getId_employé() {
        return id_employé;
    }

    public void setId_employé(int id_employé) {
        this.id_employé = id_employé;
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

    public int getTelephone() {
        return telephone;
    }

    public void setTelephone(int telephone) {
        this.telephone = telephone;
    }

    public String getPoste() {
        return poste;
    }

    public void setPoste(String poste) {
        this.poste = poste;
    }

    public role getRole() {
        return role;
    }

    public void setRole(role role) {
        this.role = role;
    }

    public LocalDate getDate_embauche() {
        return date_embauche;
    }

    public void setDate_embauche(LocalDate date_embauche) {
        this.date_embauche = date_embauche;
    }

    public static int getIdEntreprise() {
        return idEntreprise;
    }

    public String getImageProfil() { return imageProfil; }

    public void setImageProfil(String imageProfil) { this.imageProfil = imageProfil; }
    public byte[] getCv_data() { return cv_data; }
    public void setCv_data(byte[] cv_data) { this.cv_data = cv_data; }

    public String getCv_nom() { return cv_nom; }
    public void setCv_nom(String cv_nom) { this.cv_nom = cv_nom; }
    public boolean hasCv() {
        return cv_data != null && cv_data.length > 0;
    }

    public static void setIdEntreprise(int idEntreprise) {
        employe.idEntreprise = idEntreprise;
    }
    public Integer getIdCandidat() { return idCandidat; }
    public void setIdCandidat(Integer idCandidat) { this.idCandidat = idCandidat; }

    public boolean hasCustomImage() {
        return imageProfil != null
                && !imageProfil.isBlank()
                && !imageProfil.equals(DEFAULT_IMAGE);
    }
    public String getImageEffective() {
        if (imageProfil != null && !imageProfil.isBlank()) {
            return imageProfil;
        }
        return DEFAULT_IMAGE;
    }


}
