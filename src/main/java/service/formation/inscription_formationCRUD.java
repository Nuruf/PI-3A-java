package service.formation;

import entities.formation.inscription_formation;
import entities.formation.StatutInscription;
import utils.MyDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class inscription_formationCRUD implements InterfaceCRUD<inscription_formation> {

    Connection conn;

    public inscription_formationCRUD() {
        conn = MyDB.getInstance().getConn();
    }

    @Override
    public void ajouter(inscription_formation i) throws SQLException {
        String req = "INSERT INTO inscription_formation (id_formation, statut, id_employe, raison) VALUES (?, ?, ?, ?)";

        PreparedStatement ps = conn.prepareStatement(req);
        ps.setInt(1, i.getId_formation());
        ps.setString(2, i.getStatut().name());
        ps.setInt(3, i.getId_user());
        ps.setString(4, i.getRaison());

        ps.executeUpdate();
        System.out.println("Inscription ajoutee !");
    }

    @Override
    public void modifier(inscription_formation i) throws SQLException {
        String req = "UPDATE inscription_formation SET id_formation=?, statut=?, id_employe=?, raison=? WHERE id_inscription=?";

        PreparedStatement ps = conn.prepareStatement(req);
        ps.setInt(1, i.getId_formation());
        ps.setString(2, i.getStatut().name());
        ps.setInt(3, i.getId_user());
        ps.setString(4, i.getRaison());
        ps.setInt(5, i.getId_inscription());

        ps.executeUpdate();
        System.out.println("Inscription modifiee");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM inscription_formation WHERE id_inscription=?";

        PreparedStatement ps = conn.prepareStatement(req);
        ps.setInt(1, id);

        ps.executeUpdate();
        System.out.println("Inscription supprimee");
    }

    @Override
    public List<inscription_formation> afficher() throws SQLException {
        String req = "SELECT * FROM inscription_formation";

        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(req);

        List<inscription_formation> liste = new ArrayList<inscription_formation>();

        while (rs.next()) {
            inscription_formation i = new inscription_formation();

            i.setId_inscription(rs.getInt("id_inscription"));
            i.setId_formation(rs.getInt("id_formation"));
            i.setId_user(rs.getInt("id_employe"));
            i.setStatut(parseStatut(rs.getString("statut")));
            i.setRaison(rs.getString("raison"));

            liste.add(i);
        }

        return liste;
    }

    private StatutInscription parseStatut(String value) throws SQLException {
        if (value == null || value.trim().isEmpty()) {
            throw new SQLException("Statut invalide ou vide");
        }
        return StatutInscription.valueOf(value);
    }

    /**
     * Vérifier si un employé est déjà inscrit à une formation
     */
    public inscription_formation getInscriptionByFormationAndEmploye(int idFormation, int idEmploye) throws SQLException {
        String req = "SELECT * FROM inscription_formation WHERE id_formation=? AND id_employe=?";

        PreparedStatement ps = conn.prepareStatement(req);
        ps.setInt(1, idFormation);
        ps.setInt(2, idEmploye);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            inscription_formation i = new inscription_formation();
            i.setId_inscription(rs.getInt("id_inscription"));
            i.setId_formation(rs.getInt("id_formation"));
            i.setId_user(rs.getInt("id_employe"));
            i.setStatut(parseStatut(rs.getString("statut")));
            i.setRaison(rs.getString("raison"));
            return i;
        }

        return null;
    }

    /**
     * Obtenir toutes les inscriptions pour une formation spécifique
     */
    public List<inscription_formation> getInscriptionsByFormation(int idFormation) throws SQLException {
        String req = "SELECT * FROM inscription_formation WHERE id_formation=?";

        PreparedStatement ps = conn.prepareStatement(req);
        ps.setInt(1, idFormation);

        ResultSet rs = ps.executeQuery();
        List<inscription_formation> liste = new ArrayList<>();

        while (rs.next()) {
            inscription_formation i = new inscription_formation();
            i.setId_inscription(rs.getInt("id_inscription"));
            i.setId_formation(rs.getInt("id_formation"));
            i.setId_user(rs.getInt("id_employe"));
            i.setStatut(parseStatut(rs.getString("statut")));
            i.setRaison(rs.getString("raison"));
            liste.add(i);
        }

        return liste;
    }

    /**
     * Compter le nombre d'inscriptions en attente pour une formation
     */
    public int countPendingInscriptions(int idFormation) throws SQLException {
        String req = "SELECT COUNT(*) as total FROM inscription_formation WHERE id_formation=? AND statut='EN_ATTENTE'";

        PreparedStatement ps = conn.prepareStatement(req);
        ps.setInt(1, idFormation);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getInt("total");
        }

        return 0;
    }
}
