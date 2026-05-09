package service.employers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import entities.employers.competences_employe;
import entities.employers.compte;
import entities.employers.employe;
import entities.employers.role;
import service.employers.serviceEmail;
import utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class employeCRUD {
    private Connection conn;
    private compteCRUD compteCRUD;
    private competence_employeCRUD competenceCRUD;

    public employeCRUD() {
        try {
            conn = MyDB.getInstance().getConn();
            compteCRUD = new compteCRUD();
            competenceCRUD = new competence_employeCRUD();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public int add(employe employe) throws SQLException {
        String sql = "insert into employe(nom, prenom, e_mail, telephone, poste, role, date_embauche, image_profil, id_entreprise, cv_data, cv_nom) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, employe.getNom());
        ps.setString(2, employe.getPrenom());
        ps.setString(3, employe.getE_mail());
        ps.setInt(4, employe.getTelephone());
        ps.setString(5, employe.getPoste());
        ps.setString(6, employe.getRole().getLibelle());
        if (employe.getDate_embauche() != null) {
            ps.setDate(7, Date.valueOf(employe.getDate_embauche()));
        } else {
            ps.setNull(7, Types.DATE);
        }
        ps.setString(8, employe.DEFAULT_IMAGE);
        ps.setInt(9, employe.getIdEntreprise());
        ps.setBytes(10, employe.getCv_data());
        ps.setString(11, employe.getCv_nom());
        ps.executeUpdate();
        if (employe.hasCv() && employe.getCv_data() != null) {
            extraireCompetencesCV(employe);
        }
        ResultSet rs = ps.getGeneratedKeys();
        rs.next();

        int idEmploye = rs.getInt(1);
        employe.setId_employé(idEmploye);
        String motDePasse = generationMotDePasse.generer();
        compte c = new compte(motDePasse, idEmploye);
        compteCRUD.ajouter(c);
        String sujet = "Création de votre compte employé";
        String corps = "Bonjour " + employe.getPrenom() + ",\n\n"
                + "Un compte a été créé pour vous.\n"
                + "Email : " + employe.getE_mail() + "\n"
                + "Mot de passe : " + motDePasse + "\n\n"
                + "Bonne journée.";

        serviceEmail.envoyer(employe.getE_mail(), sujet, corps);
        return idEmploye;

    }

    public List<employe> afficher(int idEntreprise) throws SQLException {
        String sql = "select * from employe where id_entreprise = ?";
        List<employe> employes = new ArrayList<>();
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, idEntreprise);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            employe e = creerEmployeDepuisResultSet(rs);
            employes.add(e);
        }
        return employes;
    }

    public void modifier(employe employe) throws SQLException {
        // Read previous CV state to decide whether extraction must be re-run.
        employe ancien = getById(employe.getId_employé());

        String sql = "update employe set nom=?, prenom=?, e_mail=?, telephone=?, poste=?, role=?, date_embauche=?, image_profil=?, cv_data=?, cv_nom=? where id_employe=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, employe.getNom());
        ps.setString(2, employe.getPrenom());
        ps.setString(3, employe.getE_mail());
        ps.setInt(4, employe.getTelephone());
        ps.setString(5, employe.getPoste());
        ps.setString(6, employe.getRole().getLibelle());
        if (employe.getDate_embauche() != null) {
            ps.setDate(7, Date.valueOf(employe.getDate_embauche()));
        } else {
            ps.setNull(7, Types.DATE);
        }
        if (employe.getImageProfil() != null && !employe.getImageProfil().isBlank()) {
            ps.setString(8, employe.getImageProfil());
        } else {
            ps.setNull(8, Types.VARCHAR);
        }
        ps.setBytes(9, employe.getCv_data());
        ps.setString(10, employe.getCv_nom());
        ps.setInt(11, employe.getId_employé());
        ps.executeUpdate();

        boolean cvEtaitPresent = ancien != null && ancien.hasCv();
        boolean cvEstPresent = employe.hasCv();
        boolean cvDataChangee = ancien == null || !Arrays.equals(ancien.getCv_data(), employe.getCv_data());
        boolean cvNomChange = ancien == null || !Objects.equals(ancien.getCv_nom(), employe.getCv_nom());
        boolean cvChange = cvDataChangee || cvNomChange;

        if (cvEstPresent && cvChange) {
            // Re-run extraction when a new/updated CV is saved.
            extraireCompetencesCV(employe);
        } else if (cvEtaitPresent && !cvEstPresent) {
            // If CV was removed, remove extracted competences for consistency.
            competenceCRUD.supprimerParEmploye(employe.getId_employé());
        }
    }

    public void supprimer(int id) throws SQLException {
        try {
            conn.setAutoCommit(false);
            compteCRUD.supprimer(id);
            String sql = "delete from employe where id_employe = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private employe creerEmployeDepuisResultSet(ResultSet rs) throws SQLException {
        employe e = new employe();
        e.setId_employé(rs.getInt("id_employe"));
        e.setNom(rs.getString("nom"));
        e.setPrenom(rs.getString("prenom"));
        e.setE_mail(rs.getString("e_mail"));
        e.setTelephone(rs.getInt("telephone"));
        e.setPoste(rs.getString("poste"));
        e.setRole(role.fromString(rs.getString("role")));

        Date dateEmbauche = rs.getDate("date_embauche");
        if (dateEmbauche != null) {
            e.setDate_embauche(dateEmbauche.toLocalDate());
        }
        e.setImageProfil(rs.getString("image_profil"));
        e.setIdEntreprise(rs.getInt("id_entreprise"));
        e.setCv_data(rs.getBytes("cv_data"));
        e.setCv_nom(rs.getString("cv_nom"));

        return e;
    }

    public employe getById(int id) throws SQLException {
        String sql = "select * FROM employe where id_employe = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return creerEmployeDepuisResultSet(rs);
        }
        return null;
    }

    private void extraireCompetencesCV(employe e) {
        competence_employeCRUD crud = this.competenceCRUD;

        Thread thread = new Thread(() -> {
            try {
                System.out.println("═══════════════════════════════════════════════════════════");
                System.out.println("🔍 EXTRACTION CV - Début");
                System.out.println("📄 Employé ID: " + e.getId_employé());
                System.out.println("📄 Employé: " + e.getPrenom() + " " + e.getNom());
                System.out.println("═══════════════════════════════════════════════════════════");

                // Use new API that returns Map<String, Object>
                Map<String, Object> extractionResult = extract_CV_data.extractCVData(e.getCv_data());

                System.out.println("✅ Extraction résultat: " + (extractionResult.containsKey("success") && (boolean) extractionResult.get("success") ? "Succès" : "Erreur"));

                if (!(boolean) extractionResult.getOrDefault("success", false)) {
                    String error = (String) extractionResult.get("error");
                    System.err.println("❌ Erreur d'extraction: " + error);
                    return;
                }

                // Extract data from result map
                Map<String, Object> data = (Map<String, Object>) extractionResult.get("data");
                if (data == null) {
                    System.err.println("❌ Données extraites vides");
                    return;
                }

                List<String> skills = (List<String>) data.getOrDefault("skills", new ArrayList<>());
                List<Map<String, String>> formations = (List<Map<String, String>>) data.getOrDefault("formations", new ArrayList<>());
                List<Map<String, Object>> experience = (List<Map<String, Object>>) data.getOrDefault("experience", new ArrayList<>());

                System.out.println("📊 Données extraites:");
                System.out.println("   - Skills: " + skills.size());
                System.out.println("   - Formations: " + formations.size());
                System.out.println("   - Experience: " + experience.size());

                // Convert to JSON strings
                Gson gson = new Gson();
                String skillsJson = gson.toJson(skills);
                String formationsJson = gson.toJson(formations);
                String experienceJson = gson.toJson(experience);

                System.out.println("💾 Sauvegarde en base de données...");

                // Create/update competences_employe record
                competences_employe comp = new competences_employe(
                        e.getId_employé(),
                        skillsJson,
                        formationsJson,
                        experienceJson
                );

                crud.ajouter(comp);

                System.out.println("✅ Compétences sauvegardées avec succès");
                System.out.println("═══════════════════════════════════════════════════════════");

            } catch (Exception ex) {
                System.err.println("❌ Erreur lors de l'extraction du CV: " + ex.getMessage());
                ex.printStackTrace();
                System.out.println("═══════════════════════════════════════════════════════════");
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private String reparerJSON(String json) {
        if (json == null || json.isEmpty()) return "{}";

        int accolades = 0;
        int crochets = 0;
        boolean dansString = false;
        boolean escape = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escape) {
                escape = false;
                continue;
            }

            if (c == '\\') {
                escape = true;
                continue;
            }

            if (c == '"') {
                dansString = !dansString;
                continue;
            }

            if (!dansString) {
                if (c == '{') accolades++;
                else if (c == '}') accolades--;
                else if (c == '[') crochets++;
                else if (c == ']') crochets--;
            }
        }

        StringBuilder sb = new StringBuilder(json);
        if (dansString) {
            sb.append('"');
        }
        String temp = sb.toString().trim();
        while (temp.endsWith(",") || temp.endsWith(":")) {
            temp = temp.substring(0, temp.length() - 1).trim();
        }
        if (temp.matches(".*,\\s*\"[^\"]*\"\\s*$")) {
            temp = temp.substring(0, temp.lastIndexOf(",")).trim();
        }

        sb = new StringBuilder(temp);
        accolades = 0;
        crochets = 0;
        dansString = false;
        escape = false;

        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                continue;
            }
            if (c == '"') {
                dansString = !dansString;
                continue;
            }
            if (!dansString) {
                if (c == '{') accolades++;
                else if (c == '}') accolades--;
                else if (c == '[') crochets++;
                else if (c == ']') crochets--;
            }
        }
        for (int i = 0; i < crochets; i++) {
            sb.append(']');
        }
        for (int i = 0; i < accolades; i++) {
            sb.append('}');
        }

        return sb.toString();
    }

    public record EmployeeInfo(int id, String nom, String prenom, String role) {
        public String getFullName() {
            return nom + " " + prenom;
        }

        @Override
        public String toString() {
            return nom + " " + prenom;
        }
    }
    public EmployeeInfo getEmployeeInfoByEmail(String email) throws SQLException {
        String sql = "SELECT id_employe, nom, prenom, role FROM employe WHERE e_mail = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new EmployeeInfo(
                            rs.getInt("id_employe"),
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            rs.getString("role")
                    );
                }
            }
        }
        return null;
    }

    /**
     * Get all employees for a specific company
     */
    public List<EmployeeInfo> getAllEmployees(int idEntreprise) throws SQLException {
        String sql = "SELECT id_employe, nom, prenom, role FROM employe WHERE id_entreprise = ?";
        List<EmployeeInfo> employees = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEntreprise);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    employees.add(new EmployeeInfo(
                            rs.getInt("id_employe"),
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            rs.getString("role")
                    ));
                }
            }
        }
        return employees;
    }

    /**
     * Get all employees (no filter - kept for backward compatibility)
     */
    public List<EmployeeInfo> getAllEmployees() throws SQLException {
        String sql = "SELECT id_employe, nom, prenom, role FROM employe";
        List<EmployeeInfo> employees = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                employees.add(new EmployeeInfo(
                        rs.getInt("id_employe"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("role")
                ));
            }
        }
        return employees;
    }

    /**
     * Get employees by role for a specific company
     */
    public List<EmployeeInfo> getEmployeesByRole(String role, int idEntreprise) throws SQLException {
        String sql = "SELECT id_employe, nom, prenom, role FROM employe WHERE role = ? AND id_entreprise = ?";
        List<EmployeeInfo> employees = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role);
            ps.setInt(2, idEntreprise);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    employees.add(new EmployeeInfo(
                            rs.getInt("id_employe"),
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            rs.getString("role")
                    ));
                }
            }
        }
        return employees;
    }

    /**
     * Get employees by role
     */
    public List<EmployeeInfo> getEmployeesByRole(String role) throws SQLException {
        String sql = "SELECT id_employe, nom, prenom, role FROM employe WHERE role = ?";
        List<EmployeeInfo> employees = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    employees.add(new EmployeeInfo(
                            rs.getInt("id_employe"),
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            rs.getString("role")
                    ));
                }
            }
        }
        return employees;
    }

    /**
     * Get employees with chef projet role (responsables) for a specific company
     */
    public List<EmployeeInfo> getResponsables(int idEntreprise) throws SQLException {
        return getEmployeesByRole("chef projet", idEntreprise);
    }

    /**
     * Get employees with chef projet role (responsables)
     */
    public List<EmployeeInfo> getResponsables() throws SQLException {
        return getEmployeesByRole("chef projet");
    }
    public employe findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM employe WHERE e_mail = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return creerEmployeDepuisResultSet(rs);
        }
        return null;
    }
}