package io.mosip.registration.processor.adjudication.dto;

import lombok.Data;

import java.util.Map;

@Data
public class DataShareRequestDto {

    private String biometrics;

    private Map<String, String> identity;

    private Map<String, String> documents;

    private String metaInfo;

    private String audits;
}
