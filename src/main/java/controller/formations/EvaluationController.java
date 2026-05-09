package controller.formations;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import entities.formation.Evaluation;
import entities.formation.inscription_formation;
import entities.formation.StatutInscription;
import entities.employers.session;
import entities.employers.employe;
import service.formation.EvaluationCRUD;
import service.formation.inscription_formationCRUD;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Contrôleur pour gérer les évaluations des formations
 */
public class EvaluationController {

    @FXML
    private Label lblFormationTitle;

    @FXML
    private Label lblAverageRating;

    @FXML
    private Spinner<Integer> spinnerNote;

    @FXML
    private Label lblStars;

    @FXML
    private TextArea txtCommentaire;

    @FXML
    private Button btnSubmitEvaluation;

    @FXML
    private VBox evaluationsContainer;

    private final EvaluationCRUD evaluationService = new EvaluationCRUD();
    private final inscription_formationCRUD inscriptionService = new inscription_formationCRUD();
    private int currentFormationId;
    private Runnable onEvaluationUpdated;

    @FXML
    private void initialize() {
        // Configurer le spinner pour les notes (1-5)
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5, 5);
        spinnerNote.setValueFactory(valueFactory);

        // Mettre à jour l'affichage des étoiles lors du changement de note
        spinnerNote.valueProperty().addListener((obs, oldVal, newVal) -> updateStarDisplay(newVal));

