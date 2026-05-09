package controller.employers.RHetAdminE;

import controller.demandes.DemandeFormHelper;
import entities.demande.Demande;
import entities.demande.DemandeDetails;
import service.api.MapPickerDialog;
import service.demande.DemandeCRUD;
import service.demande.DemandeDetailsCRUD;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

public class ModifierDemandeController implements Initializable {

    // ═══════════════════════════════════════════════════════════════════════════
    // FXML FIELDS
    // ═══════════════════════════════════════════════════════════════════════════

    @FXML private Label headerLabel;
    @FXML private TextField titreField;
    @FXML private ComboBox<String> categorieCombo;
    @FXML private ComboBox<String> typeDemandeCombo;
    @FXML private ComboBox<String> prioriteCombo;
    @FXML private ComboBox<String> statusCombo;
    @FXML private TextArea descriptionArea;
    @FXML private DatePicker dateCreationPicker;

    @FXML private Label titreError;
    @FXML private Label categorieError;
    @FXML private Label typeError;
    @FXML private Label prioriteError;
    @FXML private Label statusError;
    @FXML private Label descriptionError;
    @FXML private Label dateError;

    @FXML private TitledPane detailsPane;
    @FXML private VBox dynamicFieldsContainer;

    @FXML private Label destinationLabel;
    @FXML private HBox destinationContainer;
    @FXML private TextField destinationField;
    @FXML private Button mapButton;
    @FXML private Label destinationError;

    // ═══════════════════════════════════════════════════════════════════════════
    // INSTANCE VARIABLES
    // ═══════════════════════════════════════════════════════════════════════════

    private DemandeCRUD demandeCRUD;
    private DemandeDetailsCRUD detailsCRUD;
    private DemandeFormHelper formHelper;

    private DemandesController parentController;
    private Demande currentDemande;
    private DemandeDetails currentDetails;

    // CRITICAL: Store details JSON and control filling
    private String pendingDetailsJson = null;
    private boolean shouldFillDetails = false;

    private double selectedLat = 0;
    private double selectedLon = 0;
    private String selectedDestination = null;

    private static final Map<String, List<String>> CATEGORY_TYPES = new LinkedHashMap<>();

    static {
        CATEGORY_TYPES.put("Ressources Humaines", Arrays.asList(
                "Congé", "Attestation de travail", "Attestation de salaire",
                "Certificat de travail", "Mutation", "Démission"
        ));
        CATEGORY_TYPES.put("Administrative", Arrays.asList(
                "Avance sur salaire", "Remboursement", "Matériel de bureau",
                "Badge d'accès", "Carte de visite"
        ));
        CATEGORY_TYPES.put("Informatique", Arrays.asList(
                "Matériel informatique", "Accès système", "Logiciel", "Problème technique"
        ));
        CATEGORY_TYPES.put("Formation", Arrays.asList(
                "Formation interne", "Formation externe", "Certification"
        ));
        CATEGORY_TYPES.put("Organisation du travail", Arrays.asList(
                "Télétravail", "Changement d'horaires", "Heures supplémentaires"
        ));
    }

    private static final List<String> ALL_STATUSES = Arrays.asList(
            "Nouvelle", "En cours", "En attente", "Résolue", "Fermée", "Annulée"
    );

    // ═══════════════════════════════════════════════════════════════════════════
    // SETTERS
    // ═══════════════════════════════════════════════════════════════════════════

    public void setParentController(DemandesController parent) {
        this.parentController = parent;
    }

