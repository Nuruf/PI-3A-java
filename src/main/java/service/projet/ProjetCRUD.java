package service.projet;

import entities.projet.Projet;
import entities.projet.priority;
import entities.projet.statut;
import service.InterfaceCRUD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProjetCRUD implements InterfaceCRUD<Projet> {
    private Connection connection;

    public ProjetCRUD() {
        connection = utils.MyDB.getInstance().getConn();
    }

    @Override
    public void ajouter(Projet projet) throws SQLException {
        int generatedId = ajouterAndGetId(projet);
        projet.setProjet_id(generatedId);
    }

    /**
     * Insert a project and return its generated ID
     */
    public int ajouterAndGetId(Projet projet) throws SQLException {
        String sql = "INSERT INTO projet(responsable_id, nom, description, date_debut, date_fin_prevue, date_fin_reelle, statut, priorite) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        ps.setInt(1, projet.getResponsable_id());
        ps.setString(2, projet.getNom());
        ps.setString(3, projet.getDescription());

        ps.setDate(4, Date.valueOf(projet.getDate_debut()));

        if (projet.getDate_fin_prevue() != null) {
            ps.setDate(5, Date.valueOf(projet.getDate_fin_prevue()));
        } else {
            ps.setNull(5, Types.DATE);
        }

        if (projet.getDate_fin_reelle() == null) ps.setNull(6, Types.DATE);
        else ps.setDate(6, Date.valueOf(projet.getDate_fin_reelle()));

        ps.setString(7, projet.getStatut().name());
        ps.setString(8, projet.getPriority().name());

        ps.executeUpdate();

        // Get generated ID
        try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                return generatedKeys.getInt(1);
            } else {
                throw new SQLException("Creating project failed, no ID obtained.");
            }
        }
    }

    @Override
    public void modifier(Projet projet) throws SQLException {
        // Code pour modifier un projet dans la base de données
        String sql = "UPDATE projet SET responsable_id = ?, nom = ?, description = ?, date_debut = ?, date_fin_prevue = ?, date_fin_reelle = ?, statut = ?, priorite = ? WHERE id_projet = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, projet.getResponsable_id());
        ps.setString(2, projet.getNom());
        ps.setString(3, projet.getDescription());
        ps.setDate(4, Date.valueOf(projet.getDate_debut()));
        ps.setDate(5, Date.valueOf(projet.getDate_fin_prevue()));
        if (projet.getDate_fin_reelle() == null) ps.setNull(6, Types.DATE);
        else ps.setDate(6, Date.valueOf(projet.getDate_fin_reelle()));
        ps.setString(7, projet.getStatut().name());
        ps.setString(8, projet.getPriority().name());
        ps.setInt(9, projet.getProjet_id());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        // Code pour supprimer un projet de la base de données en utilisant son ID
        String sql = "DELETE FROM projet WHERE id_projet = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Projet> afficher() throws SQLException {
        String sql = "SELECT * FROM projet";
        Statement ps = connection.createStatement();
        ResultSet rs = ps.executeQuery(sql);
        List<Projet> listeProjet = new ArrayList<>();
        while (rs.next()) {
            Projet projet = new Projet();
            projet.setProjet_id(rs.getInt("id_projet"));
            projet.setResponsable_id(rs.getInt("responsable_id"));
            projet.setNom(rs.getString("nom"));
            projet.setDescription(rs.getString("description"));
            projet.setDate_debut(rs.getDate("date_debut").toLocalDate());
            projet.setDate_fin_prevue(rs.getDate("date_fin_prevue").toLocalDate());
            Date dateFinReelle = rs.getDate("date_fin_reelle");
            if (dateFinReelle != null) {
                projet.setDate_fin_reelle(dateFinReelle.toLocalDate());
            }
            projet.setStatut(statut.valueOf(rs.getString("statut")));
            projet.setPriority(priority.valueOf(rs.getString("priorite")));
            listeProjet.add(projet);
        }
        return listeProjet;
    }
}
