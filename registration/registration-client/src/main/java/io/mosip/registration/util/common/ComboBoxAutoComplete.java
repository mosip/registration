package io.mosip.registration.util.common;

import java.text.Normalizer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Window;

/**
 * Uses a combobox tooltip as the suggestion for auto complete and updates the
 * combo box itens accordingly <br />
 * It does not work with space, space and escape cause the combobox to hide and
 * clean the filter ... Send me a PR if you want it to work with all characters
 * -> It should be a custom controller - I KNOW!
 *
 * @param <T>
 * @author wsiqueir
 */
public class ComboBoxAutoComplete<T> {

	private final ComboBox<T> cmb;
	SimpleStringProperty filter = new SimpleStringProperty();
	private final ObservableList<T> originalItems;

	public ComboBoxAutoComplete(ComboBox<T> cmb) {
		this.cmb = cmb;
		originalItems = FXCollections.observableArrayList(cmb.getItems());
		cmb.setTooltip(new Tooltip());
		cmb.setOnKeyPressed(this::handleOnKeyPressed);
		cmb.setOnHidden(this::handleOnHiding);

		filter.addListener((observable, oldValue, newValue) -> {

			if (filter.get().isEmpty()) {

				this.cmb.getItems().clear();
				this.cmb.getItems().addAll(originalItems);
			}
		});
	}

	public void handleOnKeyPressed(KeyEvent key) {
		ObservableList<T> filteredList = FXCollections.observableArrayList();

		KeyCode code = key.getCode();
		if (code.isLetterKey()) {
			if(filter.get()==null) {
				filter.set("");
			}
			filter.set(filter.get() + key.getText());
		}
		if (code == KeyCode.BACK_SPACE && filter.get().length() > 0) {
			filter.set(filter.get().substring(0, filter.get().length() - 1));
			cmb.getItems().setAll(originalItems);
		}
		if (code == KeyCode.ESCAPE) {
			filter.set("");
		}
		if (code == KeyCode.TAB) {
			filteredList = originalItems;
			cmb.getTooltip().hide();
			cmb.getEditor().setText(filter.get());
		}
		if (filter.get().length() == 0) {
			filteredList = originalItems;
			cmb.getTooltip().hide();
		} else {
			Stream<T> itens = cmb.getItems().stream();
			String txtUsr = unaccent(filter.get().toLowerCase());
			itens.filter(el -> unaccent(el.toString().toLowerCase()).contains(txtUsr)).forEach(filteredList::add);
			cmb.getTooltip().setText(txtUsr);
			Window stage = cmb.getScene().getWindow();
			double posX = stage.getX() + cmb.getBoundsInParent().getMinX();
			double posY = stage.getY() + cmb.getBoundsInParent().getMinY();

//			event.getScreenX(), event.getScreenY() + 15)
			cmb.getTooltip().show(stage, cmb.getBoundsInParent().getHeight(), cmb.getBoundsInParent().getWidth());
			cmb.show();

			cmb.getItems().setAll(filteredList);
			
		}

	}

	public void handleOnHiding(Event e) {
		filter.set("");
		cmb.getTooltip().hide();
//		T s = cmb.getSelectionModel().getSelectedItem();
//		cmb.getItems().setAll(originalItems);
//		cmb.getSelectionModel().select(s);
	}

	private String unaccent(String s) {
		String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
		Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
		return pattern.matcher(temp).replaceAll("");
	}

}
