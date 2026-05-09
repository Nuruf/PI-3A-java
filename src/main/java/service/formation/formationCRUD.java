package service.formation;

import entities.formation.formation;
import utils.MyDB;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class formationCRUD implements InterfaceCRUD<formation> {

    Connection conn;

    public formationCRUD() {
        conn = MyDB.getInstance().getConn();
    }

    @Override
    public void ajouter(formation f) throws SQLException {
        String req = "INSERT INTO formation (titre, organisme, date_debut, date_fin, lieu, capacite) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = conn.prepareStatement(req);
        ps.setString(1, f.getTitre());
        ps.setString(2, f.getOrganisme());
        ps.setDate(3, f.getDate_debut() != null ? Date.valueOf(f.getDate_debut()) : null);
        ps.setDate(4, f.getDate_fin() != null ? Date.valueOf(f.getDate_fin()) : null);
        ps.setString(5, f.getLieu());

        // Convertir capacite en int si possible, sinon utiliser 0
        try {
            int capacite = Integer.parseInt(f.getCapacite());
            ps.setInt(6, capacite);
        } catch (NumberFormatException e) {
            ps.setInt(6, 0);
        }

        ps.executeUpdate();
        System.out.println("Formation ajoutée !");
    }

    @Override
    public void modifier(formation f) throws SQLException {
        String req = "UPDATE formation SET titre=?, organisme=?, date_debut=?, date_fin=?, lieu=?, capacite=? " +
                "WHERE id_formation=?";

        PreparedStatement ps = conn.prepareStatement(req);
        ps.setString(1, f.getTitre());
        ps.setString(2, f.getOrganisme());
        ps.setDate(3, f.getDate_debut() != null ? Date.valueOf(f.getDate_debut()) : null);
        ps.setDate(4, f.getDate_fin() != null ? Date.valueOf(f.getDate_fin()) : null);
        ps.setString(5, f.getLieu());

        // Convertir capacite en int si possible, sinon utiliser 0
        try {
            int capacite = Integer.parseInt(f.getCapacite());
            ps.setInt(6, capacite);
        } catch (NumberFormatException e) {
            ps.setInt(6, 0);
        }

        ps.setInt(7, f.getId_formation());

        ps.executeUpdate();
        System.out.println("Formation modifiée");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM formation WHERE id_formation=?";

        PreparedStatement ps = conn.prepareStatement(req);
        ps.setInt(1, id);

        ps.executeUpdate();
        System.out.println("Formation supprimée");
    }

    @Override
    public List<formation> afficher() throws SQLException {
        String req = "SELECT * FROM formation";

        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(req);

        List<formation> listeFormations = new ArrayList<formation>();

        while (rs.next()) {
            formation f = new formation();

            f.setId_formation(rs.getInt("id_formation"));
            f.setTitre(rs.getString("titre"));
            f.setOrganisme(rs.getString("organisme"));
            Date dateDebut = rs.getDate("date_debut");
            Date dateFin = rs.getDate("date_fin");
            f.setDate_debut(dateDebut != null ? dateDebut.toLocalDate() : null);
            f.setDate_fin(dateFin != null ? dateFin.toLocalDate() : null);
            f.setLieu(rs.getString("lieu"));
            f.setCapacite(String.valueOf(rs.getInt("capacite")));

            listeFormations.add(f);
        }

        return listeFormations;
    }
}
