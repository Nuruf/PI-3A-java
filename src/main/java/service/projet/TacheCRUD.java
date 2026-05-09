package service.projet;

import entities.projet.Tache;
import entities.projet.priority;
import entities.projet.statut_t;
import service.InterfaceCRUD;

import java.sql.*;
import java.util.List;

public class TacheCRUD implements InterfaceCRUD<Tache> {
    private Connection connection;
    public TacheCRUD() {
        connection = utils.MyDB.getInstance().getConn();
    }
    @Override
    public void ajouter(Tache t) throws SQLException {
        String sql = "INSERT INTO tache(id_projet, id_employe, titre, description, statut_tache, priorite, date_deb, date_limite, progression) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps= connection.prepareStatement(sql);
        ps.setInt(1, t.getId_projet());
        ps.setInt(2, t.getId_employe());
        ps.setString(3, t.getTitre());
        ps.setString(4, t.getDescription());
        ps.setString(5, t.getStatut_tache().name());
        ps.setString(6, t.getPriority_tache().name());
        ps.setDate(7, Date.valueOf(t.getDate_deb()));
        ps.setDate(8, Date.valueOf(t.getDate_limite()));
        ps.setInt(9, t.getProgression());
        ps.executeUpdate();
    }

    @Override
    public void modifier(Tache t) throws SQLException {
        String sql = "UPDATE tache SET id_projet = ?, id_employe = ?, titre = ?, description = ?, statut_tache = ?, priorite = ?, date_deb = ?, date_limite = ?, progression = ? WHERE id_tache = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, t.getId_projet());
        ps.setInt(2, t.getId_employe());
        ps.setString(3, t.getTitre());
        ps.setString(4, t.getDescription());
        ps.setString(5, t.getStatut_tache().name());
        ps.setString(6, t.getPriority_tache().name());
        ps.setDate(7, Date.valueOf(t.getDate_deb()));
        ps.setDate(8, Date.valueOf(t.getDate_limite()));
        ps.setInt(9, t.getProgression());
        ps.setInt(10, t.getId_tache());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM tache WHERE id_tache = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Tache> afficher() throws SQLException {
        String sql = "SELECT * FROM tache";
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql);
        List<Tache> taches = new java.util.ArrayList<>();
        while (resultSet.next()) {
            Tache tache = new Tache();
            tache.setId_tache(resultSet.getInt("id_tache"));
            tache.setId_projet(resultSet.getInt("id_projet"));
            tache.setId_employe(resultSet.getInt("id_employe"));
            tache.setTitre(resultSet.getString("titre"));
            tache.setDescription(resultSet.getString("description"));
            tache.setStatut_tache(statut_t.valueOf(resultSet.getString("statut_tache")));
            tache.setPriority_tache(priority.valueOf(resultSet.getString("priorite")));
            tache.setDate_deb(resultSet.getDate("date_deb").toLocalDate());
            tache.setDate_limite(resultSet.getDate("date_limite").toLocalDate());
            tache.setProgression(resultSet.getInt("progression"));
            taches.add(tache);
        }
        return taches;
    }
}
