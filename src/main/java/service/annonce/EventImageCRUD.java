package service.annonce;

import entities.annonce.EventImage;
import utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventImageCRUD {

    private Connection conn;

    public EventImageCRUD() {
        conn = MyDB.getInstance().getConn();
    }

    public void ajouter(EventImage image) throws SQLException {
        String req = "INSERT INTO event_image (post_id, image_path, ordre) VALUES (?, ?, ?)";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, image.getPostId());
        pst.setString(2, image.getImagePath());
        pst.setInt(3, image.getOrdre());
        pst.executeUpdate();
    }

    public List<EventImage> getByPost(int postId) throws SQLException {
        List<EventImage> images = new ArrayList<>();
        String req = "SELECT * FROM event_image WHERE post_id = ? ORDER BY ordre ASC";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, postId);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            EventImage img = new EventImage();
            img.setIdImage(rs.getInt("id_image"));
            img.setPostId(rs.getInt("post_id"));
            img.setImagePath(rs.getString("image_path"));
            img.setOrdre(rs.getInt("ordre"));
            images.add(img);
        }
        return images;
    }

    public int countByPost(int postId) throws SQLException {
        String req = "SELECT COUNT(*) FROM event_image WHERE post_id = ?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, postId);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) return rs.getInt(1);
        return 0;
    }

    public void supprimer(int idImage) throws SQLException {
        String req = "DELETE FROM event_image WHERE id_image = ?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, idImage);
        pst.executeUpdate();
    }

    public void supprimerByPost(int postId) throws SQLException {
        String req = "DELETE FROM event_image WHERE post_id = ?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, postId);
        pst.executeUpdate();
    }
}
