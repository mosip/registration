package io.mosip.registration.processor.stages.dto;

import java.util.List;

import io.mosip.registration.processor.core.packet.dto.demographicinfo.DemographicInfoDto;
import lombok.Data;

@Data
public class DemoDedupeStatusDTO {

	private boolean isTransactionSuccessful;
	List<DemographicInfoDto> duplicateDtos;
}
