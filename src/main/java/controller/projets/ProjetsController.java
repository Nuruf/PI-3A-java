package controller.projets;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

// ======= ADAPT THESE IMPORTS =======
import service.projet.ProjetCRUD;
import service.projet.TacheCRUD;
import service.employers.employeCRUD;
import service.projet.Equipe_projet;
import service.employers.employeCRUD.EmployeeInfo;
import service.projet.GoogleCalendarService;
import service.projet.OpenAIService;
import entities.projet.Projet;
import entities.projet.Tache;
import entities.projet.statut;
import entities.projet.statut_t;
import entities.projet.priority;
import entities.employers.session;
import entities.employers.employe;

public class ProjetsController {

    // LEFT
    @FXML private TextField searchField;
    @FXML private ComboBox<statut> statusFilter;
    @FXML private ComboBox<String> sortBox;
    @FXML private Button btnNewProject;
    @FXML private Button btnRefresh;
    @FXML private ListView<Projet> projectsList;
    @FXML private Label projectsCountLabel;
    @FXML private Label activeCountLabel;
    @FXML private Label emptyStateLabel;

    // RIGHT (details panel)
    @FXML private TextField detailNomField;
    @FXML private TextArea detailDescArea;
    @FXML private DatePicker detailDebutPicker;
    @FXML private DatePicker detailFinPrevuePicker;
    @FXML private ComboBox<statut> detailStatutBox;
    @FXML private ComboBox<priority> detailPrioriteBox;
    @FXML private ComboBox<EmployeeInfo> detailResponsableBox;
    @FXML private Button btnSaveDetails;
    @FXML private Button btnDeleteProject;
    @FXML private Label detailsInfoLabel;

    // Team management in details panel
    @FXML private Label teamCountLabel;
    @FXML private ComboBox<EmployeeInfo> addEmployeeBox;
    @FXML private Button btnAddEmployee;
    @FXML private ListView<EmployeeInfo> detailTeamListView;

    // TASKS TAB - Kanban Board
    @FXML private Label selectedProjectTitle;
    @FXML private Label kanbanSubtitle;
    @FXML private TextField taskSearchField;
    @FXML private Button btnRefreshTasks;
    @FXML private Button btnCreateTaskInline;
    @FXML private ComboBox<String> taskSortBox;

    // Kanban Board containers
    @FXML private HBox kanbanBoard;
    @FXML private VBox columnTodo;
    @FXML private VBox columnInProgress;
    @FXML private VBox columnBlocked;
    @FXML private VBox columnDone;
    @FXML private VBox todoTasksContainer;
    @FXML private VBox inProgressTasksContainer;
    @FXML private VBox blockedTasksContainer;
    @FXML private VBox doneTasksContainer;
    @FXML private HBox todoAddCard;
    @FXML private HBox inProgressAddCard;
    @FXML private HBox blockedAddCard;

    // Kanban count labels
    @FXML private Label todoCount;
    @FXML private Label inProgressCount;
    @FXML private Label blockedCount;
    @FXML private Label doneCount;

    // Task stats labels
    @FXML private Label taskTotalLabel;
    @FXML private Label taskCompletedLabel;
    @FXML private Label taskInProgressLabel;


    @FXML private Label taskInfoLabel;

    // User info bar
    @FXML private Label currentUserLabel;
    @FXML private Label currentRoleLabel;
    @FXML private Button btnLogout;

    // Google Calendar
    @FXML private Button btnGoogleCalendar;
    @FXML private Label calendarStatusLabel;

    // Calendar Tab
    @FXML private Label calendarTitle;
    @FXML private Label calendarSubtitle;
    @FXML private Button btnPrevMonth;
    @FXML private Button btnNextMonth;
    @FXML private Button btnToday;
    @FXML private Label currentMonthLabel;
    @FXML private ComboBox<Projet> calendarProjectFilter;
    @FXML private VBox calendarContainer;
    @FXML private HBox calendarDaysHeader;
    @FXML private GridPane calendarGrid;
    @FXML private Label calendarInfoLabel;

    // Calendar state
    private YearMonth currentYearMonth = YearMonth.now();
    private Projet calendarSelectedProject = null;


    private final ProjetCRUD projetCRUD = new ProjetCRUD();
    private final TacheCRUD tacheCRUD = new TacheCRUD();
    private final employeCRUD employeCRUD = new employeCRUD();
    private final Equipe_projet equipeCRUD = new Equipe_projet();
    private final GoogleCalendarService calendarService = GoogleCalendarService.getInstance();
    private final OpenAIService openAIService = OpenAIService.getInstance();

    private final ObservableList<Projet> masterProjects = FXCollections.observableArrayList();
    private final ObservableList<Projet> filteredProjects = FXCollections.observableArrayList();
    private final ObservableList<Tache> masterTasks = FXCollections.observableArrayList();
    private final ObservableList<Tache> filteredTasks = FXCollections.observableArrayList();

    // Team management
    private final ObservableList<EmployeeInfo> allEmployees = FXCollections.observableArrayList();
    private final ObservableList<EmployeeInfo> responsables = FXCollections.observableArrayList();
    private final ObservableList<EmployeeInfo> projectTeamMembers = FXCollections.observableArrayList();

    // Current user information (set from login page)
    private int currentUserId;
    private String currentUserNom;
    private String currentUserPrenom;
    private String currentUserRole;

    private Projet selectedProject = null;

    @FXML
    public void initialize() {
        // Load user info from session (set by login page)
        loadUserFromSession();

        // Setup user info display
        updateUserInfoDisplay();

        // Setup Google Calendar button
        if (btnGoogleCalendar != null) {
            btnGoogleCalendar.setOnAction(e -> handleGoogleCalendarConnection());
            updateCalendarStatusDisplay();
        }

        // fill filter combos (optional)
        statusFilter.getItems().setAll(statut.values());
        statusFilter.getItems().addFirst(null); // allow "all"
        statusFilter.setPromptText("Statut");

        sortBox.getItems().setAll("Nom (A-Z)", "Nom (Z-A)", "Date début (↑)", "Date début (↓)");
        sortBox.setPromptText("Trier");

        // details combos
        detailStatutBox.getItems().setAll(statut.values());
        detailPrioriteBox.getItems().setAll(priority.values());

        // Load employees from DB
        loadAllEmployees();

        // list
        projectsList.setItems(filteredProjects);
        setupCardCellFactory();

        // listeners
        btnRefresh.setOnAction(e -> loadProjectsFromDB());
        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
        statusFilter.valueProperty().addListener((obs, o, n) -> applyFilters());
        sortBox.valueProperty().addListener((obs, o, n) -> applyFilters());

        projectsList.getSelectionModel().selectedItemProperty().addListener((obs, oldP, newP) -> {
            selectedProject = newP;
            fillDetailsFromSelected();
            loadTasksForSelectedProject();
            loadProjectTeam();
        });

        btnSaveDetails.setOnAction(e -> saveSelectedProject());
        btnDeleteProject.setOnAction(e -> deleteSelectedProject());

        btnNewProject.setOnAction(e -> openAddProject());

        // Team management buttons
        btnAddEmployee.setOnAction(e -> addEmployeeToTeam());
        setupTeamListView();

        // Tasks tab buttons
        btnCreateTaskInline.setOnAction(e -> openCreateTaskModal());
        btnRefreshTasks.setOnAction(e -> loadTasksForSelectedProject());
        taskSearchField.textProperty().addListener((obs, o, n) -> applyTaskFilters());

        if (taskSortBox != null) {
            taskSortBox.getItems().setAll("Toutes les tâches", "Mes tâches");
            taskSortBox.setValue("Toutes les tâches");
            taskSortBox.valueProperty().addListener((obs, o, n) -> applyTaskFilters());
        }

        // Setup Kanban board
        setupTasksTable();

        // Setup Calendar tab
        setupCalendarTab();

        // initial state
        setDetailsEnabled(false);
        detailsInfoLabel.setText("");
        selectedProjectTitle.setText("(Aucun projet sélectionné)");

        // Apply role-based restrictions
        applyRoleBasedRestrictions();

        // load data
        loadProjectsFromDB();
    }

    /**
     * Apply UI restrictions based on user role
     */
    private void applyRoleBasedRestrictions() {
        boolean canManage = canManageProjects();

        // Hide/disable create project button for employees
        btnNewProject.setVisible(canManage);
        btnNewProject.setManaged(canManage);

        // Hide/disable create task button for employees
        btnCreateTaskInline.setVisible(canManage);
        btnCreateTaskInline.setManaged(canManage);

        // Show/hide team management controls based on role
        btnAddEmployee.setVisible(canManage);
        btnAddEmployee.setManaged(canManage);
        addEmployeeBox.setVisible(canManage);
        addEmployeeBox.setManaged(canManage);

        // Hide/show delete and save buttons for project management
        btnDeleteProject.setVisible(canManage);
        btnDeleteProject.setManaged(canManage);

        // Re-setup team list view to apply correct role permissions for remove buttons
        setupTeamListView();

        // Re-setup Kanban board for new role
        setupKanbanBoard();
    }

    /**
     * Set the current user information (called from login page)
     */
    public void setCurrentUser(int userId, String nom, String prenom, String role) {
        this.currentUserId = userId;
        this.currentUserNom = nom;
        this.currentUserPrenom = prenom;
        this.currentUserRole = role;
        updateUserInfoDisplay();
        // Apply role-based restrictions after user is set
        applyRoleBasedRestrictions();
        // Refresh project details to show/hide buttons correctly
        if (selectedProject != null) {
            fillDetailsFromSelected();
        }
    }

    /**
     * Load user info from the session (set by login page)
     */
    private void loadUserFromSession() {
        employe emp = session.getEmploye();
        if (emp != null) {
            this.currentUserId = emp.getId_employé();
            this.currentUserNom = emp.getNom();
            this.currentUserPrenom = emp.getPrenom();
            this.currentUserRole = emp.getRole() != null ? emp.getRole().getLibelle() : "employé";
        }
    }

    /**
     * Get current user ID
     */
    public int getCurrentUserId() {
        return currentUserId;
    }

    /**
     * Get current user role
     */
    public String getCurrentUserRole() {
        return currentUserRole;
    }

    /**
     * Check if current user is a simple employee
     */
    private boolean isEmployee() {
        if (currentUserRole == null) return true;
        String role = currentUserRole.toLowerCase();
        return role.contains("employ");
    }

    /**
     * Check if current user can manage projects
     * Roles: administrateur entreprise, RH, chef projet
     */
    private boolean canManageProjects() {
        if (currentUserRole == null) return false;
        String role = currentUserRole.toLowerCase();
        return role.contains("administrateur") || "rh".equals(role) || role.contains("chef");
    }

    /**
     * Check if current user can manage tasks
     * Roles: chef projet, administrateur entreprise
     */
    private boolean canManageTasks() {
        if (currentUserRole == null) return false;
        String role = currentUserRole.toLowerCase();
        return role.contains("administrateur") || role.contains("chef");
    }

    /**
     * Update the user info display in the top bar
     */
    private void updateUserInfoDisplay() {
        if (currentUserLabel != null) {
            String fullName = currentUserPrenom + " " + currentUserNom;
            currentUserLabel.setText("👤 " + fullName.trim());
        }
        if (currentRoleLabel != null) {
            currentRoleLabel.setText(formatRole(currentUserRole));
        }
    }

