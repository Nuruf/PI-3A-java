package controller.projets;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.Node;

import entities.projet.Tache;
import entities.projet.priority;
import entities.projet.statut_t;

import service.projet.TacheCRUD;
import service.projet.Equipe_projet;
import service.projet.WorkloadBalancingAPI;
import service.projet.WorkloadBalancingAPI.WorkloadAnalysisResult;
import service.projet.WorkloadBalancingAPI.EmployeeWorkload;
import service.projet.OpenAIService;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

public class AjoutTacheController implements Initializable {

    @FXML private Button btnClose;
    @FXML private Button btnCancel;
    @FXML private Button btnSave;
    @FXML private Label modalTitle;

    @FXML private TextField titleField;
    @FXML private TextArea descArea;

    @FXML private ComboBox<statut_t> statusBox;
    @FXML private ComboBox<EmployeeOption> employeeBox;
    @FXML private DatePicker startPicker;
    @FXML private DatePicker duePicker;

    @FXML private ComboBox<priority> priorityBox;
    @FXML private Spinner<Integer> progressSpinner;

    @FXML private Label infoLabel;

    // Workload API components
    @FXML private VBox suggestionBox;
    @FXML private Label suggestionLabel;
    @FXML private Button btnApplySuggestion;
    @FXML private Button btnViewWorkload;

    // AI buttons (will be created dynamically if not in FXML)
    @FXML private Button btnAIDescription;
    @FXML private Button btnAIPriority;

    private int projectId;
    private String projectName = "Projet";
    private Runnable onSaved;
    private Tache tacheToEdit = null; // For edit mode

    // Workload API
    private final WorkloadBalancingAPI workloadAPI = new WorkloadBalancingAPI();
    private WorkloadAnalysisResult currentAnalysis = null;

    // OpenAI Service
    private final OpenAIService openAIService = OpenAIService.getInstance();

    // Debounce timer for re-analyzing workload on text changes
    private Timer suggestionDebounceTimer;

    private final TacheCRUD tacheCRUD = new TacheCRUD();
    private final Equipe_projet groupeProjetCRUD = new Equipe_projet();

