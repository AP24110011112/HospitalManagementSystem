package hospital.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.beans.property.StringProperty;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

/**
 * Password input that can reveal its current value without clearing or replacing it.
 */
public final class PasswordVisibilityField extends HBox {
    private final PasswordField maskedField = new PasswordField();
    private final TextField visibleField = new TextField();
    private final Button toggle = new Button();
    private final Tooltip tooltip = new Tooltip();
    private boolean revealed;

    public PasswordVisibilityField() {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(6);
        setMaxWidth(Double.MAX_VALUE);
        getStyleClass().add("password-visibility-field");

        maskedField.textProperty().bindBidirectional(visibleField.textProperty());
        maskedField.setMaxWidth(Double.MAX_VALUE);
        visibleField.setMaxWidth(Double.MAX_VALUE);
        visibleField.setVisible(false);
        visibleField.setManaged(false);

        StackPane input = new StackPane(maskedField, visibleField);
        input.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(input, Priority.ALWAYS);

        toggle.getStyleClass().add("password-visibility-button");
        toggle.setMinWidth(40);
        toggle.setPrefWidth(40);
        toggle.setFocusTraversable(true);
        toggle.setTooltip(tooltip);
        toggle.setOnAction(event -> setRevealed(!revealed));

        getChildren().addAll(input, toggle);
        updateToggle();
    }

    public String getText() {
        return maskedField.getText();
    }

    public StringProperty textProperty() {
        return maskedField.textProperty();
    }

    public void clear() {
        maskedField.clear();
    }

    public void setPromptText(String prompt) {
        maskedField.setPromptText(prompt);
        visibleField.setPromptText(prompt);
    }

    public void setOnAction(EventHandler<ActionEvent> handler) {
        maskedField.setOnAction(handler);
        visibleField.setOnAction(handler);
    }

    public void setFieldHeight(double height) {
        setPrefHeight(height);
        maskedField.setPrefHeight(height);
        visibleField.setPrefHeight(height);
        toggle.setPrefHeight(height);
    }

    private void setRevealed(boolean show) {
        TextInputControl previous = revealed ? visibleField : maskedField;
        int caret = previous.getCaretPosition();
        revealed = show;

        maskedField.setVisible(!show);
        maskedField.setManaged(!show);
        visibleField.setVisible(show);
        visibleField.setManaged(show);

        TextInputControl active = show ? visibleField : maskedField;
        active.positionCaret(Math.min(caret, active.getLength()));
        updateToggle();
        active.requestFocus();
    }

    private void updateToggle() {
        String label = revealed ? "Hide password" : "Show password";
        toggle.setAccessibleText(label);
        tooltip.setText(label);
        toggle.setGraphic(eyeIcon(revealed));
    }

    private static StackPane eyeIcon(boolean shown) {
        Color stroke = Color.web("#53647a");
        Ellipse outline = new Ellipse(8, 5.3);
        outline.setFill(Color.TRANSPARENT);
        outline.setStroke(stroke);
        outline.setStrokeWidth(1.5);
        Circle pupil = new Circle(2.1, Color.web("#087f70"));
        StackPane icon = new StackPane(outline, pupil);
        icon.setMinSize(20, 20);
        icon.setPrefSize(20, 20);
        if (!shown) {
            Line slash = new Line(-6.5, 6.5, 6.5, -6.5);
            slash.setStroke(stroke);
            slash.setStrokeWidth(1.5);
            icon.getChildren().add(slash);
        }
        return icon;
    }
}
