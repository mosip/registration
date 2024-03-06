package io.mosip.registration.processor.status.code;

/**
 * Valid Status codes for Registration status table.
 *
 * @author Girish Yarru
 *
 */
public enum RegistrationStatusCode {
	// If any new status is being added here then add it in
	// RegistrationStatusMapUtil also.
	/** Potential Match found in data base. */


	FAILED,

	REJECTED,

	PROCESSING,

	PROCESSED,

	REPROCESS_FAILED,

	PAUSED,

	RESUMABLE,

	REPROCESS,

	PAUSED_FOR_ADDITIONAL_INFO

}
