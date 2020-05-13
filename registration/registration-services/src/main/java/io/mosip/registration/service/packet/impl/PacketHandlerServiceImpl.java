package io.mosip.registration.service.packet.impl;

import static io.mosip.registration.constants.LoggerConstants.LOG_PKT_HANLDER;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;
import static io.mosip.registration.exception.RegistrationExceptionConstants.REG_PACKET_CREATION_ERROR_CODE;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.mosip.kernel.auditmanager.entity.Audit;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.builder.Builder;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.AuditDAO;
import io.mosip.registration.dao.AuditLogControlDAO;
import io.mosip.registration.dao.PolicySyncDAO;
import io.mosip.registration.dao.RegistrationDAO;
import io.mosip.registration.dto.ErrorResponseDTO;
import io.mosip.registration.dto.RegistrationCenterDetailDTO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.SuccessResponseDTO;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.dto.json.metadata.CustomDigitalId;
import io.mosip.registration.dto.json.metadata.RegisteredDevice;
import io.mosip.registration.dto.response.SchemaDto;
import io.mosip.registration.entity.AuditLogControl;
import io.mosip.registration.entity.KeyStore;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.mdm.service.impl.MosipBioDeviceManager;
import io.mosip.registration.packetmanager.spi.PacketCreator;
import io.mosip.registration.packetmananger.constants.PacketManagerConstants;
import io.mosip.registration.packetmananger.dto.AuditDto;
import io.mosip.registration.packetmananger.dto.BiometricsDto;
import io.mosip.registration.packetmananger.dto.DocumentDto;
import io.mosip.registration.packetmananger.dto.metadata.BiometricsException;
import io.mosip.registration.packetmananger.exception.PacketCreatorException;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.IdentitySchemaService;
import io.mosip.registration.service.external.StorageService;
import io.mosip.registration.service.packet.PacketHandlerService;
import io.mosip.registration.update.SoftwareUpdateHandler;
import io.mosip.registration.util.checksum.CheckSumUtil;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;

/**
 * The implementation class of {@link PacketHandlerService} to handle the
 * registration data to create packet out of it and save the encrypted packet
 * data in the configured local system
 * 
 * @author Balaji Sridharan
 * @since 1.0.0
 *
 */
@Service
public class PacketHandlerServiceImpl extends BaseService implements PacketHandlerService {

	private static final Logger LOGGER = AppConfig.getLogger(PacketHandlerServiceImpl.class);
	
	@Value("${mosip.registration.gps_device_enable_flag}")
	private String gpsEnabledFlag;

	/**
	 * Instance of {@code AuditFactory}
	 */
	@Autowired
	private AuditManagerService auditFactory;
	
	@Autowired
	private PolicySyncDAO policySyncDAO;
	
	@Autowired
	private RegistrationDAO registrationDAO;
	
	@Autowired
	private AuditLogControlDAO auditLogControlDAO;
	
	@Autowired
	private IdentitySchemaService identitySchemaService;
	
	@Autowired
	private PacketCreator packetCreator;
	
	@Autowired
	private StorageService storageService;
	
	@Autowired
	private AuditDAO auditDAO;
	
