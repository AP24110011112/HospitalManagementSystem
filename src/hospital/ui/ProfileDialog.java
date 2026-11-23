package hospital.ui;

import hospital.dao.DepartmentDAO;
import hospital.dao.DoctorDAO;
import hospital.dao.PatientDAO;
import hospital.model.Department;
import hospital.model.Doctor;
import hospital.model.Patient;
import hospital.model.User;
import hospital.service.AuthService;
import hospital.service.ProfileService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;

import java.sql.SQLException;
import java.util.Arrays;

/** Profile editor and password controls opened from the account icon. */
public final class ProfileDialog {
    private final User user;
    private final Node notificationOwner;
    private final Runnable onSaved;
    private final ProfileService profiles = new ProfileService();
    private final Dialog<ButtonType> dialog = new Dialog<>();
    private final TextField username = new TextField();
    private final TextField email = new TextField();
    private final TextField name = new TextField();
    private final TextField age = new TextField();
    private final ComboBox<String> gender = new ComboBox<>();
    private final TextField phone = new TextField();
    private final TextField address = new TextField();
    private final TextField specialization = new TextField();
    private final ComboBox<Department> department = new ComboBox<>();
    private final TextField fee = new TextField();
    private final PasswordVisibilityField currentPassword = new PasswordVisibilityField();
    private final PasswordVisibilityField newPassword = new PasswordVisibilityField();
    private final PasswordVisibilityField confirmPassword = new PasswordVisibilityField();
    private String loadingIssue;
    private boolean profileReady = true;
    private Integer doctorDepartmentId;

    public ProfileDialog(User user, Node notificationOwner, Runnable onSaved) {
        this.user = user;
        this.notificationOwner = notificationOwner;
        this.onSaved = onSaved;
        dialog.setTitle("CarePoint profile");
        dialog.setHeaderText("Review and update your account details");
        dialog.setResizable(true);
        if (notificationOwner != null && notificationOwner.getScene() != null) {
            Window owner = notificationOwner.getScene().getWindow();
            if (owner != null) dialog.initOwner(owner);
        }
        ButtonType save = new ButtonType("Save profile", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(690, 650);
        dialog.getDialogPane().setMinSize(560, 500);
        dialog.getDialogPane().getStyleClass().add("profile-dialog");
        var stylesheet = getClass().getResource("theme.css");
        if (stylesheet != null) dialog.getDialogPane().getStylesheets().add(stylesheet.toExternalForm());

        Node content = content();
        loadProfile();
        dialog.getDialogPane().setContent(ScrollSupport.vertical(content));
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(save);
        saveButton.getStyleClass().add("primary-button");
        saveButton.setDisable(!profileReady);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, this::saveProfile);
        dialog.setOnShown(event -> {
            if (loadingIssue != null) ToastNotifications.show(dialog.getDialogPane(), loadingIssue, ToastNotifications.Kind.ERROR);
        });
    }

    public void showAndWait() { dialog.showAndWait(); }

    private Node content() {
        GridPane account = grid();
        username.setPromptText(user.getRole() == User.Role.DOCTOR ? "Username (spaces allowed)" : "Username");
        email.setPromptText("Email address (optional)");
        addRow(account, 0, "Username", username);
        addRow(account, 1, "Email", email);

        VBox sections = new VBox(14);
        sections.getChildren().add(card("Account", account));
        if (user.getRole() == User.Role.PATIENT) sections.getChildren().add(card("Patient Information", patientFields()));
        else if (user.getRole() == User.Role.DOCTOR) sections.getChildren().add(card("Doctor details", doctorFields()));
        sections.getChildren().add(passwordSection());
        sections.setPadding(new Insets(18, 22, 18, 22));
        return sections;
    }

    private GridPane patientFields() {
        GridPane form = grid();
        name.setPromptText("Full name"); age.setPromptText("Age"); phone.setPromptText("Phone number");
        address.setPromptText("Address"); gender.getItems().setAll("Female", "Male", "Other");
        gender.setPromptText("Select gender");
        addRow(form, 0, "Patient name", name); addRow(form, 1, "Age", age); addRow(form, 2, "Gender", gender);
        addRow(form, 3, "Phone", phone); addRow(form, 4, "Address", address);
        return form;
    }

