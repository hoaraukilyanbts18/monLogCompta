package fr.monlogcompta;

import fr.monlogcompta.db.Database;
import fr.monlogcompta.model.SessionUser;
import fr.monlogcompta.ui.LoginView;
import fr.monlogcompta.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public final class MonLogComptaApp extends Application {
    private Database database;
    private Stage stage;

    @Override
    public void start(Stage primaryStage) {
        database = new Database();
        stage = primaryStage;
        stage.setTitle("MonLogCompta");
        stage.setMinWidth(1080);
        stage.setMinHeight(700);
        showLogin();
        stage.show();
    }

    private void showLogin() {
        LoginView login = new LoginView(database, this::showWorkspace);
        setScene(new Scene(login, 1180, 760));
    }

    private void showWorkspace(SessionUser user) {
        MainView main = new MainView(database, user, this::showLogin);
        setScene(new Scene(main, 1380, 820));
    }

    private void setScene(Scene scene) {
        scene.getStylesheets().add(Objects.requireNonNull(
            getClass().getResource("/fr/monlogcompta/app.css")).toExternalForm());
        stage.setScene(scene);
        stage.centerOnScreen();
    }

    @Override
    public void stop() {
        if (database != null) database.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
