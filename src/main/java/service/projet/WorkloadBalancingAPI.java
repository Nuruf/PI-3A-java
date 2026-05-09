package service.projet;

import entities.projet.Tache;
import entities.projet.statut_t;
import entities.projet.priority;
import utils.MyDB;
import utils.employers.json;

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Smart Workload Balancing API
 * Analyzes employee workload and suggests the best available employee for new tasks.
 *
 * Scoring Algorithm:
 * - Each active task adds to workload score
 * - High priority tasks = 3 points
 * - Medium priority tasks = 2 points
 * - Low priority tasks = 1 point
 * - Tasks due within 3 days = +2 bonus points (urgent)
 * - Tasks due within 7 days = +1 bonus point
 *
 * Lower score = more available employee
 */
public class WorkloadBalancingAPI {

    private final Connection cnx = MyDB.getInstance().getConn();
    private final TacheCRUD tacheCRUD = new TacheCRUD();

    /**
     * Represents an employee's workload analysis
     */
    public static class EmployeeWorkload {
        private final int employeeId;
        private final String employeeName;
        private final int totalActiveTasks;
        private final int highPriorityTasks;
        private final int mediumPriorityTasks;
        private final int lowPriorityTasks;
        private final int urgentTasks; // Due within 3 days
        private final double workloadScore;
        private final String availabilityStatus; // "Disponible", "Occupé", "Surchargé"
        private double skillMatchScore; // 0.0 to 1.0 — how well employee skills match the task
        private List<String> matchedSkills; // Skills that matched the task

        public EmployeeWorkload(int employeeId, String employeeName, int totalActiveTasks,
                                int highPriorityTasks, int mediumPriorityTasks, int lowPriorityTasks,
                                int urgentTasks, double workloadScore) {
            this.employeeId = employeeId;
            this.employeeName = employeeName;
            this.totalActiveTasks = totalActiveTasks;
            this.highPriorityTasks = highPriorityTasks;
            this.mediumPriorityTasks = mediumPriorityTasks;
            this.lowPriorityTasks = lowPriorityTasks;
            this.urgentTasks = urgentTasks;
            this.workloadScore = workloadScore;
            this.availabilityStatus = calculateAvailabilityStatus(workloadScore, urgentTasks);
            this.skillMatchScore = 0.0;
            this.matchedSkills = new ArrayList<>();
        }

        private String calculateAvailabilityStatus(double score, int urgent) {
            if (urgent >= 3 || score >= 15) return "🔴 Surchargé";
            if (urgent >= 1 || score >= 8) return "🟡 Occupé";
            return "🟢 Disponible";
        }

        // Getters
        public int getEmployeeId() { return employeeId; }
        public String getEmployeeName() { return employeeName; }
        public int getTotalActiveTasks() { return totalActiveTasks; }
        public int getHighPriorityTasks() { return highPriorityTasks; }
        public int getMediumPriorityTasks() { return mediumPriorityTasks; }
        public int getLowPriorityTasks() { return lowPriorityTasks; }
        public int getUrgentTasks() { return urgentTasks; }
        public double getWorkloadScore() { return workloadScore; }
        public String getAvailabilityStatus() { return availabilityStatus; }
        public double getSkillMatchScore() { return skillMatchScore; }
        public void setSkillMatchScore(double skillMatchScore) { this.skillMatchScore = skillMatchScore; }
        public List<String> getMatchedSkills() { return matchedSkills; }
        public void setMatchedSkills(List<String> matchedSkills) { this.matchedSkills = matchedSkills; }

        @Override
        public String toString() {
            String base = employeeName + " " + availabilityStatus +
                    " (Score: " + String.format("%.1f", workloadScore) +
                    ", Tâches: " + totalActiveTasks + ")";
            if (skillMatchScore > 0) {
                base += " | 🎯 Compétences: " + String.format("%.0f%%", skillMatchScore * 100);
                if (!matchedSkills.isEmpty()) {
                    base += " (" + String.join(", ", matchedSkills) + ")";
                }
            }
            return base;
        }
    }

