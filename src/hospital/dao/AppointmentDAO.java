package hospital.dao;
import hospital.db.DBConnection;
import hospital.model.Appointment;
import hospital.model.User;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class AppointmentDAO {
    private static final String SELECT = "SELECT a.id,a.patient_id,a.doctor_id,p.name AS patient_name,d.name AS doctor_name,"
            + "a.appt_date,a.appt_time,a.status,a.reason FROM appointments a "
            + "JOIN patients p ON p.id=a.patient_id JOIN doctors d ON d.id=a.doctor_id";

    public void book(int patientId, int doctorId, LocalDate date, LocalTime time, String reason) throws SQLException {
        String sql = "INSERT INTO appointments(patient_id,doctor_id,appt_date,appt_time,reason,status) VALUES(?,?,?,?,?,'SCHEDULED')";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, patientId); ps.setInt(2, doctorId); ps.setDate(3, Date.valueOf(date));
            ps.setTime(4, Time.valueOf(time)); ps.setString(5, reason.trim()); ps.executeUpdate();
        }
    }

    public void reschedule(int id, int patientId, int doctorId, LocalDate date, LocalTime time, String reason) throws SQLException {
        String sql = "UPDATE appointments SET patient_id=?,doctor_id=?,appt_date=?,appt_time=?,reason=?,status='SCHEDULED' "
                + "WHERE id=? AND status<>'CANCELLED'";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, patientId); ps.setInt(2, doctorId); ps.setDate(3, Date.valueOf(date));
            ps.setTime(4, Time.valueOf(time)); ps.setString(5, reason.trim()); ps.setInt(6, id);
            if (ps.executeUpdate() == 0) throw new SQLException("Appointment is no longer scheduled and cannot be rescheduled.");
        }
    }

    public void updateStatus(int id, String status) throws SQLException {
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(
                "UPDATE appointments SET status=? WHERE id=?")) {
            ps.setString(1, status); ps.setInt(2, id); ps.executeUpdate();
        }
    }

    public void cancel(int id) throws SQLException { updateStatus(id, "CANCELLED"); }

    public void cancel(User user, int id) throws SQLException {
        String sql;
        if (user.getRole() == User.Role.PATIENT) sql = "UPDATE appointments SET status='CANCELLED' WHERE id=? AND patient_id=? AND status='SCHEDULED'";
        else if (user.getRole() == User.Role.DOCTOR) sql = "UPDATE appointments SET status='CANCELLED' WHERE id=? AND doctor_id=? AND status='SCHEDULED'";
        else { cancel(id); return; }
        Integer ownerId = user.getRole() == User.Role.PATIENT ? user.getPatientId() : user.getDoctorId();
        if (ownerId == null) throw new SQLException("This account is not linked to a patient or doctor profile.");
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id); ps.setInt(2, ownerId); int updated = ps.executeUpdate();
            if (updated == 0) throw new SQLException("Appointment not found, already closed, or not owned by this account.");
        }
    }

    public void updateStatus(User user, int id, String status) throws SQLException {
        if (user.getRole() == User.Role.ADMIN) { updateStatus(id, status); return; }
        if (user.getRole() != User.Role.DOCTOR || user.getDoctorId() == null)
            throw new SQLException("This role cannot update appointment status.");
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(
                "UPDATE appointments SET status=? WHERE id=? AND doctor_id=? AND status='SCHEDULED'")) {
            ps.setString(1, status); ps.setInt(2, id); ps.setInt(3, user.getDoctorId());
            if (ps.executeUpdate() == 0) throw new SQLException("Appointment not found, already closed, or not assigned to this doctor.");
        }
    }

    public List<Appointment> getAll() throws SQLException { return list(SELECT + " ORDER BY a.appt_date DESC,a.appt_time DESC", null); }
    public List<Appointment> forPatient(int patientId) throws SQLException {
        return list(SELECT + " WHERE a.patient_id=? ORDER BY a.appt_date DESC,a.appt_time DESC", patientId);
    }
    public List<Appointment> forDoctor(int doctorId) throws SQLException {
        return list(SELECT + " WHERE a.doctor_id=? ORDER BY a.appt_date,a.appt_time", doctorId);
    }
    public Appointment findById(int id) throws SQLException {
        List<Appointment> found = list(SELECT + " WHERE a.id=?", id);
        return found.isEmpty() ? null : found.get(0);
    }
    private List<Appointment> list(String sql, Integer id) throws SQLException {
        List<Appointment> result = new ArrayList<>();
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (id != null) ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(new Appointment(rs.getInt("id"), rs.getInt("patient_id"), rs.getInt("doctor_id"),
                        rs.getString("patient_name"), rs.getString("doctor_name"), rs.getDate("appt_date").toLocalDate(),
                        rs.getTime("appt_time").toLocalTime(), rs.getString("status"), rs.getString("reason")));
            }
        }
        return result;
    }

    public int count() throws SQLException { return scalar("SELECT COUNT(*) FROM appointments"); }
    public int countToday() throws SQLException {
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) FROM appointments WHERE appt_date=? AND status<>'CANCELLED'")) {
            ps.setDate(1, Date.valueOf(LocalDate.now()));
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }
    public int countUpcoming() throws SQLException {
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) FROM appointments WHERE status='SCHEDULED' AND (appt_date>? OR (appt_date=? AND appt_time>?))")) {
            LocalDate today = LocalDate.now();
            ps.setDate(1, Date.valueOf(today)); ps.setDate(2, Date.valueOf(today)); ps.setTime(3, Time.valueOf(LocalTime.now()));
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }
    public int countStatus(String status) throws SQLException {
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM appointments WHERE status=?")) {
            ps.setString(1, status); try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }
    private int scalar(String sql) throws SQLException {
        try (Connection con = DBConnection.getConnection(); Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) { rs.next(); return rs.getInt(1); }
    }
}
