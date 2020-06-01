package io.mosip.registration.processor.core.constant;

public enum IdObjectValidatorSupportedOperations {
	NEW_REGISTRATION("new-registration"),

	CHILD_REGISTRATION("child-registration"),

	OTHER("other"),

	LOST("lost");

	private String operation;

	IdObjectValidatorSupportedOperations(String operation) {
		this.operation = operation;
	}

	public String getOperation() {
		return operation;
	}
}