    /**
     * API Response containing workload analysis results
     */
    public static class WorkloadAnalysisResult {
        private final List<EmployeeWorkload> rankedEmployees;
        private final EmployeeWorkload suggestedEmployee;
        private final String suggestionReason;
        private final LocalDate analysisDate;

        public WorkloadAnalysisResult(List<EmployeeWorkload> rankedEmployees,
                                      EmployeeWorkload suggestedEmployee,
                                      String suggestionReason) {
            this.rankedEmployees = rankedEmployees;
            this.suggestedEmployee = suggestedEmployee;
            this.suggestionReason = suggestionReason;
            this.analysisDate = LocalDate.now();
        }

        public List<EmployeeWorkload> getRankedEmployees() { return rankedEmployees; }
        public EmployeeWorkload getSuggestedEmployee() { return suggestedEmployee; }
        public String getSuggestionReason() { return suggestionReason; }
        public LocalDate getAnalysisDate() { return analysisDate; }

        public boolean hasSuggestion() { return suggestedEmployee != null; }
    }

    /**
     * Main API method: Analyzes workload for all team members of a project
     * and suggests the best employee for a new task (without skill matching).
     *
     * @param projectId The project ID to analyze
     * @return WorkloadAnalysisResult with ranked employees and suggestion
     */
    public WorkloadAnalysisResult analyzeProjectWorkload(int projectId) throws SQLException {
        return analyzeProjectWorkload(projectId, null, null);
    }

    /**
     * Main API method: Analyzes workload for all team members of a project
     * and suggests the best employee for a new task based on:
     *   1. Skill matching (primary) — how well the employee's skills match the task
     *   2. Workload availability (secondary) — who is the least busy
     *
     * @param projectId       The project ID to analyze
     * @param taskTitle       The title of the new task (used for skill matching, can be null)
     * @param taskDescription The description of the new task (used for skill matching, can be null)
     * @return WorkloadAnalysisResult with ranked employees and suggestion
     */
    public WorkloadAnalysisResult analyzeProjectWorkload(int projectId, String taskTitle, String taskDescription)
            throws SQLException {
        // Get all team members for this project
        List<Integer> teamMemberIds = getProjectTeamMembers(projectId);

        if (teamMemberIds.isEmpty()) {
            return new WorkloadAnalysisResult(new ArrayList<>(), null,
                    "Aucun membre dans l'équipe du projet.");
        }

        // Get all tasks for analysis
        List<Tache> allTasks = tacheCRUD.afficher();

        // Calculate workload for each team member
        List<EmployeeWorkload> workloads = new ArrayList<>();

        for (Integer empId : teamMemberIds) {
            EmployeeWorkload workload = calculateEmployeeWorkload(empId, allTasks);
            if (workload != null) {
                workloads.add(workload);
            }
        }

        // Calculate skill match scores if task info is provided
        boolean hasTaskInfo = (taskTitle != null && !taskTitle.isBlank())
                || (taskDescription != null && !taskDescription.isBlank());

        if (hasTaskInfo) {
            Set<String> taskKeywords = extractTaskKeywords(taskTitle, taskDescription);
            for (EmployeeWorkload w : workloads) {
                calculateSkillMatch(w, taskKeywords);
            }
        }

        // Sort: primary by skill match (descending), secondary by workload score (ascending)
        workloads.sort((a, b) -> {
            if (hasTaskInfo) {
                int skillCmp = Double.compare(b.getSkillMatchScore(), a.getSkillMatchScore());
                if (skillCmp != 0) return skillCmp;
            }
            return Double.compare(a.getWorkloadScore(), b.getWorkloadScore());
        });

        // Determine suggestion
        EmployeeWorkload suggested = null;
        String reason = "";

        if (!workloads.isEmpty()) {
            suggested = workloads.get(0);
            reason = generateSuggestionReason(suggested, workloads, hasTaskInfo);
        }

        return new WorkloadAnalysisResult(workloads, suggested, reason);
    }

