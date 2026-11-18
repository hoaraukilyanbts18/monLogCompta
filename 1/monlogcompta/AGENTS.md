# MonLogCompta

Application de bureau JavaFX 21 construite avec Maven et utilisant une base SQLite locale.

## Project Structure

- `src/main/java/fr/monlogcompta/MonLogComptaApp.java` - point d'entrée JavaFX
- `src/main/java/fr/monlogcompta/db/Database.java` - schéma SQLite, données initiales et accès JDBC
- `src/main/java/fr/monlogcompta/model/` - modèles de domaine et de session
- `src/main/java/fr/monlogcompta/ui/` - connexion, navigation et écrans métier
- `src/main/resources/fr/monlogcompta/app.css` - thème JavaFX
- `src/test/java/` - tests JUnit
- `pom.xml` - dépendances, compilation, tests et lancement JavaFX
- `.mise.toml` - versions du JDK et de Maven

## Commands

Installer les outils :

```bash
mise install
```

Compiler et tester :

```bash
mvn test
```

Lancer l'application :

```bash
mvn javafx:run
```

Ne pas lancer ou réintroduire le serveur Vite : ce dépôt n'est plus une application web.
