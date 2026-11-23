package hospital.model;

public class Doctor extends Person {
    private String specialization;
    private double fee;
    private Integer departmentId;
    private String departmentName;
    private String email;
    private Integer userId;

    public Doctor(int id, String name, String specialization, String phone, double fee) {
        this(id, name, specialization, phone, fee, null, null, null, null);
    }

    public Doctor(int id, String name, String specialization, String phone, double fee,
                  Integer departmentId, String departmentName, String email, Integer userId) {
        super(id, name, phone);
        this.specialization = specialization;
        this.fee = fee;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.email = email;
        this.userId = userId;
    }
    public String getSpecialization() { return specialization; }
    public double getFee() { return fee; }
    public Integer getDepartmentId() { return departmentId; }
    public String getDepartmentName() { return departmentName; }
    public String getEmail() { return email; }
    public Integer getUserId() { return userId; }
    @Override public String toString() { return name + " — " + specialization; }
}
