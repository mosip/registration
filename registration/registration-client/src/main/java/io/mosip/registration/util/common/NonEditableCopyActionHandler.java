package io.mosip.registration.util.common;

import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import org.springframework.stereotype.Component;

@Component
public class NonEditableCopyActionHandler extends ChangeActionHandler {

    @Override
    public String getActionClassName() {
        return "copy&disable";
    }

    @Override
    public void handle(Pane parentPane, String source, String[] args) {
        Node copyEnabledFlagNode = parentPane.lookup(HASH.concat(source));
        boolean enabled = ((CheckBox)copyEnabledFlagNode).isSelected() ? true : false;

        for(String arg : args) {
            String[] parts = arg.split("=");
            if(parts.length == 2) {
                Node fromNode = parentPane.lookup(HASH.concat(parts[0]));
                Node toNode = parentPane.lookup(HASH.concat(parts[1]));

                if(!isValidNode(fromNode) || !isValidNode(toNode))
                    continue;

                if(fromNode instanceof TextField) {
                    copy(parentPane, (TextField) fromNode, (TextField) toNode, enabled);
                }
                else if(fromNode instanceof ComboBox) {
                    copy((ComboBox) fromNode, (ComboBox) toNode, enabled);
                }
            }
        }
    }

    private void copy(Pane parentPane, TextField fromNode, TextField toNode, boolean isEnabled) {
        if(isEnabled) {
            toNode.setText(fromNode.getText());
            toNode.setDisable(true);
            Node localLangNode = parentPane.lookup(HASH.concat(toNode.getId()).concat("LocalLanguage"));
            if(isValidNode(localLangNode) && localLangNode instanceof TextField) {
                ((TextField)localLangNode).setText(fromNode.getText());
                localLangNode.setDisable(true);
            }
        }
        else {
            toNode.setDisable(false);
            Node localLangNode = parentPane.lookup(HASH.concat(toNode.getId()).concat("LocalLanguage"));
            if(isValidNode(localLangNode) && localLangNode instanceof TextField) {
                localLangNode.setDisable(false);
            }
        }
    }

    private void copy(ComboBox fromNode, ComboBox toNode, boolean isEnabled) {
        if(isEnabled) {
            toNode.getSelectionModel().select(fromNode.getSelectionModel().getSelectedItem());
            toNode.setDisable(true);
        }
        else {
            toNode.setDisable(false);
        }
    }
}
