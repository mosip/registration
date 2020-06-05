package io.mosip.registration.processor.request.handler.service.impl;

import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.PacketMetaInfoConstants;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.idobjectvalidator.exception.IdObjectIOException;
import io.mosip.kernel.core.idobjectvalidator.exception.IdObjectValidationFailedException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import io.mosip.registration.processor.core.constant.CardType;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.PacketMetaInfoConstants;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.http.RequestWrapper;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.vid.VidRequestDto;
import io.mosip.registration.processor.core.packet.dto.vid.VidResponseDTO;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.request.handler.service.PacketCreationService;
import io.mosip.registration.processor.request.handler.service.dto.PacketGeneratorResDto;
import io.mosip.registration.processor.request.handler.service.dto.RegistrationDTO;
import io.mosip.registration.processor.request.handler.service.dto.UinCardRePrintRequestDto;
import io.mosip.registration.processor.request.handler.service.dto.demographic.DemographicDTO;
import io.mosip.registration.processor.request.handler.service.exception.RegBaseCheckedException;
import io.mosip.registration.processor.request.handler.service.exception.VidCreationException;
import io.mosip.registration.processor.request.handler.upload.SyncUploadEncryptionService;
import io.mosip.registration.processor.request.handler.upload.validator.RequestHandlerRequestValidator;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;

/**
 * The Class ResidentServiceRePrintServiceImpl.
 */
@Service
public class UinCardRePrintServiceImpl {

	/** The env. */
	@Autowired
	private Environment env;

	@Value("${IDSchema.Version}")
	private String idschemaVersion;

	/** The rest client service. */
	@Autowired
	private RegistrationProcessorRestClientService<Object> restClientService;

	/** The packet creation service. */
	@Autowired
	private PacketCreationService packetCreationService;

	/** The sync upload encryption service. */
	@Autowired
	SyncUploadEncryptionService syncUploadEncryptionService;

	/** The validator. */
	@Autowired
	private RequestHandlerRequestValidator validator;

	/** The utilities. */
	@Autowired
	Utilities utilities;

	@Autowired
	private AuditLogRequestBuilder auditLogRequestBuilder;

	/** The vid type. */
	@Value("${registration.processor.id.repo.vidType}")
	private String vidType;

	/** The Constant VID_CREATE_ID. */
	public static final String VID_CREATE_ID = "registration.processor.id.repo.generate";

	/** The Constant REG_PROC_APPLICATION_VERSION. */
	public static final String REG_PROC_APPLICATION_VERSION = "registration.processor.id.repo.vidVersion";

	/** The Constant DATETIME_PATTERN. */
	public static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";

	/** The Constant UIN. */
	public static final String UIN = "UIN";

	/** The Constant VID. */
	public static final String VID = "VID";

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(UinCardRePrintServiceImpl.class);

	public static final String VID_TYPE = "registration.processor.id.repo.vidType";

