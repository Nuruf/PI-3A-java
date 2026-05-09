package controller.employers.admin_sys;

import entities.employers.session;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class adminController implements Initializable {
    @FXML private VBox sidebar;
    @FXML private VBox userCard;
    private boolean isExpanded = false;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadView("homeAdmin");

    }
    @FXML
    private void handleDeconnexion() {
        try {
            session.logout();

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Déconnexion");
            alert.setHeaderText(null);
            alert.setContentText("Vous voulez déconnecter?");
            alert.showAndWait();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/emp/Login.fxml"));
            Parent root = loader.load();
            Stage loginStage = new Stage();
            loginStage.setTitle("Connexion");
            loginStage.setScene(new Scene(root));
            loginStage.show();

            Stage currentStage = (Stage) sidebar.getScene().getWindow();
            currentStage.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void handleToggleSidebar() {
        double endWidth = isExpanded ? 68 : 200;

        Timeline timeline = new Timeline();
        KeyValue widthValue = new KeyValue(sidebar.prefWidthProperty(), endWidth);
        KeyFrame widthFrame = new KeyFrame(Duration.millis(150), widthValue);

        timeline.getKeyFrames().add(widthFrame);

        if (!isExpanded) {
            sidebar.getStyleClass().add("expanded");
            userCard.setVisible(true);
            userCard.setManaged(true);
        } else {
            sidebar.getStyleClass().remove("expanded");
            userCard.setVisible(false);
            userCard.setManaged(false);
        }

        timeline.play();
        isExpanded = !isExpanded;
    }
    @FXML private StackPane contentArea;
    @FXML private Button btnHome,btnDemande;
    private void loadView(String fxmlFileName) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("/emp/admin_sys/" + fxmlFileName + ".fxml"));
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void updateActiveButton(Button activeBtn) {
        btnHome.getStyleClass().remove("nav-active");
        btnDemande.getStyleClass().remove("nav-active");

        activeBtn.getStyleClass().add("nav-active");
    }

    @FXML
    private void showHome(ActionEvent event) {
        loadView("homeAdmin");
        updateActiveButton(btnHome);
    }

    @FXML private void showDemande(ActionEvent event) {
        loadView("demandes_entreprise");
        updateActiveButton(btnDemande);

    }

}