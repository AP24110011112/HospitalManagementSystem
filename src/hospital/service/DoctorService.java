package hospital.service;

import hospital.dao.DoctorDAO;
import hospital.model.Doctor;
import java.sql.SQLException;
import java.util.List;

public class DoctorService {
    private final DoctorDAO dao = new DoctorDAO();
    public List<Doctor> search(String query) throws SQLException { return dao.search(query); }
    public void add(Doctor d, String username, char[] password) throws SQLException {
        validate(d);
        String login = Validation.required(username, "Doctor username");
        if (!login.matches("[A-Za-z0-9_. -]{3,50}")) throw new IllegalArgumentException("Enter a valid doctor username (3–50 characters; spaces are allowed).");
        Validation.password(password);
        dao.add(d, login, password);
    }
    public void update(Doctor d) throws SQLException { validate(d); dao.update(d); }
    public void delete(int id) throws SQLException { dao.delete(id); }
    private void validate(Doctor d) {
        Validation.required(d.getName(), "Name"); Validation.required(d.getSpecialization(), "Specialization");
        Validation.phone(d.getPhone());
        if (d.getEmail() != null && !d.getEmail().isBlank()) Validation.email(d.getEmail());
        if (!Double.isFinite(d.getFee()) || d.getFee() < 0) throw new IllegalArgumentException("Fee must be a valid non-negative amount.");
    }
}
