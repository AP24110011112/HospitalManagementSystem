package hospital.ui;

import hospital.dao.DoctorDAO;
import hospital.model.Availability;
import hospital.model.Doctor;
import hospital.model.User;
import hospital.service.AvailabilityService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalTime;

public class AvailabilityView extends BorderPane {
    private final User user;
    private final boolean editable;
    private final AvailabilityService service = new AvailabilityService();
    private final TableView<Availability> table = new TableView<>();
    private final ComboBox<Doctor> doctor = new ComboBox<>();
    private final ComboBox<DayOfWeek> day = new ComboBox<>();
    private final TextField start = new TextField(), end = new TextField(), minutes = new TextField("30");
    private final CheckBox active = new CheckBox("Available for booking");
    private Availability editing;

    public AvailabilityView(User user) {
        this.user = user; this.editable = user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.DOCTOR;
        setPadding(new Insets(26));
        Label title = new Label(editable ? "Doctor availability" : "Availability" ); title.getStyleClass().add("page-title");
        Label subtitle = new Label(editable ? "Set recurring weekly hours and appointment slot length." : "Browse weekly clinic hours before booking."); subtitle.getStyleClass().add("muted");
        VBox head = new VBox(5, title, subtitle); head.setPadding(new Insets(0,0,18,0));
        ValidationFeedback.watch(doctor);
        ValidationFeedback.watch(day);
        ValidationFeedback.watch(start);
        ValidationFeedback.watch(end);
        ValidationFeedback.watch(minutes);
        configureTable();
        HBox body = new HBox(18, table); HBox.setHgrow(table, Priority.ALWAYS);
        if (editable) {
            ScrollPane form = ScrollSupport.vertical(editor());
            form.setPrefWidth(350);
            form.setMinWidth(260);
            body.getChildren().add(form);
        }
        doctor.setPromptText("Filter by doctor (all)"); doctor.setMaxWidth(Double.MAX_VALUE);
        doctor.valueProperty().addListener((o, old, selected) -> refreshTable());
        HBox filter = new HBox(10, new Label("Doctor"), doctor); filter.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        setTop(new VBox(14, head, filter)); setCenter(body);
        loadDoctors(); refreshTable();
    }
    private void configureTable() {
        column("Doctor", Availability::getDoctorName, 180);
        column("Day", a -> titleCase(a.getDayOfWeek().name()), 110);
        column("From", a -> TimeFormat.format(a.getStartTime()), 110);
        column("Until", a -> TimeFormat.format(a.getEndTime()), 110);
        column("Slot (min)", Availability::getSlotMinutes, 90);
        column("Active", a -> a.isActive() ? "Yes" : "No", 80);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No availability has been added yet"));
        if (editable) table.getSelectionModel().selectedItemProperty().addListener((o, old, a) -> fill(a));
    }
    private <T> void column(String label, java.util.function.Function<Availability,T> get, double width) {
        TableColumn<Availability,T> c = new TableColumn<>(label); c.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(get.apply(v.getValue()))); c.setPrefWidth(width); table.getColumns().add(c);
    }
    private VBox editor() {
        VBox form = new VBox(11); form.getStyleClass().add("content-card"); form.setPadding(new Insets(18));
        Label heading = new Label("Weekly hours"); heading.getStyleClass().add("section-title");
        day.getItems().addAll(DayOfWeek.values()); day.setValue(DayOfWeek.MONDAY); day.setMaxWidth(Double.MAX_VALUE);
        start.setPromptText("09:00 AM"); end.setPromptText("05:00 PM"); minutes.setPromptText("30");
        for (TextField f : new TextField[]{start,end,minutes}) f.setMaxWidth(Double.MAX_VALUE);
        active.setSelected(true);
        Button save = new Button("Save availability"); save.getStyleClass().add("primary-button"); save.setMaxWidth(Double.MAX_VALUE); save.setOnAction(e -> save());
        Button delete = new Button("Delete selected"); delete.getStyleClass().add("danger-button"); delete.setMaxWidth(Double.MAX_VALUE); delete.setOnAction(e -> delete());
        Button clear = new Button("Clear form"); clear.setMaxWidth(Double.MAX_VALUE); clear.setOnAction(e -> clear());
        form.getChildren().addAll(heading, field("Doctor", doctor), field("Day", day), field("Start time (AM/PM)", start),
                field("End time (AM/PM)", end), field("Slot length in minutes", minutes), active, save, delete, clear);
        if (user.getRole() == User.Role.DOCTOR) doctor.setDisable(true);
        return form;
    }
    private VBox field(String label, Control control) { Label l = new Label(label); l.getStyleClass().add("form-label"); return new VBox(5,l,control); }
    private void loadDoctors() {
        try {
            Integer selectedId = doctor.getValue() == null ? null : doctor.getValue().getId();
            doctor.getItems().setAll(new DoctorDAO().getAll());
            if (selectedId != null) doctor.getItems().stream().filter(d -> d.getId() == selectedId).findFirst().ifPresent(doctor::setValue);
            if (user.getRole() == User.Role.DOCTOR && user.getDoctorId() != null)
                doctor.getItems().stream().filter(d -> d.getId() == user.getDoctorId()).findFirst().ifPresent(doctor::setValue);
        } catch (SQLException ex) { showError("Doctor list could not be loaded."); }
    }
    public void refresh() {
        loadDoctors();
        refreshTable();
    }
    private void refreshTable() {
        try {
            Integer filterId = doctor.getValue() == null ? null : doctor.getValue().getId();
            if (user.getRole() == User.Role.DOCTOR && user.getDoctorId() != null) filterId = user.getDoctorId();
            table.setItems(FXCollections.observableArrayList(service.getAll(filterId)));
        } catch (SQLException ex) { showError("Availability could not be loaded. Check the database setup."); }
    }
    private void fill(Availability a) {
        if (a == null) return;
        editing = a;
        doctor.getItems().stream().filter(d -> d.getId() == a.getDoctorId()).findFirst().ifPresent(doctor::setValue);
        day.setValue(a.getDayOfWeek()); start.setText(TimeFormat.format(a.getStartTime())); end.setText(TimeFormat.format(a.getEndTime()));
        minutes.setText(String.valueOf(a.getSlotMinutes())); active.setSelected(a.isActive());
    }
    private void save() {
        Integer doctorId = user.getRole() == User.Role.DOCTOR && user.getDoctorId() != null
                ? user.getDoctorId() : doctor.getValue() == null ? null : doctor.getValue().getId();
        if (doctorId == null) ValidationFeedback.mark(doctor);
        if (day.getValue() == null) ValidationFeedback.mark(day);
        LocalTime from;
        try { from = TimeFormat.parse(start.getText()); }
        catch (java.time.format.DateTimeParseException ex) { ValidationFeedback.mark(start); return; }
        LocalTime until;
        try { until = TimeFormat.parse(end.getText()); }
        catch (java.time.format.DateTimeParseException ex) { ValidationFeedback.mark(end); return; }
        if (day.getValue() == null || doctorId == null) return;
        if (!until.isAfter(from)) { ValidationFeedback.mark(start); ValidationFeedback.mark(end); return; }
        int slot;
        try { slot = Integer.parseInt(minutes.getText().trim()); }
        catch (NumberFormatException ex) { ValidationFeedback.mark(minutes); return; }
        if (slot < 5 || slot > 240) { ValidationFeedback.mark(minutes); return; }
        try {
            service.save(editing == null ? 0 : editing.getId(), doctorId, day.getValue(), from, until, slot, active.isSelected());
            refresh(); clear(); ToastNotifications.show(this, "Availability saved.", ToastNotifications.Kind.SUCCESS);
        } catch (IllegalArgumentException ex) {
            ValidationFeedback.mark(start); ValidationFeedback.mark(end);
        } catch (SQLException ex) {
            ValidationFeedback.mark(start); ValidationFeedback.mark(end);
            if (!"23000".equals(ex.getSQLState())) showError("Availability could not be saved. Check the database connection.");
        }
    }
    private void delete() {
        Availability selected = editing != null ? editing : table.getSelectionModel().getSelectedItem();
        if (selected == null) { ValidationFeedback.mark(table); return; }
        try { service.delete(selected.getId()); refresh(); clear(); ToastNotifications.show(this, "Availability removed.", ToastNotifications.Kind.SUCCESS); }
        catch (SQLException ex) { showError("Availability could not be removed."); }
    }
    private void clear() { editing = null; table.getSelectionModel().clearSelection(); start.clear(); end.clear(); minutes.setText("30"); active.setSelected(true); }
    private static String titleCase(String s) { return s.substring(0,1) + s.substring(1).toLowerCase(); }
    private void showError(String text) { ToastNotifications.show(this, text, ToastNotifications.Kind.ERROR); }
}
