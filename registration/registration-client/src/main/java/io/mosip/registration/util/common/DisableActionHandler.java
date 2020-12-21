package io.mosip.registration.util.common;

import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.Pane;
import org.springframework.stereotype.Component;

@Component
public class DisableActionHandler extends ChangeActionHandler {

    @Override
    public String getActionClassName() {
        return "disable";
    }

    @Override
    public void handle(Pane parentPane, String source, String[] args) {
        boolean disable = true;
        Node copyEnabledFlagNode = parentPane.lookup(HASH.concat(source));
        if(isValidNode(copyEnabledFlagNode)) {
            disable = ((CheckBox)copyEnabledFlagNode).isSelected() ? true : false;
        }

        for(String arg : args) {
            Node node = parentPane.lookup(HASH.concat(arg));
            if(isValidNode(node)) {
                node.setDisable(disable);
            }
        }
    }
}
