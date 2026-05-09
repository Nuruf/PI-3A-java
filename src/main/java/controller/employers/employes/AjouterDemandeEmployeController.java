package controller.employers.employes;

import controller.demandes.DemandeFormHelper;
import entities.demande.Demande;
import entities.demande.DemandeDetails;
import entities.employers.employe;
import entities.employers.session;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import service.api.EmailService;
import service.api.MapPickerDialog;
import service.demande.DemandeCRUD;
import service.demande.DemandeDetailsCRUD;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class AjouterDemandeEmployeController implements Initializable {

    // ═══════════════════════════════════════════════════════════════════════════
    // FXML FIELDS
    // ═══════════════════════════════════════════════════════════════════════════

    @FXML private TextField titreField;
    @FXML private ComboBox<String> categorieCombo;
    @FXML private ComboBox<String> typeDemandeCombo;
    @FXML private ComboBox<String> prioriteCombo;
    @FXML private TextArea descriptionArea;
    @FXML private DatePicker dateCreationPicker;

    @FXML private Label titreError;
    @FXML private Label categorieError;
    @FXML private Label typeError;
    @FXML private Label prioriteError;
    @FXML private Label descriptionError;
    @FXML private Label dateError;

    @FXML private TitledPane detailsPane;
    @FXML private VBox dynamicFieldsContainer;

    @FXML private Button submitButton;
    @FXML private Button resetButton;

    // Destination fields
    @FXML private TextField destinationField;
    @FXML private Button mapButton;
    @FXML private Label destinationError;
    @FXML private HBox destinationContainer;

    // ═══════════════════════════════════════════════════════════════════════════
    // INSTANCE VARIABLES
    // ═══════════════════════════════════════════════════════════════════════════

    private double selectedLat = 0;
    private double selectedLon = 0;
    private String selectedDestination = null;

    private DemandeCRUD demandeCRUD;
    private DemandeDetailsCRUD detailsCRUD;
    private DemandeFormHelper formHelper;
    private EmailService emailService;
    private DemandesEmployeController parentController;
    private volatile boolean isSubmitting = false;

    private static final String[] DESTINATION_TYPES = {
            "Mission", "Déplacement", "Formation externe", "Voyage d'affaires",
            "Conférence", "Séminaire", "Visite client", "Voyage", "Transport",
            "Certification", "Mutation", "Télétravail"
    };

    // ═══════════════════════════════════════════════════════════════════════════
    // SETTERS
    // ═══════════════════════════════════════════════════════════════════════════

    public void setParentController(DemandesEmployeController p) {
        this.parentController = p;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("=== AjouterDemandeEmployeController.initialize() ===");

        // Initialize services
        demandeCRUD = new DemandeCRUD();
        detailsCRUD = new DemandeDetailsCRUD();
        formHelper = new DemandeFormHelper();
        emailService = new EmailService();

        // Initialize UI
        initializeUI();

        System.out.println("Initialization complete!");
    }

    private void initializeUI() {
        try {
            // Initialize combo boxes
            formHelper.initializeEmployeeComboBoxes(categorieCombo, typeDemandeCombo, prioriteCombo);

            // Setup date picker
            setupDatePickerWithValidation();

            // Setup destination field
            setupDestinationField();

            // Type change listener for dynamic fields
            if (typeDemandeCombo != null) {
                typeDemandeCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
                    System.out.println("Type changed: " + oldValue + " -> " + newValue);
                    if (newValue != null && !newValue.isEmpty()) {
                        formHelper.updateDynamicFields(newValue, dynamicFieldsContainer, detailsPane);
                        updateDestinationVisibility(newValue);
                    } else {
                        formHelper.updateDynamicFields(null, dynamicFieldsContainer, detailsPane);
                        hideDestinationField();
                    }
                });
            }

            // Setup realtime validation
            setupRealtimeValidation();

            System.out.println("UI initialized successfully");
        } catch (Exception e) {
            System.err.println("Error initializing UI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DESTINATION / MAP METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    private void setupDestinationField() {
        if (destinationContainer != null) {
            destinationContainer.setVisible(false);
            destinationContainer.setManaged(false);
        }

        if (destinationField != null) {
            destinationField.setEditable(false);
            destinationField.setPromptText("Cliquez sur 📍 pour choisir");
        }
    }

    private void updateDestinationVisibility(String type) {
        boolean needsDestination = isDestinationType(type);

        if (destinationContainer != null) {
            destinationContainer.setVisible(needsDestination);
            destinationContainer.setManaged(needsDestination);
        }

        if (!needsDestination) {
            clearDestination();
        }
    }

    private boolean isDestinationType(String type) {
        if (type == null) return false;
        String typeLower = type.toLowerCase();
        for (String destType : DESTINATION_TYPES) {
            if (typeLower.contains(destType.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private void hideDestinationField() {
        if (destinationContainer != null) {
            destinationContainer.setVisible(false);
            destinationContainer.setManaged(false);
        }
        clearDestination();
    }

    private void clearDestination() {
        selectedDestination = null;
        selectedLat = 0;
        selectedLon = 0;
        if (destinationField != null) {
            destinationField.clear();
            destinationField.setStyle("");
        }
        if (destinationError != null) {
            destinationError.setText("");
        }
    }

    @FXML
    private void ouvrirCarte() {
        try {
            MapPickerDialog mapDialog = new MapPickerDialog();
            mapDialog.show(result -> {
                if (result != null) {
                    Platform.runLater(() -> {
                        selectedDestination = result.cityName;
                        selectedLat = result.lat;
                        selectedLon = result.lon;

                        if (destinationField != null) {
                            destinationField.setText(result.cityName);
                            destinationField.setStyle("-fx-border-color: #27ae60; -fx-border-width: 2;");
                        }
                        if (destinationError != null) {
                            destinationError.setText("");
                        }

                        System.out.println("📍 Destination sélectionnée: " + result.cityName +
                                " (Lat: " + result.lat + ", Lon: " + result.lon + ")");
                    });
                }
            });
        } catch (Exception e) {
            System.err.println("Error opening map: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la carte: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DATE VALIDATION
    // ═══════════════════════════════════════════════════════════════════════════

    private void setupDatePickerWithValidation() {
        if (dateCreationPicker == null) {
            System.err.println("WARNING: dateCreationPicker is null!");
            return;
        }

        dateCreationPicker.setValue(LocalDate.now());

        // Day cell factory to disable past dates
        Callback<DatePicker, DateCell> dayCellFactory = datePicker -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);

                if (item.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #ffc0cb; -fx-opacity: 0.6;");
                    setTooltip(new Tooltip("Les dates passées ne sont pas autorisées"));
                } else if (item.equals(LocalDate.now())) {
                    setStyle("-fx-background-color: #90EE90; -fx-font-weight: bold;");
                    setTooltip(new Tooltip("Aujourd'hui"));
                }
            }
        };

        dateCreationPicker.setDayCellFactory(dayCellFactory);

        // Value change listener
        dateCreationPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.isBefore(LocalDate.now())) {
                Platform.runLater(() -> {
                    dateCreationPicker.setValue(LocalDate.now());
                    showDateError("La date ne peut pas être dans le passé.");
                });
            } else {
                clearDateError();
            }
        });
    }

    private void showDateError(String message) {
        if (dateError != null) {
            dateError.setText(message);
            dateError.setStyle("-fx-text-fill: #e74c3c;");
        }
        if (dateCreationPicker != null) {
            dateCreationPicker.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
        }
    }

    private void clearDateError() {
        if (dateError != null) {
            dateError.setText("");
        }
        if (dateCreationPicker != null) {
            dateCreationPicker.setStyle("");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // REALTIME VALIDATION
    // ═══════════════════════════════════════════════════════════════════════════

    private void setupRealtimeValidation() {
        if (titreField != null) {
            titreField.textProperty().addListener((o, ov, nv) -> {
                if (nv != null && !nv.trim().isEmpty()) {
                    clearFieldError(titreField, titreError);
                }
            });
        }
        if (descriptionArea != null) {
            descriptionArea.textProperty().addListener((o, ov, nv) -> {
                if (nv != null && !nv.trim().isEmpty()) {
                    clearFieldError(descriptionArea, descriptionError);
                }
            });
        }
        if (categorieCombo != null) {
            categorieCombo.valueProperty().addListener((o, ov, nv) -> {
                if (nv != null) clearFieldError(categorieCombo, categorieError);
            });
        }
        if (typeDemandeCombo != null) {
            typeDemandeCombo.valueProperty().addListener((o, ov, nv) -> {
                if (nv != null) clearFieldError(typeDemandeCombo, typeError);
            });
        }
        if (prioriteCombo != null) {
            prioriteCombo.valueProperty().addListener((o, ov, nv) -> {
                if (nv != null) clearFieldError(prioriteCombo, prioriteError);
            });
        }
        if (dateCreationPicker != null) {
            dateCreationPicker.valueProperty().addListener((o, ov, nv) -> {
                if (nv != null && !nv.isBefore(LocalDate.now())) {
                    clearFieldError(dateCreationPicker, dateError);
                }
            });
        }
        if (destinationField != null) {
            destinationField.textProperty().addListener((o, ov, nv) -> {
                if (nv != null && !nv.trim().isEmpty()) {
                    if (destinationError != null) destinationError.setText("");
                    destinationField.setStyle("-fx-border-color: #27ae60;");
                }
            });
        }
    }

    private void clearFieldError(Control field, Label errorLabel) {
        if (errorLabel != null) errorLabel.setText("");
        if (field != null) field.setStyle("");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FORM SUBMISSION
    // ═══════════════════════════════════════════════════════════════════════════

    @FXML
    private void ajouterDemande() {
        System.out.println("=== ajouterDemande() called ===");

        if (isSubmitting) {
            System.out.println("Already submitting, ignoring...");
            return;
        }

        if (!validateForm()) {
            System.out.println("Form validation failed");
            return;
        }

        isSubmitting = true;
        setFormEnabled(false);

        // Collect form data
        final String titre = titreField.getText().trim();
        final String description = descriptionArea.getText().trim();
        final String categorie = categorieCombo.getValue();
        final String typeDemande = typeDemandeCombo.getValue();
        final String priorite = prioriteCombo.getValue();
        final LocalDate dateCreation = dateCreationPicker.getValue();
        final String detailsJson = buildDetailsJsonWithDestination();

        System.out.println("Form data:");
        System.out.println("  Titre: " + titre);
        System.out.println("  Categorie: " + categorie);
        System.out.println("  Type: " + typeDemande);
        System.out.println("  Priorite: " + priorite);
        System.out.println("  Date: " + dateCreation);
        System.out.println("  Details: " + detailsJson);

        // Get employee info
        employe currentEmployee = session.getEmploye();
        if (currentEmployee == null) {
            System.err.println("ERROR: No employee in session!");
            showAlert(Alert.AlertType.ERROR, "Erreur", "Session expirée. Veuillez vous reconnecter.");
            isSubmitting = false;
            setFormEnabled(true);
            return;
        }

        final int employeId = currentEmployee.getId_employé();
        final String empName = getEmployeeName(currentEmployee);
        final String empEmail = getEmployeeEmail(currentEmployee);

        System.out.println("Employee: " + empName + " (ID: " + employeId + ", Email: " + empEmail + ")");

        // Submit task
        Task<Boolean> submitTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                System.out.println("Creating Demande object...");

                Demande demande = new Demande();
                demande.setTitre(titre);
                demande.setDescription(description);
                demande.setCategorie(categorie);
                demande.setTypeDemande(typeDemande);
                demande.setPriorite(priorite);
                demande.setStatus("Nouvelle");
                demande.setDateCreation(java.sql.Date.valueOf(dateCreation));
                demande.setIdEmploye(employeId);

                System.out.println("Saving demande to database...");
                demandeCRUD.ajouter(demande);
                System.out.println("Demande saved with ID: " + demande.getIdDemande());

                // Save details if not empty
                if (!detailsJson.equals("{}")) {
                    System.out.println("Saving demande details...");
                    DemandeDetails details = new DemandeDetails();
                    details.setIdDemande(demande.getIdDemande());
                    details.setDetails(detailsJson);
                    detailsCRUD.ajouter(details);
                    System.out.println("Details saved!");
                }

                // Send email notification (async, don't wait)
                try {
                    emailService.sendNewDemandeToRH(
                            empName,
                            empEmail,
                            demande.getTitre(),
                            demande.getTypeDemande(),
                            demande.getPriorite()
                    ).thenAccept(success -> {
                        System.out.println("Email notification: " + (success ? "sent" : "failed"));
                    });
                } catch (Exception e) {
                    System.err.println("Email error (non-blocking): " + e.getMessage());
                }

                return true;
            }
        };

        submitTask.setOnSucceeded(event -> {
            Platform.runLater(() -> {
                System.out.println("Submission successful!");
                isSubmitting = false;
                setFormEnabled(true);
                showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "✅ Demande soumise avec succès!\nVotre demande sera traitée par le service RH.");
                returnToDemandesList();
            });
        });

        submitTask.setOnFailed(event -> {
            Platform.runLater(() -> {
                isSubmitting = false;
                setFormEnabled(true);
                Throwable ex = submitTask.getException();
                System.err.println("Submission failed: " + (ex != null ? ex.getMessage() : "Unknown error"));
                if (ex != null) ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "❌ Erreur lors de la soumission: " + (ex != null ? ex.getMessage() : "Erreur inconnue"));
            });
        });

        Thread submitThread = new Thread(submitTask);
        submitThread.setDaemon(true);
        submitThread.start();
    }

    private String buildDetailsJsonWithDestination() {
        StringBuilder json = new StringBuilder("{");
        boolean hasContent = false;

        // Get form helper's dynamic fields
        String formDetails = formHelper.buildDetailsJson();
        if (!formDetails.equals("{}")) {
            String inner = formDetails.substring(1, formDetails.length() - 1);
            if (!inner.isEmpty()) {
                json.append(inner);
                hasContent = true;
            }
        }

        // Add destination if selected and visible
        if (selectedDestination != null && !selectedDestination.isEmpty() &&
                destinationContainer != null && destinationContainer.isVisible()) {

            if (hasContent) json.append(",");

            json.append("\"destination\":\"").append(escapeJson(selectedDestination)).append("\"");
            json.append(",\"destinationLat\":").append(selectedLat);
            json.append(",\"destinationLon\":").append(selectedLon);
        }

        json.append("}");
        return json.toString();
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private void setFormEnabled(boolean enabled) {
        if (titreField != null) titreField.setDisable(!enabled);
        if (descriptionArea != null) descriptionArea.setDisable(!enabled);
        if (categorieCombo != null) categorieCombo.setDisable(!enabled);
        if (typeDemandeCombo != null) typeDemandeCombo.setDisable(!enabled);
        if (prioriteCombo != null) prioriteCombo.setDisable(!enabled);
        if (dateCreationPicker != null) dateCreationPicker.setDisable(!enabled);
        if (destinationField != null) destinationField.setDisable(!enabled);
        if (mapButton != null) mapButton.setDisable(!enabled);
        if (submitButton != null) {
            submitButton.setDisable(!enabled);
            submitButton.setText(enabled ? "✅ Soumettre" : "⏳ Envoi en cours...");
        }
        if (resetButton != null) resetButton.setDisable(!enabled);
        if (dynamicFieldsContainer != null) dynamicFieldsContainer.setDisable(!enabled);
    }

    private String getEmployeeName(employe emp) {
        if (emp == null) return "Employé";
        String nom = emp.getNom() != null ? emp.getNom() : "";
        String prenom = emp.getPrenom() != null ? emp.getPrenom() : "";
        String fullName = (prenom + " " + nom).trim();
        return fullName.isEmpty() ? "Employé #" + emp.getId_employé() : fullName;
    }

    private String getEmployeeEmail(employe emp) {
        if (emp == null) return "unknown@company.com";
        String email = emp.getE_mail();
        return (email != null && !email.isEmpty()) ? email : "employe" + emp.getId_employé() + "@company.com";
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FORM VALIDATION
    // ═══════════════════════════════════════════════════════════════════════════

    private boolean validateForm() {
        boolean valid = true;

        // Titre validation
        if (titreField == null || titreField.getText() == null || titreField.getText().trim().isEmpty()) {
            setFieldError(titreField, titreError, "Le titre est obligatoire");
            valid = false;
        } else {
            String titre = titreField.getText().trim();
            if (titre.length() < 3) {
                setFieldError(titreField, titreError, "Le titre doit contenir au moins 3 caractères");
                valid = false;
            } else if (titre.length() > 100) {
                setFieldError(titreField, titreError, "Le titre ne peut pas dépasser 100 caractères");
                valid = false;
            }
        }

        // Catégorie validation
        if (categorieCombo == null || categorieCombo.getValue() == null) {
            setFieldError(categorieCombo, categorieError, "La catégorie est obligatoire");
            valid = false;
        }

        // Type validation
        if (typeDemandeCombo == null || typeDemandeCombo.getValue() == null) {
            setFieldError(typeDemandeCombo, typeError, "Le type est obligatoire");
            valid = false;
        }

        // Priorité validation
        if (prioriteCombo == null || prioriteCombo.getValue() == null) {
            setFieldError(prioriteCombo, prioriteError, "La priorité est obligatoire");
            valid = false;
        }

        // Description validation
        if (descriptionArea == null || descriptionArea.getText() == null || descriptionArea.getText().trim().isEmpty()) {
            setFieldError(descriptionArea, descriptionError, "La description est obligatoire");
            valid = false;
        } else {
            String description = descriptionArea.getText().trim();
            if (description.length() < 10) {
                setFieldError(descriptionArea, descriptionError, "La description doit contenir au moins 10 caractères");
                valid = false;
            }
        }

        // Date validation
        if (dateCreationPicker == null || dateCreationPicker.getValue() == null) {
            setFieldError(dateCreationPicker, dateError, "La date est obligatoire");
            valid = false;
        } else {
            LocalDate selectedDate = dateCreationPicker.getValue();
            if (selectedDate.isBefore(LocalDate.now())) {
                setFieldError(dateCreationPicker, dateError, "⚠️ La date ne peut pas être dans le passé");
                valid = false;
            }
        }

        // Destination validation (only if visible)
        if (destinationContainer != null && destinationContainer.isVisible()) {
            if (selectedDestination == null || selectedDestination.trim().isEmpty()) {
                if (destinationError != null) {
                    destinationError.setText("⚠️ Veuillez sélectionner une destination sur la carte");
                }
                if (destinationField != null) {
                    destinationField.setStyle("-fx-border-color: #e74c3c;");
                }
                valid = false;
            }
        }

        // Dynamic fields validation
        if (formHelper != null && !formHelper.validateDynamicFields()) {
            valid = false;
        }

        return valid;
    }

    private void setFieldError(Control field, Label errorLabel, String message) {
        if (errorLabel != null) errorLabel.setText(message);
        if (field != null) field.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // RESET FORM
    // ═══════════════════════════════════════════════════════════════════════════

    @FXML
    private void reinitialiserFormulaire() {
        if (titreField != null) {
            titreField.clear();
            titreField.setStyle("");
        }
        if (descriptionArea != null) {
            descriptionArea.clear();
            descriptionArea.setStyle("");
        }
        if (categorieCombo != null) {
            categorieCombo.setValue(null);
            categorieCombo.setStyle("");
        }
        if (typeDemandeCombo != null) {
            typeDemandeCombo.setValue(null);
            typeDemandeCombo.getItems().clear();
            typeDemandeCombo.setStyle("");
        }
        if (prioriteCombo != null) {
            prioriteCombo.setValue("NORMALE");
            prioriteCombo.setStyle("");
        }
        if (dateCreationPicker != null) {
            dateCreationPicker.setValue(LocalDate.now());
            dateCreationPicker.setStyle("");
        }

        clearDestination();
        hideDestinationField();
        clearAllErrors();

        if (formHelper != null) {
            formHelper.updateDynamicFields(null, dynamicFieldsContainer, detailsPane);
        }
    }

    private void clearAllErrors() {
        if (titreError != null) titreError.setText("");
        if (categorieError != null) categorieError.setText("");
        if (typeError != null) typeError.setText("");
        if (prioriteError != null) prioriteError.setText("");
        if (descriptionError != null) descriptionError.setText("");
        if (dateError != null) dateError.setText("");
        if (destinationError != null) destinationError.setText("");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NAVIGATION - RETURN TO LIST (FIXED)
    // ═══════════════════════════════════════════════════════════════════════════

    @FXML
    private void retourListe() {
        System.out.println("=== retourListe() called ===");
        returnToDemandesList();
    }

    /**
     * Return to the demandes list - handles both in-view and modal scenarios
     */
    private void returnToDemandesList() {
        try {
            System.out.println("Attempting to return to demandes list...");

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

            // Method 1: Check if we're in a modal window - just close it
            if (currentStage != null && currentStage.getModality() != Modality.NONE) {
                System.out.println("✅ Method 1: Closing modal window");
                currentStage.close();
                return;
            }

            // Method 2: Direct scene lookup for contentArea
            Node contentNode = scene.lookup("#contentArea");
            if (contentNode instanceof StackPane) {
                System.out.println("✅ Method 2: Found via scene.lookup()");
                loadDemandesEmployeIntoContentArea((StackPane) contentNode);
                return;
            }

            // Method 3: Lookup from root
            Parent root = scene.getRoot();
            if (root != null) {
                contentNode = root.lookup("#contentArea");
                if (contentNode instanceof StackPane) {
                    System.out.println("✅ Method 3: Found via root.lookup()");
                    loadDemandesEmployeIntoContentArea((StackPane) contentNode);
                    return;
                }

                // Method 4: If root is BorderPane, check center
                if (root instanceof javafx.scene.layout.BorderPane) {
                    javafx.scene.layout.BorderPane bp = (javafx.scene.layout.BorderPane) root;
                    Node center = bp.getCenter();
                    if (center instanceof StackPane) {
                        System.out.println("✅ Method 4: Found as BorderPane center");
                        loadDemandesEmployeIntoContentArea((StackPane) center);
                        return;
                    }
                }
            }

            // Method 5: Parent traversal
            StackPane contentArea = findContentAreaByTraversal(anyNode);
            if (contentArea != null) {
                System.out.println("✅ Method 5: Found via traversal");
                loadDemandesEmployeIntoContentArea(contentArea);
                return;
            }

            // Method 6: Use parent controller if available
            if (parentController != null) {
                System.out.println("✅ Method 6: Using parent controller");
                parentController.loadDemandes();

                // Try to close window if we're in one
                if (currentStage != null && currentStage.getOwner() != null) {
                    currentStage.close();
                }
                return;
            }

            // Method 7: Last resort - close any window
            if (currentStage != null) {
                System.out.println("✅ Method 7: Closing current window");
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
        if (startNode == null) return null;

        Parent parent = startNode.getParent();
        int depth = 0;
        final int MAX_DEPTH = 50;

        while (parent != null && depth < MAX_DEPTH) {
            depth++;

            // Check StackPane
            if (parent instanceof StackPane) {
                StackPane sp = (StackPane) parent;
                String id = sp.getId();

                if ("contentArea".equals(id)) {
                    System.out.println("Found contentArea at depth " + depth);
                    return sp;
                }

                if (sp.getStyleClass().contains("content-area")) {
                    System.out.println("Found content-area by style class at depth " + depth);
                    return sp;
                }
            }

            // Check BorderPane center
            if (parent instanceof javafx.scene.layout.BorderPane) {
                javafx.scene.layout.BorderPane bp = (javafx.scene.layout.BorderPane) parent;
                Node center = bp.getCenter();
                if (center instanceof StackPane) {
                    StackPane sp = (StackPane) center;
                    if ("contentArea".equals(sp.getId())) {
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
     * Load demandes-employe.fxml into content area
     */
    private void loadDemandesEmployeIntoContentArea(StackPane contentArea) {
        try {
            System.out.println("Loading demandes-employe.fxml into content area...");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/demandes-employe.fxml"));
            Parent demandesView = loader.load();

            contentArea.getChildren().setAll(demandesView);
            System.out.println("✅ Demandes employe view loaded successfully!");

        } catch (IOException e) {
            System.err.println("❌ Error loading demandes-employe.fxml: " + e.getMessage());
            e.printStackTrace();

            // Try alternative path
            try {
                System.out.println("Trying alternative path...");
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/emp/employes/demandes-employe.fxml"));
                Parent demandesView = loader.load();
                contentArea.getChildren().setAll(demandesView);
                System.out.println("✅ Loaded from alternative path!");
            } catch (IOException e2) {
                System.err.println("❌ Alternative path also failed: " + e2.getMessage());
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Impossible de charger la liste des demandes.\n" +
                                "Veuillez cliquer sur 'Demandes' dans le menu.");
            }
        }
    }

    /**
     * Get any available FXML node
     */
    private Node getAnyFXMLNode() {
        if (titreField != null) return titreField;
        if (descriptionArea != null) return descriptionArea;
        if (categorieCombo != null) return categorieCombo;
        if (typeDemandeCombo != null) return typeDemandeCombo;
        if (prioriteCombo != null) return prioriteCombo;
        if (dateCreationPicker != null) return dateCreationPicker;
        if (submitButton != null) return submitButton;
        if (resetButton != null) return resetButton;
        if (dynamicFieldsContainer != null) return dynamicFieldsContainer;
        if (detailsPane != null) return detailsPane;
        if (destinationField != null) return destinationField;
        if (mapButton != null) return mapButton;
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITY
    // ═══════════════════════════════════════════════════════════════════════════

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}