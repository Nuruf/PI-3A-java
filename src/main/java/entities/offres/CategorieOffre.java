package entities.offres;

public enum CategorieOffre {
    INFORMATIQUE("Informatique"),
    MARKETING("Marketing"),
    VENTE("Vente"),
    FINANCE("Finance"),
    RH("Ressources Humaines"),
    SANTE("Santé"),
    EDUCATION("Education"),
    ART("Art et Design"),
    AUTRE("Autre");

    private final String displayName;

    CategorieOffre(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static CategorieOffre fromDisplayName(String displayName) {
        for (CategorieOffre categorie : CategorieOffre.values()) {
            if (categorie.getDisplayName().equals(displayName)) {
                return categorie;
            }
        }
        throw new IllegalArgumentException("Unknown display name: " + displayName);
    }
}
