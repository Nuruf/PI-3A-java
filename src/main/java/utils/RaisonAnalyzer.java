package utils;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 🤖 SYSTÈME D'INTELLIGENCE ARTIFICIELLE
 * Analyse automatique des raisons d'inscription avec scoring de pertinence
 *
 * Fonctionnalités IA :
 * - Analyse sémantique des raisons
 * - Calcul de score de pertinence (0-100%)
 * - Catégorisation automatique des motivations
 * - Détection de raisons génériques/suspectes
 *
 * @author Votre Nom
 * @version 2.0 - Édition IA
 */
public class RaisonAnalyzer {

    // 📚 Dictionnaire de mots-clés par catégorie (apprentissage supervisé simple)
    private static final Map<String, List<String>> KEYWORDS_BY_CATEGORY = new HashMap<>();

    static {
        // Développement de compétences (Français + Anglais)
        KEYWORDS_BY_CATEGORY.put("DÉVELOPPEMENT_COMPÉTENCES", Arrays.asList(
            // Français
            "compétence", "compétences", "apprendre", "améliorer", "développer",
            "progresser", "perfectionner", "maîtriser", "acquérir", "renforcer",
            "formation", "expertise", "pratique", "technique", "savoir-faire",
            // English
            "skill", "skills", "learn", "learning", "improve", "develop", "developing",
            "master", "mastering", "acquire", "strengthen", "training", "expertise",
            "practice", "technical", "knowledge", "proficiency", "competency"
        ));

        // Évolution de carrière (Français + Anglais)
        KEYWORDS_BY_CATEGORY.put("ÉVOLUTION_CARRIÈRE", Arrays.asList(
            // Français
            "carrière", "promotion", "évolution", "poste", "responsabilité",
            "avancement", "manager", "chef", "leadership", "objectif professionnel",
            "projet professionnel", "mobilité", "progression",
            // English
            "career", "promotion", "evolution", "position", "responsibility",
            "advancement", "management", "leader", "leadership", "professional goal",
            "career path", "mobility", "progression", "growth", "job"
        ));

        // Certification/Diplôme (Français + Anglais)
        KEYWORDS_BY_CATEGORY.put("CERTIFICATION", Arrays.asList(
            // Français
            "certification", "certifié", "diplôme", "attestation", "certificat",
            "accréditation", "qualification", "titre", "diplômé", "reconnu",
            // English
            "certification", "certified", "certificate", "diploma", "accreditation",
            "qualification", "degree", "credential", "licensed", "recognized"
        ));

        // Adaptation aux nouvelles technologies (Français + Anglais)
        KEYWORDS_BY_CATEGORY.put("NOUVELLES_TECHNOLOGIES", Arrays.asList(
            // Français
            "technologie", "nouveau", "nouvelle", "innovation", "digital",
            "numérique", "moderne", "récent", "actuel", "tendance", "ia",
            "intelligence artificielle", "cloud", "data", "cybersécurité",
            // English
            "technology", "new", "innovation", "digital", "modern", "recent",
            "current", "trend", "ai", "artificial intelligence", "cloud",
            "data", "cybersecurity", "tech", "emerging", "cutting-edge"
        ));

        // Reconversion/Changement (Français + Anglais)
        KEYWORDS_BY_CATEGORY.put("RECONVERSION", Arrays.asList(
            // Français
            "reconversion", "changer", "changement", "transition", "nouveau domaine",
            "nouvelle voie", "réorientation", "pivot", "switch",
            // English
            "career change", "transition", "change", "changing", "new field",
            "new path", "reorientation", "pivot", "switch", "switching"
        ));

        // Besoin métier/projet (Français + Anglais)
        KEYWORDS_BY_CATEGORY.put("BESOIN_MÉTIER", Arrays.asList(
            // Français
            "projet", "mission", "besoin", "nécessaire", "requis", "demandé",
            "travail", "tâche", "client", "entreprise", "équipe", "département",
            // English
            "project", "mission", "need", "necessary", "required", "requested",
            "work", "task", "client", "company", "team", "department",
            "business", "assignment", "job"
        ));

        // Intérêt personnel (Français + Anglais)
        KEYWORDS_BY_CATEGORY.put("INTÉRÊT_PERSONNEL", Arrays.asList(
            // Français
            "intéressé", "intéresse", "intérêt", "passion", "passionné",
            "curieux", "découvrir", "explorer", "aime", "envie",
            // English
            "interested", "interest", "passion", "passionate", "curious",
            "discover", "explore", "like", "love", "want", "desire"
        ));
    }

