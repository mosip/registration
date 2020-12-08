package io.mosip.registration.util.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.mosip.registration.controller.reg.DemographicDetailController;
import io.mosip.registration.controller.reg.DocumentScanController;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import lombok.SneakyThrows;

/**
 * Drag rectangle with mouse cursor in order to get selection bounds
 */
public class RubberBandSelection {

	private DocumentScanController documentScanController = null;

	public void setDocumentScanController(DocumentScanController documentScanController) {
		this.documentScanController = documentScanController;
	}

	final DragContext dragContext = new DragContext();
	Rectangle rect = new Rectangle();

	Group group;

	public Bounds getBounds() {
		return rect.getBoundsInParent();
	}

	public RubberBandSelection(Group group) {

//		rubberBandSelection = this;
		this.group = group;

		rect = new Rectangle(0, 0, 0, 0);
		rect.setStroke(Color.BLUE);
		rect.setStrokeWidth(1);
		rect.setStrokeLineCap(StrokeLineCap.ROUND);
		rect.setFill(Color.LIGHTBLUE.deriveColor(0, 1.2, 1, 0.6));

		group.addEventHandler(MouseEvent.MOUSE_PRESSED, onMousePressedEventHandler);
		group.addEventHandler(MouseEvent.MOUSE_DRAGGED, onMouseDraggedEventHandler);
		group.addEventHandler(MouseEvent.MOUSE_RELEASED, onMouseReleasedEventHandler);

	}

	EventHandler<MouseEvent> onMousePressedEventHandler = new EventHandler<MouseEvent>() {

		@Override
		public void handle(MouseEvent event) {

			if (event.isSecondaryButtonDown())
				return;

			// remove old rect
			rect.setX(0);
			rect.setY(0);
			rect.setWidth(0);
			rect.setHeight(0);

			group.getChildren().remove(rect);

			// prepare new drag operation
			dragContext.mouseAnchorX = event.getX();
			dragContext.mouseAnchorY = event.getY();

			rect.setX(dragContext.mouseAnchorX);
			rect.setY(dragContext.mouseAnchorY);
			rect.setWidth(0);
			rect.setHeight(0);

			group.getChildren().add(rect);

		}
	};

	EventHandler<MouseEvent> onMouseDraggedEventHandler = new EventHandler<MouseEvent>() {

		@Override
		public void handle(MouseEvent event) {

			if (event.isSecondaryButtonDown())
				return;

			double offsetX = event.getX() - dragContext.mouseAnchorX;
			double offsetY = event.getY() - dragContext.mouseAnchorY;

			if (offsetX > 0)
				rect.setWidth(offsetX);
			else {
				rect.setX(event.getX());
				rect.setWidth(dragContext.mouseAnchorX - rect.getX());
			}

			if (offsetY > 0) {
				rect.setHeight(offsetY);
			} else {
				rect.setY(event.getY());
				rect.setHeight(dragContext.mouseAnchorY - rect.getY());
			}
		}
	};

	EventHandler<MouseEvent> onMouseReleasedEventHandler = new EventHandler<MouseEvent>() {

		@SneakyThrows
		@Override
		public void handle(MouseEvent event) {

			// get bounds for image crop
			Bounds selectionBounds = getBounds();

			// crop the image
			documentScanController.save(selectionBounds);

		}
	};

	private final class DragContext {

		public double mouseAnchorX;
		public double mouseAnchorY;

	}
}