    /**
     * Format role for display
     */
    private String formatRole(String role) {
        if (role == null) return "Utilisateur";
        String lowerRole = role.toLowerCase();
        if (lowerRole.contains("administrateur")) return "🔐 Administrateur Entreprise";
        if (lowerRole.contains("chef")) return "👔 Chef Projet";
        return switch (lowerRole) {
            case "rh" -> "🏢 Ressources Humaines";
            default -> "👷 Employé";
        };
    }


    private void loadAllEmployees() {
        try {
            int idEntreprise = session.getIdEntreprise();

            // Load all employees of the same company
            List<EmployeeInfo> employees = employeCRUD.getAllEmployees(idEntreprise);
            allEmployees.setAll(employees);

            // Load only chef projet employees of the same company for the responsable dropdown
            List<EmployeeInfo> responsablesList = employeCRUD.getEmployeesByRole("chef projet", idEntreprise);
            responsables.setAll(responsablesList);
            if (detailResponsableBox != null) {
                detailResponsableBox.setItems(FXCollections.observableArrayList(responsablesList));
            }

            // For adding employees to team, show all employees of the same company
            if (addEmployeeBox != null) {
                addEmployeeBox.setItems(FXCollections.observableArrayList(employees));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur", "Impossible de charger les employés: " + e.getMessage());
        }
    }

    // ===================== LOAD / FILTER =====================

    private void loadProjectsFromDB() {
        try {
            List<Projet> data = projetCRUD.afficher();

            // For employees, only show projects they are part of
            if (isEmployee()) {
                List<Integer> myProjectIds = equipeCRUD.getProjectIdsForEmployee(currentUserId);
                data = data.stream()
                        .filter(p -> myProjectIds.contains(p.getProjet_id()))
                        .collect(Collectors.toList());
            }

            masterProjects.setAll(data);
            applyFilters();
            // Refresh calendar project filter with latest projects
            updateCalendarProjectFilter();
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur chargement", e.getMessage());
        }
    }

    private void applyFilters() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        statut stFilter = statusFilter.getValue();
        String sort = sortBox.getValue();

        List<Projet> tmp = masterProjects.stream()
                .filter(p -> matchesQuery(p, q))
                .filter(p -> stFilter == null || (p.getStatut() == stFilter))
                .collect(Collectors.toList());

        // sorting
        if (sort != null) {
            switch (sort) {
                case "Nom (A-Z)" -> tmp.sort((a,b) -> safe(a.getNom()).compareToIgnoreCase(safe(b.getNom())));
                case "Nom (Z-A)" -> tmp.sort((a,b) -> safe(b.getNom()).compareToIgnoreCase(safe(a.getNom())));
                case "Date début (↑)" -> tmp.sort((a,b) -> safeLocalDate(a.getDate_debut()).compareTo(safeLocalDate(b.getDate_debut())));
                case "Date début (↓)" -> tmp.sort((a,b) -> safeLocalDate(b.getDate_debut()).compareTo(safeLocalDate(a.getDate_debut())));
            }
        }

        filteredProjects.setAll(tmp);

        updateCounters();
        updateEmptyState();

        // keep selection if possible
        if (selectedProject != null && filteredProjects.contains(selectedProject)) {
            projectsList.getSelectionModel().select(selectedProject);
        }
    }

    private boolean matchesQuery(Projet p, String q) {
        if (q.isBlank()) return true;
        String nom = safe(p.getNom()).toLowerCase();
        String desc = safe(p.getDescription()).toLowerCase();
        return nom.contains(q) || desc.contains(q);
    }

    private void updateCounters() {
        projectsCountLabel.setText(String.valueOf(masterProjects.size()));

        long actifs = masterProjects.stream()
                .filter(p -> p.getStatut() == statut.EN_COURS)
                .count();

        activeCountLabel.setText(String.valueOf(actifs));
    }

    private void updateEmptyState() {
        boolean empty = filteredProjects.isEmpty();
        emptyStateLabel.setVisible(empty);
        emptyStateLabel.setManaged(empty);
    }


    private void setupKanbanBoard() {
        // Setup drag and drop for each column
        setupColumnDragDrop(todoTasksContainer, statut_t.A_FAIRE);
        setupColumnDragDrop(inProgressTasksContainer, statut_t.EN_COURS);
        setupColumnDragDrop(blockedTasksContainer, statut_t.BLOCQUEE);
        setupColumnDragDrop(doneTasksContainer, statut_t.TERMINEE);

        // Setup "Add task" buttons at bottom of columns (except DONE)
        boolean canManage = canManageTasks();

        if (canManage) {
            if (todoAddCard != null) {
                todoAddCard.setVisible(true);
                todoAddCard.setManaged(true);
            }
            if (inProgressAddCard != null) {
                inProgressAddCard.setVisible(true);
                inProgressAddCard.setManaged(true);
            }
            if (blockedAddCard != null) {
                blockedAddCard.setVisible(true);
                blockedAddCard.setManaged(true);
            }

            setupAddCardButton(todoAddCard, statut_t.A_FAIRE);
            setupAddCardButton(inProgressAddCard, statut_t.EN_COURS);
            setupAddCardButton(blockedAddCard, statut_t.BLOCQUEE);
            // Hide add button for DONE column - tasks can't be added directly as completed
        } else {
            // Hide add buttons for employees
            if (todoAddCard != null) { todoAddCard.setVisible(false); todoAddCard.setManaged(false); }
            if (inProgressAddCard != null) { inProgressAddCard.setVisible(false); inProgressAddCard.setManaged(false); }
            if (blockedAddCard != null) { blockedAddCard.setVisible(false); blockedAddCard.setManaged(false); }
        }
    }

    /**
     * Setup drag and drop handlers for a column
     */
    private void setupColumnDragDrop(VBox container, statut_t targetStatus) {
        // Get the column color based on status
        String columnBgColor = getColumnBackgroundColor(targetStatus);
        String highlightColor = getColumnHighlightColor(targetStatus);

        container.setOnDragOver(event -> {
            if (event.getGestureSource() != container && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        container.setOnDragEntered(event -> {
            if (event.getGestureSource() != container && event.getDragboard().hasString()) {
                // Smooth highlight effect
                container.setStyle("-fx-padding: 8 12 12 12; -fx-background-color: " + highlightColor + "; -fx-background-radius: 8; -fx-border-color: " + getColumnAccentColor(targetStatus) + "; -fx-border-width: 2; -fx-border-style: dashed; -fx-border-radius: 10;");
            }
            event.consume();
        });

        container.setOnDragExited(event -> {
            // Reset to original style
            container.setStyle("-fx-padding: 8 12 12 12;");
            event.consume();
        });

        container.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;

            if (db.hasString()) {
                try {
                    int taskId = Integer.parseInt(db.getString());
                    Tache task = findTaskById(taskId);

                    if (task != null && task.getStatut_tache() != targetStatus) {
                        // Check if employee can only change status of their own tasks
                        if (isEmployee() && task.getId_employe() != currentUserId) {
                            showError("Action non autorisée", "Vous ne pouvez modifier que vos propres tâches.");
                        } else {
                            updateTaskStatus(task, targetStatus);
                            success = true;
                        }
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }

            event.setDropCompleted(success);
            event.consume();
        });
    }

    private String getColumnBackgroundColor(statut_t status) {
        return switch (status) {
            case A_FAIRE -> "#F1F5F9";
            case EN_COURS -> "#EFF6FF";
            case BLOCQUEE -> "#FEF2F2";
            case TERMINEE -> "#ECFDF5";
        };
    }

    private String getColumnHighlightColor(statut_t status) {
        return switch (status) {
            case A_FAIRE -> "#E2E8F0";
            case EN_COURS -> "#DBEAFE";
            case BLOCQUEE -> "#FEE2E2";
            case TERMINEE -> "#D1FAE5";
        };
    }

    private String getColumnAccentColor(statut_t status) {
        return switch (status) {
            case A_FAIRE -> "#64748B";
            case EN_COURS -> "#3B82F6";
            case BLOCQUEE -> "#EF4444";
            case TERMINEE -> "#10B981";
        };
    }

    /**
     * Setup add card button functionality
     */
    private void setupAddCardButton(HBox addCard, statut_t defaultStatus) {
        if (addCard == null) return;

        addCard.setOnMouseEntered(e ->
                addCard.setStyle("-fx-background-color: rgba(99,102,241,0.1); -fx-background-radius: 8; -fx-padding: 12; -fx-cursor: hand;")
        );

        addCard.setOnMouseExited(e ->
                addCard.setStyle("-fx-background-color: transparent; -fx-padding: 12; -fx-cursor: hand;")
        );

        addCard.setOnMouseClicked(e -> openCreateTaskModalWithStatus(defaultStatus));
    }

    /**
     * Find a task by its ID
     */
    private Tache findTaskById(int taskId) {
        return masterTasks.stream()
                .filter(t -> t.getId_tache() == taskId)
                .findFirst()
                .orElse(null);
    }

    /**
     * Update task status via drag and drop
     */
    private void updateTaskStatus(Tache task, statut_t newStatus) {
        try {
            statut_t oldStatus = task.getStatut_tache();
            task.setStatut_tache(newStatus);

            // Auto-update progression
            if (newStatus == statut_t.TERMINEE) {
                task.setProgression(100);
            } else if (newStatus == statut_t.A_FAIRE && task.getProgression() == 100) {
                task.setProgression(0);
            }

            tacheCRUD.modifier(task);
            loadTasksForSelectedProject();
            taskInfoLabel.setText("✅ Tâche déplacée: " + prettyEnum(oldStatus) + " → " + prettyEnum(newStatus));

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur", "Impossible de mettre à jour le statut: " + e.getMessage());
        }
    }

    /**
     * Create a Kanban task card with modern light design
     */
    private VBox createTaskCard(Tache task) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(14));
        card.setStyle(getTaskCardStyle(task));
        card.setUserData(task);

        // Make card draggable with smooth animation
        card.setOnDragDetected(event -> {
            // Employees can only drag their own tasks
            if (isEmployee() && task.getId_employe() != currentUserId) {
                return;
            }

            Dragboard db = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(task.getId_tache()));
            db.setContent(content);

            // Create a snapshot for drag image
            db.setDragView(card.snapshot(null, null));

            // Visual feedback during drag - scale down and fade
            card.setScaleX(0.95);
            card.setScaleY(0.95);
            card.setOpacity(0.6);
            card.setStyle(getTaskCardDraggingStyle(task));

            event.consume();
        });

        card.setOnDragDone(event -> {
            // Reset card appearance with smooth transition
            card.setScaleX(1.0);
            card.setScaleY(1.0);
            card.setOpacity(1.0);
            card.setStyle(getTaskCardStyle(task));
            event.consume();
        });

        // Hover effect
        card.setOnMouseEntered(e -> {
            card.setStyle(getTaskCardHoverStyle(task));
            card.setCursor(javafx.scene.Cursor.HAND);
        });

        card.setOnMouseExited(e -> {
            card.setStyle(getTaskCardStyle(task));
        });

        // Priority indicator - left border stripe
        String priorityColor = getPriorityColor(task.getPriority_tache());

        // Header with title and 3-dot menu
        HBox headerRow = new HBox(10);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        // Title
        Label titleLabel = new Label(task.getTitre());
        titleLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 14px; -fx-text-fill: #0F172A; -fx-font-family: 'Segoe UI', sans-serif;");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(210);

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 3-dot menu button
        Button moreButton = new Button("⋮");
        moreButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-size: 16px; -fx-cursor: hand; -fx-padding: 0; -fx-min-width: 24; -fx-min-height: 24;");
        moreButton.setOnMouseEntered(e -> moreButton.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #1E293B; -fx-font-size: 16px; -fx-cursor: hand; -fx-padding: 0; -fx-min-width: 24; -fx-min-height: 24; -fx-background-radius: 4;"));
        moreButton.setOnMouseExited(e -> moreButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-size: 16px; -fx-cursor: hand; -fx-padding: 0; -fx-min-width: 24; -fx-min-height: 24;"));

        // Create and bind context menu to button
        ContextMenu contextMenu = createTaskContextMenu(task);
        moreButton.setOnAction(e -> contextMenu.show(moreButton, Side.BOTTOM, 0, 0));

        headerRow.getChildren().addAll(titleLabel, spacer, moreButton);

        // Description (truncated)
        String desc = task.getDescription();
        if (desc != null && !desc.isEmpty()) {
            Label descLabel = new Label(desc.length() > 80 ? desc.substring(0, 80) + "..." : desc);
            descLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px; -fx-font-family: 'Segoe UI', sans-serif;");
            descLabel.setWrapText(true);
            descLabel.setMaxWidth(250);
            card.getChildren().add(descLabel);
        }

        // Tags row (priority + due date)
        HBox tagsRow = new HBox(8);
        tagsRow.setAlignment(Pos.CENTER_LEFT);

        // Priority badge with glow effect
        Label priorityBadge = new Label(getPriorityLabel(task.getPriority_tache()));
        priorityBadge.setStyle(getKanbanPriorityBadgeStyle(task.getPriority_tache()) + "; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 1);");
        tagsRow.getChildren().add(priorityBadge);

        // Due date if exists
        if (task.getDate_limite() != null) {
            Label dueDateLabel = new Label("📅 " + formatDate(task.getDate_limite()));
            dueDateLabel.setStyle(getDueDateStyle(task.getDate_limite()) + "; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 3, 0, 0, 1);");
            tagsRow.getChildren().add(dueDateLabel);
        }

        // Bottom row (assignee + progress)
        HBox bottomRow = new HBox(8);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        // Assignee avatar
        String employeeName = getEmployeeNameById(task.getId_employe());
        String initials = getInitials(employeeName);
        Label avatar = new Label(initials);
        avatar.setStyle("-fx-background-color: " + getAvatarColor(task.getId_employe()) + "; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: 700; -fx-padding: 5 7; -fx-background-radius: 50; -fx-font-family: 'Segoe UI', sans-serif; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 4, 0, 0, 2);");
        avatar.setTooltip(new Tooltip(employeeName));
        bottomRow.getChildren().add(avatar);

        // Employee name
        Label nameLabel = new Label(employeeName);
        nameLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 12px; -fx-font-weight: 500; -fx-font-family: 'Segoe UI', sans-serif;");
        bottomRow.getChildren().add(nameLabel);

        // Spacer
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        bottomRow.getChildren().add(spacer2);

        // Progress indicator
        if (task.getProgression() > 0) {
            HBox progressBox = new HBox(4);
            progressBox.setAlignment(Pos.CENTER);

            ProgressBar miniProgress = new ProgressBar(task.getProgression() / 100.0);
            miniProgress.setPrefWidth(50);
            miniProgress.setPrefHeight(6);
            miniProgress.setStyle("-fx-accent: " + getProgressColor(task.getProgression()) + "; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 1);");

            Label progressLabel = new Label(task.getProgression() + "%");
            progressLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B; -fx-font-weight: 600; -fx-font-family: 'Segoe UI', sans-serif;");

            progressBox.getChildren().addAll(miniProgress, progressLabel);
            bottomRow.getChildren().add(progressBox);
        }

        // Assemble card with priority stripe on left
        card.getChildren().add(0, headerRow);
        card.getChildren().addAll(tagsRow, bottomRow);

        // Apply left border for priority indicator
        card.setStyle(card.getStyle() + " -fx-border-color: " + priorityColor + " transparent transparent transparent; -fx-border-width: 3 0 0 0; -fx-border-radius: 10 10 0 0;");

        // Click to edit (for managers) or view details
        card.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                if (canManageTasks()) {
                    openEditTaskModal(task);
                } else {
                    showTaskDetailsPopup(task);
                }
            }
        });


