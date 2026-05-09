package service.annonce;

import entities.annonce.Participation;
import service.InterfaceCRUD;
import utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service CRUD pour la gestion des participations aux événements
 * Adapté pour la structure de base de données existante
 */
public class ParticipationCRUD implements InterfaceCRUD<Participation> {

    private Connection conn;

    public ParticipationCRUD() {
        conn = MyDB.getInstance().getConn();
    }

    /**
     * Ajouter une nouvelle participation
     */
    @Override
    public void ajouter(Participation participation) throws SQLException {
        String req = "INSERT INTO participation (utilisateur_id, post_id, statut, date_action) VALUES (?, ?, ?, ?)";

        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, participation.getUtilisateurId());
        pst.setInt(2, participation.getpostId());
        pst.setString(3, participation.getStatutParticipation());
        pst.setTimestamp(4, Timestamp.valueOf(participation.getDateAction()));

        pst.executeUpdate();
        System.out.println("✅ Participation ajoutée");
    }

    /**
     * Modifier une participation existante
     */
    @Override
    public void modifier(Participation participation) throws SQLException {
        String req = "UPDATE participation SET utilisateur_id = ?, post_id = ?, statut = ?, date_action = ? WHERE id_participation = ?";

        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, participation.getUtilisateurId());
        pst.setInt(2, participation.getpostId());
        pst.setString(3, participation.getStatutParticipation());
        pst.setTimestamp(4, Timestamp.valueOf(participation.getDateAction()));
        pst.setInt(5, participation.getIdParticipation());

        pst.executeUpdate();
        System.out.println("✅ Participation modifiée");
    }

    /**
     * Supprimer une participation par ID
     */
    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM participation WHERE id_participation = ?";

        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, id);
        pst.executeUpdate();

        System.out.println("✅ Participation supprimée");
    }

    /**
     * Afficher toutes les participations
     */
    @Override
    public List<Participation> afficher() throws SQLException {
        List<Participation> participations = new ArrayList<>();
        String req = "SELECT * FROM participation";

        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            Participation p = new Participation();
            p.setIdParticipation(rs.getInt("id_participation"));
            p.setUtilisateurId(rs.getInt("utilisateur_id"));
            p.setpostId(rs.getInt("post_id"));
            p.setStatutParticipation(rs.getString("statut")); // Colonne 'statut' dans votre DB
            p.setDateAction(rs.getTimestamp("date_action").toLocalDateTime());

            participations.add(p);
        }

        return participations;
    }

    // ========== MÉTHODES SUPPLÉMENTAIRES ==========

    /**
     * Récupérer une participation par ID
     */
    public Participation getById(int id) throws SQLException {
        String req = "SELECT * FROM participation WHERE id_participation = ?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, id);

        ResultSet rs = pst.executeQuery();

        if (rs.next()) {
            Participation p = new Participation();
            p.setIdParticipation(rs.getInt("id_participation"));
            p.setUtilisateurId(rs.getInt("utilisateur_id"));
            p.setpostId(rs.getInt("post_id"));
            p.setStatutParticipation(rs.getString("statut"));
            p.setDateAction(rs.getTimestamp("date_action").toLocalDateTime());
            return p;
        }

        return null;
    }

    /**
     * Récupérer toutes les participations d'un utilisateur
     */
    public List<Participation> getByUtilisateur(int utilisateurId) throws SQLException {
        List<Participation> participations = new ArrayList<>();
        String req = "SELECT * FROM participation WHERE utilisateur_id = ?";

        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, utilisateurId);
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            Participation p = new Participation();
            p.setIdParticipation(rs.getInt("id_participation"));
            p.setUtilisateurId(rs.getInt("utilisateur_id"));
            p.setpostId(rs.getInt("post_id"));
            p.setStatutParticipation(rs.getString("statut"));
            p.setDateAction(rs.getTimestamp("date_action").toLocalDateTime());

            participations.add(p);
        }

        return participations;
    }

    /**
     * Récupérer toutes les participations d'un événement (post)
     */
    public List<Participation> getByPost(int postId) throws SQLException {
        List<Participation> participations = new ArrayList<>();
        String req = "SELECT * FROM participation WHERE post_id = ?";

        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, postId);
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            Participation p = new Participation();
            p.setIdParticipation(rs.getInt("id_participation"));
            p.setUtilisateurId(rs.getInt("utilisateur_id"));
            p.setpostId(rs.getInt("post_id"));
            p.setStatutParticipation(rs.getString("statut"));
            p.setDateAction(rs.getTimestamp("date_action").toLocalDateTime());

            participations.add(p);
        }

        return participations;
    }

    /**
     * Récupérer les participations par statut (INTERESTED, GOING, ATTENDED)
     */
    public List<Participation> getByStatut(int postId, String statut) throws SQLException {
        List<Participation> participations = new ArrayList<>();
        String req = "SELECT * FROM participation WHERE post_id = ? AND statut = ?";

        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, postId);
        pst.setString(2, statut);
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            Participation p = new Participation();
            p.setIdParticipation(rs.getInt("id_participation"));
            p.setUtilisateurId(rs.getInt("utilisateur_id"));
            p.setpostId(rs.getInt("post_id"));
            p.setStatutParticipation(rs.getString("statut"));
            p.setDateAction(rs.getTimestamp("date_action").toLocalDateTime());

            participations.add(p);
        }

        return participations;
    }

    /**
     * Compter le nombre de participations pour un événement
     */
    public int countByPost(int postId) throws SQLException {
        String req = "SELECT COUNT(*) as total FROM participation WHERE post_id = ?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, postId);

        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return rs.getInt("total");
        }
        return 0;
    }

    /**
     * Compter par statut
     */
    public int countByStatut(int postId, String statut) throws SQLException {
        String req = "SELECT COUNT(*) as total FROM participation WHERE post_id = ? AND statut = ?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, postId);
        pst.setString(2, statut);

        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return rs.getInt("total");
        }
        return 0;
    }

    /**
     * Vérifier si un utilisateur participe déjà à un événement
     */
    public boolean isParticipating(int utilisateurId, int postId) throws SQLException {
        String req = "SELECT COUNT(*) as total FROM participation WHERE utilisateur_id = ? AND post_id = ?";
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
     * Vérifier si un utilisateur a participé avec un statut spécifique
     * @param utilisateurId ID de l'utilisateur
     * @param postId ID du post/événement
     * @param status Statut : "INTERESTED" ou "GOING"
     * @return true si l'utilisateur a ce statut de participation
     */
    public boolean hasUserParticipated(int utilisateurId, int postId, String status) throws SQLException {
        String req = "SELECT COUNT(*) as total FROM participation " +
                "WHERE utilisateur_id = ? AND post_id = ? AND statut = ?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, utilisateurId);
        pst.setInt(2, postId);
        pst.setString(3, status);

        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return rs.getInt("total") > 0;
        }
        return false;
    }

    /**
     * Récupérer la participation d'un utilisateur pour un événement spécifique
     */
    public Participation getByUserAndPost(int utilisateurId, int postId) throws SQLException {
        String req = "SELECT * FROM participation WHERE utilisateur_id = ? AND post_id = ?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, utilisateurId);
        pst.setInt(2, postId);

        ResultSet rs = pst.executeQuery();

        if (rs.next()) {
            Participation p = new Participation();
            p.setIdParticipation(rs.getInt("id_participation"));
            p.setUtilisateurId(rs.getInt("utilisateur_id"));
            p.setpostId(rs.getInt("post_id"));
            p.setStatutParticipation(rs.getString("statut"));
            p.setDateAction(rs.getTimestamp("date_action").toLocalDateTime());
            return p;
        }

        return null;
    }

    /**
     * Mettre à jour le statut d'une participation
     */
    public void updateStatut(int utilisateurId, int postId, String nouveauStatut) throws SQLException {
        String req = "UPDATE participation SET statut = ?, date_action = ? WHERE utilisateur_id = ? AND post_id = ?";

        PreparedStatement pst = conn.prepareStatement(req);
        pst.setString(1, nouveauStatut);
        pst.setTimestamp(2, Timestamp.valueOf(java.time.LocalDateTime.now()));
        pst.setInt(3, utilisateurId);
        pst.setInt(4, postId);

        pst.executeUpdate();
        System.out.println("✅ Statut de participation mis à jour: " + nouveauStatut);
    }

    /**
     * Toggle participation - Ajouter si n'existe pas, mettre à jour le statut si existe
     */
    public boolean toggleParticipation(int utilisateurId, int postId, String statut) throws SQLException {
        if (isParticipating(utilisateurId, postId)) {
            // Mettre à jour le statut
            updateStatut(utilisateurId, postId, statut);
            return false; // Participation existante mise à jour
        } else {
            // Nouvelle participation
            Participation p = new Participation();
            p.setUtilisateurId(utilisateurId);
            p.setpostId(postId);
            p.setStatutParticipation(statut);
            p.setDateAction(java.time.LocalDateTime.now());
            ajouter(p);
            return true; // Nouvelle participation créée
        }
    }

    /**
     * Supprimer tous les participations d'un événement
     */
    public void deleteAllByPost(int postId) throws SQLException {
        String req = "DELETE FROM participation WHERE post_id = ?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, postId);
        pst.executeUpdate();
        System.out.println("✅ Toutes les participations de l'événement supprimées");
    }

    /**
     * Récupérer les événements avec le plus de participants
     */
    public List<Integer> getMostPopularEvents(int limit) throws SQLException {
        List<Integer> postIds = new ArrayList<>();
        String req = "SELECT post_id, COUNT(*) as total FROM participation GROUP BY post_id ORDER BY total DESC LIMIT ?";

        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, limit);
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            postIds.add(rs.getInt("post_id"));
        }

        return postIds;
    }
}

