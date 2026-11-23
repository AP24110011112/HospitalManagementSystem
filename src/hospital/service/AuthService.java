package hospital.service;
import hospital.dao.UserDAO;
import hospital.model.Doctor;
import hospital.model.User;

import java.sql.SQLException;
public class AuthService {
    private final UserDAO users = new UserDAO();
    public void initialize() throws SQLException { users.ensureDefaultAdmin(); }
    public User login(String username, char[] password) throws SQLException {
        Validation.required(username, "Username");
        if (password == null || password.length == 0) throw new IllegalArgumentException("Password is required.");
        return users.authenticate(username, password);
    }
    public void registerPatient(String username, char[] password, String name, String ageText,
                                String gender, String phone, String address) throws SQLException {
        String login = Validation.required(username, "Username");
        if (!login.matches("[A-Za-z0-9_.-]{3,50}"))
            throw new IllegalArgumentException("Username must be 3–50 characters using letters, numbers, dots, dashes, or underscores.");
        Validation.password(password);
        String patientName = Validation.required(name, "Name");
        int age;
        try { age = Integer.parseInt(ageText.trim()); }
        catch (RuntimeException ex) { throw new IllegalArgumentException("Enter a valid age from 1 to 120."); }
        if (age < 1 || age > 120) throw new IllegalArgumentException("Enter a valid age from 1 to 120.");
        if (gender == null || !java.util.Set.of("Male", "Female", "Other").contains(gender))
            throw new IllegalArgumentException("Choose a gender.");
        users.registerPatient(login, password, patientName, age, gender, Validation.phone(phone), address);
    }
    public void registerDoctor(String username, char[] password, String name, String specialization,
                               Integer departmentId, String phone, String email, String feeText) throws SQLException {
        String login = Validation.required(username, "Doctor username");
        if (!login.matches("[A-Za-z0-9_. -]{3,50}"))
            throw new IllegalArgumentException("Doctor username must be 3–50 characters; spaces are allowed.");
        Validation.password(password);
        String doctorName = Validation.required(name, "Name");
        String specialty = Validation.required(specialization, "Specialization");
        String validEmail = email == null || email.isBlank() ? null : Validation.email(email);
        double fee;
        try { fee = Double.parseDouble(feeText == null || feeText.isBlank() ? "0" : feeText.trim()); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("Enter a valid non-negative consultation fee."); }
        if (!Double.isFinite(fee) || fee < 0)
            throw new IllegalArgumentException("Enter a valid non-negative consultation fee.");
        Doctor doctor = new Doctor(0, doctorName, specialty, Validation.phone(phone), fee,
                departmentId, null, validEmail, null);
        new DoctorService().add(doctor, login, password);
    }
    public void changePassword(User user, char[] currentPassword, char[] newPassword) throws SQLException {
        if (currentPassword == null || currentPassword.length == 0)
            throw new IllegalArgumentException("Enter your current password.");
        Validation.password(newPassword);
        users.changePassword(user.getId(), currentPassword, newPassword);
    }
}
