package io.mosip.registration.device.scanner.dto;

import lombok.Data;

@Data
public class ScanDevice {

	private String id;
	private String name;
	private boolean isWIA;
	private boolean isWebCam;
}
