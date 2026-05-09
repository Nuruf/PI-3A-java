package entities.offres;

import java.sql.Date;
import java.util.Arrays;

public class Candidat {
    private int id;
    private String codeCandidature;
    private String cvNom;
    private byte[] cvData;
    private String lettreMotivationNom;
    private byte[] lettreMotivationData;
    private String etat;
    private String note;
    private Date dateCandidature;
    private int idOffre;
    private double score = 0.0;

    private int idVisiteur;

    private String visiteurNom;
    private String visiteurPrenom;
    private String visiteurEmail;
    private int visiteurTelephone;

    public Candidat() {}

    public Candidat (String codeCandidature, String cvNom, byte[] cvData, String lettreMotivationNom, byte[] lettreMotivationData, String etat, String note, Date dateCandidature, int idOffre, int idVisiteur) {
        this.codeCandidature = codeCandidature;
        this.cvNom = cvNom;
        this.cvData = cvData;
        this.lettreMotivationNom = lettreMotivationNom;
        this.lettreMotivationData = lettreMotivationData;
        this.etat = etat;
        this.note = note;
        this.dateCandidature = dateCandidature;
        this.idOffre = idOffre;

        this.idVisiteur = idVisiteur;
    }

    public Candidat (String codeCandidature,String etat, String note, Date dateCandidature, int idOffre, int idVisiteur) {
        this.codeCandidature = codeCandidature;
        this.etat = etat;
        this.note = note;
        this.dateCandidature = dateCandidature;
        this.idOffre = idOffre;

        this.idVisiteur = idVisiteur;
    }

    public int getId() {
        return id;
    }

    public String getCodeCandidature() {
        return codeCandidature;
    }

    public String getCvNom() {
        return cvNom;
    }

    public byte[] getCvData() {
        return cvData;
    }

    public String getLettreMotivationNom() {
        return lettreMotivationNom;
    }

    public byte[] getLettreMotivationData() {
        return lettreMotivationData;
    }

    public String getEtat() {
        return etat;
    }

    public String getNote() {
        return note;
    }

    public Date getDateCandidature() {
        return dateCandidature;
    }

    public int getIdOffre() {
        return idOffre;
    }

    public double getScore() {
        return score;
    }

    public int getIdVisiteur() {return idVisiteur;}

    public String getVisiteurNom() { return visiteurNom; }

    public String getVisiteurPrenom() {
        return visiteurPrenom;
    }

    public String getVisiteurEmail() {
        return visiteurEmail;
    }

    public int getVisiteurTelephone() {
        return visiteurTelephone;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setCodeCandidature(String codeCandidature) {
        this.codeCandidature = codeCandidature;
    }

    public void setCvNom(String cvNom) {
        this.cvNom = cvNom;
    }

    public void setCvData(byte[] cvData) {
        this.cvData = cvData;
    }

    public void setLettreMotivationNom(String lettreMotivationNom) {
        this.lettreMotivationNom = lettreMotivationNom;
    }

    public void setLettreMotivationData(byte[] lettreMotivationData) {
        this.lettreMotivationData = lettreMotivationData;
    }

    public void setEtat(String etat) {
        this.etat = etat;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setDateCandidature(Date dateCandidature) {
        this.dateCandidature = dateCandidature;
    }

    public void setIdOffre(int idOffre) {
        this.idOffre = idOffre;
    }

    public void setIdVisiteur(int idVisiteur) {this.idVisiteur = idVisiteur;}

    public void setVisiteurNom(String visiteurNom) {
        this.visiteurNom = visiteurNom;
    }

    public void setVisiteurPrenom(String visiteurPrenom) {
        this.visiteurPrenom = visiteurPrenom;
    }

    public void setVisiteurEmail(String visiteurEmail) {
        this.visiteurEmail = visiteurEmail;
    }

    public void setVisiteurTelephone(int visiteurTelephone) {
        this.visiteurTelephone = visiteurTelephone;
    }

    @Override
    public String toString() {
        return "Candidat{" +
                "id=" + id +
                ", codeCandidature='" + codeCandidature + '\'' +
                ", cvNom='" + cvNom + '\'' +
                ", cvData=" + Arrays.toString(cvData) +
                ", lettreMotivationNom='" + lettreMotivationNom + '\'' +
                ", lettreMotivationData=" + Arrays.toString(lettreMotivationData) +
                ", etat='" + etat + '\'' +
                ", note='" + note + '\'' +
                ", dateCandidature=" + dateCandidature +
                ", idOffre=" + idOffre +
                ", score=" + score +
                '}';
    }
}