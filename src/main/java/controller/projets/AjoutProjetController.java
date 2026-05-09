package controller.projets;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.stage.Stage;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import service.projet.ProjetCRUD;
import service.employers.employeCRUD;
import service.projet.Equipe_projet;
import service.employers.employeCRUD.EmployeeInfo;
import entities.employers.session;
import entities.projet.Projet;
import entities.projet.statut;
import entities.projet.priority;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AjoutProjetController {

    @FXML private Label formTitle;
    @FXML private TextField nomField;
    @FXML private TextArea descArea;
    @FXML private DatePicker debutPicker;
    @FXML private DatePicker finPrevuePicker;
    @FXML private ComboBox<statut> statutBox;
    @FXML private ComboBox<priority> prioriteBox;
    @FXML private ComboBox<EmployeeInfo> responsableBox;
    @FXML private Label errorLabel;

    // Team selection
    @FXML private TextField empSearchField;
    @FXML private Button btnSelectAll;
    @FXML private Button btnClearAll;
    @FXML private ListView<EmployeeInfo> teamListView;
    @FXML private Label infoLabel;

    private final ProjetCRUD projetCRUD = new ProjetCRUD();
    private final employeCRUD employeeCRUD = new employeCRUD();
    private final Equipe_projet equipeCRUD = new Equipe_projet();

    private Projet projetToEdit = null;
    private boolean saved = false;

    // Track selected employees with checkboxes
    private final ObservableList<EmployeeInfo> allEmployees = FXCollections.observableArrayList();
    private final ObservableList<EmployeeInfo> responsables = FXCollections.observableArrayList();
    private final ObservableList<EmployeeInfo> filteredEmployees = FXCollections.observableArrayList();
    private final Map<Integer, BooleanProperty> selectionMap = new HashMap<>();

    public boolean isSaved() {
        return saved;
    }

    @FXML
    public void initialize() {
        // Fill enums
        statutBox.getItems().setAll(statut.values());
        prioriteBox.getItems().setAll(priority.values());

        statutBox.getSelectionModel().select(statut.PLANIFIE);
        prioriteBox.getSelectionModel().select(priority.MOYENNE);

        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // Load employees from database
        loadEmployees();

        // Setup team list with checkboxes
        setupTeamListView();

        // Search filter
        empSearchField.textProperty().addListener((obs, old, newVal) -> filterEmployees(newVal));

        // Select all / Clear all buttons
        btnSelectAll.setOnAction(e -> selectAllEmployees(true));
        btnClearAll.setOnAction(e -> selectAllEmployees(false));

        updateInfoLabel();
    }

    private void loadEmployees() {
        try {
            int idEntreprise = session.getIdEntreprise();
            List<EmployeeInfo> employees = employeeCRUD.getAllEmployees(idEntreprise);
            allEmployees.setAll(employees);
            filteredEmployees.setAll(employees);

            // Initialize selection map (all unselected by default)
            for (EmployeeInfo emp : employees) {
                BooleanProperty selected = new SimpleBooleanProperty(false);
                selected.addListener((obs, old, newVal) -> updateInfoLabel());
                selectionMap.put(emp.id(), selected);
            }

            // Load only responsables for the responsable dropdown
            List<EmployeeInfo> responsablesList = employeeCRUD.getResponsables(idEntreprise);
            responsables.setAll(responsablesList);
            responsableBox.setItems(FXCollections.observableArrayList(responsablesList));
            if (!responsablesList.isEmpty()) {
                responsableBox.getSelectionModel().selectFirst();
                // Auto-select the first responsable to be part of the team
                autoSelectResponsable(responsablesList.get(0));
            }

            // Add listener to auto-select responsable when changed
            responsableBox.valueProperty().addListener((obs, oldResp, newResp) -> {
                // Auto-select new responsable to be part of the team
                if (newResp != null) {
                    autoSelectResponsable(newResp);
                }
            });

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur chargement employés: " + e.getMessage());
        }
    }

    private void autoSelectResponsable(EmployeeInfo responsable) {
        if (responsable != null) {
            BooleanProperty prop = selectionMap.get(responsable.id());
            if (prop != null && !prop.get()) {
                prop.set(true);
            }
        }
    }

    private void setupTeamListView() {
        teamListView.setItems(filteredEmployees);

        // Use CheckBoxListCell for selection
        teamListView.setCellFactory(CheckBoxListCell.forListView(emp -> selectionMap.get(emp.id())));
    }

    private void filterEmployees(String query) {
        if (query == null || query.isBlank()) {
            filteredEmployees.setAll(allEmployees);
        } else {
            String q = query.toLowerCase().trim();
            List<EmployeeInfo> filtered = allEmployees.stream()
                    .filter(e -> e.getFullName().toLowerCase().contains(q))
                    .collect(Collectors.toList());
            filteredEmployees.setAll(filtered);
        }
    }

    private void selectAllEmployees(boolean select) {
        for (EmployeeInfo emp : filteredEmployees) {
            BooleanProperty prop = selectionMap.get(emp.id());
            if (prop != null) {
                prop.set(select);
            }
        }
    }

    private List<Integer> getSelectedEmployeeIds() {
        List<Integer> selected = new ArrayList<>();
        for (Map.Entry<Integer, BooleanProperty> entry : selectionMap.entrySet()) {
            if (entry.getValue().get()) {
                selected.add(entry.getKey());
            }
        }
        return selected;
    }

    private void updateInfoLabel() {
        int count = (int) selectionMap.values().stream().filter(BooleanProperty::get).count();
        if (infoLabel != null) {
            infoLabel.setText(count + " employé(s) sélectionné(s)");
        }
    }

    @FXML
    private void onCancel() {
        close();
    }

    @FXML
    private void onSave() {
        hideError();

        String nom = nomField.getText();
        if (nom == null || nom.isBlank()) {
            showError("Le nom du projet est obligatoire.");
            return;
        }
        String desc = descArea.getText();
        if (desc == null || desc.isBlank()) {
            showError("La description du projet est obligatoire.");
            return;
        }

        EmployeeInfo responsable = responsableBox.getValue();
        if (responsable == null) {
            showError("Veuillez sélectionner un responsable.");
            return;
        }

        LocalDate debut = debutPicker.getValue();
        if (debut == null) {
            showError("La date de début est obligatoire.");
            return;
        }
        if (debut.isBefore(LocalDate.now())) {
            showError("La date de début doit être aujourd'hui ou dans le futur.");
            return;
        }

        LocalDate finPrevue = finPrevuePicker.getValue();
        if (finPrevue != null && finPrevue.isBefore(debut)) {
            showError("La date fin prévue doit être après la date début.");
            return;
        }

        List<Integer> selectedEmployees = getSelectedEmployeeIds();

        // Ensure the responsable is always part of the team
        if (!selectedEmployees.contains(responsable.id())) {
            selectedEmployees.add(responsable.id());
        }

        if (selectedEmployees.isEmpty()) {
            showError("Veuillez sélectionner au moins un employé pour l'équipe.");
            return;
        }

        statut st = statutBox.getValue() == null ? statut.PLANIFIE : statutBox.getValue();
        priority pr = prioriteBox.getValue() == null ? priority.MOYENNE : prioriteBox.getValue();

        Projet p = new Projet(
                responsable.id(),
                nom.trim(),
                desc,
                debut,
                finPrevue,
                null,
                st,
                pr
        );

        try {
            if (projetToEdit == null) {
                // INSERT new project
                int projectId = projetCRUD.ajouterAndGetId(p);
                p.setProjet_id(projectId);

                // Add selected employees to project team
                equipeCRUD.addEmployeesToProject(projectId, selectedEmployees);

            } else {
                // UPDATE existing project
                p.setProjet_id(projetToEdit.getProjet_id());
                projetCRUD.modifier(p);

                // Update team: clear old and add new selection
                equipeCRUD.clearProjectTeam(projetToEdit.getProjet_id());
                equipeCRUD.addEmployeesToProject(projetToEdit.getProjet_id(), selectedEmployees);
            }

            saved = true;
            close();

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur SQL: " + e.getMessage());
        }
    }

    public void setProjetToEdit(Projet p) {
        this.projetToEdit = p;

        // Change title for edit mode
        if (formTitle != null) {
            formTitle.setText("✏️ Modifier le Projet");
        }

        nomField.setText(p.getNom());
        descArea.setText(p.getDescription());

        if (p.getDate_debut() != null) debutPicker.setValue(p.getDate_debut());
        if (p.getDate_fin_prevue() != null) finPrevuePicker.setValue(p.getDate_fin_prevue());

        statutBox.setValue(p.getStatut());
        prioriteBox.setValue(p.getPriority());

        // Set responsable (search in responsables list)
        for (EmployeeInfo emp : responsables) {
            if (emp.id() == p.getResponsable_id()) {
                responsableBox.setValue(emp);
                break;
            }
        }

        // Load existing team members
        try {
            List<Integer> teamIds = equipeCRUD.getEmployeeIdsForProject(p.getProjet_id());
            for (Integer empId : teamIds) {
                BooleanProperty prop = selectionMap.get(empId);
                if (prop != null) {
                    prop.set(true);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        updateInfoLabel();
    }

    private void close() {
        Stage stage = (Stage) nomField.getScene().getWindow();
        stage.close();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setStyle("-fx-text-fill: red;");
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private void hideError() {
        errorLabel.setText("");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
    }
}
