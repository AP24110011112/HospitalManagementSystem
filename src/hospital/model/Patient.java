package hospital.model;

public class Patient extends Person {
    private int age;
    private String gender, address, disease;
    private String email;
    private Integer userId;

    public Patient(int id, String name, int age, String gender, String phone, String address, String disease) {
        this(id, name, age, gender, phone, address, disease, null, null);
    }

    public Patient(int id, String name, int age, String gender, String phone, String address,
                   String disease, String email, Integer userId) {
        super(id, name, phone);
        this.age = age;
        this.gender = gender;
        this.address = address;
        this.disease = disease;
        this.email = email;
        this.userId = userId;
    }
    public int getAge() { return age; }
    public String getGender() { return gender; }
    public String getAddress() { return address; }
    public String getDisease() { return disease; }
    public String getEmail() { return email; }
    public Integer getUserId() { return userId; }
    @Override public String toString() { return name + " (#" + id + ")"; }
}
