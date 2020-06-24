package io.mosip.registration.mdm.dto;

import java.util.LinkedList;
import java.util.List;

import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;
import io.mosip.registration.constants.RegistrationConstants;

public enum Biometric {

	// 095 Spec Constants
	LEFT_INDEX_095("FINGERPRINT_SLAB_LEFT", "Left Slab", "leftIndex", SingleType.FINGER, "Left IndexFinger",
			"Left IndexFinger", RegistrationConstants.SPEC_VERSION_095), LEFT_MIDDLE_095("FINGERPRINT_SLAB_LEFT",
					"Left Slab", "leftMiddle", SingleType.FINGER, "Left MiddleFinger", "Left MiddleFinger",
					RegistrationConstants.SPEC_VERSION_095), LEFT_RING_095("FINGERPRINT_SLAB_LEFT", "Left Slab",
							"leftRing", SingleType.FINGER, "Left RingFinger", "Left RingFinger",
							RegistrationConstants.SPEC_VERSION_095), LEFT_LITTLE_095("FINGERPRINT_SLAB_LEFT",
									"Left Slab", "leftLittle", SingleType.FINGER, "Left LittleFinger",
									"Left LittleFinger", RegistrationConstants.SPEC_VERSION_095), RIGHT_INDEX_095(
											"FINGERPRINT_SLAB_RIGHT", "Right Slab", "rightIndex", SingleType.FINGER,
											"Right IndexFinger", "Right IndexFinger",
											RegistrationConstants.SPEC_VERSION_095), RIGHT_MIDDLE_095(
													"FINGERPRINT_SLAB_RIGHT", "Right Slab", "rightMiddle",
													SingleType.FINGER, "Right MiddleFinger", "Right MiddleFinger",
													RegistrationConstants.SPEC_VERSION_095), RIGHT_RING_095(
															"FINGERPRINT_SLAB_RIGHT", "Right Slab", "rightRing",
															SingleType.FINGER, "Right RingFinger", "Right RingFinger",
															RegistrationConstants.SPEC_VERSION_095), RIGHT_LITTLE_095(
																	"FINGERPRINT_SLAB_RIGHT", "Right Slab",
																	"rightLittle", SingleType.FINGER,
																	"Right LittleFinger", "Right LittleFinger",
																	RegistrationConstants.SPEC_VERSION_095), LEFT_THUMB_095(
																			"FINGERPRINT_SLAB_THUMBS", "Thumbs",
																			"leftThumb", SingleType.FINGER,
																			"Left Thumb", "Left Thumb",
																			RegistrationConstants.SPEC_VERSION_095), RIGHT_THUMB_095(
																					"FINGERPRINT_SLAB_THUMBS", "Thumbs",
																					"rightThumb", SingleType.FINGER,
																					"Right Thumb", "Right Thumb",
																					RegistrationConstants.SPEC_VERSION_095), RIGHT_IRIS_095(
																							"IRIS_DOUBLE", "Iris",
																							"rightEye", SingleType.IRIS,
																							"Right", "Right",
																							RegistrationConstants.SPEC_VERSION_095), LEFT_IRIS_095(
																									"IRIS_DOUBLE",
																									"Iris", "leftEye",
																									SingleType.IRIS,
																									"Left", "Left",
																									RegistrationConstants.SPEC_VERSION_095), FACE_095(
																											"FACE_FULL FACE",
																											"Face",
																											"face",
																											SingleType.FACE,
																											"FACE",
																											"Face",
																											RegistrationConstants.SPEC_VERSION_095),

