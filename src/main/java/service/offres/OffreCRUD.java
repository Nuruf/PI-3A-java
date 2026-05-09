package service;

import entities.offres.CategorieOffre;
import entities.offres.EtatOffre;
import entities.offres.Offre;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import entities.offres.TypeContrat;
import utils.MyDB;

public class OffreCRUD implements InterfaceCRUD <Offre>{

    Connection conn;

    public OffreCRUD() { conn = MyDB.getInstance().getConn(); }

    @Override
    public void ajouter(Offre o) throws SQLException {
        String req = "INSERT INTO offre (id_employer, titre_poste, type_contrat, date_limite, etat, description, categorie) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = conn.prepareStatement(req);

        ps.setInt(1, o.getIdEmployer());
        ps.setString(2, o.getTitrePoste());
        ps.setString(3, o.getTypeContrat().getDisplayName());
        ps.setDate(4, o.getDateLimite());
        ps.setString(5, o.getEtat().getDisplayName());
        ps.setString(6, o.getDescription());
        ps.setString(7, o.getOffreCategorie().getDisplayName());

        ps.executeUpdate();
        System.out.println("Offre ajoutée !");
    }

    @Override
    public void modifier(Offre o) throws SQLException {
        String req="UPDATE offre SET titre_poste=?,type_contrat=?,date_limite=?,etat=?,description=?,categorie=? WHERE id=?";

        PreparedStatement ps = conn.prepareStatement(req);

        ps.setString(1, o.getTitrePoste());
        ps.setString(2, o.getTypeContrat().getDisplayName());
        ps.setDate(3, o.getDateLimite());
        ps.setString(4, o.getEtat().getDisplayName());
        ps.setString(5, o.getDescription());
        ps.setString(6, o.getOffreCategorie().getDisplayName());

        ps.setInt(7, o.getId());

        ps.executeUpdate();
        System.out.println("Offre modifiée");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String req="DELETE FROM offre WHERE id=?";

        PreparedStatement ps = conn.prepareStatement(req);

        ps.setInt(1, id);

        ps.executeUpdate();
        System.out.println("Offre supprimer");
    }

    @Override
    public List<Offre> afficher() throws SQLException {
        String req = "SELECT * FROM offre";

        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(req);

        List<Offre> listeOffres = new ArrayList<Offre>();

        while (rs.next()) {
            Offre o = new Offre();

            o.setId(rs.getInt("id"));
            o.setIdEmployer(rs.getInt("id_employer"));
            o.setTitrePoste(rs.getString("titre_poste"));
            o.setTypeContrat(TypeContrat.fromDisplayName(rs.getString("type_contrat")));
            o.setDateLimite(rs.getDate("date_limite"));
            o.setEtat(EtatOffre.fromDisplayName(rs.getString("etat")));
            o.setDescription(rs.getString("description"));
            o.setOffreCategorie(CategorieOffre.fromDisplayName(rs.getString("categorie")));

            listeOffres.add(o);
        }

        return listeOffres;
    }

    public Offre getById(int idOffre) throws SQLException {
        String req = "SELECT * FROM offre WHERE id=?";

        try {
            PreparedStatement ps = conn.prepareStatement(req);
            ps.setInt(1, idOffre);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Offre o = new Offre();

                o.setId(rs.getInt("id"));
                o.setIdEmployer(rs.getInt("id_employer"));
                o.setTitrePoste(rs.getString("titre_poste"));
                o.setTypeContrat(TypeContrat.fromDisplayName(rs.getString("type_contrat")));
                o.setDateLimite(rs.getDate("date_limite"));
                o.setEtat(EtatOffre.fromDisplayName(rs.getString("etat")));
                o.setDescription(rs.getString("description"));
                o.setOffreCategorie(CategorieOffre.fromDisplayName(rs.getString("categorie")));

                return o;
            }
        } catch (SQLException e) {
            throw e;
        }

        return null;
    }
}