package io.mosip.registration.processor.request.handler.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.PacketMetaInfoConstants;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.PacketMetaInfoConstants;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.spi.filesystem.manager.FileManager;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.manager.dto.DirectoryPathDto;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.request.handler.service.PacketCreationService;
import io.mosip.registration.processor.request.handler.service.PacketGeneratorService;
import io.mosip.registration.processor.request.handler.service.dto.PackerGeneratorFailureDto;
import io.mosip.registration.processor.request.handler.service.dto.PacketGeneratorDto;
import io.mosip.registration.processor.request.handler.service.dto.PacketGeneratorResDto;
import io.mosip.registration.processor.request.handler.service.dto.RegistrationDTO;
import io.mosip.registration.processor.request.handler.service.dto.demographic.DemographicDTO;
import io.mosip.registration.processor.request.handler.service.exception.RegBaseCheckedException;
import io.mosip.registration.processor.request.handler.upload.SyncUploadEncryptionService;
import io.mosip.registration.processor.request.handler.upload.validator.RequestHandlerRequestValidator;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;

/**
 * @author Sowmya The Class PacketGeneratorServiceImpl.
 */
@Service
@Qualifier("packetGeneratorService")
public class PacketGeneratorServiceImpl implements PacketGeneratorService<PacketGeneratorDto> {

	/** The packet creation service. */
	@Autowired
	private PacketCreationService packetCreationService;

	/** The sync upload encryption service. */
	@Autowired
	SyncUploadEncryptionService syncUploadEncryptionService;

	@Autowired
	private Utilities utilities;

	@Value("${IDSchema.Version}")
	private String idschemaVersion;

	/** The rest client service. */
	@Autowired
	private RegistrationProcessorRestClientService<Object> restClientService;

	/** The primary languagecode. */
	@Value("${mosip.primary-language}")
	private String primaryLanguagecode;

	private static Logger regProcLogger = RegProcessorLogger.getLogger(PacketCreationServiceImpl.class);

	/** The filemanager. */
	@Autowired
	protected FileManager<DirectoryPathDto, InputStream> filemanager;

	@Autowired
	RequestHandlerRequestValidator validator;

