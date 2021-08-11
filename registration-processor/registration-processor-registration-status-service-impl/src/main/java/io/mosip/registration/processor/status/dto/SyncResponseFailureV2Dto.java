package io.mosip.registration.processor.status.dto;

public class SyncResponseFailureV2Dto extends SyncResponseFailureDto {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5799573108161476883L;
	/** The packet id. */
	private String packetId;
	
	
	public SyncResponseFailureV2Dto(SyncResponseFailureDto syncResponseFailureDto,String packetId) {
		this.packetId=packetId;
		this.setRegistrationId(syncResponseFailureDto.getRegistrationId());
		this.setStatus(syncResponseFailureDto.getStatus());
		this.setErrorCode(syncResponseFailureDto.getErrorCode());
		this.setMessage(syncResponseFailureDto.getMessage());
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
}
