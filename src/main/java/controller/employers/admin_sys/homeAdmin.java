package controller.employers.admin_sys;

import entities.employers.entreprise;
import entities.employers.statut;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import service.employers.entrepriseCRUD;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class homeAdmin implements Initializable {

    @FXML private Label lblBienvenue;
    @FXML private Label lblDate;
    @FXML private ComboBox<String> comboMois;

    @FXML private Label lblTotal;
    @FXML private Label lblTotalSub;
    @FXML private Label lblEnAttente;
    @FXML private Label lblEnAttentePct;
    @FXML private Label lblAcceptees;
    @FXML private Label lblAccepteesPct;
    @FXML private Label lblRefusees;
    @FXML private Label lblRefuseesPct;
    @FXML private Label lblTauxAcceptation;
    @FXML private Label lblTauxSub;

    @FXML private BarChart<String, Number> barChart;
    @FXML private CategoryAxis barXAxis;
    @FXML private NumberAxis barYAxis;
    @FXML private Label lblBarTitle;

    @FXML private LineChart<String, Number> lineChart;
    @FXML private CategoryAxis lineXAxis;
    @FXML private NumberAxis lineYAxis;
    @FXML private Label lblLineTitle;

    @FXML private PieChart pieChartPays;
    @FXML private Label lblPaysTitle;
    @FXML private VBox legendePays;

    private entrepriseCRUD entrepriseCrud;
    private List<entreprise> toutesEntreprises;
    private List<entreprise> entreprisesFiltrees;
    private YearMonth moisSelectionne;

    private static final String[] MOIS_FR = {"Janvier", "Février", "Mars", "Avril", "Mai", "Juin", "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};

    private static final String[] PAYS_COLORS = {"#4A5DEF", "#22c55e", "#f59e0b"};

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            entrepriseCrud = new entrepriseCRUD();
            afficherDate();
            setupComboMois();
            chargerDonnees();
        } catch (SQLException e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }

    private void afficherDate() {
        LocalDate today = LocalDate.now();
        String[] jours = {"Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"};
        lblDate.setText(jours[today.getDayOfWeek().getValue() - 1] + " " + today.getDayOfMonth() + " " + MOIS_FR[today.getMonthValue() - 1].toLowerCase() + " " + today.getYear());
    }

    private void setupComboMois() {
        ObservableList<String> items = FXCollections.observableArrayList();
        items.add("Tous les mois");
        LocalDate now = LocalDate.now();
        for (int i = 0; i < 12; i++) {
            YearMonth ym = YearMonth.from(now.minusMonths(i));
            items.add(MOIS_FR[ym.getMonthValue() - 1] + " " + ym.getYear());
        }
        comboMois.setItems(items);
        comboMois.setValue("Tous les mois");
        comboMois.setOnAction(e -> onMoisChange());
    }

    private void onMoisChange() {
        String sel = comboMois.getValue();
        moisSelectionne = "Tous les mois".equals(sel) ? null : parseMois(sel);
        filtrerEtRafraichir();
    }

    private YearMonth parseMois(String s) {
        try {
            String[] parts = s.split(" ");
            int annee = Integer.parseInt(parts[1]);
            for (int i = 0; i < MOIS_FR.length; i++) {
                if (MOIS_FR[i].equalsIgnoreCase(parts[0]))
                    return YearMonth.of(annee, i + 1);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void chargerDonnees() {
        try {
            toutesEntreprises = entrepriseCrud.afficher();
            filtrerEtRafraichir();
        } catch (SQLException e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }

    private void filtrerEtRafraichir() {
        if (toutesEntreprises == null) return;

        if (moisSelectionne == null) {
            entreprisesFiltrees = new ArrayList<>(toutesEntreprises);
        } else {
            entreprisesFiltrees = toutesEntreprises.stream()
                    .filter(e -> e.getDate_demande() != null
                            && YearMonth.from(e.getDate_demande())
                            .equals(moisSelectionne))
                    .collect(Collectors.toList());
        }

        mettreAJourKPIs();
        mettreAJourBarChart();
        mettreAJourLineChart();
        mettreAJourPieChartPays();
        mettreAJourTitres();
    }
    private void mettreAJourKPIs() {
        long total = entreprisesFiltrees.size();
        long enAttente = compter(statut.enattende);
        long acceptees = compter(statut.acceptee);
        long refusees = compter(statut.refusee);

        lblTotal.setText(String.valueOf(total));
        lblEnAttente.setText(String.valueOf(enAttente));
        lblAcceptees.setText(String.valueOf(acceptees));
        lblRefusees.setText(String.valueOf(refusees));

        if (total > 0) {
            lblEnAttentePct.setText(pct(enAttente, total) + " du total");
            lblAccepteesPct.setText(pct(acceptees, total) + " du total");
            lblRefuseesPct.setText(pct(refusees, total) + " du total");

            long traitees = acceptees + refusees;
            if (traitees > 0) {
                lblTauxAcceptation.setText(pct(acceptees, traitees));
                lblTauxSub.setText(acceptees + "/" + traitees + " traitées");
            } else {
                lblTauxAcceptation.setText("—");
                lblTauxSub.setText("Aucune traitée");
            }
        } else {
            lblEnAttentePct.setText("");
            lblAccepteesPct.setText("");
            lblRefuseesPct.setText("");
            lblTauxAcceptation.setText("—");
            lblTauxSub.setText("");
        }

        if (enAttente > 0) {
            lblTotalSub.setText(enAttente + " à traiter");
            lblTotalSub.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-warning-fg;");
        } else {
            lblTotalSub.setText("Tout traité");
            lblTotalSub.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-success-fg;");
        }
    }

    private long compter(statut s) {
        return entreprisesFiltrees.stream().filter(e -> e.getStatut() == s).count();
    }

    private String pct(long v, long t) {
        return String.format("%.0f%%", (double) v / t * 100);
    }
    private void mettreAJourBarChart() {
        barChart.getData().clear();

        long enAttente = compter(statut.enattende);
        long acceptees = compter(statut.acceptee);
        long refusees = compter(statut.refusee);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        XYChart.Data<String, Number> barAtt = new XYChart.Data<>("En attente", enAttente);
        XYChart.Data<String, Number> barAcc = new XYChart.Data<>("Acceptées", acceptees);
        XYChart.Data<String, Number> barRef = new XYChart.Data<>("Refusées", refusees);

        series.getData().addAll(barAtt, barAcc, barRef);
        barChart.getData().add(series);

        barChart.applyCss();
        barChart.layout();

        styleBarre(barAtt, "#f59e0b", enAttente);
        styleBarre(barAcc, "#22c55e", acceptees);
        styleBarre(barRef, "#ef4444", refusees);
    }

    private void styleBarre(XYChart.Data<String, Number> data,
                            String color, long val) {
        if (data.getNode() == null) return;
        data.getNode().setStyle("-fx-bar-fill: " + color + ";");

        Tooltip tip = new Tooltip(data.getXValue() + " : " + val);
        Tooltip.install(data.getNode(), tip);
    }
    private void mettreAJourLineChart() {
        lineChart.getData().clear();

        List<entreprise> acceptees = entreprisesFiltrees.stream().filter(e -> e.getStatut() == statut.acceptee).filter(e -> e.getDate_demande() != null).collect(Collectors.toList());

        LocalDate debut, fin;
        if (moisSelectionne != null) {
            debut = moisSelectionne.atDay(1);
            fin = moisSelectionne.atEndOfMonth();
        } else {
            fin = LocalDate.now();
            debut = fin.minusDays(30);
        }

        Map<LocalDate, Long> parJour = new TreeMap<>();
        LocalDate cur = debut;
        while (!cur.isAfter(fin)) {
            parJour.put(cur, 0L);
            cur = cur.plusDays(1);
        }

        for (entreprise e : acceptees) {
            LocalDate d = e.getDate_demande();
            if (!d.isBefore(debut) && !d.isAfter(fin))
                parJour.merge(d, 1L, Long::sum);
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");

        for (Map.Entry<LocalDate, Long> entry : parJour.entrySet()) {
            XYChart.Data<String, Number> point = new XYChart.Data<>(
                    entry.getKey().format(fmt), entry.getValue());
            series.getData().add(point);
        }

        lineChart.getData().add(series);
        if (series.getNode() != null)
            series.getNode().setStyle("-fx-stroke: #4A5DEF; -fx-stroke-width: 2;");

        for (XYChart.Data<String, Number> d : series.getData()) {
            if (d.getNode() != null) {
                d.getNode().setStyle("-fx-background-color: #4A5DEF, white;-fx-background-radius: 5;-fx-background-insets: 0, 2;-fx-padding: 3;");

                Tooltip tip = new Tooltip(d.getXValue() + " : " + d.getYValue().intValue());
                Tooltip.install(d.getNode(), tip);
            }
        }
    }
    private void mettreAJourPieChartPays() {
        pieChartPays.getData().clear();
        legendePays.getChildren().clear();

        Map<String, Long> parPays = entreprisesFiltrees.stream().filter(e -> e.getPays() != null ).collect(Collectors.groupingBy(e -> capitaliser(e.getPays().trim()), Collectors.counting()));

        if (parPays.isEmpty()) {
            Label vide = new Label("Aucune donnée");
            vide.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 13px;");
            legendePays.getChildren().add(vide);
            return;
        }

        List<Map.Entry<String, Long>> sorted = parPays.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toList());

        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        for (Map.Entry<String, Long> entry : sorted) {
            data.add(new PieChart.Data(
                    entry.getKey() + " (" + entry.getValue() + ")",
                    entry.getValue()));
        }
        pieChartPays.setData(data);

        pieChartPays.applyCss();
        pieChartPays.layout();

        long total = entreprisesFiltrees.size();

        for (int i = 0; i < pieChartPays.getData().size(); i++) {
            PieChart.Data d = pieChartPays.getData().get(i);
            String color = PAYS_COLORS[i % PAYS_COLORS.length];

            if (d.getNode() != null) {
                d.getNode().setStyle("-fx-pie-color: " + color + ";");

                double p = d.getPieValue() / total * 100;
                Tooltip tip = new Tooltip(
                        String.format("%s — %.0f%%", d.getName(), p));
                Tooltip.install(d.getNode(), tip);
            }

            // Légende
            legendePays.getChildren().add(
                    creerLegendItem(sorted.get(i).getKey(),
                            sorted.get(i).getValue(), total, color));
        }
    }

    private HBox creerLegendItem(String pays, long count, long total, String color) {
        HBox item = new HBox(8);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(4, 10, 4, 10));
        item.setStyle("-fx-background-color: -color-bg-default;-fx-background-radius: 5;");

        Circle circle = new Circle(5);
        circle.setStyle("-fx-fill: " + color + ";");

        Label nom = new Label(pays);
        nom.setStyle("-fx-font-size: 12px; -fx-font-weight: 600;");
        HBox.setHgrow(nom, Priority.ALWAYS);

        Label cnt = new Label(String.valueOf(count));
        cnt.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        double p = (double) count / total * 100;
        Label pctLabel = new Label(String.format("%.0f%%", p));
        pctLabel.setPrefWidth(40);
        pctLabel.setAlignment(Pos.CENTER_RIGHT);
        pctLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-fg-muted;");

        item.getChildren().addAll(circle, nom, cnt, pctLabel);
        return item;
    }

    private String capitaliser(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
    private void mettreAJourTitres() {
        if (moisSelectionne != null) {
            String m = MOIS_FR[moisSelectionne.getMonthValue() - 1] + " " + moisSelectionne.getYear();
            lblBarTitle.setText("Demandes par statut — " + m);
            lblLineTitle.setText("Acceptées par jour — " + m);
            lblPaysTitle.setText("Entreprises par pays — " + m);
        } else {
            lblBarTitle.setText("Demandes par statut");
            lblLineTitle.setText("Acceptées par jour — 30 derniers jours");
            lblPaysTitle.setText("Entreprises par pays");
        }
    }
}