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
	STOP_PROCESSING

}
