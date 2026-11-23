package hospital.model;

public class User {
    public enum Role { ADMIN, DOCTOR, PATIENT }
    private final int id;
    private String username;
    private final Role role;
    private final Integer patientId;
    private final Integer doctorId;

    public User(int id, String username, Role role, Integer patientId, Integer doctorId) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.patientId = patientId;
        this.doctorId = doctorId;
    }
    public int getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Role getRole() { return role; }
    public Integer getPatientId() { return patientId; }
    public Integer getDoctorId() { return doctorId; }
}
