package io.mosip.registration.mdm.spec_0_9_5.dto.request;

import lombok.Data;

@Data
public class StreamRequestDTO {

	private String deviceId;
	private String deviceSubId;
	/**
	 * @param deviceId
	 * @param deviceSubId
	 */
	public StreamRequestDTO(String deviceId, String deviceSubId) {
		super();
		this.deviceId = deviceId;
		this.deviceSubId = deviceSubId;
	}
}
