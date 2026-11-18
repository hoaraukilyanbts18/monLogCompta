package fr.monlogcompta.ui;

import fr.monlogcompta.db.Database;
import fr.monlogcompta.model.SessionUser;
import javafx.beans.property.ReadOnlyObjectWrapper;
import fr.monlogcompta.service.Validation;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.nio.file.Path;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;

public final class MainView extends BorderPane {
    private static final NumberFormat MONEY = NumberFormat.getCurrencyInstance(Locale.FRANCE);
    private final Database database;
    private final SessionUser user;
    private final Runnable onLogout;
    private final StackPane content = new StackPane();
    private final Map<String, Button> navButtons = new LinkedHashMap<>();

    public MainView(Database database, SessionUser user, Runnable onLogout) {
        this.database = database;
        this.user = user;
        this.onLogout = onLogout;
        getStyleClass().add("app-root");
        setLeft(sidebar());
        setCenter(content);
        navigate(home());
    }

    /** Page d'accueil : l'administrateur ne gère que les utilisateurs. */
    private String home() {
        return user.isAdmin() ? "utilisateurs" : "dashboard";
    }

    private Node sidebar() {
        VBox side = new VBox();
        side.getStyleClass().add("sidebar");
        side.setPrefWidth(232);

        HBox brand = new HBox(11, compactMark(), compactLogo());
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.getStyleClass().add("sidebar-brand");
        Label section = new Label("NAVIGATION");
        section.getStyleClass().add("nav-section");

        VBox navigation = new VBox(4);
        if (user.isAdmin()) {
            addNav(navigation, "utilisateurs", "☰", "Utilisateurs");
        } else {
            addNav(navigation, "dashboard", "⌂", "Tableau de bord");
            if (user.isStaff()) {
                addNav(navigation, "clients", "●", "Clients");
                addNav(navigation, "produits", "◆", "Produits");
            }
            addNav(navigation, "factures", "▤", user.isStaff() ? "Factures" : "Mes factures");
            if (user.isStaff()) {
                addNav(navigation, "comptabilite", "▦", "Comptabilité");
                addNav(navigation, "export", "↓", "Export");
            }
            addNav(navigation, "historique", "◷", "Historique");
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Label avatar = new Label(initials(user.displayName()));
        avatar.getStyleClass().add("avatar");
        Label username = new Label(user.displayName());
        username.getStyleClass().add("sidebar-user");
        Label role = new Label(switch (user.role()) {
            case ADMIN -> "Administrateur";
            case UTILISATEUR -> "Utilisateur";
            case CLIENT -> "Espace client";
        });
        role.getStyleClass().add("sidebar-role");
        VBox identity = new VBox(1, username, role);
        HBox profile = new HBox(10, avatar, identity);
        profile.setAlignment(Pos.CENTER_LEFT);
        Button logout = new Button("Déconnexion");
        logout.getStyleClass().add("logout-button");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setOnAction(event -> onLogout.run());
        VBox footer = new VBox(12, profile, logout);
        footer.getStyleClass().add("sidebar-footer");

        VBox.setMargin(section, new Insets(18, 18, 7, 18));
        VBox.setMargin(navigation, new Insets(0, 12, 0, 12));
        side.getChildren().addAll(brand, section, navigation, spacer, footer);
        return side;
    }

    private void addNav(VBox navigation, String id, String icon, String label) {
        Button button = new Button(icon + "   " + label);
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> navigate(id));
        navButtons.put(id, button);
        navigation.getChildren().add(button);
    }

    private void navigate(String id) {
        navButtons.values().forEach(button -> button.getStyleClass().remove("active"));
        Button active = navButtons.get(id);
        if (active == null) {
            navigate(home());
            return;
        }
        active.getStyleClass().add("active");
        Node view = switch (id) {
            case "utilisateurs" -> usersView();
            case "clients" -> clientsView();
            case "produits" -> productsView();
            case "factures" -> invoicesView();
            case "comptabilite" -> accountingView();
            case "export" -> exportView();
            case "historique" -> historyView();
            default -> dashboardView();
        };
        content.getChildren().setAll(view);
    }

