package controller.demandes;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONTokener;
import org.json.JSONObject;
import service.api.MapPickerDialog;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class DemandeFormHelper {

    // ═══════════════════════════════════════════════════════════════════════════
    // INSTANCE VARIABLES
    // ═══════════════════════════════════════════════════════════════════════════

    private Map<String, Control> dynamicFields = new LinkedHashMap<>();
    private Map<String, Label> dynamicErrorLabels = new LinkedHashMap<>();
    private Set<String> locationFieldKeys = new HashSet<>();
    private Map<String, FieldDefinition> fieldDefinitions = new LinkedHashMap<>();

    // ═══════════════════════════════════════════════════════════════════════════
    // CATEGORY AND TYPE DEFINITIONS
    // ═══════════════════════════════════════════════════════════════════════════

    private static final Map<String, List<String>> CATEGORY_TYPES = new LinkedHashMap<>();
    private static final Set<String> IGNORED_JSON_KEYS = new HashSet<>(Arrays.asList(
            "createdAt",
            "confirmedAt",
            "confirmedat",
            "source",
            "type",
            "key",
            "label",
            "value",
            "required",
            "remove",
            "replaceBase",
            "manualMode",
            "model",
            "add",
            "dynamicFieldPlan",
            "suggestedDetails",
            "prompt",
            "rawPrompt"
    ));

    static {
        CATEGORY_TYPES.put("Ressources Humaines", Arrays.asList(
                "Congé",
                "Attestation de travail",
                "Attestation de salaire",
                "Certificat de travail",
                "Mutation",
                "Démission"
        ));

        CATEGORY_TYPES.put("Administrative", Arrays.asList(
                "Avance sur salaire",
                "Remboursement",
                "Matériel de bureau",
                "Badge d'accès",
                "Carte de visite"
        ));

        CATEGORY_TYPES.put("Informatique", Arrays.asList(
                "Matériel informatique",
                "Accès système",
                "Logiciel",
                "Problème technique"
        ));

        CATEGORY_TYPES.put("Formation", Arrays.asList(
                "Formation interne",
                "Formation externe",
                "Certification"
        ));

        CATEGORY_TYPES.put("Organisation du travail", Arrays.asList(
                "Télétravail",
                "Changement d'horaires",
                "Heures supplémentaires"
        ));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INITIALIZATION METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    public void initializeComboBoxes(ComboBox<String> categorieCombo,
                                     ComboBox<String> typeDemandeCombo,
                                     ComboBox<String> prioriteCombo,
                                     ComboBox<String> statusCombo) {
        if (categorieCombo != null) {
            categorieCombo.setItems(FXCollections.observableArrayList(CATEGORY_TYPES.keySet()));
        }

        if (prioriteCombo != null) {
            prioriteCombo.setItems(FXCollections.observableArrayList("HAUTE", "NORMALE", "BASSE"));
        }

        if (statusCombo != null) {
            statusCombo.setItems(FXCollections.observableArrayList(
                    "Nouvelle", "En cours", "En attente", "Résolue", "Fermée", "Annulée"));
        }

        if (categorieCombo != null && typeDemandeCombo != null) {
            categorieCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                System.out.println("Category changed: " + oldVal + " -> " + newVal);
                if (newVal != null && CATEGORY_TYPES.containsKey(newVal)) {
                    List<String> types = CATEGORY_TYPES.get(newVal);
                    System.out.println("Setting types: " + types);
                    typeDemandeCombo.setItems(FXCollections.observableArrayList(types));
                    typeDemandeCombo.setValue(null);
                } else {
                    typeDemandeCombo.getItems().clear();
                }
            });
        }
    }

    public void initializeEmployeeComboBoxes(ComboBox<String> categorieCombo,
                                             ComboBox<String> typeDemandeCombo,
                                             ComboBox<String> prioriteCombo) {
        if (categorieCombo != null) {
            categorieCombo.setItems(FXCollections.observableArrayList(CATEGORY_TYPES.keySet()));
        }

        if (prioriteCombo != null) {
            prioriteCombo.setItems(FXCollections.observableArrayList("HAUTE", "NORMALE", "BASSE"));
            prioriteCombo.setValue("NORMALE");
        }

        if (categorieCombo != null && typeDemandeCombo != null) {
            categorieCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                System.out.println("Category changed: " + oldVal + " -> " + newVal);
                if (newVal != null && CATEGORY_TYPES.containsKey(newVal)) {
                    List<String> types = CATEGORY_TYPES.get(newVal);
                    System.out.println("Setting types: " + types);
                    typeDemandeCombo.setItems(FXCollections.observableArrayList(types));
                    typeDemandeCombo.setValue(null);
                } else {
                    typeDemandeCombo.getItems().clear();
                }
            });
        }
    }

    public void setupDatePicker(DatePicker datePicker) {
        if (datePicker != null) {
            datePicker.setValue(LocalDate.now());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DYNAMIC FIELDS MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════

    public void updateDynamicFields(String typeDemande, VBox container, TitledPane detailsPane) {
        System.out.println("=== updateDynamicFields called ===");
        System.out.println("Type: " + typeDemande);

        dynamicFields.clear();
        dynamicErrorLabels.clear();
        locationFieldKeys.clear();
        fieldDefinitions.clear();

        if (container != null) {
            container.getChildren().clear();
        }

        if (typeDemande == null || typeDemande.isEmpty()) {
            Label placeholder = new Label("💡 Sélectionnez un type de demande pour voir les champs spécifiques");
            placeholder.setStyle("-fx-text-fill: #999; -fx-font-style: italic;");
            if (container != null) {
                container.getChildren().add(placeholder);
            }
            if (detailsPane != null) {
                detailsPane.setExpanded(false);
            }
            System.out.println("No type selected, showing placeholder");
            return;
        }

        if (detailsPane != null) {
            detailsPane.setExpanded(true);
        }

        List<FieldDefinition> fields = getFieldsForType(typeDemande);
        System.out.println("Fields count for '" + typeDemande + "': " + fields.size());

        if (fields.isEmpty()) {
            Label noFields = new Label("ℹ️ Aucun champ spécifique requis pour ce type");
            noFields.setStyle("-fx-text-fill: #999; -fx-font-style: italic;");
            if (container != null) {
                container.getChildren().add(noFields);
            }
            return;
        }

        Label header = new Label("📋 Informations spécifiques pour: " + typeDemande);
        header.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: #2c3e50; -fx-padding: 0 0 10 0;");
        if (container != null) {
            container.getChildren().add(header);
        }

        for (FieldDefinition field : fields) {
            System.out.println("Creating field: " + field.key + " (" + field.label + ")");
            fieldDefinitions.put(field.key, field);
            VBox fieldBox = createFieldBox(field);
            if (container != null) {
                container.getChildren().add(fieldBox);
            }
        }

        System.out.println("Dynamic fields created: " + dynamicFields.size());
    }

    /**
     * Build editable inputs from stored JSON details when the demande type does
     * not have predefined fields in the Java project.
     *
     * This is what allows AI-generated / external JSON details to be edited
     * manually in the Java UI instead of being shown as raw JSON.
     */
    public boolean buildEditableFieldsFromJson(String detailsJson, VBox container, TitledPane detailsPane) {
        dynamicFields.clear();
        dynamicErrorLabels.clear();
        locationFieldKeys.clear();
        fieldDefinitions.clear();

        if (container != null) {
            container.getChildren().clear();
        }

        if (detailsJson == null || detailsJson.trim().isEmpty() || detailsJson.trim().equals("{}")) {
            if (detailsPane != null) {
                detailsPane.setExpanded(false);
            }
            return false;
        }

        if (detailsPane != null) {
            detailsPane.setExpanded(true);
        }

        Map<String, GenericFieldItem> items = new LinkedHashMap<>();
        try {
            Object parsed = new JSONTokener(detailsJson.trim()).nextValue();
            collectGenericFieldItems(parsed, items, null, null);
        } catch (Exception e) {
            System.err.println("Error building editable fields from JSON: " + e.getMessage());
            return false;
        }

        if (items.isEmpty()) {
            return false;
        }

        Label header = new Label("📋 Champs modifiables détectés automatiquement");
        header.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: #2c3e50; -fx-padding: 0 0 10 0;");
        if (container != null) {
            container.getChildren().add(header);
        }

        for (GenericFieldItem item : items.values()) {
            if (item == null || item.key == null || item.key.trim().isEmpty()) {
                continue;
            }

            String fieldKey = normalizeKey(item.key);
            if (fieldKey.isEmpty()) {
                continue;
            }

            String fieldLabel = item.label != null && !item.label.trim().isEmpty()
                    ? humanizeLabel(item.label)
                    : humanizeLabel(item.key);

            FieldType fieldType = inferFieldType(fieldKey, item.value);
            FieldDefinition definition = new FieldDefinition(fieldKey, fieldLabel, fieldType, false);
            fieldDefinitions.put(fieldKey, definition);

            VBox fieldBox = createFieldBox(definition);
            if (container != null) {
                container.getChildren().add(fieldBox);
            }

            Control control = dynamicFields.get(fieldKey);
            if (control != null) {
                setFieldValue(control, item.value);
            }
        }

        return !dynamicFields.isEmpty();
    }

    /**
     * Fill dynamic fields from JSON - FIXED VERSION
     */
    public void fillDynamicFieldsFromJson(String detailsJson) {
        System.out.println("=== fillDynamicFieldsFromJson ===");
        System.out.println("JSON: " + detailsJson);
        System.out.println("Available fields: " + dynamicFields.keySet());

        if (detailsJson == null || detailsJson.isEmpty() || detailsJson.equals("{}")) {
            System.out.println("No details to fill");
            return;
        }

        try {
            Map<String, String> parsedValues = parseDetailsJson(detailsJson);
            System.out.println("Parsed values: " + parsedValues);

            for (Map.Entry<String, String> entry : parsedValues.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                // Skip coordinate fields
                if (key.endsWith("Lat") || key.endsWith("Lon")) {
                    continue;
                }

                Control control = findFieldByKey(key);
                if (control != null) {
                    setFieldValue(control, value);
                    System.out.println("✅ Set field '" + key + "' = '" + value + "'");

                    // Handle location coordinates
                    if (locationFieldKeys.contains(key) && control instanceof TextField) {
                        TextField locationField = (TextField) control;
                        String latStr = parsedValues.get(key + "Lat");
                        String lonStr = parsedValues.get(key + "Lon");

                        if (latStr != null && lonStr != null) {
                            try {
                                double lat = Double.parseDouble(latStr);
                                double lon = Double.parseDouble(lonStr);
                                locationField.setUserData(new double[]{lat, lon});
                                locationField.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #27ae60; -fx-border-width: 2;");
                                System.out.println("✅ Set location coordinates: " + lat + ", " + lon);
                            } catch (NumberFormatException e) {
                                System.err.println("Could not parse coordinates for " + key);
                            }
                        }
                    }
                } else {
                    System.out.println("⚠️ Field not found for key: " + key);
                }
            }

            System.out.println("✅ Dynamic fields filled from JSON");

        } catch (Exception e) {
            System.err.println("Error filling dynamic fields: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Control findFieldByKey(String key) {
        if (key == null) return null;

        // Direct match
        if (dynamicFields.containsKey(key)) {
            return dynamicFields.get(key);
        }

        // Normalized match
        String normalizedKey = normalizeKey(key);
        for (Map.Entry<String, Control> entry : dynamicFields.entrySet()) {
            if (normalizeKey(entry.getKey()).equals(normalizedKey)) {
                return entry.getValue();
            }
        }

        return null;
    }

    private String normalizeKey(String key) {
        if (key == null) return "";
        return key.toLowerCase()
                .replaceAll("^ai([ _-]+)?", "")
                .replaceAll("[\\s_-]", "")
                .replaceAll("[àâäáã]", "a")
                .replaceAll("[éèêëẽ]", "e")
                .replaceAll("[ïîíì]", "i")
                .replaceAll("[ôöóòõ]", "o")
                .replaceAll("[ùûüúũ]", "u")
                .replaceAll("ç", "c")
                .replaceAll("ñ", "n");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FIELD DEFINITIONS BY TYPE
    // ═══════════════════════════════════════════════════════════════════════════

    private List<FieldDefinition> getFieldsForType(String typeDemande) {
        List<FieldDefinition> fields = new ArrayList<>();

        if (typeDemande == null) return fields;

        switch (typeDemande) {
            case "Congé":
                fields.add(new FieldDefinition("typeConge", "Type de congé", FieldType.COMBO,
                        true, Arrays.asList("Congé annuel", "Congé maladie", "Congé sans solde",
                        "Congé maternité", "Congé paternité", "Congé exceptionnel")));
                fields.add(new FieldDefinition("dateDebut", "Date de début", FieldType.DATE, true));
                fields.add(new FieldDefinition("dateFin", "Date de fin", FieldType.DATE, true));
                fields.add(new FieldDefinition("nombreJours", "Nombre de jours", FieldType.NUMBER, true));
                fields.add(new FieldDefinition("motif", "Motif", FieldType.TEXTAREA, false));
                break;

            case "Attestation de travail":
                fields.add(new FieldDefinition("nombreExemplaires", "Nombre d'exemplaires",
                        FieldType.NUMBER, true));
                fields.add(new FieldDefinition("motifAttestation", "Motif de la demande", FieldType.COMBO,
                        true, Arrays.asList("Démarches administratives", "Banque", "Visa",
                        "Location immobilière", "Autre")));
                fields.add(new FieldDefinition("destinataire", "Destinataire (si connu)",
                        FieldType.TEXT, false));
                break;

            case "Attestation de salaire":
                fields.add(new FieldDefinition("nombreExemplaires", "Nombre d'exemplaires",
                        FieldType.NUMBER, true));
                fields.add(new FieldDefinition("periode", "Période concernée", FieldType.COMBO,
                        true, Arrays.asList("Dernier mois", "3 derniers mois", "6 derniers mois",
                        "Année en cours", "Année précédente")));
                fields.add(new FieldDefinition("motif", "Motif", FieldType.TEXT, false));
                break;

            case "Certificat de travail":
                fields.add(new FieldDefinition("nombreExemplaires", "Nombre d'exemplaires",
                        FieldType.NUMBER, true));
                fields.add(new FieldDefinition("motif", "Motif", FieldType.TEXT, false));
                break;

            case "Mutation":
                fields.add(new FieldDefinition("departementActuel", "Département actuel",
                        FieldType.TEXT, true));
                fields.add(new FieldDefinition("departementSouhaite", "Département souhaité",
                        FieldType.TEXT, true));
                fields.add(new FieldDefinition("lieuMutation", "Lieu de mutation",
                        FieldType.LOCATION, true));
                fields.add(new FieldDefinition("posteSouhaite", "Poste souhaité",
                        FieldType.TEXT, false));
                fields.add(new FieldDefinition("motif", "Motif de la demande",
                        FieldType.TEXTAREA, true));
                break;

            case "Démission":
                fields.add(new FieldDefinition("dateSouhaitee", "Date de départ souhaitée",
                        FieldType.DATE, true));
                fields.add(new FieldDefinition("preavis", "Durée de préavis", FieldType.COMBO,
                        true, Arrays.asList("1 mois", "2 mois", "3 mois", "Dispense demandée")));
                fields.add(new FieldDefinition("motif", "Motif de départ",
                        FieldType.TEXTAREA, false));
                break;

            case "Avance sur salaire":
                fields.add(new FieldDefinition("montant", "Montant demandé (TND)",
                        FieldType.NUMBER, true));
                fields.add(new FieldDefinition("modaliteRemboursement", "Modalité de remboursement",
                        FieldType.COMBO, true, Arrays.asList("1 mois", "2 mois", "3 mois", "6 mois")));
                fields.add(new FieldDefinition("motif", "Motif de la demande",
                        FieldType.TEXTAREA, true));
                break;

            case "Remboursement":
                fields.add(new FieldDefinition("typeRemboursement", "Type de remboursement",
                        FieldType.COMBO, true, Arrays.asList("Frais de transport",
                        "Frais de mission", "Frais de formation", "Frais médicaux", "Autre")));
                fields.add(new FieldDefinition("montant", "Montant (TND)", FieldType.NUMBER, true));
                fields.add(new FieldDefinition("dateDepense", "Date de la dépense",
                        FieldType.DATE, true));
                fields.add(new FieldDefinition("justificatif", "Justificatif joint", FieldType.COMBO,
                        true, Arrays.asList("Oui", "Non - à fournir")));
                fields.add(new FieldDefinition("details", "Détails", FieldType.TEXTAREA, false));
                break;

            case "Matériel de bureau":
                fields.add(new FieldDefinition("typeMateriel", "Type de matériel", FieldType.COMBO,
                        true, Arrays.asList("Fournitures", "Mobilier", "Équipement", "Autre")));
                fields.add(new FieldDefinition("descriptionMateriel", "Description du matériel",
                        FieldType.TEXT, true));
                fields.add(new FieldDefinition("quantite", "Quantité", FieldType.NUMBER, true));
                fields.add(new FieldDefinition("urgence", "Urgence", FieldType.COMBO,
                        false, Arrays.asList("Normale", "Urgente", "Très urgente")));
                break;

            case "Badge d'accès":
                fields.add(new FieldDefinition("motifBadge", "Motif de la demande", FieldType.COMBO,
                        true, Arrays.asList("Nouveau badge", "Badge perdu", "Badge défectueux",
                        "Extension d'accès")));
                fields.add(new FieldDefinition("zonesAcces", "Zones d'accès demandées",
                        FieldType.TEXT, false));
                break;

            case "Carte de visite":
                fields.add(new FieldDefinition("quantiteCarte", "Quantité", FieldType.COMBO,
                        true, Arrays.asList("50", "100", "200", "500")));
                fields.add(new FieldDefinition("titreFonction", "Titre/Fonction à afficher",
                        FieldType.TEXT, true));
                fields.add(new FieldDefinition("telephone", "Numéro de téléphone",
                        FieldType.TEXT, false));
                fields.add(new FieldDefinition("email", "Email", FieldType.TEXT, false));
                break;

            case "Matériel informatique":
                fields.add(new FieldDefinition("typeMaterielInfo", "Type de matériel", FieldType.COMBO,
                        true, Arrays.asList("Ordinateur portable", "Ordinateur fixe", "Écran",
                        "Clavier/Souris", "Casque", "Webcam", "Autre")));
                fields.add(new FieldDefinition("motifMateriel", "Motif", FieldType.COMBO,
                        true, Arrays.asList("Nouveau besoin", "Remplacement", "Mise à niveau")));
                fields.add(new FieldDefinition("specifications", "Spécifications souhaitées",
                        FieldType.TEXTAREA, false));
                break;

            case "Accès système":
                fields.add(new FieldDefinition("systeme", "Système/Application",
                        FieldType.TEXT, true));
                fields.add(new FieldDefinition("typeAcces", "Type d'accès", FieldType.COMBO,
                        true, Arrays.asList("Lecture seule", "Lecture/Écriture", "Administrateur")));
                fields.add(new FieldDefinition("justification", "Justification",
                        FieldType.TEXTAREA, true));
                break;

            case "Logiciel":
                fields.add(new FieldDefinition("nomLogiciel", "Nom du logiciel",
                        FieldType.TEXT, true));
                fields.add(new FieldDefinition("version", "Version (si applicable)",
                        FieldType.TEXT, false));
                fields.add(new FieldDefinition("typeLicence", "Type de licence", FieldType.COMBO,
                        false, Arrays.asList("Achat", "Abonnement mensuel", "Abonnement annuel",
                        "Open source")));
                fields.add(new FieldDefinition("justificationLogiciel", "Justification du besoin",
                        FieldType.TEXTAREA, true));
                break;

            case "Problème technique":
                fields.add(new FieldDefinition("typeProbleme", "Type de problème", FieldType.COMBO,
                        true, Arrays.asList("Matériel", "Logiciel", "Réseau", "Email",
                        "Imprimante", "Autre")));
                fields.add(new FieldDefinition("descriptionProbleme", "Description du problème",
                        FieldType.TEXTAREA, true));
                fields.add(new FieldDefinition("impact", "Impact sur le travail", FieldType.COMBO,
                        true, Arrays.asList("Bloquant", "Important", "Modéré", "Faible")));
                break;

            case "Formation interne":
                fields.add(new FieldDefinition("nomFormation", "Nom de la formation",
                        FieldType.TEXT, true));
                fields.add(new FieldDefinition("formateur", "Formateur (si connu)",
                        FieldType.TEXT, false));
                fields.add(new FieldDefinition("dateSouhaiteeFormation", "Date souhaitée",
                        FieldType.DATE, false));
                fields.add(new FieldDefinition("objectifFormation", "Objectif de la formation",
                        FieldType.TEXTAREA, true));
                break;

            case "Formation externe":
                fields.add(new FieldDefinition("nomFormationExt", "Nom de la formation",
                        FieldType.TEXT, true));
                fields.add(new FieldDefinition("organisme", "Organisme de formation",
                        FieldType.TEXT, true));
                fields.add(new FieldDefinition("lieuFormation", "Lieu de formation",
                        FieldType.LOCATION, true));
                fields.add(new FieldDefinition("duree", "Durée", FieldType.TEXT, true));
                fields.add(new FieldDefinition("cout", "Coût estimé (TND)",
                        FieldType.NUMBER, false));
                fields.add(new FieldDefinition("dateDebutFormation", "Date de début souhaitée",
                        FieldType.DATE, false));
                fields.add(new FieldDefinition("objectif", "Objectif et bénéfices attendus",
                        FieldType.TEXTAREA, true));
                break;

            case "Certification":
                fields.add(new FieldDefinition("nomCertification", "Nom de la certification",
                        FieldType.TEXT, true));
                fields.add(new FieldDefinition("organismeCertif", "Organisme certificateur",
                        FieldType.TEXT, true));
                fields.add(new FieldDefinition("lieuExamen", "Lieu d'examen",
                        FieldType.LOCATION, true));
                fields.add(new FieldDefinition("coutCertif", "Coût (TND)", FieldType.NUMBER, false));
                fields.add(new FieldDefinition("datePassage", "Date de passage souhaitée",
                        FieldType.DATE, false));
                fields.add(new FieldDefinition("justificationCertif", "Justification",
                        FieldType.TEXTAREA, true));
                break;

            case "Télétravail":
                fields.add(new FieldDefinition("typeTeletravail", "Type de demande", FieldType.COMBO,
                        true, Arrays.asList("Télétravail régulier", "Télétravail occasionnel",
                        "Télétravail exceptionnel")));
                fields.add(new FieldDefinition("joursParSemaine", "Jours par semaine", FieldType.COMBO,
                        true, Arrays.asList("1 jour", "2 jours", "3 jours", "4 jours", "Temps plein")));
                fields.add(new FieldDefinition("joursSouhaites", "Jours souhaités (ex: Lundi, Mardi)",
                        FieldType.TEXT, false));
                fields.add(new FieldDefinition("adresseTeletravail", "Adresse de télétravail",
                        FieldType.LOCATION, true));
                fields.add(new FieldDefinition("dateDebutTeletravail", "Date de début", FieldType.DATE, true));
                fields.add(new FieldDefinition("dateFinTeletravail", "Date de fin (si temporaire)",
                        FieldType.DATE, false));
                fields.add(new FieldDefinition("motifTeletravail", "Motif", FieldType.TEXTAREA, false));
                break;

            case "Changement d'horaires":
                fields.add(new FieldDefinition("horairesActuels", "Horaires actuels",
                        FieldType.TEXT, true));
                fields.add(new FieldDefinition("horairesSouhaites", "Horaires souhaités",
                        FieldType.TEXT, true));
                fields.add(new FieldDefinition("dateDebutHoraires", "Date de début", FieldType.DATE, true));
                fields.add(new FieldDefinition("dureeChangement", "Durée", FieldType.COMBO,
                        false, Arrays.asList("Temporaire - 1 mois", "Temporaire - 3 mois",
                        "Temporaire - 6 mois", "Permanent")));
                fields.add(new FieldDefinition("motifHoraires", "Motif", FieldType.TEXTAREA, true));
                break;

            case "Heures supplémentaires":
                fields.add(new FieldDefinition("dateHeuresSup", "Date", FieldType.DATE, true));
                fields.add(new FieldDefinition("nombreHeures", "Nombre d'heures",
                        FieldType.NUMBER, true));
                fields.add(new FieldDefinition("heureDebut", "Heure de début (ex: 18:00)",
                        FieldType.TEXT, true));
                fields.add(new FieldDefinition("heureFin", "Heure de fin (ex: 21:00)",
                        FieldType.TEXT, true));
                fields.add(new FieldDefinition("motifHeuresSup", "Motif/Projet", FieldType.TEXTAREA, true));
                fields.add(new FieldDefinition("valideParResponsable", "Validé par responsable", FieldType.COMBO,
                        true, Arrays.asList("Oui", "En attente de validation")));
                break;

            default:
                System.out.println("Unknown type: " + typeDemande);
                break;
        }

        return fields;
    }

    private void collectGenericFieldItems(Object node, Map<String, GenericFieldItem> items, String inheritedKey, String inheritedLabel) {
        if (node == null || node == JSONObject.NULL) {
            return;
        }

        if (node instanceof String) {
            String text = ((String) node).trim();
            if (text.isEmpty()) return;
            if (looksLikeJson(text)) {
                try {
                    collectGenericFieldItems(new JSONTokener(text).nextValue(), items, inheritedKey, inheritedLabel);
                } catch (Exception ignored) {
                    // keep as plain text if parsing fails
                }
            }
            return;
        }

        if (node instanceof JSONArray) {
            JSONArray array = (JSONArray) node;
            for (int i = 0; i < array.length(); i++) {
                collectGenericFieldItems(array.opt(i), items, inheritedKey, inheritedLabel);
            }
            return;
        }

        if (node instanceof JSONObject) {
            JSONObject object = (JSONObject) node;

            // Field-like object: {key,label,value,...}
            if (object.has("value") && !object.isNull("value")) {
                Object valueNode = object.opt("value");
                String key = object.optString("key", inheritedKey != null ? inheritedKey : "");
                String label = object.optString("label", inheritedLabel != null ? inheritedLabel : "");

                if (valueNode instanceof JSONObject || valueNode instanceof JSONArray) {
                    collectGenericFieldItems(valueNode, items, key, label);
                } else {
                    String value = normalizeValue(valueNode);
                    String normalizedKey = normalizeKey(key);
                    if (!value.isEmpty() && !normalizedKey.isEmpty() && !isTechnicalKey(key)) {
                        putGenericFieldItem(items, normalizedKey, new GenericFieldItem(key, label, value));
                    }
                }
            }

            for (String key : object.keySet()) {
                if (isTechnicalKey(key)) {
                    Object child = object.opt(key);
                    if (child instanceof JSONObject || child instanceof JSONArray) {
                        collectGenericFieldItems(child, items, inheritedKey, inheritedLabel);
                    }
                    continue;
                }

                Object child = object.opt(key);
                if (child == null || child == JSONObject.NULL) {
                    continue;
                }

                if (child instanceof JSONObject || child instanceof JSONArray) {
                    collectGenericFieldItems(child, items, key, humanizeLabel(key));
                } else {
                    String value = normalizeValue(child);
                    String normalizedKey = normalizeKey(key);
                    if (!value.isEmpty() && !normalizedKey.isEmpty()) {
                        putGenericFieldItem(items, normalizedKey, new GenericFieldItem(key, humanizeLabel(key), value));
                    }
                }
            }
        }
    }

    private void putGenericFieldItem(Map<String, GenericFieldItem> items, String normalizedKey, GenericFieldItem item) {
        if (items == null || normalizedKey == null || normalizedKey.trim().isEmpty() || item == null) {
            return;
        }

        GenericFieldItem existing = items.get(normalizedKey);
        if (existing == null || isBetterGenericItem(existing, item)) {
            items.put(normalizedKey, item);
        }
    }

    private boolean isBetterGenericItem(GenericFieldItem current, GenericFieldItem candidate) {
        if (current == null) return true;
        if (candidate == null) return false;

        String currentValue = current.value != null ? current.value.trim() : "";
        String candidateValue = candidate.value != null ? candidate.value.trim() : "";

        // Prefer human-friendly text over nested/raw JSON.
        if (looksLikeJson(currentValue) && !looksLikeJson(candidateValue)) {
            return true;
        }

        // Prefer shorter metadata-free labels when both are plain text.
        boolean currentTechnical = isTechnicalKey(current.key) || isTechnicalKey(current.label);
        boolean candidateTechnical = isTechnicalKey(candidate.key) || isTechnicalKey(candidate.label);
        return currentTechnical && !candidateTechnical;
    }

    private FieldType inferFieldType(String key, String value) {
        String lowerKey = key != null ? key.toLowerCase(Locale.ROOT) : "";
        String safeValue = value != null ? value.trim() : "";

        if (lowerKey.contains("date") || lowerKey.contains("jour") || lowerKey.contains("deadline")) {
            return FieldType.DATE;
        }
        if (lowerKey.contains("montant") || lowerKey.contains("quantite") || lowerKey.contains("nombre")
                || lowerKey.contains("hours") || lowerKey.contains("heure") || lowerKey.contains("durée")
                || lowerKey.contains("duree")) {
            return FieldType.NUMBER;
        }
        if (safeValue.contains("\n") || safeValue.length() > 80) {
            return FieldType.TEXTAREA;
        }
        return FieldType.TEXT;
    }

    private String humanizeLabel(String key) {
        if (key == null) return "";

        String normalized = key.trim().toLowerCase(Locale.ROOT)
                .replaceAll("^ai([ _-]+)?", "")
                .replaceAll("[\\s_-]", "")
                .replaceAll("[àâäáã]", "a")
                .replaceAll("[éèêëẽ]", "e")
                .replaceAll("[ïîíì]", "i")
                .replaceAll("[ôöóòõ]", "o")
                .replaceAll("[ùûüúũ]", "u")
                .replaceAll("ç", "c")
                .replaceAll("ñ", "n");

        Map<String, String> translations = new LinkedHashMap<>();
        translations.put("horrairesouhaite", "Horaire souhaité");
        translations.put("horairesouhaite", "Horaire souhaité");
        translations.put("horraires", "Horaires");
        translations.put("periodeconcernee", "Période concernée");
        translations.put("motifchangement", "Motif du changement");
        translations.put("horairessouhaites", "Horaires souhaités");
        translations.put("horairesactuels", "Horaires actuels");
        translations.put("jourssouhaites", "Jours souhaités");
        translations.put("datedebut", "Date de début");
        translations.put("datefin", "Date de fin");
        translations.put("dateheure", "Date / heure");
        translations.put("typedemande", "Type de demande");
        translations.put("motif", "Motif");
        translations.put("justification", "Justification");
        translations.put("descriptionprobleme", "Description du problème");
        translations.put("impact", "Impact");
        translations.put("organisme", "Organisme");
        translations.put("lieuformation", "Lieu de formation");
        translations.put("cout", "Coût");
        translations.put("nomformationext", "Nom de la formation");
        translations.put("nomlogiciel", "Nom du logiciel");
        translations.put("systeme", "Système / application");
        translations.put("typeacces", "Type d'accès");
        translations.put("typeprobleme", "Type de problème");
        translations.put("specifications", "Spécifications souhaitées");
        translations.put("quantite", "Quantité");
        translations.put("montant", "Montant");
        translations.put("duree", "Durée");

        if (translations.containsKey(normalized)) {
            return translations.get(normalized);
        }

        String label = key.trim()
                .replaceAll("^ai([ _-]+)?", "")
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .replace('_', ' ')
                .replace('-', ' ')
                .trim();

        if (label.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (String part : label.split("\\s+")) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) {
                sb.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return sb.toString();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FIELD CREATION
    // ═══════════════════════════════════════════════════════════════════════════

    private VBox createFieldBox(FieldDefinition field) {
        VBox fieldBox = new VBox(5);
        fieldBox.setPadding(new Insets(8));
        fieldBox.setStyle("-fx-background-color: #f9f9f9; -fx-background-radius: 6; -fx-padding: 10;");

        Label label = new Label(field.label + (field.required ? " *" : ""));
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11;");
        dynamicErrorLabels.put(field.key, errorLabel);

        if (field.type == FieldType.LOCATION) {
            HBox locationBox = createLocationField(field);
            fieldBox.getChildren().addAll(label, locationBox, errorLabel);
        } else {
            Control control = createControl(field);
            dynamicFields.put(field.key, control);
            addClearErrorListener(control, errorLabel);
            fieldBox.getChildren().addAll(label, control, errorLabel);
        }

        return fieldBox;
    }

    private Control createControl(FieldDefinition field) {
        Control control;

        switch (field.type) {
            case TEXT:
                TextField textField = new TextField();
                textField.setPromptText("Entrez " + field.label.toLowerCase());
                textField.setPrefWidth(400);
                textField.setMaxWidth(Double.MAX_VALUE);
                textField.setStyle("-fx-background-radius: 6;");
                control = textField;
                break;

            case NUMBER:
                TextField numberField = new TextField();
                numberField.setPromptText("Entrez un nombre");
                numberField.setPrefWidth(200);
                numberField.setStyle("-fx-background-radius: 6;");
                numberField.textProperty().addListener((o, ov, nv) -> {
                    if (nv != null && !nv.matches("\\d*\\.?\\d*")) {
                        numberField.setText(ov);
                    }
                });
                control = numberField;
                break;

            case TEXTAREA:
                TextArea textArea = new TextArea();
                textArea.setPromptText("Entrez " + field.label.toLowerCase());
                textArea.setPrefRowCount(3);
                textArea.setPrefWidth(400);
                textArea.setMaxWidth(Double.MAX_VALUE);
                textArea.setWrapText(true);
                textArea.setStyle("-fx-background-radius: 6;");
                control = textArea;
                break;

            case DATE:
                DatePicker datePicker = new DatePicker();
                datePicker.setPromptText("Sélectionnez une date");
                datePicker.setPrefWidth(200);
                datePicker.setStyle("-fx-background-radius: 6;");
                control = datePicker;
                break;

            case COMBO:
                ComboBox<String> comboBox = new ComboBox<>();
                if (field.options != null) {
                    comboBox.setItems(FXCollections.observableArrayList(field.options));
                }
                comboBox.setPromptText("Sélectionnez...");
                comboBox.setPrefWidth(300);
                comboBox.setMaxWidth(Double.MAX_VALUE);
                comboBox.setStyle("-fx-background-radius: 6;");
                control = comboBox;
                break;

            default:
                TextField defaultField = new TextField();
                defaultField.setStyle("-fx-background-radius: 6;");
                control = defaultField;
                break;
        }

        return control;
    }

    private HBox createLocationField(FieldDefinition field) {
        HBox locationBox = new HBox(10);
        locationBox.setAlignment(Pos.CENTER_LEFT);
        locationBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(locationBox, Priority.ALWAYS);

        TextField locationField = new TextField();
        locationField.setPromptText("Cliquez sur 📍 pour sélectionner une adresse");
        locationField.setEditable(false);
        locationField.setMaxWidth(Double.MAX_VALUE);
        locationField.setPrefWidth(350);
        locationField.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 6;");
        HBox.setHgrow(locationField, Priority.ALWAYS);

        Button mapButton = new Button("📍 Carte");
        mapButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                "-fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 6;");
        mapButton.setMinWidth(100);

        mapButton.setOnAction(e -> {
            try {
                MapPickerDialog dialog = new MapPickerDialog();
                dialog.show(result -> {
                    if (result != null) {
                        Platform.runLater(() -> {
                            locationField.setText(result.cityName);
                            locationField.setUserData(new double[]{result.lat, result.lon});
                            locationField.setStyle("-fx-background-color: #f8f9fa; " +
                                    "-fx-border-color: #27ae60; -fx-border-width: 2; -fx-background-radius: 6;");

                            System.out.println("📍 Location selected: " + result.cityName +
                                    " (Lat: " + result.lat + ", Lon: " + result.lon + ")");

                            Label errorLabel = dynamicErrorLabels.get(field.key);
                            if (errorLabel != null) {
                                errorLabel.setText("");
                            }
                        });
                    }
                });
            } catch (Exception ex) {
                System.err.println("Error opening map: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        mapButton.setOnMouseEntered(e ->
                mapButton.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; " +
                        "-fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 6;"));
        mapButton.setOnMouseExited(e ->
                mapButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                        "-fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 6;"));

        locationBox.getChildren().addAll(locationField, mapButton);

        dynamicFields.put(field.key, locationField);
        locationFieldKeys.add(field.key);

        Label errorLabel = dynamicErrorLabels.get(field.key);
        if (errorLabel != null) {
            locationField.textProperty().addListener((o, ov, nv) -> {
                if (nv != null && !nv.trim().isEmpty()) {
                    clearFieldError(locationField, errorLabel);
                }
            });
        }

        return locationBox;
    }

    private void addClearErrorListener(Control control, Label errorLabel) {
        if (control instanceof TextField) {
            ((TextField) control).textProperty().addListener((o, ov, nv) -> {
                if (nv != null && !nv.trim().isEmpty()) {
                    clearFieldError(control, errorLabel);
                }
            });
        } else if (control instanceof TextArea) {
            ((TextArea) control).textProperty().addListener((o, ov, nv) -> {
                if (nv != null && !nv.trim().isEmpty()) {
                    clearFieldError(control, errorLabel);
                }
            });
        } else if (control instanceof ComboBox) {
            ((ComboBox<?>) control).valueProperty().addListener((o, ov, nv) -> {
                if (nv != null) {
                    clearFieldError(control, errorLabel);
                }
            });
        } else if (control instanceof DatePicker) {
            ((DatePicker) control).valueProperty().addListener((o, ov, nv) -> {
                if (nv != null) {
                    clearFieldError(control, errorLabel);
                    // Validate all date ranges when any date changes
                    validateDateRangesRealtime();
                }
            });
        }
    }

    /**
     * Real-time validation of date ranges as user enters dates
     */
    private void validateDateRangesRealtime() {
        String[][] dateRangePairs = {
                {"dateDebut", "dateFin"},
                {"dateDebutTeletravail", "dateFinTeletravail"},
                {"dateDebutFormation", "dateDebutFormation"}
        };

        for (String[] pair : dateRangePairs) {
            String startKey = pair[0];
            String endKey = pair[1];

            Control startControl = dynamicFields.get(startKey);
            Control endControl = dynamicFields.get(endKey);

            if (startControl instanceof DatePicker && endControl instanceof DatePicker) {
                DatePicker startPicker = (DatePicker) startControl;
                DatePicker endPicker = (DatePicker) endControl;

                LocalDate startDate = startPicker.getValue();
                LocalDate endDate = endPicker.getValue();

                Label errorLabel = dynamicErrorLabels.get(endKey);

                if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
                    if (errorLabel != null) {
                        errorLabel.setText("⚠️ La date de fin doit être >= à la date de début");
                    }
                    endPicker.setStyle(endPicker.getStyle().replaceAll("-fx-border-color:[^;]*;?", "") +
                            "; -fx-border-color: #e74c3c; -fx-border-width: 2;");
                } else if (startDate != null && endDate != null) {
                    // Dates are valid, clear any error
                    if (errorLabel != null) {
                        errorLabel.setText("");
                    }
                    endPicker.setStyle(endPicker.getStyle().replaceAll("-fx-border-color:[^;]*;?", ""));
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // VALIDATION
    // ═══════════════════════════════════════════════════════════════════════════

    public boolean validateDynamicFields() {
        boolean valid = true;

        for (Map.Entry<String, Control> entry : dynamicFields.entrySet()) {
            String key = entry.getKey();
            Control control = entry.getValue();
            Label errorLabel = dynamicErrorLabels.get(key);

            FieldDefinition fieldDef = fieldDefinitions.get(key);
            boolean isRequired = fieldDef != null && fieldDef.required;

            if (isRequired && isFieldEmpty(control)) {
                if (errorLabel != null) {
                    errorLabel.setText("Ce champ est obligatoire");
                }
                control.setStyle(control.getStyle() + "; -fx-border-color: #e74c3c; -fx-border-width: 2;");
                valid = false;
            }
        }

        // Validate date ranges (dateFin >= dateDebut, etc.)
        valid = validateDateRanges() && valid;

        return valid;
    }

    /**
     * Validate that end dates are >= start dates
     */
    private boolean validateDateRanges() {
        boolean valid = true;

        // Define date range pairs to validate: [startKey, endKey]
        String[][] dateRangePairs = {
                {"dateDebut", "dateFin"},
                {"dateDebutTeletravail", "dateFinTeletravail"},
                {"dateDebutFormation", "dateDebutFormation"}, // If there's an end date for formations
                {"dateDebutHoraires", "dureeChangement"} // Special case, check if applicable
        };

        for (String[] pair : dateRangePairs) {
            String startKey = pair[0];
            String endKey = pair[1];

            Control startControl = dynamicFields.get(startKey);
            Control endControl = dynamicFields.get(endKey);

            if (startControl instanceof DatePicker && endControl instanceof DatePicker) {
                DatePicker startPicker = (DatePicker) startControl;
                DatePicker endPicker = (DatePicker) endControl;

                LocalDate startDate = startPicker.getValue();
                LocalDate endDate = endPicker.getValue();

                if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
                    Label errorLabel = dynamicErrorLabels.get(endKey);
                    if (errorLabel != null) {
                        errorLabel.setText("⚠️ La date de fin doit être supérieure ou égale à la date de début");
                    }
                    endPicker.setStyle(endPicker.getStyle() + "; -fx-border-color: #e74c3c; -fx-border-width: 2;");
                    valid = false;
                }
            }
        }

        return valid;
    }

    private boolean isFieldEmpty(Control control) {
        if (control == null) return true;

        if (control instanceof TextField) {
            String text = ((TextField) control).getText();
            return text == null || text.trim().isEmpty();
        } else if (control instanceof TextArea) {
            String text = ((TextArea) control).getText();
            return text == null || text.trim().isEmpty();
        } else if (control instanceof ComboBox) {
            return ((ComboBox<?>) control).getValue() == null;
        } else if (control instanceof DatePicker) {
            return ((DatePicker) control).getValue() == null;
        }
        return true;
    }

    public void clearFieldError(Control control, Label errorLabel) {
        if (control != null) {
            String style = control.getStyle();
            if (style != null) {
                style = style.replaceAll("-fx-border-color:[^;]*;?", "")
                        .replaceAll("-fx-border-width:[^;]*;?", "");
                control.setStyle(style);
            }
        }
        if (errorLabel != null) {
            errorLabel.setText("");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FIELD VALUE MANIPULATION
    // ═══════════════════════════════════════════════════════════════════════════

    public void setFieldValue(Control control, String value) {
        if (control == null || value == null) return;

        try {
            if (control instanceof TextField) {
                ((TextField) control).setText(value);
            } else if (control instanceof TextArea) {
                ((TextArea) control).setText(value);
            } else if (control instanceof ComboBox) {
                @SuppressWarnings("unchecked")
                ComboBox<String> comboBox = (ComboBox<String>) control;
                if (!comboBox.getItems().contains(value) && !value.isEmpty()) {
                    comboBox.getItems().add(value);
                }
                comboBox.setValue(value);
            } else if (control instanceof DatePicker) {
                DatePicker datePicker = (DatePicker) control;
                LocalDate date = parseDate(value);
                if (date != null) {
                    datePicker.setValue(date);
                }
            }
        } catch (Exception e) {
            System.err.println("Error setting field value: " + e.getMessage());
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isEmpty()) return null;

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ignored) {}

        String[] formats = {"dd/MM/yyyy", "dd-MM-yyyy", "yyyy/MM/dd", "MM/dd/yyyy"};
        for (String format : formats) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {}
        }

        System.err.println("Could not parse date: " + value);
        return null;
    }

    public String getFieldValue(Control control) {
        if (control == null) return "";

        if (control instanceof TextField) {
            String text = ((TextField) control).getText();
            return text != null ? text.trim() : "";
        } else if (control instanceof TextArea) {
            String text = ((TextArea) control).getText();
            return text != null ? text.trim() : "";
        } else if (control instanceof ComboBox) {
            Object value = ((ComboBox<?>) control).getValue();
            return value != null ? value.toString() : "";
        } else if (control instanceof DatePicker) {
            LocalDate date = ((DatePicker) control).getValue();
            return date != null ? date.format(DateTimeFormatter.ISO_DATE) : "";
        }
        return "";
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // JSON BUILDING
    // ═══════════════════════════════════════════════════════════════════════════

    public String buildDetailsJson() {
        if (dynamicFields.isEmpty()) {
            return "{}";
        }

        StringBuilder json = new StringBuilder("{");
        boolean first = true;

        for (Map.Entry<String, Control> entry : dynamicFields.entrySet()) {
            String key = entry.getKey();
            Control control = entry.getValue();
            String value = getFieldValue(control);

            if (value != null && !value.isEmpty()) {
                if (!first) {
                    json.append(",");
                }
                json.append("\"").append(escapeJson(key)).append("\":\"")
                        .append(escapeJson(value)).append("\"");
                first = false;

                if (locationFieldKeys.contains(key) && control instanceof TextField) {
                    TextField locationField = (TextField) control;
                    Object userData = locationField.getUserData();
                    if (userData instanceof double[]) {
                        double[] coords = (double[]) userData;
                        json.append(",\"").append(escapeJson(key + "Lat")).append("\":").append(coords[0]);
                        json.append(",\"").append(escapeJson(key + "Lon")).append("\":").append(coords[1]);
                    }
                }
            }
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
    // JSON PARSING
    // ═══════════════════════════════════════════════════════════════════════════

    public Map<String, String> parseDetailsJson(String json) {
        Map<String, String> result = new LinkedHashMap<>();
        collectDetails(json, result, false);
        return result;
    }

    /**
     * Returns readable label/value pairs for display purposes.
     * This keeps only the human-friendly values and skips technical ML metadata.
     */
    public Map<String, String> extractReadableDetails(String json) {
        Map<String, String> result = new LinkedHashMap<>();
        collectDetails(json, result, true);
        return result;
    }

    private void collectDetails(String json, Map<String, String> result, boolean displayMode) {
        if (json == null || json.trim().isEmpty() || "{}".equals(json.trim())) {
            return;
        }

        try {
            Object parsed = new JSONTokener(json.trim()).nextValue();
            collectDetails(parsed, result, displayMode);
        } catch (Exception e) {
            System.err.println("Error parsing JSON: " + e.getMessage());
        }
    }

    private void collectDetails(Object node, Map<String, String> result, boolean displayMode) {
        if (node == null || node == JSONObject.NULL) {
            return;
        }

        if (node instanceof String) {
            String text = ((String) node).trim();
            if (text.isEmpty()) return;

            if (looksLikeJson(text)) {
                try {
                    collectDetails(new JSONTokener(text).nextValue(), result, displayMode);
                } catch (Exception ignored) {
                    // Fall through: plain text string
                }
            }
            return;
        }

        if (node instanceof JSONArray) {
            JSONArray array = (JSONArray) node;
            for (int i = 0; i < array.length(); i++) {
                collectDetails(array.opt(i), result, displayMode);
            }
            return;
        }

        if (node instanceof JSONObject) {
            JSONObject object = (JSONObject) node;

            // If this object is a structured field like {label, key, value}, keep only the meaningful value.
            if (object.has("value") && !object.isNull("value")) {
                handleStructuredField(object, result, displayMode);
            }

            for (String key : object.keySet()) {
                if (isTechnicalKey(key)) {
                    Object child = object.opt(key);
                    if (child instanceof JSONObject || child instanceof JSONArray) {
                        collectDetails(child, result, displayMode);
                    }
                    continue;
                }

                Object child = object.opt(key);
                if (child == null || child == JSONObject.NULL) {
                    continue;
                }

                if (child instanceof JSONObject || child instanceof JSONArray) {
                    collectDetails(child, result, displayMode);
                } else if (!displayMode) {
                    addEntry(result, key, normalizeValue(child), false);
                } else {
                    addEntry(result, formatDisplayKey(key), normalizeValue(child), true);
                }
            }
        }
    }

    private void handleStructuredField(JSONObject object, Map<String, String> result, boolean displayMode) {
        Object valueNode = object.opt("value");
        if (valueNode == null || valueNode == JSONObject.NULL) {
            return;
        }

        // If the "value" is itself JSON, recurse into it instead of printing raw JSON.
        if (valueNode instanceof JSONObject || valueNode instanceof JSONArray) {
            collectDetails(valueNode, result, displayMode);
            return;
        }

        String value = normalizeValue(valueNode);
        if (value.isEmpty()) {
            return;
        }

        String technicalKey = normalizeKey(object.optString("key", ""));
        String label = object.optString("label", "").trim();

        if (displayMode) {
            String displayKey = !label.isEmpty() ? label : formatDisplayKey(object.optString("key", ""));
            addEntry(result, displayKey, value, true);
        } else {
            String storageKey = !technicalKey.isEmpty() ? technicalKey : normalizeKey(label);
            if (!storageKey.isEmpty()) {
                addEntry(result, storageKey, value, false);
            }
        }
    }

    private void addEntry(Map<String, String> result, String key, String value, boolean displayMode) {
        if (key == null || key.trim().isEmpty() || value == null || value.trim().isEmpty()) {
            return;
        }

        String normalizedKey = displayMode ? key.trim() : normalizeKey(key);
        if (normalizedKey.isEmpty()) {
            return;
        }

        // Do not overwrite a better human-readable value with a raw nested copy.
        if (!result.containsKey(normalizedKey) || isBetterValue(result.get(normalizedKey), value)) {
            result.put(normalizedKey, value.trim());
        }
    }

    private boolean isBetterValue(String current, String candidate) {
        if (current == null || current.trim().isEmpty()) return true;
        if (candidate == null || candidate.trim().isEmpty()) return false;
        return looksLikeJson(current) && !looksLikeJson(candidate);
    }

    private boolean looksLikeJson(String text) {
        if (text == null) return false;
        String trimmed = text.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }

    private boolean isTechnicalKey(String key) {
        if (key == null) return false;
        return IGNORED_JSON_KEYS.contains(key.trim()) || IGNORED_JSON_KEYS.contains(key.trim().toLowerCase(Locale.ROOT));
    }

    private String normalizeValue(Object value) {
        if (value == null || value == JSONObject.NULL) {
            return "";
        }
        if (value instanceof JSONObject || value instanceof JSONArray) {
            return value.toString();
        }
        return String.valueOf(value).replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t").trim();
    }


    private String formatDisplayKey(String key) {
        if (key == null || key.trim().isEmpty()) return "";

        String cleaned = key.trim()
                .replaceAll("^[aA][iI][ _-]+", "")
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .replace('_', ' ')
                .trim();

        if (cleaned.isEmpty()) return "";
        return cleaned.substring(0, 1).toUpperCase(Locale.ROOT) + cleaned.substring(1);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════════════════════

    public Map<String, Control> getDynamicFields() {
        return dynamicFields;
    }

    public Map<String, Label> getDynamicErrorLabels() {
        return dynamicErrorLabels;
    }

    public boolean isLocationField(String key) {
        return locationFieldKeys.contains(key);
    }

    public Set<String> getLocationFieldKeys() {
        return locationFieldKeys;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INNER CLASSES
    // ═══════════════════════════════════════════════════════════════════════════

    private enum FieldType {
        TEXT, NUMBER, TEXTAREA, DATE, COMBO, LOCATION
    }

    private static class FieldDefinition {
        String key;
        String label;
        FieldType type;
        boolean required;
        List<String> options;

        FieldDefinition(String key, String label, FieldType type, boolean required) {
            this(key, label, type, required, null);
        }

        FieldDefinition(String key, String label, FieldType type, boolean required, List<String> options) {
            this.key = key;
            this.label = label;
            this.type = type;
            this.required = required;
            this.options = options;
        }
    }

    private static class GenericFieldItem {
        final String key;
        final String label;
        final String value;

        GenericFieldItem(String key, String label, String value) {
            this.key = key;
            this.label = label;
            this.value = value;
        }
    }
}