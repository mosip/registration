package io.mosip.registration.processor.abis.handler.dto;

import io.mosip.registration.processor.core.common.rest.dto.BaseRestResponseDTO;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class DataShareResponseDto extends BaseRestResponseDTO {

	private static final long serialVersionUID = 1L;


    private DataShare dataShare;
    

	private List<ErrorDTO> errors;
}
