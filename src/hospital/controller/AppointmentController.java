package hospital.controller;

import hospital.dao.AppointmentDAO;
import hospital.model.Appointment;
import hospital.model.User;
import hospital.service.AppointmentService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Role-aware event controller for appointment actions in JavaFX screens. */
public class AppointmentController {
    private final AppointmentDAO dao = new AppointmentDAO();
    private final AppointmentService service = new AppointmentService();

    public List<Appointment> visibleAppointments(User user) throws SQLException {
        return switch (user.getRole()) {
            case ADMIN -> dao.getAll();
            case PATIENT -> dao.forPatient(profile(user.getPatientId(), "patient"));
            case DOCTOR -> dao.forDoctor(profile(user.getDoctorId(), "doctor"));
        };
    }

    public List<LocalTime> availableSlots(int doctorId, LocalDate date, Integer exceptId) throws SQLException {
        return service.availableSlots(doctorId, date, exceptId);
    }

    public void book(User user, Integer chosenPatientId, int doctorId, LocalDate date, LocalTime time, String reason) throws SQLException {
        if (user.getRole() == User.Role.DOCTOR) throw new IllegalArgumentException("Doctors cannot book appointments for patients.");
        int patientId = user.getRole() == User.Role.PATIENT
                ? profile(user.getPatientId(), "patient")
                : require(chosenPatientId, "Choose a patient.");
        service.book(patientId, doctorId, date, time, reason);
    }

    public void reschedule(User user, int appointmentId, Integer chosenPatientId, int doctorId,
                           LocalDate date, LocalTime time, String reason) throws SQLException {
        if (user.getRole() == User.Role.DOCTOR) throw new IllegalArgumentException("Doctors cannot reschedule appointments.");
        Appointment existing = ownedAppointment(user, appointmentId);
        int patientId = user.getRole() == User.Role.PATIENT
                ? profile(user.getPatientId(), "patient")
                : require(chosenPatientId, "Choose a patient.");
        if (!"SCHEDULED".equals(existing.getStatus())) throw new IllegalArgumentException("Only scheduled appointments can be rescheduled.");
        service.reschedule(appointmentId, patientId, doctorId, date, time, reason);
    }

    public void cancel(User user, int appointmentId) throws SQLException {
        ownedAppointment(user, appointmentId);
        service.cancel(user, appointmentId);
    }

    public void updateStatus(User user, int appointmentId, String status) throws SQLException {
        ownedAppointment(user, appointmentId);
        service.updateStatus(status, user, appointmentId);
    }

    private Appointment ownedAppointment(User user, int appointmentId) throws SQLException {
        Appointment appointment = dao.findById(appointmentId);
        if (appointment == null) throw new IllegalArgumentException("Appointment no longer exists.");
        if (user.getRole() == User.Role.PATIENT && appointment.getPatientId() != profile(user.getPatientId(), "patient"))
            throw new IllegalArgumentException("You can only change your own appointments.");
        if (user.getRole() == User.Role.DOCTOR && appointment.getDoctorId() != profile(user.getDoctorId(), "doctor"))
            throw new IllegalArgumentException("You can only update appointments assigned to you.");
        return appointment;
    }

    private static int profile(Integer value, String role) {
        if (value == null) throw new IllegalArgumentException("This login is not linked to a " + role + " profile.");
        return value;
    }
    private static int require(Integer value, String message) {
        if (value == null || value <= 0) throw new IllegalArgumentException(message);
        return value;
    }
}
