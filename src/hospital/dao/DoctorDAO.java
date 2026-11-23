package hospital.dao;

import hospital.db.DBConnection;
import hospital.model.Doctor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DoctorDAO {
    private static final String SELECT = "SELECT d.id,d.user_id,d.department_id,dep.name AS department_name,"
            + "d.name,d.specialization,d.phone,d.email,d.fee FROM doctors d "
            + "LEFT JOIN departments dep ON dep.id=d.department_id WHERE d.active=TRUE";

    /** Doctor profile and its login are created atomically. */
    public void add(Doctor doctor, String username, char[] password) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                int userId = new UserDAO().createDoctorAccount(con, username, password, doctor.getEmail());
                String sql = "INSERT INTO doctors(user_id,department_id,name,specialization,phone,email,fee) VALUES(?,?,?,?,?,?,?)";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    bind(ps, doctor); ps.setInt(1, userId); // bind below uses 1-based doctor fields
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException ex) { con.rollback(); throw ex; }
            finally { con.setAutoCommit(true); }
        }
    }

    private void bind(PreparedStatement ps, Doctor d) throws SQLException {
        if (d.getUserId() != null) ps.setInt(1, d.getUserId()); else ps.setNull(1, Types.INTEGER);
        if (d.getDepartmentId() != null) ps.setInt(2, d.getDepartmentId()); else ps.setNull(2, Types.INTEGER);
        ps.setString(3, d.getName()); ps.setString(4, d.getSpecialization());
        ps.setString(5, emptyToNull(d.getPhone())); ps.setString(6, emptyToNull(d.getEmail())); ps.setDouble(7, d.getFee());
    }

    public void update(Doctor d) throws SQLException {
        String sql = "UPDATE doctors SET user_id=?,department_id=?,name=?,specialization=?,phone=?,email=?,fee=? WHERE id=?";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, d); ps.setInt(8, d.getId()); ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "UPDATE doctors d LEFT JOIN users u ON u.id=d.user_id SET d.active=FALSE,u.active=FALSE WHERE d.id=?";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }

    public List<Doctor> search(String keyword) throws SQLException {
        List<Doctor> list = new ArrayList<>();
        String sql = SELECT + " AND (d.name LIKE ? OR d.specialization LIKE ? OR dep.name LIKE ?) ORDER BY d.name";
        String pattern = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, pattern); ps.setString(2, pattern); ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        }
        return list;
    }

    public List<Doctor> getAll() throws SQLException { return search(""); }
    public Doctor findById(int id) throws SQLException {
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(SELECT + " AND d.id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }
    public int count() throws SQLException {
        try (Connection con = DBConnection.getConnection(); Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM doctors WHERE active=TRUE")) { rs.next(); return rs.getInt(1); }
    }
    public int countActiveAccounts() throws SQLException {
        String sql = "SELECT COUNT(*) FROM doctors d JOIN users u ON u.id=d.user_id "
                + "WHERE d.active=TRUE AND u.active=TRUE AND u.role='DOCTOR'";
        try (Connection con = DBConnection.getConnection(); Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) { rs.next(); return rs.getInt(1); }
    }
    private Doctor map(ResultSet rs) throws SQLException {
        return new Doctor(rs.getInt("id"), rs.getString("name"), rs.getString("specialization"), rs.getString("phone"),
                rs.getDouble("fee"), (Integer) rs.getObject("department_id"), rs.getString("department_name"),
                rs.getString("email"), (Integer) rs.getObject("user_id"));
    }
    private static String emptyToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
