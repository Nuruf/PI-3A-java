package service;

import entities.offres.Candidat;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import utils.MyDB;

public class CandidatCRUD implements InterfaceCRUD<Candidat> {

    Connection conn;
    public CandidatCRUD() {conn = MyDB.getInstance().getConn();}

    public void ajouter(Candidat c) throws SQLException {

        String req = "INSERT INTO candidat (" +
                "code_candidature, " +
                "cv_nom, cv_data, lettre_motivation_nom, lettre_motivation_data, " +
                "etat, note, date_candidature, id_offre, id_visiteur) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = conn.prepareStatement(req);

        ps.setString(1, c.getCodeCandidature());
        ps.setString(2, c.getCvNom());
        ps.setBytes(3, c.getCvData());

        ps.setString(4, c.getLettreMotivationNom());
        ps.setBytes(5, c.getLettreMotivationData());

        ps.setString(6, c.getEtat());
        ps.setString(7, c.getNote());
        ps.setDate(8, c.getDateCandidature());
        ps.setInt(9, c.getIdOffre());

        ps.setInt(10, c.getIdVisiteur());

        ps.executeUpdate();
        System.out.println("Candidat ajouté avec fichiers !");
    }


    @Override
    public void modifier(Candidat c) throws SQLException {
        String req = "UPDATE Candidat SET etat = ?, note = ? WHERE id = ?";
        PreparedStatement ps = conn.prepareStatement(req);
        ps.setString(1, c.getEtat());
        ps.setString(2, c.getNote());
        ps.setInt(3, c.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String req="DELETE FROM candidat WHERE id=?";

        PreparedStatement ps = conn.prepareStatement(req);

        ps.setInt(1, id);

        ps.executeUpdate();
        System.out.println("Candidat supprimé");
    }

    @Override
    public List<Candidat> afficher() throws SQLException {
        String req = "SELECT c.*, v.nom, v.prenom, v.e_mail, v.telephone " +
                "FROM candidat c " +
                "JOIN visiteur v ON c.id_visiteur = v.id_visiteur";

        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(req);

        List<Candidat> listeCandidats = new ArrayList<Candidat>();

        while (rs.next()) {
            Candidat c = new Candidat();
            c.setId(rs.getInt("id"));
            c.setCodeCandidature(rs.getString("code_candidature"));
            c.setEtat(rs.getString("etat"));
            c.setNote(rs.getString("note"));
            c.setDateCandidature(rs.getDate("date_candidature"));
            c.setIdOffre(rs.getInt("id_offre"));
            c.setScore(rs.getDouble("score"));
            c.setVisiteurNom(rs.getString("nom"));
            c.setVisiteurPrenom(rs.getString("prenom"));
            c.setVisiteurEmail(rs.getString("e_mail"));
            c.setVisiteurTelephone(rs.getInt("telephone"));

            c.setCvNom(rs.getString("cv_nom"));
            c.setLettreMotivationNom(rs.getString("lettre_motivation_nom"));

            c.setCvData(rs.getBytes("cv_data"));
            c.setLettreMotivationData(rs.getBytes("lettre_motivation_data"));
            c.setIdVisiteur(rs.getInt("id_visiteur"));
            listeCandidats.add(c);
        }
        return listeCandidats;
    }

    public void updateScore(int id, double score) throws SQLException {
        String req = "UPDATE Candidat SET score = ? WHERE id = ?";
        PreparedStatement ps = conn.prepareStatement(req);
        ps.setDouble(1, score);
        ps.setInt(2, id);
        ps.executeUpdate();
    }
}
