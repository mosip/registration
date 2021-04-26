package io.mosip.registration.processor.core.code;


/**
 * The Enum WorkflowActionCode.
 */
public enum WorkflowActionCode {

	/** The resume processing. */
	RESUME_PROCESSING, 

	/** The resume processing and remove hotlisted tag. */
	RESUME_PROCESSING_AND_REMOVE_HOTLISTED_TAG,

	/** The resume from beginning. */
	RESUME_FROM_BEGINNING,

	/** The resume from beginning and remove hotlisted tag. */
	RESUME_FROM_BEGINNING_AND_REMOVE_HOTLISTED_TAG,

	/** The stop processing. */
	STOP_PROCESSING,

	PAUSE_AND_REQUEST_ADDITIONAL_INFO,

	STOP_AND_RESUME_PARENT_FLOW,
	STOP_AND_RESTART_PARENT_FLOW,
	STOP_AND_NOTIFY

}
