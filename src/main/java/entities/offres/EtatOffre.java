package entities.offres;

public enum EtatOffre {
    OUVERT("Ouvert"),
    FERME("Fermé");

    private final String displayName;

    EtatOffre(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static EtatOffre fromDisplayName(String displayName) {
        for (EtatOffre etat : EtatOffre.values()) {
            if (etat.getDisplayName().equals(displayName)) {
                return etat;
            }
        }
        throw new IllegalArgumentException("Unknown display name: " + displayName);
    }

}
