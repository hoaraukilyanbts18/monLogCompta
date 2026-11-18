package fr.monlogcompta;

/**
 * Point d'entrée du JAR exécutable. Il ne doit pas hériter de Application : sinon le lancement
 * via « java -jar » échoue avec « Les composants JavaFX runtime sont manquants ».
 */
public final class Launcher {
    private Launcher() {
    }

    public static void main(String[] args) {
        MonLogComptaApp.main(args);
    }
}
