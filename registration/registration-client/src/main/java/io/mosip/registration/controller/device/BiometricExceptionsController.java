package io.mosip.registration.controller.device;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

@Controller
public class BiometricExceptionsController {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(BiometricExceptionsController.class);

	@FXML
	private GridPane exceptionBiometricsPane;

	public GridPane getExceptionBiometricsPane() {
		return exceptionBiometricsPane;
	}

	@Autowired
	private BiometricsController biometricsController;

	private String loggerClassName = "BiometricExceptionsController";

	@FXML
	public void addException(MouseEvent event) {

		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Clicked on exception Image");

		ImageView exceptionImage = (ImageView) event.getSource();

		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
				"Clicked on exception Image : " + exceptionImage.getId());

		Pane pane = (Pane) exceptionImage.getParent();

		List<ImageView> paneExceptionBioAttributes = new LinkedList<>();
		for (Node node : pane.getChildren()) {
			if (node instanceof ImageView && node.getId() != null && !node.getId().isEmpty()) {

				paneExceptionBioAttributes.add((ImageView) node);
			}
		}
		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
				"All exception images for same modality" + paneExceptionBioAttributes);

		biometricsController.updateBiometricData(exceptionImage, paneExceptionBioAttributes);

		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Add or remove exception completed");

	}
}
