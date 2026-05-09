package service.annonce;

import entities.annonce.Commentaire;
import service.InterfaceCRUD;
import utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service CRUD pour la gestion des commentaires sur les posts
 * Compatible avec votre structure de base de données existante
 */
public class CommentaireCRUD implements InterfaceCRUD<Commentaire> {

    private Connection conn;

    public CommentaireCRUD() {
        conn = MyDB.getInstance().getConn();
    }

    /**
     * Ajouter un nouveau commentaire
     */
    @Override
    public void ajouter(Commentaire commentaire) throws SQLException {
        String req = "INSERT INTO commentaire (contenu, date_commentaire, utilisateur_id, post_id) VALUES (?, ?, ?, ?)";

        PreparedStatement pst = conn.prepareStatement(req);
        pst.setString(1, commentaire.getContenu());
        pst.setTimestamp(2, Timestamp.valueOf(commentaire.getDateCommentaire()));
        pst.setInt(3, commentaire.getUtilisateurId());
        pst.setInt(4, commentaire.getPostId());

        pst.executeUpdate();
        System.out.println("✅ Commentaire ajouté");
    }

    /**
     * Modifier un commentaire existant
     */
    @Override
    public void modifier(Commentaire commentaire) throws SQLException {
        String req = "UPDATE commentaire SET contenu = ?, date_commentaire = ?, utilisateur_id = ?, post_id = ? WHERE id_commentaire = ?";

        PreparedStatement pst = conn.prepareStatement(req);
        pst.setString(1, commentaire.getContenu());
        pst.setTimestamp(2, Timestamp.valueOf(commentaire.getDateCommentaire()));
        pst.setInt(3, commentaire.getUtilisateurId());
        pst.setInt(4, commentaire.getPostId());
        pst.setInt(5, commentaire.getIdCommentaire());

        pst.executeUpdate();
        System.out.println("✅ Commentaire modifié");
    }

