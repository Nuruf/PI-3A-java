package entities.employers;

public enum role {
    RH("RH"),
    ADMINISTRATEUR_ENTREPRISE("administrateur entreprise"),
    EMPLOYE("employé"),
    CHEF_PROJET("chef projet");

    private final String libelle;


    role(String libelle) {
        this.libelle = libelle;
    }
    public String getLibelle() {
        return libelle;
    }
    public static role fromString(String text) {
        if (text == null) {
            return EMPLOYE; // Valeur par défaut
        }

        for (role r : role.values()) {
            if (r.libelle.equalsIgnoreCase(text)) {
                return r;
            }
        }
        return EMPLOYE;
    }

    @Override
    public String toString() {
        return this.libelle;
    }
}

