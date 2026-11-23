package hospital.ui;

import hospital.dao.DepartmentDAO;
import hospital.model.Department;
import hospital.model.Doctor;
import hospital.model.User;
import hospital.service.DoctorService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;

import java.sql.SQLException;
import java.util.function.Function;

public class DoctorView extends BorderPane {
    private final User user;
    private final boolean admin;
    private final DoctorService service = new DoctorService();
    private final TableView<Doctor> table = new TableView<>();
    private final TextField search = new TextField(), name = new TextField(), specialization = new TextField();
    private final TextField phone = new TextField(), email = new TextField(), fee = new TextField();
    private final TextField login = new TextField();
    private final PasswordVisibilityField password = new PasswordVisibilityField();
    private final ComboBox<Department> department = new ComboBox<>();

    public DoctorView(User user) {
        this.user = user; this.admin = user.getRole() == User.Role.ADMIN;
        setPadding(new Insets(26));
        Label title = new Label(admin ? "Doctor management" : "Find a doctor"); title.getStyleClass().add("page-title");
        Label subtitle = new Label(admin ? "Maintain provider profiles and sign-in accounts." : "Search providers by name, department, or specialty."); subtitle.getStyleClass().add("muted");
        search.setPromptText("Name, specialty, or department");
        Button searchButton = new Button("Search"); searchButton.setOnAction(e -> refresh());
        Button all = new Button("Show all"); all.setOnAction(e -> { search.clear(); refresh(); });
        HBox searchBar = new HBox(10, search, searchButton, all); HBox.setHgrow(search, Priority.ALWAYS);
        VBox header = new VBox(16, new VBox(5, title, subtitle), searchBar); header.setPadding(new Insets(0,0,18,0));
        configureTable();
        HBox body = new HBox(18, table);
        HBox.setHgrow(table, Priority.ALWAYS);
        if (admin) {
            ScrollPane form = ScrollSupport.vertical(editor());
            form.setPrefWidth(360);
            form.setMinWidth(270);
            body.getChildren().add(form);
        }
        setTop(header); setCenter(body); refresh();
    }
    private void configureTable() {
        column("ID", Doctor::getId, 55); column("Doctor", Doctor::getName, 160); column("Specialty", Doctor::getSpecialization, 150);
        column("Department", Doctor::getDepartmentName, 140); column("Phone", Doctor::getPhone, 120);
        column("Email", Doctor::getEmail, 175); column("Fee", Doctor::getFee, 80);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No doctors found"));
        if (admin) table.getSelectionModel().selectedItemProperty().addListener((o, old, d) -> fill(d));
    }
    private <T> void column(String title, Function<Doctor,T> get, double width) {
        TableColumn<Doctor,T> c = new TableColumn<>(title); c.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(get.apply(v.getValue())));
        c.setPrefWidth(width); table.getColumns().add(c);
    }
    private VBox editor() {
        VBox box = new VBox(10); box.getStyleClass().add("content-card"); box.setPadding(new Insets(18));
        Label heading = new Label("Doctor profile"); heading.getStyleClass().add("section-title");
        for (TextField f : new TextField[]{name, specialization, phone, email, fee, login}) {
            f.setMaxWidth(Double.MAX_VALUE); ValidationFeedback.watch(f);
        }
        ValidationFeedback.watch(department); ValidationFeedback.watch(password);
        name.setPromptText("Full name"); specialization.setPromptText("Specialization"); phone.setPromptText("Phone number");
        email.setPromptText("Email"); fee.setPromptText("Consultation fee"); login.setPromptText("Username (spaces allowed)");
        password.setPromptText("At least 8 characters, including a number"); password.setFieldHeight(42);
        try { department.setItems(FXCollections.observableArrayList(new DepartmentDAO().getAll())); }
        catch (SQLException ex) { showError("Departments could not be loaded."); }
        department.setPromptText("Select department"); department.setMaxWidth(Double.MAX_VALUE);
        Button add = new Button("Add doctor & account"); add.getStyleClass().add("primary-button"); add.setMaxWidth(Double.MAX_VALUE); add.setOnAction(e -> save(false));
        Button update = new Button("Save profile changes"); update.setMaxWidth(Double.MAX_VALUE); update.setOnAction(e -> save(true));
        Button deactivate = new Button("Deactivate doctor"); deactivate.getStyleClass().add("danger-button"); deactivate.setMaxWidth(Double.MAX_VALUE); deactivate.setOnAction(e -> delete());
        Button clear = new Button("Clear form"); clear.setMaxWidth(Double.MAX_VALUE); clear.setOnAction(e -> clear());
        Label passwordHint = new Label("At least 8 characters, including a number.");
        passwordHint.getStyleClass().add("small-muted");
        VBox passwordEntry = new VBox(4, password, passwordHint);
        box.getChildren().addAll(heading, field("Name", name), field("Specialization", specialization), field("Department", department),
                field("Phone", phone), field("Email", email), field("Fee", fee), new Separator(),
                new Label("New doctor account"), field("Username", login), field("Password", passwordEntry),
                add, update, deactivate, clear);
        return box;
    }
    private VBox field(String title, javafx.scene.Node control) { Label l = new Label(title); l.getStyleClass().add("form-label"); return new VBox(4, l, control); }
    public void refresh() {
        Integer selectedDoctorId = table.getSelectionModel().getSelectedItem() == null ? null : table.getSelectionModel().getSelectedItem().getId();
        if (admin) {
            Integer selectedDepartmentId = department.getValue() == null ? null : department.getValue().getId();
            try {
                department.setItems(FXCollections.observableArrayList(new DepartmentDAO().getAll()));
                if (selectedDepartmentId != null) department.getItems().stream().filter(d -> d.getId() == selectedDepartmentId).findFirst().ifPresent(department::setValue);
            }
            catch (SQLException ex) { showError("Departments could not be loaded."); }
        }
        try {
            table.setItems(FXCollections.observableArrayList(service.search(search.getText())));
            if (selectedDoctorId != null) table.getItems().stream().filter(d -> d.getId() == selectedDoctorId).findFirst().ifPresent(table.getSelectionModel()::select);
        }
        catch (SQLException ex) { showError("Doctor list could not be loaded. Check the database setup."); }
    }
    private void fill(Doctor d) {
        if (d == null) return;
        name.setText(d.getName()); specialization.setText(d.getSpecialization()); phone.setText(value(d.getPhone()));
        email.setText(value(d.getEmail())); fee.setText(String.valueOf(d.getFee())); login.clear(); password.clear();
        department.getSelectionModel().clearSelection();
        if (d.getDepartmentId() != null) department.getItems().stream().filter(x -> x.getId() == d.getDepartmentId()).findFirst().ifPresent(department::setValue);
    }
    private void save(boolean update) {
        Doctor selected = table.getSelectionModel().getSelectedItem();
        if (update && selected == null) { ValidationFeedback.mark(table); return; }
        try {
            double amount = Double.parseDouble(fee.getText().trim());
            Department dept = department.getValue();
            Doctor doctor = new Doctor(update ? selected.getId() : 0, name.getText().trim(), specialization.getText().trim(),
                    phone.getText().trim(), amount, dept == null ? null : dept.getId(), dept == null ? null : dept.getName(),
                    email.getText().trim(), selected == null ? null : selected.getUserId());
            if (update) service.update(doctor);
            else service.add(doctor, login.getText(), password.getText().toCharArray());
            refresh(); clear();
            ToastNotifications.show(this, update ? "Doctor profile updated." : "Doctor profile and login account created.", ToastNotifications.Kind.SUCCESS);
        } catch (NumberFormatException ex) { ValidationFeedback.mark(fee); }
        catch (IllegalArgumentException ex) { markDoctorIssue(ex.getMessage()); }
        catch (SQLException ex) {
            if ("23000".equals(ex.getSQLState())) {
                ValidationFeedback.mark(login);
                if (email.getText() != null && !email.getText().isBlank()) ValidationFeedback.mark(email);
            } else showError("Doctor could not be saved. Check the database connection.");
        }
    }
    private void delete() {
        Doctor selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { ValidationFeedback.mark(table); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Deactivate " + selected.getName() + "? Existing appointment history will be retained.", ButtonType.CANCEL, ButtonType.OK);
        confirm.setHeaderText("Deactivate doctor");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try { service.delete(selected.getId()); refresh(); clear(); ToastNotifications.show(this, "Doctor deactivated.", ToastNotifications.Kind.SUCCESS); }
        catch (SQLException ex) { showError("Doctor could not be deactivated."); }
    }
    private void clear() { table.getSelectionModel().clearSelection(); name.clear(); specialization.clear(); phone.clear(); email.clear(); fee.clear(); login.clear(); password.clear(); department.getSelectionModel().clearSelection(); }
    private static String value(String v) { return v == null ? "" : v; }
    private void markDoctorIssue(String text) {
        String issue = text == null ? "" : text.toLowerCase(java.util.Locale.ROOT);
        if (issue.contains("username")) ValidationFeedback.mark(login);
        else if (issue.contains("password")) ValidationFeedback.mark(password);
        else if (issue.contains("name")) ValidationFeedback.mark(name);
        else if (issue.contains("specialization")) ValidationFeedback.mark(specialization);
        else if (issue.contains("phone")) ValidationFeedback.mark(phone);
        else if (issue.contains("email")) ValidationFeedback.mark(email);
        else if (issue.contains("fee")) ValidationFeedback.mark(fee);
        else if (issue.contains("department")) ValidationFeedback.mark(department);
        else ValidationFeedback.mark(name);
    }
    private void showError(String text) { ToastNotifications.show(this, text, ToastNotifications.Kind.ERROR); }
}
