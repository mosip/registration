package io.mosip.registration.processor.stages.operator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.RegistrationCenterUserMachineMappingHistoryResponseDto;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;

/**
 * The Class UMCValidator.
 *
 * @author Satish Gohil
 */
@Service("operatorUMCValidator")
public class UMCValidator {

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(UMCValidator.class);

	@Autowired
	private RegistrationProcessorRestClientService<Object> registrationProcessorRestService;

	ObjectMapper mapper = new ObjectMapper();

	/**
	 * Validate UMC cmapping.
	 *
	 * @param effectiveTimestamp    the effective timestamp
	 * @param registrationCenterId  the registration center id
	 * @param machineId             the machine id
	 * @param officerId             the officer id
	 * @param registrationStatusDto
	 * @throws IOException
	 * @throws BaseCheckedException
	 */
	public void validateUMCmapping(String effectiveTimestamp, String registrationCenterId, String machineId,
			String officerId, InternalRegistrationStatusDto registrationStatusDto)
			throws IOException, BaseCheckedException {

		List<String> officerpathsegments = new ArrayList<>();
		officerpathsegments.add(effectiveTimestamp);
		officerpathsegments.add(registrationCenterId);
		officerpathsegments.add(machineId);
		officerpathsegments.add(officerId);
		if (officerId != null) {
			if (!validateMapping(officerpathsegments, registrationStatusDto)) {
				throw new BaseCheckedException(StatusUtil.OFFICER_NOT_ACTIVE.getMessage(),
						StatusUtil.OFFICER_NOT_ACTIVE.getCode());
			}
		} else {
			throw new BaseCheckedException(StatusUtil.OFFICER_NOT_FOUND_PACKET.getMessage(),
					StatusUtil.OFFICER_NOT_FOUND_PACKET.getCode());
		}
	}

	private boolean validateMapping(List<String> pathsegments, InternalRegistrationStatusDto registrationStatusDto)
			throws IOException, BaseCheckedException {
		boolean isValidUser = false;
		ResponseWrapper<?> responseWrapper;
		RegistrationCenterUserMachineMappingHistoryResponseDto userDto = null;

		responseWrapper = (ResponseWrapper<?>) registrationProcessorRestService.getApi(ApiName.CENTERUSERMACHINEHISTORY,
				pathsegments, "", "", ResponseWrapper.class);
		userDto = mapper.readValue(mapper.writeValueAsString(responseWrapper.getResponse()),
				RegistrationCenterUserMachineMappingHistoryResponseDto.class);
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationStatusDto.getRegistrationId(),
				"UMCValidator::validateMapping()::CenterUserMachineHistory service ended with response data : "
						+ JsonUtil.objectMapperObjectToJson(userDto));
		if (userDto != null) {
			if (responseWrapper.getErrors() == null) {
				isValidUser = userDto.getRegistrationCenters().get(0).getIsActive();
			} else {
				List<ErrorDTO> error = responseWrapper.getErrors();
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationStatusDto.getRegistrationId(),
						"UMCValidator::validateMapping()::CenterUserMachineHistory service ended with response data : "
								+ error.get(0).getMessage());
				throw new BaseCheckedException(error.get(0).getMessage(),
						StatusUtil.CENTER_DEVICE_MAPPING_NOT_FOUND.getCode());
			}
		}

		return isValidUser;
	}

}