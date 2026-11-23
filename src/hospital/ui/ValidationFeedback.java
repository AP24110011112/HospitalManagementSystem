package hospital.ui;

import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.TextInputControl;

/** Adds/removes the shared red invalid state and clears it when the user edits a field. */
public final class ValidationFeedback {
    private static final PseudoClass INVALID = PseudoClass.getPseudoClass("invalid");

    private ValidationFeedback() { }

    public static void mark(Node field) {
        if (field != null) field.pseudoClassStateChanged(INVALID, true);
    }

    public static void clear(Node field) {
        if (field != null) field.pseudoClassStateChanged(INVALID, false);
    }

    public static void watch(Node field) {
        if (field instanceof PasswordVisibilityField password) {
            password.textProperty().addListener((obs, old, value) -> clear(field));
        } else if (field instanceof TextInputControl text) {
            text.textProperty().addListener((obs, old, value) -> clear(field));
        } else if (field instanceof ComboBoxBase<?> combo) {
            combo.valueProperty().addListener((obs, old, value) -> clear(field));
        }
    }

    public static void markAll(Node... fields) {
        for (Node field : fields) mark(field);
    }
}
