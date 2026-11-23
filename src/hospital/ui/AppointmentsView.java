package hospital.ui;

import hospital.controller.AppointmentController;
import hospital.dao.DoctorDAO;
import hospital.dao.PatientDAO;
import hospital.model.Appointment;
import hospital.model.Doctor;
import hospital.model.Patient;
import hospital.model.User;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AppointmentsView extends BorderPane {
    private final User user;
    private final boolean canBook;
    private final AppointmentController controller = new AppointmentController();
    private final TableView<Appointment> table = new TableView<>();
    private final ComboBox<Patient> patient = new ComboBox<>();
    private final ComboBox<Doctor> doctor = new ComboBox<>();
    private final DatePicker date = new DatePicker(LocalDate.now().plusDays(1));
    private final ComboBox<LocalTime> time = new ComboBox<>();
    private final TextArea reason = new TextArea();
    private final ComboBox<String> status = new ComboBox<>();
    private final Label patientCondition = new Label();
    private final Label doctorSuggestion = new Label();
    private final Label timeAvailabilityHint = new Label();
    private final List<Doctor> allDoctors = new ArrayList<>();
    private boolean loading;

    public AppointmentsView(User user) {
        this.user = user;
        this.canBook = user.getRole() != User.Role.DOCTOR;
        setPadding(new Insets(26));
        Label title = new Label(user.getRole() == User.Role.DOCTOR ? "Patient visit history" : "Appointments"); title.getStyleClass().add("page-title");
        Label subtitle = new Label(switch (user.getRole()) {
            case ADMIN -> "Book, reschedule, and manage hospital appointments.";
            case PATIENT -> "Book a visit and manage your appointment history.";
            case DOCTOR -> "Patients, visit dates, reasons, and outcomes are recorded here.";
        }); subtitle.getStyleClass().add("muted");
        VBox header = new VBox(5, title, subtitle); header.setPadding(new Insets(0,0,18,0));
        configureTable();
        ValidationFeedback.watch(patient); ValidationFeedback.watch(doctor); ValidationFeedback.watch(date);
        ValidationFeedback.watch(time); ValidationFeedback.watch(reason); ValidationFeedback.watch(status);
        ScrollPane editor = ScrollSupport.vertical(editor());
        HBox body = new HBox(18, table, editor); HBox.setHgrow(table, Priority.ALWAYS);
        editor.setPrefWidth(370); editor.setMinWidth(280); table.setPrefWidth(800);
        setTop(header); setCenter(body);
        doctor.valueProperty().addListener((o, old, selected) -> refreshSlots());
        date.valueProperty().addListener((o, old, selected) -> refreshSlots());
        patient.valueProperty().addListener((o, old, selected) -> { updatePatientCondition(); refreshDoctorSuggestions(); });
        reason.textProperty().addListener((o, old, value) -> refreshDoctorSuggestions());
        reloadReferences(); refresh();
    }

    private void configureTable() {
        column("ID", Appointment::getId, 55);
        if (user.getRole() != User.Role.PATIENT) column("Patient", Appointment::getPatientName, 155);
        if (user.getRole() != User.Role.DOCTOR) column("Doctor", Appointment::getDoctorName, 165);
        column("Date", a -> a.getDate().toString(), 110);
        column("Time", a -> TimeFormat.format(a.getTime()), 105);
        column("Status", Appointment::getStatus, 110);
        column("Reason", Appointment::getReason, 200);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No appointments to show"));
        table.getSelectionModel().selectedItemProperty().addListener((o, old, selected) -> fill(selected));
    }
    private <T> void column(String title, java.util.function.Function<Appointment,T> get, double width) {
        TableColumn<Appointment,T> c = new TableColumn<>(title);
        c.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(get.apply(v.getValue()))); c.setPrefWidth(width); table.getColumns().add(c);
    }

    private VBox editor() {
        VBox box = new VBox(11); box.getStyleClass().add("content-card"); box.setPadding(new Insets(18));
        Label heading = new Label("Appointment details"); heading.getStyleClass().add("section-title");
        patient.setPromptText("Select patient"); doctor.setPromptText("Select a suggested doctor"); time.setPromptText("Available time (AM/PM)");
        time.setDisable(true);
        time.setConverter(new StringConverter<>() {
            @Override public String toString(LocalTime value) { return TimeFormat.format(value); }
            @Override public LocalTime fromString(String value) { return TimeFormat.parse(value); }
        });
        for (ComboBox<?> combo : List.of(patient, doctor, time, status)) combo.setMaxWidth(Double.MAX_VALUE);
        date.setMaxWidth(Double.MAX_VALUE); reason.setPromptText("Describe the patient's problem"); reason.setPrefRowCount(3); reason.setWrapText(true);
        patientCondition.setWrapText(true); patientCondition.getStyleClass().add("muted");
        doctorSuggestion.setWrapText(true); doctorSuggestion.getStyleClass().add("small-muted");
        timeAvailabilityHint.setWrapText(true);
        timeAvailabilityHint.getStyleClass().add("small-muted");
        timeAvailabilityHint.setManaged(false);
        timeAvailabilityHint.setVisible(false);
        if (user.getRole() == User.Role.PATIENT) {
            patient.setDisable(true);
        } else if (user.getRole() == User.Role.DOCTOR) {
            patient.setVisible(false); patient.setManaged(false);
            doctor.setVisible(false); doctor.setManaged(false);
            date.setVisible(false); date.setManaged(false); time.setVisible(false); time.setManaged(false); reason.setVisible(false); reason.setManaged(false);
        }
        Button book = new Button("Book appointment"); book.getStyleClass().add("primary-button"); book.setMaxWidth(Double.MAX_VALUE); book.setOnAction(e -> book());
        Button reschedule = new Button("Reschedule selected"); reschedule.setMaxWidth(Double.MAX_VALUE); reschedule.setOnAction(e -> reschedule());
        Button cancel = new Button("Cancel selected"); cancel.getStyleClass().add("danger-button"); cancel.setMaxWidth(Double.MAX_VALUE); cancel.setOnAction(e -> cancel());
        Button updateStatus = new Button("Update status"); updateStatus.setMaxWidth(Double.MAX_VALUE); updateStatus.setOnAction(e -> updateStatus());
        Button clear = new Button("Clear selection"); clear.setMaxWidth(Double.MAX_VALUE); clear.setOnAction(e -> clear());
        status.getItems().addAll("SCHEDULED", "COMPLETED", "CANCELLED", "NO_SHOW");
        if (user.getRole() == User.Role.PATIENT) status.setVisible(false);
        if (!canBook) {
            status.getItems().setAll("COMPLETED", "CANCELLED", "NO_SHOW");
            box.getChildren().addAll(heading, new Label("Choose a status for the selected scheduled visit."), status, updateStatus, cancel, clear);
        } else {
            box.getChildren().addAll(heading);
            VBox timeControl = new VBox(5, time, timeAvailabilityHint);
            if (user.getRole() == User.Role.ADMIN) box.getChildren().add(field("Patient", patient));
            box.getChildren().addAll(field("Recorded patient condition", patientCondition),
                    field("Patient problem / reason for visit", reason),
                    field("Doctor suggestions", doctorSuggestion), field("Doctor", doctor),
                    field("Date", date), field("Available time", timeControl), book, reschedule, cancel);
            if (user.getRole() == User.Role.ADMIN) box.getChildren().addAll(new Separator(), field("Set status", status), updateStatus);
            box.getChildren().add(clear);
        }
        return box;
    }

    private VBox field(String label, javafx.scene.Node control) { Label l = new Label(label); l.getStyleClass().add("form-label"); return new VBox(5,l,control); }
    private void reloadReferences() {
        try {
            loading = true;
            Integer selectedPatientId = patient.getValue() == null ? null : patient.getValue().getId();
            Integer selectedDoctorId = doctor.getValue() == null ? null : doctor.getValue().getId();
            patient.getItems().clear();
            doctor.getItems().clear();
            if (user.getRole() == User.Role.ADMIN) patient.setItems(FXCollections.observableArrayList(new PatientDAO().getAll()));
            else if (user.getRole() == User.Role.PATIENT && user.getPatientId() != null) {
                Patient own = new PatientDAO().findById(user.getPatientId());
                if (own != null) patient.getItems().setAll(own);
            }
            allDoctors.clear();
            if (user.getRole() != User.Role.DOCTOR) allDoctors.addAll(new DoctorDAO().getAll());
            else if (user.getDoctorId() != null) {
                Doctor own = new DoctorDAO().findById(user.getDoctorId());
                if (own != null) allDoctors.add(own);
            }
            if (selectedPatientId != null) patient.getItems().stream().filter(p -> p.getId() == selectedPatientId).findFirst().ifPresent(patient::setValue);
            updatePatientCondition();
            refreshDoctorSuggestions();
            if (selectedDoctorId != null) doctor.getItems().stream().filter(d -> d.getId() == selectedDoctorId).findFirst().ifPresent(doctor::setValue);
        } catch (SQLException ex) { showError("Patient and doctor lists could not be loaded. Check the database setup."); }
        finally { loading = false; }
        if (user.getRole() == User.Role.DOCTOR && !doctor.getItems().isEmpty()) doctor.setValue(doctor.getItems().get(0));
    }

    private void updatePatientCondition() {
        Patient selected = patient.getValue();
        if (selected == null) {
            patientCondition.setText("Select a patient to view any saved condition.");
        } else if (selected.getDisease() == null || selected.getDisease().isBlank()) {
            patientCondition.setText("No condition is saved on this patient's record.");
        } else {
            patientCondition.setText(selected.getDisease());
        }
    }

    private void refreshDoctorSuggestions() {
        Integer selectedDoctorId = doctor.getValue() == null ? null : doctor.getValue().getId();
        String savedCondition = patient.getValue() == null ? "" : value(patient.getValue().getDisease());
        String problem = (savedCondition + " " + value(reason.getText())).trim().toLowerCase(Locale.ROOT);
        Set<String> targetSpecialties = suggestedSpecialties(problem);
        List<Doctor> suggestions;
        if (problem.isBlank()) {
            suggestions = new ArrayList<>(allDoctors);
            doctorSuggestion.setText("Enter the patient's problem to suggest doctors by specialty.");
        } else {
            suggestions = allDoctors.stream().filter(d -> matchesProblem(d, problem, targetSpecialties)).toList();
            if (suggestions.isEmpty()) {
                suggestions = new ArrayList<>(allDoctors);
                doctorSuggestion.setText("No specialty match found; showing all active doctors.");
            } else {
                doctorSuggestion.setText("Doctors are suggested using the patient's saved condition and problem description.");
            }
            suggestions = new ArrayList<>(suggestions);
        }
        Appointment selectedAppointment = table.getSelectionModel().getSelectedItem();
        if (selectedAppointment != null) {
            Doctor current = allDoctors.stream().filter(d -> d.getId() == selectedAppointment.getDoctorId()).findFirst().orElse(null);
            if (current != null && suggestions.stream().noneMatch(d -> d.getId() == current.getId())) suggestions.add(current);
        }
        doctor.setItems(FXCollections.observableArrayList(suggestions));
        if (selectedDoctorId != null)
            doctor.getItems().stream().filter(d -> d.getId() == selectedDoctorId).findFirst().ifPresent(doctor::setValue);
    }

    private static Set<String> suggestedSpecialties(String problem) {
        Set<String> targets = new LinkedHashSet<>();
        if (hasAny(problem, "heart", "cardiac", "chest", "palpitation", "blood pressure")) targets.add("cardiology");
        if (hasAny(problem, "child", "children", "baby", "infant", "pediatric")) targets.add("pediatrics");
        if (hasAny(problem, "bone", "joint", "fracture", "knee", "back pain", "arthritis", "orthopedic")) targets.add("orthopedics");
        if (hasAny(problem, "fever", "cold", "flu", "cough", "general medicine")) targets.add("general medicine");
        if (hasAny(problem, "headache", "migraine", "nerve", "seizure", "neurology")) targets.add("neurology");
        if (hasAny(problem, "skin", "rash", "acne", "dermatology")) targets.add("dermatology");
        if (hasAny(problem, "eye", "vision", "ophthalmology")) targets.add("ophthalmology");
        if (hasAny(problem, "ear", "nose", "throat", "sinus", "ent")) targets.add("ent");
        if (hasAny(problem, "tooth", "teeth", "dental")) targets.add("dentistry");
        if (hasAny(problem, "stomach", "abdomen", "digestion", "gastro")) targets.add("gastroenterology");
        if (hasAny(problem, "breathing", "asthma", "lung", "pulmonology")) targets.add("pulmonology");
        if (hasAny(problem, "kidney", "urine", "urology")) targets.add("urology");
        if (hasAny(problem, "anxiety", "depression", "mental health", "psychiatry")) targets.add("psychiatry");
        return targets;
    }

    private static boolean matchesProblem(Doctor doctor, String problem, Set<String> targets) {
        String specialty = value(doctor.getSpecialization()).toLowerCase(Locale.ROOT);
        String department = value(doctor.getDepartmentName()).toLowerCase(Locale.ROOT);
        if (problem.contains(specialty) && !specialty.isBlank()) return true;
        if (problem.contains(department) && !department.isBlank()) return true;
        return targets.stream().anyMatch(target -> specialty.contains(target) || department.contains(target));
    }

    private static boolean hasAny(String text, String... values) {
        for (String value : values) if (text.contains(value)) return true;
        return false;
    }

    private static String value(String text) { return text == null ? "" : text; }
    public void refresh() {
        Integer selectedId = table.getSelectionModel().getSelectedItem() == null ? null : table.getSelectionModel().getSelectedItem().getId();
        reloadReferences();
        try {
            table.setItems(FXCollections.observableArrayList(controller.visibleAppointments(user)));
            if (selectedId != null) table.getItems().stream().filter(a -> a.getId() == selectedId).findFirst().ifPresent(table.getSelectionModel()::select);
        }
        catch (SQLException | IllegalArgumentException ex) { showError("Appointments could not be loaded. Check that this account is linked to a profile."); }
    }
    private void refreshSlots() {
        if (loading || !canBook) return;
        if (doctor.getValue() == null || date.getValue() == null) {
            time.getItems().clear();
            time.getSelectionModel().clearSelection();
            time.setDisable(true);
            time.setPromptText("Select doctor and date first");
            showTimeAvailabilityHint("Select a doctor and date to view available times.");
            ValidationFeedback.clear(time);
            return;
        }
        try {
            Appointment selected = table.getSelectionModel().getSelectedItem();
            Integer except = selected == null ? null : selected.getId();
            time.setItems(FXCollections.observableArrayList(controller.availableSlots(doctor.getValue().getId(), date.getValue(), except)));
            boolean hasSlots = !time.getItems().isEmpty();
            time.setDisable(!hasSlots);
            time.setPromptText(hasSlots ? "Available time (AM/PM)" : "No available times");
            if (hasSlots) hideTimeAvailabilityHint();
            else {
                time.getSelectionModel().clearSelection();
                showTimeAvailabilityHint("No open times on this date. Try another date, or ask the doctor/admin to add weekly availability.");
                ValidationFeedback.clear(time);
            }
            if (selected != null && selected.getDoctorId() == doctor.getValue().getId()
                    && selected.getDate().equals(date.getValue()) && time.getItems().contains(selected.getTime())) time.setValue(selected.getTime());
            else if (!time.getItems().contains(time.getValue())) time.getSelectionModel().clearSelection();
        } catch (SQLException ex) {
            time.getItems().clear();
            time.getSelectionModel().clearSelection();
            time.setDisable(true);
            time.setPromptText("Times unavailable");
            showTimeAvailabilityHint("Available times could not be loaded. Check the database connection and try again.");
        }
    }
    private void showTimeAvailabilityHint(String text) {
        timeAvailabilityHint.setText(text);
        timeAvailabilityHint.setManaged(true);
        timeAvailabilityHint.setVisible(true);
    }
    private void hideTimeAvailabilityHint() {
        timeAvailabilityHint.setText("");
        timeAvailabilityHint.setManaged(false);
        timeAvailabilityHint.setVisible(false);
    }
    private void fill(Appointment a) {
        if (a == null) { refreshSlots(); return; }
        status.setValue(a.getStatus());
        if (!canBook) return;
        doctor.getItems().stream().filter(d -> d.getId() == a.getDoctorId()).findFirst().ifPresent(doctor::setValue);
        if (user.getRole() == User.Role.ADMIN) patient.getItems().stream().filter(p -> p.getId() == a.getPatientId()).findFirst().ifPresent(patient::setValue);
        date.setValue(a.getDate()); reason.setText(a.getReason());
        refreshSlots(); time.setValue(a.getTime());
    }
    private void book() {
        if (!validateAppointmentForm()) return;
        try {
            controller.book(user, patient.getValue() == null ? null : patient.getValue().getId(),
                    doctor.getValue() == null ? 0 : doctor.getValue().getId(), date.getValue(), time.getValue(), reason.getText());
            refresh(); clear(); notify("Appointment booked.");
        } catch (IllegalArgumentException ex) { markAppointmentIssue(ex.getMessage()); }
        catch (SQLException ex) {
            if ("23000".equals(ex.getSQLState())) ValidationFeedback.mark(time);
            else showError("Appointment could not be saved. Check the database connection.");
        }
    }
    private void reschedule() {
        Appointment selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { ValidationFeedback.mark(table); return; }
        if (!validateAppointmentForm()) return;
        try {
            controller.reschedule(user, selected.getId(), patient.getValue() == null ? null : patient.getValue().getId(),
                    doctor.getValue() == null ? 0 : doctor.getValue().getId(), date.getValue(), time.getValue(), reason.getText());
            refresh(); clear(); notify("Appointment rescheduled.");
        } catch (IllegalArgumentException ex) { markAppointmentIssue(ex.getMessage()); }
        catch (SQLException ex) {
            if ("23000".equals(ex.getSQLState())) ValidationFeedback.mark(time);
            else showError("Appointment could not be rescheduled. Check the database connection.");
        }
    }
    private void cancel() {
        Appointment selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { ValidationFeedback.mark(table); return; }
        if (new Alert(Alert.AlertType.CONFIRMATION, "Cancel appointment #" + selected.getId() + "?", ButtonType.CANCEL, ButtonType.OK)
                .showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try { controller.cancel(user, selected.getId()); refresh(); clear(); notify("Appointment cancelled."); }
        catch (IllegalArgumentException ex) { markAppointmentIssue(ex.getMessage()); }
        catch (SQLException ex) { showError(ex.getMessage() == null ? "Appointment could not be cancelled." : ex.getMessage()); }
    }
    private void updateStatus() {
        Appointment selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { ValidationFeedback.mark(table); return; }
        if (status.getValue() == null) { ValidationFeedback.mark(status); return; }
        try { controller.updateStatus(user, selected.getId(), status.getValue()); refresh(); clear(); notify("Appointment status updated."); }
        catch (IllegalArgumentException ex) { ValidationFeedback.mark(status); }
        catch (SQLException ex) { showError(ex.getMessage() == null ? "Appointment status could not be updated." : ex.getMessage()); }
    }
    private void clear() {
        table.getSelectionModel().clearSelection(); reason.clear(); time.getSelectionModel().clearSelection(); status.getSelectionModel().clearSelection();
        if (user.getRole() == User.Role.ADMIN) patient.getSelectionModel().clearSelection();
        date.setValue(LocalDate.now().plusDays(1)); refreshSlots();
    }
    private boolean validateAppointmentForm() {
        boolean valid = true;
        if (user.getRole() == User.Role.ADMIN && patient.getValue() == null) { ValidationFeedback.mark(patient); valid = false; }
        if (doctor.getValue() == null) { ValidationFeedback.mark(doctor); valid = false; }
        if (date.getValue() == null) { ValidationFeedback.mark(date); valid = false; }
        if (time.getValue() == null) { ValidationFeedback.mark(time); valid = false; }
        if (reason.getText() == null || reason.getText().isBlank()) { ValidationFeedback.mark(reason); valid = false; }
        return valid;
    }
    private void markAppointmentIssue(String text) {
        String issue = text == null ? "" : text.toLowerCase(java.util.Locale.ROOT);
        if (issue.contains("reason")) ValidationFeedback.mark(reason);
        else if (issue.contains("date")) ValidationFeedback.mark(date);
        else if (issue.contains("time") || issue.contains("slot") || issue.contains("availability")) ValidationFeedback.mark(time);
        else if (issue.contains("patient")) ValidationFeedback.mark(patient);
        else if (issue.contains("doctor")) ValidationFeedback.mark(doctor);
        else ValidationFeedback.mark(time);
    }
    private void notify(String text) { ToastNotifications.show(this, text, ToastNotifications.Kind.SUCCESS); }
    private void showError(String text) { ToastNotifications.show(this, text, ToastNotifications.Kind.ERROR); }
}
