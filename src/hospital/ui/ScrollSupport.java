package hospital.ui;

import javafx.scene.Node;
import javafx.scene.control.ScrollPane;

/** Small shared helper for pages and forms that may exceed the available window height. */
public final class ScrollSupport {
    private ScrollSupport() { }

    public static ScrollPane vertical(Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setPannable(true);
        scroll.getStyleClass().add("page-scroll");
        return scroll;
    }
}
