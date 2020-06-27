package io.mosip.registration.mdm.spec_0_9_2.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class RCaptureResponseDTO {

	List<RCaptureResponseBiometricsDTO> biometrics;
}