	@Autowired
	private SoftwareUpdateHandler softwareUpdateHandler;
	
	
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
	 * @see
	 * io.mosip.registration.service.packet.PacketHandlerService#handle(io.mosip.
	 * registration.dto.RegistrationDTO)
	 */
	@Override
	public ResponseDTO handle(RegistrationDTO registrationDTO) {		
		LOGGER.info(LOG_PKT_HANLDER, APPLICATION_NAME, APPLICATION_ID, "Registration Handler had been called");
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setErrorResponseDTOs(new ArrayList<>());
		ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO();
		
		if(registrationDTO == null ||  registrationDTO.getRegistrationId() == null) {
			errorResponseDTO.setCode(REG_PACKET_CREATION_ERROR_CODE.getErrorCode());
			errorResponseDTO.setMessage(REG_PACKET_CREATION_ERROR_CODE.getErrorMessage());
			responseDTO.getErrorResponseDTOs().add(errorResponseDTO);
			return responseDTO;
		}			
		
		try {			
			SchemaDto schema = identitySchemaService.getIdentitySchema(registrationDTO.getIdSchemaVersion());			
			//TODO validate idObject with IDSchema
			
			packetCreator.initialize();		
			setDemographics(registrationDTO.getDemographics());
			setDocuments(registrationDTO.getDocuments());
			setBiometrics(registrationDTO.getBiometrics(), registrationDTO.getBiometricExceptions(), schema);
			setOtherDetails(registrationDTO);
			
			packetCreator.setAcknowledgement(registrationDTO.getAcknowledgeReceiptName(), registrationDTO.getAcknowledgeReceipt());			
			collectAudits();
			byte[] packetZip = packetCreator.createPacket(registrationDTO.getRegistrationId(), registrationDTO.getIdSchemaVersion(),
					schema.getSchemaJson(), categoryPacketMapping, getPublicKeyToEncrypt(), null);				
			
			String filePath = savePacketToDisk(registrationDTO.getRegistrationId(), packetZip);
			registrationDAO.save(filePath, registrationDTO);			
			//createAuditLog(registrationDTO);		
			SuccessResponseDTO successResponseDTO = new SuccessResponseDTO();
			successResponseDTO.setCode("0000");
			successResponseDTO.setMessage("Success");
			responseDTO.setSuccessResponseDTO(successResponseDTO);
			
		} catch (PacketCreatorException | RegBaseCheckedException e) {
			LOGGER.info(LOG_PKT_HANLDER, APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(e));

			auditFactory.audit(AuditEvent.PACKET_INTERNAL_ERROR, Components.PACKET_HANDLER, 
					registrationDTO.getRegistrationId(),
					AuditReferenceIdTypes.REGISTRATION_ID.getReferenceTypeId());

			errorResponseDTO.setCode(e.getErrorCode());
			errorResponseDTO.setMessage(e.getErrorText());
			responseDTO.getErrorResponseDTOs().add(errorResponseDTO);
		}
		LOGGER.info(LOG_PKT_HANLDER, APPLICATION_NAME, APPLICATION_ID, "Registration Handler had been ended");

		return responseDTO;
	}
	
	private void setDemographics(Map<String, Object> demographics) {
		for(String fieldName : demographics.keySet()) {
			packetCreator.setField(fieldName, demographics.get(fieldName));
		}
	}
	
	private void setDocuments(Map<String, DocumentDto> documents) {
		for(String fieldName : documents.keySet()) {
			packetCreator.setField(fieldName, documents.get(fieldName));
		}
	}
	
	private void setBiometrics(Map<String, BiometricsDto> biometrics, Map<String, BiometricsException> exceptions, 
			SchemaDto schema) {
		List<UiSchemaDTO> biometricFields = schema.getSchema().stream()
												.filter(field -> 
													PacketManagerConstants.BIOMETRICS_DATATYPE.equals(field.getType()) 
													&& field.getSubType() != null 
													&& field.getBioAttributes() != null)
												.collect(Collectors.toList());		
		
		for(UiSchemaDTO biometricField : biometricFields) {	
			List<BiometricsDto> list = new ArrayList<>();
			List<BiometricsException> exceptionList = new ArrayList<>();
			for(String attribute : biometricField.getBioAttributes()) {
				String key = String.format("%s_%s", biometricField.getSubType(), attribute);
				if(biometrics.containsKey(key))
					list.add(biometrics.get(key));
				else if(exceptions.containsKey(key))
					exceptionList.add(exceptions.get(key));
			}
			packetCreator.setBiometric(biometricField.getId(), list);
			packetCreator.setBiometricException(biometricField.getId(), exceptionList);
		}
	}
	
	private byte[] getPublicKeyToEncrypt() throws RegBaseCheckedException {
		String stationId = getStationId(RegistrationSystemPropertiesChecker.getMachineId());
		String centerMachineId = getCenterId(stationId) + "_" + stationId;
		KeyStore keyStore = policySyncDAO.getPublicKey(centerMachineId);
		if(keyStore != null && keyStore.getPublicKey() != null)
			return keyStore.getPublicKey();
		
		throw new RegBaseCheckedException(RegistrationExceptionConstants.REG_RSA_PUBLIC_KEY_NOT_FOUND.getErrorCode(),
				RegistrationExceptionConstants.REG_RSA_PUBLIC_KEY_NOT_FOUND.getErrorMessage());
	}
	
	private String savePacketToDisk(String registrationId, byte[] packetZip) throws RegBaseCheckedException {
		long maxPacketSizeInBytes = Long.valueOf(String.valueOf(ApplicationContext.map().get(RegistrationConstants.REG_PKT_SIZE))) * 1024 * 1024;
		if (packetZip.length > maxPacketSizeInBytes) {
			throw new RegBaseCheckedException(RegistrationExceptionConstants.REG_PACKET_SIZE_EXCEEDED_ERROR_CODE.getErrorCode(),
							RegistrationExceptionConstants.REG_PACKET_SIZE_EXCEEDED_ERROR_CODE.getErrorMessage());
		}
		
		return storageService.storeToDisk(registrationId, packetZip);
	}
	
	//TODO only based on registrationId
	private void collectAudits() {
		List<AuditDto> list = new ArrayList<>();
		List<Audit> audits = auditDAO.getAudits(auditLogControlDAO.getLatestRegistrationAuditDates());
		for(Audit audit : audits) {
			AuditDto dto = new AuditDto();
			dto.setActionTimeStamp(audit.getActionTimeStamp());
			dto.setApplicationId(audit.getApplicationId());
			dto.setApplicationName(audit.getApplicationName());
			dto.setCreatedBy(audit.getCreatedBy());
			dto.setDescription(audit.getDescription());
			dto.setEventId(audit.getEventId());
			dto.setEventName(audit.getEventName());
			dto.setEventType(audit.getEventType());
			dto.setHostIp(audit.getHostIp());
			dto.setHostName(audit.getHostName());
			dto.setId(audit.getId());
			dto.setIdType(audit.getIdType());
			dto.setModuleId(audit.getModuleId());
			dto.setModuleName(audit.getModuleName());
			dto.setSessionUserId(audit.getSessionUserId());
			dto.setSessionUserName(audit.getSessionUserName());
			list.add(dto);
			break;
		}
		packetCreator.setAudits(list);
	}
	
	private List<RegisteredDevice> getRegisteredDevices() {

		List<RegisteredDevice> capturedRegisteredDevices = new ArrayList<>();

		MosipBioDeviceManager.getDeviceRegistry().forEach((deviceName, device) -> {
			RegisteredDevice registerdDevice = new RegisteredDevice();
			registerdDevice.setDeviceServiceVersion(device.getSerialVersion());
			registerdDevice.setDeviceCode(device.getDigitalId().getSerialNo());
			CustomDigitalId digitalId = new CustomDigitalId();
			digitalId.setDateTime(device.getDigitalId().getDateTime());
			digitalId.setDp(device.getDigitalId().getDeviceProvider());
			digitalId.setDpId(device.getDigitalId().getDeviceProviderId());
			digitalId.setMake(device.getDigitalId().getMake());
			digitalId.setModel(device.getDigitalId().getModel());
			digitalId.setSerialNo(device.getDigitalId().getSerialNo());
			digitalId.setSubType(device.getDigitalId().getSubType());
			digitalId.setType(device.getDigitalId().getType());
			registerdDevice.setDigitalId(digitalId);
			capturedRegisteredDevices.add(registerdDevice);
		});

		return capturedRegisteredDevices;
	}
	
	private void createAuditLog(RegistrationDTO registrationDTO) {
		auditLogControlDAO.save(Builder.build(AuditLogControl.class)
				.with(auditLogControl -> auditLogControl.setAuditLogFromDateTime(registrationDTO.getAuditLogStartTime()))
				.with(auditLogControl -> auditLogControl.setAuditLogToDateTime(registrationDTO.getAuditLogEndTime()))
				.with(auditLogControl -> auditLogControl.setRegistrationId(registrationDTO.getRegistrationId()))
				.with(auditLogControl -> auditLogControl.setAuditLogSyncDateTime(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime())))
				.with(auditLogControl -> auditLogControl.setCrDtime(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime())))
				.with(auditLogControl -> auditLogControl.setCrBy(SessionContext.userContext().getUserId()))
				.get());
	}
	
	private void setOtherDetails(RegistrationDTO registrationDTO) {
		packetCreator.setMetaInfo("centerId", (String) ApplicationContext.map().get(RegistrationConstants.USER_CENTER_ID));
		packetCreator.setMetaInfo("machineId", (String) ApplicationContext.map().get(RegistrationConstants.USER_STATION_ID));
		packetCreator.setMetaInfo("keyIndex", "");
		packetCreator.setMetaInfo("Registration Client Version Number", softwareUpdateHandler.getCurrentVersion());	
		packetCreator.setMetaInfo("preRegistrationId", registrationDTO.getPreRegistrationId());
		packetCreator.setMetaInfo("consentOfApplicant",	registrationDTO.getRegistrationMetaDataDTO().getConsentOfApplicant());
		RegistrationCenterDetailDTO registrationCenter = SessionContext.userContext().getRegistrationCenterDetailDTO();		
		if (RegistrationConstants.ENABLE.equalsIgnoreCase(gpsEnabledFlag) && registrationCenter != null) {
			packetCreator.setMetaInfo("geoLocLatitude", registrationCenter.getRegistrationCenterLatitude());
			packetCreator.setMetaInfo("geoLoclongitude", registrationCenter.getRegistrationCenterLongitude());
		}
		
		Map<String, String> checkSumMap = CheckSumUtil.getCheckSumMap();
		checkSumMap.forEach((key, value) -> packetCreator.setCheckSum(key, value));
		
		//TODO - Need to change this logic
		packetCreator.setOperationsInfo("officerId", registrationDTO.getOsiDataDTO().getOperatorID());
		packetCreator.setOperationsInfo("supervisorId", registrationDTO.getOsiDataDTO().getSupervisorID());
		packetCreator.setOperationsInfo("officerBiometricFileName", null);
		packetCreator.setOperationsInfo("supervisorBiometricFileName", null);
		packetCreator.setOperationsInfo("supervisorPassword", String.valueOf(registrationDTO.getOsiDataDTO().isSuperviorAuthenticatedByPassword()));
		packetCreator.setOperationsInfo("officerPassword", String.valueOf(registrationDTO.getOsiDataDTO().isSuperviorAuthenticatedByPassword()));
		packetCreator.setOperationsInfo("supervisorPIN", null);
		packetCreator.setOperationsInfo("officerPIN", null);
		packetCreator.setOperationsInfo("supervisorOTPAuthentication", String.valueOf(registrationDTO.getOsiDataDTO().isSuperviorAuthenticatedByPIN()));
		packetCreator.setOperationsInfo("officerOTPAuthentication", String.valueOf(registrationDTO.getOsiDataDTO().isOperatorAuthenticatedByPIN()));
	}
}
