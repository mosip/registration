package io.mosip.registration.processor.status.api.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.util.DigitalSignatureUtility;
import io.mosip.registration.processor.status.code.RegistrationExternalStatusCode;
import io.mosip.registration.processor.status.dto.ErrorDTO;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.PacketStatusDTO;
import io.mosip.registration.processor.status.dto.PacketStatusErrorDTO;
import io.mosip.registration.processor.status.dto.PacketStatusRequestDTO;
import io.mosip.registration.processor.status.dto.PacketStatusSubRequestDTO;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusErrorDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusRequestDTO;
import io.mosip.registration.processor.status.dto.RegistrationStatusSubRequestDto;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.exception.RegStatusAppException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.status.service.SyncRegistrationService;
import io.mosip.registration.processor.status.sync.response.dto.PacketStatusResponseDTO;
import io.mosip.registration.processor.status.sync.response.dto.RegStatusResponseDTO;
import io.mosip.registration.processor.status.validator.PacketStatusRequestValidator;
import io.mosip.registration.processor.status.validator.RegistrationStatusRequestValidator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * The Class RegistrationStatusController.
 */
@RefreshScope
@RestController
@Api(tags = "Registration Status")
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
	PacketStatusRequestValidator packetStatusRequestValidator;



	private static final String REG_STATUS_SERVICE_ID = "mosip.registration.processor.registration.status.id";
	private static final String REG_STATUS_APPLICATION_VERSION = "mosip.registration.processor.registration.status.version";
	private static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";
	private static final String RESPONSE_SIGNATURE = "Response-Signature";
	private static final String PACKET_STATUS_SERVICE_ID = "mosip.registration.processor.packet.status.id";
	private static final String PACKET_STATUS_APPLICATION_VERSION = "mosip.registration.processor.packet.status.version";

	@Autowired
	private Environment env;

	@Value("${registration.processor.signature.isEnabled}")
	private Boolean isEnabled;

	/** 
	 * The comma separate list of external statuses that should be considered as processed 
	 * for search API response consumed by regclient
	 */
	@Value("#{'${mosip.registration.processor.registration.status.external-statuses-to-consider-processed:UIN_GENERATED,REREGISTER,REJECTED,REPROCESS_FAILED}'.split(',')}")
	private List<String> externalStatusesConsideredProcessed;

	@Autowired
	private DigitalSignatureUtility digitalSignatureUtility;

	/**
	 * Search.
	 *
	 * @param registrationIds the registration ids
	 * @return the response entity
	 * @throws RegStatusAppException
	 */
	@PreAuthorize("hasAnyRole('REGISTRATION_ADMIN', 'REGISTRATION_OFFICER', 'REGISTRATION_SUPERVISOR','RESIDENT')")
	@PostMapping(path = "/search", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Get the registration entity", response = RegistrationExternalStatusCode.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Registration Entity successfully fetched"),
			@ApiResponse(code = 400, message = "Unable to fetch the Registration Entity") })
	public ResponseEntity<Object> search(
			@RequestBody(required = true) RegistrationStatusRequestDTO registrationStatusRequestDTO)
			throws RegStatusAppException {

		try {
			registrationStatusRequestValidator.validate(registrationStatusRequestDTO,
					env.getProperty(REG_STATUS_SERVICE_ID));
			List<RegistrationStatusDto> registrations = registrationStatusService
					.getByIds(registrationStatusRequestDTO.getRequest());
			
			List<RegistrationStatusSubRequestDto> requestIdsNotAvailable = registrationStatusRequestDTO.getRequest()
					.stream()
					.filter(request -> registrations.stream().noneMatch(
							registration -> registration.getRegistrationId().equals(request.getRegistrationId())))
					.collect(Collectors.toList());
			List<RegistrationStatusDto> registrationsList = syncRegistrationService.getByIds(requestIdsNotAvailable);
			if (registrationsList != null && !registrationsList.isEmpty()) {
				registrations.addAll(syncRegistrationService.getByIds(requestIdsNotAvailable));
			}

			updatedConditionalStatusToProcessed(registrations);

			if (isEnabled) {
				RegStatusResponseDTO response = buildRegistrationStatusResponse(registrations,
						registrationStatusRequestDTO.getRequest());
				Gson gson = new GsonBuilder().serializeNulls().create();
				HttpHeaders headers = new HttpHeaders();
				headers.add(RESPONSE_SIGNATURE, digitalSignatureUtility.getDigitalSignature(gson.toJson(response)));
				return ResponseEntity.status(HttpStatus.OK).headers(headers).body(gson.toJson(response));
			}
			return ResponseEntity.status(HttpStatus.OK)
					.body(buildRegistrationStatusResponse(registrations, registrationStatusRequestDTO.getRequest()));
		} catch (RegStatusAppException e) {
			throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_DATA_VALIDATION_FAILED, e);
		} catch (Exception e) {
			throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_UNKNOWN_EXCEPTION, e);
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

	@PreAuthorize("hasAnyRole('REGISTRATION_ADMIN', 'REGISTRATION_OFFICER', 'REGISTRATION_SUPERVISOR','RESIDENT')")
	@PostMapping(path = "/packetStatus", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Get the Packet status", response = RegistrationExternalStatusCode.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Packet status successfully fetched"),
			@ApiResponse(code = 400, message = "Unable to fetch the Packet status") })
	public ResponseEntity<Object> packetStatus(
			@RequestBody(required = true) PacketStatusRequestDTO packetStatusRequestDTO) throws RegStatusAppException {

		try {
			packetStatusRequestValidator.validate(packetStatusRequestDTO,
					env.getProperty(PACKET_STATUS_SERVICE_ID));
			List<PacketStatusDTO> packetStatus = registrationStatusService
					.getByPacketIds(packetStatusRequestDTO.getRequest());

			List<PacketStatusSubRequestDTO> packetIdsNotAvailable = packetStatusRequestDTO.getRequest()
					.stream()
					.filter(request -> packetStatus.stream()
							.noneMatch(packet -> packet.getPacketId().equals(request.getPacketId())))
					.collect(Collectors.toList());
			List<PacketStatusDTO> packetIdList = syncRegistrationService
					.getByPacketIdsWithStatus(packetIdsNotAvailable);
			if (packetIdList != null && !packetIdList.isEmpty()) {
				packetStatus.addAll(syncRegistrationService.getByPacketIdsWithStatus(packetIdsNotAvailable));
			}

			updatedConditionalStatusToProcessedForPacketIds(packetStatus);

			if (isEnabled) {
				PacketStatusResponseDTO response = buildPacketStatusResponse(packetStatus,
						packetStatusRequestDTO.getRequest());
				Gson gson = new GsonBuilder().serializeNulls().create();
				HttpHeaders headers = new HttpHeaders();
				headers.add(RESPONSE_SIGNATURE, digitalSignatureUtility.getDigitalSignature(gson.toJson(response)));
				return ResponseEntity.status(HttpStatus.OK).headers(headers).body(gson.toJson(response));
			}
			return ResponseEntity.status(HttpStatus.OK)
					.body(buildPacketStatusResponse(packetStatus, packetStatusRequestDTO.getRequest()));
		} catch (RegStatusAppException e) {
			throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_DATA_VALIDATION_FAILED, e);
		} catch (Exception e) {
			throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_UNKNOWN_EXCEPTION, e);
		}
	}

	public PacketStatusResponseDTO buildPacketStatusResponse(List<PacketStatusDTO> packetStatus,
			List<PacketStatusSubRequestDTO> requestIds) {

		PacketStatusResponseDTO response = new PacketStatusResponseDTO();
		if (Objects.isNull(response.getId())) {
			response.setId(env.getProperty(PACKET_STATUS_SERVICE_ID));
		}
		response.setResponsetime(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
		response.setVersion(env.getProperty(PACKET_STATUS_APPLICATION_VERSION));
		response.setResponse(packetStatus);
		List<PacketStatusSubRequestDTO> packetIdsNotAvailable = requestIds.stream()
				.filter(request -> packetStatus.stream()
						.noneMatch(packet -> packet.getPacketId().equals(request.getPacketId())))
				.collect(Collectors.toList());
		List<ErrorDTO> errors = new ArrayList<ErrorDTO>();
		if (!packetIdsNotAvailable.isEmpty()) {

			for (PacketStatusSubRequestDTO packetStatusSubRequestDTO : packetIdsNotAvailable) {
				PacketStatusErrorDTO errorDto = new PacketStatusErrorDTO(
						PlatformErrorMessages.RPR_RGS_PACKETID_NOT_FOUND.getCode(),
						PlatformErrorMessages.RPR_RGS_PACKETID_NOT_FOUND.getMessage());

				errorDto.setPacketId(packetStatusSubRequestDTO.getPacketId());
				errors.add(errorDto);
			}
		}
		response.setErrors(errors);
		return response;
	}

	private void updatedConditionalStatusToProcessedForPacketIds(List<PacketStatusDTO> packets) {
		for (PacketStatusDTO packetStatusDTO : packets) {
			if (externalStatusesConsideredProcessed.contains(packetStatusDTO.getStatusCode()))
				packetStatusDTO.setStatusCode(RegistrationExternalStatusCode.PROCESSED.toString());
		}
	}
}