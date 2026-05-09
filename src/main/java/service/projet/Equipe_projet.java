package service.projet;

import controller.projets.AjoutTacheController;
import utils.MyDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Equipe_projet {
    private final Connection cnx = MyDB.getInstance().getConn();

    public boolean exists(int idProjet, int idEmploye) throws SQLException {
        String sql = "SELECT 1 FROM equipe_projet WHERE id_projet=? AND id_employe=? LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idProjet);
            ps.setInt(2, idEmploye);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void addEmployeeToProject(int idProjet, int idEmploye) throws SQLException {
        if (exists(idProjet, idEmploye)) {
            return;
        }

        String sql = "INSERT INTO equipe_projet (id_projet, id_employe) VALUES (?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idProjet);
            ps.setInt(2, idEmploye);
            ps.executeUpdate();
        }
    }

    public void removeEmployeeFromProject(int idProjet, int idEmploye) throws SQLException {
        String sql = "DELETE FROM equipe_projet WHERE id_projet = ? AND id_employe = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idProjet);
            ps.setInt(2, idEmploye);
            ps.executeUpdate();
        }
    }


    public void addEmployeesToProject(int idProjet, List<Integer> employeeIds) throws SQLException {
        for (Integer empId : employeeIds) {
            addEmployeeToProject(idProjet, empId);
        }
    }

    public void clearProjectTeam(int idProjet) throws SQLException {
        String sql = "DELETE FROM equipe_projet WHERE id_projet = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idProjet);
            ps.executeUpdate();
        }
    }


    public List<Integer> getEmployeeIdsForProject(int idProjet) throws SQLException {
        String sql = "SELECT id_employe FROM equipe_projet WHERE id_projet = ?";
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idProjet);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("id_employe"));
                }
            }
        }
        return ids;
    }

    /**
     * Get all project IDs that an employee is part of
     */
    public List<Integer> getProjectIdsForEmployee(int idEmploye) throws SQLException {
        String sql = "SELECT id_projet FROM equipe_projet WHERE id_employe = ?";
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idEmploye);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("id_projet"));
                }
            }
        }
        return ids;
    }


    public List<AjoutTacheController.EmployeeOption> getEmployeesForProject(int idProjet) throws SQLException {
        // Only return employees who are part of the project team
        String sql = """
            SELECT e.id_employe AS id_emp, CONCAT(e.nom, ' ', e.prenom) AS full_name
            FROM equipe_projet gp
            JOIN employe e ON e.id_employe = gp.id_employe
            WHERE gp.id_projet = ?
            ORDER BY e.nom, e.prenom
        """;
        List<AjoutTacheController.EmployeeOption> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idProjet);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new AjoutTacheController.EmployeeOption(rs.getInt("id_emp"), rs.getString("full_name")));
                }
            }
        }
        return list;
    }
}
