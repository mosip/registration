package io.mosip.registration.util.common;

import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import org.springframework.stereotype.Component;

@Component
public class CopytoActionHandler extends ChangeActionHandler {

    @Override
    public String getActionClassName() {
        return "copyto";
    }

    @Override
    public void handle(Pane parentPane, String source, String[] args) {
        boolean copyEnabled = false;

        if(args.length == 3) {
           Node flagNode = parentPane.lookup(HASH.concat(args[2]));
           if(flagNode != null && flagNode instanceof CheckBox) {
               copyEnabled = ((CheckBox) flagNode).isSelected();
               if(!copyEnabled) { return; }
           }
        }

        if(args.length > 1) {
            Node fromNode = parentPane.lookup(HASH.concat(args[0]));
            Node toNode = parentPane.lookup(HASH.concat(args[1]));

            if(!isValidNode(fromNode) || !isValidNode(toNode))
                return;

            if(fromNode instanceof TextField) {
                copy(parentPane, (TextField) fromNode, (TextField) toNode);
            }
            else if(fromNode instanceof ComboBox) {
                copy((ComboBox) fromNode, (ComboBox) toNode);
            }

            if(copyEnabled) { toNode.setDisable(true); }
        }
    }

    private void copy(Pane parentPane, TextField fromNode, TextField toNode) {
        toNode.setText(fromNode.getText());
        Node localLangNode = parentPane.lookup(HASH.concat(toNode.getId()).concat("LocalLanguage"));
        if(isValidNode(localLangNode) && localLangNode instanceof TextField) {
            ((TextField)localLangNode).setText(fromNode.getText());
        }
    }

    private void copy(ComboBox fromNode, ComboBox toNode) {
        toNode.getSelectionModel().select(fromNode.getSelectionModel().getSelectedItem());
    }
}
