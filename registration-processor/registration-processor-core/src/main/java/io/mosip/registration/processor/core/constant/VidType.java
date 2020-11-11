package io.mosip.registration.processor.core.constant;

public enum VidType {

	PERPETUAL("Perpetual"),


	TEMPORARY("Temporary");

	public String vidType;

	private VidType(String vidType) {
		this.vidType = vidType;
	}

	@Override
	public String toString() {
		return vidType;
	}
}
