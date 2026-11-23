package hospital.dao;
import hospital.db.DBConnection;
import hospital.model.Availability;
import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class AvailabilityDAO {
    public List<Availability> getAll(Integer doctorId) throws SQLException {
        List<Availability> result = new ArrayList<>();
        String sql = "SELECT a.id,a.doctor_id,d.name,a.day_of_week,a.start_time,a.end_time,a.slot_minutes,a.active "
                + "FROM doctor_availability a JOIN doctors d ON d.id=a.doctor_id WHERE (? IS NULL OR a.doctor_id=?) "
                + "ORDER BY d.name,a.day_of_week,a.start_time";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (doctorId == null) { ps.setNull(1, Types.INTEGER); ps.setNull(2, Types.INTEGER); }
            else { ps.setInt(1, doctorId); ps.setInt(2, doctorId); }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(new Availability(rs.getInt("id"), rs.getInt("doctor_id"), rs.getString("name"),
                        DayOfWeek.of(rs.getInt("day_of_week")), rs.getTime("start_time").toLocalTime(),
                        rs.getTime("end_time").toLocalTime(), rs.getInt("slot_minutes"), rs.getBoolean("active")));
            }
        }
        return result;
    }

    public void add(int doctorId, DayOfWeek day, LocalTime start, LocalTime end, int slotMinutes) throws SQLException {
        String sql = "INSERT INTO doctor_availability(doctor_id,day_of_week,start_time,end_time,slot_minutes) VALUES(?,?,?,?,?)";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ensureNoOverlap(con, 0, doctorId, day, start, end);
            ps.setInt(1, doctorId); ps.setInt(2, day.getValue()); ps.setTime(3, Time.valueOf(start));
            ps.setTime(4, Time.valueOf(end)); ps.setInt(5, slotMinutes); ps.executeUpdate();
        }
    }

    public void update(int id, int doctorId, DayOfWeek day, LocalTime start, LocalTime end, int slotMinutes, boolean active) throws SQLException {
        String sql = "UPDATE doctor_availability SET doctor_id=?,day_of_week=?,start_time=?,end_time=?,slot_minutes=?,active=? WHERE id=?";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (active) ensureNoOverlap(con, id, doctorId, day, start, end);
            ps.setInt(1, doctorId); ps.setInt(2, day.getValue()); ps.setTime(3, Time.valueOf(start));
            ps.setTime(4, Time.valueOf(end)); ps.setInt(5, slotMinutes); ps.setBoolean(6, active); ps.setInt(7, id); ps.executeUpdate();
        }
    }

    private void ensureNoOverlap(Connection con, int exceptId, int doctorId, DayOfWeek day,
                                 LocalTime start, LocalTime end) throws SQLException {
        String sql = "SELECT COUNT(*) FROM doctor_availability WHERE doctor_id=? AND day_of_week=? AND active=TRUE "
                + "AND start_time<? AND end_time>? AND id<>?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, doctorId); ps.setInt(2, day.getValue()); ps.setTime(3, Time.valueOf(end));
            ps.setTime(4, Time.valueOf(start)); ps.setInt(5, exceptId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); if (rs.getInt(1) > 0)
                throw new SQLException("Availability overlaps an existing active window.", "23000"); }
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement("DELETE FROM doctor_availability WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }

    public List<LocalTime> availableSlots(int doctorId, LocalDate date) throws SQLException {
        return availableSlots(doctorId, date, null);
    }

    public List<LocalTime> availableSlots(int doctorId, LocalDate date, Integer exceptAppointmentId) throws SQLException {
        List<LocalTime> slots = new ArrayList<>();
        String sql = "SELECT a.start_time,a.end_time,a.slot_minutes FROM doctor_availability a "
                + "JOIN doctors d ON d.id=a.doctor_id WHERE a.doctor_id=? AND d.active=TRUE "
                + "AND a.day_of_week=? AND a.active=TRUE ORDER BY a.start_time";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, doctorId); ps.setInt(2, date.getDayOfWeek().getValue());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalTime start = rs.getTime(1).toLocalTime();
                    LocalTime end = rs.getTime(2).toLocalTime();
                    int minutes = rs.getInt(3);
                    for (LocalTime slot = start; !slot.plusMinutes(minutes).isAfter(end); slot = slot.plusMinutes(minutes))
                        slots.add(slot);
                }
            }
        }
        slots.removeAll(bookedSlots(doctorId, date, exceptAppointmentId));
        if (date.equals(LocalDate.now())) slots.removeIf(t -> !t.isAfter(LocalTime.now()));
        return slots;
    }

    private List<LocalTime> bookedSlots(int doctorId, LocalDate date, Integer exceptAppointmentId) throws SQLException {
        List<LocalTime> booked = new ArrayList<>();
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(
                "SELECT appt_time FROM appointments WHERE doctor_id=? AND appt_date=? AND status<>'CANCELLED' "
                        + "AND (? IS NULL OR id<>?)")) {
            ps.setInt(1, doctorId); ps.setDate(2, Date.valueOf(date));
            if (exceptAppointmentId == null) { ps.setNull(3, Types.INTEGER); ps.setNull(4, Types.INTEGER); }
            else { ps.setInt(3, exceptAppointmentId); ps.setInt(4, exceptAppointmentId); }
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) booked.add(rs.getTime(1).toLocalTime()); }
        }
        return booked;
    }
}