    private Node dashboardView() {
        String restriction = user.isStaff() ? "" : " WHERE f.client_id=?";
        Object[] parameters = user.isStaff() ? new Object[]{} : new Object[]{user.clientId()};
        Map<String, Object> totals = database.query("""
            SELECT COALESCE(SUM(total_ttc),0) total,
            COALESCE(SUM(CASE WHEN statut='En attente' THEN total_ttc ELSE 0 END),0) attente,
            SUM(CASE WHEN statut='Payée' THEN 1 ELSE 0 END) payees,
            SUM(CASE WHEN statut='En retard' THEN 1 ELSE 0 END) retard,
            COUNT(*) nombre FROM facture f""" + restriction, parameters).getFirst();

        FlowPane kpis = new FlowPane(14, 14);
        kpis.getChildren().addAll(
            metric("Chiffre d'affaires TTC", money(totals.get("total")), "Cumulé sur la période", "blue"),
            metric("En attente de règlement", money(totals.get("attente")), "À encaisser", "amber"),
            metric("Factures réglées", totals.get("payees") + " / " + totals.get("nombre"), "Ce trimestre", "green"),
            metric("Factures en retard", String.valueOf(totals.get("retard")), "Action requise", "red")
        );

        String recentSql = """
            SELECT f.numero Numéro, c.raison_sociale Client, f.date_emission Date,
                   f.total_ttc "Montant TTC", f.statut Statut
            FROM facture f JOIN client c ON c.id=f.client_id""" + restriction + " ORDER BY f.date_emission DESC LIMIT 5";
        TableView<Map<String, Object>> recent = table(database.query(recentSql, parameters),
            "Numéro", "Client", "Date", "Montant TTC", "Statut");
        recent.setPrefHeight(275);
        return page(user.isStaff() ? "Tableau de bord" : "Bonjour, " + user.displayName(),
            user.isStaff() ? "Période : septembre – octobre 2024" : "Retrouvez vos factures et leurs transmissions.",
            kpis, card("Dernières factures", recent));
    }

    private Node clientsView() {
        List<Map<String, Object>> rows = database.query("""
            SELECT c.code Référence, c.raison_sociale "Raison sociale", c.siret SIRET,
                   c.email "E-mail", COUNT(f.id) Factures, COALESCE(SUM(f.total_ttc),0) "CA TTC"
            FROM client c LEFT JOIN facture f ON f.client_id=c.id GROUP BY c.id ORDER BY c.raison_sociale""");
        TableView<Map<String, Object>> table = table(rows,
            "Référence", "Raison sociale", "SIRET", "E-mail", "Factures", "CA TTC");
        TextField search = new TextField();
        search.setPromptText("Rechercher un client…");
        search.textProperty().addListener((observable, before, value) -> {
            String needle = value.toLowerCase(Locale.ROOT);
            table.setItems(FXCollections.observableArrayList(rows.stream()
                .filter(row -> row.values().stream().anyMatch(item ->
                    String.valueOf(item).toLowerCase(Locale.ROOT).contains(needle))).toList()));
        });
        Button add = actionButton("+ Nouveau client");
        add.setOnAction(event -> newClientDialog());
        HBox tools = new HBox(10, search, add);
        HBox.setHgrow(search, Priority.ALWAYS);
        return page("Clients", rows.size() + " clients enregistrés", tools, card(null, table));
    }

    private Node productsView() {
        List<Map<String, Object>> rows = database.query("""
            SELECT reference Référence, libelle Libellé, prix_ht "Prix HT",
                   prix_achat "Prix achat HT", tva "TVA %", 
                   ROUND(CASE WHEN prix_ht=0 THEN 0 ELSE (prix_ht-prix_achat)*100/prix_ht END,1) "Marge %"
            FROM produit ORDER BY reference""");
        return page("Produits & Prestations", rows.size() + " références au catalogue",
            card(null, table(rows, "Référence", "Libellé", "Prix HT", "Prix achat HT", "TVA %", "Marge %")));
    }