        // Affichage initial
        updateStarDisplay(5);
    }

    /**
     * Initialiser le contrôleur avec les informations de la formation
     */
    public void setFormationData(int formationId, String formationTitle, Runnable onUpdated) {
        this.currentFormationId = formationId;
        this.onEvaluationUpdated = onUpdated;

        lblFormationTitle.setText(formationTitle);
        loadEvaluations();
        loadAverageRating();
        loadUserEvaluation();
    }

    /**
     * Mettre à jour l'affichage des étoiles
     */
    private void updateStarDisplay(int note) {
        lblStars.setText("⭐".repeat(note));
    }

    /**
     * Charger la note moyenne et le nombre d'évaluations
     */
    private void loadAverageRating() {
        new Thread(() -> {
            try {
                double average = evaluationService.getAverageRating(currentFormationId);
                int count = evaluationService.getEvaluationCount(currentFormationId);

                Platform.runLater(() -> {
                    if (count == 0) {
                        lblAverageRating.setText("⭐ Pas encore d'évaluations");
                    } else {
                        String stars = getStarRepresentation(average);
                        lblAverageRating.setText(String.format("%s %.1f/5 (%d avis)", stars, average, count));
                    }
                });
            } catch (SQLException e) {
                System.err.println("Erreur lors du chargement de la note moyenne: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Charger les évaluations de la formation
     */
    private void loadEvaluations() {
        new Thread(() -> {
            try {
                List<Evaluation> evaluations = evaluationService.getEvaluationsByFormation(currentFormationId);

                Platform.runLater(() -> {
                    evaluationsContainer.getChildren().clear();

                    if (evaluations.isEmpty()) {
                        Label noEvaluations = new Label("Pas encore d'évaluations pour cette formation.");
                        noEvaluations.setStyle("-fx-text-fill: #999; -fx-font-style: italic;");
                        evaluationsContainer.getChildren().add(noEvaluations);
                        return;
                    }

                    // Afficher les évaluations les plus récentes en premier
                    for (Evaluation eval : evaluations) {
                        evaluationsContainer.getChildren().add(createEvaluationCard(eval));
                    }
                });
            } catch (SQLException e) {
                System.err.println("Erreur lors du chargement des évaluations: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Créer une carte pour afficher une évaluation
     */
    private VBox createEvaluationCard(Evaluation eval) {
        VBox card = new VBox();
        card.setStyle("-fx-border-color: -color-border-muted; -fx-border-width: 1; -fx-border-radius: 5; " +
                "-fx-padding: 12; -fx-background-color: -color-bg-subtle;");
        card.setSpacing(8.0);

        // En-tête avec nom et note
        HBox headerBox = new HBox();
        headerBox.setSpacing(10.0);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(eval.getNom_employe());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        Label starsLabel = new Label(eval.getStarRepresentation());
        starsLabel.setStyle("-fx-font-size: 14px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label dateLabel = new Label(eval.getDate_evaluation()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        dateLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");

        headerBox.getChildren().addAll(nameLabel, starsLabel, spacer, dateLabel);

        // Commentaire
        Label commentLabel = new Label(eval.getCommentaire());
        commentLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #333;");
        commentLabel.setWrapText(true);

        card.getChildren().addAll(headerBox, commentLabel);
        return card;
    }

    /**
     * Charger l'évaluation de l'utilisateur connecté (s'il existe)
     */
    private void loadUserEvaluation() {
        employe emp = session.getEmploye();
        if (emp == null) return;

        new Thread(() -> {
            try {
                Evaluation userEval = evaluationService.getEvaluationByFormationAndEmploye(
                        currentFormationId,
                        emp.getId_employé()
                );

                Platform.runLater(() -> {
                    if (userEval != null) {
                        // Pré-remplir le formulaire avec l'évaluation existante
                        spinnerNote.getValueFactory().setValue(userEval.getNote());
                        txtCommentaire.setText(userEval.getCommentaire());
                        btnSubmitEvaluation.setText("🔄 Mettre à jour l'évaluation");
                        updateStarDisplay(userEval.getNote());
                    } else {
                        btnSubmitEvaluation.setText("📤 Soumettre l'évaluation");
                    }
                });
            } catch (SQLException e) {
                System.err.println("Erreur lors du chargement de l'évaluation utilisateur: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Soumettre ou mettre à jour l'évaluation
     */
    @FXML
    private void handleSubmitEvaluation() {
        employe emp = session.getEmploye();
        if (emp == null) {
            showAlert(Alert.AlertType.WARNING, "Non connecté",
                    "Veuillez vous connecter en tant qu'employé pour évaluer cette formation.");
            return;
        }

        // Vérifier que l'inscription est acceptée
        try {
            inscription_formation inscription = inscriptionService.getInscriptionByFormationAndEmploye(
                    currentFormationId,
                    emp.getId_employé()
            );

            if (inscription == null) {
                showAlert(Alert.AlertType.ERROR, "Inscription requise",
                        "Vous devez d'abord vous inscrire à cette formation.");
                return;
            }

            if (inscription.getStatut() != StatutInscription.ACCEPTEE) {
                showAlert(Alert.AlertType.ERROR, "Inscription non acceptée",
                        "Votre inscription doit être acceptée par le RH avant de pouvoir évaluer cette formation.\n" +
                        "Statut actuel: " + getStatutText(inscription.getStatut()));
                return;
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de vérifier votre inscription: " + e.getMessage());
            return;
        }

        String commentaire = txtCommentaire.getText();
        if (commentaire == null || commentaire.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ requis",
                    "Veuillez écrire un commentaire pour votre évaluation.");
            txtCommentaire.requestFocus();
            return;
        }

        if (commentaire.trim().length() < 5) {
            showAlert(Alert.AlertType.WARNING, "Commentaire trop court",
                    "Veuillez fournir un commentaire plus détaillé (au moins 5 caractères).");
            txtCommentaire.requestFocus();
            return;
        }

        int note = spinnerNote.getValue();

        new Thread(() -> {
            try {
                // Vérifier si l'évaluation existe déjà
                Evaluation existingEval = evaluationService.getEvaluationByFormationAndEmploye(
                        currentFormationId,
                        emp.getId_employé()
                );

                if (existingEval != null) {
                    // Mettre à jour
                    existingEval.setNote(note);
                    existingEval.setCommentaire(commentaire.trim());
                    existingEval.setDate_evaluation(java.time.LocalDateTime.now());
                    evaluationService.modifier(existingEval);
                } else {
                    // Créer nouvelle évaluation
                    Evaluation newEval = new Evaluation(
                            currentFormationId,
                            emp.getId_employé(),
                            note,
                            commentaire.trim()
                    );
                    evaluationService.ajouter(newEval);
                }

                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.INFORMATION, "✅ Succès",
                            "Votre évaluation a été " + (existingEval != null ? "mise à jour" : "enregistrée") + " avec succès !");

                    // Rafraîchir les données
                    loadEvaluations();
                    loadAverageRating();

                    if (onEvaluationUpdated != null) {
                        onEvaluationUpdated.run();
                    }
                });
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Erreur",
                            "Impossible d'enregistrer votre évaluation: " + e.getMessage());
                });
                System.err.println("Erreur lors de l'enregistrement de l'évaluation: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Effacer le formulaire d'évaluation
     */
    @FXML
    private void handleClearEvaluation() {
        spinnerNote.getValueFactory().setValue(5);
        txtCommentaire.clear();
        updateStarDisplay(5);
    }

    /**
     * Obtenir une représentation visuelle de la note
     */
    private String getStarRepresentation(double note) {
        int roundedNote = (int) Math.round(note);
        return "⭐".repeat(roundedNote);
    }

    /**
     * Afficher une alerte
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Obtenir le texte du statut d'inscription en français
     */
    private String getStatutText(StatutInscription statut) {
        switch (statut) {
            case EN_ATTENTE: return "En attente de validation";
            case ACCEPTEE: return "Inscription acceptée";
            case REFUSEE: return "Inscription refusée";
            default: return statut.name();
        }
    }
}

