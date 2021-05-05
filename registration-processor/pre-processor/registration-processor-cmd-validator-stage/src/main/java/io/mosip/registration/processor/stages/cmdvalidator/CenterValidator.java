package io.mosip.registration.processor.stages.cmdvalidator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.RegOsiDto;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.RegistartionCenterTimestampResponseDto;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.RegistrationCenterResponseDto;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.util.JsonUtil;

@Service
public class CenterValidator {

	private static Logger regProcLogger = RegProcessorLogger.getLogger(CenterValidator.class);

	private static final String VALID = "Valid";

	ObjectMapper mapper = new ObjectMapper();

	@Value("${mosip.workinghour.validation.required}")
	private Boolean isWorkingHourValidationRequired;

	@Autowired
	private RegistrationProcessorRestClientService<Object> registrationProcessorRestService;

	/**
	 * Validate registration center.
	 *
	 * @param registrationCenterId  the registration center id
	 * @param langCode              the lang code
	 * @param effectiveDate         the effective date
	 * @param registrationStatusDto
	 * @return true, if successful
	 * @throws IOException
	 * @throws BaseCheckedException
	 * @throws JsonProcessingException
	 * @throws com.fasterxml.jackson.databind.JsonMappingException
	 * @throws com.fasterxml.jackson.core.JsonParseException
	 */
	private boolean isValidRegistrationCenter(String registrationCenterId, String langCode, String effectiveDate,
			String registrationId) throws IOException, BaseCheckedException, ApisResourceAccessException {
		boolean activeRegCenter = false;
		if (registrationCenterId == null || effectiveDate == null) {
			throw new BaseCheckedException(StatusUtil.CENTER_ID_NOT_FOUND.getMessage(),
					StatusUtil.CENTER_ID_NOT_FOUND.getCode());
		}
		List<String> pathsegments = new ArrayList<>();
		pathsegments.add(registrationCenterId);
		pathsegments.add(langCode);
		pathsegments.add(effectiveDate);
		RegistrationCenterResponseDto rcpdto = null;
		ResponseWrapper<?> responseWrapper;

		responseWrapper = (ResponseWrapper<?>) registrationProcessorRestService.getApi(ApiName.CENTERHISTORY,
				pathsegments, "", "", ResponseWrapper.class);
		rcpdto = mapper.readValue(mapper.writeValueAsString(responseWrapper.getResponse()),
				RegistrationCenterResponseDto.class);

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId,
				"CenterValidator::isValidRegistrationCenter()::CenterHistory service ended with response data : "
						+ JsonUtil.objectMapperObjectToJson(rcpdto));

		if (responseWrapper.getErrors() == null) {
			activeRegCenter = rcpdto.getRegistrationCentersHistory().get(0).getIsActive();
			if (!activeRegCenter) {
				throw new BaseCheckedException(StatusUtil.CENTER_ID_INACTIVE.getMessage(),
						StatusUtil.CENTER_ID_INACTIVE.getCode());
			}
		} else {
			List<ErrorDTO> error = responseWrapper.getErrors();
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId,
					"CenterValidator::isValidRegistrationCenter()::CenterHistory service ended with response data : "
							+ error.get(0).getMessage());
			throw new BaseCheckedException(error.get(0).getMessage(), StatusUtil.FAILED_TO_GET_CENTER_DETAIL.getCode());
		}

		return activeRegCenter;

	}

	/**
	 * Checks if is valid center id timestamp.
	 * 
	 * @param registrationStatusDto
	 *
	 * @param rcmDto                the RegistrationCenterMachineDto
	 * @param registrationStatusDto the InternalRegistrationStatusDto
	 * @return true, if is valid center id timestamp
	 * @throws IOException
	 * @throws BaseCheckedException
	 * @throws JsonProcessingException
	 * @throws com.fasterxml.jackson.databind.JsonMappingException
	 * @throws com.fasterxml.jackson.core.JsonParseException
	 *
	 */

	private boolean validateCenterIdAndTimestamp(RegOsiDto rcmDto, String primaryLanguagecode, String registrationId)
			throws IOException, BaseCheckedException, ApisResourceAccessException {
		boolean isValid = false;

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				rcmDto.getRegId(), "CenterValidator::validateCenterIdAndTimestamp()::entry");
		List<String> pathsegments = new ArrayList<>();
		pathsegments.add(rcmDto.getRegcntrId());
		pathsegments.add(primaryLanguagecode);
		pathsegments.add(rcmDto.getPacketCreationDate());
		ResponseWrapper<?> responseWrapper;
		RegistartionCenterTimestampResponseDto result;

		responseWrapper = (ResponseWrapper<?>) registrationProcessorRestService
				.getApi(ApiName.REGISTRATIONCENTERTIMESTAMP, pathsegments, "", "", ResponseWrapper.class);

		result = mapper.readValue(mapper.writeValueAsString(responseWrapper.getResponse()),
				RegistartionCenterTimestampResponseDto.class);
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId,
				"CenterValidator::isDeviceActive()::CenterUserMachineHistory service ended with response data : "
						+ JsonUtil.objectMapperObjectToJson(result));
		if (responseWrapper.getErrors() == null) {
			if (result.getStatus().equals(VALID)) {
				isValid = true;
			} else {
				throw new BaseCheckedException(
						StatusUtil.PACKET_CREATION_WORKING_HOURS.getMessage() + rcmDto.getPacketCreationDate(),
						StatusUtil.PACKET_CREATION_WORKING_HOURS.getCode());
			}
		} else {
			List<ErrorDTO> error = responseWrapper.getErrors();
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId,
					"CenterValidator::isDeviceActive()::CenterUserMachineHistory service ended with response data : "
							+ error.get(0).getMessage());
			throw new BaseCheckedException(error.get(0).getMessage(),
					StatusUtil.REGISTRATION_CENTER_TIMESTAMP_FAILURE.getCode());
		}

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				rcmDto.getRegId(), "CenterValidator::validateCenterIdAndTimestamp()::exit");
		return isValid;
	}

	public boolean isValidCenterDetails(String primaryLanguagecode, RegOsiDto regOsi, String registrationId)
			throws IOException, BaseCheckedException {
		return isValidRegistrationCenter(regOsi.getRegcntrId(), primaryLanguagecode, regOsi.getPacketCreationDate(),
				registrationId) && validateCenterIdAndTimestamp(regOsi, primaryLanguagecode, registrationId);
	}

}
