package fr.monlogcompta.db;

import fr.monlogcompta.model.SessionUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseTest {
    @TempDir
    Path directory;

    @Test
    void authenticatesAllDemoRoles() {
        try (Database database = new Database(directory.resolve("test.db"))) {
            assertTrue(database.authenticate("utilisateur@monlogcompta.fr", "bts2024").orElseThrow().isStaff());
            assertTrue(database.authenticate("admin@monlogcompta.fr", "admin2024").orElseThrow().isAdmin());
            assertTrue(database.authenticate("client@dupont-boulangerie.fr", "client2024").orElseThrow().isClient());
            assertTrue(database.authenticate("utilisateur@monlogcompta.fr", "incorrect").isEmpty());
        }
    }

    @Test
    void seededJournalIsBalanced() {
        try (Database database = new Database(directory.resolve("test.db"))) {
            var totals = database.query("SELECT ROUND(SUM(debit),2) d, ROUND(SUM(credit),2) c FROM ecriture_comptable").getFirst();
            assertEquals(((Number) totals.get("d")).doubleValue(), ((Number) totals.get("c")).doubleValue(), 0.001);
        }
    }

    @Test
    void userCanCreateClientAccountThatCanLogIn() throws SQLException {
        try (Database database = new Database(directory.resolve("test.db"))) {
            long clientId = database.createClientWithAccount("Boucherie Test", "12345678901234",
                "1 rue du Test, 75001 Paris", "Boucherie@Test.fr", "motdepasse1");
            SessionUser client = database.authenticate("boucherie@test.fr", "motdepasse1").orElseThrow();
            assertTrue(client.isClient());
            assertEquals(clientId, client.clientId());
        }
    }

    @Test
    void duplicateClientCreationLeavesNothingBehind() throws SQLException {
        try (Database database = new Database(directory.resolve("test.db"))) {
            long before = (Long) database.query("SELECT COUNT(*) n FROM client").getFirst().get("n");
            // SIRET déjà utilisé par la Boulangerie Dupont : la transaction doit être annulée
            assertThrows(SQLException.class, () -> database.createClientWithAccount("Doublon", "41234567890123",
                "adresse", "doublon@test.fr", "motdepasse1"));
            assertEquals(before, (Long) database.query("SELECT COUNT(*) n FROM client").getFirst().get("n"));
            assertTrue(database.authenticate("doublon@test.fr", "motdepasse1").isEmpty());
        }
    }

    @Test
    void adminManagesUsersButKeepsLastAdmin() throws SQLException {
        try (Database database = new Database(directory.resolve("test.db"))) {
            SessionUser admin = database.authenticate("admin@monlogcompta.fr", "admin2024").orElseThrow();
            database.createUser("nouveau@monlogcompta.fr", "Nouveau", "motdepasse1", SessionUser.Role.UTILISATEUR);
            SessionUser created = database.authenticate("nouveau@monlogcompta.fr", "motdepasse1").orElseThrow();
            assertTrue(created.isStaff());

            database.resetPassword(created.id(), "autremdp123");
            assertTrue(database.authenticate("nouveau@monlogcompta.fr", "motdepasse1").isEmpty());
            assertFalse(database.authenticate("nouveau@monlogcompta.fr", "autremdp123").isEmpty());

            database.deleteUser(created.id(), admin.id());
            assertTrue(database.authenticate("nouveau@monlogcompta.fr", "autremdp123").isEmpty());
            assertThrows(IllegalStateException.class, () -> database.deleteUser(admin.id(), admin.id()));
        }
    }

    /** Une base créée avant ce changement doit garder ses données : l'ancien admin devient utilisateur. */
    @Test
    void migratesLegacyAdminToUtilisateur() throws SQLException {
        Path file = directory.resolve("legacy.db");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE client (id INTEGER PRIMARY KEY, code TEXT UNIQUE NOT NULL, "
                + "raison_sociale TEXT NOT NULL, siret TEXT UNIQUE NOT NULL, adresse TEXT NOT NULL, email TEXT NOT NULL)");
            statement.executeUpdate("INSERT INTO client VALUES(1,'C001','Ancien client','12345678901234','adresse','a@a.fr')");
            statement.executeUpdate("CREATE TABLE utilisateur (id INTEGER PRIMARY KEY, email TEXT UNIQUE NOT NULL, "
                + "password_hash TEXT NOT NULL, nom_affiche TEXT NOT NULL, "
                + "role TEXT NOT NULL CHECK(role IN ('ADMIN','CLIENT')), client_id INTEGER REFERENCES client(id))");
            statement.executeUpdate("INSERT INTO utilisateur(email,password_hash,nom_affiche,role) VALUES("
                + "'admin@monlogcompta.fr','aa01abe9cce6c4dc342462866013f384de93d07759fe698637f856045f766deb',"
                + "'Romain & Lucas','ADMIN')");
        }
        try (Database database = new Database(file)) {
            assertTrue(database.authenticate("utilisateur@monlogcompta.fr", "bts2024").orElseThrow().isStaff());
            assertTrue(database.authenticate("admin@monlogcompta.fr", "admin2024").orElseThrow().isAdmin());
        }
    }
}
