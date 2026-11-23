package hospital.dao;

import hospital.db.DBConnection;
import hospital.model.Patient;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PatientDAO {
    private static final String SELECT = "SELECT id,user_id,name,age,gender,phone,email,address,disease FROM patients";

    public void add(Patient patient) throws SQLException {
        String sql = "INSERT INTO patients(name,age,gender,phone,email,address,disease) VALUES(?,?,?,?,?,?,?)";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, patient); ps.executeUpdate();
        }
    }

    public void update(Patient patient) throws SQLException {
        String sql = "UPDATE patients SET name=?,age=?,gender=?,phone=?,email=?,address=?,disease=? WHERE id=?";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, patient); ps.setInt(8, patient.getId()); ps.executeUpdate();
        }
    }

    private void bind(PreparedStatement ps, Patient p) throws SQLException {
        ps.setString(1, p.getName()); ps.setInt(2, p.getAge()); ps.setString(3, p.getGender());
        ps.setString(4, emptyToNull(p.getPhone())); ps.setString(5, emptyToNull(p.getEmail()));
        ps.setString(6, emptyToNull(p.getAddress())); ps.setString(7, emptyToNull(p.getDisease()));
    }

    public void delete(int id) throws SQLException {
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement("DELETE FROM patients WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }

    public List<Patient> search(String keyword) throws SQLException {
        List<Patient> list = new ArrayList<>();
        String sql = SELECT + " WHERE name LIKE ? OR disease LIKE ? OR email LIKE ? OR phone LIKE ? ORDER BY name";
        String pattern = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 1; i <= 4; i++) ps.setString(i, pattern);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        }
        return list;
    }

    public List<Patient> getAll() throws SQLException { return search(""); }
    public Patient findById(int id) throws SQLException {
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(SELECT + " WHERE id=?")) {
            ps.setInt(1, id); try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }
    public int count() throws SQLException {
        try (Connection con = DBConnection.getConnection(); Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM patients")) { rs.next(); return rs.getInt(1); }
    }

    private Patient map(ResultSet rs) throws SQLException {
        return new Patient(rs.getInt("id"), rs.getString("name"), rs.getInt("age"), rs.getString("gender"),
                rs.getString("phone"), rs.getString("address"), rs.getString("disease"),
                rs.getString("email"), (Integer) rs.getObject("user_id"));
    }
    static String emptyToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
