package io.mosip.registration.processor.status.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.core.constant.ResponseStatusCode;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.util.DigitalSignatureUtility;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.*;
import io.mosip.registration.processor.status.exception.RegStatusAppException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.status.service.SyncRegistrationService;
import io.mosip.registration.processor.status.sync.response.dto.RegSyncResponseDTO;
import io.mosip.registration.processor.status.validator.RegistrationSyncRequestValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * The Class RegistrationStatusController.
 */
@RefreshScope
@RestController
@Tag(name = "Registration Status", description = "Registration Status Controller")
public class RegistrationSyncController {

	/** The registration status service. */
	@Autowired
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** The sync registration service. */
	@Autowired
	SyncRegistrationService<SyncResponseDto, SyncRegistrationDto> syncRegistrationService;

	/** The validator. */
	@Autowired
	private RegistrationSyncRequestValidator validator;

	@Autowired
	private Environment env;

	@Autowired
	private ObjectMapper objectMapper;



	@Value("${registration.processor.signature.isEnabled}")
	private Boolean isEnabled;

	@Autowired
	private DigitalSignatureUtility digitalSignatureUtility;

	private static final String REG_SYNC_SERVICE_ID = "mosip.registration.processor.registration.sync.id";
	private static final String REG_SYNC_APPLICATION_VERSION = "mosip.registration.processor.sync.version";
	private static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";
	private static final String RESPONSE_SIGNATURE = "Response-Signature";

