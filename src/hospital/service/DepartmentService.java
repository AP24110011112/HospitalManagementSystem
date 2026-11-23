package hospital.service;

import hospital.dao.DepartmentDAO;
import hospital.model.Department;
import java.sql.SQLException;
import java.util.List;

public class DepartmentService {
    private final DepartmentDAO dao = new DepartmentDAO();
    public List<Department> getAll() throws SQLException { return dao.getAll(); }
    public void add(String name, String description) throws SQLException { dao.add(Validation.required(name, "Department name"), description); }
    public void update(int id, String name, String description) throws SQLException { dao.update(id, Validation.required(name, "Department name"), description); }
    public void delete(int id) throws SQLException { dao.delete(id); }
}