    // ⚠️ Mots/phrases suspects (raisons génériques/faibles) - Français + Anglais
    private static final List<String> SUSPICIOUS_PATTERNS = Arrays.asList(
        // Français
        "je veux", "j'aime", "c'est bien", "pourquoi pas", "ça m'intéresse",
        "je sais pas", "parce que", "on verra", "peut-être", "je pense",
        // English
        "i want", "i like", "it's good", "why not", "i'm interested",
        "i don't know", "because", "we'll see", "maybe", "i think"
    );

    // 🎯 Mots de qualité (indicateurs de raisons détaillées) - Français + Anglais
    private static final List<String> QUALITY_INDICATORS = Arrays.asList(
        // Français
        "afin de", "dans le but de", "pour pouvoir", "permettra", "essentiel",
        "indispensable", "nécessaire pour", "me permettra de", "objectif",
        "souhait", "ambition", "projet de", "dans le cadre de", "car", "étant donné",
        // English
        "in order to", "for the purpose of", "to be able to", "will allow", "essential",
        "indispensable", "necessary for", "will enable me to", "goal", "objective",
        "wish", "ambition", "as part of", "because", "given that", "since"
    );

    /**
     * 🤖 ANALYSE PRINCIPALE - Retourne un objet avec tous les résultats
     */
    public static AnalysisResult analyzeRaison(String raison) {
        if (raison == null || raison.trim().isEmpty()) {
            return new AnalysisResult(0, "INVALIDE", "Raison vide", new HashMap<>());
        }

        String raisonLower = raison.toLowerCase().trim();

        // Calculer le score de pertinence
        int score = calculateRelevanceScore(raisonLower);

        // Identifier la catégorie principale
        String category = identifyMainCategory(raisonLower);

        // Générer un feedback
        String feedback = generateFeedback(score, raisonLower);

        // Calculer les scores par catégorie
        Map<String, Integer> categoryScores = calculateCategoryScores(raisonLower);

        return new AnalysisResult(score, category, feedback, categoryScores);
    }

    /**
     * 📊 ALGORITHME DE SCORING (0-100%)
     */
    private static int calculateRelevanceScore(String raison) {
        int score = 0;

        // 1️⃣ Longueur (max 20 points)
        int length = raison.length();
        if (length >= 100) score += 20;
        else if (length >= 50) score += 15;
        else if (length >= 30) score += 10;
        else if (length >= 10) score += 5;

        // 2️⃣ Présence de mots-clés métier (max 30 points)
        int keywordMatches = 0;
        for (List<String> keywords : KEYWORDS_BY_CATEGORY.values()) {
            for (String keyword : keywords) {
                if (raison.contains(keyword)) {
                    keywordMatches++;
                }
            }
        }
        score += Math.min(keywordMatches * 3, 30);

        // 3️⃣ Présence d'indicateurs de qualité (max 25 points)
        int qualityMatches = 0;
        for (String indicator : QUALITY_INDICATORS) {
            if (raison.contains(indicator)) {
                qualityMatches++;
            }
        }
        score += Math.min(qualityMatches * 5, 25);

        // 4️⃣ Structure grammaticale (max 15 points)
        if (raison.contains(".") || raison.contains(",")) score += 5; // Phrases construites
        if (Character.isUpperCase(raison.charAt(0))) score += 5; // Majuscule au début
        if (raison.split("\\s+").length > 5) score += 5; // Au moins 5 mots

        // 5️⃣ Détection de contenu suspect (max -20 points)
        for (String suspicious : SUSPICIOUS_PATTERNS) {
            if (raison.contains(suspicious)) {
                score -= 5;
            }
        }

        // 6️⃣ Bonus pour raisons très détaillées (max 10 points)
        if (length > 150 && keywordMatches > 3) score += 10;

        // Limiter entre 0 et 100
        return Math.max(0, Math.min(100, score));
    }

