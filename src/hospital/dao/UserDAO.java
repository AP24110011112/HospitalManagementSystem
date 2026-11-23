package hospital.dao;

import hospital.db.DBConnection;
import hospital.model.User;
import hospital.util.PasswordUtil;

import java.sql.*;

public class UserDAO {
    /** Creates the documented demo admin only when no admin account exists. */
    public void ensureDefaultAdmin() throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement find = con.prepareStatement("SELECT id FROM users WHERE username=?")) {
            find.setString(1, "admin");
            try (ResultSet rs = find.executeQuery()) { if (rs.next()) return; }
            try (PreparedStatement insert = con.prepareStatement(
                    "INSERT INTO users(username,password_hash,password,role) VALUES(?,?,NULL,'ADMIN')")) {
                insert.setString(1, "admin");
                insert.setString(2, PasswordUtil.hash("admin123".toCharArray()));
                insert.executeUpdate();
            }
        }
    }

    public User authenticate(String username, char[] password) throws SQLException {
        String sql = "SELECT id,username,password_hash,password,role FROM users WHERE username=? AND active=TRUE";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                String encoded = rs.getString("password_hash");
                String legacy = rs.getString("password");
                boolean valid = encoded != null && PasswordUtil.verify(password, encoded);
                boolean migrate = !valid && legacy != null && legacy.contentEquals(new String(password));
                if (!valid && !migrate) return null;
                int id = rs.getInt("id");
                String name = rs.getString("username");
                User.Role role = User.Role.valueOf(rs.getString("role").toUpperCase());
                if (migrate) {
                    try (PreparedStatement update = con.prepareStatement(
                            "UPDATE users SET password_hash=?,password=NULL WHERE id=?")) {
                        update.setString(1, PasswordUtil.hash(password));
                        update.setInt(2, id);
                        update.executeUpdate();
                    }
                }
                return loadProfile(con, id, name, role);
            }
        }
    }

    private User loadProfile(Connection con, int id, String username, User.Role role) throws SQLException {
        String sql = "SELECT p.id AS patient_id,d.id AS doctor_id FROM users u "
                + "LEFT JOIN patients p ON p.user_id=u.id LEFT JOIN doctors d ON d.user_id=u.id WHERE u.id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new User(id, username, role, (Integer) rs.getObject("patient_id"),
                        (Integer) rs.getObject("doctor_id"));
            }
        }
    }

    public void registerPatient(String username, char[] password, String name,
                                int age, String gender, String phone, String address) throws SQLException {
        String hash = PasswordUtil.hash(password);
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                int userId;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO users(username,password_hash,password,role) VALUES(?,?,NULL,'PATIENT')",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, username.trim()); ps.setString(2, hash);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); userId = keys.getInt(1); }
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO patients(user_id,name,age,gender,phone,address) VALUES(?,?,?,?,?,?)")) {
                    ps.setInt(1, userId); ps.setString(2, name.trim()); ps.setInt(3, age); ps.setString(4, gender);
                    ps.setString(5, nullable(phone)); ps.setString(6, nullable(address));
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException ex) {
                con.rollback(); throw ex;
            } finally { con.setAutoCommit(true); }
        }
    }

    public int createDoctorAccount(Connection con, String username, char[] password, String email) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO users(username,password_hash,password,role,email) VALUES(?,?,NULL,'DOCTOR',?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username.trim()); ps.setString(2, PasswordUtil.hash(password));
            ps.setString(3, nullable(email)); ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); return keys.getInt(1); }
        }
    }

    public void changePassword(int userId, char[] currentPassword, char[] newPassword) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String hash = null, legacy = null;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT password_hash,password FROM users WHERE id=? AND active=TRUE")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Account is unavailable.");
                    hash = rs.getString("password_hash"); legacy = rs.getString("password");
                }
            }
            boolean valid = hash != null && PasswordUtil.verify(currentPassword, hash);
            if (!valid && (legacy == null || !legacy.contentEquals(new String(currentPassword))))
                throw new IllegalArgumentException("Current password is incorrect.");
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE users SET password_hash=?,password=NULL WHERE id=?")) {
                ps.setString(1, PasswordUtil.hash(newPassword)); ps.setInt(2, userId); ps.executeUpdate();
            }
        }
    }

    private static String nullable(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
