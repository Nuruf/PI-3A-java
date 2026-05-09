package service.annonce;

import entities.annonce.Like;
import service.InterfaceCRUD;
import utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service CRUD pour la gestion des likes sur les posts
 * Adapté pour la table 'like_post' de votre base de données
 */
public class LikeCRUD implements InterfaceCRUD<Like> {

    private Connection conn;

    public LikeCRUD() {
        conn = MyDB.getInstance().getConn();
    }

    /**
     * Ajouter un nouveau like
     */
    @Override
    public void ajouter(Like like) throws SQLException {
        String req = "INSERT INTO like_post (utilisateur_id, post_id, date_like) VALUES (?, ?, ?)";

        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, like.getUtilisateurId());
        pst.setInt(2, like.getpostId());
        pst.setTimestamp(3, Timestamp.valueOf(like.getDateLike()));

        pst.executeUpdate();
        System.out.println("✅ Like ajouté");
    }

    /**
     * Modifier un like (rarement utilisé)
     */
    @Override
    public void modifier(Like like) throws SQLException {
        String req = "UPDATE like_post SET utilisateur_id = ?, post_id = ?, date_like = ? WHERE id_like = ?";

        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, like.getUtilisateurId());
        pst.setInt(2, like.getpostId());
        pst.setTimestamp(3, Timestamp.valueOf(like.getDateLike()));
        pst.setInt(4, like.getIdLike());

        pst.executeUpdate();
        System.out.println("✅ Like modifié");
    }

    /**
     * Supprimer un like par ID
     */
    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM like_post WHERE id_like = ?";

        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, id);
        pst.executeUpdate();

        System.out.println("✅ Like supprimé");
    }

    /**
     * Afficher tous les likes
     */
    @Override
    public List<Like> afficher() throws SQLException {
        List<Like> likes = new ArrayList<>();
        String req = "SELECT * FROM like_post";

        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            Like like = new Like();
            like.setIdLike(rs.getInt("id_like"));
            like.setUtilisateurId(rs.getInt("utilisateur_id"));
            like.setpostId(rs.getInt("post_id"));
            like.setDateLike(rs.getTimestamp("date_like").toLocalDateTime());

            likes.add(like);
        }

        return likes;
    }

    // ========== MÉTHODES SUPPLÉMENTAIRES ==========

    /**
     * Récupérer un like par ID
     */
    public Like getById(int id) throws SQLException {
        String req = "SELECT * FROM like_post WHERE id_like = ?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, id);

        ResultSet rs = pst.executeQuery();

        if (rs.next()) {
            Like like = new Like();
            like.setIdLike(rs.getInt("id_like"));
            like.setUtilisateurId(rs.getInt("utilisateur_id"));
            like.setpostId(rs.getInt("post_id"));
            like.setDateLike(rs.getTimestamp("date_like").toLocalDateTime());
            return like;
        }

        return null;
    }

    /**
     * Récupérer tous les likes d'un utilisateur
     */
    public List<Like> getByUtilisateur(int utilisateurId) throws SQLException {
        List<Like> likes = new ArrayList<>();
        String req = "SELECT * FROM like_post WHERE utilisateur_id = ?";

        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, utilisateurId);
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            Like like = new Like();
            like.setIdLike(rs.getInt("id_like"));
            like.setUtilisateurId(rs.getInt("utilisateur_id"));
            like.setpostId(rs.getInt("post_id"));
            like.setDateLike(rs.getTimestamp("date_like").toLocalDateTime());

            likes.add(like);
        }

        return likes;
    }

    /**
     * Récupérer tous les likes d'un post
     */
    public List<Like> getByPost(int postId) throws SQLException {
        List<Like> likes = new ArrayList<>();
        String req = "SELECT * FROM like_post WHERE post_id = ?";

        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, postId);
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            Like like = new Like();
            like.setIdLike(rs.getInt("id_like"));
            like.setUtilisateurId(rs.getInt("utilisateur_id"));
            like.setpostId(rs.getInt("post_id"));
            like.setDateLike(rs.getTimestamp("date_like").toLocalDateTime());

            likes.add(like);
        }

        return likes;
    }

    /**
     * Compter le nombre de likes pour un post
     */
    public int countByPost(int postId) throws SQLException {
        String req = "SELECT COUNT(*) as total FROM like_post WHERE post_id = ?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, postId);

        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return rs.getInt("total");
        }
        return 0;
    }

    /**
     * Vérifier si un utilisateur a liké un post
     */
    public boolean hasLiked(int utilisateurId, int postId) throws SQLException {
        String req = "SELECT COUNT(*) as total FROM like_post WHERE utilisateur_id = ? AND post_id = ?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, utilisateurId);
        pst.setInt(2, postId);

        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return rs.getInt("total") > 0;
        }
        return false;
    }

    /**
     * Récupérer le like d'un utilisateur pour un post spécifique
     */
    public Like getLikeByUserAndPost(int utilisateurId, int postId) throws SQLException {
        String req = "SELECT * FROM like_post WHERE utilisateur_id = ? AND post_id = ?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, utilisateurId);
        pst.setInt(2, postId);

        ResultSet rs = pst.executeQuery();

        if (rs.next()) {
            Like like = new Like();
            like.setIdLike(rs.getInt("id_like"));
            like.setUtilisateurId(rs.getInt("utilisateur_id"));
            like.setpostId(rs.getInt("post_id"));
            like.setDateLike(rs.getTimestamp("date_like").toLocalDateTime());
            return like;
        }

        return null;
    }

    /**
     * Toggle like - Ajouter si n'existe pas, supprimer si existe
     * @return true si like ajouté, false si like supprimé
     */
    public boolean toggleLike(int utilisateurId, int postId) throws SQLException {
        if (hasLiked(utilisateurId, postId)) {
            // Unlike - supprimer le like
            Like like = getLikeByUserAndPost(utilisateurId, postId);
            if (like != null) {
                supprimer(like.getIdLike());
                System.out.println("❤️ Unlike effectué");
                return false; // Unlike effectué
            }
        } else {
            // Like - ajouter le like
            Like like = new Like();
            like.setUtilisateurId(utilisateurId);
            like.setpostId(postId);
            like.setDateLike(java.time.LocalDateTime.now());
            ajouter(like);
            System.out.println("❤️ Like effectué");
            return true; // Like effectué
        }
        return false;
    }

    /**
     * Supprimer tous les likes d'un post (utile avant de supprimer un post)
     */
    public void deleteAllByPost(int postId) throws SQLException {
        String req = "DELETE FROM like_post WHERE post_id = ?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, postId);
        int deleted = pst.executeUpdate();
        System.out.println("✅ " + deleted + " like(s) supprimé(s) pour le post #" + postId);
    }

    /**
     * Supprimer tous les likes d'un utilisateur
     */
    public void deleteAllByUtilisateur(int utilisateurId) throws SQLException {
        String req = "DELETE FROM like_post WHERE utilisateur_id = ?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, utilisateurId);
        int deleted = pst.executeUpdate();
        System.out.println("✅ " + deleted + " like(s) supprimé(s) pour l'utilisateur #" + utilisateurId);
    }

    /**
     * Récupérer les posts les plus likés
     */
    public List<Integer> getMostLikedPosts(int limit) throws SQLException {
        List<Integer> postIds = new ArrayList<>();
        String req = "SELECT post_id, COUNT(*) as total FROM like_post GROUP BY post_id ORDER BY total DESC LIMIT ?";

        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, limit);
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            postIds.add(rs.getInt("post_id"));
        }

        return postIds;
    }

    /**
     * Récupérer les utilisateurs qui ont liké un post
     */
    public List<Integer> getUsersWhoLiked(int postId) throws SQLException {
        List<Integer> userIds = new ArrayList<>();
        String req = "SELECT utilisateur_id FROM like_post WHERE post_id = ?";

        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, postId);
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            userIds.add(rs.getInt("utilisateur_id"));
        }

        return userIds;
    }

    /**
     * Récupérer les likes récents (activité récente)
     */
    public List<Like> getRecentLikes(int limit) throws SQLException {
        List<Like> likes = new ArrayList<>();
        String req = "SELECT * FROM like_post ORDER BY date_like DESC LIMIT ?";

        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, limit);
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            Like like = new Like();
            like.setIdLike(rs.getInt("id_like"));
            like.setUtilisateurId(rs.getInt("utilisateur_id"));
            like.setpostId(rs.getInt("post_id"));
            like.setDateLike(rs.getTimestamp("date_like").toLocalDateTime());

            likes.add(like);
        }

        return likes;
    }
}
