package hospital.service;
import hospital.dao.AppointmentDAO;
import hospital.dao.AvailabilityDAO;
import hospital.model.User;

import java.sql.SQLException;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalTime;

public class AppointmentService {
    private final AppointmentDAO appointments = new AppointmentDAO();
    private final AvailabilityDAO availability = new AvailabilityDAO();

    public List<LocalTime> availableSlots(int doctorId, LocalDate date) throws SQLException {
        if (date == null || date.isBefore(LocalDate.now())) return List.of();
        return availability.availableSlots(doctorId, date);
    }
    public List<LocalTime> availableSlots(int doctorId, LocalDate date, Integer exceptAppointmentId) throws SQLException {
        if (date == null || date.isBefore(LocalDate.now())) return List.of();
        return availability.availableSlots(doctorId, date, exceptAppointmentId);
    }

    public void book(int patientId, int doctorId, LocalDate date, LocalTime time, String reason) throws SQLException {
        Validation.appointmentSlot(date, time);
        String note = Validation.required(reason, "Reason");
        checkAvailable(doctorId, date, time);
        try { appointments.book(patientId, doctorId, date, time, note); }
        catch (SQLException ex) { throw slotConflict(ex); }
    }

    public void reschedule(int id, int patientId, int doctorId, LocalDate date, LocalTime time, String reason) throws SQLException {
        Validation.appointmentSlot(date, time);
        String note = Validation.required(reason, "Reason");
        checkAvailable(doctorId, date, time, id);
        try { appointments.reschedule(id, patientId, doctorId, date, time, note); }
        catch (SQLException ex) { throw slotConflict(ex); }
    }

    private void checkAvailable(int doctorId, LocalDate date, LocalTime time) throws SQLException {
        checkAvailable(doctorId, date, time, null);
    }
    private void checkAvailable(int doctorId, LocalDate date, LocalTime time, Integer exceptId) throws SQLException {
        if (!availability.availableSlots(doctorId, date, exceptId).contains(time))
            throw new IllegalArgumentException("That time is outside the doctor's availability or has already been booked.");
    }

    public void cancel(User user, int appointmentId) throws SQLException {
        appointments.cancel(user, appointmentId);
    }

    public void updateStatus(String status, User user, int appointmentId) throws SQLException {
        if (!List.of("SCHEDULED", "COMPLETED", "CANCELLED", "NO_SHOW").contains(status))
            throw new IllegalArgumentException("Choose a valid appointment status.");
        appointments.updateStatus(user, appointmentId, status);
    }

    private SQLException slotConflict(SQLException ex) {
        if ("23000".equals(ex.getSQLState()))
            return new SQLException("That doctor already has an appointment at that date and time. Choose another slot.", ex.getSQLState(), ex);
        return ex;
    }
}
