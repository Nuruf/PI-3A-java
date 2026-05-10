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

    public String getStorageValue() {
        return name();
    }

    public static CategorieOffre fromDisplayName(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        for (CategorieOffre categorie : CategorieOffre.values()) {
            if (categorie.name().equalsIgnoreCase(normalized) || categorie.getDisplayName().equalsIgnoreCase(normalized)) {
                return categorie;
            }
        }
        throw new IllegalArgumentException("Unknown display name: " + value);
    }
}