    /**
     * 🏷️ CATÉGORISATION AUTOMATIQUE
     */
    private static String identifyMainCategory(String raison) {
        int maxScore = 0;
        String mainCategory = "AUTRE";

        for (Map.Entry<String, List<String>> entry : KEYWORDS_BY_CATEGORY.entrySet()) {
            int categoryScore = 0;
            for (String keyword : entry.getValue()) {
                if (raison.contains(keyword)) {
                    categoryScore++;
                }
            }

            if (categoryScore > maxScore) {
                maxScore = categoryScore;
                mainCategory = entry.getKey();
            }
        }

        return maxScore > 0 ? mainCategory : "AUTRE";
    }

    /**
     * 📝 GÉNÉRATION DE FEEDBACK INTELLIGENT
     */
    private static String generateFeedback(int score, String raison) {
        if (score >= 80) {
            return "✅ Excellente raison - Très pertinente et détaillée";
        } else if (score >= 60) {
            return "👍 Bonne raison - Motivation claire";
        } else if (score >= 40) {
            return "⚠️ Raison acceptable - Pourrait être plus détaillée";
        } else if (score >= 20) {
            return "⚠️ Raison faible - Manque de détails ou de motivation claire";
        } else {
            return "❌ Raison insuffisante - Trop courte ou trop générique";
        }
    }

    /**
     * 📊 SCORES PAR CATÉGORIE (pour graphiques)
     */
    private static Map<String, Integer> calculateCategoryScores(String raison) {
        Map<String, Integer> scores = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : KEYWORDS_BY_CATEGORY.entrySet()) {
            int categoryScore = 0;
            for (String keyword : entry.getValue()) {
                if (raison.contains(keyword)) {
                    categoryScore += 10;
                }
            }
            scores.put(entry.getKey(), Math.min(100, categoryScore));
        }

        return scores;
    }

    /**
     * 🎨 CLASSE DE RÉSULTAT D'ANALYSE
     */
    public static class AnalysisResult {
        private final int relevanceScore;        // Score 0-100
        private final String mainCategory;        // Catégorie principale
        private final String feedback;            // Feedback textuel
        private final Map<String, Integer> categoryScores; // Scores détaillés

        public AnalysisResult(int score, String category, String feedback, Map<String, Integer> categoryScores) {
            this.relevanceScore = score;
            this.mainCategory = category;
            this.feedback = feedback;
            this.categoryScores = categoryScores;
        }

        public int getRelevanceScore() { return relevanceScore; }
        public String getMainCategory() { return mainCategory; }
        public String getFeedback() { return feedback; }
        public Map<String, Integer> getCategoryScores() { return categoryScores; }

        public String getCategoryDisplayName() {
            switch (mainCategory) {
                case "DÉVELOPPEMENT_COMPÉTENCES": return "Développement de compétences";
                case "ÉVOLUTION_CARRIÈRE": return "Évolution de carrière";
                case "CERTIFICATION": return "Certification/Diplôme";
                case "NOUVELLES_TECHNOLOGIES": return "Nouvelles technologies";
                case "RECONVERSION": return "Reconversion";
                case "BESOIN_MÉTIER": return "Besoin métier";
                case "INTÉRÊT_PERSONNEL": return "Intérêt personnel";
                default: return "Autre";
            }
        }

        public String getScoreEmoji() {
            if (relevanceScore >= 80) return "🟢";
            if (relevanceScore >= 60) return "🟡";
            if (relevanceScore >= 40) return "🟠";
            return "🔴";
        }

        @Override
        public String toString() {
            return String.format("Score: %d%% %s | Catégorie: %s | %s",
                relevanceScore, getScoreEmoji(), getCategoryDisplayName(), feedback);
        }
    }

    /**
     * 🧪 MÉTHODE DE TEST
     */
    public static void main(String[] args) {
        // Tests
        String[] testRaisons = {
            "Je souhaite acquérir des compétences en développement web afin de pouvoir contribuer aux projets de digitalisation de l'entreprise.",
            "J'aime bien",
            "Cette formation me permettra d'obtenir la certification Java nécessaire pour mon évolution professionnelle vers un poste de tech lead.",
            "Pourquoi pas",
            "Dans le cadre de mon projet de reconversion vers le métier de data scientist, cette formation en machine learning est essentielle pour maîtriser les algorithmes prédictifs."
        };

        for (String raison : testRaisons) {
            System.out.println("\n📝 Raison: " + raison);
            AnalysisResult result = analyzeRaison(raison);
            System.out.println("   " + result);
        }
    }
}

