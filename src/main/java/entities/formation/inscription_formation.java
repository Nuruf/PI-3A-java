package entities.formation;

import utils.RaisonAnalyzer;

public class inscription_formation {

    private int id_inscription;
    private int id_formation;
    private int id_user;
    private StatutInscription statut;
    private String raison;

    public inscription_formation() {
    }

    public inscription_formation(int id_formation, int id_user, StatutInscription statut, String raison) {
        this.id_formation = id_formation;
        this.id_user = id_user;
        this.statut = statut;
        this.raison = raison;
    }

    public inscription_formation(int id_inscription, int id_formation, int id_user, StatutInscription statut, String raison) {
        this.id_inscription = id_inscription;
        this.id_formation = id_formation;
        this.id_user = id_user;
        this.statut = statut;
        this.raison = raison;
    }

    // ===== Getters & Setters =====

    public int getId_inscription() {
        return id_inscription;
    }

    public void setId_inscription(int id_inscription) {
        this.id_inscription = id_inscription;
    }

    public int getId_formation() {
        return id_formation;
    }

    public void setId_formation(int id_formation) {
        this.id_formation = id_formation;
    }

    public int getId_user() {
        return id_user;
    }

    public void setId_user(int id_user) {
        this.id_user = id_user;
    }

    public StatutInscription getStatut() {
        return statut;
    }

    public void setStatut(StatutInscription statut) {
        this.statut = statut;
    }

    public String getRaison() {
        return raison;
    }

    public void setRaison(String raison) {
        this.raison = raison;
    }

    // ===== 🤖 Méthodes IA =====

    /**
     * Obtenir l'analyse IA de la raison
     */
    public RaisonAnalyzer.AnalysisResult getAiAnalysis() {
        if (raison == null || raison.trim().isEmpty()) {
            return null;
        }
        return RaisonAnalyzer.analyzeRaison(raison);
    }

    /**
     * Obtenir le score de pertinence IA
     */
    public int getRelevanceScore() {
        RaisonAnalyzer.AnalysisResult analysis = getAiAnalysis();
        return analysis != null ? analysis.getRelevanceScore() : 0;
    }

    /**
     * Obtenir le feedback IA
     */
    public String getAiFeedback() {
        RaisonAnalyzer.AnalysisResult analysis = getAiAnalysis();
        return analysis != null ? analysis.getFeedback() : "Aucune analyse";
    }

    /**
     * Obtenir la catégorie détectée par l'IA
     */
    public String getAiCategory() {
        RaisonAnalyzer.AnalysisResult analysis = getAiAnalysis();
        return analysis != null ? analysis.getCategoryDisplayName() : "Non analysé";
    }
}