	/**
	 * Creates the packet.
	 *
	 * @param uinCardRePrintRequestDto the uin card re print request dto
	 * @return the packet generator res dto
	 * @throws RegBaseCheckedException the reg base checked exception
	 * @throws IOException             Signals that an I/O exception has occurred.
	 */
	@SuppressWarnings("unchecked")
	public PacketGeneratorResDto createPacket(UinCardRePrintRequestDto uinCardRePrintRequestDto)
			throws RegBaseCheckedException, IOException {
		boolean isTransactional = false;
		String uin = null;
		String vid = null;
		byte[] packetZipBytes = null;
		PacketGeneratorResDto packetGeneratorResDto = new PacketGeneratorResDto();
		validator.validate(uinCardRePrintRequestDto.getRequesttime(), uinCardRePrintRequestDto.getId(),
				uinCardRePrintRequestDto.getVersion());
		try {
			if (validator.isValidCenter(uinCardRePrintRequestDto.getRequest().getCenterId())
					&& validator.isValidMachine(uinCardRePrintRequestDto.getRequest().getMachineId())
					&& validator
							.isValidRePrintRegistrationType(uinCardRePrintRequestDto.getRequest().getRegistrationType())
					&& validator.isValidIdType(uinCardRePrintRequestDto.getRequest().getIdType())
					&& validator.isValidCardType(uinCardRePrintRequestDto.getRequest().getCardType())
					&& isValidUinVID(uinCardRePrintRequestDto)) {
				String cardType = uinCardRePrintRequestDto.getRequest().getCardType();
				String regType = uinCardRePrintRequestDto.getRequest().getRegistrationType();

				if (uinCardRePrintRequestDto.getRequest().getIdType().equalsIgnoreCase(UIN))
					uin = uinCardRePrintRequestDto.getRequest().getId();
				else
					vid = uinCardRePrintRequestDto.getRequest().getId();

				if (cardType.equalsIgnoreCase(CardType.MASKED_UIN.toString()) && vid == null) {

					VidRequestDto vidRequestDto = new VidRequestDto();
					RequestWrapper<VidRequestDto> request = new RequestWrapper<>();
					VidResponseDTO response;
					vidRequestDto.setUIN(uin);
					vidRequestDto.setVidType(env.getProperty(VID_TYPE));
					request.setId(env.getProperty(VID_CREATE_ID));
					request.setRequest(vidRequestDto);
					DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
					LocalDateTime localdatetime = LocalDateTime
							.parse(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);
					request.setRequesttime(localdatetime);
					request.setVersion(env.getProperty(REG_PROC_APPLICATION_VERSION));

					regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), "",
							"UinCardRePrintServiceImpl::createPacket():: post CREATEVID service call started with request data : "
									+ JsonUtil.objectMapperObjectToJson(vidRequestDto));

					response = (VidResponseDTO) restClientService.postApi(ApiName.CREATEVID, "", "", request,
							VidResponseDTO.class);

					regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), "",
							"UinCardRePrintServiceImpl::createPacket():: post CREATEVID service call ended successfully");

