package controller.employers.RHetAdminE;

import entities.demande.Demande;
import entities.demande.HistoriqueDemande;
import entities.employers.employe;
import entities.employers.session;
import service.api.EmailService;
import service.demande.DemandeCRUD;
import service.demande.HistoriqueDemandeCRUD;
import service.employers.employeCRUD;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;

public class AvancerDemandeController implements Initializable {

    // ═══════════════════════════════════════════════════════════════════════════
    // FXML FIELDS
    // ═══════════════════════════════════════════════════════════════════════════

    @FXML private Label infoTitreLabel;
    @FXML private Label infoTypeLabel;
    @FXML private Label infoPrioriteLabel;
    @FXML private Label infoStatusLabel;
    @FXML private HBox statusFlowContainer;

    @FXML private ComboBox<String> nouveauStatutCombo;
    @FXML private Label acteurLabel;
    @FXML private TextArea commentaireArea;
    @FXML private Label statutError;
    @FXML private Label commentaireError;

    @FXML private TableView<HistoriqueDemande> historiqueTable;
    @FXML private TableColumn<HistoriqueDemande, String> ancienStatutCol;
    @FXML private TableColumn<HistoriqueDemande, String> nouveauStatutCol;
    @FXML private TableColumn<HistoriqueDemande, String> acteurCol;
    @FXML private TableColumn<HistoriqueDemande, Date> dateActionCol;
    @FXML private TableColumn<HistoriqueDemande, String> commentaireCol;

    @FXML private Button submitBtn;
    @FXML private Button cancelBtn;

    // ═══════════════════════════════════════════════════════════════════════════
    // INSTANCE VARIABLES
    // ═══════════════════════════════════════════════════════════════════════════

    private DemandeCRUD demandeCRUD;
    private HistoriqueDemandeCRUD historiqueCRUD;
    private employeCRUD employeCrud;
    private EmailService emailService;
    private DemandesController parentController;
    private Demande currentDemande;

    private String connectedActeurRole;
    private String connectedActeurFullName;

    private static final List<String> STATUS_ORDER = Arrays.asList(
            "Nouvelle", "En cours", "En attente", "Résolue", "Fermée"
    );

    // ═══════════════════════════════════════════════════════════════════════════
    // SETTERS
    // ═══════════════════════════════════════════════════════════════════════════

    public void setParentController(DemandesController parentController) {
        this.parentController = parentController;
    }

