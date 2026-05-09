package entities.employers;

public class compte {
    private int id;
    private String e_mail;
    private String password;
    private int id_employe;
    public compte() {}
    public compte(String e_mail, String password, int id_employe) {
        this.e_mail = e_mail;
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

    public String getE_mail() {
        return e_mail;
    }

    public void setE_mail(String e_mail) {
        this.e_mail = e_mail;
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
                ", e_mail='" + e_mail + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
