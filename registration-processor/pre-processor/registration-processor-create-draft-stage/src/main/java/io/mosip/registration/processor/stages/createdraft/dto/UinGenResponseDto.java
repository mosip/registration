package io.mosip.registration.processor.stages.createdraft.dto;

import java.util.List;

import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import lombok.Data;

/**
 * Response wrapper DTO for the UIN Generator service.
 */
@Data
public class UinGenResponseDto {

    private String id;

    private String version;

    private String responsetime;

    private String metadata;

    private UinResponseDto response;

    private List<ErrorDTO> errors;
}