    public void setDemande(Demande demande) {
        this.currentDemande = demande;
        if (demande != null) {
            fillDemandeInfo(demande);
            buildStatusFlow(demande.getStatus());
            setupAllowedStatuses(demande.getStatus());
            loadHistorique(demande.getIdDemande());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("=== AvancerDemandeController.initialize() ===");

        // Initialize services
        demandeCRUD = new DemandeCRUD();
        historiqueCRUD = new HistoriqueDemandeCRUD();
        employeCrud = new employeCRUD();
        emailService = new EmailService();

        // Setup
        detectConnectedUser();
        initializeHistoriqueTable();
        setupRealtimeValidation();

        System.out.println("═══════════════════════════════════════════════");
        System.out.println("📧 Email Service Status: " + (emailService.isEnabled() ? "✅ ENABLED" : "❌ DISABLED"));
        System.out.println("═══════════════════════════════════════════════");

        System.out.println("AvancerDemandeController initialized!");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // USER DETECTION
    // ═══════════════════════════════════════════════════════════════════════════

    private void detectConnectedUser() {
        connectedActeurRole = "RH";
        connectedActeurFullName = "Utilisateur";

        try {
            employe emp = session.getEmploye();
            if (emp != null) {
                String nom = emp.getNom() != null ? emp.getNom() : "";
                String prenom = emp.getPrenom() != null ? emp.getPrenom() : "";
                connectedActeurFullName = (prenom + " " + nom).trim();

                if (connectedActeurFullName.isEmpty()) {
                    connectedActeurFullName = "Utilisateur #" + emp.getId_employé();
                }

                if (emp.getRole() != null) {
                    String roleStr = emp.getRole().toString().toUpperCase();
                    if (roleStr.contains("RH")) {
                        connectedActeurRole = "RH";
                    } else if (roleStr.contains("ADMIN")) {
                        connectedActeurRole = "ADMIN";
                    } else if (roleStr.contains("CHEF")) {
                        connectedActeurRole = "CHEF PROJET";
                    } else {
                        connectedActeurRole = emp.getRole().getLibelle();
                    }
                }
            } else if (session.getCompte() != null) {
                String email = session.getCompte().getE_mail();
                if (email != null && !email.isEmpty()) {
                    connectedActeurFullName = email;
                }
            }
        } catch (Exception e) {
            System.out.println("Role detection: " + e.getMessage());
        }

        updateActeurLabel();
    }

    private void updateActeurLabel() {
        if (acteurLabel == null) return;

        String roleIcon = "👤";
        String roleColor = "#8e44ad";
        String bgColor = "#f4ecf7";

        if (connectedActeurRole.contains("ADMIN")) {
            roleIcon = "🔧";
            roleColor = "#c0392b";
            bgColor = "#fadbd8";
        } else if (connectedActeurRole.contains("CHEF")) {
            roleIcon = "👔";
            roleColor = "#2980b9";
            bgColor = "#d4e6f1";
        }

        acteurLabel.setText(roleIcon + " " + connectedActeurFullName + "  —  " + connectedActeurRole);
        acteurLabel.setStyle(
                "-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: " + roleColor + ";"
                        + " -fx-background-color: " + bgColor + ";"
                        + " -fx-padding: 10 15; -fx-background-radius: 8;"
        );
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TABLE INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════

    private void initializeHistoriqueTable() {
        if (ancienStatutCol != null) {
            ancienStatutCol.setCellValueFactory(new PropertyValueFactory<>("ancienStatut"));
            ancienStatutCol.setCellFactory(col -> new TableCell<HistoriqueDemande, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        Label badge = new Label(item);
                        badge.setStyle(getStatusBadgeStyle(item));
                        setGraphic(badge);
                    }
                }
            });
        }

        if (nouveauStatutCol != null) {
            nouveauStatutCol.setCellValueFactory(new PropertyValueFactory<>("nouveauStatut"));
            nouveauStatutCol.setCellFactory(col -> new TableCell<HistoriqueDemande, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        Label badge = new Label(item);
                        badge.setStyle(getStatusBadgeStyle(item));
                        setGraphic(badge);
                    }
                }
            });
        }

        if (acteurCol != null) {
            acteurCol.setCellValueFactory(new PropertyValueFactory<>("acteur"));
        }

        if (dateActionCol != null) {
            dateActionCol.setCellValueFactory(new PropertyValueFactory<>("dateAction"));
            dateActionCol.setCellFactory(col -> new TableCell<HistoriqueDemande, Date>() {
                @Override
                protected void updateItem(Date item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
                        setText(sdf.format(item));
                    }
                }
            });
        }

        if (commentaireCol != null) {
            commentaireCol.setCellValueFactory(new PropertyValueFactory<>("commentaire"));
            commentaireCol.setCellFactory(col -> new TableCell<HistoriqueDemande, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setTooltip(null);
                    } else {
                        String display = item.length() > 30 ? item.substring(0, 27) + "..." : item;
                        setText(display);
                        if (item.length() > 30) {
                            setTooltip(new Tooltip(item));
                        }
                    }
                }
            });
        }
    }

    private String getStatusBadgeStyle(String status) {
        String base = "-fx-padding: 3 8; -fx-background-radius: 10; -fx-font-size: 10; -fx-font-weight: bold;";
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
            case "Annulée":
                return base + "-fx-background-color: #e74c3c; -fx-text-fill: white;";
            default:
                return base + "-fx-background-color: #bdc3c7; -fx-text-fill: #2c3e50;";
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FILL DEMANDE INFO
    // ═══════════════════════════════════════════════════════════════════════════

    private void fillDemandeInfo(Demande demande) {
        if (demande == null) return;

        if (infoTitreLabel != null) {
            infoTitreLabel.setText(demande.getTitre() != null ? demande.getTitre() : "Sans titre");
        }

        if (infoTypeLabel != null) {
            infoTypeLabel.setText(demande.getTypeDemande() != null ? demande.getTypeDemande() : "N/A");
        }

        if (infoPrioriteLabel != null) {
            String priorite = demande.getPriorite() != null ? demande.getPriorite() : "N/A";
            infoPrioriteLabel.setText(priorite);
            switch (priorite.toUpperCase()) {
                case "HAUTE":
                    infoPrioriteLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 13;");
                    break;
                case "NORMALE":
                    infoPrioriteLabel.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold; -fx-font-size: 13;");
                    break;
                case "BASSE":
                    infoPrioriteLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 13;");
                    break;
                default:
                    infoPrioriteLabel.setStyle("-fx-font-size: 13;");
            }
        }

        if (infoStatusLabel != null) {
            String status = demande.getStatus() != null ? demande.getStatus() : "N/A";
            infoStatusLabel.setText(status);
            infoStatusLabel.setStyle(getStatusBadgeStyle(status) + " -fx-font-size: 12;");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STATUS FLOW VISUALIZATION
    // ═══════════════════════════════════════════════════════════════════════════

    private void buildStatusFlow(String currentStatus) {
        if (statusFlowContainer == null) return;
        statusFlowContainer.getChildren().clear();

        int currentIndex = STATUS_ORDER.indexOf(currentStatus);
        if (currentIndex == -1) currentIndex = 0;

        for (int i = 0; i < STATUS_ORDER.size(); i++) {
            String status = STATUS_ORDER.get(i);
            Label statusLabel = new Label(status);
            statusLabel.setMinWidth(80);
            statusLabel.setAlignment(Pos.CENTER);

            if (i < currentIndex) {
                // Completed
                statusLabel.setStyle("-fx-background-color: #d5f5e3; -fx-text-fill: #27ae60; " +
                        "-fx-padding: 8 12; -fx-background-radius: 15; -fx-font-weight: bold;");
            } else if (i == currentIndex) {
                // Current
                statusLabel.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                        "-fx-padding: 8 12; -fx-background-radius: 15; -fx-font-weight: bold;");
            } else {
                // Future
                statusLabel.setStyle("-fx-background-color: #ecf0f1; -fx-text-fill: #bdc3c7; " +
                        "-fx-padding: 8 12; -fx-background-radius: 15;");
            }

            statusFlowContainer.getChildren().add(statusLabel);

            if (i < STATUS_ORDER.size() - 1) {
                Label arrow = new Label("→");
                arrow.setStyle("-fx-font-size: 16; -fx-text-fill: #bdc3c7; -fx-padding: 0 5;");
                statusFlowContainer.getChildren().add(arrow);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ALLOWED STATUSES SETUP
    // ═══════════════════════════════════════════════════════════════════════════

    private void setupAllowedStatuses(String currentStatus) {
        if (nouveauStatutCombo == null) return;

        List<String> allowed = new ArrayList<>();

        if (currentStatus == null) currentStatus = "Nouvelle";

        switch (currentStatus) {
            case "Nouvelle":
                allowed.addAll(Arrays.asList("En cours", "En attente"));
                break;
            case "En cours":
                allowed.addAll(Arrays.asList("En attente", "Résolue"));
                break;
            case "En attente":
                allowed.addAll(Arrays.asList("En cours", "Résolue"));
                break;
            case "Résolue":
                allowed.addAll(Arrays.asList("Fermée", "En cours"));
                break;
            case "Fermée":
                // No transitions from Fermée
                break;
            case "Annulée":
                // No transitions from Annulée
                break;
            default:
                allowed.addAll(Arrays.asList("En cours", "En attente"));
        }

        nouveauStatutCombo.setItems(FXCollections.observableArrayList(allowed));

        if (allowed.isEmpty()) {
            nouveauStatutCombo.setDisable(true);
            nouveauStatutCombo.setPromptText("Aucune transition possible");

            if (submitBtn != null) {
                submitBtn.setDisable(true);
                submitBtn.setText("✅ Statut final atteint");
                submitBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white;");
            }
        } else {
            nouveauStatutCombo.setDisable(false);
            nouveauStatutCombo.setPromptText("Sélectionner...");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LOAD HISTORIQUE
    // ═══════════════════════════════════════════════════════════════════════════

    private void loadHistorique(int idDemande) {
        try {
            List<HistoriqueDemande> historiques = historiqueCRUD.getByDemande(idDemande);
            if (historiqueTable != null) {
                historiqueTable.setItems(FXCollections.observableArrayList(historiques));

                if (historiques.isEmpty()) {
                    historiqueTable.setPlaceholder(new Label("📋 Aucun historique pour cette demande"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading historique: " + e.getMessage());
            if (historiqueTable != null) {
                historiqueTable.setPlaceholder(new Label("❌ Erreur de chargement"));
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // VALIDATION
    // ═══════════════════════════════════════════════════════════════════════════

    private void setupRealtimeValidation() {
        if (nouveauStatutCombo != null) {
            nouveauStatutCombo.valueProperty().addListener((obs, o, n) -> {
                if (n != null) {
                    if (statutError != null) statutError.setText("");
                    nouveauStatutCombo.setStyle("");
                }
            });
        }

        if (commentaireArea != null) {
            commentaireArea.textProperty().addListener((obs, o, n) -> {
                if (n != null && !n.trim().isEmpty()) {
                    if (commentaireError != null) commentaireError.setText("");
                    commentaireArea.setStyle("");
                }
            });
        }
    }

    private boolean validateForm() {
        boolean valid = true;

        // Validate status
        if (nouveauStatutCombo == null || nouveauStatutCombo.getValue() == null) {
            if (statutError != null) {
                statutError.setText("⚠️ Sélectionnez un nouveau statut");
            }
            if (nouveauStatutCombo != null) {
                nouveauStatutCombo.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
            }
            valid = false;
        }

        // Validate comment
        String comment = commentaireArea != null ? commentaireArea.getText() : null;
        if (comment == null || comment.trim().isEmpty()) {
            if (commentaireError != null) {
                commentaireError.setText("⚠️ Le commentaire est obligatoire");
            }
            if (commentaireArea != null) {
                commentaireArea.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
            }
            valid = false;
        } else if (comment.trim().length() < 5) {
            if (commentaireError != null) {
                commentaireError.setText("⚠️ Minimum 5 caractères");
            }
            if (commentaireArea != null) {
                commentaireArea.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
            }
            valid = false;
        }

        return valid;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MAIN ACTION - AVANCER STATUT (FIXED)
    // ═══════════════════════════════════════════════════════════════════════════

    @FXML
    private void avancerStatut() {
        System.out.println("=== avancerStatut() called ===");

        // Validate form
        if (!validateForm()) {
            System.out.println("Validation failed");
            return;
        }

        // Check demande
        if (currentDemande == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "❌ Aucune demande sélectionnée");
            return;
        }

        // Get values
        String ancienStatut = currentDemande.getStatus();
        String nouveauStatut = nouveauStatutCombo.getValue();
        String acteur = connectedActeurFullName + " (" + connectedActeurRole + ")";
        String commentaire = commentaireArea.getText().trim();

        System.out.println("Ancien statut: " + ancienStatut);
        System.out.println("Nouveau statut: " + nouveauStatut);
        System.out.println("Acteur: " + acteur);
        System.out.println("Commentaire: " + commentaire);

        // Disable button during processing
        if (submitBtn != null) {
            submitBtn.setDisable(true);
            submitBtn.setText("⏳ Traitement...");
        }

        try {
            // 1. Update demande status in database
            currentDemande.setStatus(nouveauStatut);
            demandeCRUD.modifier(currentDemande);
            System.out.println("✅ Demande status updated: " + ancienStatut + " → " + nouveauStatut);

            // 2. Add historique entry
            HistoriqueDemande historique = new HistoriqueDemande();
            historique.setIdDemande(currentDemande.getIdDemande());
            historique.setAncienStatut(ancienStatut);
            historique.setNouveauStatut(nouveauStatut);
            historique.setDateAction(new java.util.Date());
            historique.setActeur(acteur);
            historique.setCommentaire(commentaire);
            historiqueCRUD.ajouter(historique);
            System.out.println("✅ Historique entry added");

            // 3. Send email notification (async - don't wait)
            sendEmailToEmployee(ancienStatut, nouveauStatut, acteur, commentaire);

            // 4. Show success message
            showAlert(Alert.AlertType.INFORMATION, "Succès ✅",
                    "Statut mis à jour avec succès!\n\n" +
                            "📋 " + currentDemande.getTitre() + "\n" +
                            "📊 " + ancienStatut + " → " + nouveauStatut + "\n" +
                            "👤 Par: " + acteur + "\n\n" +
                            "📧 Notification envoyée à l'employé.");

            // 5. Return to demandes list
            System.out.println("Returning to demandes list...");
            returnToDemandesList();

        } catch (SQLException e) {
            System.err.println("❌ Error updating demande: " + e.getMessage());
            e.printStackTrace();

            // Re-enable button on error
            if (submitBtn != null) {
                submitBtn.setDisable(false);
                submitBtn.setText("⚡ Avancer le statut");
            }

            showAlert(Alert.AlertType.ERROR, "Erreur", "❌ Erreur: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EMAIL NOTIFICATION
    // ═══════════════════════════════════════════════════════════════════════════

    private void sendEmailToEmployee(String ancienStatut, String nouveauStatut,
                                     String acteur, String commentaire) {
        try {
            System.out.println("═══════════════════════════════════════════════");
            System.out.println("📧 SENDING EMAIL NOTIFICATION");
            System.out.println("   Demande ID: " + currentDemande.getIdDemande());
            System.out.println("   Employee ID: " + currentDemande.getIdEmploye());

            // Get employee
            employe emp = employeCrud.getById(currentDemande.getIdEmploye());

            if (emp == null) {
                System.err.println("❌ Employee not found for ID: " + currentDemande.getIdEmploye());
                return;
            }

            // Get employee name
            String nom = emp.getNom() != null ? emp.getNom() : "";
            String prenom = emp.getPrenom() != null ? emp.getPrenom() : "";
            String empName = (prenom + " " + nom).trim();
            if (empName.isEmpty()) {
                empName = "Employé #" + emp.getId_employé();
            }

            // Get employee email
            String empEmail = emp.getE_mail();

            System.out.println("   Employee Name: " + empName);
            System.out.println("   Employee Email: " + empEmail);
            System.out.println("═══════════════════════════════════════════════");

            // Validate email
            if (empEmail == null || empEmail.isEmpty() || !empEmail.contains("@")) {
                System.err.println("❌ Invalid email for employee: " + empEmail);
                return;
            }

            // Send email asynchronously
            final String finalEmpName = empName;
            final String finalEmpEmail = empEmail;

            emailService.sendStatusChangeToEmployee(
                    finalEmpName,
                    finalEmpEmail,
                    currentDemande.getTitre(),
                    ancienStatut,
                    nouveauStatut,
                    acteur,
                    commentaire
            ).thenAccept(success -> {
                Platform.runLater(() -> {
                    System.out.println(success ?
                            "✅ Email sent successfully to: " + finalEmpEmail :
                            "❌ Email failed to: " + finalEmpEmail);
                });
            }).exceptionally(ex -> {
                System.err.println("❌ Email error: " + ex.getMessage());
                return null;
            });

        } catch (Exception e) {
            System.err.println("❌ Email error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NAVIGATION - RETURN TO LIST (COMPLETELY FIXED)
    // ═══════════════════════════════════════════════════════════════════════════

    @FXML
    private void retourListe() {
        System.out.println("=== retourListe() called ===");
        returnToDemandesList();
    }

    /**
     * Return to the demandes list - handles ALL scenarios
     */
    private void returnToDemandesList() {
        System.out.println("=== returnToDemandesList() ===");

        try {
            // Get any available node
            Node anyNode = getAnyFXMLNode();
            if (anyNode == null) {
                System.err.println("❌ No FXML node available!");
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de navigation");
                return;
            }

            Scene scene = anyNode.getScene();
            if (scene == null) {
                System.err.println("❌ Scene is null!");
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de navigation");
                return;
            }

            Stage currentStage = (Stage) scene.getWindow();
            System.out.println("Current stage: " + currentStage);
            System.out.println("Stage modality: " + (currentStage != null ? currentStage.getModality() : "null"));

            // ═══════════════════════════════════════════════════════════════
            // METHOD 1: Check if we're in a modal window - just close it
            // ═══════════════════════════════════════════════════════════════
            if (currentStage != null && currentStage.getModality() != Modality.NONE) {
                System.out.println("✅ Method 1: Closing modal window");
                currentStage.close();
                return;
            }

            // ═══════════════════════════════════════════════════════════════
            // METHOD 2: Direct scene lookup for contentArea
            // ═══════════════════════════════════════════════════════════════
            Node contentNode = scene.lookup("#contentArea");
            if (contentNode instanceof StackPane) {
                System.out.println("✅ Method 2: Found via scene.lookup()");
                loadDemandesIntoContentArea((StackPane) contentNode);
                return;
            }

            // ═══════════════════════════════════════════════════════════════
            // METHOD 3: Lookup from root
            // ═══════════════════════════════════════════════════════════════
            Parent root = scene.getRoot();
            if (root != null) {
                contentNode = root.lookup("#contentArea");
                if (contentNode instanceof StackPane) {
                    System.out.println("✅ Method 3: Found via root.lookup()");
                    loadDemandesIntoContentArea((StackPane) contentNode);
                    return;
                }

                // ═══════════════════════════════════════════════════════════
                // METHOD 4: If root is BorderPane, check center
                // ═══════════════════════════════════════════════════════════
                if (root instanceof BorderPane) {
                    BorderPane bp = (BorderPane) root;
                    Node center = bp.getCenter();
                    if (center instanceof StackPane) {
                        System.out.println("✅ Method 4: Found as BorderPane center");
                        loadDemandesIntoContentArea((StackPane) center);
                        return;
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════════
            // METHOD 5: Parent traversal
            // ═══════════════════════════════════════════════════════════════
            StackPane contentArea = findContentAreaByTraversal(anyNode);
            if (contentArea != null) {
                System.out.println("✅ Method 5: Found via traversal");
                loadDemandesIntoContentArea(contentArea);
                return;
            }

            // ═══════════════════════════════════════════════════════════════
            // METHOD 6: Use parent controller if available
            // ═══════════════════════════════════════════════════════════════
            if (parentController != null) {
                System.out.println("✅ Method 6: Using parent controller");
                parentController.loadDemandes();

                // Close window if we're in a separate stage
                if (currentStage != null && currentStage.getOwner() != null) {
                    currentStage.close();
                }
                return;
            }

            // ═══════════════════════════════════════════════════════════════
            // METHOD 7: Force close any window (last resort)
            // ═══════════════════════════════════════════════════════════════
            if (currentStage != null) {
                System.out.println("✅ Method 7: Force closing current window");
                currentStage.close();
                return;
            }

            System.err.println("❌ All navigation methods failed!");
            showAlert(Alert.AlertType.WARNING, "Navigation",
                    "Impossible de retourner automatiquement.\n" +
                            "Veuillez cliquer sur 'Demandes' dans le menu.");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de navigation: " + e.getMessage());
        }
    }

    /**
     * Find content area by traversing parent hierarchy
     */
    private StackPane findContentAreaByTraversal(Node startNode) {
        if (startNode == null) {
            System.out.println("startNode is null");
            return null;
        }

        Parent parent = startNode.getParent();
        int depth = 0;
        final int MAX_DEPTH = 100;

        System.out.println("Starting traversal from: " + startNode.getClass().getSimpleName());

        while (parent != null && depth < MAX_DEPTH) {
            depth++;

            // Check StackPane
            if (parent instanceof StackPane) {
                StackPane sp = (StackPane) parent;
                String id = sp.getId();

                System.out.println("  Depth " + depth + ": StackPane, id=" + id);

                if ("contentArea".equals(id)) {
                    System.out.println("Found contentArea at depth " + depth);
                    return sp;
                }

                // Check style class
                if (sp.getStyleClass().contains("content-area")) {
                    System.out.println("Found content-area (by style) at depth " + depth);
                    return sp;
                }
            }

            // Check BorderPane center
            if (parent instanceof BorderPane) {
                BorderPane bp = (BorderPane) parent;
                Node center = bp.getCenter();

                System.out.println("  Depth " + depth + ": BorderPane found");

                if (center instanceof StackPane) {
                    StackPane sp = (StackPane) center;
                    String id = sp.getId();

                    System.out.println("    BorderPane center is StackPane, id=" + id);

                    if ("contentArea".equals(id) || id == null) {
                        System.out.println("Found contentArea in BorderPane center at depth " + depth);
                        return sp;
                    }
                }
            }

            parent = parent.getParent();
        }

        System.out.println("contentArea not found after " + depth + " levels");
        return null;
    }

    /**
     * Load demandes.fxml into content area
     */
    private void loadDemandesIntoContentArea(StackPane contentArea) {
        System.out.println("Loading demandes.fxml into content area...");

        // Try multiple paths
        String[] possiblePaths = {
                "/demandes.fxml",
                "/emp/RHetAdminE/demandes.fxml",
                "/demandes/demandes.fxml"
        };

        for (String path : possiblePaths) {
            try {
                System.out.println("Trying path: " + path);
                URL resource = getClass().getResource(path);

                if (resource != null) {
                    FXMLLoader loader = new FXMLLoader(resource);
                    Parent demandesView = loader.load();
                    contentArea.getChildren().setAll(demandesView);
                    System.out.println("✅ Demandes view loaded successfully from: " + path);
                    return;
                }
            } catch (IOException e) {
                System.out.println("Failed to load from " + path + ": " + e.getMessage());
            }
        }

        // If all paths fail
        System.err.println("❌ Could not load demandes.fxml from any path");
        showAlert(Alert.AlertType.ERROR, "Erreur",
                "Impossible de charger la liste des demandes.\n" +
                        "Veuillez cliquer sur 'Demandes' dans le menu.");
    }

    /**
     * Get any available FXML node
     */
    private Node getAnyFXMLNode() {
        // Try all available nodes
        if (nouveauStatutCombo != null && nouveauStatutCombo.getScene() != null) return nouveauStatutCombo;
        if (commentaireArea != null && commentaireArea.getScene() != null) return commentaireArea;
        if (submitBtn != null && submitBtn.getScene() != null) return submitBtn;
        if (cancelBtn != null && cancelBtn.getScene() != null) return cancelBtn;
        if (infoTitreLabel != null && infoTitreLabel.getScene() != null) return infoTitreLabel;
        if (acteurLabel != null && acteurLabel.getScene() != null) return acteurLabel;
        if (historiqueTable != null && historiqueTable.getScene() != null) return historiqueTable;
        if (statusFlowContainer != null && statusFlowContainer.getScene() != null) return statusFlowContainer;
        if (infoTypeLabel != null && infoTypeLabel.getScene() != null) return infoTypeLabel;
        if (infoPrioriteLabel != null && infoPrioriteLabel.getScene() != null) return infoPrioriteLabel;
        if (infoStatusLabel != null && infoStatusLabel.getScene() != null) return infoStatusLabel;

        // Return any non-null node even without scene check
        if (nouveauStatutCombo != null) return nouveauStatutCombo;
        if (commentaireArea != null) return commentaireArea;
        if (submitBtn != null) return submitBtn;
        if (cancelBtn != null) return cancelBtn;
        if (infoTitreLabel != null) return infoTitreLabel;

        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITY
    // ═══════════════════════════════════════════════════════════════════════════

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}