# MonLogCompta

Application de bureau JavaFX réalisée dans le cadre d'un projet BTS SIO. Elle gère les clients,
produits, factures, paiements, écritures comptables et transmissions de factures électroniques.
Les données sont conservées localement dans une base SQLite.

## Prérequis

- JDK 21
- Maven 3.9+

Avec `mise`, exécuter `mise install`.

## Lancement

```bash
mvn javafx:run
```

La base est créée au premier lancement dans `~/.monlogcompta/monlogcompta.db`.

## Comptes de démonstration

| Profil | Adresse e-mail | Mot de passe |
|---|---|---|
| Administrateur | `admin@monlogcompta.fr` | `bts2024` |
| Client (Boulangerie Dupont) | `client@dupont-boulangerie.fr` | `client2024` |

L'administrateur accède aux sept modules. Le client ne voit que son tableau de bord, ses factures
et leur historique de transmission. Les restrictions sont appliquées dans les requêtes SQL, pas
uniquement dans l'interface.

## Structure

- `src/main/java/fr/monlogcompta/MonLogComptaApp.java` : cycle de vie JavaFX
- `src/main/java/fr/monlogcompta/db/Database.java` : schéma, données de démonstration et accès JDBC
- `src/main/java/fr/monlogcompta/model/SessionUser.java` : utilisateur connecté et rôle
- `src/main/java/fr/monlogcompta/ui/LoginView.java` : authentification
- `src/main/java/fr/monlogcompta/ui/MainView.java` : navigation et vues métier
- `src/main/resources/fr/monlogcompta/app.css` : identité visuelle JavaFX

Les exports JSON, XML/UBL fictif et FEC sont écrits dans `~/MonLogCompta/exports`.