	@Autowired
	private AuditLogRequestBuilder auditLogRequestBuilder;

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.processor.packet.service.PacketGeneratorService#
	 * createPacket(io.mosip.registration.processor.packet.service.dto.
	 * PacketGeneratorDto)
	 */
	@Override
	public PacketGeneratorResDto createPacket(PacketGeneratorDto request) throws RegBaseCheckedException, IOException {
		boolean isTransactional = false;
		PacketGeneratorResDto packerGeneratorResDto = null;
		PackerGeneratorFailureDto dto = new PackerGeneratorFailureDto();
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"PacketGeneratorServiceImpl ::createPacket()::entry");
		byte[] packetZipBytes = null;
		try {
			if (validator.isValidCenter(request.getCenterId()) && validator.isValidMachine(request.getMachineId())
					&& validator.isValidRegistrationTypeAndUin(request.getRegistrationType(), request.getUin())) {
				try {
					regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), "",
							"Packet Generator Validation successfull");
					RegistrationDTO registrationDTO = createRegistrationDTOObject(request.getUin(),
							request.getRegistrationType(), request.getCenterId(), request.getMachineId());
					packetZipBytes = packetCreationService.create(registrationDTO, request.getCenterId(), request.getMachineId());
					String rid = registrationDTO.getRegistrationId();
					String packetCreatedDateTime = rid.substring(rid.length() - 14);
					String formattedDate = packetCreatedDateTime.substring(0, 8) + "T"
							+ packetCreatedDateTime.substring(packetCreatedDateTime.length() - 6);
					LocalDateTime ldt = LocalDateTime.parse(formattedDate,
							DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
					String creationTime = ldt.toString() + ".000Z";

					packerGeneratorResDto = syncUploadEncryptionService.uploadUinPacket(
							registrationDTO.getRegistrationId(), creationTime, request.getRegistrationType(),
							packetZipBytes);
					isTransactional = true;
					return packerGeneratorResDto;
				} catch (Exception e) {
					regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(),
							PlatformErrorMessages.RPR_PGS_REG_BASE_EXCEPTION.getMessage(),
							ExceptionUtils.getStackTrace(e));
					if (e instanceof RegBaseCheckedException) {
						throw (RegBaseCheckedException) e;
					}
					throw new RegBaseCheckedException(StatusUtil.UNKNOWN_EXCEPTION_OCCURED, e);

				}
			} else
				return dto;
		} finally {
			String eventId = isTransactional ? EventId.RPR_402.toString() : EventId.RPR_405.toString();
			String eventName = isTransactional ? EventName.UPDATE.toString() : EventName.EXCEPTION.toString();
			String eventType = isTransactional ? EventType.BUSINESS.toString() : EventType.SYSTEM.toString();
			String message = isTransactional ? StatusUtil.RESIDENT_UPDATE_SUCCES.getMessage()
					: StatusUtil.RESIDENT_UPDATE_FAILED.getMessage();
			String moduleName = ModuleName.REQUEST_HANDLER_SERVICE.toString();
			String moduleId = isTransactional ? StatusUtil.PACKET_GENERATION_SUCCESS.getCode()
					: StatusUtil.PACKET_GENERATION_FAILED.getCode();
			auditLogRequestBuilder.createAuditRequestBuilder(message, eventId, eventName, eventType, moduleId,
					moduleName, dto.getRegistrationId());
		}
	}

	/**
	 * Creates the registration DTO object.
	 *
	 * @param uin              the uin
	 * @param registrationType the registration type
	 * @param centerId         the center id
	 * @param machineId        the machine id
	 * @return the registration DTO
	 * @throws RegBaseCheckedException
	 */
	private RegistrationDTO createRegistrationDTOObject(String uin, String registrationType, String centerId,
			String machineId) throws RegBaseCheckedException, IOException {
		RegistrationDTO registrationDTO = new RegistrationDTO();
		registrationDTO.setDemographicDTO(getDemographicDTO(uin));
		Map<String, String> metadata = getRegistrationMetaData(registrationType, uin, centerId,
				machineId);
		String registrationId = generateRegistrationId(centerId,
				machineId);
		registrationDTO.setRegistrationId(registrationId);
		registrationDTO.setMetadata(metadata);
		return registrationDTO;

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
	 * Gets the registration meta data DTO.
	 *
	 * @param registrationType the registration type
	 * @param uin              the uin
	 * @param centerId         the center id
	 * @param machineId        the machine id
	 * @return the registration meta data DTO
	 */
	private Map<String, String> getRegistrationMetaData(String registrationType, String uin, String centerId,
																 String machineId) {
		Map<String, String> metadata = new HashMap<>();
		metadata.put(PacketMetaInfoConstants.CENTERID, centerId);
		metadata.put(PacketMetaInfoConstants.MACHINEID, machineId);
		metadata.put(PacketMetaInfoConstants.REGISTRATION_TYPE, registrationType);
		metadata.put(PacketMetaInfoConstants.UIN, uin);
		return metadata;
	}

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
					"", "PacketGeneratorServiceImpl::generateRegistrationId():: RIDgeneration Api call started");
			responseWrapper = (ResponseWrapper<?>) restClientService.getApi(ApiName.RIDGENERATION, pathsegments, "", "",
					ResponseWrapper.class);
			if (responseWrapper.getErrors() == null) {
				ridJson = mapper.readValue(mapper.writeValueAsString(responseWrapper.getResponse()), JSONObject.class);
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), "",
						"\"PacketGeneratorServiceImpl::generateRegistrationId():: RIDgeneration Api call  ended with response data : "
								+ JsonUtil.objectMapperObjectToJson(ridJson));
				rid = (String) ridJson.get("rid");

			} else {
				List<ErrorDTO> error = responseWrapper.getErrors();
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), "",
						"\"PacketGeneratorServiceImpl::generateRegistrationId():: RIDgeneration Api call  ended with response data : "
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
}
