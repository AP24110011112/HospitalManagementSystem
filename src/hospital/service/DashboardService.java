package hospital.service;

import hospital.dao.AppointmentDAO;
import hospital.dao.DoctorDAO;
import hospital.dao.PatientDAO;
import java.sql.SQLException;

public class DashboardService {
    public Stats load() throws SQLException {
        AppointmentDAO appointments = new AppointmentDAO();
        return new Stats(new PatientDAO().count(), new DoctorDAO().count(), appointments.count(),
                appointments.countToday(), appointments.countUpcoming(), appointments.countStatus("COMPLETED"),
                appointments.countStatus("CANCELLED"), appointments.countStatus("NO_SHOW"));
    }
    public record Stats(int patients, int doctors, int appointments, int today, int upcoming,
                        int completed, int cancelled, int noShow) { }
}