    /**
     * Calculate workload score for a specific employee
     */
    private EmployeeWorkload calculateEmployeeWorkload(int employeeId, List<Tache> allTasks)
            throws SQLException {

        // Get employee info
        String employeeName = getEmployeeName(employeeId);
        if (employeeName == null) return null;

        // Filter active tasks for this employee (not completed)
        List<Tache> employeeTasks = allTasks.stream()
                .filter(t -> t.getId_employe() == employeeId)
                .filter(t -> t.getStatut_tache() != statut_t.TERMINEE)
                .collect(Collectors.toList());

        int totalActive = employeeTasks.size();
        int highPriority = 0;
        int mediumPriority = 0;
        int lowPriority = 0;
        int urgent = 0;
        double score = 0;

        LocalDate today = LocalDate.now();

        for (Tache task : employeeTasks) {
            // Count by priority
            priority p = task.getPriority_tache();
            if (p == priority.HAUTE) {
                highPriority++;
                score += 3;
            } else if (p == priority.MOYENNE) {
                mediumPriority++;
                score += 2;
            } else {
                lowPriority++;
                score += 1;
            }

            // Check deadline urgency
            LocalDate deadline = task.getDate_limite();
            if (deadline != null) {
                long daysUntilDue = ChronoUnit.DAYS.between(today, deadline);
                if (daysUntilDue <= 3 && daysUntilDue >= 0) {
                    urgent++;
                    score += 2; // Urgent bonus
                } else if (daysUntilDue <= 7 && daysUntilDue > 3) {
                    score += 1; // Soon bonus
                }
            }

            // Blocked tasks reduce effective workload slightly (waiting on others)
            if (task.getStatut_tache() == statut_t.BLOCQUEE) {
                score -= 0.5;
            }
        }

        return new EmployeeWorkload(employeeId, employeeName, totalActive,
                highPriority, mediumPriority, lowPriority,
                urgent, Math.max(0, score));
    }

    /**
     * Generate a human-readable suggestion reason
     */
    private String generateSuggestionReason(EmployeeWorkload suggested,
                                            List<EmployeeWorkload> allWorkloads,
                                            boolean hasSkillMatch) {
        StringBuilder reason = new StringBuilder();

        // Skill-based reasoning (primary)
        if (hasSkillMatch && suggested.getSkillMatchScore() > 0) {
            reason.append("🎯 ").append(suggested.getEmployeeName())
                    .append(" possède les compétences adaptées à cette tâche (")
                    .append(String.format("%.0f%%", suggested.getSkillMatchScore() * 100))
                    .append(" de correspondance");
            if (!suggested.getMatchedSkills().isEmpty()) {
                reason.append(": ").append(String.join(", ", suggested.getMatchedSkills()));
            }
            reason.append(").\n");
        } else if (hasSkillMatch) {
            reason.append("⚠️ Aucun employé n'a de compétences correspondantes — suggestion basée sur la disponibilité.\n");
        }

        // Workload-based reasoning (secondary)
        if (suggested.getTotalActiveTasks() == 0) {
            reason.append("✨ ").append(suggested.getEmployeeName())
                    .append(" n'a aucune tâche active - parfait pour une nouvelle tâche!");
        } else if (suggested.getWorkloadScore() < 5) {
            reason.append("👍 ").append(suggested.getEmployeeName())
                    .append(" a une charge de travail légère (")
                    .append(suggested.getTotalActiveTasks())
                    .append(" tâche(s) active(s)).");
        } else if (suggested.getUrgentTasks() == 0) {
            reason.append("📋 ").append(suggested.getEmployeeName())
                    .append(" n'a pas de tâches urgentes et peut prendre du travail supplémentaire.");
        } else {
            reason.append("⚖️ ").append(suggested.getEmployeeName())
                    .append(" est le plus disponible parmi l'équipe.");
        }

        // Add comparison if there are multiple employees
        if (allWorkloads.size() > 1) {
            double avgScore = allWorkloads.stream()
                    .mapToDouble(EmployeeWorkload::getWorkloadScore)
                    .average()
                    .orElse(0);

            if (suggested.getWorkloadScore() < avgScore * 0.5) {
                reason.append("\n📊 Score bien inférieur à la moyenne de l'équipe.");
            }
        }

        return reason.toString();
    }

