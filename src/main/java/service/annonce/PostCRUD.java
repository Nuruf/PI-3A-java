package service.annonce;

import entities.annonce.Post;
import service.InterfaceCRUD;
import utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PostCRUD implements InterfaceCRUD<Post> {

    Connection conn;

    public PostCRUD() {
        conn = MyDB.getInstance().getConn();
    }

    @Override
    public void ajouter(Post p) throws SQLException {

        String req = """
            INSERT INTO post 
            (titre, contenu, type_post, date_creation, utilisateur_id, active,
             date_evenement, date_fin_evenement, lieu, capacite_max, latitude, longitude)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        PreparedStatement pst = conn.prepareStatement(req);

        pst.setString(1, p.getTitre());
        pst.setString(2, p.getContenu());
        pst.setInt(3, p.getTypePost());
        pst.setTimestamp(4, Timestamp.valueOf(p.getDateCreation()));
        pst.setInt(5, p.getUtilisateurId());
        pst.setBoolean(6, p.isActive());

        pst.setDate(7, p.getDateEvenement() != null ? Date.valueOf(p.getDateEvenement()) : null);
        pst.setDate(8, p.getDateFinEvenement() != null ? Date.valueOf(p.getDateFinEvenement()) : null);
        pst.setString(9, p.getLieu());
        pst.setObject(10, p.getCapaciteMax());
        pst.setObject(11, p.getLatitude());
        pst.setObject(12, p.getLongitude());

        pst.executeUpdate();
        System.out.println("Post ajouté");
    }

    @Override
    public void modifier(Post p) throws SQLException {

        String req = """
            UPDATE post SET
            titre = ?, contenu = ?, type_post = ?, active = ?,
            date_evenement = ?, date_fin_evenement = ?, lieu = ?, capacite_max = ?,
            latitude = ?, longitude = ?
            WHERE id_post = ?
        """;

        PreparedStatement pst = conn.prepareStatement(req);

        pst.setString(1, p.getTitre());
        pst.setString(2, p.getContenu());
        pst.setInt(3, p.getTypePost());
        pst.setBoolean(4, p.isActive());

        pst.setDate(5, p.getDateEvenement() != null ? Date.valueOf(p.getDateEvenement()) : null);
        pst.setDate(6, p.getDateFinEvenement() != null ? Date.valueOf(p.getDateFinEvenement()) : null);
        pst.setString(7, p.getLieu());
        pst.setObject(8, p.getCapaciteMax());
        pst.setObject(9, p.getLatitude());
        pst.setObject(10, p.getLongitude());

        pst.setInt(11, p.getIdPost());

        pst.executeUpdate();
        System.out.println("Post modifié");
    }

    @Override
    public void supprimer(int id) throws SQLException {

        String req = "DELETE FROM post WHERE id_post = ?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, id);
        pst.executeUpdate();

        System.out.println("Post supprimé");
    }

    @Override
    public List<Post> afficher() throws SQLException {

        List<Post> posts = new ArrayList<>();
        String req = "SELECT * FROM post";

        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {

            Post p = new Post();
            p.setIdPost(rs.getInt("id_post"));
            p.setTitre(rs.getString("titre"));
            p.setContenu(rs.getString("contenu"));
            p.setTypePost(rs.getInt("type_post"));
            p.setDateCreation(rs.getTimestamp("date_creation").toLocalDateTime());
            p.setUtilisateurId(rs.getInt("utilisateur_id"));
            p.setActive(rs.getBoolean("active"));

            Date d = rs.getDate("date_evenement");
            if (d != null) p.setDateEvenement(d.toLocalDate());

            Date df = rs.getDate("date_fin_evenement");
            if (df != null) p.setDateFinEvenement(df.toLocalDate());

            p.setLieu(rs.getString("lieu"));
            p.setCapaciteMax((Integer) rs.getObject("capacite_max"));
            p.setLatitude(rs.getObject("latitude") != null ? rs.getDouble("latitude") : null);
            p.setLongitude(rs.getObject("longitude") != null ? rs.getDouble("longitude") : null);

            posts.add(p);
        }

        return posts;
    }
    // ========== MÉTHODES ADMIN ==========

    /**
     * Récupérer un post par ID
     */
    public Post getById(int id) throws SQLException {
        String req = "SELECT * FROM post WHERE id_post = ?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, id);

        ResultSet rs = pst.executeQuery();

        if (rs.next()) {
            Post p = new Post();
            p.setIdPost(rs.getInt("id_post"));
            p.setTitre(rs.getString("titre"));
            p.setContenu(rs.getString("contenu"));
            p.setTypePost(rs.getInt("type_post"));
            p.setDateCreation(rs.getTimestamp("date_creation").toLocalDateTime());
            p.setUtilisateurId(rs.getInt("utilisateur_id"));
            p.setActive(rs.getBoolean("active"));

            Date d = rs.getDate("date_evenement");
            if (d != null) p.setDateEvenement(d.toLocalDate());

            Date df = rs.getDate("date_fin_evenement");
            if (df != null) p.setDateFinEvenement(df.toLocalDate());

            p.setLieu(rs.getString("lieu"));
            p.setCapaciteMax((Integer) rs.getObject("capacite_max"));
            p.setLatitude(rs.getObject("latitude") != null ? rs.getDouble("latitude") : null);
            p.setLongitude(rs.getObject("longitude") != null ? rs.getDouble("longitude") : null);

            return p;
        }

        return null;
    }

    /**
     * Recherche des posts par mot-clé dans le titre ou le contenu
     */
    public List<Post> searchByKeyword(String keyword) throws SQLException {
        List<Post> posts = new ArrayList<>();
        String query = "SELECT * FROM post WHERE titre LIKE ? OR contenu LIKE ? ORDER BY date_creation DESC";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            String searchPattern = "%" + keyword + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Post post = new Post();
                post.setIdPost(rs.getInt("id_post"));
                post.setUtilisateurId(rs.getInt("utilisateur_id"));
                post.setTitre(rs.getString("titre"));
                post.setContenu(rs.getString("contenu"));
                post.setDateCreation(rs.getTimestamp("date_creation").toLocalDateTime());
                post.setTypePost(rs.getInt("type_post"));
                post.setActive(rs.getBoolean("active"));

                // Event fields (nullable)
                if (rs.getDate("date_evenement") != null) {
                    post.setDateEvenement(rs.getDate("date_evenement").toLocalDate());
                }
                if (rs.getDate("date_fin_evenement") != null) {
                    post.setDateFinEvenement(rs.getDate("date_fin_evenement").toLocalDate());
                }
                post.setLieu(rs.getString("lieu"));

                Integer capacite = (Integer) rs.getObject("capacite_max");
                post.setCapaciteMax(capacite);

                posts.add(post);
            }
        }

        return posts;
    }

    /**
     * Classe pour les statistiques globales
     */
    public static class StatistiquesGlobales {
        public int totalPosts;
        public int totalAnnonces;
        public int totalEvenements;
        public int totalLikes;
        public int totalCommentaires;
        public int postsActifs;
        public int postsInactifs;
        public int totalParticipations;
    }

    /**
     * Récupère toutes les statistiques globales
     */
    public StatistiquesGlobales getStatistiquesGlobales() throws SQLException {
        StatistiquesGlobales stats = new StatistiquesGlobales();

        // Total posts
        String queryTotalPosts = "SELECT COUNT(*) FROM post";
        try (PreparedStatement stmt = conn.prepareStatement(queryTotalPosts)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                stats.totalPosts = rs.getInt(1);
            }
        }

        // Total annonces (type_post = 1)
        String queryAnnonces = "SELECT COUNT(*) FROM post WHERE type_post = 1";
        try (PreparedStatement stmt = conn.prepareStatement(queryAnnonces)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                stats.totalAnnonces = rs.getInt(1);
            }
        }

        // Total événements (type_post = 2)
        String queryEvenements = "SELECT COUNT(*) FROM post WHERE type_post = 2";
        try (PreparedStatement stmt = conn.prepareStatement(queryEvenements)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                stats.totalEvenements = rs.getInt(1);
            }
        }

        // Posts actifs
        String queryActifs = "SELECT COUNT(*) FROM post WHERE active = true";
        try (PreparedStatement stmt = conn.prepareStatement(queryActifs)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                stats.postsActifs = rs.getInt(1);
            }
        }

        // Posts inactifs
        String queryInactifs = "SELECT COUNT(*) FROM post WHERE active = false";
        try (PreparedStatement stmt = conn.prepareStatement(queryInactifs)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                stats.postsInactifs = rs.getInt(1);
            }
        }

        // Total likes
        String queryLikes = "SELECT COUNT(*) FROM like_post";
        try (PreparedStatement stmt = conn.prepareStatement(queryLikes)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                stats.totalLikes = rs.getInt(1);
            }
        }

        // Total commentaires
        String queryComments = "SELECT COUNT(*) FROM commentaire";
        try (PreparedStatement stmt = conn.prepareStatement(queryComments)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                stats.totalCommentaires = rs.getInt(1);
            }
        }

        // Total participations (si table existe)
        String queryParticipations = "SELECT COUNT(*) FROM participation";
        try (PreparedStatement stmt = conn.prepareStatement(queryParticipations)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                stats.totalParticipations = rs.getInt(1);
            }
        } catch (SQLException e) {
            // Table participation n'existe peut-être pas
            stats.totalParticipations = 0;
        }

        return stats;
    }
}