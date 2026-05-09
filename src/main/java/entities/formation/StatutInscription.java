package entities.formation;

public enum StatutInscription {
    EN_ATTENTE("En attente"),
    ACCEPTEE("Acceptée"),
    REFUSEE("Refusée");

    private final String displayName;

    StatutInscription(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
