package io.mosip.registration.processor.core.digital.signature.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author Md Tarique Azeez
 * @since 1.3.0-SNAPSHOT
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JWTSignatureResponseDto {

    /**
     * encrypted data
     */
    private String jwtSignedData;

    /**
     * response time.
     */
    private LocalDateTime timestamp;
}
