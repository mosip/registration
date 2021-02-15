package io.mosip.registration.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CaCertificateDto {
        private String certId;
        private String certSubject;
        private String certIssuer;
        private String issuerId;
        private LocalDateTime certNotBefore;
        private LocalDateTime certNotAfter;
        private String crlUri;
        private String certData;
        private String certThumbprint;
        private String certSerialNo;
        private String partnerDomain;
        private String createdBy;
        private LocalDateTime createdtimes;
        private String updatedBy;
        private LocalDateTime updatedtimes;
        private Boolean isDeleted;
        private LocalDateTime deletedtimes;
}
