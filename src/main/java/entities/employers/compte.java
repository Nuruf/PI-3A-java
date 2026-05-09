package entities.employers;

public class compte {
    private int id;
    private String password;
    private int id_employe;
    public compte() {}
    public compte(String password, int id_employe) {
        this.password = password;
        this.id_employe = id_employe;
    }

    public int getId_employe() {
        return id_employe;
    }

    public void setId_employe(int id_employe) {
        this.id_employe = id_employe;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "compte{" +
                "id=" + id +
                ", password='" + password + '\'' +
                '}';
    }
}
