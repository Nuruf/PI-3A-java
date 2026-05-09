package entities.offres;

public enum TypeContrat {
    CDI("CDI"),
    CDD("CDD"),
    CVP("CVP"),
    STAGE("Stage");

    private final String displayName;

    TypeContrat(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TypeContrat fromDisplayName(String displayName) {
        for (TypeContrat type : TypeContrat.values()) {
            if (type.getDisplayName().equals(displayName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown display name: " + displayName);
    }

}
