package service.annonce;

import entities.annonce.Notification;
import utils.MyDB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotificationCRUD {

    private Connection con = MyDB.getInstance().getConn();

    // ── Créer une notification ────────────────────────────────────
    public void ajouter(Notification n) throws SQLException {
        String sql = "INSERT INTO notification (user_id, titre, message, post_id, is_read, date_creation) VALUES (?,?,?,?,?,?)";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, n.getUtilisateurId());
        ps.setString(2, n.getType());       // on stocke le type dans "titre"
        ps.setString(3, n.getMessage());
        ps.setInt(4, n.getPostId());
        ps.setBoolean(5, false);
        ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
        ps.executeUpdate();
    }

    // ── Récupérer toutes les notifs d'un utilisateur ──────────────
    public List<Notification> getByUser(int userId) throws SQLException {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM notification WHERE user_id = ? ORDER BY date_creation DESC LIMIT 30";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Notification n = new Notification();
            n.setIdNotification(rs.getInt("id_notification"));
            n.setUtilisateurId(rs.getInt("user_id"));
            n.setType(rs.getString("titre"));   // type stocké dans "titre"
            n.setMessage(rs.getString("message"));
            n.setPostId(rs.getInt("post_id"));
            n.setLu(rs.getBoolean("is_read"));
            n.setDateCreation(rs.getTimestamp("date_creation").toLocalDateTime());
            list.add(n);
        }
        return list;
    }

    // ── Compter les non-lues ──────────────────────────────────────
    public int countUnread(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notification WHERE user_id = ? AND is_read = FALSE";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getInt(1) : 0;
    }

    // ── Marquer toutes comme lues ─────────────────────────────────
    public void markAllAsRead(int userId) throws SQLException {
        String sql = "UPDATE notification SET is_read = TRUE WHERE user_id = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.executeUpdate();
    }

    // ── Marquer une seule comme lue ───────────────────────────────
    public void markAsRead(int notifId) throws SQLException {
        String sql = "UPDATE notification SET is_read = TRUE WHERE id_notification = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, notifId);
        ps.executeUpdate();
    }

    // ── Récupère l'id du dernier post inséré ─────────────────────
    public int getLastInsertedPostId() throws SQLException {
        String sql = "SELECT id_post FROM post ORDER BY date_creation DESC LIMIT 1";
        PreparedStatement ps = con.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getInt(1) : -1;
    }

    // ── Notifier tous les users sauf l'acteur ─────────────────────
    public void notifierTous(int actorId, String type, String message, int postId) throws SQLException {
        List<Integer> allIds = getAllUserIds();
        for (int userId : allIds) {
            if (userId == actorId) continue; // pas soi-même
            Notification n = new Notification();
            n.setUtilisateurId(userId);
            n.setType(type);
            n.setMessage(message);
            n.setPostId(postId);
            ajouter(n);
        }
    }

    // ── Récupère tous les IDs utilisateurs connus ─────────────────
    private List<Integer> getAllUserIds() throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT id_employe FROM `employé`";
        PreparedStatement ps = con.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            ids.add(rs.getInt(1));
        }
        return ids;
    }

    // ── Supprimer toutes les notifs d'un user ─────────────────────
    public void clearAll(int userId) throws SQLException {
        String sql = "DELETE FROM notification WHERE user_id = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.executeUpdate();
    }
}
