package controller.offres;

import entities.offres.Candidat;
import entities.offres.Offre;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import service.CandidatCRUD;
import service.OffreCRUD;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RecrutementStatController {

    // Nombre de candidatures par offre
    @FXML private BarChart<String, Number> chartCandidatParOffre;

    // Répartition par état
    @FXML private PieChart chartEtatCandidatures;

    // Score moyen par offre
    @FXML private BarChart<String, Number> chartScoreMoyenOffre;

    private final OffreCRUD offreService = new OffreCRUD();
    private final CandidatCRUD candidatService = new CandidatCRUD();

    @FXML
    public void initialize() {
        refreshStats();
    }

    public void refreshStats() {
        try {
            List<Offre> offres = offreService.afficher();
            List<Candidat> candidats = candidatService.afficher();

            // Nombre de candidatures par offre
            Map<Integer, Long> candCountMap = candidats.stream()
                    .collect(Collectors.groupingBy(Candidat::getIdOffre, Collectors.counting()));

            XYChart.Series<String, Number> seriesCount = new XYChart.Series<>();
            seriesCount.setName("Candidatures");

            for (Offre o : offres) {
                long count = candCountMap.getOrDefault(o.getId(), 0L);
                seriesCount.getData().add(new XYChart.Data<>(o.getTitrePoste(), count));
            }
            chartCandidatParOffre.getData().setAll(seriesCount);

            // Répartition par état
            Map<String, Long> statusMap = candidats.stream()
                    .collect(Collectors.groupingBy(Candidat::getEtat, Collectors.counting()));

            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            statusMap.forEach((etat, count) -> pieData.add(new PieChart.Data(etat, count)));
            chartEtatCandidatures.setData(pieData);

            // Score moyen par offre
            Map<Integer, Double> avgScoreMap = candidats.stream()
                    .filter(c -> c.getScore() > 0) // Ignore 0/null scores
                    .collect(Collectors.groupingBy(
                            Candidat::getIdOffre,
                            Collectors.averagingDouble(Candidat::getScore)
                    ));

            XYChart.Series<String, Number> seriesScore = new XYChart.Series<>();
            seriesScore.setName("Moyenne Score IA");

            // Affiche seulement les offres qui ont des scores moyens calculés
            for (Offre o : offres) {
                if (avgScoreMap.containsKey(o.getId())) {
                    double avg = avgScoreMap.get(o.getId());
                    seriesScore.getData().add(new XYChart.Data<>(o.getTitrePoste(), avg));
                }
            }
            chartScoreMoyenOffre.getData().setAll(seriesScore);

        } catch (SQLException e) {
            System.err.println("Erreur SQL lors du rafraîchissement des stats: " + e.getMessage());
        }
    }
}