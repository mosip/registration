package io.mosip.registration.dto;

import java.util.Map;

import javafx.scene.Node;
import lombok.Data;

@Data
public class ScreenDTO {

	private Map<String, String> screenNames;
	private String screenDesc;
	private Node screenNode;

	private boolean isVisible;

	private boolean canContinue;
}
