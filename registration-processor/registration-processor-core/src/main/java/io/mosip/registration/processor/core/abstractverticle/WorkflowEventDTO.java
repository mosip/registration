package io.mosip.registration.processor.core.abstractverticle;

import java.io.Serializable;

// TODO: Auto-generated Javadoc
/**
 * The Class WorkFlowEventDTO.
 */
public class WorkflowEventDTO extends MessageDTO implements Serializable {


	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new work flow event DTO.
	 */
	public WorkflowEventDTO() {
		super();
	}

	/** The rid. */
	private String rid;

	/** The status code. */
	private String statusCode;

	/** The status comment. */
	private String statusComment;

	/** The resume timestamp. */
	private String resumeTimestamp;

	/** The default resume action. */
	private String defaultResumeAction;

	/** The event timestamp. */
	private String eventTimestamp;



	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.processor.core.abstractverticle.MessageDTO#getRid()
	 */
	public String getRid() {
		return rid;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.processor.core.abstractverticle.MessageDTO#setRid(java.
	 * lang.String)
	 */
	public void setRid(String rid) {
		this.rid = rid;
	}

	/**
	 * Gets the status code.
	 *
	 * @return the status code
	 */
	public String getStatusCode() {
		return statusCode;
	}

	/**
	 * Sets the status code.
	 *
	 * @param statusCode the new status code
	 */
	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}

	/**
	 * Gets the status comment.
	 *
	 * @return the status comment
	 */
	public String getStatusComment() {
		return statusComment;
	}

	/**
	 * Sets the status comment.
	 *
	 * @param statusComment the new status comment
	 */
	public void setStatusComment(String statusComment) {
		this.statusComment = statusComment;
	}

	/**
	 * Gets the resume timestamp.
	 *
	 * @return the resume timestamp
	 */
	public String getResumeTimestamp() {
		return resumeTimestamp;
	}

	/**
	 * Sets the resume timestamp.
	 *
	 * @param resumeTimestamp the new resume timestamp
	 */
	public void setResumeTimestamp(String resumeTimestamp) {
		this.resumeTimestamp = resumeTimestamp;
	}

	/**
	 * Gets the default resume action.
	 *
	 * @return the default resume action
	 */
	public String getDefaultResumeAction() {
		return defaultResumeAction;
	}

	/**
	 * Sets the default resume action.
	 *
	 * @param defaultResumeAction the new default resume action
	 */
	public void setDefaultResumeAction(String defaultResumeAction) {
		this.defaultResumeAction = defaultResumeAction;
	}

	/**
	 * Gets the event timestamp.
	 *
	 * @return the event timestamp
	 */
	public String getEventTimestamp() {
		return eventTimestamp;
	}

	/**
	 * Sets the event timestamp.
	 *
	 * @param eventTimestamp the new event timestamp
	 */
	public void setEventTimestamp(String eventTimestamp) {
		this.eventTimestamp = eventTimestamp;
	}

}