	// 092 Spec Constants
	LEFT_INDEX_092("FINGERPRINT_SLAB_LEFT", "Left Slab", "leftIndex", SingleType.FINGER, "LF_INDEX", "LEFT INDEX",
			RegistrationConstants.SPEC_VERSION_092), LEFT_MIDDLE_092("FINGERPRINT_SLAB_LEFT", "Left Slab", "leftMiddle",
					SingleType.FINGER, "LF_MIDDLE", "LEFT MIDDLE",
					RegistrationConstants.SPEC_VERSION_092), LEFT_RING_092("FINGERPRINT_SLAB_LEFT", "Left Slab",
							"leftRing", SingleType.FINGER, "LF_RING", "LEFT RING",
							RegistrationConstants.SPEC_VERSION_092), LEFT_LITTLE_092("FINGERPRINT_SLAB_LEFT",
									"Left Slab", "leftLittle", SingleType.FINGER, "LF_LITTLE", "LEFT LITTLE",
									RegistrationConstants.SPEC_VERSION_092), RIGHT_INDEX_092("FINGERPRINT_SLAB_RIGHT",
											"Right Slab", "rightIndex", SingleType.FINGER, "RF_INDEX", "RIGHT INDEX",
											RegistrationConstants.SPEC_VERSION_092), RIGHT_MIDDLE_092(
													"FINGERPRINT_SLAB_RIGHT", "Right Slab", "rightMiddle",
													SingleType.FINGER, "RF_MIDDLE", "RIGHT MIDDLE",
													RegistrationConstants.SPEC_VERSION_092), RIGHT_RING_092(
															"FINGERPRINT_SLAB_RIGHT", "Right Slab", "rightRing",
															SingleType.FINGER, "RF_RING", "RIGHT RING",
															RegistrationConstants.SPEC_VERSION_092), RIGHT_LITTLE_092(
																	"FINGERPRINT_SLAB_RIGHT", "Right Slab",
																	"rightLittle", SingleType.FINGER, "RF_LITTLE",
																	"RIGHT LITTLE",
																	RegistrationConstants.SPEC_VERSION_092), LEFT_THUMB_092(
																			"FINGERPRINT_SLAB_THUMBS", "Thumbs",
																			"leftThumb", SingleType.FINGER, "LF_THUMB",
																			"LEFT THUMB",
																			RegistrationConstants.SPEC_VERSION_092), RIGHT_THUMB_092(
																					"FINGERPRINT_SLAB_THUMBS", "Thumbs",
																					"rightThumb", SingleType.FINGER,
																					"RF_THUMB", "RIGHT THUMB",
																					RegistrationConstants.SPEC_VERSION_092), RIGHT_IRIS_092(
																							"IRIS_DOUBLE", "Iris",
																							"rightEye", SingleType.IRIS,
																							"R_IRIS", "RIGHT IRIS",
																							RegistrationConstants.SPEC_VERSION_092), LEFT_IRIS_092(
																									"IRIS_DOUBLE",
																									"Iris", "leftEye",
																									SingleType.IRIS,
																									"L_IRIS",
																									"LEFT IRIS",
																									RegistrationConstants.SPEC_VERSION_092), FACE_092(
																											"FACE_FULL FACE",
																											"Face",
																											"face",
																											SingleType.FACE,
																											"FACE",
																											"Face",
																											RegistrationConstants.SPEC_VERSION_092),;

	Biometric(String modalityName, String modalityShortName, String uiSchemaAttributeName, SingleType singleType,
			String mdmRequestAttributeName, String mdmResponseAttributeName, String specVersion) {
		this.modalityName = modalityName;
		this.modalityShortName = modalityShortName;
		this.uiSchemaAttributeName = uiSchemaAttributeName;
		this.singleType = singleType;
		this.mdmRequestAttributeName = mdmRequestAttributeName;
		this.mdmResponseAttributeName = mdmResponseAttributeName;
		this.specVersion = specVersion;
	}

	private String modalityName;
	private String modalityShortName;
	private String uiSchemaAttributeName;
	private SingleType singleType;
	private String mdmRequestAttributeName;
	private String mdmResponseAttributeName;
	private String specVersion;

	public String getSpecVersion() {
		return specVersion;
	}

	public void setSpecVersion(String specVersion) {
		this.specVersion = specVersion;
	}

