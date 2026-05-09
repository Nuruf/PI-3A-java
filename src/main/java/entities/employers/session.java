package entities.employers;

public class session {
    private static compte currentCompte;
    private static administrateur_systeme currentAdmin;
    private static employe currentEmploye;
    private static visiteur visiteur;

    // Compte
    public static void setCompte(compte c) {
        currentCompte = c;
    }

    public static compte getCompte() {
        return currentCompte;
    }

    // Admin système
    public static void setAdmin(administrateur_systeme admin) {
        currentAdmin = admin;
    }
    public static administrateur_systeme getAdmin() {
        return currentAdmin;
    }
    public static void setEmploye(employe emp) {
        currentEmploye = emp;
    }
    public static employe getEmploye() {
        return currentEmploye;
    }
    public static int getIdEntreprise() {
        if (currentEmploye != null) {
            return currentEmploye.getIdEntreprise();
        }
        return -1;
    }
    public static visiteur getVisiteur() { return visiteur; }
    public static void setVisiteur(visiteur v) { visiteur = v; }

    public static void logout() {
        currentCompte = null;
        currentAdmin = null;
        currentEmploye = null;
        visiteur = null;
    }
}