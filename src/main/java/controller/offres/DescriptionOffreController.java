package controller.offres;

import javafx.fxml.FXML;
import javafx.scene.web.HTMLEditor;
import javafx.stage.Stage;

public class DescriptionOffreController {

    @FXML private HTMLEditor htmlEditor;
    private String savedHtml = "";
    private boolean saveClicked = false;

    public void setInitialText(String text) {
        htmlEditor.setHtmlText(text);
    }

    public String getHtmlText() {
        return savedHtml;
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    @FXML
    private void handleSave() {
        this.savedHtml = htmlEditor.getHtmlText();
        this.saveClicked = true;

        Stage stage = (Stage) htmlEditor.getScene().getWindow();
        stage.close();
    }
}