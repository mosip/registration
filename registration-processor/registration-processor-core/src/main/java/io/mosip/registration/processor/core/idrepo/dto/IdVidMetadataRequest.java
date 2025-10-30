package io.mosip.registration.processor.core.idrepo.dto;

import lombok.Data;

@Data
public class IdVidMetadataRequest {

    private String individualId;
    private String idType;
}
