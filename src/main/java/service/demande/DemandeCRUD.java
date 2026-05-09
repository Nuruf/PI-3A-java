package service.demande;

import entities.demande.Demande;
import utils.MyDB;

import java.sql.*;
import java.util.*;

public class DemandeCRUD {

    private Connection connection;

    public DemandeCRUD() {
        connection = MyDB.getInstance().getConn();
    }

    public void ajouter(Demande demande) throws SQLException {
        String sql = "INSERT INTO demande (id_employe, categorie, titre, "
                + "description, priorite, status, date_creation, type_demande) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS);
        ps.setInt(1, demande.getIdEmploye());
        ps.setString(2, demande.getCategorie());
        ps.setString(3, demande.getTitre());
        ps.setString(4, demande.getDescription());
        ps.setString(5, demande.getPriorite());
        ps.setString(6, demande.getStatus());
        ps.setDate(7, new java.sql.Date(
                demande.getDateCreation().getTime()));
        ps.setString(8, demande.getTypeDemande());
        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            demande.setIdDemande(rs.getInt(1));
        }
    }

    public void modifier(Demande demande) throws SQLException {
        String sql = "UPDATE demande SET categorie=?, titre=?, "
                + "description=?, priorite=?, status=?, "
                + "date_creation=?, type_demande=? WHERE id_demande=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, demande.getCategorie());
        ps.setString(2, demande.getTitre());
        ps.setString(3, demande.getDescription());
        ps.setString(4, demande.getPriorite());
        ps.setString(5, demande.getStatus());
        ps.setDate(6, new java.sql.Date(
                demande.getDateCreation().getTime()));
        ps.setString(7, demande.getTypeDemande());
        ps.setInt(8, demande.getIdDemande());
        ps.executeUpdate();
    }

    public void supprimer(int idDemande) throws SQLException {
        String sql = "DELETE FROM demande WHERE id_demande=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, idDemande);
        ps.executeUpdate();
    }

    public List<Demande> afficher() throws SQLException {
        List<Demande> list = new ArrayList<>();
        String sql = "SELECT * FROM demande ORDER BY date_creation DESC";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            list.add(extractDemande(rs));
        }
        return list;
    }

    public List<Demande> getByEmploye(int idEmploye) throws SQLException {
        List<Demande> list = new ArrayList<>();
        String sql = "SELECT * FROM demande WHERE id_employe=? "
                + "ORDER BY date_creation DESC";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, idEmploye);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(extractDemande(rs));
        }
        return list;
    }

    // ============ STATISTICS ============

    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM demande";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);
        return rs.next() ? rs.getInt(1) : 0;
    }

    public int countByStatus(String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM demande WHERE status=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, status);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getInt(1) : 0;
    }

    public Map<String, Integer> countGroupByStatus() throws SQLException {
        return countGroupBy("status");
    }

    public Map<String, Integer> countGroupByPriorite() throws SQLException {
        return countGroupBy("priorite");
    }

    public Map<String, Integer> countGroupByType() throws SQLException {
        return countGroupBy("type_demande");
    }

    public Map<String, Integer> countGroupByCategorie() throws SQLException {
        return countGroupBy("categorie");
    }

    private Map<String, Integer> countGroupBy(String column)
            throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT " + column + ", COUNT(*) as cnt "
                + "FROM demande GROUP BY " + column;
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            map.put(rs.getString(1), rs.getInt("cnt"));
        }
        return map;
    }

    private Demande extractDemande(ResultSet rs) throws SQLException {
        Demande d = new Demande();
        d.setIdDemande(rs.getInt("id_demande"));
        d.setIdEmploye(rs.getInt("id_employe"));
        d.setCategorie(rs.getString("categorie"));
        d.setTitre(rs.getString("titre"));
        d.setDescription(rs.getString("description"));
        d.setPriorite(rs.getString("priorite"));
        d.setStatus(rs.getString("status"));
        d.setDateCreation(rs.getDate("date_creation"));
        d.setTypeDemande(rs.getString("type_demande"));
        return d;
    }
}