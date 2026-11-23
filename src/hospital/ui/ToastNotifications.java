package hospital.ui;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

/** Non-blocking notifications positioned at the top-center of the active app window. */
public final class ToastNotifications {
    public enum Kind { SUCCESS, ERROR, INFO }

    private ToastNotifications() { }

    public static void show(Node source, String text, Kind kind) {
        if (source == null || source.getScene() == null || source.getScene().getWindow() == null) return;
        Window owner = source.getScene().getWindow();
        Label icon = new Label(kind == Kind.SUCCESS ? "✓" : kind == Kind.ERROR ? "!" : "i");
        icon.getStyleClass().add("toast-icon");
        Label message = new Label(text);
        message.setWrapText(true);
        HBox.setHgrow(message, Priority.ALWAYS);
        HBox card = new HBox(12, icon, message);
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        card.setPadding(new Insets(14, 18, 14, 18));
        card.setPrefWidth(360);
        card.setMaxWidth(420);
        card.getStyleClass().addAll("toast", "toast-" + kind.name().toLowerCase(java.util.Locale.ROOT));

        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.setAutoFix(true);
        popup.setHideOnEscape(true);
        popup.getContent().add(card);
        popup.show(owner);

        Scene popupScene = card.getScene();
        if (popupScene != null) {
            var stylesheet = ToastNotifications.class.getResource("theme.css");
            if (stylesheet != null && !popupScene.getStylesheets().contains(stylesheet.toExternalForm()))
                popupScene.getStylesheets().add(stylesheet.toExternalForm());
        }
        card.applyCss();
        card.autosize();

        popup.setX(owner.getX() + Math.max(0, (owner.getWidth() - card.getWidth()) / 2));
        popup.setY(owner.getY() + 24);
        PauseTransition timeout = new PauseTransition(Duration.seconds(3.6));
        timeout.setOnFinished(event -> popup.hide());
        timeout.play();
    }
}
