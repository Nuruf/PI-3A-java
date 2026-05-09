package utils;

import java.util.*;

/**
 * 🤖 ASSISTANT INTELLIGENT DE TEXTE
 * - Correction orthographique
 * - Génération de paragraphes professionnels
 * - Suggestions intelligentes
 *
 * @author Votre Nom
 * @version 1.0 - Assistant IA
 */
public class TextAssistant {

    // 📖 Dictionnaire de corrections orthographiques courantes
    private static final Map<String, String> SPELLING_CORRECTIONS = new HashMap<>();

    // 💼 Templates de paragraphes professionnels par intention
    private static final Map<String, List<String>> PROFESSIONAL_TEMPLATES = new HashMap<>();

    static {
        // ===== CORRECTIONS ORTHOGRAPHIQUES =====

        // Anglais courant
        SPELLING_CORRECTIONS.put("intrested", "interested");
        SPELLING_CORRECTIONS.put("communciate", "communicate");
        SPELLING_CORRECTIONS.put("developement", "development");
        SPELLING_CORRECTIONS.put("technologie", "technology");
        SPELLING_CORRECTIONS.put("skilll", "skill");
        SPELLING_CORRECTIONS.put("skils", "skills");
        SPELLING_CORRECTIONS.put("experiance", "experience");
        SPELLING_CORRECTIONS.put("profesional", "professional");
        SPELLING_CORRECTIONS.put("carreer", "career");
        SPELLING_CORRECTIONS.put("recieve", "receive");
        SPELLING_CORRECTIONS.put("acheive", "achieve");
        SPELLING_CORRECTIONS.put("beleive", "believe");
        SPELLING_CORRECTIONS.put("knowlege", "knowledge");
        SPELLING_CORRECTIONS.put("certfication", "certification");

        // Français courant
        SPELLING_CORRECTIONS.put("compétance", "compétence");
        SPELLING_CORRECTIONS.put("dévelopement", "développement");
        SPELLING_CORRECTIONS.put("formations", "formation");
        SPELLING_CORRECTIONS.put("aprendre", "apprendre");
        SPELLING_CORRECTIONS.put("aquérir", "acquérir");
        SPELLING_CORRECTIONS.put("nécéssaire", "nécessaire");
        SPELLING_CORRECTIONS.put("objectif", "objectif");
        SPELLING_CORRECTIONS.put("projets", "projet");

        // ===== TEMPLATES PROFESSIONNELS AMÉLIORÉS =====

        // Communication (templates plus riches et variés)
        PROFESSIONAL_TEMPLATES.put("communication", Arrays.asList(
            "Je souhaite développer mes compétences en communication afin d'améliorer mes interactions professionnelles et de mieux collaborer avec les équipes. Cette formation me permettra d'acquérir des techniques de communication efficaces essentielles pour mon rôle actuel et mes objectifs de carrière.",
            "I want to improve my communication skills to enhance professional interactions and better collaborate with teams. This training will help me develop effective communication techniques that are essential for my current role and career goals.",
            "Dans le cadre de mon développement professionnel, je souhaite renforcer mes capacités de communication interpersonnelle. Cette formation représente une opportunité unique d'apprendre des méthodes éprouvées pour communiquer plus efficacement avec mes collègues et clients.",
            "As part of my professional development, I want to strengthen my interpersonal communication abilities. This training represents a unique opportunity to learn proven methods for communicating more effectively with colleagues and clients."
        ));

        // Compétences techniques (plus détaillé et spécifique)
        PROFESSIONAL_TEMPLATES.put("technical", Arrays.asList(
            "Je souhaite acquérir des compétences techniques avancées pour contribuer efficacement aux projets innovants de l'entreprise. Cette formation me permettra de maîtriser les outils et technologies nécessaires à l'évolution de mon poste et d'apporter une réelle valeur ajoutée à l'équipe.",
            "I want to acquire advanced technical skills to effectively contribute to the company's innovative projects. This training will enable me to master the tools and technologies necessary for my job evolution and bring real added value to the team.",
            "Afin de rester compétitif dans mon domaine d'expertise, je souhaite développer mes compétences techniques à travers cette formation. Les connaissances acquises me permettront d'améliorer mes performances et de participer activement aux projets stratégiques de l'entreprise.",
            "To remain competitive in my field of expertise, I want to develop my technical skills through this training. The knowledge acquired will allow me to improve my performance and actively participate in the company's strategic projects."
        ));

        // Évolution de carrière (avec objectifs précis)
        PROFESSIONAL_TEMPLATES.put("career", Arrays.asList(
            "Je souhaite suivre cette formation dans le cadre de mon projet d'évolution professionnelle vers un poste à plus hautes responsabilités. Cette formation constitue une étape clé dans mon parcours de développement et me permettra d'acquérir les compétences managériales nécessaires pour atteindre mes objectifs de carrière.",
            "I want to take this training as part of my professional development plan towards a position with greater responsibilities. This training is a key step in my development path and will help me acquire the managerial skills necessary to achieve my career goals.",
            "Dans une perspective d'évolution de carrière, cette formation s'inscrit parfaitement dans mon plan de développement professionnel. Elle me permettra d'acquérir les compétences essentielles pour progresser vers des fonctions de leadership et assumer de nouvelles responsabilités au sein de l'entreprise.",
            "From a career development perspective, this training fits perfectly into my professional development plan. It will allow me to acquire the essential skills to progress towards leadership positions and take on new responsibilities within the company."
        ));

        // Certification (avec valeur ajoutée)
        PROFESSIONAL_TEMPLATES.put("certification", Arrays.asList(
            "Je souhaite obtenir la certification proposée par cette formation afin de valider officiellement mes compétences et d'améliorer mon employabilité. Cette certification, reconnue internationalement dans mon secteur d'activité, constituera un atout majeur pour mon parcours professionnel et renforcera ma crédibilité auprès des clients et partenaires.",
            "I want to obtain the certification offered by this training to officially validate my skills and improve my employability. This internationally recognized certification in my field will be a major asset for my career path and strengthen my credibility with clients and partners.",
            "L'obtention de cette certification représente un objectif professionnel important pour moi. Elle me permettra non seulement de valider formellement mes compétences, mais aussi de démontrer mon engagement envers l'excellence professionnelle et le développement continu de mon expertise.",
            "Obtaining this certification represents an important professional goal for me. It will not only allow me to formally validate my skills, but also demonstrate my commitment to professional excellence and the continuous development of my expertise."
        ));

        // Nouvelles technologies (focus sur l'innovation)
        PROFESSIONAL_TEMPLATES.put("technology", Arrays.asList(
            "Je souhaite me former aux nouvelles technologies pour rester à jour dans mon domaine et pouvoir contribuer aux projets de transformation digitale de l'entreprise. Cette formation me permettra de maîtriser les outils technologiques innovants qui deviennent essentiels pour notre secteur d'activité et nos futurs projets stratégiques.",
            "I want to train in new technologies to stay up-to-date in my field and contribute to the company's digital transformation projects. This training will enable me to master the innovative technological tools that are becoming essential for our industry and future strategic projects.",
            "Face à l'évolution rapide des technologies dans notre domaine, cette formation représente une opportunité essentielle pour développer mes compétences digitales. Elle me permettra d'adopter les meilleures pratiques et d'utiliser les outils les plus récents pour optimiser mon travail et participer activement à l'innovation au sein de l'entreprise.",
            "Given the rapid evolution of technologies in our field, this training represents an essential opportunity to develop my digital skills. It will allow me to adopt best practices and use the latest tools to optimize my work and actively participate in innovation within the company."
        ));

        // Projet spécifique (avec impact mesurable)
        PROFESSIONAL_TEMPLATES.put("project", Arrays.asList(
            "Je souhaite suivre cette formation car elle est directement liée à mon projet actuel et me permettra d'acquérir les compétences nécessaires pour le mener à bien avec succès. Les connaissances acquises auront un impact direct sur la qualité de mes livrables et contribueront significativement aux objectifs stratégiques de l'équipe.",
            "I want to take this training because it is directly related to my current project and will help me acquire the necessary skills to complete it successfully. The knowledge acquired will have a direct impact on the quality of my deliverables and significantly contribute to the team's strategic objectives.",
            "Dans le cadre de ma mission actuelle sur le projet X, cette formation est indispensable pour maîtriser les méthodes et outils requis. Elle me permettra d'améliorer mon efficacité opérationnelle et de garantir la réussite du projet en respectant les délais et les standards de qualité attendus.",
            "As part of my current assignment on project X, this training is essential to master the required methods and tools. It will allow me to improve my operational efficiency and ensure project success while meeting expected deadlines and quality standards."
        ));

        // Développement personnel (avec retour sur investissement)
        PROFESSIONAL_TEMPLATES.put("development", Arrays.asList(
            "Je souhaite développer mes compétences professionnelles à travers cette formation pour améliorer mes performances et contribuer davantage aux objectifs de l'équipe. Cette opportunité d'apprentissage me permettra d'enrichir mon expertise, d'adopter de nouvelles méthodologies et de renforcer ma valeur ajoutée au sein de l'organisation.",
            "I want to develop my professional skills through this training to improve my performance and contribute more to the team's objectives. This learning opportunity will allow me to enrich my expertise, adopt new methodologies and strengthen my added value within the organization.",
            "Cette formation représente une opportunité stratégique pour mon développement professionnel continu. Elle me permettra d'acquérir de nouvelles compétences directement applicables dans mon travail quotidien, d'améliorer ma productivité et de mieux répondre aux défis actuels et futurs de mon poste.",
            "This training represents a strategic opportunity for my ongoing professional development. It will allow me to acquire new skills directly applicable in my daily work, improve my productivity and better meet the current and future challenges of my position."
        ));
    }

