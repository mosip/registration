package io.mosip.registration.processor.status.api.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.util.DigitalSignatureUtility;
import io.mosip.registration.processor.status.code.RegistrationExternalStatusCode;
import io.mosip.registration.processor.status.dto.ErrorDTO;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.LostRidDto;
import io.mosip.registration.processor.status.dto.LostRidRequestDto;
import io.mosip.registration.processor.status.dto.LostRidResponseDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusErrorDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusRequestDTO;
import io.mosip.registration.processor.status.dto.RegistrationStatusSubRequestDto;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.exception.RegStatusAppException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.status.service.SyncRegistrationService;
import io.mosip.registration.processor.status.sync.response.dto.RegStatusResponseDTO;
import io.mosip.registration.processor.status.validator.LostRidRequestValidator;
import io.mosip.registration.processor.status.validator.RegistrationStatusRequestValidator;
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
public class RegistrationStatusController {

	/** The registration status service. */
	@Autowired
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** The sync registration service. */
	@Autowired
	SyncRegistrationService<SyncResponseDto, SyncRegistrationDto> syncRegistrationService;

	/** The validator. */
	@Autowired
	RegistrationStatusRequestValidator registrationStatusRequestValidator;

	@Autowired
	LostRidRequestValidator lostRidRequestValidator;

	private static final String REG_STATUS_SERVICE_ID = "mosip.registration.processor.registration.status.id";
	private static final String REG_LOSTRID_SERVICE_ID = "mosip.registration.processor.lostrid.id";
	private static final String REG_STATUS_APPLICATION_VERSION = "mosip.registration.processor.registration.status.version";
	private static final String REG_LOSTRID_APPLICATION_VERSION = "mosip.registration.processor.lostrid.version";
	private static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";
	private static final String RESPONSE_SIGNATURE = "Response-Signature";

	@Autowired
	private Environment env;

	@Value("${registration.processor.signature.isEnabled}")
	private Boolean isEnabled;
	
	@Autowired
	ObjectMapper objMp;

	/** 
	 * The comma separate list of external statuses that should be considered as processed 
	 * for search API response consumed by regclient
	 */
	@Value("#{'${mosip.registration.processor.registration.status.external-statuses-to-consider-processed:UIN_GENERATED,REREGISTER,REJECTED,REPROCESS_FAILED}'.split(',')}")
	private List<String> externalStatusesConsideredProcessed;

	@Value("${registration.processor.fetch.registration.records.limit:100}")
	private int maxLimit;

	@Autowired
	private DigitalSignatureUtility digitalSignatureUtility;

