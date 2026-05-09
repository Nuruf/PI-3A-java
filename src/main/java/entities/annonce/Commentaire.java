package entities.annonce;

import java.time.LocalDateTime;

public class Commentaire {
    private int idCommentaire;
    private String contenu;
    private LocalDateTime dateCommentaire;

    private int utilisateurId;
    private int postId;

    public Commentaire() {}

    public Commentaire(int idCommentaire,  String contenu, LocalDateTime dateCommentaire, int utilisateurId, int postId) {
        this.idCommentaire = idCommentaire;
        this.contenu = contenu;
        this.dateCommentaire = dateCommentaire;
        this.utilisateurId = utilisateurId;
        this.postId = postId;
    }

    public int getIdCommentaire() {
        return idCommentaire;
    }

    public void setIdCommentaire(int idCommentaire) {
        this.idCommentaire = idCommentaire;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public LocalDateTime getDateCommentaire() {
        return dateCommentaire;
    }

    public void setDateCommentaire(LocalDateTime dateCommentaire) {
        this.dateCommentaire = dateCommentaire;
    }

    public int getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(int utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    public int getPostId() {
        return postId;
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }

    @Override
    public String toString() {
        return "Commentaire{" +
                "idCommentaire=" + idCommentaire +
                ", contenu='" + contenu + '\'' +
                ", dateCommentaire=" + dateCommentaire +
                ", utilisateurId=" + utilisateurId +
                ", postId=" + postId +
                '}';
    }
}
