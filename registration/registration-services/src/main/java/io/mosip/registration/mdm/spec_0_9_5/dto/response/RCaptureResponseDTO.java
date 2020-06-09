package io.mosip.registration.mdm.spec_0_9_5.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class RCaptureResponseDTO {

	List<RCaptureResponseBiometricsDTO> biometrics;
}
