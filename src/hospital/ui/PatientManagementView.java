package hospital.ui;

import hospital.model.Patient;
import hospital.service.PatientService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;

import java.sql.SQLException;
import java.util.function.Function;

public class PatientManagementView extends BorderPane {
    private final PatientService service = new PatientService();
    private final TableView<Patient> table = new TableView<>();
    private final TextField search = new TextField();
    private final TextField name = new TextField(), age = new TextField(), phone = new TextField();
    private final TextField email = new TextField(), address = new TextField(), disease = new TextField();
    private final ComboBox<String> gender = new ComboBox<>();

    public PatientManagementView() {
        setPadding(new Insets(26));
        Label title = new Label("Patient management"); title.getStyleClass().add("page-title");
        Label subtitle = new Label("Search and maintain patient records."); subtitle.getStyleClass().add("muted");
        VBox heading = new VBox(5, title, subtitle);
        search.setPromptText("Search name, phone, email, or condition");
        Button searchButton = new Button("Search"); searchButton.setOnAction(e -> refresh());
        Button all = new Button("Show all"); all.setOnAction(e -> { search.clear(); refresh(); });
        HBox searchBar = new HBox(10, search, searchButton, all); HBox.setHgrow(search, Priority.ALWAYS);
        VBox top = new VBox(18, heading, searchBar); top.setPadding(new Insets(0, 0, 18, 0));
        configureTable();
        for (TextField field : new TextField[]{search, name, age, phone, email, address, disease})
            ValidationFeedback.watch(field);
        ValidationFeedback.watch(gender);
        ScrollPane editor = ScrollSupport.vertical(editor());
        HBox body = new HBox(18, table, editor); HBox.setHgrow(table, Priority.ALWAYS);
        table.setPrefWidth(730); editor.setPrefWidth(350); editor.setMinWidth(270);
        setTop(top); setCenter(body);
        refresh();
    }

    private void configureTable() {
        column("ID", Patient::getId, 55); column("Name", Patient::getName, 150); column("Age", Patient::getAge, 55);
        column("Gender", Patient::getGender, 80); column("Phone", Patient::getPhone, 120);
        column("Email", Patient::getEmail, 180); column("Condition", Patient::getDisease, 150);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getSelectionModel().selectedItemProperty().addListener((o, old, p) -> fill(p));
        table.setPlaceholder(new Label("No patients found"));
    }
    private <T> void column(String title, Function<Patient,T> get, double width) {
        TableColumn<Patient,T> c = new TableColumn<>(title);
        c.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(get.apply(v.getValue())));
        c.setPrefWidth(width); table.getColumns().add(c);
    }

    private VBox editor() {
        VBox box = new VBox(11); box.getStyleClass().add("content-card"); box.setPadding(new Insets(18));
        Label heading = new Label("Patient Information"); heading.getStyleClass().add("section-title");
        gender.getItems().addAll("Female", "Male", "Other"); gender.setValue("Female");
        for (TextField field : new TextField[]{name, age, phone, email, address, disease}) field.setMaxWidth(Double.MAX_VALUE);
        name.setPromptText("Full name"); age.setPromptText("Age (1–120)"); phone.setPromptText("Phone number");
        email.setPromptText("Email"); address.setPromptText("Address"); disease.setPromptText("Condition / notes");
        Button add = new Button("Add patient"); add.getStyleClass().add("primary-button"); add.setMaxWidth(Double.MAX_VALUE); add.setOnAction(e -> save(false));
        Button update = new Button("Save changes"); update.setMaxWidth(Double.MAX_VALUE); update.setOnAction(e -> save(true));
        Button delete = new Button("Delete patient"); delete.getStyleClass().add("danger-button"); delete.setMaxWidth(Double.MAX_VALUE); delete.setOnAction(e -> delete());
        Button clear = new Button("Clear form"); clear.setMaxWidth(Double.MAX_VALUE); clear.setOnAction(e -> clear());
        box.getChildren().addAll(heading, label("Full name", name), label("Age", age), label("Gender", gender),
                label("Phone", phone), label("Email", email), label("Address", address), label("Condition", disease),
                add, update, delete, clear);
        return box;
    }
    private VBox label(String text, Control field) {
        Label caption = new Label(text); caption.getStyleClass().add("form-label");
        VBox wrap = new VBox(5, caption, field); return wrap;
    }
    public void refresh() {
        Integer selectedId = table.getSelectionModel().getSelectedItem() == null ? null : table.getSelectionModel().getSelectedItem().getId();
        try {
            table.setItems(FXCollections.observableArrayList(service.search(search.getText())));
            if (selectedId != null) table.getItems().stream().filter(p -> p.getId() == selectedId).findFirst().ifPresent(table.getSelectionModel()::select);
        }
        catch (SQLException ex) { showError("Could not load patient records."); }
    }
    private void fill(Patient p) {
        if (p == null) return;
        name.setText(p.getName()); age.setText(String.valueOf(p.getAge())); gender.setValue(p.getGender());
        phone.setText(value(p.getPhone())); email.setText(value(p.getEmail())); address.setText(value(p.getAddress())); disease.setText(value(p.getDisease()));
    }
    private void save(boolean update) {
        Patient selected = table.getSelectionModel().getSelectedItem();
        if (update && selected == null) { ValidationFeedback.mark(table); return; }
        int patientId = update ? selected.getId() : 0;
        try {
            int years = Integer.parseInt(age.getText().trim());
            Patient patient = new Patient(patientId, name.getText().trim(), years, gender.getValue(), phone.getText().trim(),
                    address.getText().trim(), disease.getText().trim(), email.getText().trim(), selected == null ? null : selected.getUserId());
            service.save(patient); refresh(); clear();
            ToastNotifications.show(this, update ? "Patient details updated." : "Patient added.", ToastNotifications.Kind.SUCCESS);
        } catch (NumberFormatException ex) { ValidationFeedback.mark(age); }
        catch (IllegalArgumentException ex) { markPatientIssue(ex.getMessage()); }
        catch (SQLException ex) {
            if ("23000".equals(ex.getSQLState())) ValidationFeedback.mark(email);
            else showError("The patient could not be saved. Check the database connection.");
        }
    }
    private void delete() {
        Patient selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { ValidationFeedback.mark(table); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + selected.getName() + "? Appointment history prevents deletion while appointments remain.", ButtonType.CANCEL, ButtonType.OK);
        confirm.setHeaderText("Delete patient");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try { service.delete(selected.getId()); refresh(); clear(); ToastNotifications.show(this, "Patient record deleted.", ToastNotifications.Kind.SUCCESS); }
        catch (SQLException ex) { showError("This patient may have appointments. Cancel or complete those records before deleting the patient."); }
    }
    private void clear() {
        table.getSelectionModel().clearSelection(); name.clear(); age.clear(); phone.clear(); email.clear(); address.clear(); disease.clear(); gender.setValue("Female");
    }
    private static String value(String v) { return v == null ? "" : v; }
    private void markPatientIssue(String text) {
        String issue = text == null ? "" : text.toLowerCase(java.util.Locale.ROOT);
        if (issue.contains("age")) ValidationFeedback.mark(age);
        else if (issue.contains("gender")) ValidationFeedback.mark(gender);
        else if (issue.contains("phone")) ValidationFeedback.mark(phone);
        else if (issue.contains("email")) ValidationFeedback.mark(email);
        else ValidationFeedback.mark(name);
    }
    private void showError(String text) { ToastNotifications.show(this, text, ToastNotifications.Kind.ERROR); }
}
