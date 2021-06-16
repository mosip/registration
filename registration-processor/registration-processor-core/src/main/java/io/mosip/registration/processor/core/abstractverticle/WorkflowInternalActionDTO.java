package io.mosip.registration.processor.core.abstractverticle;

import java.io.Serializable;

// TODO: Auto-generated Javadoc
/**
 * The Class WorkflowInternalActionDTO.
 */
public class WorkflowInternalActionDTO extends MessageDTO implements Serializable {
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new work flow event DTO.
	 */
	public WorkflowInternalActionDTO() {
		super();
	}

	/** The rid. */
	private String rid;

	/** The action code. */
	private String actionCode;

	/** The action message. */
	private String actionMessage;

	/** The resume timestamp. */
	private String resumeTimestamp;

	/** The default resume action. */
	private String defaultResumeAction;

	/** The event timestamp. */
	private String eventTimestamp;

	/** The resume remove tags. */
	private String resumeRemoveTags;

	/** The sub process. */
	private String additionalInfoProcess;
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
	 * Gets the action code.
	 *
	 * @return the action code
	 */
	public String getActionCode() {
		return actionCode;
	}

	/**
	 * Sets the action code.
	 *
	 * @param actionCode the new action code
	 */
	public void setActionCode(String actionCode) {
		this.actionCode = actionCode;
	}

	/**
	 * Gets the action message.
	 *
	 * @return the action message
	 */
	public String getActionMessage() {
		return actionMessage;
	}

	/**
	 * Sets the action message.
	 *
	 * @param actionMessage the new action message
	 */
	public void setActionMessage(String actionMessage) {
		this.actionMessage = actionMessage;
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

	/**
	 * Gets the resume remove tags.
	 *
	 * @return the resume remove tags
	 */
	public String getResumeRemoveTags() {
		return resumeRemoveTags;
	}

	/**
	 * Sets the resume remove tags.
	 *
	 * @param resumeRemoveTags the new resume remove tags
	 */
	public void setResumeRemoveTags(String resumeRemoveTags) {
		this.resumeRemoveTags = resumeRemoveTags;
	}

	/**
	 * Gets the additional info process.
	 *
	 * @return the additional info process
	 */
	public String getAdditionalInfoProcess() {
		return additionalInfoProcess;
	}

	/**
	 * Sets the additional info process.
	 *
	 * @param additionalInfoProcess the new additional info process
	 */
	public void setAdditionalInfoProcess(String additionalInfoProcess) {
		this.additionalInfoProcess = additionalInfoProcess;
	}


}
