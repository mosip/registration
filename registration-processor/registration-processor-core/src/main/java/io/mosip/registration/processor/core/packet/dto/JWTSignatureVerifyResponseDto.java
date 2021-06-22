package io.mosip.registration.processor.core.packet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JWTSignatureVerifyResponseDto {
    
    /**
     * The Signature verification status.
    */
    private boolean signatureValid;
    
	/**
	 * The Signature validation message.
	 */
    private String message;
    
    /**
	 * The Trust validation status.
	 */
    private String trustValid;
}
