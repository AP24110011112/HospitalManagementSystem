package hospital.model;

import java.time.DayOfWeek;
import java.time.LocalTime;

public class Availability {
    private final int id;
    private final int doctorId;
    private final String doctorName;
    private final DayOfWeek dayOfWeek;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final int slotMinutes;
    private final boolean active;
    public Availability(int id, int doctorId, String doctorName, DayOfWeek dayOfWeek,
                        LocalTime startTime, LocalTime endTime, int slotMinutes, boolean active) {
        this.id = id;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.slotMinutes = slotMinutes;
        this.active = active;
    }
    public int getId() { return id; }
    public int getDoctorId() { return doctorId; }
    public String getDoctorName() { return doctorName; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public int getSlotMinutes() { return slotMinutes; }
    public boolean isActive() { return active; }
}