        return card;
    }

    private String formatDate(LocalDate date) {
        if (date == null) return "";
        LocalDate today = LocalDate.now();
        if (date.equals(today)) return "Aujourd'hui";
        if (date.equals(today.plusDays(1))) return "Demain";
        if (date.equals(today.minusDays(1))) return "Hier";
        return date.getDayOfMonth() + "/" + date.getMonthValue();
    }

    private String getPriorityLabel(priority p) {
        if (p == null) return "Normal";
        return switch (p) {
            case HAUTE -> "Haute";
            case MOYENNE -> "Moyenne";
            case BASSE -> "Basse";
        };
    }

    private String getPriorityColor(priority p) {
        if (p == null) return "#94A3B8";
        return switch (p) {
            case HAUTE -> "#EF4444";
            case MOYENNE -> "#F59E0B";
            case BASSE -> "#22C55E";
        };
    }

    /**
     * Create context menu for task card
     */
    private ContextMenu createTaskContextMenu(Tache task) {
        ContextMenu menu = new ContextMenu();

        // View details
        MenuItem viewItem = new MenuItem("👁 Voir les détails");
        viewItem.setOnAction(e -> showTaskDetailsPopup(task));
        menu.getItems().add(viewItem);

        if (canManageTasks()) {
            // Edit
            MenuItem editItem = new MenuItem("✏️ Modifier");
            editItem.setOnAction(e -> openEditTaskModal(task));
            menu.getItems().add(editItem);

            // Separator
            menu.getItems().add(new SeparatorMenuItem());

            // Move to status submenu
            Menu moveMenu = new Menu("📦 Déplacer vers...");
            for (statut_t status : statut_t.values()) {
                if (status != task.getStatut_tache()) {
                    MenuItem statusItem = new MenuItem(getStatusEmoji(status) + " " + prettyEnum(status));
                    statusItem.setOnAction(e -> updateTaskStatus(task, status));
                    moveMenu.getItems().add(statusItem);
                }
            }
            menu.getItems().add(moveMenu);

            // Separator
            menu.getItems().add(new SeparatorMenuItem());

            // Delete
            MenuItem deleteItem = new MenuItem("🗑 Supprimer");
            deleteItem.setStyle("-fx-text-fill: #DC2626;");
            deleteItem.setOnAction(e -> deleteTask(task));
            menu.getItems().add(deleteItem);
        } else if (isEmployee() && task.getId_employe() == currentUserId) {
            // Employee can change status of their own tasks
            menu.getItems().add(new SeparatorMenuItem());

            Menu moveMenu = new Menu("📦 Changer le statut...");
            for (statut_t status : statut_t.values()) {
                if (status != task.getStatut_tache()) {
                    MenuItem statusItem = new MenuItem(getStatusEmoji(status) + " " + prettyEnum(status));
                    statusItem.setOnAction(e -> updateTaskStatus(task, status));
                    moveMenu.getItems().add(statusItem);
                }
            }
            menu.getItems().add(moveMenu);
        }

        return menu;
    }

    /**
     * Show task details in a popup
     */
    private void showTaskDetailsPopup(Tache task) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails de la tâche");
        dialog.setHeaderText(null);

        VBox content = new VBox(16);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: #F8FAFC;");
        content.setPrefWidth(480);

        // Title section with priority color bar
        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.setStyle("-fx-padding: 16; -fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);");

        VBox priorityIndicator = new VBox();
        priorityIndicator.setPrefWidth(4);
        priorityIndicator.setPrefHeight(40);
        priorityIndicator.setStyle("-fx-background-color: " + getPriorityColor(task.getPriority_tache()) + "; -fx-background-radius: 2;");

        Label title = new Label(task.getTitre());
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #0F172A; -fx-font-family: 'Segoe UI', sans-serif;");
        title.setWrapText(true);
        titleRow.getChildren().addAll(priorityIndicator, title);

        // Status and priority badges with glow
        HBox badgesRow = new HBox(10);
        badgesRow.setAlignment(Pos.CENTER_LEFT);

        Label statusBadge = new Label(getStatusEmoji(task.getStatut_tache()) + " " + prettyEnum(task.getStatut_tache()));
        statusBadge.setStyle(getKanbanStatusBadgeStyle(task.getStatut_tache()) + "; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 6, 0, 0, 2);");

        Label priorityBadge = new Label(getPriorityLabel(task.getPriority_tache()));
        priorityBadge.setStyle(getKanbanPriorityBadgeStyle(task.getPriority_tache()) + "; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 6, 0, 0, 2);");

        badgesRow.getChildren().addAll(statusBadge, priorityBadge);

        // Description Section
        VBox descBox = new VBox(6);
        descBox.setStyle("-fx-padding: 14; -fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 6, 0, 0, 1);");
        Label descTitle = new Label("📝 Description");
        descTitle.setStyle("-fx-font-weight: 700; -fx-text-fill: #0F172A; -fx-font-size: 13px; -fx-font-family: 'Segoe UI', sans-serif;");
        Label descContent = new Label(task.getDescription() != null && !task.getDescription().isEmpty() ? task.getDescription() : "Aucune description fournie");
        descContent.setStyle("-fx-text-fill: #475569; -fx-font-size: 14px; -fx-font-family: 'Segoe UI', sans-serif;");
        descContent.setWrapText(true);
        descBox.getChildren().addAll(descTitle, descContent);

        // Assignee Section
        HBox assigneeBox = new HBox(10);
        assigneeBox.setAlignment(Pos.CENTER_LEFT);
        assigneeBox.setStyle("-fx-padding: 14; -fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 6, 0, 0, 1);");

        Label assigneeTitle = new Label("👤 Assigné à");
        assigneeTitle.setStyle("-fx-font-weight: 700; -fx-text-fill: #0F172A; -fx-font-size: 13px; -fx-font-family: 'Segoe UI', sans-serif;");

        String employeeName = getEmployeeNameById(task.getId_employe());
        Label avatar = new Label(getInitials(employeeName));
        avatar.setStyle("-fx-background-color: " + getAvatarColor(task.getId_employe()) + "; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: 700; -fx-padding: 7 9; -fx-background-radius: 50; -fx-font-family: 'Segoe UI', sans-serif; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 5, 0, 0, 2);");

        Label assigneeName = new Label(employeeName);
        assigneeName.setStyle("-fx-text-fill: #475569; -fx-font-size: 14px; -fx-font-weight: 600; -fx-font-family: 'Segoe UI', sans-serif;");

        VBox assigneeContent = new VBox(4);
        assigneeContent.getChildren().addAll(assigneeTitle, new HBox(8, avatar, assigneeName) {{ setAlignment(Pos.CENTER_LEFT); }});
        assigneeBox.getChildren().add(assigneeContent);

        // Dates Section
        HBox datesBox = new HBox(15);
        datesBox.setStyle("-fx-padding: 14; -fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 6, 0, 0, 1);");

        VBox startDate = new VBox(4);
        startDate.getChildren().addAll(
                new Label("📅 Date début") {{ setStyle("-fx-font-weight: 700; -fx-text-fill: #0F172A; -fx-font-size: 12px; -fx-font-family: 'Segoe UI', sans-serif;"); }},
                new Label(task.getDate_deb() != null ? task.getDate_deb().toString() : "Non défini") {{ setStyle("-fx-text-fill: #475569; -fx-font-size: 13px; -fx-font-family: 'Segoe UI', sans-serif;"); }}
        );

        VBox endDate = new VBox(4);
        endDate.getChildren().addAll(
                new Label("⏰ Date limite") {{ setStyle("-fx-font-weight: 700; -fx-text-fill: #0F172A; -fx-font-size: 12px; -fx-font-family: 'Segoe UI', sans-serif;"); }},
                new Label(task.getDate_limite() != null ? task.getDate_limite().toString() : "Non défini") {{ setStyle("-fx-text-fill: #475569; -fx-font-size: 13px; -fx-font-family: 'Segoe UI', sans-serif;"); }}
        );

        datesBox.getChildren().addAll(startDate, endDate);

        // Progress Section - Prominent and Separate
        VBox progressSection = new VBox(10);
        progressSection.setStyle("-fx-padding: 16; -fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 6, 0, 0, 1); -fx-border-color: #E0E7FF; -fx-border-width: 2; -fx-border-radius: 10;");

        Label progressLabel = new Label("📊 Progression: " + task.getProgression() + "%");
        progressLabel.setStyle("-fx-font-weight: 700; -fx-text-fill: #0F172A; -fx-font-size: 14px; -fx-font-family: 'Segoe UI', sans-serif;");

        ProgressBar progressBar = new ProgressBar(task.getProgression() / 100.0);
        progressBar.setPrefWidth(430);
        progressBar.setPrefHeight(10);
        progressBar.setStyle("-fx-accent: " + getProgressColor(task.getProgression()) + "; -fx-control-inner-background: #E2E8F0; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 1);");

        progressSection.getChildren().addAll(progressLabel, progressBar);

        // For employees, add progress slider and update button
        if (isEmployee() && task.getId_employe() == currentUserId) {
            Separator sep = new Separator();
            sep.setStyle("-fx-background-color: #E2E8F0;");

            Label updateLabel = new Label("Mettre à jour la progression:");
            updateLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #475569; -fx-font-family: 'Segoe UI', sans-serif;");

            Slider progressSlider = new Slider(0, 100, task.getProgression());
            progressSlider.setShowTickLabels(true);
            progressSlider.setShowTickMarks(true);
            progressSlider.setMajorTickUnit(25);
            progressSlider.setBlockIncrement(10);
            progressSlider.setStyle("-fx-control-inner-background: #E2E8F0;");

            HBox buttonBox = new HBox(10);
            buttonBox.setAlignment(Pos.CENTER_RIGHT);

            Button updateBtn = new Button("💾 Enregistrer");
            updateBtn.setStyle("-fx-background-color: #6366F1; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 10 20; -fx-font-family: 'Segoe UI', sans-serif; -fx-font-weight: 600; -fx-effect: dropshadow(gaussian, rgba(99,102,241,0.3), 6, 0, 0, 2);");
            updateBtn.setOnMouseEntered(e -> updateBtn.setStyle("-fx-background-color: #4F46E5; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 10 20; -fx-font-family: 'Segoe UI', sans-serif; -fx-font-weight: 600; -fx-effect: dropshadow(gaussian, rgba(99,102,241,0.4), 8, 0, 0, 3);"));
            updateBtn.setOnMouseExited(e -> updateBtn.setStyle("-fx-background-color: #6366F1; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 10 20; -fx-font-family: 'Segoe UI', sans-serif; -fx-font-weight: 600; -fx-effect: dropshadow(gaussian, rgba(99,102,241,0.3), 6, 0, 0, 2);"));
            updateBtn.setOnAction(e -> {
                int newProgress = (int) progressSlider.getValue();
                updateTaskProgressInline(task, newProgress);
                dialog.close();
            });

            buttonBox.getChildren().add(updateBtn);
            progressSection.getChildren().addAll(sep, updateLabel, progressSlider, buttonBox);
        }

        content.getChildren().addAll(titleRow, badgesRow, descBox, assigneeBox, datesBox, progressSection);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setStyle("-fx-background-color: #F8FAFC;");
        dialog.showAndWait();
    }

    // ===== Kanban Styling Methods =====

    private String getTaskCardStyle(Tache task) {
        return "-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 3);";
    }

    private String getTaskCardHoverStyle(Tache task) {
        return "-fx-background-color: #FAFAFA; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 5);";
    }

    private String getTaskCardDraggingStyle(Tache task) {
        return "-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(99,102,241,0.35), 18, 0, 0, 6);";
    }

    private String getPriorityBarStyle(priority p) {
        if (p == null) return "-fx-background-color: #94A3B8; -fx-background-radius: 10 10 0 0;";
        return switch (p) {
            case HAUTE -> "-fx-background-color: #EF4444; -fx-background-radius: 10 10 0 0;";
            case MOYENNE -> "-fx-background-color: #F59E0B; -fx-background-radius: 10 10 0 0;";
            case BASSE -> "-fx-background-color: #22C55E; -fx-background-radius: 10 10 0 0;";
        };
    }

    private String getPriorityEmoji(priority p) {
        if (p == null) return "⚪";
        return switch (p) {
            case HAUTE -> "🔴";
            case MOYENNE -> "🟡";
            case BASSE -> "🟢";
        };
    }

    private String getStatusEmoji(statut_t s) {
        if (s == null) return "⚪";
        return switch (s) {
            case A_FAIRE -> "📝";
            case EN_COURS -> "🔄";
            case BLOCQUEE -> "⛔";
            case TERMINEE -> "✅";
        };
    }

    private String getKanbanPriorityBadgeStyle(priority p) {
        String base = "-fx-padding: 3 8; -fx-background-radius: 6; -fx-font-size: 11px; -fx-font-weight: 600; -fx-font-family: 'Segoe UI', sans-serif;";
        if (p == null) return base + "-fx-background-color: #F1F5F9; -fx-text-fill: #64748B;";
        return switch (p) {
            case HAUTE -> base + "-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626;";
            case MOYENNE -> base + "-fx-background-color: #FEF3C7; -fx-text-fill: #D97706;";
            case BASSE -> base + "-fx-background-color: #DCFCE7; -fx-text-fill: #16A34A;";
        };
    }

    private String getKanbanStatusBadgeStyle(statut_t s) {
        String base = "-fx-padding: 6 12; -fx-background-radius: 8; -fx-font-size: 12px; -fx-font-weight: 600; -fx-font-family: 'Segoe UI', sans-serif;";
        if (s == null) return base + "-fx-background-color: #F1F5F9; -fx-text-fill: #64748B;";
        return switch (s) {
            case A_FAIRE -> base + "-fx-background-color: #F1F5F9; -fx-text-fill: #64748B;";
            case EN_COURS -> base + "-fx-background-color: #DBEAFE; -fx-text-fill: #2563EB;";
            case BLOCQUEE -> base + "-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626;";
            case TERMINEE -> base + "-fx-background-color: #DCFCE7; -fx-text-fill: #16A34A;";
        };
    }

    private String getDueDateStyle(LocalDate dueDate) {
        String base = "-fx-font-size: 11px; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-family: 'Segoe UI', sans-serif;";
        if (dueDate == null) return base + "-fx-text-fill: #94A3B8;";

        LocalDate today = LocalDate.now();
        if (dueDate.isBefore(today)) {
            // Overdue
            return base + "-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626;";
        } else if (dueDate.equals(today) || dueDate.isBefore(today.plusDays(3))) {
            // Due soon
            return base + "-fx-background-color: #FEF3C7; -fx-text-fill: #D97706;";
        }
        return base + "-fx-background-color: #F1F5F9; -fx-text-fill: #64748B;";
    }

    private String getProgressColor(int progress) {
        if (progress >= 100) return "#22C55E";
        if (progress >= 60) return "#3B82F6";
        if (progress >= 30) return "#F59E0B";
        return "#94A3B8";
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.isEmpty()) return "?";
        String[] parts = fullName.split(" ");
        if (parts.length >= 2) {
            return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
        }
        return fullName.substring(0, Math.min(2, fullName.length())).toUpperCase();
    }

    private String getAvatarColor(int employeeId) {
        String[] colors = {"#6366F1", "#EC4899", "#F59E0B", "#10B981", "#3B82F6", "#8B5CF6", "#06B6D4"};
        return colors[Math.abs(employeeId) % colors.length];
    }

    /**
     * Populate Kanban columns with tasks
     */
    private void populateKanbanBoard() {
        // Clear all columns
        todoTasksContainer.getChildren().clear();
        inProgressTasksContainer.getChildren().clear();
        blockedTasksContainer.getChildren().clear();
        doneTasksContainer.getChildren().clear();

        // Counters
        int todoCounter = 0, inProgressCounter = 0, blockedCounter = 0, doneCounter = 0;

        // Apply search filter
        String searchText = taskSearchField.getText() != null ? taskSearchField.getText().toLowerCase().trim() : "";

        // Copy and optionally filter by "My tasks"
        List<Tache> tasksToRender = new java.util.ArrayList<>(masterTasks);

        if (taskSortBox != null && "Mes tâches".equals(taskSortBox.getValue())) {
            // Filter to show only my tasks
            tasksToRender = tasksToRender.stream()
                    .filter(t -> t.getId_employe() == currentUserId)
                    .collect(Collectors.toList());
        }
        // Sort by task ID (default order)
        tasksToRender.sort(Comparator.comparingInt(Tache::getId_tache));

        for (Tache task : tasksToRender) {
            // Apply search filter
            if (!searchText.isEmpty()) {
                String title = task.getTitre() != null ? task.getTitre().toLowerCase() : "";
                String desc = task.getDescription() != null ? task.getDescription().toLowerCase() : "";
                if (!title.contains(searchText) && !desc.contains(searchText)) {
                    continue;
                }
            }

            VBox card = createTaskCard(task);

            switch (task.getStatut_tache()) {
                case A_FAIRE:
                    todoTasksContainer.getChildren().add(card);
                    todoCounter++;
                    break;
                case EN_COURS:
                    inProgressTasksContainer.getChildren().add(card);
                    inProgressCounter++;
                    break;
                case BLOCQUEE:
                    blockedTasksContainer.getChildren().add(card);
                    blockedCounter++;
                    break;
                case TERMINEE:
                    doneTasksContainer.getChildren().add(card);
                    doneCounter++;
                    break;
            }
        }

        // Update column counts
        todoCount.setText(String.valueOf(todoCounter));
        inProgressCount.setText(String.valueOf(inProgressCounter));
        blockedCount.setText(String.valueOf(blockedCounter));
        doneCount.setText(String.valueOf(doneCounter));

        // Update stats
        updateTaskInfo();

        // Show empty state if needed
        if (masterTasks.isEmpty()) {
            addEmptyStateToColumn(todoTasksContainer, "Aucune tâche à faire");
        }
    }

    /**
     * Add empty state placeholder to a column
     */
    private void addEmptyStateToColumn(VBox container, String message) {
        VBox emptyState = new VBox(8);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(30, 20, 30, 20));
        emptyState.setStyle("-fx-background-color: rgba(255,255,255,0.5); -fx-background-radius: 8;");

        Label icon = new Label("📭");
        icon.setStyle("-fx-font-size: 28px;");

        Label text = new Label(message);
        text.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px; -fx-font-family: 'Segoe UI', sans-serif;");

        emptyState.getChildren().addAll(icon, text);
        container.getChildren().add(emptyState);
    }

    /**
     * Open create task modal with a preset status
     */
    private void openCreateTaskModalWithStatus(statut_t defaultStatus) {
        if (selectedProject == null) {
            showError("Aucun projet sélectionné", "Veuillez d'abord sélectionner un projet.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Projet/AjoutTache.fxml"));
            Parent root = loader.load();

            AjoutTacheController c = loader.getController();
            c.setProjectId(selectedProject.getProjet_id());
            c.setProjectName(selectedProject.getNom()); // For AI features
            c.setOnSaved(this::loadTasksForSelectedProject);
            c.loadEmployeesForProject();
            c.setDefaultStatus(defaultStatus);

            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle("Nouvelle tâche - " + prettyEnum(defaultStatus));
            modal.setScene(new Scene(root));
            modal.showAndWait();

        } catch (IOException ex) {
            ex.printStackTrace();
            showError("Erreur", "Impossible d'ouvrir le formulaire de tâche: " + ex.getMessage());
        }
    }

    private void setupTasksTable() {
        // Initialize Kanban board
        setupKanbanBoard();
    }

    /**
     * Update task status inline (for employees)
     */
    private void updateTaskStatusInline(Tache tache, statut_t newStatus) {
        try {
            tache.setStatut_tache(newStatus);
            // Auto-set progress to 100% if completed
            if (newStatus == statut_t.TERMINEE) {
                tache.setProgression(100);
            }
            tacheCRUD.modifier(tache);
            loadTasksForSelectedProject();
            taskInfoLabel.setText("✅ Statut mis à jour: " + prettyEnum(newStatus));
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur", "Impossible de mettre à jour le statut: " + e.getMessage());
        }
    }

    /**
     * Update task progress inline (for employees)
     */
    private void updateTaskProgressInline(Tache tache, int newProgress) {
        try {
            tache.setProgression(newProgress);
            // Auto-set status to completed if progress is 100%
            if (newProgress == 100 && tache.getStatut_tache() != statut_t.TERMINEE) {
                tache.setStatut_tache(statut_t.TERMINEE);
            } else if (newProgress > 0 && newProgress < 100 && tache.getStatut_tache() == statut_t.A_FAIRE) {
                tache.setStatut_tache(statut_t.EN_COURS);
            }
            tacheCRUD.modifier(tache);
            loadTasksForSelectedProject();
            taskInfoLabel.setText("✅ Progression mise à jour: " + newProgress + "%");
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur", "Impossible de mettre à jour la progression: " + e.getMessage());
        }
    }

    private String getPriorityBadgeStyle(priority p) {
        String base = "-fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: 700;";
        return switch (p) {
            case HAUTE -> base + "-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626;";
            case MOYENNE -> base + "-fx-background-color: #FEF3C7; -fx-text-fill: #D97706;";
            case BASSE -> base + "-fx-background-color: #DCFCE7; -fx-text-fill: #16A34A;";
        };
    }

    private String getTaskStatusBadgeStyle(statut_t s) {
        String base = "-fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: 700;";
        return switch (s) {
            case A_FAIRE -> base + "-fx-background-color: #E2E8F0; -fx-text-fill: #475569;";
            case EN_COURS -> base + "-fx-background-color: #DBEAFE; -fx-text-fill: #2563EB;";
            case BLOCQUEE -> base + "-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626;";
            case TERMINEE -> base + "-fx-background-color: #DCFCE7; -fx-text-fill: #16A34A;";
        };
    }

    // ===================== TEAM MANAGEMENT =====================

    private void setupTeamListView() {
        detailTeamListView.setItems(projectTeamMembers);
        detailTeamListView.setCellFactory(lv -> new ListCell<>() {
            private final Button btnRemove = new Button("✕");
            private final HBox container = new HBox(10);
            private final Label nameLabel = new Label();
            private final Label roleLabel = new Label();
            private final Region spacer = new Region();

            {
                btnRemove.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-background-radius: 50; -fx-min-width: 24; -fx-min-height: 24; -fx-cursor: hand;");
                btnRemove.setOnAction(e -> {
                    EmployeeInfo emp = getItem();
                    if (emp != null) {
                        removeEmployeeFromTeam(emp);
                    }
                });
                container.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(spacer, Priority.ALWAYS);
            }

            @Override
            protected void updateItem(EmployeeInfo emp, boolean empty) {
                super.updateItem(emp, empty);
                if (empty || emp == null) {
                    setGraphic(null);
                } else {
                    // Check if this employee is the responsable
                    boolean isResponsable = selectedProject != null && emp.id() == selectedProject.getResponsable_id();

                    nameLabel.setText("👤 " + emp.getFullName());
                    nameLabel.setStyle("-fx-font-weight: 600;");

                    // Dynamically update container based on current role
                    container.getChildren().clear();
                    container.getChildren().add(nameLabel);

                    // Add responsable badge if this is the project responsable
                    if (isResponsable) {
                        roleLabel.setText("👔 Responsable");
                        roleLabel.setStyle("-fx-background-color: #DBEAFE; -fx-text-fill: #1D4ED8; -fx-padding: 2 8; -fx-background-radius: 10; -fx-font-size: 10px; -fx-font-weight: 700;");
                        container.getChildren().add(roleLabel);
                    }

                    container.getChildren().add(spacer);

                    // Only show remove button for managers and not for the responsable
                    if (canManageProjects() && !isResponsable) {
                        container.getChildren().add(btnRemove);
                    }

                    setGraphic(container);
                }
            }
        });
    }

    private void loadProjectTeam() {
        projectTeamMembers.clear();
        if (selectedProject == null) {
            updateTeamCount();
            return;
        }

        try {
            List<Integer> teamIds = equipeCRUD.getEmployeeIdsForProject(selectedProject.getProjet_id());
            List<EmployeeInfo> teamMembers = allEmployees.stream()
                    .filter(e -> teamIds.contains(e.id()))
                    .collect(Collectors.toList());
            projectTeamMembers.setAll(teamMembers);
            updateAddEmployeeDropdown();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        updateTeamCount();
    }

    private void updateAddEmployeeDropdown() {
        // Show only employees not already in the team
        List<EmployeeInfo> available = allEmployees.stream()
                .filter(e -> !projectTeamMembers.contains(e))
                .collect(Collectors.toList());
        addEmployeeBox.setItems(FXCollections.observableArrayList(available));
        addEmployeeBox.setValue(null);
    }

    private void addEmployeeToTeam() {
        if (selectedProject == null) return;
        EmployeeInfo selected = addEmployeeBox.getValue();
        if (selected == null) {
            showError("Sélection requise", "Veuillez sélectionner un employé à ajouter.");
            return;
        }

        try {
            equipeCRUD.addEmployeeToProject(selectedProject.getProjet_id(), selected.id());
            loadProjectTeam();
            detailsInfoLabel.setText("✅ " + selected.getFullName() + " ajouté à l'équipe.");
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur", "Impossible d'ajouter l'employé: " + e.getMessage());
        }
    }

    private void removeEmployeeFromTeam(EmployeeInfo emp) {
        if (selectedProject == null) return;

        // Prevent removing the responsable from the team
        if (emp.id() == selectedProject.getResponsable_id()) {
            showError("Action non autorisée", "Le responsable du projet ne peut pas être retiré de l'équipe.\nPour changer de responsable, modifiez-le dans les détails du projet.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Retirer " + emp.getFullName() + " de l'équipe ?");
        confirm.setContentText("L'employé sera retiré du projet.");

        confirm.showAndWait().ifPresent(type -> {
            if (type == ButtonType.OK) {
                try {
                    equipeCRUD.removeEmployeeFromProject(selectedProject.getProjet_id(), emp.id());
                    loadProjectTeam();
                    detailsInfoLabel.setText("✅ " + emp.getFullName() + " retiré de l'équipe.");
                } catch (SQLException e) {
                    e.printStackTrace();
                    showError("Erreur", "Impossible de retirer l'employé: " + e.getMessage());
                }
            }
        });
    }

    private void updateTeamCount() {
        teamCountLabel.setText(projectTeamMembers.size() + " membre(s)");
    }

    // ===================== DETAILS PANEL =====================

    private void fillDetailsFromSelected() {
        if (selectedProject == null) {
            clearDetails();
            selectedProjectTitle.setText("(Aucun projet sélectionné)");
            return;
        }

        setDetailsEnabled(true);
        selectedProjectTitle.setText(safe(selectedProject.getNom()));

        detailNomField.setText(selectedProject.getNom());
        detailDescArea.setText(selectedProject.getDescription() == null ? "" : selectedProject.getDescription());

        if (selectedProject.getDate_debut() != null) {
            detailDebutPicker.setValue(selectedProject.getDate_debut());
        } else {
            detailDebutPicker.setValue(null);
        }

        if (selectedProject.getDate_fin_prevue() != null) {
            detailFinPrevuePicker.setValue(selectedProject.getDate_fin_prevue());
        } else {
            detailFinPrevuePicker.setValue(null);
        }

        detailStatutBox.setValue(selectedProject.getStatut());
        detailPrioriteBox.setValue(selectedProject.getPriority());

        // Find and set responsable (search in responsables list)
        int respId = selectedProject.getResponsable_id();
        for (EmployeeInfo emp : responsables) {
            if (emp.id() == respId) {
                detailResponsableBox.setValue(emp);
                break;
            }
        }

        detailsInfoLabel.setText("");
    }

    private void clearDetails() {
        setDetailsEnabled(false);
        detailNomField.clear();
        detailDescArea.clear();
        detailDebutPicker.setValue(null);
        detailFinPrevuePicker.setValue(null);
        detailStatutBox.setValue(null);
        detailPrioriteBox.setValue(null);
        detailResponsableBox.setValue(null);
        detailsInfoLabel.setText("Sélectionnez un projet pour afficher ses détails.");
    }

    private void setDetailsEnabled(boolean enabled) {
        boolean canManage = canManageProjects();

        // For employees, fields are always disabled (read-only view)
        boolean editable = enabled && canManage;

        detailNomField.setDisable(!editable);
        detailDescArea.setDisable(!editable);
        detailDebutPicker.setDisable(!editable);
        detailFinPrevuePicker.setDisable(!editable);
        detailStatutBox.setDisable(!editable);
        detailPrioriteBox.setDisable(!editable);
        detailResponsableBox.setDisable(!editable);

        // Hide save/delete buttons for employees
        btnSaveDetails.setDisable(!editable);
        btnSaveDetails.setVisible(canManage);
        btnSaveDetails.setManaged(canManage);

        btnDeleteProject.setDisable(!editable);
        btnDeleteProject.setVisible(canManage);
        btnDeleteProject.setManaged(canManage);
    }

    // ===================== SAVE / DELETE =====================

    private void saveSelectedProject() {
        if (selectedProject == null) return;

        String nom = detailNomField.getText();
        if (nom == null || nom.isBlank()) {
            showError("Validation", "Le nom du projet est obligatoire.");
            return;
        }

        EmployeeInfo resp = detailResponsableBox.getValue();
        if (resp == null) {
            showError("Validation", "Veuillez choisir un responsable.");
            return;
        }

        LocalDate debut = detailDebutPicker.getValue();
        if (debut == null) {
            showError("Validation", "La date début est obligatoire.");
            return;
        }

        LocalDate finPrev = detailFinPrevuePicker.getValue();
        if (finPrev != null && finPrev.isBefore(debut)) {
            showError("Validation", "La date fin prévue doit être après la date début.");
            return;
        }

        statut st = detailStatutBox.getValue();
        priority pr = detailPrioriteBox.getValue();

        try {
            // Check if responsable changed
            boolean responsableChanged = resp.id() != selectedProject.getResponsable_id();

            // Update selectedProject object
            selectedProject.setNom(nom.trim());
            selectedProject.setDescription(detailDescArea.getText());
            selectedProject.setResponsable_id(resp.id());
            selectedProject.setDate_debut(debut);
            selectedProject.setDate_fin_prevue(finPrev);
            selectedProject.setStatut(st);
            selectedProject.setPriority(pr);

            projetCRUD.modifier(selectedProject);

            // If responsable changed, ensure new responsable is in the team
            if (responsableChanged) {
                List<Integer> currentTeam = equipeCRUD.getEmployeeIdsForProject(selectedProject.getProjet_id());
                if (!currentTeam.contains(resp.id())) {
                    equipeCRUD.addEmployeeToProject(selectedProject.getProjet_id(), resp.id());
                }
                loadProjectTeam(); // Refresh team list
            }

            detailsInfoLabel.setText("✅ Projet mis à jour.");
            loadProjectsFromDB();

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur SQL", e.getMessage());
        }
    }

    private void deleteSelectedProject() {
        if (selectedProject == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer le projet ?");
        confirm.setContentText("Projet : " + safe(selectedProject.getNom()) + "\nCette action est irréversible.");

        ButtonType btnYes = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(btnYes, btnNo);

        confirm.showAndWait().ifPresent(type -> {
            if (type == btnYes) {
                try {
                    projetCRUD.supprimer(selectedProject.getProjet_id()); // adapt getter name
                    selectedProject = null;
                    loadProjectsFromDB();
                } catch (SQLException e) {
                    e.printStackTrace();
                    showError("Erreur suppression", e.getMessage());
                }
            }
        });
    }

    // ===================== ADD PROJECT MODAL =====================

    private void openAddProject() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Projet/AjoutProjet.fxml"));
            Parent root = loader.load();

            AjoutProjetController ctrl = loader.getController();

            Stage dialog = new Stage();
            dialog.setTitle("Nouveau projet");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root));
            dialog.showAndWait();

            if (ctrl.isSaved()) {
                loadProjectsFromDB();
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur", "Impossible d'ouvrir le formulaire : " + e.getMessage());
        }
    }

    // ===================== LIST CARD CELL =====================

    private void setupCardCellFactory() {
        projectsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Projet p, boolean empty) {
                super.updateItem(p, empty);

                if (empty || p == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                HBox row = new HBox(12);
                row.setMinHeight(70);
                row.setStyle(
                        "-fx-padding: 12;" +
                                "-fx-background-color: white;" +
                                "-fx-background-radius: 14;" +
                                "-fx-border-radius: 14;" +
                                "-fx-border-color: #E5E7EB;"
                );


                VBox left = new VBox(4);

                Label title = new Label(safe(p.getNom()));
                title.setStyle("-fx-font-size: 14px; -fx-font-weight: 800;");

                // Get responsable name from allEmployees
                String responsableName = getEmployeeNameById(p.getResponsable_id());
                String subtitleText =
                        "👔 " + responsableName +
                                " • Priorité: " + prettyEnum(p.getPriority());

                Label subtitle = new Label(subtitleText);
                subtitle.setStyle("-fx-opacity: 0.75;");

                left.getChildren().addAll(title, subtitle);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Label badge = new Label(prettyEnum(p.getStatut()));
                badge.setStyle(badgeStyleForStatut(p.getStatut()));

                row.getChildren().addAll(left, spacer, badge);

                VBox wrapper = new VBox(row);
                wrapper.setStyle("-fx-padding: 6 0 6 0;");

                setText(null);
                setGraphic(wrapper);

            }
        });
    }


    private String safe(String s) {
        return (s == null || s.isBlank()) ? "(Sans nom)" : s;
    }

    private String prettyEnum(Object e) {
        if (e == null) return "—";
        String s = e.toString().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private LocalDate safeLocalDate(LocalDate d) {
        return d == null ? LocalDate.MIN : d;
    }

    private String getEmployeeNameById(int employeeId) {
        for (EmployeeInfo emp : allEmployees) {
            if (emp.id() == employeeId) {
                return emp.getFullName();
            }
        }
        return "Inconnu";
    }

    private String badgeStyleForStatut(Object statutObj) {
        String base = "-fx-padding: 6 12; -fx-background-radius: 999; -fx-font-weight: 800;";
        if (statutObj == null) return base + "-fx-background-color: #E5E7EB; -fx-text-fill: #374151;";

        String s = statutObj.toString().toUpperCase();

        if (s.contains("EN_COURS")) return base + "-fx-background-color: #DCFCE7; -fx-text-fill: #166534;";
        if (s.contains("PLANIF")) return base + "-fx-background-color: #DBEAFE; -fx-text-fill: #1D4ED8;";
        if (s.contains("ATTENTE")) return base + "-fx-background-color: #FEF9C3; -fx-text-fill: #854D0E;";
        if (s.contains("TERMINE")) return base + "-fx-background-color: #E5E7EB; -fx-text-fill: #111827;";
        if (s.contains("ANNULE")) return base + "-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B;";

        return base + "-fx-background-color: #E5E7EB; -fx-text-fill: #374151;";
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
    private void openCreateTaskModal() {
        if (selectedProject == null) {
            showError("Aucun projet sélectionné", "Veuillez d'abord sélectionner un projet.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Projet/AjoutTache.fxml"));
            Parent root = loader.load();

            AjoutTacheController c = loader.getController();
            c.setProjectId(selectedProject.getProjet_id());
            c.setProjectName(selectedProject.getNom()); // For AI features
            c.setOnSaved(this::loadTasksForSelectedProject);
            c.loadEmployeesForProject();

            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle("Nouvelle tâche");
            modal.setScene(new Scene(root));
            modal.showAndWait();

        } catch (IOException ex) {
            ex.printStackTrace();
            showError("Erreur", "Impossible d'ouvrir le formulaire de tâche: " + ex.getMessage());
        }
    }

    private void loadTasksForSelectedProject() {
        if (selectedProject == null) {
            masterTasks.clear();
            filteredTasks.clear();
            updateTaskInfo();
            return;
        }

        try {
            List<Tache> allTasks = tacheCRUD.afficher();

            // Filter tasks for the selected project
            List<Tache> projectTasks = allTasks.stream()
                    .filter(t -> t.getId_projet() == selectedProject.getProjet_id())
                    .collect(Collectors.toList());

            // NOTE: Don't filter by employee here - let employees see all project tasks
            // The filtering by "Mes tâches" is done in populateKanbanBoard via the sort dropdown

            masterTasks.setAll(projectTasks);
            applyTaskFilters();

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur chargement tâches", e.getMessage());
        }
    }


    private boolean matchesTaskQuery(Tache t, String q) {
        if (q.isBlank()) return true;
        String titre = t.getTitre() == null ? "" : t.getTitre().toLowerCase();
        String desc = t.getDescription() == null ? "" : t.getDescription().toLowerCase();
        return titre.contains(q) || desc.contains(q);
    }

    private void applyTaskFilters() {
        // Populate the Kanban board with filtered tasks
        populateKanbanBoard();
    }

    private void updateTaskInfo() {
        int total = masterTasks.size();
        long completed = masterTasks.stream()
                .filter(t -> t.getStatut_tache() == statut_t.TERMINEE)
                .count();
        long inProgress = masterTasks.stream()
                .filter(t -> t.getStatut_tache() == statut_t.EN_COURS)
                .count();

        taskTotalLabel.setText(String.valueOf(total));
        taskCompletedLabel.setText(String.valueOf(completed));
        taskInProgressLabel.setText(String.valueOf(inProgress));
        taskInfoLabel.setText(filteredTasks.size() + " tâche(s) affichée(s)");
    }

    // ===================== TASK EDIT / DELETE =====================

    private void openEditTaskModal(Tache tache) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Projet/AjoutTache.fxml"));
            Parent root = loader.load();

            AjoutTacheController c = loader.getController();
            c.setProjectId(selectedProject.getProjet_id());
            c.setOnSaved(this::loadTasksForSelectedProject);
            c.loadEmployeesForProject();
            c.setTacheToEdit(tache); // Set task to edit mode

            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle("Modifier la tâche");
            modal.setScene(new Scene(root));
            modal.showAndWait();

        } catch (IOException ex) {
            ex.printStackTrace();
            showError("Erreur", "Impossible d'ouvrir le formulaire: " + ex.getMessage());
        }
    }

    private void deleteTask(Tache tache) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer cette tâche ?");
        confirm.setContentText("Tâche : " + tache.getTitre() + "\nCette action est irréversible.");

        ButtonType btnYes = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(btnYes, btnNo);

        confirm.showAndWait().ifPresent(type -> {
            if (type == btnYes) {
                try {
                    tacheCRUD.supprimer(tache.getId_tache());
                    loadTasksForSelectedProject();
                    taskInfoLabel.setText("✅ Tâche supprimée.");
                } catch (SQLException e) {
                    e.printStackTrace();
                    showError("Erreur", "Impossible de supprimer la tâche: " + e.getMessage());
                }
            }
        });
    }

    // ===================== GOOGLE CALENDAR INTEGRATION =====================

    /**
     * Handle Google Calendar connection/disconnection
     */
    private void handleGoogleCalendarConnection() {
        if (calendarService.isConnected()) {
            // Show options: disconnect or sync
            showCalendarOptionsDialog();
        } else {
            // Connect to Google Calendar
            connectToGoogleCalendar();
        }
    }

    /**
     * Connect to Google Calendar
     */
    private void connectToGoogleCalendar() {
        Alert loadingAlert = new Alert(Alert.AlertType.INFORMATION);
        loadingAlert.setTitle("Google Calendar");
        loadingAlert.setHeaderText("🔄 Connexion en cours...");
        loadingAlert.setContentText("Une fenêtre de navigateur va s'ouvrir pour l'authentification Google.");
        loadingAlert.show();

        // Run connection in background thread
        new Thread(() -> {
            try {
                boolean connected = calendarService.connect();
                Platform.runLater(() -> {
                    loadingAlert.close();
                    if (connected) {
                        showInfo("Google Calendar", "✅ Connecté avec succès à Google Calendar!");
                        updateCalendarStatusDisplay();
                    } else {
                        showError("Google Calendar", "❌ Échec de la connexion.\n\nAssurez-vous d'avoir placé le fichier credentials.json dans src/main/resources/");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    loadingAlert.close();
                    showError("Erreur Google Calendar", "Impossible de se connecter: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Show calendar options dialog
     */
    private void showCalendarOptionsDialog() {
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle("Google Calendar");
        dialog.setHeaderText("📅 Google Calendar connecté");
        dialog.setContentText("Que souhaitez-vous faire ?");

        ButtonType btnSyncMyTasks = new ButtonType("📋 Sync mes tâches");
        ButtonType btnDisconnect = new ButtonType("🔌 Déconnecter");
        ButtonType btnCancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getButtonTypes().setAll(btnSyncMyTasks, btnDisconnect, btnCancel);

        dialog.showAndWait().ifPresent(type -> {
            if (type == btnSyncMyTasks) {
                showProjectSelectionForSync();
            } else if (type == btnDisconnect) {
                disconnectGoogleCalendar();
            }
        });
    }

    /**
     * Show a dialog to select a project from the user's projects, then sync their tasks
     */
    private void showProjectSelectionForSync() {
        try {
            // Get projects the user is part of
            List<Projet> userProjects;

            if (canManageProjects()) {
                // Managers can see all projects
                userProjects = projetCRUD.afficher();
            } else {
                // Employees only see projects they belong to
                List<Integer> myProjectIds = equipeCRUD.getProjectIdsForEmployee(currentUserId);
                userProjects = projetCRUD.afficher().stream()
                        .filter(p -> myProjectIds.contains(p.getProjet_id()))
                        .collect(Collectors.toList());
            }

            if (userProjects.isEmpty()) {
                showError("Aucun projet", "Vous n'êtes assigné à aucun projet.");
                return;
            }

            // Create dialog with project selection
            Dialog<Projet> projectDialog = new Dialog<>();
            projectDialog.setTitle("Synchroniser avec Google Calendar");
            projectDialog.setHeaderText("📅 Sélectionnez un projet à synchroniser");

            // Create content
            VBox content = new VBox(15);
            content.setPadding(new Insets(20));
            content.setStyle("-fx-background-color: #F8FAFC;");

            Label instructionLabel = new Label("Choisissez un projet pour synchroniser vos tâches assignées:");
            instructionLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");

            ComboBox<Projet> projectCombo = new ComboBox<>();
            projectCombo.getItems().addAll(userProjects);
            projectCombo.setPromptText("Sélectionner un projet...");
            projectCombo.setPrefWidth(300);
            projectCombo.setStyle("-fx-background-radius: 8; -fx-border-radius: 8;");

            // Custom cell factory to show project name nicely
            projectCombo.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(Projet item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText("📁 " + item.getNom() + " (" + prettyEnum(item.getStatut()) + ")");
                    }
                }
            });
            projectCombo.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(Projet item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText("Sélectionner un projet...");
                    } else {
                        setText("📁 " + item.getNom());
                    }
                }
            });

            // Info label to show task count
            Label taskInfoLbl = new Label("");
            taskInfoLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

            projectCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    try {
                        int taskCount = countUserTasksInProject(newVal.getProjet_id(), currentUserId, canManageProjects());
                        taskInfoLbl.setText("📋 " + taskCount + " tâche(s) à synchroniser");
                    } catch (SQLException e) {
                        taskInfoLbl.setText("");
                    }
                }
            });

            content.getChildren().addAll(instructionLabel, projectCombo, taskInfoLbl);

            projectDialog.getDialogPane().setContent(content);
            projectDialog.getDialogPane().getButtonTypes().addAll(
                    new ButtonType("🔄 Synchroniser", ButtonBar.ButtonData.OK_DONE),
                    new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE)
            );

            // Disable sync button until project is selected
            projectDialog.getDialogPane().lookupButton(projectDialog.getDialogPane().getButtonTypes().get(0))
                    .disableProperty().bind(projectCombo.valueProperty().isNull());

            projectDialog.setResultConverter(buttonType -> {
                if (buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                    return projectCombo.getValue();
                }
                return null;
            });

            Optional<Projet> result = projectDialog.showAndWait();
            result.ifPresent(this::syncUserTasksForProject);

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur", "Impossible de charger les projets: " + e.getMessage());
        }
    }

    /**
     * Count tasks assigned to the user in a specific project
     */
    private int countUserTasksInProject(int projectId, int userId, boolean isManager) throws SQLException {
        List<Tache> allTasks = tacheCRUD.afficher();
        return (int) allTasks.stream()
                .filter(t -> t.getId_projet() == projectId)
                .filter(t -> isManager || t.getId_employe() == userId)
                .count();
    }

    /**
     * Sync the user's tasks for a specific project to Google Calendar
     */
    private void syncUserTasksForProject(Projet project) {

        try {
            // Get all tasks for the project
            List<Tache> allTasks = tacheCRUD.afficher();

            // Filter tasks: for managers show all project tasks, for employees only their tasks
            List<Tache> tasksToSync = allTasks.stream()
                    .filter(t -> t.getId_projet() == project.getProjet_id())
                    .filter(t -> canManageProjects() || t.getId_employe() == currentUserId)
                    .collect(Collectors.toList());

            if (tasksToSync.isEmpty()) {
                showInfo("Aucune tâche", "Vous n'avez aucune tâche assignée dans ce projet.");
                return;
            }

            // Show progress dialog
            Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
            progressAlert.setTitle("Synchronisation");
            progressAlert.setHeaderText("🔄 Synchronisation en cours...");
            progressAlert.setContentText("Veuillez patienter...");
            progressAlert.show();

            // Sync in background thread
            new Thread(() -> {
                int synced = 0;
                int failed = 0;

                for (Tache task : tasksToSync) {
                    try {
                        calendarService.createTaskEvent(task, project.getNom());
                        synced++;
                    } catch (Exception e) {
                        failed++;
                        System.err.println("Failed to sync task: " + task.getTitre() + " - " + e.getMessage());
                    }
                }

                final int finalSynced = synced;
                final int finalFailed = failed;

                Platform.runLater(() -> {
                    progressAlert.close();
                    String fullName = currentUserPrenom + " " + currentUserNom;
                    showInfo("Synchronisation terminée",
                            "📅 Synchronisation avec Google Calendar:\n\n" +
                                    "📁 Projet: " + project.getNom() + "\n" +
                                    "👤 Utilisateur: " + fullName.trim() + "\n\n" +
                                    "✅ Tâches synchronisées: " + finalSynced + "\n" +
                                    (finalFailed > 0 ? "❌ Échecs: " + finalFailed : ""));
                });
            }).start();

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur", "Impossible de charger les tâches: " + e.getMessage());
        }
    }

    /**
     * Disconnect from Google Calendar
     */
    private void disconnectGoogleCalendar() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Déconnexion Google Calendar");
        confirm.setHeaderText("🔌 Voulez-vous vous déconnecter de Google Calendar?");
        confirm.setContentText("Vous devrez vous reconnecter pour synchroniser à nouveau.");

        confirm.showAndWait().ifPresent(type -> {
            if (type == ButtonType.OK) {
                calendarService.disconnect();
                updateCalendarStatusDisplay();
                showInfo("Google Calendar", "Déconnecté de Google Calendar.");
            }
        });
    }

    /**
     * Update the calendar status display
     */
    private void updateCalendarStatusDisplay() {
        if (calendarStatusLabel != null) {
            if (calendarService.isConnected()) {
                calendarStatusLabel.setText("✅ Connecté");
                calendarStatusLabel.setStyle("-fx-text-fill: #16A34A; -fx-font-weight: bold;");
            } else {
                calendarStatusLabel.setText("⚪ Non connecté");
                calendarStatusLabel.setStyle("-fx-text-fill: #64748B;");
            }
        }

        if (btnGoogleCalendar != null) {
            if (calendarService.isConnected()) {
                btnGoogleCalendar.setText("📅 Calendar");
                btnGoogleCalendar.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #166534; -fx-background-radius: 8; -fx-cursor: hand;");
            } else {
                btnGoogleCalendar.setText("📅 Connecter Calendar");
                btnGoogleCalendar.setStyle("-fx-background-color: #DBEAFE; -fx-text-fill: #1D4ED8; -fx-background-radius: 8; -fx-cursor: hand;");
            }
        }
    }

    /**
     * Sync a single task to Google Calendar (called when creating/updating a task)
     */
    public void syncTaskToCalendar(Tache task) {
        if (!calendarService.isConnected()) {
            return; // Silent fail if not connected
        }

        String projectName = selectedProject != null ? selectedProject.getNom() : "Projet #" + task.getId_projet();

        new Thread(() -> {
            try {
                calendarService.createTaskEvent(task, projectName);
                Platform.runLater(() -> {
                    taskInfoLabel.setText("✅ Tâche synchronisée avec Google Calendar");
                });
            } catch (Exception e) {
                System.err.println("Failed to sync task to calendar: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Show info dialog
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ===================== CALENDAR TAB =====================

    /**
     * Setup the Calendar tab
     */
    private void setupCalendarTab() {
        if (calendarGrid == null) return;

        // Setup navigation buttons
        if (btnPrevMonth != null) {
            btnPrevMonth.setOnAction(e -> {
                currentYearMonth = currentYearMonth.minusMonths(1);
                refreshCalendar();
            });
        }

        if (btnNextMonth != null) {
            btnNextMonth.setOnAction(e -> {
                currentYearMonth = currentYearMonth.plusMonths(1);
                refreshCalendar();
            });
        }

        if (btnToday != null) {
            btnToday.setOnAction(e -> {
                currentYearMonth = YearMonth.now();
                refreshCalendar();
            });
        }

        // Setup project filter
        if (calendarProjectFilter != null) {
            calendarProjectFilter.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(Projet item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText("Tous les projets");
                    } else {
                        setText("📁 " + item.getNom());
                    }
                }
            });
            calendarProjectFilter.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(Projet item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText("Tous les projets");
                    } else {
                        setText("📁 " + item.getNom());
                    }
                }
            });

            calendarProjectFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
                calendarSelectedProject = newVal;
                refreshCalendar();
            });
        }

        // Setup days header
        setupCalendarDaysHeader();

        // Populate project filter dropdown
        updateCalendarProjectFilter();

        // Initial render
        refreshCalendar();
    }

    /**
     * Setup the days of week header
     */
    private void setupCalendarDaysHeader() {
        if (calendarDaysHeader == null) return;

        calendarDaysHeader.getChildren().clear();
        String[] days = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};

        for (String day : days) {
            Label dayLabel = new Label(day);
            dayLabel.setStyle("-fx-font-weight: 700; -fx-text-fill: #64748B; -fx-font-size: 12px; -fx-font-family: 'Segoe UI', sans-serif;");
            dayLabel.setPrefWidth(150);
            dayLabel.setMaxWidth(Double.MAX_VALUE);
            dayLabel.setAlignment(Pos.CENTER);
            HBox.setHgrow(dayLabel, Priority.ALWAYS);
            calendarDaysHeader.getChildren().add(dayLabel);
        }
    }

    /**
     * Refresh the calendar view
     */
    private void refreshCalendar() {
        if (calendarGrid == null) return;

        // Update month label
        if (currentMonthLabel != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);
            String monthText = currentYearMonth.format(formatter);
            currentMonthLabel.setText(monthText.substring(0, 1).toUpperCase() + monthText.substring(1));
        }

        // Clear grid
        calendarGrid.getChildren().clear();
        calendarGrid.getColumnConstraints().clear();
        calendarGrid.getRowConstraints().clear();

        // Setup column constraints (7 days)
        for (int i = 0; i < 7; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / 7);
            col.setHgrow(Priority.ALWAYS);
            calendarGrid.getColumnConstraints().add(col);
        }

        // Get first day of month and calculate starting position
        LocalDate firstOfMonth = currentYearMonth.atDay(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue(); // Monday = 1, Sunday = 7
        int daysInMonth = currentYearMonth.lengthOfMonth();

        // Calculate rows needed
        int totalCells = (dayOfWeek - 1) + daysInMonth;
        int rows = (int) Math.ceil(totalCells / 7.0);

        // Setup row constraints
        for (int i = 0; i < rows; i++) {
            RowConstraints row = new RowConstraints();
            row.setVgrow(Priority.ALWAYS);
            row.setMinHeight(100);
            calendarGrid.getRowConstraints().add(row);
        }

        // Load tasks for calendar
        List<Tache> calendarTasks = loadTasksForCalendar();

        // Fill in the calendar
        int day = 1;
        LocalDate today = LocalDate.now();

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 7; col++) {
                int cellIndex = row * 7 + col;

                if (cellIndex < dayOfWeek - 1 || day > daysInMonth) {
                    // Empty cell (before first day or after last day)
                    VBox emptyCell = createEmptyCalendarCell();
                    calendarGrid.add(emptyCell, col, row);
                } else {
                    // Day cell
                    LocalDate cellDate = currentYearMonth.atDay(day);
                    boolean isToday = cellDate.equals(today);
                    boolean isPast = cellDate.isBefore(today);

                    // Get tasks for this day
                    List<Tache> dayTasks = calendarTasks.stream()
                            .filter(t -> cellDate.equals(t.getDate_limite()) || cellDate.equals(t.getDate_deb()))
                            .collect(Collectors.toList());

                    VBox dayCell = createCalendarDayCell(day, isToday, isPast, dayTasks, cellDate);
                    calendarGrid.add(dayCell, col, row);

                    day++;
                }
            }
        }
    }

    /**
     * Update the project filter in calendar - call this only when needed (role change, initial load)
     */
    private void updateCalendarProjectFilter() {
        if (calendarProjectFilter == null) return;

        // Save current selection
        Projet currentSelection = calendarProjectFilter.getValue();

        try {
            List<Projet> projects = projetCRUD.afficher();

            // Filter for employees
            if (isEmployee()) {
                List<Integer> myProjectIds = equipeCRUD.getProjectIdsForEmployee(currentUserId);
                projects = projects.stream()
                        .filter(p -> myProjectIds.contains(p.getProjet_id()))
                        .collect(Collectors.toList());
            }

            // Add null option at the beginning for "All projects"
            calendarProjectFilter.getItems().clear();
            calendarProjectFilter.getItems().add(null);
            calendarProjectFilter.getItems().addAll(projects);

            // Restore selection if it still exists in the list
            if (currentSelection != null) {
                for (Projet p : projects) {
                    if (p.getProjet_id() == currentSelection.getProjet_id()) {
                        calendarProjectFilter.setValue(p);
                        calendarSelectedProject = p;
                        break;
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load tasks for the calendar view
     */
    private List<Tache> loadTasksForCalendar() {
        try {
            List<Tache> allTasks = tacheCRUD.afficher();

            // For employees, first filter to only show tasks from projects they belong to
            if (isEmployee()) {
                List<Integer> myProjectIds = equipeCRUD.getProjectIdsForEmployee(currentUserId);
                allTasks = allTasks.stream()
                        .filter(t -> myProjectIds.contains(t.getId_projet()))
                        .collect(Collectors.toList());
            }

            // Filter by project if selected
            if (calendarSelectedProject != null) {
                final int selectedProjectId = calendarSelectedProject.getProjet_id();
                allTasks = allTasks.stream()
                        .filter(t -> t.getId_projet() == selectedProjectId)
                        .collect(Collectors.toList());
            }

            return allTasks;
        } catch (SQLException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Create an empty calendar cell
     */
    private VBox createEmptyCalendarCell() {
        VBox cell = new VBox();
        cell.setStyle("-fx-background-color: #F1F5F9;");
        return cell;
    }

    /**
     * Create a calendar day cell
     */
    private VBox createCalendarDayCell(int day, boolean isToday, boolean isPast, List<Tache> tasks, LocalDate cellDate) {
        VBox cell = new VBox(4);
        cell.setPadding(new Insets(6));
        cell.setStyle(getDayCellStyle(isToday, isPast));

        // Day number header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label dayLabel = new Label(String.valueOf(day));
        String dayStyle = "-fx-font-size: 13px; -fx-font-weight: 600; -fx-font-family: 'Segoe UI', sans-serif;";
        if (isToday) {
            dayStyle += "-fx-text-fill: white; -fx-background-color: #6366F1; -fx-padding: 2 8; -fx-background-radius: 50;";
        } else if (isPast) {
            dayStyle += "-fx-text-fill: #94A3B8;";
        } else {
            dayStyle += "-fx-text-fill: #1E293B;";
        }
        dayLabel.setStyle(dayStyle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().add(dayLabel);
        header.getChildren().add(spacer);

        // Add task button (only for today and future, and ONLY for managers/responsables)
        if (!isPast && canManageProjects()) {
            Button addBtn = new Button("+");
            addBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0 4; -fx-min-width: 20;");
            addBtn.setOnMouseEntered(e -> addBtn.setStyle("-fx-background-color: #6366F1; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0 4; -fx-min-width: 20; -fx-background-radius: 4;"));
            addBtn.setOnMouseExited(e -> addBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0 4; -fx-min-width: 20;"));
            addBtn.setOnAction(e -> openAddTaskForDate(cellDate));
            header.getChildren().add(addBtn);
        }

        cell.getChildren().add(header);

        // Tasks container with scroll
        VBox tasksContainer = new VBox(3);
        tasksContainer.setStyle("-fx-padding: 2 0 0 0;");

        // Show up to 3 tasks, then show "+X more"
        int shown = 0;
        for (Tache task : tasks) {
            if (shown < 3) {
                HBox taskChip = createCalendarTaskChip(task);
                tasksContainer.getChildren().add(taskChip);
                shown++;
            }
        }

        if (tasks.size() > 3) {
            Label moreLabel = new Label("+" + (tasks.size() - 3) + " autres");
            moreLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 10px; -fx-cursor: hand;");
            moreLabel.setOnMouseClicked(e -> showAllTasksForDay(cellDate, tasks));
            tasksContainer.getChildren().add(moreLabel);
        }

        cell.getChildren().add(tasksContainer);
        VBox.setVgrow(tasksContainer, Priority.ALWAYS);

        return cell;
    }

    /**
     * Get the style for a day cell
     */
    private String getDayCellStyle(boolean isToday, boolean isPast) {
        String base = "-fx-background-radius: 0;";
        if (isToday) {
            return base + "-fx-background-color: #EEF2FF; -fx-border-color: #6366F1; -fx-border-width: 2;";
        } else if (isPast) {
            return base + "-fx-background-color: #F8FAFC;";
        }
        return base + "-fx-background-color: white;";
    }

    /**
     * Create a task chip for the calendar
     */
    private HBox createCalendarTaskChip(Tache task) {
        HBox chip = new HBox(4);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setPadding(new Insets(3, 6, 3, 6));
        chip.setStyle(getCalendarTaskChipStyle(task.getPriority_tache()));
        chip.setCursor(javafx.scene.Cursor.HAND);

        // Status indicator
        Label statusDot = new Label(getStatusEmoji(task.getStatut_tache()));
        statusDot.setStyle("-fx-font-size: 8px;");

        // Task title
        Label titleLabel = new Label(truncateText(task.getTitre(), 15));
        titleLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #1E293B; -fx-font-family: 'Segoe UI', sans-serif;");

        chip.getChildren().addAll(statusDot, titleLabel);

        // Click to show details
        chip.setOnMouseClicked(e -> showTaskDetailsPopup(task));

        // Hover effect
        String baseStyle = getCalendarTaskChipStyle(task.getPriority_tache());
        chip.setOnMouseEntered(e -> chip.setStyle(baseStyle + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 4, 0, 0, 1);"));
        chip.setOnMouseExited(e -> chip.setStyle(baseStyle));

        return chip;
    }

    /**
     * Get the style for a calendar task chip based on priority
     */
    private String getCalendarTaskChipStyle(priority p) {
        String base = "-fx-background-radius: 4; -fx-cursor: hand;";
        if (p == null) return base + "-fx-background-color: #F1F5F9;";
        return switch (p) {
            case HAUTE -> base + "-fx-background-color: #FEE2E2;";
            case MOYENNE -> base + "-fx-background-color: #FEF3C7;";
            case BASSE -> base + "-fx-background-color: #DCFCE7;";
        };
    }

    /**
     * Truncate text to a maximum length
     */
    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 2) + "..";
    }

    /**
     * Open add task modal for a specific date
     */
    private void openAddTaskForDate(LocalDate date) {
        // First check if we have a project selected
        Projet projectToUse = calendarSelectedProject;

        if (projectToUse == null) {
            // Ask user to select a project
            List<Projet> availableProjects = new java.util.ArrayList<>();
            try {
                availableProjects = projetCRUD.afficher();
                if (isEmployee()) {
                    List<Integer> myProjectIds = equipeCRUD.getProjectIdsForEmployee(currentUserId);
                    availableProjects = availableProjects.stream()
                            .filter(p -> myProjectIds.contains(p.getProjet_id()))
                            .collect(Collectors.toList());
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            if (availableProjects.isEmpty()) {
                showError("Aucun projet", "Vous n'avez aucun projet disponible.");
                return;
            }

            // Show project selection dialog with proper ComboBox
            Dialog<Projet> dialog = new Dialog<>();
            dialog.setTitle("Sélectionner un projet");
            dialog.setHeaderText("📁 Choisissez un projet pour la nouvelle tâche");

            // Create content
            VBox content = new VBox(12);
            content.setPadding(new Insets(20));
            content.setStyle("-fx-background-color: #F8FAFC;");

            Label label = new Label("Projet:");
            label.setStyle("-fx-font-weight: 600; -fx-text-fill: #475569;");

            ComboBox<Projet> projectCombo = new ComboBox<>();
            projectCombo.getItems().addAll(availableProjects);
            projectCombo.setPrefWidth(300);
            projectCombo.setStyle("-fx-background-radius: 8;");

            // Set cell factory to display project names
            projectCombo.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(Projet item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : "📁 " + item.getNom());
                }
            });
            projectCombo.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(Projet item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "Sélectionner un projet..." : "📁 " + item.getNom());
                }
            });

            // Select first by default
            if (!availableProjects.isEmpty()) {
                projectCombo.setValue(availableProjects.get(0));
            }

            content.getChildren().addAll(label, projectCombo);

            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(
                    new ButtonType("✅ Confirmer", ButtonBar.ButtonData.OK_DONE),
                    new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE)
            );
            dialog.getDialogPane().setStyle("-fx-background-color: #F8FAFC;");

            dialog.setResultConverter(buttonType -> {
                if (buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                    return projectCombo.getValue();
                }
                return null;
            });

            Optional<Projet> result = dialog.showAndWait();
            if (result.isEmpty() || result.get() == null) return;
            projectToUse = result.get();
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Projet/AjoutTache.fxml"));
            Parent root = loader.load();

            AjoutTacheController c = loader.getController();
            c.setProjectId(projectToUse.getProjet_id());
            c.setProjectName(projectToUse.getNom()); // For AI features
            c.setOnSaved(this::refreshCalendar);
            c.loadEmployeesForProject();
            c.setDefaultStartDate(date);
            c.setDefaultEndDate(date);

            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle("Nouvelle tâche - " + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            modal.setScene(new Scene(root));
            modal.showAndWait();

        } catch (IOException ex) {
            ex.printStackTrace();
            showError("Erreur", "Impossible d'ouvrir le formulaire de tâche: " + ex.getMessage());
        }
    }

    /**
     * Show all tasks for a specific day in a popup
     */
    private void showAllTasksForDay(LocalDate date, List<Tache> tasks) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Tâches du " + date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH)));
        dialog.setHeaderText(null);

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #F8FAFC;");
        content.setPrefWidth(400);

        Label titleLabel = new Label("📅 " + tasks.size() + " tâche(s) pour cette date");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #1E293B;");
        content.getChildren().add(titleLabel);

        for (Tache task : tasks) {
            HBox taskRow = new HBox(10);
            taskRow.setAlignment(Pos.CENTER_LEFT);
            taskRow.setPadding(new Insets(10));
            taskRow.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-cursor: hand;");

            Label statusLabel = new Label(getStatusEmoji(task.getStatut_tache()));
            Label nameLabel = new Label(task.getTitre());
            nameLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #1E293B;");

            Label priorityLabel = new Label(getPriorityLabel(task.getPriority_tache()));
            priorityLabel.setStyle(getKanbanPriorityBadgeStyle(task.getPriority_tache()));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            taskRow.getChildren().addAll(statusLabel, nameLabel, spacer, priorityLabel);

            taskRow.setOnMouseClicked(e -> {
                dialog.close();
                showTaskDetailsPopup(task);
            });

            content.getChildren().add(taskRow);
        }

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setStyle("-fx-background-color: #F8FAFC;");
        dialog.showAndWait();
    }

}
