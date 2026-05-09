package utils;

import javafx.scene.control.Label;

public class BadgeFactory {

    public static Label createBadge(String status) {
        if (status == null) status = "Inconnu";

        Label badge = new Label(status.toUpperCase());

        // Add the base style class
        badge.getStyleClass().add("badge");

        // Add the specific color class based on status
        switch (status.toLowerCase()) {
            case "présélectionné":
            case "ouvert":
                badge.getStyleClass().add("badge-accent");
                break;

            case "accepté":
            case "publiée":
                badge.getStyleClass().add("badge-success");
                break;

            case "refusé":
            case "fermé":
            case "annulée":
                badge.getStyleClass().add("badge-danger");
                break;

            case "entretien":
                badge.getStyleClass().add("badge-info");
                break;

            case "en attente":
            case "en cours":
            default:
                badge.getStyleClass().add("badge-neutral");
                break;
        }

        return badge;
    }
}