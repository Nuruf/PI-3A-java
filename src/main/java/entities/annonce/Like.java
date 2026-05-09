package entities.annonce;

import java.time.LocalDateTime;

public class Like {
    private int idLike;
    private int utilisateurId;
    private int postId;
    private LocalDateTime dateLike;

    public Like() {}

    public Like(int idLike, int utilisateurId, int postId) {
        this.idLike = idLike;
        this.utilisateurId = utilisateurId;
        this.postId = postId;
        this.dateLike = LocalDateTime.now();
    }

    public int getIdLike() {
        return idLike;
    }
    public void setIdLike(int idLike) {
        this.idLike = idLike;
    }
    public int getUtilisateurId() {
        return utilisateurId;
    }
    public void setUtilisateurId(int utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    public int getpostId() {
        return postId;
    }

    public void setpostId(int postId) {
        this.postId = postId;
    }
    public LocalDateTime getDateLike() {
        return dateLike;
    }

    public void setDateLike(LocalDateTime dateLike) {
        this.dateLike = dateLike;
    }

    @Override
    public String toString() {
        return "Like{" +
                "idLike=" + idLike +
                ", utilisateurId=" + utilisateurId +
                ", postId=" + postId +
                ", dateLike=" + dateLike +
                '}';
    }
}