	public String getModalityName() {
		return modalityName;
	}

	public void setModalityName(String modalityName) {
		this.modalityName = modalityName;
	}

	public String getModalityShortName() {
		return modalityShortName;
	}

	public void setModalityShortName(String modalityShortName) {
		this.modalityShortName = modalityShortName;
	}

	public String getUiSchemaAttributeName() {
		return uiSchemaAttributeName;
	}

	public void setUiSchemaAttributeName(String uiSchemaAttributeName) {
		this.uiSchemaAttributeName = uiSchemaAttributeName;
	}

	public SingleType getSingleType() {
		return singleType;
	}

	public void setSingleType(SingleType singleType) {
		this.singleType = singleType;
	}

	public String getMdmRequestAttributeName() {
		return mdmRequestAttributeName;
	}

	public void setMdmRequestAttributeName(String mdmRequestAttributeName) {
		this.mdmRequestAttributeName = mdmRequestAttributeName;
	}

	public String getMdmResponseAttributeName() {
		return mdmResponseAttributeName;
	}

	public void setMdmResponseAttributeName(String mdmResponseAttributeName) {
		this.mdmResponseAttributeName = mdmResponseAttributeName;
	}

	public static String getmdmRequestAttributeName(String uiSchemaAttribute, String specVersion) {
		String mdmRequestAttribute = null;
		for (Biometric biometric : Biometric.values()) {
			if (biometric.getUiSchemaAttributeName().equalsIgnoreCase(uiSchemaAttribute)
					&& biometric.getSpecVersion().equalsIgnoreCase(specVersion)) {
				mdmRequestAttribute = biometric.getMdmRequestAttributeName();
				break;
			}
		}
		return mdmRequestAttribute;
	}

	public static String getUiSchemaAttributeName(String mdmResponseAttributeName, String specVersion) {
		String constant = null;

		if (specVersion.equals(RegistrationConstants.SPEC_VERSION_092)) {
			if (mdmResponseAttributeName.toLowerCase().contains("iris")
					|| mdmResponseAttributeName.toLowerCase().contains("eye")) {
				if (mdmResponseAttributeName.toLowerCase().contains("l")) {
					return "leftEye";
				} else {
					return "rightEye";
				}
			} else if (mdmResponseAttributeName.toLowerCase().contains("face")) {
				return "face";
			}
		}
		for (Biometric biometric : Biometric.values()) {
			if (biometric.getMdmResponseAttributeName().equalsIgnoreCase(mdmResponseAttributeName)
					&& biometric.getSpecVersion().equalsIgnoreCase(specVersion)) {
				constant = biometric.getUiSchemaAttributeName();
				break;
			}
		}
		return constant;
	}

	public static String getmdmResponseAttributeName(String uiSchemaAttribute, String specVersion) {
		String mdmRequestAttribute = null;
		for (Biometric biometric : Biometric.values()) {
			if (biometric.getUiSchemaAttributeName().equalsIgnoreCase(uiSchemaAttribute)
					&& biometric.getSpecVersion().equalsIgnoreCase(specVersion)) {
				mdmRequestAttribute = biometric.getMdmResponseAttributeName();
				break;
			}
		}
		return mdmRequestAttribute;
	}

	public static List<String> getAvailableSpecVersions() {
		List<String> specVersionsList = new LinkedList<>();
		for (Biometric biometric : Biometric.values()) {
			if (!specVersionsList.contains(biometric.getSpecVersion())) {
				specVersionsList.add(biometric.getSpecVersion());
			}
		}
		return specVersionsList;
	}

	public static SingleType getSingleTypeBySpecConstant(String uiSchemaAttribute) {
		SingleType singleType = null;
		for (Biometric biometric : Biometric.values()) {
			if (biometric.getUiSchemaAttributeName().equalsIgnoreCase(uiSchemaAttribute)) {
				singleType = biometric.getSingleType();
				break;
			}
		}
		return singleType;
	}

}
