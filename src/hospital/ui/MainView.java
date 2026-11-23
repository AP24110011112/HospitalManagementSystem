package hospital.ui;

import hospital.dao.DoctorDAO;
import hospital.dao.PatientDAO;
import hospital.model.Doctor;
import hospital.model.Patient;
import hospital.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

public class MainView extends BorderPane {
    private final User user;
    private final TabPane tabs = new TabPane();
    private final Label identity = new Label();
    private final Label roleBadge = new Label();
    private final Label patientNameValue = new Label();
    private final Label patientAgeValue = new Label();
    private final Label adminNameValue = new Label();

    public MainView(User user, Runnable onLogout) {
        this.user = user;
        setTop(header(onLogout));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        DashboardView dashboard = new DashboardView(user);
        addTab("Overview", dashboard);
        if (user.getRole() == User.Role.ADMIN) {
            addTab("Patients", new PatientManagementView());
            addTab("Doctors", new DoctorView(user));
            addTab("Departments", new DepartmentView());
            addTab("Availability", new AvailabilityView(user));
            addTab("Appointments", new AppointmentsView(user));
        } else if (user.getRole() == User.Role.DOCTOR) {
            addTab("Patient visits", new AppointmentsView(user));
            addTab("My availability", new AvailabilityView(user));
            addTab("Doctors", new DoctorView(user));
        } else {
            addTab("Find a doctor", new DoctorView(user));
            addTab("Availability", new AvailabilityView(user));
            addTab("My appointments", new AppointmentsView(user));
        }
        setCenter(tabs);
    }

    private HBox header(Runnable onLogout) {
        Label mark = new Label("✚"); mark.getStyleClass().add("brand-mark");
        Label brand = new Label("CarePoint"); brand.getStyleClass().add("brand-name");
        Label section = new Label(switch (user.getRole()) {
            case ADMIN -> "Hospital operations";
            case DOCTOR -> "Doctor workspace";
            case PATIENT -> "Patient portal";
        }); section.getStyleClass().add("muted");
        VBox branding = new VBox(2, new HBox(9,mark,brand), section);
        HBox.setHgrow(branding, Priority.ALWAYS);
        identity.getStyleClass().addAll("user-name", "profile-summary");
        identity.setWrapText(true);
        Label patientCaption = new Label("Patient:");
        patientCaption.getStyleClass().add("patient-caption");
        patientNameValue.getStyleClass().add("patient-value");
        Label patientAgeCaption = new Label("Age:");
        patientAgeCaption.getStyleClass().add("patient-caption");
        patientAgeValue.getStyleClass().add("patient-value");
        HBox patientName = new HBox(5, patientCaption, patientNameValue);
        HBox patientAge = new HBox(5, patientAgeCaption, patientAgeValue);
        patientName.setAlignment(Pos.CENTER_LEFT);
        patientAge.setAlignment(Pos.CENTER_LEFT);
        HBox patientInfo = new HBox(14, patientName, patientAge);
        patientInfo.setAlignment(Pos.CENTER_LEFT);
        Label adminCaption = new Label("Admin Name:");
        adminCaption.getStyleClass().add("patient-caption");
        adminNameValue.getStyleClass().add("patient-value");
        HBox adminInfo = new HBox(5, adminCaption, adminNameValue);
        adminInfo.setAlignment(Pos.CENTER_LEFT);
        roleBadge.setText(user.getRole().name()); roleBadge.getStyleClass().add("role-badge");
        Button profile = new Button();
        profile.setGraphic(profileIcon());
        profile.setTooltip(new Tooltip("Profile and account settings"));
        profile.setAccessibleText("Profile and account settings");
        profile.getStyleClass().add("profile-button");
        ContextMenu profileMenu = new ContextMenu();
        MenuItem editProfile = new MenuItem("Profile settings");
        editProfile.setOnAction(e -> new ProfileDialog(user, this, this::refreshIdentity).showAndWait());
        MenuItem signOut = new MenuItem("Sign out");
        signOut.setOnAction(e -> onLogout.run());
        profileMenu.getItems().addAll(editProfile, new SeparatorMenuItem(), signOut);
        profile.setOnAction(e -> profileMenu.show(profile, Side.BOTTOM, 0, 4));
        refreshIdentity();
        HBox actions = new HBox(10);
        if (user.getRole() == User.Role.PATIENT) actions.getChildren().addAll(patientInfo, profile);
        else if (user.getRole() == User.Role.ADMIN) actions.getChildren().addAll(adminInfo, roleBadge, profile);
        else actions.getChildren().addAll(identity, roleBadge, profile);
        actions.setAlignment(Pos.CENTER_RIGHT);
        HBox bar = new HBox(28, branding, actions); bar.setAlignment(Pos.CENTER_LEFT); bar.setPadding(new Insets(16, 26, 16, 26));
        bar.getStyleClass().add("app-header");
        return bar;
    }

    private SVGPath profileIcon() {
        SVGPath icon = new SVGPath();
        icon.setContent("M12 12c2.2 0 4-1.8 4-4s-1.8-4-4-4-4 1.8-4 4 1.8 4 4 4zm0 2c-2.7 0-8 1.4-8 4v2h16v-2c0-2.6-5.3-4-8-4z");
        icon.setFill(Color.web("#33445d"));
        return icon;
    }

    private void refreshIdentity() {
        try {
            if (user.getRole() == User.Role.PATIENT) {
                Patient patient = user.getPatientId() == null ? null : new PatientDAO().findById(user.getPatientId());
                patientNameValue.setText(patient == null ? user.getUsername() : patient.getName());
                patientAgeValue.setText(patient == null ? "—" : String.valueOf(patient.getAge()));
            } else if (user.getRole() == User.Role.DOCTOR && user.getDoctorId() != null) {
                Doctor doctor = new DoctorDAO().findById(user.getDoctorId());
                identity.setText(doctor == null ? "Doctor Name: " + user.getUsername() : "Doctor Name: " + doctor.getName());
            } else if (user.getRole() == User.Role.ADMIN) {
                adminNameValue.setText(user.getUsername());
            } else {
                identity.setText(user.getUsername());
            }
        } catch (java.sql.SQLException ex) {
            if (user.getRole() == User.Role.PATIENT) {
                patientNameValue.setText(user.getUsername());
                patientAgeValue.setText("—");
            } else identity.setText(user.getUsername());
        }
    }
    private void addTab(String label, javafx.scene.Node content) {
        javafx.scene.Node displayed = content instanceof DashboardView ? ScrollSupport.vertical(content) : content;
        Tab tab = new Tab(label, displayed);
        tab.setOnSelectionChanged(event -> {
            if (!tab.isSelected()) return;
            if (content instanceof DashboardView view) view.refresh();
            else if (content instanceof PatientManagementView view) view.refresh();
            else if (content instanceof DoctorView view) view.refresh();
            else if (content instanceof DepartmentView view) view.refresh();
            else if (content instanceof AvailabilityView view) view.refresh();
            else if (content instanceof AppointmentsView view) view.refresh();
        });
        tabs.getTabs().add(tab);
    }

}
