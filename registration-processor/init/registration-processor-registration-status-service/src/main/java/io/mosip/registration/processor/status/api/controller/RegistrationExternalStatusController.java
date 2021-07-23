package io.mosip.registration.processor.status.api.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.util.DigitalSignatureUtility;
import io.mosip.registration.processor.status.dto.*;
import io.mosip.registration.processor.status.exception.RegStatusAppException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.status.service.SyncRegistrationService;
import io.mosip.registration.processor.status.sync.response.dto.RegExternalStatusResponseDTO;
import io.mosip.registration.processor.status.validator.RegistrationExternalStatusRequestValidator;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RefreshScope
@RestController
@Tag(name = "External Registration Status", description = "External Registration Status")
public class RegistrationExternalStatusController {
	
	/** The registration status service. */
	@Autowired
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** The sync registration service. */
	@Autowired
	SyncRegistrationService<SyncResponseDto, SyncRegistrationDto> syncRegistrationService;
	
	@Autowired
	RegistrationExternalStatusRequestValidator registrationExternalStatusRequestValidator;
	
	@Autowired
	private DigitalSignatureUtility digitalSignatureUtility;
	
	@Autowired
	private Environment env;
	
	@Value("${registration.processor.signature.isEnabled}")
	private Boolean isEnabled;
	
	private static final String REG_EXTERNAL_STATUS_SERVICE_ID = "mosip.registration.processor.registration.external.status.id";
	private static final String RESPONSE_SIGNATURE = "Response-Signature";
	private static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";
	private static final String REG_EXTERNAL_STATUS_APPLICATION_VERSION = "mosip.registration.processor.registration.external.status.version";
	
	/**
	 * Search.
	 *
	 * @param registrationIds the registration ids
	 * @return the response entity
	 * @throws RegStatusAppException
	 */
	@PreAuthorize("hasAnyRole('REGISTRATION_ADMIN', 'REGISTRATION_OFFICER', 'REGISTRATION_SUPERVISOR','RESIDENT')")
	@PostMapping(path = "/externalstatus/search", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get the registration external status", description = "registration external status", tags = { "External Registration Status" })
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Registration external status successfully fetched"),
			@ApiResponse(code = 400, message = "Unable to fetch the registration external status") })
	public ResponseEntity<Object> search(
			@RequestBody(required = true) RegistrationExternalStatusRequestDTO registrationExternalStatusRequestDTO)
			throws RegStatusAppException {

		try {
			registrationExternalStatusRequestValidator.validate(registrationExternalStatusRequestDTO,
					env.getProperty(REG_EXTERNAL_STATUS_SERVICE_ID));
			List<RegistrationStatusDto> registrations = registrationStatusService
					.getByIds(registrationExternalStatusRequestDTO.getRequest());
			
			List<RegistrationStatusSubRequestDto> requestIdsNotAvailable = registrationExternalStatusRequestDTO.getRequest()
					.stream()
					.filter(request -> registrations.stream().noneMatch(
							registration -> registration.getRegistrationId().equals(request.getRegistrationId())))
					.collect(Collectors.toList());
			List<RegistrationStatusDto> registrationsList = syncRegistrationService.getByIds(requestIdsNotAvailable);
			if (registrationsList != null && !registrationsList.isEmpty()) {
				registrations.addAll(registrationsList);
			}

			if (isEnabled) {
				RegExternalStatusResponseDTO response = buildRegistrationStatusResponse(registrations,
						registrationExternalStatusRequestDTO.getRequest());
				Gson gson = new GsonBuilder().serializeNulls().create();
				HttpHeaders headers = new HttpHeaders();
				headers.add(RESPONSE_SIGNATURE, digitalSignatureUtility.getDigitalSignature(gson.toJson(response)));
				return ResponseEntity.status(HttpStatus.OK).headers(headers).body(gson.toJson(response));
			}
			return ResponseEntity.status(HttpStatus.OK)
					.body(buildRegistrationStatusResponse(registrations, registrationExternalStatusRequestDTO.getRequest()));
		} catch (RegStatusAppException e) {
			throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_DATA_VALIDATION_FAILED, e);
		} catch (Exception e) {
			throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_UNKNOWN_EXCEPTION, e);
		}
	}
	
	public RegExternalStatusResponseDTO buildRegistrationStatusResponse(List<RegistrationStatusDto> registrations,
			List<RegistrationStatusSubRequestDto> requestIds) {

		RegExternalStatusResponseDTO response = new RegExternalStatusResponseDTO();
		if (Objects.isNull(response.getId())) {
			response.setId(env.getProperty(REG_EXTERNAL_STATUS_SERVICE_ID));
		}
		response.setResponsetime(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
		response.setVersion(env.getProperty(REG_EXTERNAL_STATUS_APPLICATION_VERSION));
		response.setResponse(registrations);
		List<RegistrationStatusSubRequestDto> requestIdsNotAvailable = requestIds.stream()
				.filter(request -> registrations.stream().noneMatch(
						registration -> registration.getRegistrationId().equals(request.getRegistrationId())))
				.collect(Collectors.toList());
		List<ErrorDTO> errors = new ArrayList<ErrorDTO>();
		if (!requestIdsNotAvailable.isEmpty()) {

			for (RegistrationStatusSubRequestDto requestDto : requestIdsNotAvailable) {
				RegistrationStatusErrorDto errorDto = new RegistrationStatusErrorDto(
						PlatformErrorMessages.RPR_RGS_RID_NOT_FOUND.getCode(),
						PlatformErrorMessages.RPR_RGS_RID_NOT_FOUND.getMessage());

				errorDto.setRegistrationId(requestDto.getRegistrationId());
				errors.add(errorDto);
			}
		}
		response.setErrors(errors);
		return response;
	}

}
