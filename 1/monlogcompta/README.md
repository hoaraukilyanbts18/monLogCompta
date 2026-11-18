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

## Déploiement

```bash
mvn clean package                              # tests + JAR exécutable
java -jar target/monlogcompta-1.0.0-shaded.jar  # lancement du JAR
./scripts/package.sh app-image                 # application autonome avec JRE embarqué (jpackage)
```

Le JAR embarque les bibliothèques JavaFX de l'OS qui l'a construit : construire sur chaque OS cible
(Windows, Linux). Pour un `.exe`/`.msi` Windows : `package.sh exe` avec WiX 3 installé.
Sauvegarde : copier `~/.monlogcompta/monlogcompta.db`.

## Comptes de démonstration

| Profil | Adresse e-mail | Mot de passe |
|---|---|---|
| Utilisateur (gère l'activité) | `utilisateur@monlogcompta.fr` | `bts2024` |
| Administrateur (gère les comptes) | `admin@monlogcompta.fr` | `admin2024` |
| Client (Boulangerie Dupont) | `client@dupont-boulangerie.fr` | `client2024` |

- **Utilisateur** : clients, produits, factures, comptabilité, exports, historique. Il crée les clients
  depuis l'écran Clients (« + Nouveau client ») : la fiche et le compte de connexion du client sont
  créés ensemble.
- **Administrateur** : écran Utilisateurs uniquement (créer, modifier, réinitialiser le mot de passe,
  supprimer ; le dernier administrateur ne peut pas être supprimé).
- **Client** : son tableau de bord, ses factures et leur historique. Les restrictions sont appliquées
  dans les requêtes SQL, pas uniquement dans l'interface.

Une base créée avant l'ajout des rôles est migrée automatiquement : l'ancien compte
`admin@monlogcompta.fr` devient `utilisateur@monlogcompta.fr`. Pour repartir de zéro, supprimer
`~/.monlogcompta/monlogcompta.db`.

## Structure

- `src/main/java/fr/monlogcompta/MonLogComptaApp.java` : cycle de vie JavaFX
- `src/main/java/fr/monlogcompta/db/Database.java` : schéma, données de démonstration et accès JDBC
- `src/main/java/fr/monlogcompta/model/SessionUser.java` : utilisateur connecté et rôle
- `src/main/java/fr/monlogcompta/ui/LoginView.java` : authentification
- `src/main/java/fr/monlogcompta/ui/MainView.java` : navigation et vues métier
- `src/main/resources/fr/monlogcompta/app.css` : identité visuelle JavaFX

Les exports JSON, XML/UBL fictif et FEC sont écrits dans `~/MonLogCompta/exports`.
