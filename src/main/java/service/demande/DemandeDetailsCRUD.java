package service.demande;

import entities.demande.DemandeDetails;
import utils.MyDB;

import java.sql.*;

public class DemandeDetailsCRUD {
    private final Connection conn;

    public DemandeDetailsCRUD() {
        conn = MyDB.getInstance().getConn();
    }

    public void ajouter(DemandeDetails details) throws SQLException {
        String req = "INSERT INTO demande_details (id_demande, details) VALUES (?, ?)";
        try (PreparedStatement pst = conn.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, details.getIdDemande());
            pst.setString(2, details.getDetails());
            pst.executeUpdate();

            ResultSet rs = pst.getGeneratedKeys();
            if (rs.next()) {
                details.setIdDetails(rs.getInt(1));
            }
        }
    }

    public void modifier(DemandeDetails details) throws SQLException {
        String req = "UPDATE demande_details SET details=? WHERE id_demande=?";
        try (PreparedStatement pst = conn.prepareStatement(req)) {
            pst.setString(1, details.getDetails());
            pst.setInt(2, details.getIdDemande());
            pst.executeUpdate();
        }
    }

    public DemandeDetails getByDemande(int idDemande) throws SQLException {
        String req = "SELECT * FROM demande_details WHERE id_demande=?";
        try (PreparedStatement pst = conn.prepareStatement(req)) {
            pst.setInt(1, idDemande);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                DemandeDetails d = new DemandeDetails();
                d.setIdDetails(rs.getInt("id_details"));
                d.setIdDemande(rs.getInt("id_demande"));
                d.setDetails(rs.getString("details"));
                return d;
            }
        }
        return null;
    }

    public void supprimerByDemande(int idDemande) throws SQLException {
        String req = "DELETE FROM demande_details WHERE id_demande=?";
        try (PreparedStatement pst = conn.prepareStatement(req)) {
            pst.setInt(1, idDemande);
            pst.executeUpdate();
        }
    }
}