					if (!response.getErrors().isEmpty()) {
						throw new VidCreationException(PlatformErrorMessages.RPR_PGS_VID_EXCEPTION.getMessage(),
								"VID creation exception");

					} else {
						vid = response.getResponse().getVid();
					}

				}
				if (uin == null) {
					uin = utilities.getUinByVid(vid);
				}

				RegistrationDTO registrationDTO = createRegistrationDTOObject(uin,
						uinCardRePrintRequestDto.getRequest().getRegistrationType(),
						uinCardRePrintRequestDto.getRequest().getCenterId(),
						uinCardRePrintRequestDto.getRequest().getMachineId(), vid, cardType);
				packetZipBytes = packetCreationService.create(registrationDTO,
						uinCardRePrintRequestDto.getRequest().getCenterId(), uinCardRePrintRequestDto.getRequest().getMachineId());
				String rid = registrationDTO.getRegistrationId();
				String packetCreatedDateTime = rid.substring(rid.length() - 14);
				String formattedDate = packetCreatedDateTime.substring(0, 8) + "T"
						+ packetCreatedDateTime.substring(packetCreatedDateTime.length() - 6);
				LocalDateTime ldt = LocalDateTime.parse(formattedDate,
						DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
				String creationTime = ldt.toString() + ".000Z";

				if (utilities.linkRegIdWrtUin(rid, uin))
					packetGeneratorResDto = syncUploadEncryptionService.uploadUinPacket(
							registrationDTO.getRegistrationId(), creationTime, regType, packetZipBytes);
				else
					regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), rid,
							"UinCardRePrintServiceImpl::createPacket():: RID link to UIN failed");

			}
			isTransactional = true;
			return packetGeneratorResDto;
		} catch (ApisResourceAccessException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", PlatformErrorMessages.RPR_PGS_API_RESOURCE_NOT_AVAILABLE.getMessage()
							+ ExceptionUtils.getStackTrace(e));
			throw new RegBaseCheckedException(PlatformErrorMessages.RPR_PGS_JSON_PROCESSING_EXCEPTION, e);
		} catch (VidCreationException
				| io.mosip.registration.processor.packet.storage.exception.VidCreationException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", PlatformErrorMessages.RPR_PGS_VID_CREATION_EXCEPTION.getMessage()
							+ ExceptionUtils.getStackTrace(e));
			throw new RegBaseCheckedException(PlatformErrorMessages.RPR_PGS_VID_CREATION_EXCEPTION, e);
		} catch (IdObjectValidationFailedException | IdObjectIOException | ParseException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"",
					PlatformErrorMessages.RPR_PGS_ID_OBJECT_EXCEPTION.getMessage() + ExceptionUtils.getStackTrace(e));
			throw new RegBaseCheckedException(PlatformErrorMessages.RPR_PGS_ID_OBJECT_EXCEPTION, e);
		} finally {
			String eventId = isTransactional ? EventId.RPR_402.toString() : EventId.RPR_405.toString();
			String eventName = isTransactional ? EventName.UPDATE.toString() : EventName.EXCEPTION.toString();
			String eventType = isTransactional ? EventType.BUSINESS.toString() : EventType.SYSTEM.toString();
			String message = isTransactional ? StatusUtil.UIN_CARD_REPRINT_SUCCESS.getMessage()
					: StatusUtil.UIN_CARD_REPRINT_FAILED.getMessage();
			String moduleName = ModuleName.REQUEST_HANDLER_SERVICE.toString();
			String moduleId = isTransactional ? StatusUtil.UIN_CARD_REPRINT_SUCCESS.getCode()
					: StatusUtil.UIN_CARD_REPRINT_FAILED.getCode();
			auditLogRequestBuilder.createAuditRequestBuilder(message, eventId, eventName, eventType, moduleId,
					moduleName, packetGeneratorResDto.getRegistrationId());
		}
	}

	/**
	 * Creates the registration DTO object.
	 *
	 * @param uin              the uin
	 * @param registrationType the registration type
	 * @param centerId         the center id
	 * @param machineId        the machine id
	 * @param vid              the vid
	 * @param cardType         the card type
	 * @return the registration DTO
	 * @throws RegBaseCheckedException the reg base checked exception
	 */
	private RegistrationDTO createRegistrationDTOObject(String uin, String registrationType, String centerId,
			String machineId, String vid, String cardType) throws RegBaseCheckedException, IOException {
		RegistrationDTO registrationDTO = new RegistrationDTO();
		registrationDTO.setDemographicDTO(getDemographicDTO(uin));
		Map<String, String> metadata = getRegistrationMetaData(uin, registrationType, centerId,
				machineId, vid, cardType);
		String registrationId = generateRegistrationId(centerId,
				machineId);
		registrationDTO.setRegistrationId(registrationId);
		registrationDTO.setMetadata(metadata);
		return registrationDTO;

	}

	/**
	 * Gets the registration meta data DTO.
	 *
	 * @param uin              the uin
	 * @param registrationType the registration type
	 * @param centerId         the center id
	 * @param machineId        the machine id
	 * @param vid              the vid
	 * @param cardType         the card type
	 * @return the registration meta data DTO
	 */
	private Map<String, String> getRegistrationMetaData(String uin, String registrationType, String centerId,
														String machineId, String vid, String cardType) {
		Map<String, String> metadata = new HashMap<>();

		metadata.put(PacketMetaInfoConstants.CENTERID, centerId);
		metadata.put(PacketMetaInfoConstants.MACHINEID, machineId);
		metadata.put(PacketMetaInfoConstants.REGISTRATION_TYPE, registrationType);
		metadata.put(PacketMetaInfoConstants.UIN, uin);
		metadata.put(PacketMetaInfoConstants.VID, vid);
		metadata.put(PacketMetaInfoConstants.CARD_TYPE, cardType);
		return metadata;
	}

	/**
	 * Gets the demographic DTO.
	 *
	 * @param uin the uin
	 * @return the demographic DTO
	 */
	private DemographicDTO getDemographicDTO(String uin) throws IOException {
		DemographicDTO demographicDTO = new DemographicDTO();
		JSONObject jsonObject = new JSONObject();

		JSONObject regProcessorIdentityJson = utilities.getRegistrationProcessorMappingJson();
		String schemaVersion = JsonUtil.getJSONValue(
				JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.IDSCHEMA_VERSION),
				MappingJsonConstants.VALUE);

		String uinLabel = JsonUtil.getJSONValue(
				JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.UIN),
				MappingJsonConstants.VALUE);

		jsonObject.put(schemaVersion, Float.valueOf(idschemaVersion));
		jsonObject.put(uinLabel, uin);
		demographicDTO.setIdentity(jsonObject);
		return demographicDTO;
	}

	/**
	 * Generate registration id.
	 *
	 * @param centerId  the center id
	 * @param machineId the machine id
	 * @return the string
	 * @throws RegBaseCheckedException the reg base checked exception
	 */
	private String generateRegistrationId(String centerId, String machineId) throws RegBaseCheckedException {

		List<String> pathsegments = new ArrayList<>();
		pathsegments.add(centerId);
		pathsegments.add(machineId);
		String rid = null;
		ResponseWrapper<?> responseWrapper;
		JSONObject ridJson;
		ObjectMapper mapper = new ObjectMapper();
		try {

			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", "UinCardRePrintServiceImpl::generateRegistrationId():: RIDgeneration Api call started");
			responseWrapper = (ResponseWrapper<?>) restClientService.getApi(ApiName.RIDGENERATION, pathsegments, "", "",
					ResponseWrapper.class);
			if (responseWrapper.getErrors() == null) {
				ridJson = mapper.readValue(mapper.writeValueAsString(responseWrapper.getResponse()), JSONObject.class);
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), "",
						"\"UinCardRePrintServiceImpl::generateRegistrationId():: RIDgeneration Api call  ended with response data : "
								+ JsonUtil.objectMapperObjectToJson(ridJson));
				rid = (String) ridJson.get("rid");

			} else {
				List<ErrorDTO> error = responseWrapper.getErrors();
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), "",
						"\"UinCardRePrintServiceImpl::generateRegistrationId():: RIDgeneration Api call  ended with response data : "
								+ error.get(0).getMessage());
				throw new RegBaseCheckedException(PlatformErrorMessages.RPR_PGS_REG_BASE_EXCEPTION,
						error.get(0).getMessage(), new Throwable());
			}

		} catch (ApisResourceAccessException e) {
			if (e.getCause() instanceof HttpClientErrorException) {
				throw new RegBaseCheckedException(PlatformErrorMessages.RPR_PGS_REG_BASE_EXCEPTION, e.getMessage(), e);
			}
		} catch (IOException e) {
			throw new RegBaseCheckedException(PlatformErrorMessages.RPR_PGS_REG_BASE_EXCEPTION, e.getMessage(), e);
		}
		return rid;
	}

	/**
	 * Checks if is valid uin VID.
	 *
	 * @param uinCardRePrintRequestDto the uin card re print request dto
	 * @return true, if is valid uin VID
	 * @throws RegBaseCheckedException the reg base checked exception
	 */
	public boolean isValidUinVID(UinCardRePrintRequestDto uinCardRePrintRequestDto) throws RegBaseCheckedException {
		boolean isValid = false;
		if (uinCardRePrintRequestDto.getRequest().getIdType().equalsIgnoreCase(UIN)) {
			isValid = validator.isValidUin(uinCardRePrintRequestDto.getRequest().getId());
		} else if (uinCardRePrintRequestDto.getRequest().getIdType().equalsIgnoreCase(VID)) {
			isValid = validator.isValidVid(uinCardRePrintRequestDto.getRequest().getId());
		}
		return isValid;
	}
}
