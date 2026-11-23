package hospital.service;

import hospital.db.DBConnection;
import hospital.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/** Updates an account and its linked patient/doctor profile atomically. */
public class ProfileService {
    @FunctionalInterface private interface ProfileUpdate { void run(Connection connection) throws SQLException; }

    public String email(int userId) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT email FROM users WHERE id=?")) {
            statement.setInt(1, userId);
            try (ResultSet rows = statement.executeQuery()) { return rows.next() ? rows.getString(1) : null; }
        }
    }

    public void updatePatient(User user, String username, String email, String name, String ageText,
                              String gender, String phone, String address) throws SQLException {
        String login = validUsername(User.Role.PATIENT, username);
        String patientName = Validation.required(name, "Name");
        int age;
        try { age = Integer.parseInt(ageText == null ? "" : ageText.trim()); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("Enter an age from 1 to 120."); }
        if (age < 1 || age > 120) throw new IllegalArgumentException("Enter an age from 1 to 120.");
        if (gender == null || !java.util.Set.of("Male", "Female", "Other").contains(gender))
            throw new IllegalArgumentException("Choose a gender.");
        String validPhone = Validation.phone(phone);
        String validEmail = optionalEmail(email);
        int patientId = requiredProfile(user.getPatientId(), "patient");
        updateAccount(user, login, validEmail, connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE patients SET name=?,age=?,gender=?,phone=?,email=?,address=? WHERE id=? AND user_id=?")) {
                statement.setString(1, patientName); statement.setInt(2, age); statement.setString(3, gender);
                setNullable(statement, 4, validPhone); setNullable(statement, 5, validEmail); setNullable(statement, 6, address);
                statement.setInt(7, patientId); statement.setInt(8, user.getId());
                statement.executeUpdate();
            }
        });
    }

    public void updateDoctor(User user, String username, String email, String name, String specialization,
                             Integer departmentId, String phone, String feeText) throws SQLException {
        String login = validUsername(User.Role.DOCTOR, username);
        String doctorName = Validation.required(name, "Name");
        String specialty = Validation.required(specialization, "Specialization");
        String validPhone = Validation.phone(phone);
        String validEmail = optionalEmail(email);
        double fee;
        try { fee = Double.parseDouble(feeText == null ? "" : feeText.trim()); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("Enter a valid non-negative fee."); }
        if (!Double.isFinite(fee) || fee < 0) throw new IllegalArgumentException("Enter a valid non-negative fee.");
        int doctorId = requiredProfile(user.getDoctorId(), "doctor");
        updateAccount(user, login, validEmail, connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE doctors SET name=?,specialization=?,department_id=?,phone=?,email=?,fee=? WHERE id=? AND user_id=?")) {
                statement.setString(1, doctorName); statement.setString(2, specialty);
                if (departmentId == null) statement.setNull(3, Types.INTEGER); else statement.setInt(3, departmentId);
                setNullable(statement, 4, validPhone); setNullable(statement, 5, validEmail); statement.setDouble(6, fee);
                statement.setInt(7, doctorId); statement.setInt(8, user.getId());
                statement.executeUpdate();
            }
        });
    }

    public void updateAdministrator(User user, String username, String email) throws SQLException {
        String login = validUsername(User.Role.ADMIN, username);
        String validEmail = optionalEmail(email);
        updateAccount(user, login, validEmail, connection -> { });
    }

    private void updateAccount(User user, String username, String email, ProfileUpdate profileUpdate) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE users SET username=?,email=? WHERE id=? AND active=TRUE")) {
                    statement.setString(1, username); setNullable(statement, 2, email); statement.setInt(3, user.getId());
                    statement.executeUpdate();
                }
                profileUpdate.run(connection);
                connection.commit();
                user.setUsername(username);
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private static String validUsername(User.Role role, String value) {
        String username = Validation.required(value, "Username");
        String pattern = role == User.Role.DOCTOR ? "[A-Za-z0-9_. -]{3,50}" : "[A-Za-z0-9_.-]{3,50}";
        if (!username.matches(pattern))
            throw new IllegalArgumentException(role == User.Role.DOCTOR
                    ? "Doctor username must be 3–50 characters; spaces are allowed."
                    : "Username must be 3–50 characters using letters, numbers, dots, dashes, or underscores.");
        return username;
    }

    private static String optionalEmail(String value) {
        return value == null || value.isBlank() ? null : Validation.email(value);
    }

    private static int requiredProfile(Integer id, String type) {
        if (id == null || id <= 0) throw new IllegalArgumentException("This account is not linked to a " + type + " profile.");
        return id;
    }

    private static void setNullable(PreparedStatement statement, int parameter, String value) throws SQLException {
        if (value == null || value.isBlank()) statement.setNull(parameter, Types.VARCHAR);
        else statement.setString(parameter, value.trim());
    }

}
