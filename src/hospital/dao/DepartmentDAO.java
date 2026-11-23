package hospital.dao;

import hospital.db.DBConnection;
import hospital.model.Department;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DepartmentDAO {
    public List<Department> getAll() throws SQLException {
        List<Department> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT id,name,description FROM departments ORDER BY name")) {
            while (rs.next()) list.add(new Department(rs.getInt("id"), rs.getString("name"), rs.getString("description")));
        }
        return list;
    }
    public void add(String name, String description) throws SQLException {
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(
                "INSERT INTO departments(name,description) VALUES(?,?)")) {
            ps.setString(1, name.trim()); ps.setString(2, emptyToNull(description)); ps.executeUpdate();
        }
    }
    public void update(int id, String name, String description) throws SQLException {
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(
                "UPDATE departments SET name=?,description=? WHERE id=?")) {
            ps.setString(1, name.trim()); ps.setString(2, emptyToNull(description)); ps.setInt(3, id); ps.executeUpdate();
        }
    }
    public void delete(int id) throws SQLException {
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement("DELETE FROM departments WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }
    private static String emptyToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