	/**
	 * Search.
	 *
	 * @param registrationStatusRequestDTO the registration ids
	 * @return the response entity
	 * @throws RegStatusAppException
	 */
	//@PreAuthorize("hasAnyRole('REGISTRATION_ADMIN', 'REGISTRATION_OFFICER', 'REGISTRATION_SUPERVISOR','RESIDENT')")
	@PreAuthorize("hasAnyRole(@authorizedRoles.getPostsearch())")
	@PostMapping(path = "/search", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get the registration entity", description = "Get the registration entity", tags = { "Registration Status" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Registration Entity successfully fetched",
					content = @Content(schema = @Schema(implementation = RegistrationExternalStatusCode.class))),
			@ApiResponse(responseCode = "201", description = "Created" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "400", description = "Unable to fetch the Registration Entity" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true)))})
	public ResponseEntity<Object> search(
			@RequestBody(required = true) RegistrationStatusRequestDTO registrationStatusRequestDTO)
			throws RegStatusAppException {

		try {
			registrationStatusRequestValidator.validate(registrationStatusRequestDTO,
					env.getProperty(REG_STATUS_SERVICE_ID));

			// if number of rids in request exceeds max limit then get status for first 100 ids.
			List<RegistrationStatusSubRequestDto> recordsToFetch = CollectionUtils.isNotEmpty(registrationStatusRequestDTO.getRequest())
					&& registrationStatusRequestDTO.getRequest().size() > maxLimit ?
					registrationStatusRequestDTO.getRequest().stream().limit(maxLimit).collect(Collectors.toList()) : registrationStatusRequestDTO.getRequest();

			List<RegistrationStatusDto> registrations = registrationStatusService
					.getByIds(recordsToFetch);
			List<RegistrationStatusSubRequestDto> requestIdsNotAvailable = recordsToFetch
					.stream()
					.filter(request -> registrations.stream().noneMatch(
							registration -> registration.getRegistrationId().equals(request.getRegistrationId())))
					.collect(Collectors.toList());

			if (CollectionUtils.isNotEmpty(requestIdsNotAvailable)) {
				List<RegistrationStatusDto> registrationsList = syncRegistrationService.getByIds(requestIdsNotAvailable);
				if (registrationsList != null && !registrationsList.isEmpty()) {
					registrations.addAll(syncRegistrationService.getByIds(requestIdsNotAvailable));
				}
			}

			updatedConditionalStatusToProcessed(registrations);

			if (isEnabled) {
				String response = objMp.writeValueAsString(buildRegistrationStatusResponse(registrations,
						recordsToFetch));	
				HttpHeaders headers = new HttpHeaders();
				headers.add(RESPONSE_SIGNATURE, digitalSignatureUtility.getDigitalSignature(response));
				return ResponseEntity.status(HttpStatus.OK).headers(headers).body(response);
			}
			return ResponseEntity.status(HttpStatus.OK)
					.body(buildRegistrationStatusResponse(registrations, recordsToFetch));
		} catch (RegStatusAppException e) {
			throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_DATA_VALIDATION_FAILED, e);
		} catch (Exception e) {
			throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_UNKNOWN_EXCEPTION, e);
		}
	}

	/**
	 * Search
	 *
	 * @param lostRidRequestDto
	 * @return
	 * @throws RegStatusAppException
	 */
	//@PreAuthorize("hasAnyRole('REGISTRATION_ADMIN', 'REGISTRATION_OFFICER', 'ZONAL_ADMIN','GLOBAL_ADMIN')")
	@PreAuthorize("hasAnyRole(@authorizedRoles.getPostlostridsearch())")
	@PostMapping(path = "/lostridsearch", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get the lost registration id", description = "Get the lost registration id", tags = { "Registration Status" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Registration id successfully fetched",
					content = @Content(schema = @Schema(implementation = RegistrationExternalStatusCode.class))),
			@ApiResponse(responseCode = "201", description = "Created" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "400", description = "Unable to fetch the Registration Entity" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true)))})
	public ResponseEntity<Object> searchLostRid(
			@RequestBody(required = true) LostRidRequestDto lostRidRequestDto)
			throws RegStatusAppException {

		try {
			lostRidRequestValidator.validate(lostRidRequestDto);
			List<LostRidDto> lostRidDtos = syncRegistrationService.searchLostRid(lostRidRequestDto.getRequest());
			return ResponseEntity.status(HttpStatus.OK)
					.body(buildLostRidResponse(lostRidDtos));
		} catch (RegStatusAppException e) {
			throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_DATA_VALIDATION_FAILED, e);
		} catch (Exception e) {
			throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_INVALID_SEARCH, e);
		}
	}

	public RegStatusResponseDTO buildRegistrationStatusResponse(List<RegistrationStatusDto> registrations,
			List<RegistrationStatusSubRequestDto> requestIds) {

		RegStatusResponseDTO response = new RegStatusResponseDTO();
		if (Objects.isNull(response.getId())) {
			response.setId(env.getProperty(REG_STATUS_SERVICE_ID));
		}
		response.setResponsetime(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
		response.setVersion(env.getProperty(REG_STATUS_APPLICATION_VERSION));
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

	private void updatedConditionalStatusToProcessed(List<RegistrationStatusDto> registrations) {
		for(RegistrationStatusDto registrationStatusDto : registrations) {
			if(externalStatusesConsideredProcessed.contains(registrationStatusDto.getStatusCode()))
				registrationStatusDto.setStatusCode(RegistrationExternalStatusCode.PROCESSED.toString());
		}
	}

	public LostRidResponseDto buildLostRidResponse(List<LostRidDto> lostRidDtos) {

		LostRidResponseDto response = new LostRidResponseDto();
		if (Objects.isNull(response.getId())) {
			response.setId(env.getProperty(REG_LOSTRID_SERVICE_ID));
		}
		response.setResponsetime(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
		response.setVersion(env.getProperty(REG_LOSTRID_APPLICATION_VERSION));
		response.setResponse(lostRidDtos);
		List<ErrorDTO> errors = new ArrayList<ErrorDTO>();
		if (lostRidDtos.isEmpty()) {
			RegistrationStatusErrorDto errorDto = new RegistrationStatusErrorDto(
					PlatformErrorMessages.RPR_RGS_RID_NOT_FOUND.getCode(),
					PlatformErrorMessages.RPR_RGS_RID_NOT_FOUND.getMessage());
			errors.add(errorDto);
		}
		response.setErrors(errors);
		return response;
	}

}
