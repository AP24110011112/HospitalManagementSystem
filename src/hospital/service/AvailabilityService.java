package hospital.service;

import hospital.dao.AvailabilityDAO;
import hospital.model.Availability;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public class AvailabilityService {
    private final AvailabilityDAO dao = new AvailabilityDAO();
    public List<Availability> getAll(Integer doctorId) throws SQLException { return dao.getAll(doctorId); }
    public void save(int id, int doctorId, DayOfWeek day, LocalTime start, LocalTime end, int minutes, boolean active) throws SQLException {
        if (doctorId <= 0) throw new IllegalArgumentException("Choose a doctor.");
        if (day == null || start == null || end == null || !end.isAfter(start))
            throw new IllegalArgumentException("Choose a valid day and an end time after the start time.");
        if (minutes < 5 || minutes > 240) throw new IllegalArgumentException("Slot length must be between 5 and 240 minutes.");
        if (id == 0) dao.add(doctorId, day, start, end, minutes); else dao.update(id, doctorId, day, start, end, minutes, active);
    }
    public void delete(int id) throws SQLException { dao.delete(id); }
}