    /**
     * Supprimer un commentaire par ID
     */
    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM commentaire WHERE id_commentaire = ?";

        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, id);
        pst.executeUpdate();

        System.out.println("✅ Commentaire supprimé");
    }

    /**
     * Afficher tous les commentaires
     */
    @Override
    public List<Commentaire> afficher() throws SQLException {
        List<Commentaire> commentaires = new ArrayList<>();
        String req = "SELECT * FROM commentaire ORDER BY date_commentaire DESC";

        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            Commentaire c = new Commentaire();
            c.setIdCommentaire(rs.getInt("id_commentaire"));
            c.setContenu(rs.getString("contenu"));
            c.setDateCommentaire(rs.getTimestamp("date_commentaire").toLocalDateTime());
            c.setUtilisateurId(rs.getInt("utilisateur_id"));
            c.setPostId(rs.getInt("post_id"));

            commentaires.add(c);
        }

        return commentaires;
    }

    // ========== MÉTHODES SUPPLÉMENTAIRES ==========

    /**
     * Récupérer un commentaire par ID
     */
    public Commentaire getById(int id) throws SQLException {
        String req = "SELECT * FROM commentaire WHERE id_commentaire = ?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, id);

        ResultSet rs = pst.executeQuery();

        if (rs.next()) {
            Commentaire c = new Commentaire();
            c.setIdCommentaire(rs.getInt("id_commentaire"));
            c.setContenu(rs.getString("contenu"));
            c.setDateCommentaire(rs.getTimestamp("date_commentaire").toLocalDateTime());
            c.setUtilisateurId(rs.getInt("utilisateur_id"));
            c.setPostId(rs.getInt("post_id"));
            return c;
        }

        return null;
    }

    /**
     * Récupérer tous les commentaires d'un utilisateur
     */
    public List<Commentaire> getByUtilisateur(int utilisateurId) throws SQLException {
        List<Commentaire> commentaires = new ArrayList<>();
        String req = "SELECT * FROM commentaire WHERE utilisateur_id = ? ORDER BY date_commentaire DESC";

        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, utilisateurId);
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            Commentaire c = new Commentaire();
            c.setIdCommentaire(rs.getInt("id_commentaire"));
            c.setContenu(rs.getString("contenu"));
            c.setDateCommentaire(rs.getTimestamp("date_commentaire").toLocalDateTime());
            c.setUtilisateurId(rs.getInt("utilisateur_id"));
            c.setPostId(rs.getInt("post_id"));

            commentaires.add(c);
        }

        return commentaires;
    }

    /**
     * Récupérer tous les commentaires d'un post (triés du plus ancien au plus récent)
     */
    public List<Commentaire> getByPost(int postId) throws SQLException {
        List<Commentaire> commentaires = new ArrayList<>();
        String req = "SELECT * FROM commentaire WHERE post_id = ? ORDER BY date_commentaire ASC";

        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, postId);
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            Commentaire c = new Commentaire();
            c.setIdCommentaire(rs.getInt("id_commentaire"));
            c.setContenu(rs.getString("contenu"));
            c.setDateCommentaire(rs.getTimestamp("date_commentaire").toLocalDateTime());
            c.setUtilisateurId(rs.getInt("utilisateur_id"));
            c.setPostId(rs.getInt("post_id"));

            commentaires.add(c);
        }

        return commentaires;
    }

    /**
     * Compter le nombre de commentaires pour un post
     */
    public int countByPost(int postId) throws SQLException {
        String req = "SELECT COUNT(*) as total FROM commentaire WHERE post_id = ?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, postId);

        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return rs.getInt("total");
        }
        return 0;
    }

    /**
     * Compter le nombre de commentaires d'un utilisateur
     */
    public int countByUtilisateur(int utilisateurId) throws SQLException {
        String req = "SELECT COUNT(*) as total FROM commentaire WHERE utilisateur_id = ?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, utilisateurId);

        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return rs.getInt("total");
        }
        return 0;
    }

    /**
     * Récupérer les derniers commentaires (activité récente)
     */
    public List<Commentaire> getRecent(int limit) throws SQLException {
        List<Commentaire> commentaires = new ArrayList<>();
        String req = "SELECT * FROM commentaire ORDER BY date_commentaire DESC LIMIT ?";

        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, limit);
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            Commentaire c = new Commentaire();
            c.setIdCommentaire(rs.getInt("id_commentaire"));
            c.setContenu(rs.getString("contenu"));
            c.setDateCommentaire(rs.getTimestamp("date_commentaire").toLocalDateTime());
            c.setUtilisateurId(rs.getInt("utilisateur_id"));
            c.setPostId(rs.getInt("post_id"));

            commentaires.add(c);
        }

        return commentaires;
    }

    /**
     * Rechercher des commentaires par contenu
     */
    public List<Commentaire> searchByContenu(String keyword) throws SQLException {
        List<Commentaire> commentaires = new ArrayList<>();
        String req = "SELECT * FROM commentaire WHERE contenu LIKE ? ORDER BY date_commentaire DESC";

        PreparedStatement pst = conn.prepareStatement(req);
        pst.setString(1, "%" + keyword + "%");
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            Commentaire c = new Commentaire();
            c.setIdCommentaire(rs.getInt("id_commentaire"));
            c.setContenu(rs.getString("contenu"));
            c.setDateCommentaire(rs.getTimestamp("date_commentaire").toLocalDateTime());
            c.setUtilisateurId(rs.getInt("utilisateur_id"));
            c.setPostId(rs.getInt("post_id"));

            commentaires.add(c);
        }

        return commentaires;
    }

    /**
     * Supprimer tous les commentaires d'un post
     * Note: Avec ON DELETE CASCADE dans votre DB, ceci se fait automatiquement
     * mais cette méthode peut être utile pour un nettoyage manuel
     */
    public void deleteAllByPost(int postId) throws SQLException {
        String req = "DELETE FROM commentaire WHERE post_id = ?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, postId);
        int deleted = pst.executeUpdate();
        System.out.println("✅ " + deleted + " commentaire(s) supprimé(s) pour le post #" + postId);
    }

    /**
     * Supprimer tous les commentaires d'un utilisateur
     */
    public void deleteAllByUtilisateur(int utilisateurId) throws SQLException {
        String req = "DELETE FROM commentaire WHERE utilisateur_id = ?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, utilisateurId);
        int deleted = pst.executeUpdate();
        System.out.println("✅ " + deleted + " commentaire(s) supprimé(s) pour l'utilisateur #" + utilisateurId);
    }

    /**
     * Récupérer les posts les plus commentés
     */
    public List<Integer> getMostCommentedPosts(int limit) throws SQLException {
        List<Integer> postIds = new ArrayList<>();
        String req = "SELECT post_id, COUNT(*) as total FROM commentaire GROUP BY post_id ORDER BY total DESC LIMIT ?";

        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, limit);
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            postIds.add(rs.getInt("post_id"));
        }

        return postIds;
    }

    /**
     * Mettre à jour seulement le contenu d'un commentaire
     */
    public void updateContenu(int idCommentaire, String nouveauContenu) throws SQLException {
        String req = "UPDATE commentaire SET contenu = ?, date_commentaire = ? WHERE id_commentaire = ?";

        PreparedStatement pst = conn.prepareStatement(req);
        pst.setString(1, nouveauContenu);
        pst.setTimestamp(2, Timestamp.valueOf(java.time.LocalDateTime.now()));
        pst.setInt(3, idCommentaire);

        pst.executeUpdate();
        System.out.println("✅ Contenu du commentaire mis à jour");
    }

    /**
     * Vérifier si un utilisateur a déjà commenté un post
     */
    public boolean hasCommented(int utilisateurId, int postId) throws SQLException {
        String req = "SELECT COUNT(*) as total FROM commentaire WHERE utilisateur_id = ? AND post_id = ?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, utilisateurId);
        pst.setInt(2, postId);

        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return rs.getInt("total") > 0;
        }
        return false;
    }
}
