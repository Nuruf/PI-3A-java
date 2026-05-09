package service.employers;

import com.google.gson.Gson;
import entities.employers.competences_employe;
import utils.MyDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class competence_employeCRUD {

    private Connection conn;
    private static final Gson gson = new Gson();

    public competence_employeCRUD() throws SQLException {
        conn = MyDB.getInstance().getConn();
    }

    public void ajouter(competences_employe c) throws SQLException {
        String sql = "insert into competence_employe (skills, formations, experience,id_employe) values (?, ?, ?, ?) on DUPLICATE key update skills = values(skills),formations = VALUES(formations), experience = VALUES(experience)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getSkills());
            ps.setString(2, c.getFormations());
            ps.setString(3, c.getExperience());
            ps.setInt(4, c.getIdEmploye());
            ps.executeUpdate();
        }
    }

    public competences_employe getByEmploye(int idEmploye) throws SQLException {
        String sql = "select * from competence_employe where id_employe = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEmploye);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    competences_employe c = new competences_employe();
                    c.setId(rs.getInt("id"));
                    c.setIdEmploye(rs.getInt("id_employe"));
                    c.setSkills(rs.getString("skills"));
                    c.setFormations(rs.getString("formations"));
                    c.setExperience(rs.getString("experience"));
                    return c;
                }
            }
        }
        return null;
    }

    public void supprimerParEmploye(int idEmploye) throws SQLException {
        String sql = "delete from competence_employe where id_employe = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEmploye);
            ps.executeUpdate();
        }
    }
}