    /**
     * 🔤 Corriger l'orthographe d'un texte
     */
    public static String correctSpelling(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        String correctedText = text;

        // Appliquer les corrections
        for (Map.Entry<String, String> correction : SPELLING_CORRECTIONS.entrySet()) {
            String wrong = correction.getKey();
            String correct = correction.getValue();

            // Correction sensible à la casse
            correctedText = correctedText.replaceAll("(?i)\\b" + wrong + "\\b", correct);
        }

        return correctedText;
    }

    /**
     * 🔍 Détecter les fautes d'orthographe dans un texte
     */
    public static List<SpellingError> detectSpellingErrors(String text) {
        List<SpellingError> errors = new ArrayList<>();

        if (text == null || text.trim().isEmpty()) {
            return errors;
        }

        String lowerText = text.toLowerCase();

        for (Map.Entry<String, String> correction : SPELLING_CORRECTIONS.entrySet()) {
            String wrong = correction.getKey();
            String correct = correction.getValue();

            if (lowerText.contains(wrong)) {
                errors.add(new SpellingError(wrong, correct));
            }
        }

        return errors;
    }

    /**
     * ✨ Générer un paragraphe professionnel basé sur des mots-clés
     */
    public static String generateProfessionalParagraph(String keywords) {
        if (keywords == null || keywords.trim().isEmpty()) {
            return "";
        }

        String lowerKeywords = keywords.toLowerCase();

        // Détecter l'intention
        String intention = detectIntention(lowerKeywords);

        // Récupérer les templates
        List<String> templates = PROFESSIONAL_TEMPLATES.get(intention);
        if (templates == null || templates.isEmpty()) {
            return generateGenericParagraph(keywords);
        }

        // Détecter la langue (français ou anglais)
        boolean isFrench = lowerKeywords.contains("je ") ||
                          lowerKeywords.contains("compétence") ||
                          lowerKeywords.contains("projet") ||
                          lowerKeywords.contains("formation");

        // Choisir un template approprié
        for (String template : templates) {
            if (isFrench && template.startsWith("Je")) {
                return template;
            } else if (!isFrench && template.startsWith("I ")) {
                return template;
            }
        }

        // Par défaut, retourner le premier template
        return templates.get(0);
    }

