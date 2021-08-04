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
import io.mosip.registration.processor.status.dto.PacketExternalStatusDTO;
import io.mosip.registration.processor.status.dto.PacketExternalStatusRequestDTO;
import io.mosip.registration.processor.status.dto.PacketExternalStatusSubRequestDTO;
import io.mosip.registration.processor.status.dto.PacketStatusErrorDTO;
import io.mosip.registration.processor.status.exception.RegStatusAppException;
import io.mosip.registration.processor.status.service.PacketExternalStatusService;
import io.mosip.registration.processor.status.sync.response.dto.PacketExternalStatusResponseDTO;
import io.mosip.registration.processor.status.validator.PacketExternalStatusRequestValidator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;


/**
 * The Class PacketStatusController.
 */
@RefreshScope
@RestController
@Api(tags = "Packet Status")
public class PacketExternalStatusController {


	/** The packet external status service. */
	@Autowired
	PacketExternalStatusService packetExternalStatusService;

	/** The packet status request validator. */
	@Autowired
	PacketExternalStatusRequestValidator packetExternalStatusRequestValidator;

	/** The env. */
	@Autowired
	private Environment env;

	/** The digital signature utility. */
	@Autowired
	private DigitalSignatureUtility digitalSignatureUtility;

	/** The is enabled. */
	@Value("${registration.processor.signature.isEnabled}")
	private Boolean isEnabled;

	/** The Constant DATETIME_PATTERN. */
	private static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";

	/** The Constant RESPONSE_SIGNATURE. */
	private static final String RESPONSE_SIGNATURE = "Response-Signature";

	private static final String PACKET_EXTERNAL_STATUS_SERVICE_ID = "mosip.registration.processor.packet.external.status.id";

	private static final String PACKET_EXTERNAL_STATUS_APPLICATION_VERSION = "mosip.registration.processor.packet.external.status.version";

	/**
	 * Packet external status.
	 *
	 * @param packetExternalStatusRequestDTO the packet external status request DTO
	 * @return the response entity
	 * @throws RegStatusAppException the reg status app exception
	 */
	@PreAuthorize("hasAnyRole('REGISTRATION_ADMIN', 'REGISTRATION_OFFICER', 'REGISTRATION_SUPERVISOR','RESIDENT')")
	@PostMapping(path = "/packetexternalstatus", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Get the Packet external status", response = RegistrationExternalStatusCode.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Packet external status successfully fetched"),
			@ApiResponse(code = 400, message = "Unable to fetch the Packet external status") })
	public ResponseEntity<Object> packetExternalStatus(
			@RequestBody(required = true) PacketExternalStatusRequestDTO packetExternalStatusRequestDTO)
			throws RegStatusAppException {

		try {
			packetExternalStatusRequestValidator.validate(packetExternalStatusRequestDTO,
					env.getProperty(PACKET_EXTERNAL_STATUS_SERVICE_ID));
			List<String> packetIdList = new ArrayList<>();

			for (PacketExternalStatusSubRequestDTO packetExternalStatusSubRequestDTO : packetExternalStatusRequestDTO
					.getRequest()) {
				packetIdList.add(packetExternalStatusSubRequestDTO.getPacketId());
			}
			List<PacketExternalStatusDTO> packetExternalStatusDTOList = packetExternalStatusService
					.getByPacketIds(packetIdList);

			if (isEnabled) {
				PacketExternalStatusResponseDTO packetExternalStatusResponseDTO = buildPacketStatusResponse(
						packetExternalStatusDTOList, packetExternalStatusRequestDTO.getRequest());
				Gson gson = new GsonBuilder().serializeNulls().create();
				HttpHeaders headers = new HttpHeaders();
				headers.add(RESPONSE_SIGNATURE,
						digitalSignatureUtility.getDigitalSignature(gson.toJson(packetExternalStatusResponseDTO)));
				return ResponseEntity.status(HttpStatus.OK).headers(headers)
						.body(gson.toJson(packetExternalStatusResponseDTO));
			}
			return ResponseEntity.status(HttpStatus.OK)
					.body(buildPacketStatusResponse(packetExternalStatusDTOList, packetExternalStatusRequestDTO.getRequest()));
		} catch (RegStatusAppException e) {
			throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_DATA_VALIDATION_FAILED, e);
		} catch (Exception e) {
			throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_UNKNOWN_EXCEPTION, e);
		}
	}

	/**
	 * Builds the packet external status response.
	 *
	 * @param packetStatus the packet status
	 * @param packetIds    the packet ids
	 * @return the packet external status response DTO
	 */
	public PacketExternalStatusResponseDTO buildPacketStatusResponse(List<PacketExternalStatusDTO> packetStatus,
			List<PacketExternalStatusSubRequestDTO> packetIds) {

		PacketExternalStatusResponseDTO response = new PacketExternalStatusResponseDTO();
		if (Objects.isNull(response.getId())) {
			response.setId(env.getProperty(PACKET_EXTERNAL_STATUS_SERVICE_ID));
		}
		response.setResponsetime(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
		response.setVersion(env.getProperty(PACKET_EXTERNAL_STATUS_APPLICATION_VERSION));
		response.setResponse(packetStatus);
		List<PacketExternalStatusSubRequestDTO> packetIdsNotAvailable = packetIds.stream()
				.filter(request -> packetStatus.stream()
						.noneMatch(packet -> packet.getPacketId().equals(request.getPacketId())))
				.collect(Collectors.toList());
		List<ErrorDTO> errors = new ArrayList<ErrorDTO>();
		if (!packetIdsNotAvailable.isEmpty()) {

			for (PacketExternalStatusSubRequestDTO packetStatusSubRequestDTO : packetIdsNotAvailable) {
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
}
