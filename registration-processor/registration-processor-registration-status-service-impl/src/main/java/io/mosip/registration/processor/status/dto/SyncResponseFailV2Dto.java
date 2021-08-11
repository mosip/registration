package io.mosip.registration.processor.status.dto;

public class SyncResponseFailV2Dto extends SyncResponseFailDto {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7523630515203666016L;
	/** The packet id. */
	private String packetId;
	
	public SyncResponseFailV2Dto(SyncResponseFailDto syncResponseFailDto,String packetId) {
		this.packetId=packetId;
		this.setRegistrationId(syncResponseFailDto.getRegistrationId());
		this.setStatus(syncResponseFailDto.getStatus());
		this.setErrorCode(syncResponseFailDto.getErrorCode());
		this.setMessage(syncResponseFailDto.getMessage());
	}
	
	public SyncResponseFailV2Dto() {
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
