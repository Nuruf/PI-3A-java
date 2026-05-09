package controller.employers.RHetAdminE;

import controller.demandes.NavigationHelper;
import entities.demande.Demande;
import entities.demande.DemandeDetails;
import entities.demande.HistoriqueDemande;
import service.api.AIDocumentGeneratorService;
import service.demande.DemandeCRUD;
import service.demande.DemandeDetailsCRUD;
import service.demande.HistoriqueDemandeCRUD;
import service.employers.employeCRUD;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.util.Optional;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class DemandesController implements Initializable {

    // ═══════════════════════════════════════════════════════════════════════════
    // FXML COMPONENTS - TABLE & PAGINATION
    // ═══════════════════════════════════════════════════════════════════════════

    @FXML private TabPane mainTabPane;
    @FXML private TextField rechercheField;
    @FXML private TableView<Demande> demandesTable;
    @FXML private TableColumn<Demande, String> titreCol;
    @FXML private TableColumn<Demande, String> categorieCol;
    @FXML private TableColumn<Demande, String> typeCol;
    @FXML private TableColumn<Demande, String> prioriteCol;
    @FXML private TableColumn<Demande, String> statusCol;
    @FXML private TableColumn<Demande, Date> dateCol;
    @FXML private TableColumn<Demande, Void> actionsCol;

    // Pagination
    @FXML private VBox tableContainer;
    @FXML private HBox paginationBar;
    @FXML private HBox pageButtonsContainer;
    @FXML private Button btnFirstPage;
    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;
    @FXML private Button btnLastPage;
    @FXML private ComboBox<Integer> pageSizeCombo;
    @FXML private Label pageInfoLabel;

    // ═══════════════════════════════════════════════════════════════════════════
    // FXML COMPONENTS - DETAILS PANEL
    // ═══════════════════════════════════════════════════════════════════════════

    @FXML private VBox placeholderBox;
    @FXML private VBox detailsContent;
    @FXML private Label detailTitreLabel;
    @FXML private Label detailCategorieLabel;
    @FXML private Label detailTypeLabel;
    @FXML private Label detailPrioriteLabel;
    @FXML private Label detailStatusLabel;
    @FXML private Label detailDateLabel;
    @FXML private Label detailDescriptionLabel;
    @FXML private VBox detailSpecificContainer;
    @FXML private VBox historiqueContainer;
    @FXML private Button avancerBtn;

    // ═══════════════════════════════════════════════════════════════════════════
    // FXML COMPONENTS - STATISTICS
    // ═══════════════════════════════════════════════════════════════════════════

    @FXML private Label lblTotalDemandes;
    @FXML private Label lblTotalHistorique;
    @FXML private Label lblDemandesResolues;
    @FXML private Label lblUrgentDemandes;
    @FXML private HBox statutStatsContainer;
    @FXML private HBox prioriteStatsContainer;
    @FXML private HBox typeStatsContainer;
    @FXML private HBox categorieStatsContainer;

    // Performance Stats
    @FXML private Label lblPerformanceScore;
    @FXML private Label lblPerformanceGrade;
    @FXML private Label lblOnTimeRate;
    @FXML private Label lblOnTimeCount;
    @FXML private Label lblTotalProcessed;
    @FXML private Label lblLateRate;
    @FXML private Label lblLateCount;
    @FXML private Label lblAvgResponseTime;
    @FXML private Label lblResponseTimeStatus;
    @FXML private Label lblHighPriorityTime;
    @FXML private Label lblNormalPriorityTime;
    @FXML private Label lblLowPriorityTime;
    @FXML private ProgressBar progressHighPriority;
    @FXML private ProgressBar progressNormalPriority;
    @FXML private ProgressBar progressLowPriority;
    @FXML private Label lblSlaHigh;
    @FXML private Label lblSlaNormal;
    @FXML private Label lblSlaLow;
    @FXML private Label lblPendingCount;

    // ═══════════════════════════════════════════════════════════════════════════
    // SERVICES & DATA
    // ═══════════════════════════════════════════════════════════════════════════

    private DemandeCRUD demandeCRUD;
    private DemandeDetailsCRUD detailsCRUD;
    private HistoriqueDemandeCRUD historiqueCRUD;
    private employeCRUD employeCrud;
    private AIDocumentGeneratorService docService;
    private Demande selectedDemande;
    private Stage loadingStage;

    private List<Demande> allDemandes = new ArrayList<>();
    private List<Demande> filteredDemandes = new ArrayList<>();

    // ═══════════════════════════════════════════════════════════════════════════
    // PAGINATION SETTINGS - FIXED ROW HEIGHT SYSTEM
    // ═══════════════════════════════════════════════════════════════════════════

    private int currentPage = 1;
    private int pageSize = 10;
    private int totalPages = 1;
    private int currentPageItemCount = 0;
    private static final double FIXED_ROW_HEIGHT = 45.0;
    private static final double TABLE_HEADER_HEIGHT = 35.0;
    private static final double PAGINATION_BAR_HEIGHT = 55.0;
    private static final double EXTRA_PADDING = 15.0;
    private static final int MIN_ROWS = 3;
    private static final int MAX_ROWS = 30;
    private boolean needsRecalculation = true;
    private double lastKnownHeight = 0;
    private static final int SLA_HIGH_PRIORITY = 24;
    private static final int SLA_NORMAL_PRIORITY = 48;
    private static final int SLA_LOW_PRIORITY = 72;

    // ═══════════════════════════════════════════════════════════════════════════
    // INITIALIZE
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            System.out.println("═══════════════════════════════════════════════");
            System.out.println("🚀 Initializing DemandesController...");
            System.out.println("═══════════════════════════════════════════════");
            demandeCRUD = new DemandeCRUD();
            detailsCRUD = new DemandeDetailsCRUD();
            historiqueCRUD = new HistoriqueDemandeCRUD();
            employeCrud = new employeCRUD();
            docService = new AIDocumentGeneratorService();
            setupFixedRowHeightTable();
            initializeTableColumns();
            setupDynamicTableSizing();
            loadAllDemandes();
            setupTableSelectionListener();
            setupSearchListener();
            setupTabChangeListener();

            System.out.println("✅ DemandesController initialized!");

        } catch (Exception e) {
            System.err.println("❌ INIT ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FIXED ROW HEIGHT TABLE SETUP - NO SCROLL
    // ═══════════════════════════════════════════════════════════════════════════

    private void setupFixedRowHeightTable() {
        if (demandesTable == null) return;
        demandesTable.setFixedCellSize(FIXED_ROW_HEIGHT);
        demandesTable.setStyle("-fx-background-color: transparent;");
        demandesTable.setFocusTraversable(false);
        Label placeholder = new Label("📋 Aucune demande trouvée");
        placeholder.setStyle("-fx-text-fill: #999; -fx-font-size: 14px;");
        demandesTable.setPlaceholder(placeholder);
        Platform.runLater(() -> {
            forceRemoveScrollBars();
            Platform.runLater(() -> {
                forceRemoveScrollBars();
            });
        });
    }

    private void forceRemoveScrollBars() {
        if (demandesTable == null) return;
        Set<Node> scrollBars = demandesTable.lookupAll(".scroll-bar");
        for (Node node : scrollBars) {
            if (node instanceof ScrollBar) {
                ScrollBar sb = (ScrollBar) node;
                sb.setVisible(false);
                sb.setManaged(false);
                sb.setPrefWidth(0);
                sb.setMaxWidth(0);
                sb.setPrefHeight(0);
                sb.setMaxHeight(0);
                sb.setOpacity(0);
            }
        }
        ScrollBar vBar = (ScrollBar) demandesTable.lookup(".scroll-bar:vertical");
        if (vBar != null) {
            vBar.setVisible(false);
            vBar.setManaged(false);
            vBar.setMaxWidth(0);
            vBar.setPrefWidth(0);
            vBar.setOpacity(0);
        }

        ScrollBar hBar = (ScrollBar) demandesTable.lookup(".scroll-bar:horizontal");
        if (hBar != null) {
            hBar.setVisible(false);
            hBar.setManaged(false);
            hBar.setMaxHeight(0);
            hBar.setPrefHeight(0);
            hBar.setOpacity(0);
        }
        Node virtualFlow = demandesTable.lookup(".virtual-flow");
        if (virtualFlow != null) {
            virtualFlow.setStyle("-fx-background-color: transparent;");
        }
        demandesTable.setStyle(demandesTable.getStyle() +
                "-fx-focus-color: transparent;" +
                "-fx-faint-focus-color: transparent;");
    }

    private void setupDynamicTableSizing() {
        if (demandesTable == null) return;

        if (pageSizeCombo != null) {
            pageSizeCombo.setVisible(false);
            pageSizeCombo.setManaged(false);
        }

        demandesTable.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                setupWindowListeners(newScene);
            }
        });

        demandesTable.heightProperty().addListener((obs, oldH, newH) -> {
            if (newH.doubleValue() > 0 && Math.abs(newH.doubleValue() - lastKnownHeight) > 10) {
                lastKnownHeight = newH.doubleValue();
                Platform.runLater(this::recalculateAndRefresh);
            }
        });

        if (tableContainer != null) {
            tableContainer.heightProperty().addListener((obs, oldH, newH) -> {
                if (newH.doubleValue() > 0) {
                    Platform.runLater(this::recalculateAndRefresh);
                }
            });
        }

        Platform.runLater(() -> {
            Platform.runLater(() -> {
                Platform.runLater(this::recalculateAndRefresh);
            });
        });
    }

    private void setupWindowListeners(Scene scene) {
        scene.windowProperty().addListener((obsW, oldW, newW) -> {
            if (newW instanceof Stage) {
                Stage stage = (Stage) newW;
                stage.heightProperty().addListener((obsH, oldH, newH) -> {
                    needsRecalculation = true;
                    Platform.runLater(this::recalculateAndRefresh);
                });
                stage.maximizedProperty().addListener((obsM, wasMax, isMax) -> {
                    needsRecalculation = true;
                    Platform.runLater(() -> {
                        Platform.runLater(() -> {
                            Platform.runLater(this::recalculateAndRefresh);
                        });
                    });
                });
                stage.widthProperty().addListener((obsW2, oldW2, newW2) -> {
                    Platform.runLater(this::forceRemoveScrollBars);
                });
            }
        });
    }

    private int calculateExactRowCount() {
        double availableHeight = 0;
        if (tableContainer != null && tableContainer.getHeight() > 0) {
            availableHeight = tableContainer.getHeight() - PAGINATION_BAR_HEIGHT;
        } else if (demandesTable != null && demandesTable.getHeight() > 0) {
            availableHeight = demandesTable.getHeight();
        } else {
            return 10;
        }
        availableHeight -= TABLE_HEADER_HEIGHT;
        availableHeight -= EXTRA_PADDING;

        int rows = (int) Math.floor(availableHeight / FIXED_ROW_HEIGHT);

        rows = Math.max(MIN_ROWS, Math.min(rows, MAX_ROWS));

        System.out.println(String.format("📐 Container Height: %.0f | Available: %.0f | Rows: %d",
                tableContainer != null ? tableContainer.getHeight() : 0, availableHeight, rows));

        return rows;
    }

    private void recalculateAndRefresh() {
        int newPageSize = calculateExactRowCount();

        if (newPageSize != pageSize || needsRecalculation) {
            System.out.println("🔄 PageSize changed: " + pageSize + " → " + newPageSize);
            pageSize = newPageSize;
            needsRecalculation = false;

            int totalItems = filteredDemandes.size();
            if (totalItems > 0) {
                totalPages = (int) Math.ceil((double) totalItems / pageSize);
                if (currentPage > totalPages) {
                    currentPage = totalPages;
                }
            }

            refreshTablePage();
        }
        forceRemoveScrollBars();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TABLE COLUMNS INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════

    private void initializeTableColumns() {
        // Titre column
        if (titreCol != null) {
            titreCol.setCellValueFactory(new PropertyValueFactory<>("titre"));
            titreCol.setCellFactory(col -> new TableCell<Demande, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setTooltip(null);
                    } else {
                        String display = item.length() > 22 ? item.substring(0, 19) + "..." : item;
                        setText(display);
                        setTooltip(new Tooltip(item));
                    }
                }
            });
        }

        // Categorie column
        if (categorieCol != null) {
            categorieCol.setCellValueFactory(new PropertyValueFactory<>("categorie"));
            categorieCol.setCellFactory(col -> new TableCell<Demande, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item);
                }
            });
        }

        // Type column
        if (typeCol != null) {
            typeCol.setCellValueFactory(new PropertyValueFactory<>("typeDemande"));
            typeCol.setCellFactory(col -> new TableCell<Demande, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item);
                }
            });
        }

        // Priorite column with badge
        if (prioriteCol != null) {
            prioriteCol.setCellValueFactory(new PropertyValueFactory<>("priorite"));
            prioriteCol.setCellFactory(col -> new TableCell<Demande, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        Label badge = new Label(item);
                        badge.setPadding(new Insets(2, 8, 2, 8));
                        badge.setStyle(getPriorityBadgeStyle(item));
                        setGraphic(badge);
                        setText(null);
                    }
                }
            });
        }

        // Status column with badge
        if (statusCol != null) {
            statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
            statusCol.setCellFactory(col -> new TableCell<Demande, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        Label badge = new Label(item);
                        badge.setPadding(new Insets(2, 8, 2, 8));
                        badge.setStyle(getStatusBadgeStyle(item));
                        setGraphic(badge);
                        setText(null);
                    }
                }
            });
        }

        // Date column
        if (dateCol != null) {
            dateCol.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));
            dateCol.setCellFactory(col -> new TableCell<Demande, Date>() {
                private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

                @Override
                protected void updateItem(Date item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(sdf.format(item));
                    }
                }
            });
        }

        // Actions column
        if (actionsCol != null) {
            actionsCol.setMinWidth(100);
            actionsCol.setPrefWidth(120);
            actionsCol.setCellFactory(col -> new TableCell<Demande, Void>() {
                private final Button editBtn = new Button("✏");
                private final Button deleteBtn = new Button("🗑");
                private final HBox box = new HBox(5, editBtn, deleteBtn);

                {
                    box.setAlignment(Pos.CENTER);
                    box.setPadding(new Insets(2));

                    editBtn.setStyle("-fx-background-color: #4A5DEF; -fx-text-fill: white; " +
                            "-fx-cursor: hand; -fx-background-radius: 4; -fx-padding: 3 8; -fx-font-size: 11;");
                    deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                            "-fx-cursor: hand; -fx-background-radius: 4; -fx-padding: 3 8; -fx-font-size: 11;");

                    editBtn.setMinWidth(30);
                    deleteBtn.setMinWidth(30);

                    editBtn.setOnAction(e -> {
                        int index = getIndex();
                        if (index >= 0 && index < getTableView().getItems().size()) {
                            Demande d = getTableView().getItems().get(index);
                            ouvrirModifier(d);
                        }
                    });

                    deleteBtn.setOnAction(e -> {
                        int index = getIndex();
                        if (index >= 0 && index < getTableView().getItems().size()) {
                            Demande d = getTableView().getItems().get(index);
                            supprimerDemande(d);
                        }
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                        setGraphic(null);
                    } else {
                        setGraphic(box);
                    }
                }
            });
        }
    }

    private String getPriorityBadgeStyle(String priority) {
        String base = "-fx-background-radius: 10; -fx-font-size: 10; -fx-font-weight: bold;";
        if (priority == null) return base + "-fx-background-color: #eee; -fx-text-fill: #666;";

        switch (priority.toUpperCase()) {
            case "HAUTE":
                return base + "-fx-background-color: #fadbd8; -fx-text-fill: #c0392b;";
            case "NORMALE":
                return base + "-fx-background-color: #d4e6f1; -fx-text-fill: #2980b9;";
            case "BASSE":
                return base + "-fx-background-color: #d5f5e3; -fx-text-fill: #27ae60;";
            default:
                return base + "-fx-background-color: #eee; -fx-text-fill: #666;";
        }
    }

    private String getStatusBadgeStyle(String status) {
        String base = "-fx-background-radius: 10; -fx-font-size: 10; -fx-font-weight: bold;";
        if (status == null) return base + "-fx-background-color: #bdc3c7; -fx-text-fill: #2c3e50;";

        switch (status) {
            case "Nouvelle":
                return base + "-fx-background-color: #3498db; -fx-text-fill: white;";
            case "En cours":
                return base + "-fx-background-color: #f39c12; -fx-text-fill: white;";
            case "En attente":
                return base + "-fx-background-color: #e67e22; -fx-text-fill: white;";
            case "Résolue":
                return base + "-fx-background-color: #27ae60; -fx-text-fill: white;";
            case "Fermée":
                return base + "-fx-background-color: #95a5a6; -fx-text-fill: white;";
            default:
                return base + "-fx-background-color: #bdc3c7; -fx-text-fill: #2c3e50;";
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SETUP LISTENERS
    // ═══════════════════════════════════════════════════════════════════════════

    private void setupTableSelectionListener() {
        if (demandesTable != null) {
            demandesTable.getSelectionModel().selectedItemProperty()
                    .addListener((obs, oldVal, newVal) -> {
                        if (newVal != null) {
                            selectedDemande = newVal;
                            showDetails(newVal);
                        }
                    });
        }
    }

    private void setupSearchListener() {
        if (rechercheField != null) {
            rechercheField.textProperty().addListener((obs, oldVal, newVal) -> {
                filterAndRefresh(newVal);
            });
        }
    }

    private void setupTabChangeListener() {
        if (mainTabPane != null) {
            mainTabPane.getSelectionModel().selectedItemProperty()
                    .addListener((obs, oldTab, newTab) -> {
                        if (newTab != null && newTab.getText().contains("Statistiques")) {
                            loadStatistiques();
                            loadPerformanceStats();
                        }
                    });
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DATA LOADING
    // ═══════════════════════════════════════════════════════════════════════════

    private void loadAllDemandes() {
        try {
            allDemandes = demandeCRUD.afficher();
            filteredDemandes = new ArrayList<>(allDemandes);
            currentPage = 1;
            refreshTablePage();
            System.out.println("✅ Loaded " + allDemandes.size() + " demandes");
        } catch (Exception e) {
            System.err.println("❌ Error loading demandes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loadDemandes() {
        loadAllDemandes();
    }

    private void filterAndRefresh(String searchText) {
        resetDetailsPanel();

        if (searchText == null || searchText.trim().isEmpty()) {
            filteredDemandes = new ArrayList<>(allDemandes);
        } else {
            String s = searchText.toLowerCase().trim();
            filteredDemandes = new ArrayList<>();

            for (Demande d : allDemandes) {
                boolean matches = false;
                if (d.getTitre() != null && d.getTitre().toLowerCase().contains(s)) matches = true;
                if (d.getCategorie() != null && d.getCategorie().toLowerCase().contains(s)) matches = true;
                if (d.getTypeDemande() != null && d.getTypeDemande().toLowerCase().contains(s)) matches = true;
                if (d.getStatus() != null && d.getStatus().toLowerCase().contains(s)) matches = true;
                if (d.getPriorite() != null && d.getPriorite().toLowerCase().contains(s)) matches = true;

                if (matches) {
                    filteredDemandes.add(d);
                }
            }
        }

        currentPage = 1;
        refreshTablePage();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PAGINATION - CORE LOGIC
    // ═══════════════════════════════════════════════════════════════════════════

    @FXML
    public void goToFirstPage() {
        if (currentPage != 1) {
            currentPage = 1;
            refreshTablePage();
        }
    }

    @FXML
    public void goToPrevPage() {
        if (currentPage > 1) {
            currentPage--;
            refreshTablePage();
        }
    }

    @FXML
    public void goToNextPage() {
        if (canGoToNextPage()) {
            currentPage++;
            refreshTablePage();
        }
    }

    @FXML
    public void goToLastPage() {
        if (canGoToNextPage() && currentPage < totalPages) {
            currentPage = totalPages;
            refreshTablePage();
        }
    }

    @FXML
    public void onPageSizeChanged(ActionEvent event) {
        // Not used - auto calculated
    }

    private boolean canGoToNextPage() {
        // Can only go next if current page is FULL and there are more pages
        return currentPageItemCount >= pageSize && currentPage < totalPages;
    }

    private boolean canGoToPage(int targetPage) {
        if (targetPage < 1 || targetPage > totalPages || targetPage == currentPage) {
            return false;
        }
        // Can always go backwards
        if (targetPage < currentPage) {
            return true;
        }
        // Forward only if current page is full
        return currentPageItemCount >= pageSize;
    }

    private void goToPage(int page) {
        if (canGoToPage(page)) {
            currentPage = page;
            refreshTablePage();
        }
    }

    private void refreshTablePage() {
        int totalItems = filteredDemandes.size();
        totalPages = Math.max(1, (int) Math.ceil((double) totalItems / pageSize));

        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;

        int fromIndex = (currentPage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalItems);

        if (fromIndex >= totalItems && totalItems > 0) {
            fromIndex = 0;
            toIndex = Math.min(pageSize, totalItems);
            currentPage = 1;
        }

        List<Demande> pageData;
        if (totalItems == 0) {
            pageData = new ArrayList<>();
        } else {
            pageData = new ArrayList<>(filteredDemandes.subList(fromIndex, toIndex));
        }

        currentPageItemCount = pageData.size();

        System.out.println(String.format("📄 Page %d/%d | Items: %d | PageSize: %d | Full: %s",
                currentPage, totalPages, currentPageItemCount, pageSize,
                currentPageItemCount >= pageSize ? "✅" : "❌"));

        // Update table
        if (demandesTable != null) {
            demandesTable.setItems(FXCollections.observableArrayList(pageData));

            double exactHeight = TABLE_HEADER_HEIGHT + (pageSize * FIXED_ROW_HEIGHT) + 2;
            demandesTable.setMinHeight(exactHeight);
            demandesTable.setPrefHeight(exactHeight);
            demandesTable.setMaxHeight(exactHeight);

            // Refresh and remove scrollbars
            demandesTable.refresh();
            Platform.runLater(this::forceRemoveScrollBars);
        }

        updatePaginationUI();
        resetDetailsPanel();
    }

    private void updatePaginationUI() {
        // Info label
        if (pageInfoLabel != null) {
            if (filteredDemandes.isEmpty()) {
                pageInfoLabel.setText("0 résultat");
            } else {
                int start = ((currentPage - 1) * pageSize) + 1;
                int end = start + currentPageItemCount - 1;
                pageInfoLabel.setText(String.format("Page %d/%d • %d-%d sur %d",
                        currentPage, totalPages, start, end, filteredDemandes.size()));
            }
        }

        // Navigation buttons
        boolean isFirst = currentPage <= 1;
        boolean canGoNext = canGoToNextPage();

        if (btnFirstPage != null) btnFirstPage.setDisable(isFirst);
        if (btnPrevPage != null) btnPrevPage.setDisable(isFirst);

        if (btnNextPage != null) {
            btnNextPage.setDisable(!canGoNext);
            if (!canGoNext && currentPageItemCount < pageSize && currentPageItemCount > 0) {
                btnNextPage.setTooltip(new Tooltip(
                        "Page incomplète (" + currentPageItemCount + "/" + pageSize + ")"));
            } else {
                btnNextPage.setTooltip(null);
            }
        }

        if (btnLastPage != null) {
            btnLastPage.setDisable(!canGoNext || currentPage >= totalPages);
        }

        buildPageNumberButtons();
    }

    private void buildPageNumberButtons() {
        if (pageButtonsContainer == null) return;
        pageButtonsContainer.getChildren().clear();

        if (totalPages <= 1) {
            addPageButton(1);
            return;
        }

        int maxButtons = 7;
        int startPage = Math.max(1, currentPage - 3);
        int endPage = Math.min(totalPages, startPage + maxButtons - 1);

        if (endPage - startPage < maxButtons - 1) {
            startPage = Math.max(1, endPage - maxButtons + 1);
        }

        // First page
        if (startPage > 1) {
            addPageButton(1);
            if (startPage > 2) {
                addEllipsis();
            }
        }

        // Page range
        for (int i = startPage; i <= endPage; i++) {
            addPageButton(i);
        }

        // Last page
        if (endPage < totalPages) {
            if (endPage < totalPages - 1) {
                addEllipsis();
            }
            addPageButton(totalPages);
        }
    }

    private void addPageButton(int page) {
        Button btn = new Button(String.valueOf(page));
        btn.setMinWidth(32);
        btn.setMinHeight(32);

        boolean canNavigate = canGoToPage(page);

        if (page == currentPage) {
            // Current page - highlighted
            btn.setStyle("-fx-background-color: #4A5DEF; -fx-text-fill: white; " +
                    "-fx-background-radius: 6; -fx-font-weight: bold;");
            btn.setDisable(true);
        } else if (page < currentPage) {
            // Previous pages - always accessible
            btn.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #333; " +
                    "-fx-background-radius: 6; -fx-cursor: hand;");
            btn.setOnMouseEntered(e -> btn.setStyle(
                    "-fx-background-color: #d0d0d0; -fx-text-fill: #333; -fx-background-radius: 6; -fx-cursor: hand;"));
            btn.setOnMouseExited(e -> btn.setStyle(
                    "-fx-background-color: #f0f0f0; -fx-text-fill: #333; -fx-background-radius: 6; -fx-cursor: hand;"));
            btn.setOnAction(e -> goToPage(page));
        } else if (canNavigate) {
            // Future pages - accessible
            btn.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #333; " +
                    "-fx-background-radius: 6; -fx-cursor: hand;");
            btn.setOnMouseEntered(e -> btn.setStyle(
                    "-fx-background-color: #d0d0d0; -fx-text-fill: #333; -fx-background-radius: 6; -fx-cursor: hand;"));
            btn.setOnMouseExited(e -> btn.setStyle(
                    "-fx-background-color: #f0f0f0; -fx-text-fill: #333; -fx-background-radius: 6; -fx-cursor: hand;"));
            btn.setOnAction(e -> goToPage(page));
        } else {
            // Future pages - not accessible (page not full)
            btn.setStyle("-fx-background-color: #e8e8e8; -fx-text-fill: #bbb; -fx-background-radius: 6;");
            btn.setDisable(true);
            btn.setTooltip(new Tooltip("Remplissez la page actuelle"));
        }

        pageButtonsContainer.getChildren().add(btn);
    }

    private void addEllipsis() {
        Label ellipsis = new Label("...");
        ellipsis.setStyle("-fx-text-fill: #999; -fx-padding: 5 8;");
        pageButtonsContainer.getChildren().add(ellipsis);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DETAILS PANEL
    // ═══════════════════════════════════════════════════════════════════════════

    private void showDetails(Demande d) {
        try {
            // Hide placeholder, show content
            if (placeholderBox != null) {
                placeholderBox.setVisible(false);
                placeholderBox.setManaged(false);
            }
            if (detailsContent != null) {
                detailsContent.setVisible(true);
                detailsContent.setManaged(true);
            }

            // Fill basic info
            if (detailTitreLabel != null) {
                detailTitreLabel.setText(d.getTitre() != null ? d.getTitre() : "Sans titre");
            }
            if (detailCategorieLabel != null) {
                detailCategorieLabel.setText(d.getCategorie() != null ? d.getCategorie() : "N/A");
            }
            if (detailTypeLabel != null) {
                detailTypeLabel.setText(d.getTypeDemande() != null ? d.getTypeDemande() : "N/A");
            }
            if (detailDescriptionLabel != null) {
                detailDescriptionLabel.setText(d.getDescription() != null ? d.getDescription() : "Aucune description");
            }

            // Priority with badge style
            if (detailPrioriteLabel != null) {
                String priorite = d.getPriorite() != null ? d.getPriorite() : "N/A";
                detailPrioriteLabel.setText(priorite);
                detailPrioriteLabel.setStyle(getPriorityBadgeStyle(priorite) + " -fx-padding: 4 12;");
            }

            // Status with badge style
            if (detailStatusLabel != null) {
                String status = d.getStatus() != null ? d.getStatus() : "N/A";
                detailStatusLabel.setText(status);
                detailStatusLabel.setStyle(getStatusBadgeStyle(status) + " -fx-padding: 4 12;");
            }

            // Date
            if (detailDateLabel != null) {
                if (d.getDateCreation() != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy 'à' HH:mm", Locale.FRENCH);
                    detailDateLabel.setText(sdf.format(d.getDateCreation()));
                } else {
                    detailDateLabel.setText("Date non disponible");
                }
            }

            // Load specific details
            loadDemandeDetails(d.getIdDemande());

            // Load historique
            loadHistorique(d.getIdDemande());

            // Update avancer button
            if (avancerBtn != null) {
                boolean isClosed = "Fermée".equals(d.getStatus());
                avancerBtn.setDisable(isClosed);
                if (isClosed) {
                    avancerBtn.setText("✅ Fermée");
                    avancerBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; " +
                            "-fx-background-radius: 6; -fx-padding: 8 15;");
                } else {
                    avancerBtn.setText("⚡ Avancer");
                    avancerBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; " +
                            "-fx-background-radius: 6; -fx-padding: 8 15; -fx-cursor: hand;");
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error showing details: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadDemandeDetails(int idDemande) {
        try {
            if (detailSpecificContainer == null) return;
            detailSpecificContainer.getChildren().clear();

            DemandeDetails details = detailsCRUD.getByDemande(idDemande);

            if (details != null && details.getDetails() != null && !details.getDetails().isEmpty()) {
                String detailsText = details.getDetails();

                // Try to parse as JSON
                try {
                    if (detailsText.trim().startsWith("{")) {
                        org.json.JSONObject json = new org.json.JSONObject(detailsText);
                        for (String key : json.keySet()) {
                            HBox row = new HBox(10);
                            row.setAlignment(Pos.CENTER_LEFT);

                            Label keyLabel = new Label(formatKey(key) + ":");
                            keyLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #666; -fx-min-width: 100;");
                            keyLabel.setMinWidth(100);

                            Label valueLabel = new Label(json.optString(key, "N/A"));
                            valueLabel.setWrapText(true);

                            row.getChildren().addAll(keyLabel, valueLabel);
                            detailSpecificContainer.getChildren().add(row);
                        }
                    } else {
                        // Plain text
                        Label label = new Label(detailsText);
                        label.setWrapText(true);
                        detailSpecificContainer.getChildren().add(label);
                    }
                } catch (Exception jsonEx) {
                    Label label = new Label(detailsText);
                    label.setWrapText(true);
                    detailSpecificContainer.getChildren().add(label);
                }

            } else {
                Label noDetails = new Label("Aucun détail spécifique");
                noDetails.setStyle("-fx-text-fill: #999; -fx-font-style: italic;");
                detailSpecificContainer.getChildren().add(noDetails);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error loading details: " + e.getMessage());
        }
    }

    private String formatKey(String key) {
        if (key == null) return "";
        String formatted = key.replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("_", " ");
        return formatted.substring(0, 1).toUpperCase() + formatted.substring(1);
    }

    private void loadHistorique(int idDemande) {
        try {
            if (historiqueContainer == null) return;
            historiqueContainer.getChildren().clear();

            List<HistoriqueDemande> historiques = historiqueCRUD.getByDemande(idDemande);

            if (historiques != null && !historiques.isEmpty()) {
                for (HistoriqueDemande h : historiques) {
                    VBox item = createHistoriqueItem(h);
                    historiqueContainer.getChildren().add(item);
                }
            } else {
                Label noHist = new Label("Aucun historique");
                noHist.setStyle("-fx-text-fill: #999; -fx-font-style: italic;");
                historiqueContainer.getChildren().add(noHist);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error loading historique: " + e.getMessage());
        }
    }

    private VBox createHistoriqueItem(HistoriqueDemande h) {
        VBox item = new VBox(4);
        item.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 8; " +
                "-fx-background-radius: 6; -fx-border-color: #e0e0e0; -fx-border-radius: 6;");

        // Status change row
        HBox statusRow = new HBox(6);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        Label oldStatus = new Label(h.getAncienStatut());
        oldStatus.setStyle(getStatusBadgeStyle(h.getAncienStatut()) + " -fx-padding: 2 6;");

        Label arrow = new Label("→");
        arrow.setStyle("-fx-font-size: 12; -fx-text-fill: #666;");

        Label newStatus = new Label(h.getNouveauStatut());
        newStatus.setStyle(getStatusBadgeStyle(h.getNouveauStatut()) + " -fx-padding: 2 6;");

        statusRow.getChildren().addAll(oldStatus, arrow, newStatus);

        // Meta info
        String actor = h.getActeur() != null ? h.getActeur() : "Inconnu";
        String dateStr = h.getDateAction() != null ?
                new SimpleDateFormat("dd/MM HH:mm").format(h.getDateAction()) : "N/A";

        Label metaLabel = new Label("👤 " + actor + " • 📅 " + dateStr);
        metaLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #888;");

        item.getChildren().addAll(statusRow, metaLabel);

        // Comment if exists
        if (h.getCommentaire() != null && !h.getCommentaire().isEmpty()) {
            Label comment = new Label("💬 " + h.getCommentaire());
            comment.setStyle("-fx-font-size: 10; -fx-text-fill: #555;");
            comment.setWrapText(true);
            item.getChildren().add(comment);
        }

        return item;
    }

    private void resetDetailsPanel() {
        if (placeholderBox != null) {
            placeholderBox.setVisible(true);
            placeholderBox.setManaged(true);
        }
        if (detailsContent != null) {
            detailsContent.setVisible(false);
            detailsContent.setManaged(false);
        }
        selectedDemande = null;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DOCUMENT GENERATION
    // ═══════════════════════════════════════════════════════════════════════════

    @FXML
    public void generateDocument(ActionEvent event) {
        try {
            if (selectedDemande == null) {
                showAlert(Alert.AlertType.WARNING, "Avertissement",
                        "⚠️ Veuillez sélectionner une demande d'abord !");
                return;
            }

            // Choose folder
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Choisir où sauvegarder les documents");

            String userHome = System.getProperty("user.home");
            File defaultDir = new File(userHome, "Documents");
            if (defaultDir.exists()) {
                chooser.setInitialDirectory(defaultDir);
            }

            File folder = chooser.showDialog(mainTabPane.getScene().getWindow());
            if (folder == null) return;

            // Get employee info
            entities.employers.employe emp = employeCrud.getById(selectedDemande.getIdEmploye());

            if (emp == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "❌ Employé non trouvé !");
                return;
            }

            String nom = emp.getNom() != null ? emp.getNom().trim() : "";
            String prenom = emp.getPrenom() != null ? emp.getPrenom().trim() : "";
            String employeeName = (prenom + " " + nom).trim();
            if (employeeName.isEmpty()) employeeName = "Employe_" + emp.getId_employé();

            String position = emp.getPoste() != null ? emp.getPoste() : "Employé";
            String employeeId = "EMP" + emp.getId_employé();

            // Get additional details - PROPERLY FORMATTED
            StringBuilder additionalInfo = new StringBuilder();

            // Add description if exists
            if (selectedDemande.getDescription() != null && !selectedDemande.getDescription().isEmpty()) {
                additionalInfo.append(selectedDemande.getDescription());
            }

            // Get and format specific details
            try {
                DemandeDetails details = detailsCRUD.getByDemande(selectedDemande.getIdDemande());
                if (details != null && details.getDetails() != null && !details.getDetails().isEmpty()) {
                    String formattedDetails = formatJsonDetails(details.getDetails());
                    if (!formattedDetails.isEmpty()) {
                        if (additionalInfo.length() > 0) {
                            additionalInfo.append("\n\n");
                        }
                        additionalInfo.append(formattedDetails);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error getting details: " + e.getMessage());
            }

            // Show loading
            showLoadingDialog("🔄 Génération en cours...",
                    "Type: " + selectedDemande.getTypeDemande() +
                            "\nEmployé: " + employeeName);

            // Generate document
            final String finalEmployeeName = employeeName;
            final String finalPosition = position;
            final String finalEmployeeId = employeeId;
            final String finalAdditionalInfo = additionalInfo.toString();
            final File finalFolder = folder;

            docService.generateDocumentAsync(
                    selectedDemande.getTypeDemande(),
                    finalEmployeeName,
                    finalPosition,
                    finalEmployeeId,
                    selectedDemande.getDateCreation(),
                    finalAdditionalInfo
            ).thenCompose(doc -> {
                if (doc != null && doc.isValid) {
                    String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    String baseName = sanitizeFilename(doc.title + "_" + finalEmployeeName);

                    String pdfPath = finalFolder.getAbsolutePath() + File.separator +
                            baseName + "_" + timestamp + ".pdf";
                    String wordPath = finalFolder.getAbsolutePath() + File.separator +
                            baseName + "_" + timestamp + ".docx";

                    System.out.println("╔═══════════════════════════════════════════════════════╗");
                    System.out.println("║ 📤 EXPORTING BOTH FORMATS                             ║");
                    System.out.println("╠═══════════════════════════════════════════════════════╣");
                    System.out.println("║ PDF:  " + pdfPath);
                    System.out.println("║ Word: " + wordPath);
                    System.out.println("╚═══════════════════════════════════════════════════════╝");

                    return docService.exportBothFormatsAsync(doc, pdfPath, wordPath)
                            .thenApply(exportResult -> {
                                return new Object[]{doc, exportResult, baseName + "_" + timestamp, finalFolder};
                            });
                } else {
                    return java.util.concurrent.CompletableFuture.completedFuture(null);
                }
            }).thenAccept(result -> {
                hideLoadingDialog();

                Platform.runLater(() -> {
                    if (result != null) {
                        Object[] data = (Object[]) result;
                        AIDocumentGeneratorService.GeneratedDocument doc =
                                (AIDocumentGeneratorService.GeneratedDocument) data[0];
                        AIDocumentGeneratorService.ExportResult exportResult =
                                (AIDocumentGeneratorService.ExportResult) data[1];
                        String baseName = (String) data[2];
                        File folder2 = (File) data[3];

                        System.out.println("╔═══════════════════════════════════════════════════════╗");
                        System.out.println("║ 📊 EXPORT COMPLETED                                   ║");
                        System.out.println("║ " + exportResult);
                        System.out.println("╚═══════════════════════════════════════════════════════╝");

                        if (exportResult.isFullySuccessful()) {
                            showSuccessDialogBoth(folder2, baseName, exportResult);
                        } else if (exportResult.isPartiallySuccessful()) {
                            showPartialSuccessDialog(folder2, baseName, exportResult);
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Erreur d'export",
                                    "❌ Échec de l'export:\n" +
                                            "PDF: " + exportResult.pdfError + "\n" +
                                            "Word: " + exportResult.wordError);
                        }
                    } else {
                        showAlert(Alert.AlertType.WARNING, "Avertissement",
                                "⚠️ Document généré mais export échoué.");
                    }
                });
            }).exceptionally(ex -> {
                hideLoadingDialog();
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "❌ " + ex.getMessage());
                    ex.printStackTrace();
                });
                return null;
            });

        } catch (Exception e) {
            hideLoadingDialog();
            showAlert(Alert.AlertType.ERROR, "Erreur", "❌ " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Formats JSON details into readable text for documents
     */
    private String formatJsonDetails(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return "";
        }

        try {
            String trimmed = jsonString.trim();

            // Check if it's JSON
            if (trimmed.startsWith("{")) {
                org.json.JSONObject json = new org.json.JSONObject(trimmed);
                StringBuilder formatted = new StringBuilder();

                for (String key : json.keySet()) {
                    String value = json.optString(key, "");
                    if (!value.isEmpty()) {
                        String formattedKey = formatJsonKey(key);
                        formatted.append("• ").append(formattedKey).append(": ").append(value).append("\n");
                    }
                }

                return formatted.toString().trim();

            } else if (trimmed.startsWith("[")) {
                // JSON Array
                org.json.JSONArray jsonArray = new org.json.JSONArray(trimmed);
                StringBuilder formatted = new StringBuilder();

                for (int i = 0; i < jsonArray.length(); i++) {
                    Object item = jsonArray.get(i);
                    if (item instanceof org.json.JSONObject) {
                        org.json.JSONObject obj = (org.json.JSONObject) item;
                        for (String key : obj.keySet()) {
                            String value = obj.optString(key, "");
                            if (!value.isEmpty()) {
                                String formattedKey = formatJsonKey(key);
                                formatted.append("• ").append(formattedKey).append(": ").append(value).append("\n");
                            }
                        }
                    } else {
                        formatted.append("• ").append(item.toString()).append("\n");
                    }
                }

                return formatted.toString().trim();
            }

            // Not JSON, return as is
            return jsonString;

        } catch (Exception e) {
            // If JSON parsing fails, return original string
            System.err.println("JSON parsing error: " + e.getMessage());
            return jsonString;
        }
    }

    /**
     * Formats a JSON key into human-readable text
     * Example: "nomLogiciel" -> "Nom du logiciel"
     */
    private String formatJsonKey(String key) {
        if (key == null || key.isEmpty()) return "";

        // Common key translations
        Map<String, String> translations = new HashMap<>();
        translations.put("nomLogiciel", "Nom du logiciel");
        translations.put("version", "Version");
        translations.put("typeLicence", "Type de licence");
        translations.put("justificationLogiciel", "Justification");
        translations.put("dateDebut", "Date de début");
        translations.put("dateFin", "Date de fin");
        translations.put("nombreJours", "Nombre de jours");
        translations.put("typeConge", "Type de congé");
        translations.put("motif", "Motif");
        translations.put("description", "Description");
        translations.put("quantite", "Quantité");
        translations.put("urgence", "Urgence");
        translations.put("justification", "Justification");
        translations.put("nomEquipement", "Nom de l'équipement");
        translations.put("typeEquipement", "Type d'équipement");
        translations.put("marque", "Marque");
        translations.put("modele", "Modèle");
        translations.put("specifications", "Spécifications");
        translations.put("raisonDemande", "Raison de la demande");
        translations.put("dateNecessite", "Date de nécessité");
        translations.put("priorite", "Priorité");
        translations.put("commentaires", "Commentaires");
        translations.put("montant", "Montant");
        translations.put("devise", "Devise");
        translations.put("periode", "Période");
        translations.put("destinataire", "Destinataire");
        translations.put("objetDocument", "Objet du document");

        // Check if we have a translation
        if (translations.containsKey(key)) {
            return translations.get(key);
        }

        // Convert camelCase to readable text
        String result = key
                .replaceAll("([a-z])([A-Z])", "$1 $2")  // camelCase -> camel Case
                .replaceAll("_", " ")                     // snake_case -> snake case
                .trim();

        // Capitalize first letter
        if (!result.isEmpty()) {
            result = result.substring(0, 1).toUpperCase() + result.substring(1).toLowerCase();
        }

        return result;
    }

    private void showSuccessDialogBoth(File folder, String baseName,
                                       AIDocumentGeneratorService.ExportResult result) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès ✅");
        alert.setHeaderText("Documents générés avec succès !");

        StringBuilder content = new StringBuilder();
        content.append("Les fichiers ont été créés:\n\n");

        if (result.pdfFile != null) {
            content.append("📄 PDF: ").append(result.pdfFile.getName())
                    .append(" (").append(formatFileSize(result.pdfFile.length())).append(")\n");
        }

        if (result.wordFile != null) {
            content.append("📝 Word: ").append(result.wordFile.getName())
                    .append(" (").append(formatFileSize(result.wordFile.length())).append(")\n");
        }

        content.append("\n📂 ").append(folder.getAbsolutePath());

        alert.setContentText(content.toString());

        ButtonType openFolder = new ButtonType("📂 Ouvrir le dossier");
        ButtonType close = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(openFolder, close);

        alert.showAndWait().ifPresent(response -> {
            if (response == openFolder) {
                try {
                    if (java.awt.Desktop.isDesktopSupported()) {
                        new Thread(() -> {
                            try {
                                java.awt.Desktop.getDesktop().open(folder);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void showPartialSuccessDialog(File folder, String baseName,
                                          AIDocumentGeneratorService.ExportResult result) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Export Partiel ⚠️");
        alert.setHeaderText("Un seul fichier a été créé");

        StringBuilder content = new StringBuilder();

        if (result.pdfSuccess) {
            content.append("✅ PDF créé: ").append(result.pdfFile.getName()).append("\n");
            content.append("❌ Word échoué: ").append(result.wordError).append("\n");
        } else if (result.wordSuccess) {
            content.append("❌ PDF échoué: ").append(result.pdfError).append("\n");
            content.append("✅ Word créé: ").append(result.wordFile.getName()).append("\n");
        }

        content.append("\n📂 ").append(folder.getAbsolutePath());

        alert.setContentText(content.toString());
        alert.showAndWait();
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private void showLoadingDialog(String title, String message) {
        Platform.runLater(() -> {
            try {
                loadingStage = new Stage();
                loadingStage.initModality(Modality.APPLICATION_MODAL);
                loadingStage.initStyle(StageStyle.TRANSPARENT);
                loadingStage.setResizable(false);

                VBox content = new VBox(20);
                content.setAlignment(Pos.CENTER);
                content.setPadding(new Insets(40));
                content.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 5);");

                ProgressIndicator progress = new ProgressIndicator();
                progress.setPrefSize(60, 60);

                Label titleLabel = new Label(title);
                titleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

                Label messageLabel = new Label(message);
                messageLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #666;");
                messageLabel.setWrapText(true);
                messageLabel.setMaxWidth(280);

                content.getChildren().addAll(progress, titleLabel, messageLabel);

                Scene scene = new Scene(content);
                scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
                loadingStage.setScene(scene);

                if (mainTabPane != null && mainTabPane.getScene() != null) {
                    Stage parent = (Stage) mainTabPane.getScene().getWindow();
                    loadingStage.initOwner(parent);
                    loadingStage.setX(parent.getX() + (parent.getWidth() - 380) / 2);
                    loadingStage.setY(parent.getY() + (parent.getHeight() - 200) / 2);
                }

                loadingStage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void hideLoadingDialog() {
        Platform.runLater(() -> {
            if (loadingStage != null) {
                loadingStage.close();
                loadingStage = null;
            }
        });
    }

    private void showSuccessDialog(File folder, String baseName) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès ✅");
        alert.setHeaderText("Documents générés avec succès !");
        alert.setContentText("📄 " + baseName + ".pdf\n" +
                "📄 " + baseName + ".docx\n\n" +
                "📂 " + folder.getAbsolutePath());

        ButtonType openFolder = new ButtonType("📂 Ouvrir le dossier");
        ButtonType close = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(openFolder, close);

        alert.showAndWait().ifPresent(response -> {
            if (response == openFolder) {
                try {
                    if (java.awt.Desktop.isDesktopSupported()) {
                        new Thread(() -> {
                            try {
                                java.awt.Desktop.getDesktop().open(folder);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private String sanitizeFilename(String s) {
        if (s == null || s.isEmpty()) return "document";

        String clean = s.replaceAll("[^a-zA-Z0-9àâäéèêëïîôùûüçÀÂÄÉÈÊËÏÎÔÙÛÜÇ\\s_-]", "")
                .replaceAll("\\s+", "_")
                .trim();

        if (clean.isEmpty()) return "document";
        return clean.length() > 50 ? clean.substring(0, 50) : clean;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════════════════════

    @FXML
    public void refreshStatistiques() {
        loadStatistiques();
        loadPerformanceStats();
    }

    private void loadStatistiques() {
        try {
            int total = allDemandes.size();
            int resolues = 0;
            int urgent = 0;

            for (Demande d : allDemandes) {
                String status = d.getStatus();
                String priority = d.getPriorite();

                if ("Résolue".equals(status) || "Fermée".equals(status)) {
                    resolues++;
                }

                if ("HAUTE".equalsIgnoreCase(priority) &&
                        !"Fermée".equals(status) && !"Résolue".equals(status)) {
                    urgent++;
                }
            }

            // Update labels
            if (lblTotalDemandes != null) lblTotalDemandes.setText(String.valueOf(total));
            if (lblDemandesResolues != null) lblDemandesResolues.setText(String.valueOf(resolues));
            if (lblUrgentDemandes != null) lblUrgentDemandes.setText(String.valueOf(urgent));

            try {
                if (lblTotalHistorique != null) {
                    lblTotalHistorique.setText(String.valueOf(historiqueCRUD.countAll()));
                }
            } catch (Exception e) {
                if (lblTotalHistorique != null) lblTotalHistorique.setText("0");
            }

            // Load stats containers
            loadStatsByField(statutStatsContainer, "status");
            loadStatsByField(prioriteStatsContainer, "priorite");
            loadStatsByField(typeStatsContainer, "type");
            loadStatsByField(categorieStatsContainer, "categorie");

        } catch (Exception e) {
            System.err.println("❌ Error loading statistics: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadStatsByField(HBox container, String field) {
        if (container == null) return;
        container.getChildren().clear();

        Map<String, Integer> stats = new LinkedHashMap<>();

        for (Demande d : allDemandes) {
            String value = "";
            switch (field) {
                case "status":
                    value = d.getStatus();
                    break;
                case "priorite":
                    value = d.getPriorite();
                    break;
                case "type":
                    value = d.getTypeDemande();
                    break;
                case "categorie":
                    value = d.getCategorie();
                    break;
            }

            if (value != null && !value.isEmpty()) {
                stats.merge(value, 1, Integer::sum);
            }
        }

        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            VBox box = new VBox(5);
            box.setAlignment(Pos.CENTER);
            box.setStyle("-fx-background-color: white; -fx-padding: 15; " +
                    "-fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 5, 0, 0, 1);");
            box.setMinWidth(80);

            Label countLabel = new Label(String.valueOf(entry.getValue()));
            countLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #4A5DEF;");

            Label nameLabel = new Label(entry.getKey());
            nameLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #888;");
            nameLabel.setWrapText(true);
            nameLabel.setMaxWidth(80);

            box.getChildren().addAll(countLabel, nameLabel);
            HBox.setHgrow(box, Priority.ALWAYS);
            container.getChildren().add(box);
        }

        if (stats.isEmpty()) {
            Label empty = new Label("Aucune donnée");
            empty.setStyle("-fx-text-fill: #999;");
            container.getChildren().add(empty);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PERFORMANCE STATISTICS
    // ═══════════════════════════════════════════════════════════════════════════

    private void loadPerformanceStats() {
        try {
            int onTime = 0;
            int late = 0;
            int pending = 0;
            long totalResponseTime = 0;
            int responseCount = 0;

            // SLA tracking per priority
            int highTotal = 0, highOnTime = 0;
            int normalTotal = 0, normalOnTime = 0;
            int lowTotal = 0, lowOnTime = 0;

            long highTotalTime = 0, normalTotalTime = 0, lowTotalTime = 0;
            int highCount = 0, normalCount = 0, lowCount = 0;

            Date now = new Date();

            for (Demande d : allDemandes) {
                String status = d.getStatus();
                String priority = d.getPriorite() != null ? d.getPriorite().toUpperCase() : "NORMALE";
                int slaHours = getSlaHours(priority);

                // Get historique for this demande
                List<HistoriqueDemande> hist = null;
                try {
                    hist = historiqueCRUD.getByDemande(d.getIdDemande());
                } catch (Exception e) {
                    // Ignore
                }

                if ("Nouvelle".equals(status) || "En attente".equals(status)) {
                    // Pending demande
                    pending++;

                    // Check if already late
                    if (d.getDateCreation() != null) {
                        long hoursWaiting = getHoursBetween(d.getDateCreation(), now);
                        if (hoursWaiting > slaHours) {
                            late++;
                        }
                    }

                } else if (hist != null && !hist.isEmpty()) {
                    // Processed demande - find first action time
                    HistoriqueDemande firstAction = hist.get(hist.size() - 1); // Oldest (last in list)

                    if (d.getDateCreation() != null && firstAction.getDateAction() != null) {
                        long responseHours = getHoursBetween(d.getDateCreation(), firstAction.getDateAction());
                        totalResponseTime += responseHours;
                        responseCount++;

                        // Check SLA compliance
                        boolean isOnTime = responseHours <= slaHours;

                        if (isOnTime) {
                            onTime++;
                        } else {
                            late++;
                        }

                        // Track by priority
                        switch (priority) {
                            case "HAUTE":
                                highTotal++;
                                highTotalTime += responseHours;
                                highCount++;
                                if (isOnTime) highOnTime++;
                                break;
                            case "BASSE":
                                lowTotal++;
                                lowTotalTime += responseHours;
                                lowCount++;
                                if (isOnTime) lowOnTime++;
                                break;
                            default:
                                normalTotal++;
                                normalTotalTime += responseHours;
                                normalCount++;
                                if (isOnTime) normalOnTime++;
                                break;
                        }
                    }
                }
            }

            // Calculate metrics
            int totalProcessed = onTime + late;
            double onTimeRate = totalProcessed > 0 ? (double) onTime / totalProcessed * 100 : 100;
            double lateRate = totalProcessed > 0 ? (double) late / totalProcessed * 100 : 0;
            double avgResponseTime = responseCount > 0 ? (double) totalResponseTime / responseCount : 0;

            // Calculate performance score
            double resolutionRate = allDemandes.size() > 0 ?
                    (double) totalProcessed / allDemandes.size() * 100 : 0;

            double speedBonus = avgResponseTime <= 24 ? 100 :
                    Math.max(0, 100 - (avgResponseTime - 24) * 2);

            double performanceScore = (onTimeRate * 0.6) + (resolutionRate * 0.3) + (speedBonus * 0.1);

            // Average times per priority
            long highAvg = highCount > 0 ? highTotalTime / highCount : 0;
            long normalAvg = normalCount > 0 ? normalTotalTime / normalCount : 0;
            long lowAvg = lowCount > 0 ? lowTotalTime / lowCount : 0;

            // Update UI
            updatePerformanceUI(
                    performanceScore, onTimeRate, lateRate,
                    onTime, late, totalProcessed, avgResponseTime, pending,
                    highOnTime, highTotal, normalOnTime, normalTotal, lowOnTime, lowTotal,
                    highAvg, normalAvg, lowAvg
            );

        } catch (Exception e) {
            System.err.println("❌ Error loading performance stats: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int getSlaHours(String priority) {
        switch (priority) {
            case "HAUTE":
                return SLA_HIGH_PRIORITY;
            case "BASSE":
                return SLA_LOW_PRIORITY;
            default:
                return SLA_NORMAL_PRIORITY;
        }
    }

    private long getHoursBetween(Date start, Date end) {
        if (start == null || end == null) return 0;
        long diffMillis = end.getTime() - start.getTime();
        return TimeUnit.MILLISECONDS.toHours(diffMillis);
    }

    private void updatePerformanceUI(
            double score, double onTimeRate, double lateRate,
            int onTime, int late, int totalProcessed, double avgTime, int pending,
            int highOnTime, int highTotal, int normalOnTime, int normalTotal,
            int lowOnTime, int lowTotal, long highAvg, long normalAvg, long lowAvg) {

        // Performance score
        if (lblPerformanceScore != null) {
            lblPerformanceScore.setText(String.format("%.0f%%", score));
        }

        // Grade
        if (lblPerformanceGrade != null) {
            String grade;
            if (score >= 90) grade = "⭐ Excellent";
            else if (score >= 75) grade = "👍 Très Bien";
            else if (score >= 60) grade = "✓ Bien";
            else if (score >= 40) grade = "⚠️ À Améliorer";
            else grade = "❌ Critique";
            lblPerformanceGrade.setText(grade);
        }

        // On-time rate
        if (lblOnTimeRate != null) {
            lblOnTimeRate.setText(String.format("%.0f%%", onTimeRate));
            lblOnTimeRate.setStyle("-fx-font-size: 42; -fx-font-weight: bold; " +
                    "-fx-text-fill: " + (onTimeRate >= 80 ? "#27ae60" : onTimeRate >= 60 ? "#f39c12" : "#e74c3c") + ";");
        }
        if (lblOnTimeCount != null) lblOnTimeCount.setText(String.valueOf(onTime));
        if (lblTotalProcessed != null) lblTotalProcessed.setText(String.valueOf(totalProcessed));

        // Late rate
        if (lblLateRate != null) {
            lblLateRate.setText(String.format("%.0f%%", lateRate));
        }
        if (lblLateCount != null) lblLateCount.setText(String.valueOf(late));

        // Average response time
        if (lblAvgResponseTime != null) {
            if (avgTime < 24) {
                lblAvgResponseTime.setText(String.format("%.0fh", avgTime));
            } else {
                lblAvgResponseTime.setText(String.format("%.1fj", avgTime / 24));
            }
        }

        if (lblResponseTimeStatus != null) {
            String status;
            if (avgTime <= 24) status = "🟢 Excellent";
            else if (avgTime <= 48) status = "🟡 Acceptable";
            else status = "🔴 Lent";
            lblResponseTimeStatus.setText(status);
        }

        // Priority times
        if (lblHighPriorityTime != null) {
            lblHighPriorityTime.setText(highAvg + "h (SLA: " + SLA_HIGH_PRIORITY + "h)");
        }
        if (lblNormalPriorityTime != null) {
            lblNormalPriorityTime.setText(normalAvg + "h (SLA: " + SLA_NORMAL_PRIORITY + "h)");
        }
        if (lblLowPriorityTime != null) {
            lblLowPriorityTime.setText(lowAvg + "h (SLA: " + SLA_LOW_PRIORITY + "h)");
        }

        // Progress bars
        if (progressHighPriority != null) {
            progressHighPriority.setProgress(Math.min(1.0, (double) highAvg / SLA_HIGH_PRIORITY));
        }
        if (progressNormalPriority != null) {
            progressNormalPriority.setProgress(Math.min(1.0, (double) normalAvg / SLA_NORMAL_PRIORITY));
        }
        if (progressLowPriority != null) {
            progressLowPriority.setProgress(Math.min(1.0, (double) lowAvg / SLA_LOW_PRIORITY));
        }

        // SLA compliance
        if (lblSlaHigh != null) {
            double sla = highTotal > 0 ? (double) highOnTime / highTotal * 100 : 100;
            lblSlaHigh.setText(String.format("%.0f%%", sla));
            lblSlaHigh.setStyle("-fx-font-size: 18; -fx-font-weight: bold; " +
                    "-fx-text-fill: " + (sla >= 80 ? "#27ae60" : "#e74c3c") + ";");
        }

        if (lblSlaNormal != null) {
            double sla = normalTotal > 0 ? (double) normalOnTime / normalTotal * 100 : 100;
            lblSlaNormal.setText(String.format("%.0f%%", sla));
            lblSlaNormal.setStyle("-fx-font-size: 18; -fx-font-weight: bold; " +
                    "-fx-text-fill: " + (sla >= 80 ? "#27ae60" : "#e74c3c") + ";");
        }

        if (lblSlaLow != null) {
            double sla = lowTotal > 0 ? (double) lowOnTime / lowTotal * 100 : 100;
            lblSlaLow.setText(String.format("%.0f%%", sla));
            lblSlaLow.setStyle("-fx-font-size: 18; -fx-font-weight: bold; " +
                    "-fx-text-fill: " + (sla >= 80 ? "#27ae60" : "#e74c3c") + ";");
        }

        if (lblPendingCount != null) {
            lblPendingCount.setText(String.valueOf(pending));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ACTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    // ═══════════════════════════════════════════════════════════════════════════
// ACTIONS - FIXED NAVIGATION
// ═══════════════════════════════════════════════════════════════════════════

    // ═══════════════════════════════════════════════════════════════════════════
// ACTIONS - FIXED NAVIGATION (Open in New Window)
// ═══════════════════════════════════════════════════════════════════════════


    private void openAvancerInNewWindow(Demande demande) {
        try {
            System.out.println("Opening avancer demande in new window...");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/emp/RHetAdminE/avancer-demande.fxml"));
            Parent root = loader.load();

            AvancerDemandeController ctrl = loader.getController();
            ctrl.setParentController(this);
            ctrl.setDemande(demande);

            Stage stage = new Stage();
            stage.setTitle("⚡ Avancer la Demande: " + demande.getTitre());
            stage.setScene(new Scene(root, 750, 650));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(true);
            stage.setMinWidth(650);
            stage.setMinHeight(550);

            // Refresh list when window closes
            stage.setOnHidden(event -> {
                System.out.println("Avancer window closed, refreshing list...");
                loadAllDemandes();
                resetDetailsPanel();
            });

            stage.show();
            System.out.println("Avancer window opened successfully!");

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger le formulaire: " + e.getMessage());
        }
    }

    // Add this helper method to your DemandesController class
    private StackPane findContentArea() {
        if (demandesTable != null && demandesTable.getScene() != null) {
            Node node = demandesTable.getScene().lookup("#contentArea");
            if (node instanceof StackPane) {
                return (StackPane) node;
            }
        }
        return null;
    }

    @FXML
    public void ouvrirAvancer() {
        try {
            if (selectedDemande == null) {
                showAlert(Alert.AlertType.WARNING, "Avertissement", "⚠️ Sélectionnez une demande d'abord !");
                return;
            }

            // Load the view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/emp/RHetAdminE/avancer-demande.fxml"));
            Parent view = loader.load();

            // Pass data to the controller
            AvancerDemandeController ctrl = loader.getController();
            ctrl.setParentController(this);
            ctrl.setDemande(selectedDemande);

            // Switch view inside contentArea
            StackPane area = findContentArea();
            if (area != null) {
                area.getChildren().setAll(view);
            } else {
                // Fallback if contentArea ID isn't found
                openAvancerInNewWindow(selectedDemande);
            }

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "❌ " + e.getMessage());
        }
    }

    private void ouvrirModifier(Demande demande) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/emp/RHetAdminE/modifier-demande.fxml"));
            Parent view = loader.load();

            ModifierDemandeController ctrl = loader.getController();
            ctrl.setParentController(this);
            ctrl.setDemande(demande);

            StackPane area = findContentArea();
            if (area != null) {
                area.getChildren().setAll(view);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "❌ " + e.getMessage());
        }
    }

    private void supprimerDemande(Demande demande) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de suppression");
        confirm.setHeaderText("⚠️ Supprimer cette demande ?");
        confirm.setContentText("Voulez-vous vraiment supprimer \"" + demande.getTitre() + "\" ?\n\n" +
                "Cette action est irréversible et supprimera également:\n" +
                "• L'historique de la demande\n" +
                "• Les détails spécifiques");

        ButtonType btnOui = new ButtonType("Oui, supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNon = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(btnOui, btnNon);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == btnOui) {
            try {
                // Delete in correct order (child tables first)
                historiqueCRUD.supprimerByDemande(demande.getIdDemande());
                detailsCRUD.supprimerByDemande(demande.getIdDemande());
                demandeCRUD.supprimer(demande.getIdDemande());

                loadAllDemandes();
                resetDetailsPanel();

                showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "✅ Demande supprimée avec succès !");

            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "❌ " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}