    private Node invoicesView() {
        String restriction = user.isStaff() ? "" : " WHERE f.client_id=?";
        Object[] parameters = user.isStaff() ? new Object[]{} : new Object[]{user.clientId()};
        List<Map<String, Object>> rows = database.query("""
            SELECT f.id ID, f.numero Numéro, c.raison_sociale Client, f.date_emission Date,
                   f.montant_ht "Montant HT", f.tva_montant TVA, f.total_ttc "Total TTC", f.statut Statut
            FROM facture f JOIN client c ON c.id=f.client_id""" + restriction + " ORDER BY f.date_emission DESC", parameters);
        TableView<Map<String, Object>> invoices = table(rows,
            "Numéro", "Client", "Date", "Montant HT", "TVA", "Total TTC", "Statut");
        invoices.getSelectionModel().selectedItemProperty().addListener((observable, previous, selected) -> {
            if (selected != null) showInvoice(selected);
        });
        Label help = new Label("Sélectionnez une facture pour afficher ses lignes et ses totaux.");
        help.getStyleClass().add("muted");
        return page(user.isStaff() ? "Factures" : "Mes factures",
            rows.size() + " facture(s) accessible(s)", help, card(null, invoices));
    }

    private void showInvoice(Map<String, Object> invoice) {
        long id = ((Number) invoice.get("ID")).longValue();
        List<Map<String, Object>> lines = database.query("""
            SELECT libelle Désignation, quantite Quantité, prix_unitaire_ht "P.U. HT",
                   tva "TVA %", total_ht "Total HT" FROM ligne_facture WHERE facture_id=?""", id);
        VBox body = new VBox(16,
            table(lines, "Désignation", "Quantité", "P.U. HT", "TVA %", "Total HT"),
            metric("Total TTC", money(invoice.get("Total TTC")), "TVA : " + money(invoice.get("TVA")), "blue"));
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(String.valueOf(invoice.get("Numéro")));
        dialog.setHeaderText(invoice.get("Client") + " • " + invoice.get("Statut"));
        dialog.getDialogPane().setContent(body);
        dialog.getDialogPane().setPrefSize(760, 470);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private Node accountingView() {
        List<Map<String, Object>> rows = database.query("""
            SELECT date_ecriture Date, compte_numero "N° compte", libelle Libellé,
                   reference Référence, debit Débit, credit Crédit
            FROM ecriture_comptable ORDER BY date_ecriture, id""");
        Map<String, Object> totals = database.query(
            "SELECT SUM(debit) Débit, SUM(credit) Crédit FROM ecriture_comptable").getFirst();
        HBox metrics = new HBox(14,
            metric("Total débit", money(totals.get("Débit")), "Mouvements débiteurs", "blue"),
            metric("Total crédit", money(totals.get("Crédit")), "Mouvements créditeurs", "green"),
            metric("Balance", money(Math.abs(number(totals.get("Débit")) - number(totals.get("Crédit")))),
                "Contrôle de l'équilibre", "amber"));
        metrics.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));
        Button fec = actionButton("Exporter le FEC");
        fec.setOnAction(event -> exportFec(rows));
        return page("Journal comptable", "Écritures générées à la validation des factures",
            metrics, fec, card(null, table(rows, "Date", "N° compte", "Libellé", "Référence", "Débit", "Crédit")));
    }

    private Node exportView() {
        List<Map<String, Object>> invoices = database.query(
            "SELECT id ID, numero Numéro FROM facture ORDER BY date_emission DESC");
        ComboBox<Map<String, Object>> choice = new ComboBox<>(FXCollections.observableArrayList(invoices));
        choice.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Map<String, Object> row) { return row == null ? "" : String.valueOf(row.get("Numéro")); }
            public Map<String, Object> fromString(String value) { return null; }
        });
        choice.getSelectionModel().selectFirst();
        Button json = actionButton("Exporter en JSON");
        Button xml = actionButton("Exporter en XML / UBL");
        json.setOnAction(event -> exportInvoice(choice.getValue(), false));
        xml.setOnAction(event -> exportInvoice(choice.getValue(), true));
        HBox actions = new HBox(12, json, xml);
        VBox panel = new VBox(14, new Label("Facture à exporter"), choice, actions);
        panel.getStyleClass().add("export-panel");
        return page("Export & Interopérabilité",
            "Générez des fichiers structurés pour vos échanges", card("Facturation électronique fictive", panel));
    }

    private Node historyView() {
        String restriction = user.isStaff() ? "" : " WHERE f.client_id=?";
        Object[] parameters = user.isStaff() ? new Object[]{} : new Object[]{user.clientId()};
        List<Map<String, Object>> rows = database.query("""
            SELECT h.id ID, f.numero Facture, h.date_transmission Date, h.statut Statut,
                   h.format Format, c.raison_sociale Client, f.total_ttc Montant
            FROM historique_transmission h JOIN facture f ON f.id=h.facture_id
            JOIN client c ON c.id=f.client_id""" + restriction + " ORDER BY h.date_transmission DESC", parameters);
        return page("Historique des transmissions",
            user.isStaff() ? "Suivi des factures électroniques envoyées" : "Suivi de vos factures électroniques",
            card(null, table(rows, "ID", "Facture", "Date", "Statut", "Format", "Client", "Montant")));
    }

    // ---- Gestion des utilisateurs (administrateur) ----

    private Node usersView() {
        List<Map<String, Object>> rows = database.query("""
            SELECT u.id ID, u.nom_affiche Nom, u.email "E-mail",
                   CASE u.role WHEN 'ADMIN' THEN 'Administrateur' WHEN 'UTILISATEUR' THEN 'Utilisateur'
                        ELSE 'Client' END Rôle,
                   COALESCE(c.raison_sociale, '') Client
            FROM utilisateur u LEFT JOIN client c ON c.id=u.client_id
            ORDER BY u.role, u.nom_affiche""");
        TableView<Map<String, Object>> table = table(rows, "ID", "Nom", "E-mail", "Rôle", "Client");

        Button add = actionButton("+ Nouvel utilisateur");
        Button edit = actionButton("Modifier");
        Button reset = actionButton("Réinitialiser le mot de passe");
        Button delete = actionButton("Supprimer");

        add.setOnAction(event -> newUserDialog());
        edit.setOnAction(event -> withSelection(table, this::editUserDialog));
        reset.setOnAction(event -> withSelection(table, this::resetPasswordDialog));
        delete.setOnAction(event -> withSelection(table, this::deleteUser));

        HBox tools = new HBox(10, add, edit, reset, delete);
        return page("Utilisateurs", rows.size() + " compte(s) — gestion des accès à l'application",
            tools, card(null, table));
    }

    private void withSelection(TableView<Map<String, Object>> table, java.util.function.Consumer<Map<String, Object>> action) {
        Map<String, Object> selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            information("Sélection", "Sélectionnez d'abord un utilisateur dans la liste.");
            return;
        }
        action.accept(selected);
    }

    private void newUserDialog() {
        TextField name = new TextField();
        TextField email = new TextField();
        PasswordField password = new PasswordField();
        ComboBox<String> role = new ComboBox<>(FXCollections.observableArrayList("Utilisateur", "Administrateur"));
        role.getSelectionModel().selectFirst();
        Map<String, Control> fields = new LinkedHashMap<>();
        fields.put("Nom affiché", name);
        fields.put("Adresse e-mail", email);
        fields.put("Mot de passe", password);
        fields.put("Rôle", role);
        boolean created = formDialog("Nouvel utilisateur", "Créer un compte de connexion", fields, () -> {
            if (name.getText().isBlank()) return "Le nom est obligatoire.";
            String problem = Validation.checkEmail(email.getText());
            if (problem == null) problem = Validation.checkPassword(password.getText());
            if (problem != null) return problem;
            try {
                database.createUser(email.getText(), name.getText(), password.getText(),
                    "Administrateur".equals(role.getValue()) ? SessionUser.Role.ADMIN : SessionUser.Role.UTILISATEUR);
                return null;
            } catch (SQLException exception) {
                return "Création impossible : cette adresse e-mail est déjà utilisée.";
            }
        });
        if (created) navigate("utilisateurs");
    }

    private void editUserDialog(Map<String, Object> selected) {
        long id = ((Number) selected.get("ID")).longValue();
        TextField name = new TextField(String.valueOf(selected.get("Nom")));
        TextField email = new TextField(String.valueOf(selected.get("E-mail")));
        Map<String, Control> fields = new LinkedHashMap<>();
        fields.put("Nom affiché", name);
        fields.put("Adresse e-mail", email);
        boolean saved = formDialog("Modifier l'utilisateur", String.valueOf(selected.get("E-mail")), fields, () -> {
            if (name.getText().isBlank()) return "Le nom est obligatoire.";
            String problem = Validation.checkEmail(email.getText());
            if (problem != null) return problem;
            try {
                database.updateUser(id, name.getText(), email.getText());
                return null;
            } catch (SQLException exception) {
                return "Modification impossible : cette adresse e-mail est déjà utilisée.";
            }
        });
        if (saved) navigate("utilisateurs");
    }

    private void resetPasswordDialog(Map<String, Object> selected) {
        long id = ((Number) selected.get("ID")).longValue();
        PasswordField password = new PasswordField();
        Map<String, Control> fields = new LinkedHashMap<>();
        fields.put("Nouveau mot de passe", password);
        boolean saved = formDialog("Réinitialiser le mot de passe", String.valueOf(selected.get("E-mail")), fields, () -> {
            String problem = Validation.checkPassword(password.getText());
            if (problem != null) return problem;
            try {
                database.resetPassword(id, password.getText());
                return null;
            } catch (SQLException exception) {
                return "Impossible de modifier le mot de passe.";
            }
        });
        if (saved) information("Mot de passe modifié", "Le nouveau mot de passe est actif.");
    }

    private void deleteUser(Map<String, Object> selected) {
        long id = ((Number) selected.get("ID")).longValue();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer le compte " + selected.get("E-mail") + " ?", ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText("Confirmer la suppression");
        confirm.showAndWait().filter(button -> button == ButtonType.OK).ifPresent(button -> {
            try {
                database.deleteUser(id, user.id());
                navigate("utilisateurs");
            } catch (SQLException | IllegalStateException exception) {
                error(exception.getMessage());
            }
        });
    }

    // ---- Création d'un client et de son compte (utilisateur) ----

    private void newClientDialog() {
        TextField name = new TextField();
        TextField siret = new TextField();
        siret.setPromptText("14 chiffres");
        TextField address = new TextField();
        TextField email = new TextField();
        PasswordField password = new PasswordField();
        Map<String, Control> fields = new LinkedHashMap<>();
        fields.put("Raison sociale", name);
        fields.put("SIRET", siret);
        fields.put("Adresse", address);
        fields.put("E-mail (identifiant de connexion)", email);
        fields.put("Mot de passe du compte", password);
        boolean created = formDialog("Nouveau client", "Crée la fiche client et son compte de connexion", fields, () -> {
            String problem = Validation.checkClient(name.getText(), siret.getText(), address.getText(),
                email.getText(), password.getText());
            if (problem != null) return problem;
            try {
                database.createClientWithAccount(name.getText(), Validation.normalizeSiret(siret.getText()),
                    address.getText(), email.getText(), password.getText());
                return null;
            } catch (SQLException exception) {
                return "Création impossible : ce SIRET ou cette adresse e-mail existe déjà.";
            }
        });
        if (created) navigate("clients");
    }

    /**
     * Formulaire modal générique. « submit » renvoie un message d'erreur (la fenêtre reste ouverte)
     * ou null quand l'enregistrement a réussi (la fenêtre se ferme). Renvoie true si enregistré.
     */
    private boolean formDialog(String title, String header, Map<String, Control> fields, Supplier<String> submit) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));
        int row = 0;
        for (Map.Entry<String, Control> field : fields.entrySet()) {
            field.getValue().setPrefWidth(300);
            grid.add(new Label(field.getKey()), 0, row);
            grid.add(field.getValue(), 1, row++);
        }
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        boolean[] done = {false};
        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(ActionEvent.ACTION, event -> {
            String problem = submit.get();
            if (problem == null) {
                done[0] = true;
            } else {
                event.consume();
                error(problem);
            }
        });
        dialog.showAndWait();
        return done[0];
    }

    private static void error(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText("Action impossible");
        alert.showAndWait();
    }

    private void exportInvoice(Map<String, Object> selected, boolean xml) {
        if (selected == null) return;
        long id = ((Number) selected.get("ID")).longValue();
        Map<String, Object> invoice = database.query("""
            SELECT f.numero, f.date_emission, c.raison_sociale, c.siret,
                   f.montant_ht, f.tva_montant, f.total_ttc, f.statut
            FROM facture f JOIN client c ON c.id=f.client_id WHERE f.id=?""", id).getFirst();
        String number = String.valueOf(invoice.get("numero"));
        String content;
        String extension;
        if (xml) {
            extension = "xml";
            content = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Invoice xmlns="urn:oasis:names:specification:ubl:schema:xsd:Invoice-2">
                  <ID>%s</ID><IssueDate>%s</IssueDate>
                  <AccountingCustomerParty><Name>%s</Name><CompanyID>%s</CompanyID></AccountingCustomerParty>
                  <TaxExclusiveAmount currencyID="EUR">%s</TaxExclusiveAmount>
                  <TaxAmount currencyID="EUR">%s</TaxAmount>
                  <TaxInclusiveAmount currencyID="EUR">%s</TaxInclusiveAmount>
                </Invoice>
                """.formatted(number, invoice.get("date_emission"), escapeXml(invoice.get("raison_sociale")),
                invoice.get("siret"), invoice.get("montant_ht"), invoice.get("tva_montant"), invoice.get("total_ttc"));
        } else {
            extension = "json";
            content = """
                {
                  "numero": "%s",
                  "date": "%s",
                  "client": {"raisonSociale": "%s", "siret": "%s"},
                  "montantHT": %s,
                  "tva": %s,
                  "totalTTC": %s,
                  "statut": "%s"
                }
                """.formatted(number, invoice.get("date_emission"), escapeJson(invoice.get("raison_sociale")),
                invoice.get("siret"), invoice.get("montant_ht"), invoice.get("tva_montant"),
                invoice.get("total_ttc"), invoice.get("statut"));
        }
        Path path = database.writeExport(number + "." + extension, content);
        information("Export terminé", "Fichier créé :\n" + path);
    }

    private void exportFec(List<Map<String, Object>> rows) {
        StringBuilder fec = new StringBuilder("JournalCode|JournalLib|EcritureDate|CompteNum|EcritureLib|PieceRef|Debit|Credit\n");
        for (Map<String, Object> row : rows) {
            fec.append("VE|Ventes|").append(row.get("Date")).append('|').append(row.get("N° compte")).append('|')
                .append(row.get("Libellé")).append('|').append(row.get("Référence")).append('|')
                .append(row.get("Débit")).append('|').append(row.get("Crédit")).append('\n');
        }
        Path path = database.writeExport("FEC-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".txt", fec.toString());
        information("Export FEC terminé", "Fichier créé :\n" + path);
    }

    private VBox page(String title, String subtitle, Node... nodes) {
        Label heading = new Label(title);
        heading.getStyleClass().add("page-title");
        Label subheading = new Label(subtitle);
        subheading.getStyleClass().add("muted");
        VBox header = new VBox(3, heading, subheading);
        VBox page = new VBox(20);
        page.getStyleClass().add("page");
        page.getChildren().add(header);
        page.getChildren().addAll(nodes);
        for (Node node : nodes) VBox.setVgrow(node, node instanceof VBox ? Priority.ALWAYS : Priority.NEVER);
        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("page-scroll");
        VBox wrapper = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return wrapper;
    }

    private VBox card(String title, Node content) {
        VBox card = new VBox();
        card.getStyleClass().add("card");
        if (title != null) {
            Label label = new Label(title);
            label.getStyleClass().add("card-title");
            VBox.setMargin(label, new Insets(0, 0, 10, 0));
            card.getChildren().add(label);
        }
        card.getChildren().add(content);
        VBox.setVgrow(content, Priority.ALWAYS);
        return card;
    }

    private VBox metric(String label, String value, String detail, String color) {
        Label caption = new Label(label);
        caption.getStyleClass().add("metric-label");
        Label amount = new Label(value);
        amount.getStyleClass().addAll("metric-value", color);
        Label note = new Label(detail);
        note.getStyleClass().add("metric-detail");
        VBox metric = new VBox(7, caption, amount, note);
        metric.getStyleClass().add("metric-card");
        metric.setMinWidth(210);
        return metric;
    }

    @SafeVarargs
    private final TableView<Map<String, Object>> table(List<Map<String, Object>> rows, String... columns) {
        TableView<Map<String, Object>> table = new TableView<>(FXCollections.observableArrayList(rows));
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Aucune donnée disponible"));
        for (String name : columns) {
            TableColumn<Map<String, Object>, Object> column = new TableColumn<>(name);
            column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().get(name)));
            column.setCellFactory(ignored -> new TableCell<>() {
                @Override protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) setText(null);
                    else if (item instanceof Number && (name.contains("Montant") || name.contains("Prix")
                        || name.contains("Total") || name.equals("TVA") || name.equals("Débit")
                        || name.equals("Crédit") || name.equals("CA TTC"))) setText(money(item));
                    else setText(String.valueOf(item));
                    getStyleClass().removeAll("status-paid", "status-waiting", "status-late");
                    if ("Statut".equals(name) && item != null) {
                        String status = item.toString().toLowerCase(Locale.ROOT);
                        getStyleClass().add(status.contains("pay") || status.contains("accept") ? "status-paid"
                            : status.contains("retard") || status.contains("rejet") ? "status-late" : "status-waiting");
                    }
                }
            });
            table.getColumns().add(column);
        }
        table.setPrefHeight(Math.max(240, Math.min(520, rows.size() * 44 + 48)));
        return table;
    }

    private Button actionButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("primary-button");
        return button;
    }

    private static StackPane compactMark() {
        Label label = new Label("M");
        label.getStyleClass().add("logo-symbol");
        StackPane mark = new StackPane(label);
        mark.getStyleClass().add("logo-mark-small");
        return mark;
    }

    private static VBox compactLogo() {
        Label name = new Label("MonLogCompta");
        name.getStyleClass().add("sidebar-logo");
        Label version = new Label("v1.0 — BTS SIO");
        version.getStyleClass().add("sidebar-role");
        return new VBox(name, version);
    }

    private static String initials(String value) {
        return Arrays.stream(value.split("\\s+")).limit(2).map(part -> part.substring(0, 1))
            .reduce("", String::concat).toUpperCase(Locale.ROOT);
    }

    private static double number(Object value) {
        return value == null ? 0 : ((Number) value).doubleValue();
    }

    private static String money(Object value) {
        return MONEY.format(number(value));
    }

    private static String escapeXml(Object value) {
        return String.valueOf(value).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String escapeJson(Object value) {
        return String.valueOf(value).replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void information(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.showAndWait();
    }
}