	/**
	 * Sync registration ids.
	 *
	 * @param syncRegistrationList
	 *            the sync registration list
	 * @return the response entity
	 * @throws RegStatusAppException
	 */
	//@PreAuthorize("hasAnyRole('REGISTRATION_ADMIN', 'REGISTRATION_PROCESSOR', 'REGISTRATION_OFFICER','REGISTRATION_SUPERVISOR', 'RESIDENT' )")
	@PreAuthorize("hasAnyRole(@authorizedRoles.getPostsync())")
	@PostMapping(path = "/sync", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get the synchronizing registration entity", description = "Get the synchronizing registration entity", tags = { "Registration Status" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Synchronizing Registration Entity successfully fetched",
					content = @Content(schema = @Schema(implementation = RegistrationStatusCode.class))),
			@ApiResponse(responseCode = "201", description = "Created" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true)))})
	public ResponseEntity<Object> syncRegistrationController(
			@RequestHeader(name = "Center-Machine-RefId", required = true) String referenceId,
			@RequestHeader(name = "timestamp", required = true) String timeStamp,
			@RequestBody(required = true) Object encryptedSyncMetaInfo) throws RegStatusAppException {
		try {
			List<SyncResponseDto> syncResponseList = new ArrayList<>();
			RegistrationSyncRequestDTO registrationSyncRequestDTO = syncRegistrationService
					.decryptAndGetSyncRequest(encryptedSyncMetaInfo, referenceId, timeStamp, syncResponseList);

			if (registrationSyncRequestDTO != null && validator.validate(registrationSyncRequestDTO,
					env.getProperty(REG_SYNC_SERVICE_ID), syncResponseList)) {
				syncResponseList = syncRegistrationService.sync(registrationSyncRequestDTO.getRequest(), referenceId, timeStamp);
			}
			if (isEnabled) {
				RegSyncResponseDTO responseDto = buildRegistrationSyncResponse(syncResponseList);
				HttpHeaders headers = new HttpHeaders();
				headers.add(RESPONSE_SIGNATURE,
						digitalSignatureUtility.getDigitalSignature(objectMapper.writeValueAsString(responseDto)));
				return ResponseEntity.ok().headers(headers).body(responseDto);
			}

			return ResponseEntity.ok().body(buildRegistrationSyncResponse(syncResponseList));

		} catch (JsonProcessingException e) {
			throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_DATA_VALIDATION_FAILED, e);
		}
	}
	
	/**
	 * Sync registration ids.
	 *
	 * @param syncRegistrationList
	 *            the sync registration list
	 * @return the response entity
	 * @throws RegStatusAppException
	 */
	//@PreAuthorize("hasAnyRole('REGISTRATION_ADMIN', 'REGISTRATION_PROCESSOR', 'REGISTRATION_OFFICER','REGISTRATION_SUPERVISOR', 'RESIDENT' )")
	@PreAuthorize("hasAnyRole(@authorizedRoles.getPostsyncv2())")
	@PostMapping(path = "/syncV2", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get the synchronizing registration entity", description = "Get the synchronizing registration entity", tags = { "Registration Status" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Synchronizing Registration Entity successfully fetched",
					content = @Content(schema = @Schema(implementation = RegistrationStatusCode.class))),
			@ApiResponse(responseCode = "201", description = "Created" ,content = @Content(schema = @Schema(hidden = true))) ,
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true)))})
	public ResponseEntity<Object> syncRegistrationController2(
			@RequestHeader(name = "Center-Machine-RefId", required = true) String referenceId,
			@RequestHeader(name = "timestamp", required = true) String timeStamp,
			@RequestBody(required = true) Object encryptedSyncMetaInfo) throws RegStatusAppException {
		try {
			List<SyncResponseDto> syncResponseList = new ArrayList<>();
			RegistrationSyncRequestDTO registrationSyncRequestDTO = syncRegistrationService
					.decryptAndGetSyncRequest(encryptedSyncMetaInfo, referenceId, timeStamp, syncResponseList);

			if (registrationSyncRequestDTO != null && validator.validate(registrationSyncRequestDTO,
					env.getProperty(REG_SYNC_SERVICE_ID), syncResponseList)) {
				syncResponseList = syncRegistrationService.syncV2(registrationSyncRequestDTO.getRequest(), referenceId, timeStamp);
			}
			if (isEnabled) {
				RegSyncResponseDTO responseDto = buildRegistrationSyncResponse(syncResponseList);
				HttpHeaders headers = new HttpHeaders();
				headers.add(RESPONSE_SIGNATURE,
						digitalSignatureUtility.getDigitalSignature(objectMapper.writeValueAsString(responseDto)));
				return ResponseEntity.ok().headers(headers).body(responseDto);
			}

			return ResponseEntity.ok().body(buildRegistrationSyncResponse(syncResponseList));

		} catch (JsonProcessingException e) {
			throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_DATA_VALIDATION_FAILED, e);
		}
	}

	public RegSyncResponseDTO buildRegistrationSyncResponse(List<SyncResponseDto> syncResponseDtoList)
			throws JsonProcessingException {

		RegSyncResponseDTO response = new RegSyncResponseDTO();
		if (Objects.isNull(response.getId())) {
			response.setId(env.getProperty(REG_SYNC_SERVICE_ID));
		}
		response.setResponsetime(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
		response.setVersion(env.getProperty(REG_SYNC_APPLICATION_VERSION));
		List<SyncErrorDTO> syncErrorDTOList = new ArrayList<>();
		List<SyncResponseDto> syncResponseList = new ArrayList<>();
		for (SyncResponseDto syncResponseDto : syncResponseDtoList) {
			if (syncResponseDto.getStatus().equals(ResponseStatusCode.SUCCESS.toString())) {
				syncResponseList.add(syncResponseDto);
			} else {
				if (syncResponseDto instanceof SyncResponseFailureDto) {
					SyncErrDTO errors = new SyncErrDTO(((SyncResponseFailureDto) syncResponseDto).getErrorCode(),
							((SyncResponseFailureDto) syncResponseDto).getMessage());
					errors.setRegistrationId(((SyncResponseFailureDto) syncResponseDto).getRegistrationId());
					errors.setStatus(syncResponseDto.getStatus());

					syncErrorDTOList.add(errors);
				} else if (syncResponseDto instanceof SyncResponseFailDto) {
					SyncErrorDTO errors = new SyncErrorDTO(((SyncResponseFailDto) syncResponseDto).getErrorCode(),
							((SyncResponseFailDto) syncResponseDto).getMessage());
					errors.setStatus(syncResponseDto.getStatus());
					syncErrorDTOList.add(errors);
				}
			}
		}
		if (!syncErrorDTOList.isEmpty()) {
			response.setErrors(syncErrorDTOList);
		}
		if (!syncResponseList.isEmpty()) {
			response.setResponse(syncResponseList);
		}

		return response;
	}

}