    /**
     * Extract keywords from the task title and description for skill matching.
     * Normalizes to lowercase and filters out common stop words.
     */
    private Set<String> extractTaskKeywords(String title, String description) {
        Set<String> keywords = new HashSet<>();
        Set<String> stopWords = Set.of(
                "le", "la", "les", "un", "une", "des", "de", "du", "et", "ou", "en", "à", "au", "aux",
                "pour", "par", "sur", "dans", "avec", "est", "sont", "être", "avoir", "fait", "faire",
                "the", "a", "an", "and", "or", "in", "on", "of", "for", "to", "with", "is", "are",
                "this", "that", "it", "do", "be", "has", "was", "will", "can", "should",
                "il", "elle", "ce", "cette", "ces", "qui", "que", "ne", "pas", "se", "sa", "son",
                "tâche", "task", "projet", "project", "créer", "create", "mettre", "jour", "mise"
        );

        String combined = "";
        if (title != null) combined += title + " ";
        if (description != null) combined += description;

        // Split on non-alphanumeric characters, keep tokens of length >= 2
        String[] tokens = combined.toLowerCase().split("[^a-zA-Z0-9àâäéèêëïîôùûüçñ]+");
        for (String token : tokens) {
            if (token.length() >= 2 && !stopWords.contains(token)) {
                keywords.add(token);
            }
        }

        return keywords;
    }