    /**
     * 🎯 Détecter l'intention de l'utilisateur
     */
    private static String detectIntention(String keywords) {
        // Communication
        if (keywords.contains("communication") || keywords.contains("communi") ||
            keywords.contains("team") || keywords.contains("équipe")) {
            return "communication";
        }

        // Compétences techniques
        if (keywords.contains("technical") || keywords.contains("technique") ||
            keywords.contains("skill") || keywords.contains("compétence")) {
            return "technical";
        }

        // Évolution de carrière
        if (keywords.contains("career") || keywords.contains("carrière") ||
            keywords.contains("promotion") || keywords.contains("évolution")) {
            return "career";
        }

        // Certification
        if (keywords.contains("certification") || keywords.contains("certifi") ||
            keywords.contains("diplôme") || keywords.contains("diploma")) {
            return "certification";
        }

        // Technologies
        if (keywords.contains("technology") || keywords.contains("technologie") ||
            keywords.contains("digital") || keywords.contains("numérique") ||
            keywords.contains("innovation")) {
            return "technology";
        }

        // Projet
        if (keywords.contains("project") || keywords.contains("projet") ||
            keywords.contains("mission")) {
            return "project";
        }

        // Par défaut : développement
        return "development";
    }

    /**
     * 📝 Générer un paragraphe générique
     */
    private static String generateGenericParagraph(String keywords) {
        return "Je souhaite suivre cette formation pour développer mes compétences en " +
               keywords + " et ainsi contribuer plus efficacement aux objectifs de l'entreprise.";
    }

