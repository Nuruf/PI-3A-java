package entities.employers;

public enum statut {
    acceptee("accéptée"),
    refusee("réfusée"),
    enattende("en attende");
    private String libelle;
    private statut(String libelle) {
        this.libelle = libelle;
    }
    public static statut fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return enattende;
        }

        for (statut s : statut.values()) {
            if (s.libelle.equalsIgnoreCase(text.trim())) {
                return s;
            }
        }

        System.err.println("Statut non reconnu: " + text + ", utilisation de EN_ATTENTE par défaut");
        return enattende;
    }
    public String getLibelle() {
        return libelle;
    }
}
