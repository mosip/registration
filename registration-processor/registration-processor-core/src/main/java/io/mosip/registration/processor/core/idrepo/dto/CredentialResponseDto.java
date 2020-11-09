package io.mosip.registration.processor.core.idrepo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CredentialResponseDto {
	
	private String id;
	private String requestId;
}
