package io.mosip.registration.util.common;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.controller.device.ScanPopUpViewController;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import lombok.SneakyThrows;

/**
 * Drag rectangle with mouse cursor in order to get selection bounds
 */
public class RubberBandSelection {

	private static final Logger LOGGER = AppConfig.getLogger(RubberBandSelection.class);
	private static final String loggerClassName = "RubberBandSelection";
	private ScanPopUpViewController scanPopUpViewController = null;
	private Rectangle rect = new Rectangle();
	private ImageView imageView = null;

	private double startSelectionX;
	private double startSelectionY;

	private Group group;

	public void setscanPopUpViewController(ScanPopUpViewController scanPopUpViewController) {
		this.scanPopUpViewController = scanPopUpViewController;
	}

	public Bounds getBounds() {
		return rect.getBoundsInParent();
	}

	public RubberBandSelection(Group group) {

		LOGGER.debug(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
				"Rubber band selection called");
		this.group = group;

		rect = new Rectangle(0, 0, 0, 0);
		rect.setStroke(Color.BLUE);
		rect.setStrokeWidth(1);
		rect.setStrokeLineCap(StrokeLineCap.ROUND);
		rect.setFill(Color.LIGHTBLUE.deriveColor(0, 1.2, 1, 0.6));

		for (Node node : group.getChildren()) {
			if (node instanceof ImageView) {
				imageView = (ImageView) node;
			}
		}

		LOGGER.debug(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
				"Setting listeners on mouse,pressed,dragged and released for crop");
		group.addEventHandler(MouseEvent.MOUSE_PRESSED, onMousePressedEventHandler);
		group.addEventHandler(MouseEvent.MOUSE_DRAGGED, onMouseDraggedEventHandler);
		group.addEventHandler(MouseEvent.MOUSE_RELEASED, onMouseReleasedEventHandler);

	}

	EventHandler<MouseEvent> onMousePressedEventHandler = new EventHandler<MouseEvent>() {

		@Override
		public void handle(MouseEvent event) {

			LOGGER.debug(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
					"Crop on mouse pressed startred");
			if (event.isSecondaryButtonDown())
				return;

			// prepare new drag operation
			startSelectionX = event.getX();
			startSelectionY = event.getY();

			rect.setX(startSelectionX);
			rect.setY(startSelectionY);
			rect.setWidth(0);
			rect.setHeight(0);

			group.getChildren().add(rect);

			LOGGER.debug(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
					"Crop on mouse pressed completed");
		}
	};

	EventHandler<MouseEvent> onMouseDraggedEventHandler = new EventHandler<MouseEvent>() {

		@Override
		public void handle(MouseEvent event) {

			LOGGER.debug(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
					"Crop on mouse draged startred");
			if (event.isSecondaryButtonDown())
				return;

			double endSelectionX = event.getX() - startSelectionX;
			double endSelectionY = event.getY() - startSelectionY;

			Bounds bounds = imageView.getLayoutBounds();
			double xScale = bounds.getWidth() / imageView.getImage().getWidth();
			double yScale = bounds.getHeight() / imageView.getImage().getHeight();

			endSelectionX /= xScale;
			endSelectionY /= yScale;

			double xLimit = bounds.getMaxX() - startSelectionX;

			double yLimit = bounds.getMaxY() - startSelectionY;
			if (endSelectionX > xLimit) {
				endSelectionX = xLimit;
			}

			if (endSelectionY > yLimit) {
				endSelectionY = yLimit;
			}

			if (endSelectionX > 0)
				rect.setWidth(endSelectionX);
			else {
				rect.setX(event.getX());
				rect.setWidth(startSelectionX - rect.getX());
			}

			if (endSelectionY > 0) {
				rect.setHeight(endSelectionY);
			} else {
				rect.setY(event.getY());
				rect.setHeight(startSelectionY - rect.getY());
			}
			LOGGER.debug(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
					"Crop on mouse draged completed");
		}

	};

	EventHandler<MouseEvent> onMouseReleasedEventHandler = new EventHandler<MouseEvent>() {

		@SneakyThrows
		@Override
		public void handle(MouseEvent event) {

			LOGGER.debug(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
					"Crop on mouse released started");
			// get bounds for image crop
			Bounds selectionBounds = getBounds();

			// crop the image
			scanPopUpViewController.save(selectionBounds);

			group.removeEventHandler(MouseEvent.MOUSE_PRESSED, onMousePressedEventHandler);
			group.removeEventHandler(MouseEvent.MOUSE_DRAGGED, onMouseDraggedEventHandler);
			group.removeEventHandler(MouseEvent.MOUSE_RELEASED, onMouseReleasedEventHandler);
			group.getChildren().remove(rect);
			LOGGER.debug(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
					"Crop on mouse released completed");
		}
	};

}