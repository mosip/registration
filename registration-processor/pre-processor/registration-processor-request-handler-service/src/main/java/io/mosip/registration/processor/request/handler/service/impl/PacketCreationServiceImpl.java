package io.mosip.registration.processor.request.handler.service.impl;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.idobjectvalidator.exception.IdObjectIOException;
import io.mosip.kernel.core.idobjectvalidator.exception.IdObjectValidationFailedException;
import io.mosip.kernel.core.idobjectvalidator.spi.IdObjectValidator;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.packetmanager.spi.PacketCreator;
import io.mosip.registration.packetmananger.dto.AuditDto;
import io.mosip.registration.packetmananger.exception.PacketCreatorException;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.util.ServerUtil;
import io.mosip.registration.processor.packet.utility.exception.ApiNotAccessibleException;
import io.mosip.registration.processor.packet.utility.utils.IdSchemaUtils;
import io.mosip.registration.processor.request.handler.service.PacketCreationService;
import io.mosip.registration.processor.request.handler.service.builder.AuditRequestBuilder;
import io.mosip.registration.processor.request.handler.service.constants.RegistrationConstants;
import io.mosip.registration.processor.request.handler.service.dto.RegistrationDTO;
import io.mosip.registration.processor.request.handler.service.exception.RegBaseCheckedException;
import io.mosip.registration.processor.request.handler.service.external.ZipCreationService;
import io.mosip.registration.processor.request.handler.service.utils.EncryptorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class for creating the Resident Registration
 * 
 * @author Sowmya
 * @since 1.0.0
 *
 */
@Service
public class PacketCreationServiceImpl implements PacketCreationService {

	private static final String loggerMessage = "Byte array of %s file generated successfully";

	@Autowired
	private ZipCreationService zipCreationService;

	@Autowired
	private IdObjectValidator idObjectSchemaValidator;

	@Autowired
	private Environment environment;

	@Autowired
	private PacketCreator packetCreator;

	@Autowired
	private EncryptorUtil encryptorUtil;

	@Value("${IDSchema.Version}")
	private String idschemaVersion;

	@Autowired
	private IdSchemaUtils idSchemaUtils;

	private String creationTime = null;

	private static Logger regProcLogger = RegProcessorLogger.getLogger(PacketCreationServiceImpl.class);

	private static Map<String, String> categoryPacketMapping = new HashMap<>();

	static {
		categoryPacketMapping.put("pvt", "id");
		categoryPacketMapping.put("kyc", "id");
		categoryPacketMapping.put("none", "id");
		categoryPacketMapping.put("evidence", "evidence");
		categoryPacketMapping.put("optional", "optional");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.PacketCreationService#create(io.mosip.
	 * registration.dto.RegistrationDTO)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public byte[] create(final RegistrationDTO registrationDTO , String centerId, String machineId)
			throws RegBaseCheckedException, IdObjectValidationFailedException, IdObjectIOException, IOException {
		String rid = registrationDTO.getRegistrationId();
		try {

			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					rid, "PacketCreationServiceImpl ::create()::entry");

			packetCreator.initialize();

			// id object creation
			HashMap<String, Object> idobjectMap = registrationDTO.getDemographicDTO().getDemographicInfoDTO().getIdentity();
			idobjectMap.keySet().forEach(id -> {
				packetCreator.setField(id, idobjectMap.get(id));
			});

			// audit log creation
			packetCreator.setAudits(generateAudit(rid));

			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					rid, String.format(loggerMessage, RegistrationConstants.AUDIT_JSON_FILE));

			// Meta-Info JSON creation
			Map<String, String> metadata = registrationDTO.getMetadata();
			for (String field : metadata.keySet()) {
				packetCreator.setMetaInfo(field, metadata.get(field));
			}
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					rid, String.format(loggerMessage, RegistrationConstants.PACKET_META_JSON_NAME));

			String refId = centerId + "_" + machineId;
			float idschema = Float.valueOf(idschemaVersion);
			String idSchema = idSchemaUtils.getIdSchema();

			byte[] packetZip = packetCreator.createPacket(registrationDTO.getRegistrationId(), idschema,
					idSchema, categoryPacketMapping, encryptorUtil.getPublickey(refId).getPublicKey().getBytes(), null);

			return packetZip;
		} catch (RuntimeException | PacketCreatorException | ApisResourceAccessException | ApiNotAccessibleException runtimeException) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					rid, PlatformErrorMessages.RPR_SYS_SERVER_ERROR.getMessage()
							+ ExceptionUtils.getStackTrace(runtimeException));
			throw new RegBaseCheckedException(PlatformErrorMessages.RPR_SYS_SERVER_ERROR, runtimeException);
		}
	}

	private List<AuditDto> generateAudit(String rid) {
		AuditRequestBuilder auditRequestBuilder = new AuditRequestBuilder();
		// Getting Host IP Address and Name
		String hostIP = null;
		String hostName = null;
		try {
			hostIP = InetAddress.getLocalHost().getHostAddress();
			hostName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException unknownHostException) {

			hostIP = ServerUtil.getServerUtilInstance().getServerIp();
			hostName = ServerUtil.getServerUtilInstance().getServerName();
		}
		auditRequestBuilder.setActionTimeStamp(LocalDateTime.now(ZoneOffset.UTC))
				.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC)).setUuid("")
				.setApplicationId(environment.getProperty(RegistrationConstants.AUDIT_APPLICATION_ID))
				.setApplicationName(environment.getProperty(RegistrationConstants.AUDIT_APPLICATION_NAME))
				.setCreatedBy("Packet_Generator").setDescription("Packet uploaded successfully")
				.setEventId("RPR_405").setEventName("packet uploaded").setEventType("USER").setHostIp(hostIP)
				.setHostName(hostName).setId(rid).setIdType("REGISTRATION_ID").setModuleId("REG - MOD - 119")
				.setModuleName("REQUEST_HANDLER_SERVICE").setSessionUserId("mosip")
				.setSessionUserName("Registration");
		AuditDto auditDto = auditRequestBuilder.build();
		List<AuditDto> auditDTOS = new ArrayList<AuditDto>();
		auditDTOS.add(auditDto);
		return auditDTOS;
	}

	@Override
	public String getCreationTime() {
		return creationTime;
	}

}