    /**
     * 💡 Obtenir des suggestions d'amélioration
     */
    public static List<String> getSuggestions(String text) {
        List<String> suggestions = new ArrayList<>();

        if (text == null || text.trim().isEmpty()) {
            suggestions.add("💡 Commencez par expliquer votre objectif principal");
            suggestions.add("💡 Mentionnez les compétences que vous souhaitez acquérir");
            return suggestions;
        }

        int length = text.length();

        // Suggestions selon la longueur
        if (length < 30) {
            suggestions.add("💡 Développez davantage votre raison (au moins 30 caractères)");
            suggestions.add("💡 Ajoutez des détails sur vos motivations");
        } else if (length < 60) {
            suggestions.add("💡 Expliquez comment cette formation s'inscrit dans votre projet professionnel");
            suggestions.add("💡 Mentionnez les bénéfices pour l'entreprise");
        }

        // Suggestions selon le contenu
        if (!text.toLowerCase().contains("afin") && !text.toLowerCase().contains("pour") &&
            !text.toLowerCase().contains("to") && !text.toLowerCase().contains("in order")) {
            suggestions.add("💡 Ajoutez 'afin de' ou 'pour' pour expliquer votre objectif");
        }

        if (!text.toLowerCase().contains("compétence") && !text.toLowerCase().contains("skill")) {
            suggestions.add("💡 Mentionnez les compétences que vous souhaitez acquérir");
        }

        if (suggestions.isEmpty()) {
            suggestions.add("✅ Votre raison est bien structurée !");
        }

        return suggestions;
    }

    // ===== CLASSES INTERNES =====

    /**
     * Représente une erreur d'orthographe détectée
     */
    public static class SpellingError {
        private final String wrongWord;
        private final String correction;

        public SpellingError(String wrongWord, String correction) {
            this.wrongWord = wrongWord;
            this.correction = correction;
        }

        public String getWrongWord() { return wrongWord; }
        public String getCorrection() { return correction; }

        @Override
        public String toString() {
            return wrongWord + " → " + correction;
        }
    }
}

