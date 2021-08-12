package io.mosip.registration.processor.status.dto;

public class SyncResponseSuccessV2Dto extends SyncResponseDto {

	/**
	 * 
	 */
	private static final long serialVersionUID = -94470774846847861L;
	/** The packet id. */
	private String packetId;
	
	public SyncResponseSuccessV2Dto(String registrationId,String status,String packetId) {
		this.setPacketId(packetId);
		this.setRegistrationId(registrationId);
		this.setStatus(status);
	}
	public SyncResponseSuccessV2Dto() {
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
}
