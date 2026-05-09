package service.employers;

import entities.employers.compte;
import utils.MyDB;

import java.sql.*;
import java.util.List;

public class compteCRUD  {

    private Connection conn;

    public compteCRUD() throws SQLException {
        conn = MyDB.getInstance().getConn();
    }

    public void ajouter(compte c) throws SQLException {
        String sql = "INSERT INTO compte(mot_de_passe, id_employe) VALUES (?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            String hashed = hachageMotDePasse.hashPassword(c.getPassword());
            ps.setString(1, hashed);
            ps.setInt(2, c.getId_employe());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    c.setId(rs.getInt(1));
                }
            }
        }
    }

    public List<compte> afficher() throws SQLException {
        return List.of();
    }

    public void modifierMotDePasse(int idCompte, String nouveauMotDePasseHashe) throws SQLException {
        String sql = "UPDATE compte SET mot_de_passe = ? WHERE id_compte = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nouveauMotDePasseHashe);
            ps.setInt(2, idCompte);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Aucun compte trouvé avec l'id " + idCompte);
            }
        }
    }
    public void supprimer(int idEmploye) throws SQLException {
        String sql = "DELETE FROM compte WHERE id_employe = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, idEmploye);
        ps.executeUpdate();
    }

    public compte findByEmail(String e_mail) throws SQLException {
        String sql1 = "SELECT c.* FROM compte c JOIN employe e ON c.id_employe = e.id_employe WHERE e.e_mail = ?";
        PreparedStatement ps1 = conn.prepareStatement(sql1);
        ps1.setString(1, e_mail);
        ResultSet rs = ps1.executeQuery();
        if (rs.next()) {
            compte c = new compte();
            c.setId(rs.getInt("id_compte"));
            c.setE_mail(e_mail);
            c.setPassword(rs.getString("mot_de_passe"));
            c.setId_employe(rs.getInt("id_employe"));
            return c;
        }
        return null;
    }

    public compte authentifier(String email, String password) {
        String sql = "SELECT c.*, e.e_mail FROM compte c JOIN employe e ON c.id_employe = e.id_employe WHERE e.e_mail = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("mot_de_passe");
                    String hashedInput = hachageMotDePasse.hashPassword(password);
                    /*System.out.println("[AUTH] stored: " + storedPassword);
                    System.out.println("[AUTH] hashed input: " + hashedInput);
                    System.out.println("[AUTH] plain input: " + password);*/

                    if (storedPassword.equals(hashedInput) || storedPassword.equals(password)) {
                        compte c = new compte();
                        c.setId(rs.getInt("id_compte"));
                        c.setE_mail(email);
                        c.setPassword(storedPassword);
                        c.setId_employe(rs.getInt("id_employe"));
                        return c;
                    }
                } else {
                    System.out.println("[AUTH] No compte found for email: " + email);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    public compte findByEmployeId(int idEmploye) throws SQLException {
        String sql = "SELECT c.*, e.e_mail FROM compte c JOIN employe e ON c.id_employe = e.id_employe WHERE c.id_employe = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, idEmploye);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return new compte(
                    rs.getString("e_mail"),
                    rs.getString("mot_de_passe"),
                    rs.getInt("id_employe")
            );
        }
        return null;
    }

}
