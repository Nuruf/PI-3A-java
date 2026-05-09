import entities.offres.CategorieOffre;
import entities.offres.EtatOffre;
import entities.offres.Offre;
import entities.offres.TypeContrat;
import org.junit.jupiter.api.*;
import service.OffreCRUD;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OffreTest {
    static OffreCRUD oCRUD;

    @BeforeAll
    static void setup() {
        oCRUD = new OffreCRUD();
    }

    static int idOffreTest = -1;

    @Test
    @Order(1)
    void testAjouterOffre() throws SQLException {
        Offre offre = new Offre(1, "Développeur Java", TypeContrat.CDI, java.sql.Date.valueOf("2024-12-31"), EtatOffre.OUVERT, "Description test pour ajout", CategorieOffre.INFORMATIQUE);
        oCRUD.ajouter(offre);

        List<Offre> offres = oCRUD.afficher();
        assertFalse(offres.isEmpty());

        // Find the newly added offre by title and description
        Offre foundOffre = offres.stream()
                .filter(o -> o.getTitrePoste().equals("Développeur Java") &&
                           o.getDescription().equals("Description test pour ajout"))
                .findFirst()
                .orElse(null);

        assertNotNull(foundOffre, "Offre should be found after insertion");
        idOffreTest = foundOffre.getId();
        System.out.println("idOffreTest: " + idOffreTest);
    }

    @Test
    @Order(2)
    void testModifierOffre() throws SQLException {
        assertTrue(idOffreTest > 0, "idOffreTest should be set from testAjouterOffre");

        Offre offre = new Offre(1, "Développeur Java Senior", TypeContrat.CDI, java.sql.Date.valueOf("2024-12-31"), EtatOffre.OUVERT, "Description test pour modification", CategorieOffre.INFORMATIQUE);
        offre.setId(idOffreTest);
        oCRUD.modifier(offre);

        Offre modifiedOffre = oCRUD.getById(idOffreTest);
        assertNotNull(modifiedOffre, "Modified offre should exist");
        assertEquals("Développeur Java Senior", modifiedOffre.getTitrePoste(), "Title should be updated");
        assertEquals("Description test pour modification", modifiedOffre.getDescription(), "Description should be updated");
    }

    @Test
    @Order(3)
    void testSupprimerOffre() throws SQLException {
        assertTrue(idOffreTest > 0, "idOffreTest should be set");

        oCRUD.supprimer(idOffreTest);
        Offre deletedOffre = oCRUD.getById(idOffreTest);
        assertNull(deletedOffre, "Offre should be deleted");
    }

    @AfterEach
    void cleanUp() {
        // Clean up any test offres that might have been created
        if (idOffreTest > 0) {
            try {
                oCRUD.supprimer(idOffreTest);
            } catch (Exception e) {
                // Already deleted or doesn't exist
            }
        }
    }

}
