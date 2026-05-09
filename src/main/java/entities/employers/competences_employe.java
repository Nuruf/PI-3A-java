package entities.employers;

public class competences_employe {

    private int id;
    private int idEmploye;
    private String skills;
    private String formations;
    private String experience;

    public competences_employe() {
    }

    public competences_employe(int idEmploye, String skills, String formations, String experience) {
        this.idEmploye = idEmploye;
        this.skills = skills;
        this.formations = formations;
        this.experience = experience;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIdEmploye() {
        return idEmploye;
    }

    public void setIdEmploye(int idEmploye) {
        this.idEmploye = idEmploye;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getFormations() {
        return formations;
    }

    public void setFormations(String formations) {
        this.formations = formations;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

}