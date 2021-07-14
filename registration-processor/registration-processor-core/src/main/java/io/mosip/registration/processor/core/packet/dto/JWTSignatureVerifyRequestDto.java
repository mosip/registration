package io.mosip.registration.processor.core.packet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JWTSignatureVerifyRequestDto {

    private String jwtSignatureData;
    
    private String actualData;

	private String applicationId;

	private String referenceId;

	private String certificateData;

	private Boolean validateTrust;

	private String domain;

}
