package hospital.ui;

import hospital.dao.AppointmentDAO;
import hospital.model.Appointment;
import hospital.model.User;
import hospital.service.DashboardService;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.geometry.Insets;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class DashboardView extends VBox {
    private final User user;
    private final GridPane cards = new GridPane();
    private final VBox statusBreakdown = new VBox(13);
    private final VBox statusCard = new VBox(14);
    private final Label error = new Label();

    public DashboardView(User user) {
        this.user = user;
        setSpacing(22); setPadding(new Insets(28));
        Label title = new Label("Dashboard"); title.getStyleClass().add("page-title");
        Label subtitle = new Label("A quick view of care activity."); subtitle.getStyleClass().add("muted");
        cards.setHgap(16); cards.setVgap(16); cards.getStyleClass().add("stat-grid");
        Label statusTitle = new Label("Appointments by status"); statusTitle.getStyleClass().add("section-title");
        statusCard.getChildren().addAll(statusTitle, statusBreakdown);
        statusCard.getStyleClass().add("content-card"); statusCard.setPadding(new Insets(20));
        error.getStyleClass().add("error-message");
        getChildren().addAll(title, subtitle, cards, statusCard, error);
        refresh();
    }

    public void refresh() {
        cards.getChildren().clear(); statusBreakdown.getChildren().clear(); error.setText("");
        try {
            if (user.getRole() == User.Role.ADMIN) {
                DashboardService.Stats stats = new DashboardService().load();
                addCard("Patients", stats.patients(), "Total patient records");
                addCard("Doctors", stats.doctors(), "Active care providers");
                addCard("Appointments", stats.appointments(), "All time");
                addCard("Today", stats.today(), "Appointments scheduled");
                addCard("Upcoming", stats.upcoming(), "Still scheduled");
                addCard("Completed", stats.completed(), "Closed as complete");
                addCard("Cancelled", stats.cancelled(), "Cancelled visits");
                addCard("No-show", stats.noShow(), "Missed appointments");
                setChart(stats.appointments(), stats.completed(), stats.cancelled(), stats.noShow());
            } else {
                List<Appointment> list = user.getRole() == User.Role.PATIENT
                        ? new AppointmentDAO().forPatient(requiredProfile(user.getPatientId()))
                        : new AppointmentDAO().forDoctor(requiredProfile(user.getDoctorId()));
                long today = list.stream().filter(a -> a.getDate().equals(LocalDate.now()) && !a.getStatus().equals("CANCELLED")).count();
                long upcoming = list.stream().filter(a -> a.getDate().isAfter(LocalDate.now()) && a.getStatus().equals("SCHEDULED")).count();
                long completed = list.stream().filter(a -> a.getStatus().equals("COMPLETED")).count();
                addCard("My appointments", list.size(), "Your appointment history");
                addCard("Today", today, "Appointments scheduled");
                addCard("Upcoming", upcoming, "Still scheduled");
                addCard("Completed", completed, "Completed visits");
                if (user.getRole() == User.Role.DOCTOR) {
                    long patientsChecked = list.stream().filter(a -> a.getStatus().equals("COMPLETED"))
                            .map(Appointment::getPatientId).distinct().count();
                    addCard("Patients checked", patientsChecked, "Distinct patients with completed visits");
                } else {
                    long doctorsVisited = list.stream().filter(a -> a.getStatus().equals("COMPLETED"))
                            .map(Appointment::getDoctorId).distinct().count();
                    addCard("Doctors visited", doctorsVisited, "Distinct doctors seen");
                }
                long cancelled = list.stream().filter(a -> a.getStatus().equals("CANCELLED")).count();
                long noShow = list.stream().filter(a -> a.getStatus().equals("NO_SHOW")).count();
                setChart(list.size(), completed, cancelled, noShow);
            }
        } catch (SQLException | IllegalArgumentException ex) {
            error.setText("Dashboard data could not be loaded. Check the database connection and account profile.");
        }
    }

    private int requiredProfile(Integer id) {
        if (id == null) throw new IllegalArgumentException("This login is not linked to a profile.");
        return id;
    }
    private void addCard(String label, long value, String note) {
        VBox card = new VBox(7);
        card.getStyleClass().add("stat-card");
        Label caption = new Label(label); caption.getStyleClass().add("muted");
        Label number = new Label(String.valueOf(value)); number.getStyleClass().add("stat-number");
        Label foot = new Label(note); foot.getStyleClass().add("small-muted");
        card.getChildren().addAll(caption, number, foot);
        int index = cards.getChildren().size();
        cards.add(card, index % 4, index / 4);
    }
    private void setChart(long total, long completed, long cancelled, long noShow) {
        long scheduled = Math.max(0, total - completed - cancelled - noShow);
        long max = Math.max(1, scheduled + completed + cancelled + noShow);
        addStatusRow("Scheduled", scheduled, max, "status-scheduled");
        addStatusRow("Completed", completed, max, "status-completed");
        addStatusRow("Cancelled", cancelled, max, "status-cancelled");
        addStatusRow("No-show", noShow, max, "status-no-show");
    }

    private void addStatusRow(String label, long value, long max, String style) {
        Label caption = new Label(label); caption.setMinWidth(92); caption.getStyleClass().add("muted");
        ProgressBar bar = new ProgressBar(Math.min(1.0, (double) value / max));
        bar.setMaxWidth(Double.MAX_VALUE); bar.getStyleClass().addAll("status-bar", style);
        HBox.setHgrow(bar, Priority.ALWAYS);
        Label count = new Label(String.valueOf(value)); count.setMinWidth(30); count.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        statusBreakdown.getChildren().add(new HBox(12, caption, bar, count));
    }
}
