package io.mosip.registration.constants;

public enum ActiveProfiles {
	
	DEV("dev"),
	QA("qa");
	
	/**
	 * Instantiates active profile.
	 *
	 * @param code the code
	 */
	private ActiveProfiles(String code) {
		this.code=code;
	}
	
	/** The code. */
	private final String code;

	/**
	 * @return the code
	 */
	public String getCode() {
		return code;
	}
}