    /**
     * Get the skills list for an employee from the compétence_employé table
     */
    private List<String> getEmployeeSkills(int employeeId) throws SQLException {
        String sql = "SELECT skills, formations, experience FROM competence_employe WHERE id_employe = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    List<String> allSkills = new ArrayList<>();

                    // Parse skills (JSON array of strings, e.g. ["Java", "Python", "SQL"])
                    String skillsJson = rs.getString("skills");
                    allSkills.addAll(json.parseJsonArray(skillsJson));

                    // Parse formations — extract degree field
                    String formationsJson = rs.getString("formations");
                    allSkills.addAll(json.parseJsonArrayField(formationsJson, "degree"));

                    // Parse experience — extract job_title field
                    String experienceJson = rs.getString("experience");
                    allSkills.addAll(json.parseJsonArrayField(experienceJson, "job_title"));

                    return allSkills;
                }
            }
        }
        return new ArrayList<>();
    }

    /**
     * Calculate how well an employee's skills match the task keywords.
     * Uses case-insensitive substring matching (a skill like "JavaScript" matches keyword "javascript",
     * and a keyword like "backend" matches a skill like "Backend Development").
     */
    private void calculateSkillMatch(EmployeeWorkload workload, Set<String> taskKeywords)
            throws SQLException {
        if (taskKeywords.isEmpty()) {
            workload.setSkillMatchScore(0.0);
            return;
        }

        List<String> employeeSkills = getEmployeeSkills(workload.getEmployeeId());
        if (employeeSkills.isEmpty()) {
            workload.setSkillMatchScore(0.0);
            return;
        }

        // Normalize employee skills to lowercase for comparison
        List<String> normalizedSkills = employeeSkills.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        List<String> matched = new ArrayList<>();
        int matchCount = 0;

        for (String keyword : taskKeywords) {
            for (int i = 0; i < normalizedSkills.size(); i++) {
                String skill = normalizedSkills.get(i);
                // Check if keyword is contained in a skill or skill is contained in keyword
                if (skill.contains(keyword) || keyword.contains(skill)) {
                    matchCount++;
                    String originalSkill = employeeSkills.get(i);
                    if (!matched.contains(originalSkill)) {
                        matched.add(originalSkill);
                    }
                    break; // Count each keyword only once
                }
            }
        }

        double score = (double) matchCount / taskKeywords.size();
        workload.setSkillMatchScore(Math.min(1.0, score));
        workload.setMatchedSkills(matched);
    }

    /**
     * Get team member IDs for a project
     */
    private List<Integer> getProjectTeamMembers(int projectId) throws SQLException {
        String sql = "SELECT id_employe FROM equipe_projet WHERE id_projet = ?";
        List<Integer> ids = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("id_employe"));
                }
            }
        }
        return ids;
    }

    /**
     * Get employee full name by ID
     */
    private String getEmployeeName(int employeeId) throws SQLException {
        String sql = "SELECT CONCAT(nom, ' ', prenom) AS full_name FROM employe WHERE id_employe = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("full_name");
                }
            }
        }
        return null;
    }

    /**
     * Quick method to just get the suggested employee ID for a project
     */
    public Integer getSuggestedEmployeeId(int projectId) throws SQLException {
        WorkloadAnalysisResult result = analyzeProjectWorkload(projectId);
        if (result.hasSuggestion()) {
            return result.getSuggestedEmployee().getEmployeeId();
        }
        return null;
    }

    /**
     * Quick method to get the suggested employee ID considering task skills
     */
    public Integer getSuggestedEmployeeId(int projectId, String taskTitle, String taskDescription)
            throws SQLException {
        WorkloadAnalysisResult result = analyzeProjectWorkload(projectId, taskTitle, taskDescription);
        if (result.hasSuggestion()) {
            return result.getSuggestedEmployee().getEmployeeId();
        }
        return null;
    }

    /**
     * Get workload summary as formatted string (useful for tooltips/display)
     */
    public String getWorkloadSummary(int projectId) throws SQLException {
        return getWorkloadSummary(projectId, null, null);
    }

    /**
     * Get workload summary with skill matching as formatted string
     */
    public String getWorkloadSummary(int projectId, String taskTitle, String taskDescription)
            throws SQLException {
        WorkloadAnalysisResult result = analyzeProjectWorkload(projectId, taskTitle, taskDescription);

        StringBuilder sb = new StringBuilder();
        sb.append("📊 ANALYSE DE CHARGE DE TRAVAIL\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        if (result.getRankedEmployees().isEmpty()) {
            sb.append("Aucun membre dans l'équipe.");
            return sb.toString();
        }

        for (EmployeeWorkload w : result.getRankedEmployees()) {
            sb.append(w.getAvailabilityStatus()).append(" ")
                    .append(w.getEmployeeName()).append("\n")
                    .append("   📋 Tâches actives: ").append(w.getTotalActiveTasks()).append("\n")
                    .append("   🔴 Haute priorité: ").append(w.getHighPriorityTasks()).append("\n")
                    .append("   ⏰ Urgentes: ").append(w.getUrgentTasks()).append("\n")
                    .append("   📈 Score: ").append(String.format("%.1f", w.getWorkloadScore())).append("\n");
            if (w.getSkillMatchScore() > 0) {
                sb.append("   🎯 Compétences: ").append(String.format("%.0f%%", w.getSkillMatchScore() * 100));
                if (!w.getMatchedSkills().isEmpty()) {
                    sb.append(" (").append(String.join(", ", w.getMatchedSkills())).append(")");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        if (result.hasSuggestion()) {
            sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            sb.append("💡 SUGGESTION:\n");
            sb.append(result.getSuggestionReason());
        }

        return sb.toString();
    }
}
