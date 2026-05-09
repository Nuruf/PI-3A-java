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
                }
            });
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

        if (json == null || json.equals("{}") || json.trim().isEmpty()) {
            return result;
        }

        try {
            String content = json.trim();
            if (content.startsWith("{")) content = content.substring(1);
            if (content.endsWith("}")) content = content.substring(0, content.length() - 1);

            if (content.isEmpty()) return result;

            List<String> pairs = splitJsonPairs(content);

            for (String pair : pairs) {
                int colonIndex = findColonIndex(pair);
                if (colonIndex > 0) {
                    String key = removeQuotes(pair.substring(0, colonIndex).trim());
                    String value = pair.substring(colonIndex + 1).trim();

                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = removeQuotes(value);
                    }

                    key = unescapeJson(key);
                    value = unescapeJson(value);
                    result.put(key, value);
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing JSON: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    private int findColonIndex(String pair) {
        boolean inQuotes = false;
        for (int i = 0; i < pair.length(); i++) {
            char c = pair.charAt(i);
            if (c == '"' && (i == 0 || pair.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            }
            if (c == ':' && !inQuotes) {
                return i;
            }
        }
        return -1;
    }

    private List<String> splitJsonPairs(String content) {
        List<String> pairs = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        int braceDepth = 0;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            if (c == '"' && (i == 0 || content.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            }
            if (!inQuotes) {
                if (c == '{') braceDepth++;
                if (c == '}') braceDepth--;
            }
            if (c == ',' && !inQuotes && braceDepth == 0) {
                String pair = current.toString().trim();
                if (!pair.isEmpty()) {
                    pairs.add(pair);
                }
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        String lastPair = current.toString().trim();
        if (!lastPair.isEmpty()) {
            pairs.add(lastPair);
        }

        return pairs;
    }

    private String removeQuotes(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private String unescapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
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
}