    private GridPane doctorFields() {
        GridPane form = grid();
        name.setPromptText("Full name"); specialization.setPromptText("Specialty");
        phone.setPromptText("Phone number"); fee.setPromptText("Consultation fee");
        department.setPromptText("Select department (optional)");
        try { department.getItems().setAll(new DepartmentDAO().getAll()); }
        catch (SQLException ex) { loadingIssue = "Departments could not be loaded. Check the database connection."; }
        addRow(form, 0, "Doctor name", name); addRow(form, 1, "Specialty", specialization);
        addRow(form, 2, "Department", department); addRow(form, 3, "Phone", phone); addRow(form, 4, "Fee", fee);
        return form;
    }

    private TitledPane passwordSection() {
        currentPassword.setPromptText("Current password");
        newPassword.setPromptText("At least 8 characters, including a number");
        confirmPassword.setPromptText("Repeat new password");
        currentPassword.setFieldHeight(42); newPassword.setFieldHeight(42); confirmPassword.setFieldHeight(42);
        ValidationFeedback.watch(currentPassword); ValidationFeedback.watch(newPassword); ValidationFeedback.watch(confirmPassword);
        GridPane form = grid();
        addRow(form, 0, "Current password", currentPassword);
        addRow(form, 1, "New password", newPassword);
        Label hint = new Label("At least 8 characters, including a number."); hint.getStyleClass().add("small-muted");
        addRow(form, 2, "Confirm password", new VBox(4, confirmPassword, hint));
        Button change = new Button("Update password"); change.getStyleClass().add("quiet-button");
        change.setOnAction(event -> changePassword());
        VBox body = new VBox(12, form, change); body.setPadding(new Insets(12));
        TitledPane pane = new TitledPane("Change password", body);
        pane.setExpanded(false);
        return pane;
    }

    private GridPane grid() {
        GridPane grid = new GridPane(); grid.setHgap(14); grid.setVgap(12);
        ColumnConstraints labels = new ColumnConstraints(); labels.setMinWidth(132); labels.setPrefWidth(132);
        ColumnConstraints fields = new ColumnConstraints(); fields.setHgrow(Priority.ALWAYS); fields.setFillWidth(true);
        grid.getColumnConstraints().addAll(labels, fields);
        for (Node field : new Node[]{username, email, name, age, gender, phone, address, specialization, department, fee})
            ValidationFeedback.watch(field);
        return grid;
    }

    private VBox card(String title, GridPane form) {
        Label heading = new Label(title); heading.getStyleClass().add("section-title");
        VBox card = new VBox(12, heading, form); card.setPadding(new Insets(16)); card.getStyleClass().add("content-card");
        return card;
    }

    private static void addRow(GridPane grid, int row, String text, Node field) {
        Label label = new Label(text); label.getStyleClass().add("form-label");
        grid.add(label, 0, row); grid.add(field, 1, row);
        if (field instanceof Region region) region.setMaxWidth(Double.MAX_VALUE);
    }

    private void loadProfile() {
        username.setText(user.getUsername());
        try {
            email.setText(value(profiles.email(user.getId())));
            if (user.getRole() == User.Role.PATIENT && user.getPatientId() != null) {
                Patient patient = new PatientDAO().findById(user.getPatientId());
                if (patient == null) { loadingIssue = "Patient profile could not be found."; profileReady = false; return; }
                name.setText(patient.getName()); age.setText(String.valueOf(patient.getAge())); gender.setValue(patient.getGender());
                phone.setText(value(patient.getPhone())); address.setText(value(patient.getAddress())); email.setText(value(patient.getEmail()));
            } else if (user.getRole() == User.Role.DOCTOR && user.getDoctorId() != null) {
                Doctor doctor = new DoctorDAO().findById(user.getDoctorId());
                if (doctor == null) { loadingIssue = "Doctor profile could not be found."; profileReady = false; return; }
                name.setText(doctor.getName()); specialization.setText(doctor.getSpecialization());
                phone.setText(value(doctor.getPhone())); email.setText(value(doctor.getEmail())); fee.setText(String.valueOf(doctor.getFee()));
                doctorDepartmentId = doctor.getDepartmentId();
                if (doctor.getDepartmentId() != null)
                    department.getItems().stream().filter(d -> d.getId() == doctor.getDepartmentId()).findFirst().ifPresent(department::setValue);
            }
        } catch (SQLException ex) {
            loadingIssue = "Profile details could not be loaded. Check the database connection.";
            profileReady = false;
        }
    }

