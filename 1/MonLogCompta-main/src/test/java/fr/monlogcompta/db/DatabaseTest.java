package fr.monlogcompta.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseTest {
    @Test
    void authenticatesBothDemoRoles() {
        try (Database database = new Database()) {
            assertTrue(database.authenticate("admin@monlogcompta.fr", "bts2024").orElseThrow().isAdmin());
            assertFalse(database.authenticate("client@dupont-boulangerie.fr", "client2024").orElseThrow().isAdmin());
            assertTrue(database.authenticate("admin@monlogcompta.fr", "incorrect").isEmpty());
        }
    }
}
