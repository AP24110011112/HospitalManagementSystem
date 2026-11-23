package hospital;

import hospital.model.User;
import hospital.service.AuthService;
import hospital.ui.LoginView;
import hospital.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.sql.SQLException;

public class Main extends Application {
    private final AuthService auth = new AuthService();
    private Stage stage;

    @Override public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("CarePoint | Hospital Appointments");
        String setupMessage = null;
        try { auth.initialize(); }
        catch (SQLException ex) { setupMessage = "Database is not ready. " + friendly(ex); }
        showLogin(setupMessage);
        stage.show();
    }

    private void showLogin(String message) {
        LoginView view = new LoginView(auth, this::showMain, message);
        Scene scene = new Scene(view, 1040, 720);
        style(scene);
        stage.setScene(scene);
        stage.setMinWidth(760);
        stage.setMinHeight(540);
    }

    private void showMain(User user) {
        MainView view = new MainView(user, () -> showLogin(null));
        Scene scene = new Scene(view, 1280, 820);
        style(scene);
        stage.setScene(scene);
        stage.setMinWidth(760);
        stage.setMinHeight(540);
    }

    private void style(Scene scene) {
        var stylesheet = getClass().getResource("/hospital/ui/theme.css");
        if (stylesheet != null) scene.getStylesheets().add(stylesheet.toExternalForm());
    }

    private static String friendly(SQLException ex) {
        if (ex.getErrorCode() == 1045 || "28000".equals(ex.getSQLState()))
            return "MySQL rejected the locally configured username/password. Check db.properties or HOSPITAL_DB_* environment variables.";
        if (ex.getErrorCode() == 1049 || ex.getErrorCode() == 1146)
            return "Database schema is missing. Run database/schema.sql (or database/upgrade_existing_db.sql for the original project).";
        if (ex.getErrorCode() == 2003 || ex.getErrorCode() == 0)
            return "Could not reach MySQL. Start MySQL and check the configured URL and port.";
        return ex.getMessage() == null ? "Database connection failed." : ex.getMessage();
    }

    public static void main(String[] args) { launch(args); }
}
