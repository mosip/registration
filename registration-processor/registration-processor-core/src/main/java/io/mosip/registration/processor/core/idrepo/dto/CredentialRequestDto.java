package io.mosip.registration.processor.core.idrepo.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CredentialRequestDto {

	private String id;
	
	private String credentialType;
	
	private boolean encrypt;
	
	private String issuer;

	private String encryptionKey;
	
	private String recepiant;
	
	private String user;
	
    private List<String> sharableAttributes;
    
    private Map<String,Object> additionalData;
}
