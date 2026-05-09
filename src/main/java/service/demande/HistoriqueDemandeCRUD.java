package service.demande;

import entities.demande.HistoriqueDemande;
import utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HistoriqueDemandeCRUD {
    private final Connection conn;

    public HistoriqueDemandeCRUD() {
        conn = MyDB.getInstance().getConn();
    }

    public void ajouter(HistoriqueDemande h) throws SQLException {
        String req = "INSERT INTO historique_demande (id_demande, ancien_statut, nouveau_statut, date_action, acteur, commentaire) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = conn.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, h.getIdDemande());
            pst.setString(2, h.getAncienStatut());
            pst.setString(3, h.getNouveauStatut());
            pst.setTimestamp(4, new Timestamp(h.getDateAction().getTime()));
            pst.setString(5, h.getActeur());
            pst.setString(6, h.getCommentaire());
            pst.executeUpdate();

            ResultSet rs = pst.getGeneratedKeys();
            if (rs.next()) {
                h.setIdHistorique(rs.getInt(1));
            }
        }
    }

    public List<HistoriqueDemande> getByDemande(int idDemande) throws SQLException {
        List<HistoriqueDemande> list = new ArrayList<>();
        String req = "SELECT * FROM historique_demande WHERE id_demande=? ORDER BY date_action DESC, id_historique DESC";
        try (PreparedStatement pst = conn.prepareStatement(req)) {
            pst.setInt(1, idDemande);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                HistoriqueDemande h = new HistoriqueDemande();
                h.setIdHistorique(rs.getInt("id_historique"));
                h.setIdDemande(rs.getInt("id_demande"));
                h.setAncienStatut(rs.getString("ancien_statut"));
                h.setNouveauStatut(rs.getString("nouveau_statut"));
                h.setDateAction(rs.getTimestamp("date_action"));
                h.setActeur(rs.getString("acteur"));
                h.setCommentaire(rs.getString("commentaire"));
                list.add(h);
            }
        }
        return list;
    }

    public void supprimerByDemande(int idDemande) throws SQLException {
        String req = "DELETE FROM historique_demande WHERE id_demande=?";
        try (PreparedStatement pst = conn.prepareStatement(req)) {
            pst.setInt(1, idDemande);
            pst.executeUpdate();
        }
    }
    // Add these methods at the end of your existing HistoriqueDemandeCRUD.java class

    public int countAll() throws SQLException {
        String req = "SELECT COUNT(*) FROM historique_demande";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(req)) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public Map<String, Integer> countGroupByActeur() throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        String req = "SELECT acteur, COUNT(*) as total FROM historique_demande GROUP BY acteur";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                map.put(rs.getString("acteur"), rs.getInt("total"));
            }
        }
        return map;
    }}