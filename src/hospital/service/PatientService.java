package hospital.service;

import hospital.dao.PatientDAO;
import hospital.model.Patient;
import java.sql.SQLException;
import java.util.List;

public class PatientService {
    private final PatientDAO dao = new PatientDAO();
    public List<Patient> search(String query) throws SQLException { return dao.search(query); }
    public void save(Patient p) throws SQLException {
        Validation.required(p.getName(), "Name");
        if (p.getAge() < 1 || p.getAge() > 120) throw new IllegalArgumentException("Age must be from 1 to 120.");
        if (p.getGender() == null || !java.util.Set.of("Male", "Female", "Other").contains(p.getGender()))
            throw new IllegalArgumentException("Choose a gender.");
        Validation.phone(p.getPhone());
        if (p.getEmail() != null && !p.getEmail().isBlank()) Validation.email(p.getEmail());
        if (p.getId() == 0) dao.add(p); else dao.update(p);
    }
    public void delete(int id) throws SQLException { dao.delete(id); }
}
