package service.formation;

import entities.formation.Evaluation;
import utils.MyDB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD pour gérer les évaluations des formations
 */
public class EvaluationCRUD implements InterfaceCRUD<Evaluation> {

    private Connection conn;

    public EvaluationCRUD() {
        conn = MyDB.getInstance().getConn();
    }

    @Override
    public void ajouter(Evaluation e) throws SQLException {
        String req = "INSERT INTO evaluation_formation (id_formation, id_employe, note, commentaire, date_evaluation) " +
                "VALUES (?, ?, ?, ?, ?)";

        PreparedStatement ps = conn.prepareStatement(req);
        ps.setInt(1, e.getId_formation());
        ps.setInt(2, e.getId_employe());
        ps.setInt(3, e.getNote());
        ps.setString(4, e.getCommentaire());
        ps.setTimestamp(5, Timestamp.valueOf(e.getDate_evaluation()));

        ps.executeUpdate();
        System.out.println("✅ Évaluation ajoutée !");
    }

    @Override
    public void modifier(Evaluation e) throws SQLException {
        String req = "UPDATE evaluation_formation SET note=?, commentaire=?, date_evaluation=? WHERE id_evaluation=?";

        PreparedStatement ps = conn.prepareStatement(req);
        ps.setInt(1, e.getNote());
        ps.setString(2, e.getCommentaire());
        ps.setTimestamp(3, Timestamp.valueOf(e.getDate_evaluation()));
        ps.setInt(4, e.getId_evaluation());

        ps.executeUpdate();
        System.out.println("✏️ Évaluation modifiée !");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM evaluation_formation WHERE id_evaluation=?";

        PreparedStatement ps = conn.prepareStatement(req);
        ps.setInt(1, id);

        ps.executeUpdate();
        System.out.println("🗑️ Évaluation supprimée !");
    }

    @Override
    public List<Evaluation> afficher() throws SQLException {
        String req = "SELECT e.*, emp.prenom, emp.nom " +
                "FROM evaluation_formation e " +
                "LEFT JOIN employe emp ON e.id_employe = emp.id_employe " +
                "ORDER BY e.date_evaluation DESC";

        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(req);

        List<Evaluation> evaluations = new ArrayList<>();

        while (rs.next()) {
            Evaluation eval = new Evaluation(
                rs.getInt("id_evaluation"),
                rs.getInt("id_formation"),
                rs.getInt("id_employe"),
                rs.getInt("note"),
                rs.getString("commentaire"),
                rs.getTimestamp("date_evaluation").toLocalDateTime()
            );

            String nomEmploye = rs.getString("prenom") + " " + rs.getString("nom");
            eval.setNom_employe(nomEmploye);

            evaluations.add(eval);
        }

        return evaluations;
    }

    /**
     * Obtenir les évaluations d'une formation spécifique
     */
    public List<Evaluation> getEvaluationsByFormation(int idFormation) throws SQLException {
        String req = "SELECT e.*, emp.prenom, emp.nom " +
                "FROM evaluation_formation e " +
                "LEFT JOIN employe emp ON e.id_employe = emp.id_employe " +
                "WHERE e.id_formation = ? " +
                "ORDER BY e.date_evaluation DESC";

        PreparedStatement ps = conn.prepareStatement(req);
        ps.setInt(1, idFormation);
        ResultSet rs = ps.executeQuery();

        List<Evaluation> evaluations = new ArrayList<>();

        while (rs.next()) {
            Evaluation eval = new Evaluation(
                rs.getInt("id_evaluation"),
                rs.getInt("id_formation"),
                rs.getInt("id_employe"),
                rs.getInt("note"),
                rs.getString("commentaire"),
                rs.getTimestamp("date_evaluation").toLocalDateTime()
            );

            String nomEmploye = rs.getString("prenom") + " " + rs.getString("nom");
            eval.setNom_employe(nomEmploye);

            evaluations.add(eval);
        }

        return evaluations;
    }

    /**
     * Obtenir la note moyenne d'une formation
     */
    public double getAverageRating(int idFormation) throws SQLException {
        String req = "SELECT AVG(note) as average FROM evaluation_formation WHERE id_formation = ?";

        PreparedStatement ps = conn.prepareStatement(req);
        ps.setInt(1, idFormation);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getDouble("average");
        }
        return 0.0;
    }

    /**
     * Obtenir le nombre d'évaluations pour une formation
     */
    public int getEvaluationCount(int idFormation) throws SQLException {
        String req = "SELECT COUNT(*) as count FROM evaluation_formation WHERE id_formation = ?";

        PreparedStatement ps = conn.prepareStatement(req);
        ps.setInt(1, idFormation);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getInt("count");
        }
        return 0;
    }

    /**
     * Vérifier si un employé a déjà évalué une formation
     */
    public boolean hasEvaluated(int idFormation, int idEmploye) throws SQLException {
        String req = "SELECT COUNT(*) as count FROM evaluation_formation WHERE id_formation = ? AND id_employe = ?";

        PreparedStatement ps = conn.prepareStatement(req);
        ps.setInt(1, idFormation);
        ps.setInt(2, idEmploye);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getInt("count") > 0;
        }
        return false;
    }

    /**
     * Obtenir l'évaluation d'un employé pour une formation
     */
    public Evaluation getEvaluationByFormationAndEmploye(int idFormation, int idEmploye) throws SQLException {
        String req = "SELECT e.*, emp.prenom, emp.nom " +
                "FROM evaluation_formation e " +
                "LEFT JOIN employe emp ON e.id_employe = emp.id_employe " +
                "WHERE e.id_formation = ? AND e.id_employe = ?";

        PreparedStatement ps = conn.prepareStatement(req);
        ps.setInt(1, idFormation);
        ps.setInt(2, idEmploye);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            Evaluation eval = new Evaluation(
                rs.getInt("id_evaluation"),
                rs.getInt("id_formation"),
                rs.getInt("id_employe"),
                rs.getInt("note"),
                rs.getString("commentaire"),
                rs.getTimestamp("date_evaluation").toLocalDateTime()
            );

            String nomEmploye = rs.getString("prenom") + " " + rs.getString("nom");
            eval.setNom_employe(nomEmploye);

            return eval;
        }

        return null;
    }

    /**
     * Obtenir les formations les mieux notées (pour le tri)
     */
    public List<Integer> getTopRatedFormations() throws SQLException {
        String req = "SELECT id_formation, AVG(note) as avg_note " +
                "FROM evaluation_formation " +
                "GROUP BY id_formation " +
                "ORDER BY avg_note DESC";

        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(req);

        List<Integer> topFormations = new ArrayList<>();

        while (rs.next()) {
            topFormations.add(rs.getInt("id_formation"));
        }

        return topFormations;
    }
}

