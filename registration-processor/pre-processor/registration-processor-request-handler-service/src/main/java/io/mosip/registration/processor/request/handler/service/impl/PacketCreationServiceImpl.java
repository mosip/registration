package io.mosip.registration.processor.request.handler.service.impl;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.idobjectvalidator.exception.IdObjectIOException;
import io.mosip.kernel.core.idobjectvalidator.exception.IdObjectValidationFailedException;
import io.mosip.kernel.core.idobjectvalidator.spi.IdObjectValidator;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.packetmanager.spi.PacketCreator;
import io.mosip.registration.packetmananger.constants.PacketManagerConstants;
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
import io.mosip.registration.processor.request.handler.service.dto.json.FieldValueArray;
import io.mosip.registration.processor.request.handler.service.dto.json.HashSequence;
import io.mosip.registration.processor.request.handler.service.exception.RegBaseCheckedException;
import io.mosip.registration.processor.request.handler.service.external.ZipCreationService;
import io.mosip.registration.processor.request.handler.service.utils.EncryptorUtil;
import org.json.JSONException;
import org.json.JSONObject;
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
import java.util.LinkedList;
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

			packetCreator.initialize();

			String loggerMessage = "Byte array of %s file generated successfully";

			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					rid, "PacketCreationServiceImpl ::create()::entry");

			// Map object to store the byte array of JSON objects namely Demographic, HMAC,
			// Packet Meta-Data and Audit
			//Map<String, byte[]> filesGeneratedForPacket = new HashMap<>();
			/*JSONObject idObject = null;
			String idJsonAsString = null;*/
			
			/*if(demoJsonObject != null) {
				idObject = demoJsonObject;
				idJsonAsString = idObject.toJSONString();
			}else {
				// Generating Demographic JSON as byte array
				*//*idJsonAsString = javaObjectToJsonString(registrationDTO.getDemographicDTO().getDemographicInfoDTO());
				ObjectMapper mapper = new ObjectMapper();
				idObject = mapper.readValue(idJsonAsString, JSONObject.class);*//*
				//idObject = registrationDTO.getDemographicDTO().getDemographicInfoDTO().getIdentity();
			}*/


			//filesGeneratedForPacket.put(DEMOGRPAHIC_JSON_NAME, idJsonAsString.getBytes());
			HashMap<String, Object> idobjectMap = registrationDTO.getDemographicDTO().getDemographicInfoDTO().getIdentity();
			idobjectMap.keySet().forEach(id -> {
				packetCreator.setField(id, idobjectMap.get(id));
			});

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

			packetCreator.setAudits(auditDTOS);

			/*filesGeneratedForPacket.put(RegistrationConstants.AUDIT_JSON_FILE,
					javaObjectToJsonString(auditDto).getBytes());*/
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					rid, String.format(loggerMessage, RegistrationConstants.AUDIT_JSON_FILE));

			// Generating Packet Meta-Info JSON as byte array
			Map<String, String> metadata = registrationDTO.getMetadata();
			for (String field : metadata.keySet()) {
				packetCreator.setMetaInfo(field, metadata.get(field));
			}
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					rid, String.format(loggerMessage, RegistrationConstants.PACKET_META_JSON_NAME));

			String refId = centerId + "_" + machineId;
			float idschema = Float.valueOf(idschemaVersion);
			String idSchema = (String) idSchemaUtils.getIdSchema();

			JSONObject schema = null;
			try {
				schema = new JSONObject(idSchema);
				schema =  schema.getJSONObject(PacketManagerConstants.PROPERTIES);
				schema =  schema.getJSONObject(PacketManagerConstants.IDENTITY);
				schema =  schema.getJSONObject(PacketManagerConstants.PROPERTIES);
			} catch (JSONException e) {
				e.printStackTrace();
			}

			byte[] packetZip = packetCreator.createPacket(registrationDTO.getRegistrationId(), idschema,
					idSchema, categoryPacketMapping, encryptorUtil.getPublickey(refId).getPublicKey().getBytes(), null);

			// Add HashSequence
			/*packetInfo.getIdentity().setHashSequence1(buildHashSequence(hashSequence));
			List<String> hashsequence2List = new ArrayList<String>();
			hashsequence2List.add("audit");
			// Add HashSequence for packet_osi_data
			packetInfo.getIdentity()
					.setHashSequence2(
							(List<FieldValueArray>) Builder.build(ArrayList.class)
									.with(values -> values.add(Builder.build(FieldValueArray.class)
											.with(field -> field.setLabel("otherFiles"))
											.with(field -> field.setValue(hashsequence2List)).get()))
									.get());
			filesGeneratedForPacket.put(RegistrationConstants.PACKET_META_JSON_NAME,
					javaObjectToJsonString(packetInfo).getBytes());
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					rid, String.format(loggerMessage, RegistrationConstants.PACKET_META_JSON_NAME));
			// Creating in-memory zip file for Packet Encryption
			byte[] packetZipBytes = zipCreationService.createPacket(registrationDTO, filesGeneratedForPacket);
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					rid, "PacketCreationServiceImpl ::create()::exit()");*/
			return packetZip;
		} catch (RuntimeException | PacketCreatorException | ApisResourceAccessException | ApiNotAccessibleException runtimeException) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					rid, PlatformErrorMessages.RPR_SYS_SERVER_ERROR.getMessage()
							+ ExceptionUtils.getStackTrace(runtimeException));
			throw new RegBaseCheckedException(PlatformErrorMessages.RPR_SYS_SERVER_ERROR, runtimeException);
		}
	}

	private List<FieldValueArray> buildHashSequence(final HashSequence hashSequence) {
		List<FieldValueArray> hashSequenceList = new LinkedList<>();
		// Add Sequence of Applicant Biometric
		FieldValueArray fieldValueArray = new FieldValueArray();

		// Add Sequence of Applicant Demographic
		fieldValueArray.setLabel("applicantDemographicSequence");
		fieldValueArray.setValue(hashSequence.getDemographicSequence().getApplicant());
		hashSequenceList.add(fieldValueArray);

		return hashSequenceList;
	}

	@Override
	public String getCreationTime() {

		return creationTime;
	}

}
