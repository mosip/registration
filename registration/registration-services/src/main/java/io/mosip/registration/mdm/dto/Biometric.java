package io.mosip.registration.mdm.dto;

import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;

public enum Biometric {
	

	LEFT_INDEX("FINGERPRINT_SLAB_LEFT", "Left Slab", "leftIndex", SingleType.FINGER, "LF_INDEX","Left IndexFinger"),
	LEFT_MIDDLE("FINGERPRINT_SLAB_LEFT", "Left Slab",  "leftMiddle", SingleType.FINGER, "LF_MIDDLE","Left MiddleFinger"),
	LEFT_RING("FINGERPRINT_SLAB_LEFT", "Left Slab", "leftRing", SingleType.FINGER, "LF_RING","Left RingFinger"),
	LEFT_LITTLE("FINGERPRINT_SLAB_LEFT", "Left Slab", "leftLittle", SingleType.FINGER, "LF_LITTLE","Left LittleFinger"),
	RIGHT_INDEX("FINGERPRINT_SLAB_RIGHT", "Right Slab", "rightIndex", SingleType.FINGER, "RF_INDEX","Right IndexFinger"),
	RIGHT_MIDDLE("FINGERPRINT_SLAB_RIGHT", "Right Slab", "rightMiddle", SingleType.FINGER, "RF_MIDDLE","Right MiddleFinger"),
	RIGHT_RING("FINGERPRINT_SLAB_RIGHT", "Right Slab", "rightRing", SingleType.FINGER, "RF_RING","Right RingFinger"),
	RIGHT_LITTLE("FINGERPRINT_SLAB_RIGHT", "Right Slab", "rightLittle", SingleType.FINGER, "RF_LITTLE","Right LittleFinger"),
	LEFT_THUMB("FINGERPRINT_SLAB_THUMBS", "Thumbs", "leftThumb", SingleType.FINGER, "LF_THUMB","Left Thumb"),
	RIGHT_THUMB("FINGERPRINT_SLAB_THUMBS", "Thumbs", "rightThumb", SingleType.FINGER, "RF_THUMB","Right Thumb"),
	RIGHT_IRIS("IRIS_DOUBLE", "Iris", "rightEye", SingleType.IRIS, "R_IRIS","Left"),
	LEFT_IRIS("IRIS_DOUBLE", "Iris", "leftEye", SingleType.IRIS, "L_IRIS","Right"),
	FACE("FACE_FULL FACE", "Face", "face", SingleType.FACE, "FACE","Face");
	
	Biometric(String modalityName, String modalityShortName, String attributeName, SingleType singleType, String mdmConstant,String specConstant) {
		this.modalityName = modalityName;
		this.setModalityShortName(modalityShortName);
		this.attributeName = attributeName;
		this.singleType = singleType;
		this.mdmConstant = mdmConstant;
		this.specConstant = specConstant;
	}
	
	private String modalityName;
	private String modalityShortName;
	private String attributeName;
	private SingleType singleType;
	private String mdmConstant;
	private String specConstant;
	
	public String getSpecConstant() {
		return specConstant;
	}
	public void setSpecConstant(String specConstant) {
		this.specConstant = specConstant;
	}
	public String getModalityName() {
		return modalityName;
	}
	public void setModalityName(String modalityName) {
		this.modalityName = modalityName;
	}
	public String getAttributeName() {
		return attributeName;
	}
	public void setAttributes(String attributeName) {
		this.attributeName = attributeName;
	}
	public SingleType getSingleType() {
		return singleType;
	}
	public void setSingleType(SingleType singleType) {
		this.singleType = singleType;
	}
	
	public String getModalityShortName() {
		return modalityShortName;
	}
	public void setModalityShortName(String modalityShortName) {
		this.modalityShortName = modalityShortName;
	}
	public String getMdmConstant() {
		return mdmConstant;
	}
	public void setMdsConstant(String mdmConstant) {
		this.mdmConstant = mdmConstant;
	}
	
	public static String getModalityNameByAttribute(String attributeName) {
		String modalityName = null;
		for(Biometric biometric : Biometric.values()) {
			if(biometric.getAttributeName().equalsIgnoreCase(attributeName)) {
				modalityName = biometric.getModalityName();
				break;
			}
		}
		return modalityName;
	}	


	
	public static Biometric getBiometricByAttribute(String attributeName) {
		Biometric constant = null;
		for(Biometric biometric : Biometric.values()) {
			if(biometric.getMdmConstant().equalsIgnoreCase(attributeName) || 
					biometric.getAttributeName().equalsIgnoreCase(attributeName)) {
				constant = biometric;
				break;
			}
		}
		return constant;
	}
	
	public static Biometric getBiometricByMDMConstant(String mdmConstant) {
		Biometric constant = null;
		for(Biometric biometric : Biometric.values()) {
			if(biometric.getMdmConstant().equalsIgnoreCase(mdmConstant)) {
				constant = biometric;
				break;
			}
		}
		return constant;
	}
	

	public static String getSpecConstantByMDMConstant(String mdmConstant) {
		String constant = null;
		for(Biometric biometric : Biometric.values()) {
			if(biometric.getMdmConstant().equalsIgnoreCase(mdmConstant)) {
				constant = biometric.getSpecConstant();
				break;
			}
		}
		return constant;
	}
	
	public static String getSpecConstantByAttributeName(String attributeName) {
		String constant = null;
		for(Biometric biometric : Biometric.values()) {
			if(biometric.getAttributeName().equalsIgnoreCase(attributeName)) {
				constant = biometric.getSpecConstant();
				break;
			}
		}
		return constant;
	}
	
	public static SingleType getSingleTypeBySpecConstant(String specConstant) {
		SingleType singleType = null;
		for(Biometric biometric : Biometric.values()) {
			if(biometric.getSpecConstant().equalsIgnoreCase(specConstant)) {
				singleType = biometric.getSingleType();
				break;
			}
		}
		return singleType;
	}
	
}
