package io.mosip.registration.util.common;

import javafx.scene.Node;
import javafx.scene.layout.Pane;

public abstract class ChangeActionHandler {

    public static final String HASH = "#";
    public abstract String getActionClassName();
    public abstract void handle(Pane parentPane, String source, String[] args);

    public boolean isValidNode(Node node) {
        return (node != null && node.isVisible()) ? true : false;
    }

}
