package io.mosip.registration.processor.status.dto;

public class SyncResponseSuccessDto extends SyncResponseDto {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 4422198670538094471L;

	/** The registration id. */
	private String packetId;

	/**
	 * Gets the registration id.
	 *
	 * @return the registration id
	 */
	public String getPacketIdId() {
		return packetId;
	}

	/**
	 * Sets the registration id.
	 *
	 * @param registrationId
	 *            the new registration id
	 */
	public void setPacketId(String packetId) {
		this.packetId = packetId;
	}

}
