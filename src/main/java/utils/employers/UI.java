package utils.employers;

import entities.employers.employe;
import entities.employers.entreprise;
import entities.employers.role;
import entities.employers.statut;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class UI {

    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private static final String[] AVATAR_COLORS = {"#4A5DEF", "#16A34A", "#F59E0B", "#DC2626", "#8B5CF6", "#06B6D4", "#EC4899", "#F97316"};
    public static Label creerBadgeStatut(statut s) {
        Label badge = new Label();
        String baseStyle = "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 12; -fx-background-radius: 15;";
        switch (s) {
            case enattende -> {
                badge.setText("En attente");
                badge.setStyle(baseStyle + "-fx-background-color: -color-warning-muted; -fx-text-fill: -color-warning-fg;");
            }
            case acceptee -> {
                badge.setText("Acceptée");
                badge.setStyle(baseStyle + "-fx-background-color: -color-success-muted; -fx-text-fill: -color-success-fg;");
            }
            case refusee -> {
                badge.setText("Refusée");
                badge.setStyle(baseStyle + "-fx-background-color: -color-danger-muted; -fx-text-fill: -color-danger-fg;");
            }
        }
        return badge;
    }

    public static Label creerBadgeRole(role r) {
        Label badge = new Label(r.getLibelle());
        String base = "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 10; -fx-background-radius: 12;";
        switch (r) {
            case ADMINISTRATEUR_ENTREPRISE -> badge.setStyle(base +
                    "-fx-background-color: -color-accent-muted; -fx-text-fill: -color-accent-fg;");
            case RH -> badge.setStyle(base +
                    "-fx-background-color: -color-danger-muted; -fx-text-fill: -color-danger-fg;");
            case CHEF_PROJET -> badge.setStyle(base +
                    "-fx-background-color: -color-success-muted; -fx-text-fill: -color-success-fg;");
            default -> badge.setStyle(base +
                    "-fx-background-color: -color-bg-subtle; -fx-text-fill: -color-fg-muted; " +
                    "-fx-border-color: -color-border-muted; -fx-border-radius: 12;");
        }
        return badge;
    }
    public static StackPane creerAvatarEmploye(employe emp, double size) {
        StackPane av = new StackPane();
        av.setPrefSize(size, size);
        av.setMinSize(size, size);
        av.setMaxSize(size, size);

        Image image = null;
        double loadSize = size * 2;
        if (emp.hasCustomImage()) {
                File imgFile = new File(emp.getImageProfil());
                if (imgFile.exists()) {
                    image = new Image(imgFile.toURI().toString(), loadSize, loadSize, true, true);
                }

        }

        if (image == null) {
                URL resource = UI.class.getResource(employe.DEFAULT_IMAGE);
                if (resource != null) {
                    image = new Image(resource.toExternalForm(), loadSize, loadSize, true, true);
                }
        }
        if (image != null) {
            ImageView iv = new ImageView(image);
            iv.setFitWidth(size);
            iv.setFitHeight(size);
            iv.setSmooth(true);
            double w = image.getWidth(), h = image.getHeight(), side = Math.min(w, h);
            double x = (w - side) / 2, y = (h - side) / 2;
            iv.setViewport(new javafx.geometry.Rectangle2D(x, y, side, side));
            iv.setPreserveRatio(false);
            iv.setClip(new Circle(size / 2, size / 2, size / 2));
            av.getChildren().add(iv);
            return av;
        }
        String initials = "";
        if (emp.getPrenom() != null && !emp.getPrenom().isEmpty()) initials += emp.getPrenom().charAt(0);
        if (emp.getNom() != null && !emp.getNom().isEmpty()) initials += emp.getNom().charAt(0);

        Label il = new Label(initials.toUpperCase());
        il.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: " + (size * 0.35) + "px;");
        av.setStyle("-fx-background-color: #4A5DEF; -fx-background-radius: " + (size / 2) + ";");
        av.getChildren().add(il);

        return av;
    }

    public static StackPane creerLogoEntreprise(entreprise ent, double size, double radius, double fontSize) {
        StackPane container = new StackPane();
        container.setPrefSize(size, size);
        container.setMinSize(size, size);
        container.setMaxSize(size, size);

        double loadSize = size * 2;

        if (ent.getLogo() != null) {
                File logoFile = new File(ent.getLogo());
                if (logoFile.exists()) {
                    Image image = new Image(logoFile.toURI().toString(), loadSize, loadSize, true, true);

                    ImageView logoView = new ImageView(image);
                    logoView.setFitWidth(size);
                    logoView.setFitHeight(size);
                    logoView.setSmooth(true);

                    double w = image.getWidth(), h = image.getHeight(), side = Math.min(w, h);
                    double x = (w - side) / 2, y = (h - side) / 2;
                    logoView.setViewport(new javafx.geometry.Rectangle2D(x, y, side, side));
                    logoView.setPreserveRatio(false);
                    Rectangle clip = new Rectangle(size, size);
                    clip.setArcWidth(radius * 2);
                    clip.setArcHeight(radius * 2);
                    logoView.setClip(clip);
                    container.setStyle(String.format("-fx-background-color: white; -fx-background-radius: %.0f;-fx-border-color: -color-border-muted; -fx-border-radius: %.0f; -fx-border-width: 1;", radius, radius));
                    container.getChildren().add(logoView);
                    return container;
                }
        }
        String nom = ent.getNom_entreprise();
        int colorIndex = Math.abs(nom.hashCode()) % AVATAR_COLORS.length;
        container.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: %.0f;", AVATAR_COLORS[colorIndex], radius));
        String initials = nom.length() >= 2 ? nom.substring(0, 2).toUpperCase() : nom.substring(0, 1).toUpperCase();
        Label initialsLabel = new Label(initials);
        initialsLabel.setStyle(String.format("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: %.0fpx;", fontSize));
        container.getChildren().add(initialsLabel);

        return container;
    }
    public static Label creerIconLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 11px;");
        return label;
    }

    public static Label creerLabel(String text, double prefWidth, boolean bold) {
        Label label = new Label(text);
        label.setPrefWidth(prefWidth);
        label.setStyle(bold ? "-fx-font-size: 14px; -fx-font-weight: bold;" : "-fx-font-size: 13px;");
        return label;
    }

    public static Region creerSeparateurVertical() {
        Region sep = new Region();
        sep.setPrefWidth(1);
        sep.setPrefHeight(12);
        sep.setStyle("-fx-background-color: -color-border-muted;");
        return sep;
    }
    public static Button creerBouton(String text, String type) {
        Button btn = new Button(text);
        btn.getStyleClass().addAll("button", type, "outline");
        btn.setPrefHeight(32);
        btn.setStyle("-fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 12px;");
        return btn;
    }

    public static Button creerBoutonFooter(String text, String type) {
        Button btn = new Button(text);
        btn.getStyleClass().addAll("button", type, "outline");
        btn.setPrefHeight(35);
        btn.setStyle("-fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 20;");
        return btn;
    }

    public static void desactiverBoutons(Button... buttons) {
        for (Button btn : buttons) {
            btn.setDisable(true);
            btn.getStyleClass().removeAll("success", "danger", "accent");
            btn.setOpacity(0.5);
        }
    }
    public static void afficherErreur(String titre, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public static void afficherSucces(String titre, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public static boolean confirmer(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(titre);
        alert.setHeaderText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public static void afficherMessage(Label label, String message, boolean isError) {
        label.setText(message);
        label.setStyle(isError ? "-fx-text-fill: -color-danger-fg;;" : "-fx-text-fill: -color-success-fg;");
        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(400), label);
            ft.setFromValue(1);
            ft.setToValue(0);
            ft.setOnFinished(ev -> {
                label.setText("");
                label.setOpacity(1);
            });
            ft.play();
        });
        pause.play();
    }
    public static VBox creerMessageVide(String icon, String message, String subMessage) {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(60));

        if (icon != null && !icon.isEmpty()) {
            Label iconLabel = new Label(icon);
            iconLabel.setStyle("-fx-font-size: 36px;");
            box.getChildren().add(iconLabel);
        }

        Label msg = new Label(message);
        msg.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 16px;");

        Label sub = new Label(subMessage);
        sub.setStyle("-fx-text-fill: -color-fg-subtle; -fx-font-size: 13px;");

        box.getChildren().addAll(msg, sub);
        return box;
    }
    public static String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMAT) : "N/A";
    }

    public static String formatTelephone(int telephone) {
        String tel = String.valueOf(telephone);
        if (tel.length() >0) {
            return tel.substring(0, 2) + " " + tel.substring(2, 5) + " " + tel.substring(5);
        }
        return tel;
    }

    public static String pct(long valeur, long total) {
        if (total == 0) return "0%";
        return String.format("%.0f%%", (double) valeur / total * 100);
    }

    public static boolean validerEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    public static void marquerErreur(Control control) {
        control.setStyle("-fx-border-color: #DC2626; -fx-border-width: 1; -fx-border-radius: 3;");
    }

    public static void effacerErreur(Control control) {
        control.setStyle("");
    }
    public static void appliquerStyleCarte(VBox card, boolean selected) {
        card.setStyle(selected ? "-fx-background-color: -color-bg-subtle; -fx-background-radius: 5; " + "-fx-border-color: #4A5DEF; -fx-border-radius: 5; -fx-border-width: 1.5;" : "-fx-background-color: -color-bg-subtle; -fx-background-radius: 5; " + "-fx-border-color: -color-border-muted; -fx-border-radius: 5;");
    }

    public static void appliquerStyleCarteHover(VBox card, boolean hover) {
        card.setStyle(hover ? "-fx-background-color: -color-bg-default; -fx-background-radius: 5; " + "-fx-border-color: -color-accent-muted; -fx-border-radius: 5;" : "-fx-background-color: -color-bg-subtle; -fx-background-radius: 5; " + "-fx-border-color: -color-border-muted; -fx-border-radius: 5;");
    }
}