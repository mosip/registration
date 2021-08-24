package io.mosip.registration.processor.core.abstractverticle;

import java.io.Serializable;
import java.util.Map;

/**
 * This class contains parameters for communication between MOSIP stages.
 *
 * @author Pranav Kumar
 * @since 0.0.1
 */
public class MessageDTO implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;
	/** The registration type. */
	private String reg_type;
	/** The rid. */
	private String rid;

	/** The is valid. */
	private Boolean isValid;

	/** The internal error. */
	private Boolean internalError;

	/** The message bus address. */
	private MessageBusAddress messageBusAddress;

	/** The retry count. */
	private Integer retryCount;

	/** The tags of the packets */
	private Map<String, String> tags;

	/** The timestamp when last stage hop was completed */
	private String lastHopTimestamp;

	private String source;

	private int iteration;

	private String workflowInstanceId;

	/**
	 * Instantiates a new message DTO.
	 */
	public MessageDTO() {
		super();
	}



	public String getReg_type() {
		return reg_type;
	}

	public void setReg_type(String reg_type) {
		this.reg_type = reg_type;
	}

	/**
	 * Gets the rid.
	 *
	 * @return the rid
	 */

	public String getRid() {
		return rid;
	}

	/**
	 * Sets the rid.
	 *
	 * @param rid
	 *            the new rid
	 */
	public void setRid(String rid) {
		this.rid = rid;
	}

	/**
	 * Gets the checks if is valid.
	 *
	 * @return the checks if is valid
	 */
	public Boolean getIsValid() {
		return isValid;
	}

	/**
	 * Sets the checks if is valid.
	 *
	 * @param isValid
	 *            the new checks if is valid
	 */
	public void setIsValid(Boolean isValid) {
		this.isValid = isValid;
	}

	/**
	 * Gets the internal error.
	 *
	 * @return the internal error
	 */
	public Boolean getInternalError() {
		return internalError;
	}

	/**
	 * Sets the internal error.
	 *
	 * @param internalError
	 *            the new internal error
	 */
	public void setInternalError(Boolean internalError) {
		this.internalError = internalError;
	}

	/**
	 * Gets the retry count.
	 *
	 * @return the retry count
	 */
	public Integer getRetryCount() {
		return retryCount;
	}

	/**
	 * Sets the retry count.
	 *
	 * @param retryCount
	 *            the new retry count
	 */
	public void setRetryCount(Integer retryCount) {
		this.retryCount = retryCount;
	}

	/**
	 * Sets the message bus address.
	 *
	 * @param messageBusAddress
	 *            the new message bus address
	 */
	public void setMessageBusAddress(MessageBusAddress messageBusAddress) {
		this.messageBusAddress = messageBusAddress;
	}

	/**
	 * Gets the message bus address.
	 *
	 * @return the message bus address
	 */
	public MessageBusAddress getMessageBusAddress() {
		return messageBusAddress;
	}

	/**
	 * Gets the message tags
	 *
	 * @return the tags map
	 */
	public Map<String, String> getTags() {
		return tags;
	}

	/**
	 * Sets the tags map.
	 *
	 * @param tags the message tags
	 */
	public void setTags(Map<String, String> tags) {
		this.tags = tags;
	}

	/**
	 * Gets the last hop timestamp
	 *
	 * @return last hop timestamp in ISO format
	 */
	public String getLastHopTimestamp() {
		return lastHopTimestamp;
	}

	/**
	 * Sets the last hop timestamp.
	 *
	 * @param lastHopTimestamp the timestamp in ISO format
	 */
	public void setLastHopTimestamp(String lastHopTimestamp) {
		this.lastHopTimestamp = lastHopTimestamp;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public int getIteration() {
		return iteration;
	}

	public void setIteration(int iteration) {
		this.iteration = iteration;
	}

	public String getWorkflowInstanceId() {
		return workflowInstanceId;
	}

	public void setWorkflowInstanceId(String workflowInstanceId) {
		this.workflowInstanceId = workflowInstanceId;
	}

	@Override
	public String toString() {
		String msgBusAddress=null;
		
		if(messageBusAddress!=null) {
			msgBusAddress = messageBusAddress.getAddress(); 
		}
		return "MessageDTO{" + "reg_type='" + reg_type + '\'' + ", rid='" + rid + '\'' + ", isValid=" + isValid
				+ ", internalError=" + internalError + ", messageBusAddress=" + msgBusAddress
				+ ", retryCount=" + retryCount + '}';
	}

}
