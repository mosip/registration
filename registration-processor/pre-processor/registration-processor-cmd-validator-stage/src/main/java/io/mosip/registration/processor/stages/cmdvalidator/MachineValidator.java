package io.mosip.registration.processor.stages.cmdvalidator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.ValidationFailedException;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.MachineHistoryDto;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.MachineHistoryResponseDto;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.util.JsonUtil;

@Service
public class MachineValidator {

	private static Logger regProcLogger = RegProcessorLogger.getLogger(MachineValidator.class);

	@Autowired
	ObjectMapper mapper;

	@Autowired
	private RegistrationProcessorRestClientService<Object> registrationProcessorRestService;

	/**
	 * Validate machine.
	 *
	 * @param machineId             the machine id
	 * @param langCode              the lang code
	 * @param effdatetimes          the effdatetimes
	 * @param registrationStatusDto
	 * @throws IOException
	 * @throws BaseCheckedException
	 * @throws JsonProcessingException
	 * @throws com.fasterxml.jackson.databind.JsonMappingException
	 * @throws com.fasterxml.jackson.core.JsonParseException
	 */
	public void validate(String machineId, String langCode, String effdatetimes, String registrationId)
			throws IOException, BaseCheckedException, ApisResourceAccessException {

		if (machineId == null) {
			throw new BaseCheckedException(StatusUtil.MACHINE_ID_NOT_FOUND.getMessage(),
					StatusUtil.MACHINE_ID_NOT_FOUND.getCode());
		}

		List<String> pathsegments = new ArrayList<>();
		pathsegments.add(machineId);
		pathsegments.add(langCode);
		pathsegments.add(effdatetimes);
		MachineHistoryResponseDto mhrdto;
		ResponseWrapper<?> responseWrapper;

		responseWrapper = (ResponseWrapper<?>) registrationProcessorRestService.getApi(ApiName.MACHINEHISTORY,
				pathsegments, "", "", ResponseWrapper.class);
		mhrdto = mapper.readValue(mapper.writeValueAsString(responseWrapper.getResponse()),
				MachineHistoryResponseDto.class);
		if (responseWrapper.getErrors() == null) {

			mhrdto.setMachineHistoryDetails(mhrdto.getMachineHistoryDetails().stream().filter(m ->
			m!=null && m.getId()!=null  && m.getId().equalsIgnoreCase(machineId)).collect(Collectors.toList()));

			if (mhrdto.getMachineHistoryDetails()!=null && !mhrdto.getMachineHistoryDetails().isEmpty()) {

				mhrdto.setMachineHistoryDetails(mhrdto.getMachineHistoryDetails().stream().filter(m ->
				 m.getIsActive()!=null  && m.getIsActive() ).collect(Collectors.toList()));

				if (mhrdto.getMachineHistoryDetails()==null || mhrdto.getMachineHistoryDetails().isEmpty()) {
					throw new ValidationFailedException(StatusUtil.MACHINE_ID_NOT_ACTIVE.getMessage() + machineId,
							StatusUtil.MACHINE_ID_NOT_ACTIVE.getCode());
				}

			} else {
				throw new BaseCheckedException(StatusUtil.MACHINE_ID_NOT_FOUND_MASTER_DB.getMessage(),
						StatusUtil.MACHINE_ID_NOT_FOUND_MASTER_DB.getCode());
			}
		} else {
			List<ErrorDTO> error = responseWrapper.getErrors();
			regProcLogger.error("validate call ended for registrationId {} with error data : {}", registrationId,
					error.get(0).getMessage());
			throw new BaseCheckedException(error.get(0).getMessage(),
					StatusUtil.FAILED_TO_GET_MACHINE_DETAIL.getCode());
		}
		regProcLogger.debug("validate call ended for registrationId {} with response data : {}", registrationId,
				JsonUtil.objectMapperObjectToJson(mhrdto));
	}

}