    private void saveProfile(javafx.event.ActionEvent event) {
        try {
            switch (user.getRole()) {
                case PATIENT -> profiles.updatePatient(user, username.getText(), email.getText(), name.getText(), age.getText(),
                        gender.getValue(), phone.getText(), address.getText());
                case DOCTOR -> profiles.updateDoctor(user, username.getText(), email.getText(), name.getText(), specialization.getText(),
                        department.getValue() == null ? doctorDepartmentId : department.getValue().getId(), phone.getText(), fee.getText());
                case ADMIN -> profiles.updateAdministrator(user, username.getText(), email.getText());
            }
            Platform.runLater(() -> {
                onSaved.run();
                ToastNotifications.show(notificationOwner, "Profile updated.", ToastNotifications.Kind.SUCCESS);
            });
        } catch (IllegalArgumentException ex) {
            event.consume(); markProfileIssue(ex.getMessage());
        } catch (SQLException ex) {
            event.consume();
            if ("23000".equals(ex.getSQLState())) {
                ValidationFeedback.mark(username);
                if (email.getText() != null && !email.getText().isBlank()) ValidationFeedback.mark(email);
            } else ToastNotifications.show(dialog.getDialogPane(), "Profile could not be saved. Check the database connection.", ToastNotifications.Kind.ERROR);
        }
    }

    private void changePassword() {
        char[] current = currentPassword.getText().toCharArray();
        char[] next = newPassword.getText().toCharArray();
        try {
            if (!newPassword.getText().equals(confirmPassword.getText())) {
                ValidationFeedback.mark(newPassword); ValidationFeedback.mark(confirmPassword); return;
            }
            new AuthService().changePassword(user, current, next);
            currentPassword.clear(); newPassword.clear(); confirmPassword.clear();
            ToastNotifications.show(dialog.getDialogPane(), "Password updated.", ToastNotifications.Kind.SUCCESS);
        } catch (IllegalArgumentException ex) {
            String issue = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase(java.util.Locale.ROOT);
            if (issue.contains("current password")) ValidationFeedback.mark(currentPassword);
            else ValidationFeedback.mark(newPassword);
        } catch (SQLException ex) {
            ToastNotifications.show(dialog.getDialogPane(), "Password could not be updated. Check the database connection.", ToastNotifications.Kind.ERROR);
        } finally { Arrays.fill(current, '\0'); Arrays.fill(next, '\0'); }
    }

    private void markProfileIssue(String text) {
        String issue = text == null ? "" : text.toLowerCase(java.util.Locale.ROOT);
        if (issue.contains("username")) ValidationFeedback.mark(username);
        else if (issue.contains("email")) ValidationFeedback.mark(email);
        else if (issue.contains("age")) ValidationFeedback.mark(age);
        else if (issue.contains("gender")) ValidationFeedback.mark(gender);
        else if (issue.contains("phone")) ValidationFeedback.mark(phone);
        else if (issue.contains("specialty") || issue.contains("specialization")) ValidationFeedback.mark(specialization);
        else if (issue.contains("fee")) ValidationFeedback.mark(fee);
        else if (issue.contains("name")) ValidationFeedback.mark(name);
        else ToastNotifications.show(dialog.getDialogPane(), "The profile could not be updated.", ToastNotifications.Kind.ERROR);
    }

    private static String value(String value) { return value == null ? "" : value; }
}
