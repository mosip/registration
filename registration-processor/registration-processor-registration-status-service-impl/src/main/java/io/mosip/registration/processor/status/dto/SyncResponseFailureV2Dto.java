package io.mosip.registration.processor.status.dto;

public class SyncResponseFailureV2Dto extends SyncResponseDto {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5799573108161476883L;
	/** The packet id. */
	private String packetId;
	/** The errror code. */
	private String errorCode;
	/** The message . */
	private String message;
	
	
	public SyncResponseFailureV2Dto(String registrationId,String status,String errorCode,String message,String packetId) {
		this.setPacketId(packetId);
		this.setRegistrationId(registrationId);
		this.setStatus(status);
		this.setErrorCode(errorCode);
		this.setMessage(message);
	}
	public SyncResponseFailureV2Dto() {
		super();
	}
	/**
	 * @return the packetId
	 */
	public String getPacketId() {
		return packetId;
	}
	/**
	 * @param packetId the packetId to set
	 */
	public void setPacketId(String packetId) {
		this.packetId = packetId;
	}
	/**
	 * @return the errorCode
	 */
	public String getErrorCode() {
		return errorCode;
	}
	/**
	 * @param errorCode the errorCode to set
	 */
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}
	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}
	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}
}
