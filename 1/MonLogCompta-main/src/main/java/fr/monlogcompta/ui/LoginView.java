package fr.monlogcompta.ui;

import fr.monlogcompta.db.Database;
import fr.monlogcompta.model.SessionUser;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.function.Consumer;

public final class LoginView extends HBox {
    public LoginView(Database database, Consumer<SessionUser> onLogin) {
        getStyleClass().add("login-root");

        VBox brand = new VBox();
        brand.getStyleClass().add("login-brand");
        brand.setPadding(new Insets(48, 52, 42, 52));
        brand.setPrefWidth(500);

        HBox logo = new HBox(12, mark(), logoText());
        logo.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label headline = new Label("Simplifiez votre\nfacturation.");
        headline.getStyleClass().add("login-headline");
        Label pitch = new Label("Gérez vos clients, produits et factures en un seul endroit. "
            + "Générez les écritures comptables et exportez vos factures électroniques.");
        pitch.setWrapText(true);
        pitch.getStyleClass().add("login-pitch");

        VBox features = new VBox(12,
            feature("✓", "Calculs TVA et marges automatiques"),
            feature("↓", "Export JSON, XML/UBL 2.1 et FEC"),
            feature("≡", "Journal comptable généré automatiquement"));
        features.setPadding(new Insets(30, 0, 30, 0));

        Label project = new Label("RL   Romain & Lucas\n       Projet BTS SIO");
        project.getStyleClass().add("project-badge");
        brand.getChildren().addAll(logo, spacer, headline, pitch, features, project);

        VBox form = new VBox(16);
        form.setMaxWidth(390);
        Label title = new Label("Connexion");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Accédez à votre espace sécurisé.");
        subtitle.getStyleClass().add("muted");

        TextField email = new TextField();
        email.setPromptText("Adresse e-mail");
        PasswordField password = new PasswordField();
        password.setPromptText("Mot de passe");
        Label error = new Label();
        error.getStyleClass().add("error-message");
        error.setVisible(false);
        error.setManaged(false);

        Button submit = new Button("Se connecter");
        submit.getStyleClass().add("primary-button");
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.setDefaultButton(true);

        Runnable authenticate = () -> {
            error.setVisible(false);
            error.setManaged(false);
            if (email.getText().isBlank() || password.getText().isBlank()) {
                showError(error, "Veuillez remplir tous les champs.");
                return;
            }
            database.authenticate(email.getText(), password.getText()).ifPresentOrElse(onLogin,
                () -> showError(error, "Adresse e-mail ou mot de passe incorrect."));
        };
        submit.setOnAction(event -> authenticate.run());
        password.setOnAction(event -> authenticate.run());

        VBox demo = new VBox(8);
        demo.getStyleClass().add("demo-card");
        Label demoTitle = new Label("COMPTES DE DÉMONSTRATION");
        demoTitle.getStyleClass().add("eyebrow");
        Button admin = new Button("Administrateur   admin@monlogcompta.fr / bts2024");
        Button client = new Button("Client   client@dupont-boulangerie.fr / client2024");
        admin.getStyleClass().add("demo-button");
        client.getStyleClass().add("demo-button");
        admin.setOnAction(event -> fill(email, password, "admin@monlogcompta.fr", "bts2024"));
        client.setOnAction(event -> fill(email, password, "client@dupont-boulangerie.fr", "client2024"));
        demo.getChildren().addAll(demoTitle, admin, client);

        form.getChildren().addAll(title, subtitle, new Separator(), email, password, error, submit, demo);
        StackPane formArea = new StackPane(form);
        formArea.getStyleClass().add("login-form-area");
        HBox.setHgrow(formArea, Priority.ALWAYS);
        getChildren().addAll(brand, formArea);
    }

    private static VBox logoText() {
        Label name = new Label("MonLogCompta");
        name.getStyleClass().add("logo-name");
        Label detail = new Label("Gestion commerciale & comptable");
        detail.getStyleClass().add("logo-detail");
        return new VBox(1, name, detail);
    }

    private static StackPane mark() {
        Label symbol = new Label("M");
        symbol.getStyleClass().add("logo-symbol");
        StackPane pane = new StackPane(symbol);
        pane.getStyleClass().add("logo-mark");
        return pane;
    }

    private static HBox feature(String icon, String text) {
        Label symbol = new Label(icon);
        symbol.getStyleClass().add("feature-icon");
        Label label = new Label(text);
        label.getStyleClass().add("feature-text");
        HBox row = new HBox(12, symbol, label);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static void fill(TextField email, PasswordField password, String login, String secret) {
        email.setText(login);
        password.setText(secret);
    }

    private static void showError(Label label, String message) {
        label.setText(message);
        label.setManaged(true);
        label.setVisible(true);
    }
}