    /**
     * MAIN ENTRY POINT - Called when editing a demande
     */
    public void setDemande(Demande demande) {
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║ setDemande() called                                       ║");
        System.out.println("║ Demande: " + (demande != null ? demande.getTitre() : "null"));
        System.out.println("╚═══════════════════════════════════════════════════════════╝");

        this.currentDemande = demande;

        if (demande != null) {
            // Step 1: Load details from database
            loadDemandeDetailsFromDB(demande.getIdDemande());

            // Step 2: Fill form with proper sequencing
            fillFormWithProperSequencing(demande);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("=== ModifierDemandeController.initialize() ===");

        demandeCRUD = new DemandeCRUD();
        detailsCRUD = new DemandeDetailsCRUD();
        formHelper = new DemandeFormHelper();

        initializeComboBoxes();
        setupCategoryListener();
        setupTypeListener();
        setupRealtimeValidation();
        setupDestinationField();

        System.out.println("ModifierDemandeController initialized!");
    }

    private void initializeComboBoxes() {
        if (categorieCombo != null) {
            categorieCombo.setItems(FXCollections.observableArrayList(CATEGORY_TYPES.keySet()));
        }

        if (prioriteCombo != null) {
            prioriteCombo.setItems(FXCollections.observableArrayList("HAUTE", "NORMALE", "BASSE"));
        }

        if (statusCombo != null) {
            statusCombo.setItems(FXCollections.observableArrayList(ALL_STATUSES));
        }

        if (typeDemandeCombo != null) {
            typeDemandeCombo.setItems(FXCollections.observableArrayList());
        }
    }

    private void setupCategoryListener() {
        if (categorieCombo != null) {
            categorieCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                System.out.println("📁 Category changed: " + oldVal + " → " + newVal);

                if (newVal != null && CATEGORY_TYPES.containsKey(newVal)) {
                    List<String> types = CATEGORY_TYPES.get(newVal);
                    typeDemandeCombo.setItems(FXCollections.observableArrayList(types));
                    System.out.println("   Types set: " + types);
                } else {
                    typeDemandeCombo.getItems().clear();
                }
            });
        }
    }

    /**
     * CRITICAL: Type listener creates dynamic fields and fills them
     */
    private void setupTypeListener() {
        if (typeDemandeCombo != null) {
            typeDemandeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                System.out.println("╔═══════════════════════════════════════════════════════════╗");
                System.out.println("║ TYPE CHANGED: " + oldVal + " → " + newVal);
                System.out.println("║ shouldFillDetails: " + shouldFillDetails);
                System.out.println("║ pendingDetailsJson: " + (pendingDetailsJson != null ? "EXISTS" : "NULL"));
                System.out.println("╚═══════════════════════════════════════════════════════════╝");

                if (newVal != null && !newVal.isEmpty()) {
                    // Step 1: Create dynamic fields
                    System.out.println(">>> Creating dynamic fields for: " + newVal);
                    formHelper.updateDynamicFields(newVal, dynamicFieldsContainer, detailsPane);

                    // Step 2: Update destination visibility
                    updateDestinationVisibility(newVal);

                    // Step 3: Fill fields if we have pending data
                    if (shouldFillDetails && pendingDetailsJson != null &&
                            !pendingDetailsJson.isEmpty() && !pendingDetailsJson.equals("{}")) {

                        // Use multiple Platform.runLater to ensure fields are fully created
                        Platform.runLater(() -> {
                            Platform.runLater(() -> {
                                System.out.println(">>> FILLING DYNAMIC FIELDS NOW <<<");
                                System.out.println("JSON: " + pendingDetailsJson);

                                fillDynamicFieldsFromJson();
                                fillDestinationFromJson();

                                System.out.println(">>> FILLING COMPLETE <<<");

                                // Reset flag after filling
                                shouldFillDetails = false;
                            });
                        });
                    }
                } else {
                    formHelper.updateDynamicFields(null, dynamicFieldsContainer, detailsPane);
                    hideDestinationField();
                }
            });
        }
    }

    private void setupRealtimeValidation() {
        if (titreField != null) {
            titreField.textProperty().addListener((o, ov, nv) -> {
                if (nv != null && !nv.trim().isEmpty()) clearFieldError(titreField, titreError);
            });
        }
        if (descriptionArea != null) {
            descriptionArea.textProperty().addListener((o, ov, nv) -> {
                if (nv != null && !nv.trim().isEmpty()) clearFieldError(descriptionArea, descriptionError);
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
        if (statusCombo != null) {
            statusCombo.valueProperty().addListener((o, ov, nv) -> {
                if (nv != null) clearFieldError(statusCombo, statusError);
            });
        }
    }

    private void clearFieldError(Control field, Label errorLabel) {
        if (errorLabel != null) errorLabel.setText("");
        if (field != null) field.setStyle("");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LOAD DETAILS FROM DATABASE
    // ═══════════════════════════════════════════════════════════════════════════

    private void loadDemandeDetailsFromDB(int idDemande) {
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║ Loading details from DB for demande ID: " + idDemande);

        try {
            currentDetails = detailsCRUD.getByDemande(idDemande);

            if (currentDetails != null && currentDetails.getDetails() != null
                    && !currentDetails.getDetails().isEmpty()) {
                pendingDetailsJson = currentDetails.getDetails();
                shouldFillDetails = true;
                System.out.println("║ ✅ Details loaded: " + pendingDetailsJson);
            } else {
                pendingDetailsJson = null;
                shouldFillDetails = false;
                System.out.println("║ ⚠️ No details found in database");
            }
        } catch (SQLException e) {
            System.err.println("║ ❌ Error loading details: " + e.getMessage());
            e.printStackTrace();
            pendingDetailsJson = null;
            shouldFillDetails = false;
        }

        System.out.println("╚═══════════════════════════════════════════════════════════╝");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FILL FORM WITH PROPER SEQUENCING
    // ═══════════════════════════════════════════════════════════════════════════

    private void fillFormWithProperSequencing(Demande demande) {
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║ fillFormWithProperSequencing()");
        System.out.println("║ Title: " + demande.getTitre());
        System.out.println("║ Category: " + demande.getCategorie());
        System.out.println("║ Type: " + demande.getTypeDemande());
        System.out.println("╚═══════════════════════════════════════════════════════════╝");

        // Header
        if (headerLabel != null) {
            headerLabel.setText("✏️ Modifier: " + demande.getTitre());
        }

        // Basic fields (these don't depend on anything)
        if (titreField != null) {
            titreField.setText(demande.getTitre() != null ? demande.getTitre() : "");
        }

        if (descriptionArea != null) {
            descriptionArea.setText(demande.getDescription() != null ? demande.getDescription() : "");
        }

        if (prioriteCombo != null && demande.getPriorite() != null) {
            prioriteCombo.setValue(demande.getPriorite());
        }

        if (statusCombo != null && demande.getStatus() != null) {
            statusCombo.setValue(demande.getStatus());
        }

        if (dateCreationPicker != null && demande.getDateCreation() != null) {
            try {
                LocalDate date = demande.getDateCreation().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
                dateCreationPicker.setValue(date);
            } catch (Exception e) {
                dateCreationPicker.setValue(LocalDate.now());
            }
        }

        // CRITICAL SEQUENCING: Category → Types populated → Type set → Dynamic fields created → Fields filled

        String category = demande.getCategorie();
        String type = demande.getTypeDemande();

        if (category != null && !category.isEmpty()) {
            // Ensure category is in the list
            if (!categorieCombo.getItems().contains(category)) {
                categorieCombo.getItems().add(category);
            }

            // Set category - this will trigger the listener which populates types
            System.out.println(">>> Setting category: " + category);
            categorieCombo.setValue(category);

            // Now set the type AFTER category listener has run
            Platform.runLater(() -> {
                if (type != null && !type.isEmpty()) {
                    // Ensure type is in the list
                    if (!typeDemandeCombo.getItems().contains(type)) {
                        System.out.println(">>> Adding type to list: " + type);
                        typeDemandeCombo.getItems().add(type);
                    }

                    // Set the flag BEFORE setting type
                    shouldFillDetails = (pendingDetailsJson != null && !pendingDetailsJson.isEmpty());

                    System.out.println(">>> Setting type: " + type);
                    System.out.println(">>> shouldFillDetails: " + shouldFillDetails);

                    // Set type - this will trigger the type listener which creates fields and fills them
                    typeDemandeCombo.setValue(type);
                }
            });
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FILL DYNAMIC FIELDS FROM JSON
    // ═══════════════════════════════════════════════════════════════════════════

    private void fillDynamicFieldsFromJson() {
        if (pendingDetailsJson == null || pendingDetailsJson.isEmpty()) {
            System.out.println("No pending JSON to fill");
            return;
        }

        System.out.println("Filling dynamic fields from: " + pendingDetailsJson);

        // Get all dynamic fields from form helper
        Map<String, Control> fields = formHelper.getDynamicFields();
        System.out.println("Available dynamic fields: " + fields.keySet());

        if (fields.isEmpty()) {
            System.out.println("⚠️ No dynamic fields available!");
            return;
        }

        // Parse JSON
        Map<String, String> values = formHelper.parseDetailsJson(pendingDetailsJson);
        System.out.println("Parsed values: " + values);

        // Fill each field
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            // Skip coordinate fields
            if (key.endsWith("Lat") || key.endsWith("Lon")) {
                continue;
            }

            // Find and fill the field
            Control control = findControlByKey(fields, key);
            if (control != null) {
                setControlValue(control, value);
                System.out.println("✅ Set '" + key + "' = '" + value + "'");

                // Handle location field coordinates
                if (formHelper.isLocationField(key) && control instanceof TextField) {
                    String latStr = values.get(key + "Lat");
                    String lonStr = values.get(key + "Lon");
                    if (latStr != null && lonStr != null) {
                        try {
                            double lat = Double.parseDouble(latStr);
                            double lon = Double.parseDouble(lonStr);
                            ((TextField) control).setUserData(new double[]{lat, lon});
                            control.setStyle("-fx-border-color: #27ae60; -fx-border-width: 2;");
                        } catch (NumberFormatException e) {
                            // Ignore
                        }
                    }
                }
            } else {
                System.out.println("⚠️ Control not found for: " + key);
            }
        }
    }

    private Control findControlByKey(Map<String, Control> fields, String key) {
        // Direct match
        if (fields.containsKey(key)) {
            return fields.get(key);
        }

        // Try case-insensitive match
        String keyLower = key.toLowerCase();
        for (Map.Entry<String, Control> entry : fields.entrySet()) {
            if (entry.getKey().toLowerCase().equals(keyLower)) {
                return entry.getValue();
            }
        }

        // Try normalized match
        String normalizedKey = normalizeKey(key);
        for (Map.Entry<String, Control> entry : fields.entrySet()) {
            if (normalizeKey(entry.getKey()).equals(normalizedKey)) {
                return entry.getValue();
            }
        }

        return null;
    }

    private String normalizeKey(String key) {
        if (key == null) return "";
        return key.toLowerCase()
                .replaceAll("[\\s_-]", "")
                .replaceAll("[àâäáã]", "a")
                .replaceAll("[éèêëẽ]", "e")
                .replaceAll("[ïîíì]", "i")
                .replaceAll("[ôöóòõ]", "o")
                .replaceAll("[ùûüúũ]", "u")
                .replaceAll("ç", "c");
    }

    private void setControlValue(Control control, String value) {
        if (control == null || value == null) return;

        try {
            if (control instanceof TextField) {
                ((TextField) control).setText(value);
            } else if (control instanceof TextArea) {
                ((TextArea) control).setText(value);
            } else if (control instanceof ComboBox) {
                @SuppressWarnings("unchecked")
                ComboBox<String> combo = (ComboBox<String>) control;
                if (!combo.getItems().contains(value) && !value.isEmpty()) {
                    combo.getItems().add(value);
                }
                combo.setValue(value);
            } else if (control instanceof DatePicker) {
                try {
                    LocalDate date = LocalDate.parse(value);
                    ((DatePicker) control).setValue(date);
                } catch (Exception e) {
                    // Try other formats
                    try {
                        if (value.contains("/")) {
                            String[] parts = value.split("/");
                            if (parts.length == 3) {
                                LocalDate date = LocalDate.of(
                                        Integer.parseInt(parts[2]),
                                        Integer.parseInt(parts[1]),
                                        Integer.parseInt(parts[0])
                                );
                                ((DatePicker) control).setValue(date);
                            }
                        }
                    } catch (Exception ex) {
                        System.err.println("Could not parse date: " + value);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error setting value for control: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DESTINATION / MAP
    // ═══════════════════════════════════════════════════════════════════════════

    private void setupDestinationField() {
        if (destinationContainer != null) {
            destinationContainer.setVisible(false);
            destinationContainer.setManaged(false);
        }
        if (destinationLabel != null) {
            destinationLabel.setVisible(false);
            destinationLabel.setManaged(false);
        }
        if (destinationField != null) {
            destinationField.setEditable(false);
        }
    }

    private void updateDestinationVisibility(String type) {
        boolean needsDestination = isDestinationType(type);

        if (destinationContainer != null) {
            destinationContainer.setVisible(needsDestination);
            destinationContainer.setManaged(needsDestination);
        }
        if (destinationLabel != null) {
            destinationLabel.setVisible(needsDestination);
            destinationLabel.setManaged(needsDestination);
        }

        if (!needsDestination) {
            clearDestination();
        }
    }

    private boolean isDestinationType(String type) {
        if (type == null) return false;
        String typeLower = type.toLowerCase();
        String[] destTypes = {"mission", "déplacement", "formation externe", "voyage",
                "conférence", "séminaire", "visite", "transport", "certification", "mutation", "télétravail"};
        for (String destType : destTypes) {
            if (typeLower.contains(destType)) {
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
        if (destinationLabel != null) {
            destinationLabel.setVisible(false);
            destinationLabel.setManaged(false);
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

    private void fillDestinationFromJson() {
        if (pendingDetailsJson == null || pendingDetailsJson.isEmpty()) return;

        try {
            Map<String, String> values = formHelper.parseDetailsJson(pendingDetailsJson);

            if (values.containsKey("destination")) {
                selectedDestination = values.get("destination");

                try {
                    selectedLat = Double.parseDouble(values.getOrDefault("destinationLat", "0"));
                    selectedLon = Double.parseDouble(values.getOrDefault("destinationLon", "0"));
                } catch (NumberFormatException e) {
                    selectedLat = 0;
                    selectedLon = 0;
                }

                if (destinationField != null && selectedDestination != null && !selectedDestination.isEmpty()) {
                    destinationField.setText(selectedDestination);
                    destinationField.setStyle("-fx-border-color: #27ae60; -fx-border-width: 2;");
                    System.out.println("✅ Loaded destination: " + selectedDestination);
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading destination: " + e.getMessage());
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
                    });
                }
            });
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la carte: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FORM VALIDATION
    // ═══════════════════════════════════════════════════════════════════════════

    private boolean validateForm() {
        boolean valid = true;

        if (titreField == null || titreField.getText() == null || titreField.getText().trim().isEmpty()) {
            setFieldError(titreField, titreError, "Le titre est obligatoire");
            valid = false;
        } else if (titreField.getText().trim().length() < 3) {
            setFieldError(titreField, titreError, "Minimum 3 caractères");
            valid = false;
        }

        if (categorieCombo == null || categorieCombo.getValue() == null) {
            setFieldError(categorieCombo, categorieError, "Sélectionnez une catégorie");
            valid = false;
        }

        if (typeDemandeCombo == null || typeDemandeCombo.getValue() == null) {
            setFieldError(typeDemandeCombo, typeError, "Sélectionnez un type");
            valid = false;
        }

        if (prioriteCombo == null || prioriteCombo.getValue() == null) {
            setFieldError(prioriteCombo, prioriteError, "Sélectionnez une priorité");
            valid = false;
        }

        if (statusCombo == null || statusCombo.getValue() == null) {
            setFieldError(statusCombo, statusError, "Sélectionnez un statut");
            valid = false;
        }

        if (descriptionArea == null || descriptionArea.getText() == null ||
                descriptionArea.getText().trim().isEmpty()) {
            setFieldError(descriptionArea, descriptionError, "La description est obligatoire");
            valid = false;
        } else if (descriptionArea.getText().trim().length() < 10) {
            setFieldError(descriptionArea, descriptionError, "Minimum 10 caractères");
            valid = false;
        }

        if (dateCreationPicker == null || dateCreationPicker.getValue() == null) {
            setFieldError(dateCreationPicker, dateError, "La date est obligatoire");
            valid = false;
        }

        if (destinationContainer != null && destinationContainer.isVisible()) {
            if (selectedDestination == null || selectedDestination.trim().isEmpty()) {
                if (destinationError != null) destinationError.setText("⚠️ Sélectionnez une destination");
                if (destinationField != null) destinationField.setStyle("-fx-border-color: #e74c3c;");
                valid = false;
            }
        }

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
    // MODIFY DEMANDE
    // ═══════════════════════════════════════════════════════════════════════════

    @FXML
    private void modifierDemande() {
        System.out.println("=== modifierDemande() ===");

        if (!validateForm()) {
            System.out.println("Validation failed");
            return;
        }

        if (currentDemande == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "❌ Aucune demande à modifier");
            return;
        }

        try {
            currentDemande.setTitre(titreField.getText().trim());
            currentDemande.setDescription(descriptionArea.getText().trim());
            currentDemande.setCategorie(categorieCombo.getValue());
            currentDemande.setTypeDemande(typeDemandeCombo.getValue());
            currentDemande.setPriorite(prioriteCombo.getValue());
            currentDemande.setStatus(statusCombo.getValue());

            if (dateCreationPicker.getValue() != null) {
                currentDemande.setDateCreation(java.sql.Date.valueOf(dateCreationPicker.getValue()));
            }

            demandeCRUD.modifier(currentDemande);
            System.out.println("✅ Demande updated");

            // Build and save details
            String detailsJson = buildDetailsJson();
            System.out.println("Details JSON: " + detailsJson);

            if (currentDetails != null) {
                currentDetails.setDetails(detailsJson);
                detailsCRUD.modifier(currentDetails);
            } else if (!detailsJson.equals("{}")) {
                DemandeDetails newDetails = new DemandeDetails();
                newDetails.setIdDemande(currentDemande.getIdDemande());
                newDetails.setDetails(detailsJson);
                detailsCRUD.ajouter(newDetails);
            }

            pendingDetailsJson = null;
            shouldFillDetails = false;

            showAlert(Alert.AlertType.INFORMATION, "Succès ✅",
                    "Demande modifiée avec succès!\n\n📋 " + currentDemande.getTitre());

            returnToDemandesList();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "❌ " + e.getMessage());
        }
    }

    private String buildDetailsJson() {
        StringBuilder json = new StringBuilder("{");
        boolean hasContent = false;

        String formDetails = formHelper.buildDetailsJson();
        if (!formDetails.equals("{}")) {
            String inner = formDetails.substring(1, formDetails.length() - 1);
            if (!inner.isEmpty()) {
                json.append(inner);
                hasContent = true;
            }
        }

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

    // ═══════════════════════════════════════════════════════════════════════════
    // NAVIGATION
    // ═══════════════════════════════════════════════════════════════════════════

    @FXML
    private void retourListe() {
        returnToDemandesList();
    }

    private void returnToDemandesList() {
        try {
            StackPane contentArea = findContentAreaByTraversal();

            if (contentArea != null) {
                loadDemandesIntoContentArea(contentArea);
                return;
            }

            Stage currentStage = getStageFromAnyNode();
            if (currentStage != null) {
                if (currentStage.getModality() != Modality.NONE) {
                    currentStage.close();
                    return;
                }

                Scene scene = currentStage.getScene();
                if (scene != null) {
                    Node contentNode = scene.lookup("#contentArea");
                    if (contentNode instanceof StackPane) {
                        loadDemandesIntoContentArea((StackPane) contentNode);
                        return;
                    }
                }
            }

            if (parentController != null) {
                parentController.loadDemandes();
                if (currentStage != null) currentStage.close();
                return;
            }

            showAlert(Alert.AlertType.WARNING, "Navigation", "Veuillez fermer cette fenêtre manuellement.");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de navigation: " + e.getMessage());
        }
    }

    private StackPane findContentAreaByTraversal() {
        Node startNode = getAnyFXMLNode();
        if (startNode == null) return null;

        Parent parent = startNode.getParent();
        int depth = 0;

        while (parent != null && depth < 50) {
            depth++;
            if (parent instanceof StackPane) {
                StackPane sp = (StackPane) parent;
                if ("contentArea".equals(sp.getId()) || sp.getStyleClass().contains("content-area")) {
                    return sp;
                }
            }
            parent = parent.getParent();
        }
        return null;
    }

    private void loadDemandesIntoContentArea(StackPane contentArea) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/demandes.fxml"));
            Parent demandesView = loader.load();
            contentArea.getChildren().setAll(demandesView);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger la liste");
        }
    }

    private Node getAnyFXMLNode() {
        if (titreField != null) return titreField;
        if (descriptionArea != null) return descriptionArea;
        if (categorieCombo != null) return categorieCombo;
        if (typeDemandeCombo != null) return typeDemandeCombo;
        if (headerLabel != null) return headerLabel;
        if (dynamicFieldsContainer != null) return dynamicFieldsContainer;
        return null;
    }

    private Stage getStageFromAnyNode() {
        Node node = getAnyFXMLNode();
        if (node != null) {
            Scene scene = node.getScene();
            if (scene != null && scene.getWindow() instanceof Stage) {
                return (Stage) scene.getWindow();
            }
        }
        return null;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}