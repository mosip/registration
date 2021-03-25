package io.mosip.registration.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CaCertificateDto {
        private String certId;
        private String certSubject;
        private String certIssuer;
        private String issuerId;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @JsonDeserialize(using= LocalDateTimeDeserializer.class)
        private LocalDateTime certNotBefore;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @JsonDeserialize(using=LocalDateTimeDeserializer.class)
        private LocalDateTime certNotAfter;
        private String crlUri;
        private String certData;
        private String certThumbprint;
        private String certSerialNo;
        private String partnerDomain;
        private String createdBy;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        @JsonDeserialize(using=LocalDateTimeDeserializer.class)
        private LocalDateTime createdtimes;
        private String updatedBy;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        @JsonDeserialize(using=LocalDateTimeDeserializer.class)
        private LocalDateTime updatedtimes;
        private Boolean isDeleted;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        @JsonDeserialize(using=LocalDateTimeDeserializer.class)
        private LocalDateTime deletedtimes;
}
