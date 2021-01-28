package io.mosip.registration.constants;

public enum BiometricAttributes {

	LEFT_INDEX("LeftIndexFinger", "leftIndex"), LEFT_MIDDLE("LeftMiddleFinger", "leftMiddle"), LEFT_RING("LeftRingFinger", "leftRing"),
	LEFT_LITTLE("LeftLittleFinger", "leftLittle"), RIGHT_INDEX("RightIndexFinger", "rightIndex"),
	RIGHT_MIDDLE("RightMiddleFinger", "rightMiddle"), RIGHT_RING("RightRingFinger", "rightRing"),
	RIGHT_LITTLE("RightLittleFinger", "rightLittle"), LEFT_THUMB("LeftThumb", "leftThumb"), RIGHT_THUMB("RightThumb", "rightThumb"),
	RIGHT_IRIS("Right", "rightEye"), LEFT_IRIS("Left", "leftEye"), FACE("Face", "face");

	BiometricAttributes(String subType, String attributeName) {
		this.subType = subType;
		this.attributeName = attributeName;
	}

	private String subType;
	private String attributeName;

	public String getSubType() {
		return subType;
	}

	public void setSubType(String subType) {
		this.subType = subType;
	}

	public String getAttributeName() {
		return attributeName;
	}

	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	public static String getAttributeBySubType(String subType) {
		String attributeName = null;
		for (BiometricAttributes biometricAttributes : BiometricAttributes.values()) {
			if (biometricAttributes.getSubType().equalsIgnoreCase(subType)) {
				attributeName = biometricAttributes.getAttributeName();
				break;
			}
		}
		return attributeName;
	}
}
