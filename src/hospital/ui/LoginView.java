package hospital.ui;

import hospital.controller.LoginController;
import hospital.dao.DepartmentDAO;
import hospital.dao.DoctorDAO;
import hospital.model.Department;
import hospital.model.User;
import hospital.service.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.function.Consumer;

public class LoginView extends BorderPane {
    private final LoginController controller;
    private final Consumer<User> onLogin;
    private final Label message = new Label();
    private final TextField username = new TextField();
    private final PasswordVisibilityField password = new PasswordVisibilityField();
    private final ToggleGroup accountTypes = new ToggleGroup();
    private final Hyperlink register = new Hyperlink();
    private final VBox footer = new VBox(6);
    private Boolean doctorAccountsRegistered;

    public LoginView(AuthService auth, Consumer<User> onLogin, String initialMessage) {
        this.controller = new LoginController(auth);
        this.onLogin = onLogin;
        setPadding(new Insets(24));
        setTop(brandBar());

        VBox panel = new VBox(15);
        panel.getStyleClass().add("login-card");
        panel.setMaxWidth(460);
        panel.setPadding(new Insets(32));
        Label eyebrow = new Label("WELCOME BACK");
        eyebrow.getStyleClass().add("eyebrow");
        Label title = new Label("Sign in to CarePoint");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Manage appointments and care in one place.");
        subtitle.getStyleClass().add("muted");

        ToggleButton patientType = roleButton("Patient", User.Role.PATIENT);
        ToggleButton doctorType = roleButton("Doctor", User.Role.DOCTOR);
        ToggleButton adminType = roleButton("Administrator", User.Role.ADMIN);
        HBox roleSelector = new HBox(7, patientType, doctorType, adminType);
        roleSelector.getStyleClass().add("role-selector");
        for (ToggleButton option : new ToggleButton[]{patientType, doctorType, adminType}) {
            option.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(option, Priority.ALWAYS);
        }
        patientType.setSelected(true);

        username.setPromptText("Username");
        username.setPrefHeight(44);
        password.setPromptText("Password");
        password.setFieldHeight(44);
        ValidationFeedback.watch(username);
        ValidationFeedback.watch(password);

        Button signIn = new Button();
        signIn.getStyleClass().add("primary-button");
        signIn.setMaxWidth(Double.MAX_VALUE);
        signIn.setPrefHeight(44);
        signIn.setOnAction(e -> login());
        password.setOnAction(e -> login());

        message.getStyleClass().add("error-message");
        message.setWrapText(true);
        if (initialMessage != null) message.setText(initialMessage);

        register.setOnAction(e -> {
            if (selectedRole() == User.Role.DOCTOR) showDoctorRegistration();
            else showPatientRegistration();
        });
        register.getStyleClass().add("login-register-link");
        register.setWrapText(true);
        register.setMaxWidth(Double.MAX_VALUE);
        footer.setAlignment(Pos.CENTER);
        footer.getChildren().addAll(new Separator(), register);
        accountTypes.selectedToggleProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                message.setText("");
                updateRoleControls(signIn);
            }
        });
        updateRoleControls(signIn);

        panel.getChildren().addAll(eyebrow, title, subtitle, new Label("Sign in as"), roleSelector,
                new Label("Username"), username, new Label("Password"), password, passwordHint(), signIn, message, footer);

        try {
            doctorAccountsRegistered = new DoctorDAO().countActiveAccounts() > 0;
        } catch (SQLException ignored) {
            // Keep the standard registration prompt if account availability cannot be determined.
        }
        updateRegistrationPrompt(selectedRole());

        StackPane center = new StackPane(panel);
        center.getStyleClass().add("login-background");
        center.setMinHeight(0);
        ScrollPane scroll = ScrollSupport.vertical(center);
        scroll.setFitToHeight(true);
        setCenter(scroll);
    }

    private ToggleButton roleButton(String label, User.Role role) {
        ToggleButton button = new ToggleButton(label);
        button.setToggleGroup(accountTypes);
        button.setUserData(role);
        button.getStyleClass().add("role-toggle");
        return button;
    }

    private User.Role selectedRole() {
        return (User.Role) accountTypes.getSelectedToggle().getUserData();
    }

    private void updateRoleControls(Button signIn) {
        User.Role role = selectedRole();
        signIn.setText("Sign in as " + switch (role) {
            case PATIENT -> "Patient";
            case DOCTOR -> "Doctor";
            case ADMIN -> "Administrator";
        });
        boolean canRegister = role != User.Role.ADMIN;
        updateRegistrationPrompt(role);
        register.setVisible(canRegister);
        register.setManaged(canRegister);
        footer.setVisible(canRegister);
        footer.setManaged(canRegister);
    }

    private void updateRegistrationPrompt(User.Role role) {
        if (role == User.Role.DOCTOR && Boolean.FALSE.equals(doctorAccountsRegistered))
            register.setText("No doctor has registered an account yet. Register as a doctor");
        else if (role == User.Role.DOCTOR)
            register.setText("Don't have a doctor account? Register as a doctor");
        else
            register.setText("New to CarePoint? Create a patient account");
    }

    private HBox brandBar() {
        Label mark = new Label("✚");
        mark.getStyleClass().add("brand-mark");
        Label brand = new Label("CarePoint");
        brand.getStyleClass().add("brand-name");
        HBox bar = new HBox(12, mark, brand);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private void login() {
        char[] entered = password.getText().toCharArray();
        try {
            User user = controller.login(username.getText(), entered);
            if (user == null || user.getRole() != selectedRole()) {
                ValidationFeedback.mark(username);
                ValidationFeedback.mark(password);
                return;
            }
            onLogin.accept(user);
        } catch (IllegalArgumentException ex) {
            if (username.getText() == null || username.getText().isBlank()) ValidationFeedback.mark(username);
            if (password.getText() == null || password.getText().isBlank()) ValidationFeedback.mark(password);
        } catch (SQLException ex) {
            message.setText("Could not connect to MySQL. Check the local database configuration.");
        } finally {
            Arrays.fill(entered, '\0');
            password.clear();
        }
    }

    private void showPatientRegistration() {
        Dialog<ButtonType> dialog = registrationDialog("Create patient account", "Register as a patient", 700, 580);
        ButtonType create = new ButtonType("Create patient account", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(create, ButtonType.CANCEL);

        TextField name = input("Full name");
        TextField age = input("Age in years");
        ComboBox<String> gender = new ComboBox<>();
        gender.getItems().addAll("Female", "Male", "Other");
        gender.setPromptText("Select gender");
        TextField phone = input("Phone number");
        TextField address = input("Address (optional)");
        TextField user = input("Username");
        PasswordVisibilityField pass = new PasswordVisibilityField();
        pass.setPromptText("At least 8 characters, including a number");
        pass.setFieldHeight(42);
        ValidationFeedback.watch(name); ValidationFeedback.watch(age); ValidationFeedback.watch(gender);
        ValidationFeedback.watch(phone); ValidationFeedback.watch(address); ValidationFeedback.watch(user); ValidationFeedback.watch(pass);

        GridPane form = registrationForm();
        addRow(form, 0, "Full name", name);
        addRow(form, 1, "Age", age);
        addRow(form, 2, "Gender", gender);
        addRow(form, 3, "Phone", phone);
        addRow(form, 4, "Address", address);
        addRow(form, 5, "Username", user);
        addRow(form, 6, "Password", passwordEntry(pass));
        finishRegistrationDialog(dialog, form);

        Button createButton = (Button) dialog.getDialogPane().lookupButton(create);
        createButton.getStyleClass().add("primary-button");
        createButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            char[] pwd = pass.getText().toCharArray();
            try {
                controller.registerPatient(user.getText(), pwd, name.getText(), age.getText(),
                        gender.getValue(), phone.getText(), address.getText());
                Platform.runLater(() -> ToastNotifications.show(this,
                        "Patient account created. You can sign in now.", ToastNotifications.Kind.SUCCESS));
            } catch (IllegalArgumentException ex) {
                event.consume();
                markPatientRegistrationIssue(ex.getMessage(), user, pass, name, age, gender, phone);
            } catch (SQLException ex) {
                event.consume();
                if ("23000".equals(ex.getSQLState())) ValidationFeedback.mark(user);
                else ToastNotifications.show(dialog.getDialogPane(), patientRegistrationError(ex), ToastNotifications.Kind.ERROR);
            } finally {
                Arrays.fill(pwd, '\0');
            }
        });
        dialog.showAndWait();
    }

    private void showDoctorRegistration() {
        Dialog<ButtonType> dialog = registrationDialog("Register as a doctor", "Create a doctor profile and sign-in account", 700, 620);
        ButtonType create = new ButtonType("Register doctor", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(create, ButtonType.CANCEL);

        TextField name = input("Full name");
        TextField specialization = input("Specialty");
        ComboBox<Department> department = new ComboBox<>();
        department.setPromptText("Select department (optional)");
        try {
            department.getItems().setAll(new DepartmentDAO().getAll());
        } catch (SQLException ex) {
            ToastNotifications.show(this, "Departments could not be loaded. Check the database connection.", ToastNotifications.Kind.ERROR);
            return;
        }
        TextField phone = input("Phone number (optional)");
        TextField email = input("Email (optional)");
        TextField fee = input("Consultation fee");
        fee.setText("0.00");
        TextField user = input("Username (spaces allowed)");
        PasswordVisibilityField pass = new PasswordVisibilityField();
        pass.setPromptText("At least 8 characters, including a number");
        pass.setFieldHeight(42);
        ValidationFeedback.watch(name); ValidationFeedback.watch(specialization); ValidationFeedback.watch(department);
        ValidationFeedback.watch(phone); ValidationFeedback.watch(email); ValidationFeedback.watch(fee);
        ValidationFeedback.watch(user); ValidationFeedback.watch(pass);

        GridPane form = registrationForm();
        addRow(form, 0, "Full name", name);
        addRow(form, 1, "Specialty", specialization);
        addRow(form, 2, "Department", department);
        addRow(form, 3, "Phone", phone);
        addRow(form, 4, "Email", email);
        addRow(form, 5, "Consultation fee", fee);
        addRow(form, 6, "Username", user);
        addRow(form, 7, "Password", passwordEntry(pass));
        finishRegistrationDialog(dialog, form);

        Button createButton = (Button) dialog.getDialogPane().lookupButton(create);
        createButton.getStyleClass().add("primary-button");
        createButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            char[] pwd = pass.getText().toCharArray();
            Department selectedDepartment = department.getValue();
            try {
                controller.registerDoctor(user.getText(), pwd, name.getText(), specialization.getText(),
                        selectedDepartment == null ? null : selectedDepartment.getId(), phone.getText(), email.getText(), fee.getText());
                Platform.runLater(() -> {
                    doctorAccountsRegistered = true;
                    updateRegistrationPrompt(User.Role.DOCTOR);
                    ToastNotifications.show(this,
                            "Doctor account created. Sign in and add weekly availability.", ToastNotifications.Kind.SUCCESS);
                });
            } catch (IllegalArgumentException ex) {
                event.consume();
                markDoctorRegistrationIssue(ex.getMessage(), user, pass, name, specialization, phone, email, fee);
            } catch (SQLException ex) {
                event.consume();
                if ("23000".equals(ex.getSQLState())) {
                    ValidationFeedback.mark(user);
                    if (email.getText() != null && !email.getText().isBlank()) ValidationFeedback.mark(email);
                } else ToastNotifications.show(dialog.getDialogPane(), doctorRegistrationError(ex), ToastNotifications.Kind.ERROR);
            } finally {
                Arrays.fill(pwd, '\0');
            }
        });
        dialog.showAndWait();
    }

    private Dialog<ButtonType> registrationDialog(String title, String header, double width, double height) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setResizable(true);
        if (getScene() != null && getScene().getWindow() != null)
            dialog.initOwner(getScene().getWindow());
        dialog.getDialogPane().setPrefSize(width, height);
        dialog.getDialogPane().setMinSize(Math.min(600, width), Math.min(500, height));
        dialog.getDialogPane().getStyleClass().add("registration-dialog");
        var stylesheet = getClass().getResource("theme.css");
        if (stylesheet != null) dialog.getDialogPane().getStylesheets().add(stylesheet.toExternalForm());
        return dialog;
    }

    private GridPane registrationForm() {
        GridPane form = new GridPane();
        form.setHgap(18);
        form.setVgap(14);
        form.setPadding(new Insets(20, 24, 20, 24));
        form.getStyleClass().add("registration-form");
        ColumnConstraints labels = new ColumnConstraints();
        labels.setMinWidth(128);
        labels.setPrefWidth(128);
        ColumnConstraints fields = new ColumnConstraints();
        fields.setHgrow(Priority.ALWAYS);
        fields.setFillWidth(true);
        form.getColumnConstraints().addAll(labels, fields);
        return form;
    }

    private void finishRegistrationDialog(Dialog<ButtonType> dialog, GridPane form) {
        ScrollPane scroll = ScrollSupport.vertical(form);
        scroll.setPrefViewportHeight(430);
        dialog.getDialogPane().setContent(scroll);
    }

    private static TextField input(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefHeight(42);
        return field;
    }

    private static VBox passwordEntry(PasswordVisibilityField field) {
        return new VBox(4, field, passwordHint());
    }

    private static Label passwordHint() {
        Label hint = new Label("At least 8 characters, including a number.");
        hint.getStyleClass().add("small-muted");
        return hint;
    }

    private static void markPatientRegistrationIssue(String text, TextField username, PasswordVisibilityField password,
                                                      TextField name, TextField age, ComboBox<String> gender, TextField phone) {
        String issue = text == null ? "" : text.toLowerCase(java.util.Locale.ROOT);
        if (issue.contains("username")) ValidationFeedback.mark(username);
        else if (issue.contains("password")) ValidationFeedback.mark(password);
        else if (issue.contains("age")) ValidationFeedback.mark(age);
        else if (issue.contains("gender")) ValidationFeedback.mark(gender);
        else if (issue.contains("phone")) ValidationFeedback.mark(phone);
        else ValidationFeedback.mark(name);
    }

    private static void markDoctorRegistrationIssue(String text, TextField username, PasswordVisibilityField password,
                                                     TextField name, TextField specialization, TextField phone,
                                                     TextField email, TextField fee) {
        String issue = text == null ? "" : text.toLowerCase(java.util.Locale.ROOT);
        if (issue.contains("username")) ValidationFeedback.mark(username);
        else if (issue.contains("password")) ValidationFeedback.mark(password);
        else if (issue.contains("specialization") || issue.contains("specialty")) ValidationFeedback.mark(specialization);
        else if (issue.contains("phone")) ValidationFeedback.mark(phone);
        else if (issue.contains("email")) ValidationFeedback.mark(email);
        else if (issue.contains("fee")) ValidationFeedback.mark(fee);
        else ValidationFeedback.mark(name);
    }

    private static void addRow(GridPane grid, int row, String label, javafx.scene.Node field) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("form-label");
        grid.add(labelNode, 0, row);
        grid.add(field, 1, row);
        if (field instanceof Region region) region.setMaxWidth(Double.MAX_VALUE);
    }

    private static String patientRegistrationError(SQLException ex) {
        if ("23000".equals(ex.getSQLState())) return "That username is already registered.";
        return "Registration could not be saved. Check your database connection.";
    }

    private static String doctorRegistrationError(SQLException ex) {
        if ("23000".equals(ex.getSQLState())) return "That username or email is already in use.";
        return "Doctor registration could not be saved. Check your database connection.";
    }
}
