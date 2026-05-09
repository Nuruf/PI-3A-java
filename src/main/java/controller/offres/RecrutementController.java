package controller.offres;

import javafx.fxml.FXML;

public class RecrutementController {
    @FXML
    private CandidatController candidatTabController;

    @FXML
    private void refreshCandidatTab() {
        if (candidatTabController != null) {
            candidatTabController.loadOffresIntoCombo();
        }
    }
}