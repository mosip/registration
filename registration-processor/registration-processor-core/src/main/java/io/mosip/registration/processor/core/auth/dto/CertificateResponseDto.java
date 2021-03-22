package io.mosip.registration.processor.core.auth.dto;

import lombok.Data;

@Data
public class CertificateResponseDto {

    String certificate;

    String certSignRequest;
    String issuedAt;
    String expiryAt;
    String timestamp;

}
