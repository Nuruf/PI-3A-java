package controller.formations;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import entities.formation.formation;
import service.formation.formationCRUD;

public class EditFormationController {

    @FXML
    private TextField txtTitre;
    @FXML
    private TextField txtOrganisme;
    @FXML
    private DatePicker dpDateDebut;
    @FXML
    private DatePicker dpDateFin;
    @FXML
    private TextField txtLieu;
    @FXML
    private TextField txtCapacite;
    @FXML
    private Button btnSave;
    @FXML
    private Button btnCancel;

    private final formationCRUD formationService = new formationCRUD();
    private Runnable onSaved;
    private int editingId;

    public void setFormation(formation f, Runnable onSaved) {
        this.onSaved = onSaved;
        this.editingId = f.getId_formation();

        txtTitre.setText(safeText(f.getTitre()));
        txtOrganisme.setText(safeText(f.getOrganisme()));
        dpDateDebut.setValue(f.getDate_debut());
        dpDateFin.setValue(f.getDate_fin());
        txtLieu.setText(safeText(f.getLieu()));
        txtCapacite.setText(safeText(f.getCapacite()));
    }

    @FXML
    private void handleSave() {
        if (!isFormValid()) {
            showAlert(AlertType.WARNING, "Champs requis", "Veuillez remplir le titre, l'organisme et les dates.");
            return;
        }

        formation f = new formation(
                txtTitre.getText().trim(),
                txtOrganisme.getText().trim(),
                dpDateDebut.getValue(),
                dpDateFin.getValue(),
                txtLieu.getText().trim(),
                txtCapacite.getText().trim()
        );
        f.setId_formation(editingId);

        try {
            formationService.modifier(f);
            showAlert(AlertType.INFORMATION, "Succes", "Formation modifiee avec succes.");
            if (onSaved != null) {
                onSaved.run();
            }
            closeWindow();
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erreur", "Echec de la modification: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) btnSave.getScene().getWindow();
        stage.close();
    }

    private boolean isFormValid() {
        return txtTitre != null && !txtTitre.getText().trim().isEmpty()
                && txtOrganisme != null && !txtOrganisme.getText().trim().isEmpty()
                && dpDateDebut != null && dpDateDebut.getValue() != null
                && dpDateFin != null && dpDateFin.getValue() != null;
    }

    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }
}

