package hospital.controller;

import hospital.model.User;
import hospital.service.AuthService;

import java.sql.SQLException;

/** JavaFX login and registration actions delegate validation to the authentication service. */
public class LoginController {
    private final AuthService auth;
    public LoginController(AuthService auth) { this.auth = auth; }
    public User login(String username, char[] password) throws SQLException { return auth.login(username, password); }
    public void registerPatient(String username, char[] password, String name, String age,
                                String gender, String phone, String address) throws SQLException {
        auth.registerPatient(username, password, name, age, gender, phone, address);
    }
    public void registerDoctor(String username, char[] password, String name, String specialization,
                               Integer departmentId, String phone, String email, String fee) throws SQLException {
        auth.registerDoctor(username, password, name, specialization, departmentId, phone, email, fee);
    }
}
