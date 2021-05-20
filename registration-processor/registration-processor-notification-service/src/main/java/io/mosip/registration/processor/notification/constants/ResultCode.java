package io.mosip.registration.processor.notification.constants;
public enum ResultCode {
	// If any new status is being added here then add it in
	// RegistrationStatusMapUtil also.
	/** Potential Match found in data base. */


	FAILED,

	REJECTED,

	PROCESSING,

	PROCESSED,

	REPROCESS_FAILED

}