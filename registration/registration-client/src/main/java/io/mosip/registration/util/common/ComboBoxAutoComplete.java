package io.mosip.registration.util.common;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.util.stream.Stream;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
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

	private static final Logger LOGGER = AppConfig.getLogger(ComboBoxAutoComplete.class);
	private static final String loggerClassName = "ComboBoxAutoComplete";

	private final ComboBox<T> cmb;
	private SimpleStringProperty filter = new SimpleStringProperty();
	private final ObservableList<T> originalItems;

	public ComboBoxAutoComplete(ComboBox<T> cmb) {
		LOGGER.debug(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
				"New ComboBoxAutoComplete called");

		this.cmb = cmb;
		originalItems = FXCollections.observableArrayList(cmb.getItems());
		cmb.setTooltip(new Tooltip());
		cmb.setOnKeyPressed(this::handleOnKeyPressed);
		cmb.setOnHidden(this::handleOnHiding);

		filter.addListener((observable, oldValue, newValue) -> {
			if (filter.get().isEmpty()) {
				LOGGER.debug(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
						"New ComboBoxAutoComplete called");
				T selectedItem = this.cmb.getSelectionModel().getSelectedItem();
				this.cmb.getItems().clear();
				this.cmb.getItems().addAll(originalItems);

				if (selectedItem != null) {
					this.cmb.getSelectionModel().select(selectedItem);
				}
			}
		});
	}

	public void handleOnKeyPressed(KeyEvent key) {
		LOGGER.debug(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
				"Combo box search started");
		ObservableList<T> filteredList = FXCollections.observableArrayList();

		KeyCode code = key.getCode();
		if (code.isLetterKey()) {
			if (filter.get() == null) {
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
			String txtUsr = filter.get().toLowerCase();
			itens.filter(el -> el.toString().toLowerCase().contains(txtUsr)).forEach(filteredList::add);
			cmb.getTooltip().setText(txtUsr);
			Window stage = cmb.getScene().getWindow();
			double posX = stage.getX() + cmb.localToScene(cmb.getBoundsInLocal()).getMinX();
			double posY = stage.getY() + cmb.localToScene(cmb.getBoundsInLocal()).getMinY();
			cmb.getTooltip().show(stage, posX, posY);
			cmb.show();
			cmb.getItems().setAll(filteredList);

		}
		LOGGER.debug(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
				"Combo box search completed");
	}

	public void handleOnHiding(Event e) {
		LOGGER.debug(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID, "Hide tool tip started");
		filter.set("");
		cmb.getTooltip().hide();
		LOGGER.debug(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
				"Hide tool tip completed");
	}

}
