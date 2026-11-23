package hospital.db;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** JDBC configuration comes from environment variables; credentials are not stored in source. */
public final class DBConnection {
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/hospital_db?serverTimezone=UTC&useSSL=false";
    private static final Path LOCAL_CONFIG = Path.of("db.properties");
    private DBConnection() { }

    public static Connection getConnection() throws SQLException {
        Properties local = loadLocalConfiguration();
        return connect(setting("HOSPITAL_DB_URL", "url", DEFAULT_URL, local),
                setting("HOSPITAL_DB_USER", "user", "root", local),
                setting("HOSPITAL_DB_PASSWORD", "password", "", local));
    }

    public static Connection testConnection(String url, String username, String password) throws SQLException {
        if (url == null || url.isBlank()) throw new SQLException("Database URL is required.");
        if (username == null || username.isBlank()) throw new SQLException("Database username is required.");
        return connect(url.trim(), username.trim(), password == null ? "" : password);
    }

    private static Connection connect(String url, String username, String password) throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    public static void saveLocalConfiguration(String url, String username, String password) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("url", url.trim());
        properties.setProperty("user", username.trim());
        properties.setProperty("password", password == null ? "" : password);
        try (OutputStream out = Files.newOutputStream(LOCAL_CONFIG)) {
            properties.store(out, "Local Hospital Management System database settings");
        }
    }

    private static Properties loadLocalConfiguration() throws SQLException {
        Properties properties = new Properties();
        if (!Files.isRegularFile(LOCAL_CONFIG)) return properties;
        try (InputStream in = Files.newInputStream(LOCAL_CONFIG)) {
            properties.load(in);
            return properties;
        } catch (IOException ex) {
            throw new SQLException("Could not read local db.properties settings.", ex);
        }
    }

    private static String setting(String environmentKey, String propertyKey, String fallback, Properties local) {
        String value = System.getenv(environmentKey);
        if (value != null && !value.isBlank()) return value.trim();
        value = local.getProperty(propertyKey);
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
