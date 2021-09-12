package io.mosip.registration.processor.core.code;

/**
 * The Enum WorkflowInternalActionCode.
 */
public enum WorkflowInternalActionCode {

	/** The mark as paused. */
	MARK_AS_PAUSED,

	/** The complete as processed. */
	COMPLETE_AS_PROCESSED,

	/** The complete as rejected. */
	COMPLETE_AS_REJECTED,

	/** The complete as failed. */
	COMPLETE_AS_FAILED,

	/** The mark as reprocess. */
	MARK_AS_REPROCESS,

	/** The paused and request additional info. */
	PAUSE_AND_REQUEST_ADDITIONAL_INFO,

	/** The restart parent flow. */
	RESTART_PARENT_FLOW,
	
	/** The complete as rejected without parent flow. */
	COMPLETE_AS_REJECTED_WITHOUT_PARENT_FLOW,
	
	/** The anonymous profile flow. */
	ANONYMOUS_PROFILE,

}
