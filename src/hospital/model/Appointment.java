package hospital.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class Appointment {
    private final int id;
    private final int patientId;
    private final int doctorId;
    private final String patientName;
    private final String doctorName;
    private final LocalDate date;
    private final LocalTime time;
    private final String status;
    private final String reason;

    public Appointment(int id, int patientId, int doctorId, String patientName, String doctorName,
                       LocalDate date, LocalTime time, String status, String reason) {
        this.id = id;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.patientName = patientName;
        this.doctorName = doctorName;
        this.date = date;
        this.time = time;
        this.status = status;
        this.reason = reason;
    }
    public int getId() { return id; }
    public int getPatientId() { return patientId; }
    public int getDoctorId() { return doctorId; }
    public String getPatientName() { return patientName; }
    public String getDoctorName() { return doctorName; }
    public LocalDate getDate() { return date; }
    public LocalTime getTime() { return time; }
    public String getStatus() { return status; }
    public String getReason() { return reason; }
}
