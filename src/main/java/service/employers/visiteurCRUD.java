package service.employers;

import entities.employers.visiteur;
import utils.MyDB;

import java.sql.*;

public class visiteurCRUD {
    private Connection conn;
    public visiteurCRUD() throws SQLException {
        conn = MyDB.getInstance().getConn();
    }
    public void ajouter(visiteur v) throws Exception {
        String sql = "INSERT INTO visiteur(nom, prenom, e_mail, mot_de_passe, telephone) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, v.getNom());
            ps.setString(2, v.getPrenom());
            ps.setString(3, v.getE_mail());
            ps.setString(4, hachageMotDePasse.hashPassword(v.getMot_de_passe())); // hash ici
            ps.setInt(5, v.getTelephone());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    v.setId_visiteur(rs.getInt(1));
                }
            }
        }
    }
    public visiteur authentifier(String email, String Password) throws SQLException {
        String sql = "SELECT * FROM visiteur WHERE e_mail = ? AND mot_de_passe = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, hachageMotDePasse.hashPassword(Password));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    visiteur v = new visiteur();
                    v.setId_visiteur(rs.getInt("id_visiteur"));
                    v.setNom(rs.getString("nom"));
                    v.setPrenom(rs.getString("prenom"));
                    v.setE_mail(rs.getString("e_mail"));
                    v.setTelephone(rs.getInt("telephone"));
                    return v;
                }
            }
        }
        return null;
    }
    // Récupérer un visiteur par ID
    public visiteur getById(int id) throws SQLException {
        String req = "SELECT * FROM visiteur WHERE id_visiteur = ?";
        PreparedStatement ps = conn.prepareStatement(req);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            visiteur v = new visiteur();
            v.setId_visiteur(rs.getInt("id_visiteur"));
            v.setNom(rs.getString("nom"));
            v.setPrenom(rs.getString("prenom"));
            v.setE_mail(rs.getString("e_mail"));
            v.setMot_de_passe(rs.getString("mot_de_passe"));
            v.setTelephone(rs.getInt("telephone"));
            return v;
        }
        return null;
    }

}