    public void setProjectId(int projectId) { this.projectId = projectId; }
    public void setProjectName(String name) { this.projectName = name; }
    public void setOnSaved(Runnable onSaved) { this.onSaved = onSaved; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        statusBox.getItems().setAll(statut_t.values());
        statusBox.setValue(statut_t.A_FAIRE);

        priorityBox.getItems().setAll(priority.values());
        priorityBox.setValue(priority.MOYENNE);

        progressSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 0, 5));
        progressSpinner.setEditable(true);

        startPicker.setValue(LocalDate.now());

        btnClose.setOnAction(this::close);
        btnCancel.setOnAction(this::close);
        btnSave.setOnAction(e -> save());

        // Setup AI buttons if they exist in FXML
        if (btnAIDescription != null) {
            btnAIDescription.setOnAction(e -> generateAIDescription());
        }
        if (btnAIPriority != null) {
            btnAIPriority.setOnAction(e -> suggestAIPriority());
        }

        // Hide info label initially
        if (infoLabel != null) {
            infoLabel.setVisible(false);
            infoLabel.setManaged(false);
        }

        // Re-analyze employee suggestion when title or description changes (debounced)
        titleField.textProperty().addListener((obs, oldVal, newVal) -> debounceSuggestionRefresh());
        descArea.textProperty().addListener((obs, oldVal, newVal) -> debounceSuggestionRefresh());
    }

    /**
     * Debounce re-analysis: waits 500ms after the last keystroke before re-running.
     */
    private void debounceSuggestionRefresh() {
        if (tacheToEdit != null) return; // Skip for edit mode
        if (suggestionDebounceTimer != null) {
            suggestionDebounceTimer.cancel();
        }
        suggestionDebounceTimer = new Timer();
        suggestionDebounceTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> analyzeAndSuggestEmployee());
            }
        }, 500);
    }

    public void loadEmployeesForProject() {
        try {
            List<EmployeeOption> options = groupeProjetCRUD.getEmployeesForProject(projectId);
            employeeBox.getItems().setAll(options);

            // Run workload analysis and show suggestion (only for new tasks)
            if (tacheToEdit == null) {
                analyzeAndSuggestEmployee();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showInfo("Erreur chargement employés du projet.");
        }
    }

    /**
     * Analyze workload and suggest the best employee
     */
    private void analyzeAndSuggestEmployee() {
        try {
            String title = titleField.getText();
            String desc = descArea.getText();
            currentAnalysis = workloadAPI.analyzeProjectWorkload(projectId, title, desc);

            if (currentAnalysis.hasSuggestion()) {
                EmployeeWorkload suggested = currentAnalysis.getSuggestedEmployee();

                // Show suggestion in UI
                showSuggestion(
                        "💡 Suggestion IA: " + suggested.getEmployeeName(),
                        currentAnalysis.getSuggestionReason(),
                        suggested.getEmployeeId()
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Don't show error - suggestion is optional feature
        }
    }

    /**
     * Display the workload suggestion in the UI
     */
    private void showSuggestion(String title, String reason, int suggestedEmployeeId) {
        if (suggestionBox != null) {
            suggestionLabel.setText(title + "\n" + reason);
            suggestionBox.setVisible(true);
            suggestionBox.setManaged(true);

            btnApplySuggestion.setOnAction(e -> {
                // Find and select the suggested employee
                for (EmployeeOption opt : employeeBox.getItems()) {
                    if (opt.id() == suggestedEmployeeId) {
                        employeeBox.setValue(opt);
                        showInfo("✅ Employé suggéré sélectionné!");
                        break;
                    }
                }
            });

            btnViewWorkload.setOnAction(e -> showWorkloadDetails());
        }
    }

    /**
     * Show detailed workload analysis in a popup
     */
    private void showWorkloadDetails() {
        if (currentAnalysis == null) return;

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Analyse de Charge de Travail");
        alert.setHeaderText("📊 Smart Workload Balancing API");

        // Build detailed content
        StringBuilder content = new StringBuilder();
        content.append("Classement des employés (du plus disponible au moins disponible):\n\n");

        int rank = 1;
        for (EmployeeWorkload w : currentAnalysis.getRankedEmployees()) {
            content.append(rank++).append(". ").append(w.getAvailabilityStatus())
                    .append(" ").append(w.getEmployeeName()).append("\n")
                    .append("    • Tâches actives: ").append(w.getTotalActiveTasks()).append("\n")
                    .append("    • Haute priorité: ").append(w.getHighPriorityTasks()).append("\n")
                    .append("    • Tâches urgentes: ").append(w.getUrgentTasks()).append("\n")
                    .append("    • Score charge: ").append(String.format("%.1f", w.getWorkloadScore())).append("\n");
            if (w.getSkillMatchScore() > 0) {
                content.append("    • 🎯 Compétences: ").append(String.format("%.0f%%", w.getSkillMatchScore() * 100));
                if (!w.getMatchedSkills().isEmpty()) {
                    content.append(" (").append(String.join(", ", w.getMatchedSkills())).append(")");
                }
                content.append("\n");
            }
            content.append("\n");
        }

        if (currentAnalysis.hasSuggestion()) {
            content.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            content.append("💡 RECOMMANDATION:\n");
            content.append(currentAnalysis.getSuggestionReason());
        }

        TextArea textArea = new TextArea(content.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefWidth(500);
        textArea.setPrefHeight(400);
        textArea.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px;");

        alert.getDialogPane().setContent(textArea);
        alert.getDialogPane().setPrefWidth(550);
        alert.showAndWait();
    }

    /**
     * Set default status for new task (used by Kanban board when clicking add in a specific column)
     */
    public void setDefaultStatus(statut_t status) {
        if (status != null && statusBox != null) {
            statusBox.setValue(status);
        }
    }

    /**
     * Set default start date for new task (used by Calendar view)
     */
    public void setDefaultStartDate(LocalDate date) {
        if (date != null && startPicker != null) {
            startPicker.setValue(date);
        }
    }

    /**
     * Set default end/due date for new task (used by Calendar view)
     */
    public void setDefaultEndDate(LocalDate date) {
        if (date != null && duePicker != null) {
            duePicker.setValue(date);
        }
    }

    /**
     * Set a task to edit. Call this after loadEmployeesForProject()
     */
    public void setTacheToEdit(Tache t) {
        this.tacheToEdit = t;

        // Update modal title and button
        if (modalTitle != null) {
            modalTitle.setText("✏️ Modifier la tâche");
        }
        btnSave.setText("💾 Enregistrer");

        // Fill form with task data
        titleField.setText(t.getTitre());
        descArea.setText(t.getDescription());
        statusBox.setValue(t.getStatut_tache());
        priorityBox.setValue(t.getPriority_tache());
        progressSpinner.getValueFactory().setValue(t.getProgression());

        if (t.getDate_deb() != null) startPicker.setValue(t.getDate_deb());
        if (t.getDate_limite() != null) duePicker.setValue(t.getDate_limite());

        // Select the assigned employee
        for (EmployeeOption opt : employeeBox.getItems()) {
            if (opt.id() == t.getId_employe()) {
                employeeBox.setValue(opt);
                break;
            }
        }
    }

    private void save() {
        hideInfo();

        String titre = titleField.getText() == null ? "" : titleField.getText().trim();
        if (titre.isBlank()) {
            showInfo("Le titre est obligatoire.");
            return;
        }

        statut_t st = statusBox.getValue();
        if (st == null) {
            showInfo("Veuillez choisir un statut.");
            return;
        }

        EmployeeOption emp = employeeBox.getValue();
        if (emp == null) {
            showInfo("Veuillez choisir un employé assigné.");
            return;
        }

        LocalDate debut = startPicker.getValue();
        if (debut != null && debut.isBefore(LocalDate.now())) {
            showInfo("La date de début ne peut pas être dans le passé.");
            return;
        }
        LocalDate limite = duePicker.getValue();
        if (debut != null && limite != null && limite.isBefore(debut)) {
            showInfo("La date limite doit être >= date début.");
            return;
        }

        // Team validation
        try {
            boolean ok = groupeProjetCRUD.exists(projectId, emp.id());
            if (!ok) {
                showInfo("Cet employé n'appartient pas à l'équipe du projet.");
                return;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            showInfo("Erreur validation équipe projet.");
            return;
        }

        // Build or update entity
        Tache t = tacheToEdit != null ? tacheToEdit : new Tache();
        t.setId_projet(projectId);
        t.setId_employe(emp.id());
        t.setTitre(titre);
        t.setDescription(descArea.getText());
        t.setStatut_tache(st);
        t.setPriority_tache(priorityBox.getValue());
        t.setProgression(progressSpinner.getValue());

        if (debut != null) t.setDate_deb(debut);
        if (limite != null) t.setDate_limite(limite);

        try {
            if (tacheToEdit != null) {
                // UPDATE
                tacheCRUD.modifier(t);
            } else {
                // INSERT
                tacheCRUD.ajouter(t);
            }

            if (onSaved != null) onSaved.run();
            close(null);

        } catch (SQLException ex) {
            ex.printStackTrace();
            showInfo("Erreur : " + ex.getMessage());
        }
    }

    private void showInfo(String msg) {
        if (infoLabel != null) {
            infoLabel.setText(msg);
            infoLabel.setVisible(true);
            infoLabel.setManaged(true);
        }
    }

    private void hideInfo() {
        if (infoLabel != null) {
            infoLabel.setText("");
            infoLabel.setVisible(false);
            infoLabel.setManaged(false);
        }
    }

    private void close(javafx.event.ActionEvent e) {
        Stage s;
        if (e != null) {
            s = (Stage) ((Node) e.getSource()).getScene().getWindow();
        } else {
            s = (Stage) btnCancel.getScene().getWindow();
        }
        s.close();
    }

    // ===================== AI FEATURES =====================

    /**
     * Generate task description using OpenAI
     */
    private void generateAIDescription() {
        String title = titleField.getText();
        if (title == null || title.trim().isEmpty()) {
            showInfo("⚠️ Veuillez d'abord entrer un titre pour la tâche.");
            return;
        }

        if (!openAIService.isConfigured()) {
            showAPIKeyDialog();
            return;
        }

        showInfo("🤖 Génération de la description en cours...");

        // Run in background thread
        new Thread(() -> {
            try {
                String description = openAIService.generateTaskDescription(title.trim(), projectName);
                Platform.runLater(() -> {
                    descArea.setText(description);
                    showInfo("✅ Description générée par l'IA!");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> showInfo("❌ Erreur: " + ex.getMessage()));
            }
        }).start();
    }

    /**
     * Suggest priority using OpenAI
     */
    private void suggestAIPriority() {
        String title = titleField.getText();
        if (title == null || title.trim().isEmpty()) {
            showInfo("⚠️ Veuillez d'abord entrer un titre pour la tâche.");
            return;
        }

        if (!openAIService.isConfigured()) {
            showAPIKeyDialog();
            return;
        }

        showInfo("🤖 Analyse de la priorité en cours...");

        // Run in background thread
        new Thread(() -> {
            try {
                String description = descArea.getText();
                priority suggestedPriority = openAIService.suggestPriority(title.trim(), description);
                Platform.runLater(() -> {
                    priorityBox.setValue(suggestedPriority);
                    String emoji = switch (suggestedPriority) {
                        case HAUTE -> "🔴";
                        case MOYENNE -> "🟡";
                        case BASSE -> "🟢";
                    };
                    showInfo("✅ Priorité suggérée: " + emoji + " " + suggestedPriority);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> showInfo("❌ Erreur: " + ex.getMessage()));
            }
        }).start();
    }

    /**
     * Show dialog to configure API key for Groq
     */
    private void showAPIKeyDialog() {
        Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
        infoAlert.setTitle("Configuration IA - Groq (GRATUIT)");
        infoAlert.setHeaderText("🤖 Fonctionnalités IA - 100% GRATUIT");
        infoAlert.setContentText(OpenAIService.getApiKeyInstructions());
        infoAlert.showAndWait();

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Clé API Groq");
        dialog.setHeaderText("🔑 Entrez votre clé API Groq");
        dialog.setContentText("Clé API:");
        dialog.getEditor().setPromptText("gsk_...");

        dialog.showAndWait().ifPresent(apiKey -> {
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                openAIService.setApiKey(apiKey.trim());
                showInfo("✅ Clé API Groq configurée!");
            }
        });
    }

    // Helper record for employee display in ComboBox
    public record EmployeeOption(int id, String label) {
        @Override public String toString() { return label; }
    }
}
