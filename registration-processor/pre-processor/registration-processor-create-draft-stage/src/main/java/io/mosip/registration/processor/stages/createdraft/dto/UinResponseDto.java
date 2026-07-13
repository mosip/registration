package io.mosip.registration.processor.stages.createdraft.dto;

import lombok.Data;

/**
 * Response DTO containing the allocated UIN.
 */
@Data
public class UinResponseDto {

    private String uin;

    private String status;
}
