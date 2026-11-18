package fr.monlogcompta.db;

import fr.monlogcompta.model.SessionUser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Database implements AutoCloseable {
    private final Connection connection;

    public Database() {
        try {
            Path directory = Path.of(System.getProperty("user.home"), ".monlogcompta");
            Files.createDirectories(directory);
            connection = DriverManager.getConnection("jdbc:sqlite:" + directory.resolve("monlogcompta.db"));
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }
            initialize();
        } catch (SQLException | IOException exception) {
            throw new IllegalStateException("Impossible d'initialiser la base de données", exception);
        }
    }

    private void initialize() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS client (
                    id INTEGER PRIMARY KEY, code TEXT UNIQUE NOT NULL, raison_sociale TEXT NOT NULL,
                    siret TEXT UNIQUE NOT NULL, adresse TEXT NOT NULL, email TEXT NOT NULL
                )""");
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS utilisateur (
                    id INTEGER PRIMARY KEY, email TEXT UNIQUE NOT NULL, password_hash TEXT NOT NULL,
                    nom_affiche TEXT NOT NULL, role TEXT NOT NULL CHECK(role IN ('ADMIN','CLIENT')),
                    client_id INTEGER REFERENCES client(id)
                )""");
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS produit (
                    id INTEGER PRIMARY KEY, reference TEXT UNIQUE NOT NULL, libelle TEXT NOT NULL,
                    prix_ht REAL NOT NULL, prix_achat REAL NOT NULL, tva REAL NOT NULL
                )""");
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS facture (
                    id INTEGER PRIMARY KEY, numero TEXT UNIQUE NOT NULL, date_emission TEXT NOT NULL,
                    client_id INTEGER NOT NULL REFERENCES client(id), montant_ht REAL NOT NULL,
                    tva_montant REAL NOT NULL, total_ttc REAL NOT NULL, statut TEXT NOT NULL,
                    remise REAL NOT NULL DEFAULT 0
                )""");
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS ligne_facture (
                    id INTEGER PRIMARY KEY, facture_id INTEGER NOT NULL REFERENCES facture(id),
                    libelle TEXT NOT NULL, quantite REAL NOT NULL, prix_unitaire_ht REAL NOT NULL,
                    tva REAL NOT NULL, total_ht REAL NOT NULL
                )""");
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS paiement (
                    id INTEGER PRIMARY KEY, facture_id INTEGER NOT NULL REFERENCES facture(id),
                    date_paiement TEXT NOT NULL, montant REAL NOT NULL, mode TEXT NOT NULL
                )""");
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS compte_comptable (
                    numero TEXT PRIMARY KEY, libelle TEXT NOT NULL
                )""");
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS ecriture_comptable (
                    id INTEGER PRIMARY KEY, date_ecriture TEXT NOT NULL, compte_numero TEXT NOT NULL
                    REFERENCES compte_comptable(numero), libelle TEXT NOT NULL, reference TEXT NOT NULL,
                    debit REAL NOT NULL DEFAULT 0, credit REAL NOT NULL DEFAULT 0
                )""");
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS historique_transmission (
                    id INTEGER PRIMARY KEY, facture_id INTEGER NOT NULL REFERENCES facture(id),
                    date_transmission TEXT NOT NULL, statut TEXT NOT NULL, format TEXT NOT NULL
                )""");
        }
        if (scalarLong("SELECT COUNT(*) FROM client") == 0) {
            seed();
        }
    }

    private void seed() throws SQLException {
        connection.setAutoCommit(false);
        try {
            executeBatch("INSERT INTO client(id,code,raison_sociale,siret,adresse,email) VALUES(?,?,?,?,?,?)",
                List.of(
                    new Object[]{1,"C001","Boulangerie Dupont & Fils","41234567890123","14 rue de la Paix, 75001 Paris","contact@dupont-boulangerie.fr"},
                    new Object[]{2,"C002","SARL Martin Technologies","52345678901234","3 allée des Acacias, 69003 Lyon","admin@martin-tech.fr"},
                    new Object[]{3,"C003","Cabinet Leroy Architecture","63456789012345","8 boulevard Haussmann, 75009 Paris","comptabilite@leroy-archi.fr"},
                    new Object[]{4,"C004","Fromagerie Beaumont SAS","74567890123456","22 place du Marché, 13001 Marseille","gestion@fromagerie-beaumont.fr"},
                    new Object[]{5,"C005","Transports Girard & Co","85678901234567","5 zone industrielle Nord, 31000 Toulouse","facturation@transports-girard.fr"},
                    new Object[]{6,"C006","Imprimerie des Flandres","96789012345678","17 rue Nationale, 59000 Lille","direction@flandres-print.fr"}
                ));
            execute("INSERT INTO utilisateur(email,password_hash,nom_affiche,role,client_id) VALUES(?,?,?,?,?)",
                "admin@monlogcompta.fr", hash("bts2024"), "Romain & Lucas", "ADMIN", null);
            execute("INSERT INTO utilisateur(email,password_hash,nom_affiche,role,client_id) VALUES(?,?,?,?,?)",
                "client@dupont-boulangerie.fr", hash("client2024"), "Boulangerie Dupont", "CLIENT", 1);

            executeBatch("INSERT INTO produit(id,reference,libelle,prix_ht,prix_achat,tva) VALUES(?,?,?,?,?,?)",
                List.of(
                    new Object[]{1,"P001","Développement web — heure",95d,42d,20d},
                    new Object[]{2,"P002","Maintenance mensuelle (forfait)",250d,80d,20d},
                    new Object[]{3,"P003","Audit et conseil comptable — heure",180d,60d,20d},
                    new Object[]{4,"P004","Formation utilisateur — demi-journée",120d,40d,20d},
                    new Object[]{5,"P005","Licence logiciel annuelle",480d,200d,20d},
                    new Object[]{6,"P006","Consultation juridique — heure",220d,0d,20d},
                    new Object[]{7,"P007","Hébergement serveur — mois",35d,12d,20d}
                ));
            executeBatch("INSERT INTO facture(id,numero,date_emission,client_id,montant_ht,tva_montant,total_ttc,statut,remise) VALUES(?,?,?,?,?,?,?,?,?)",
                List.of(
                    new Object[]{1,"FAC-2024-0042","2024-09-15",1,1425d,285d,1710d,"Payée",0d},
                    new Object[]{2,"FAC-2024-0043","2024-09-18",2,2223d,444.60d,2667.60d,"En attente",5d},
                    new Object[]{3,"FAC-2024-0044","2024-09-22",3,860d,172d,1032d,"En retard",0d},
                    new Object[]{4,"FAC-2024-0045","2024-09-28",4,2880d,576d,3456d,"Payée",10d},
                    new Object[]{5,"FAC-2024-0046","2024-10-01",5,750d,150d,900d,"En attente",0d},
                    new Object[]{6,"FAC-2024-0047","2024-10-05",3,1960d,392d,2352d,"En attente",0d}
                ));
            executeBatch("INSERT INTO ligne_facture(facture_id,libelle,quantite,prix_unitaire_ht,tva,total_ht) VALUES(?,?,?,?,?,?)",
                List.of(
                    new Object[]{1,"Développement web — heure",12d,95d,20d,1140d},
                    new Object[]{1,"Formation utilisateur — demi-journée",2d,120d,20d,240d},
                    new Object[]{1,"Hébergement serveur — mois",1d,35d,20d,35d},
                    new Object[]{2,"Audit et conseil comptable — heure",8d,180d,20d,1440d},
                    new Object[]{2,"Licence logiciel annuelle",1d,480d,20d,480d},
                    new Object[]{2,"Maintenance mensuelle (forfait)",1d,250d,20d,250d},
                    new Object[]{3,"Développement web — heure",6d,95d,20d,570d},
                    new Object[]{3,"Consultation juridique — heure",1d,220d,20d,220d},
                    new Object[]{3,"Hébergement serveur — mois",2d,35d,20d,70d},
                    new Object[]{4,"Développement web — heure",24d,95d,20d,2280d},
                    new Object[]{4,"Formation utilisateur — demi-journée",5d,120d,20d,600d},
                    new Object[]{5,"Maintenance mensuelle (forfait)",3d,250d,20d,750d},
                    new Object[]{6,"Audit et conseil comptable — heure",8d,180d,20d,1440d},
                    new Object[]{6,"Développement web — heure",4d,95d,20d,380d},
                    new Object[]{6,"Hébergement serveur — mois",4d,35d,20d,140d}
                ));
            execute("INSERT INTO paiement(facture_id,date_paiement,montant,mode) VALUES(?,?,?,?)",
                1, "2024-09-20", 1710d, "Virement bancaire");
            executeBatch("INSERT INTO compte_comptable(numero,libelle) VALUES(?,?)", List.of(
                new Object[]{"411100","Clients"}, new Object[]{"706200","Prestations de services"},
                new Object[]{"709100","Rabais et remises accordés"}, new Object[]{"445711","TVA collectée"}
            ));
            executeBatch("INSERT INTO ecriture_comptable(date_ecriture,compte_numero,libelle,reference,debit,credit) VALUES(?,?,?,?,?,?)",
                List.of(
                    new Object[]{"2024-09-15","411100","Boulangerie Dupont & Fils","FAC-2024-0042",1710d,0d},
                    new Object[]{"2024-09-15","706200","Prestations de services","FAC-2024-0042",0d,1425d},
                    new Object[]{"2024-09-15","445711","TVA collectée 20 %","FAC-2024-0042",0d,285d},
                    new Object[]{"2024-09-18","411100","SARL Martin Technologies","FAC-2024-0043",2667.60d,0d},
                    new Object[]{"2024-09-18","709100","Rabais et remises accordés","FAC-2024-0043",117d,0d},
                    new Object[]{"2024-09-18","706200","Prestations de services","FAC-2024-0043",0d,2340d},
                    new Object[]{"2024-09-18","445711","TVA collectée 20 %","FAC-2024-0043",0d,444.60d}
                ));
            executeBatch("INSERT INTO historique_transmission(facture_id,date_transmission,statut,format) VALUES(?,?,?,?)",
                List.of(
                    new Object[]{1,"2024-09-15 14:32","Acceptée","XML/UBL 2.1"},
                    new Object[]{3,"2024-09-22 09:17","Rejetée","XML/UBL 2.1"},
                    new Object[]{4,"2024-09-28 16:45","Acceptée","XML/UBL 2.1"},
                    new Object[]{5,"2024-10-01 11:05","En cours","XML/UBL 2.1"}
                ));
            connection.commit();
        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public Optional<SessionUser> authenticate(String email, String password) {
        String sql = "SELECT id,email,nom_affiche,role,client_id FROM utilisateur WHERE lower(email)=lower(?) AND password_hash=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email.strip());
            statement.setString(2, hash(password));
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return Optional.empty();
                long client = result.getLong("client_id");
                return Optional.of(new SessionUser(result.getLong("id"), result.getString("email"),
                    result.getString("nom_affiche"), SessionUser.Role.valueOf(result.getString("role")),
                    result.wasNull() ? null : client));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Échec de l'authentification", exception);
        }
    }

    public List<Map<String, Object>> query(String sql, Object... parameters) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            try (ResultSet result = statement.executeQuery()) {
                ResultSetMetaData metadata = result.getMetaData();
                List<Map<String, Object>> rows = new ArrayList<>();
                while (result.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int column = 1; column <= metadata.getColumnCount(); column++) {
                        row.put(metadata.getColumnLabel(column), result.getObject(column));
                    }
                    rows.add(row);
                }
                return rows;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Échec de la requête", exception);
        }
    }

    public void execute(String sql, Object... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            statement.executeUpdate();
        }
    }

    private void executeBatch(String sql, List<Object[]> rows) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Object[] row : rows) {
                bind(statement, row);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void bind(PreparedStatement statement, Object... parameters) throws SQLException {
        for (int index = 0; index < parameters.length; index++) {
            statement.setObject(index + 1, parameters[index]);
        }
    }

    private long scalarLong(String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            return result.next() ? result.getLong(1) : 0;
        }
    }

    public Path writeExport(String filename, String content) {
        try {
            Path directory = Path.of(System.getProperty("user.home"), "MonLogCompta", "exports");
            Files.createDirectories(directory);
            return Files.writeString(directory.resolve(filename), content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible d'écrire le fichier d'export", exception);
        }
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException ignored) {
            // L'application se termine déjà ; aucune récupération utile n'est possible.
        }
    }
}
