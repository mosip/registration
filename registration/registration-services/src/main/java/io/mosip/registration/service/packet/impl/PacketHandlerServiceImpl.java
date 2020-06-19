package io.mosip.registration.service.packet.impl;

import static io.mosip.registration.constants.LoggerConstants.LOG_PKT_HANLDER;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;
import static io.mosip.registration.exception.RegistrationExceptionConstants.REG_PACKET_CREATION_ERROR_CODE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import io.mosip.kernel.auditmanager.entity.Audit;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.idobjectvalidator.exception.IdObjectIOException;
import io.mosip.kernel.core.idobjectvalidator.exception.IdObjectValidationFailedException;
import io.mosip.kernel.core.idobjectvalidator.exception.InvalidIdSchemaException;
import io.mosip.kernel.core.idobjectvalidator.spi.IdObjectValidator;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.packetmanager.constants.PacketManagerConstants;
import io.mosip.kernel.packetmanager.dto.AuditDto;
import io.mosip.kernel.packetmanager.dto.BiometricsDto;
import io.mosip.kernel.packetmanager.dto.DocumentDto;
import io.mosip.kernel.packetmanager.dto.SimpleDto;
import io.mosip.kernel.packetmanager.dto.metadata.BiometricsException;
import io.mosip.kernel.packetmanager.dto.metadata.DeviceMetaInfo;
import io.mosip.kernel.packetmanager.dto.metadata.DigitalId;
import io.mosip.kernel.packetmanager.exception.PacketCreatorException;
import io.mosip.kernel.packetmanager.spi.PacketCreator;
import io.mosip.registration.audit.AuditManagerService;
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
import io.mosip.registration.dto.response.SchemaDto;
import io.mosip.registration.entity.KeyStore;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.mdm.service.impl.MosipDeviceSpecificationFactory;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.IdentitySchemaService;
import io.mosip.registration.service.external.StorageService;
import io.mosip.registration.service.packet.PacketHandlerService;
import io.mosip.registration.update.SoftwareUpdateHandler;
import io.mosip.registration.util.checksum.CheckSumUtil;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;
import io.mosip.registration.validator.RegIdObjectMasterDataValidator;

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
	
	@Autowired
	private Environment environment;
	
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
	
	@Autowired
	@Qualifier("schema")
	private IdObjectValidator idObjectValidator;
	
	@Autowired
	private RegIdObjectMasterDataValidator regIdObjectMasterDataValidator;
	

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
					
			packetCreator.initialize();				
			setDemographics(registrationDTO, schema);
			setDocuments(registrationDTO.getDocuments());
			setBiometrics(registrationDTO.getBiometrics(), registrationDTO.getBiometricExceptions(), schema);
			
			validateIdObject(schema.getSchemaJson(), packetCreator.getIdentityObject(), registrationDTO.getRegistrationCategory());
			
			setOtherDetails(registrationDTO);			
			packetCreator.setAcknowledgement(registrationDTO.getAcknowledgeReceiptName(), registrationDTO.getAcknowledgeReceipt());			
			collectAudits();
			byte[] packetZip = packetCreator.createPacket(registrationDTO.getRegistrationId(), registrationDTO.getIdSchemaVersion(),
					schema.getSchemaJson(), null, getPublicKeyToEncrypt(), null);				
			
			String filePath = savePacketToDisk(registrationDTO.getRegistrationId(), packetZip);
			registrationDAO.save(filePath, registrationDTO);			
		//	createAuditLog(registrationDTO);			
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
	
	private void setDemographics(RegistrationDTO registrationDTO, SchemaDto schema) {		
		Map<String, Object> demographics = registrationDTO.getDemographics();
		for(String fieldName : demographics.keySet()) {
			switch (registrationDTO.getRegistrationCategory()) {
			case RegistrationConstants.PACKET_TYPE_UPDATE:
				if(demographics.get(fieldName) != null && registrationDTO.getUpdatableFields().contains(fieldName))
					packetCreator.setField(fieldName, demographics.get(fieldName));				
				break;
			case RegistrationConstants.PACKET_TYPE_LOST:
				if( demographics.get(fieldName) != null)
					packetCreator.setField(fieldName, demographics.get(fieldName));
				break;
			case RegistrationConstants.PACKET_TYPE_NEW:
				packetCreator.setField(fieldName, demographics.get(fieldName));
				break;
			}
			
			if(fieldName.equals("UIN") && demographics.get(fieldName) != null) {
				packetCreator.setField(fieldName, demographics.get(fieldName));
			}							
		}
		
		String printingNameFieldId = getPrintingNameFieldName(schema);
		LOGGER.info(LOG_PKT_HANLDER, APPLICATION_NAME, APPLICATION_ID, "printingNameFieldId >>>>> " + printingNameFieldId);		
		if(demographics.get(printingNameFieldId) != null  && registrationDTO.getUpdatableFields() != null 
				&& !registrationDTO.getUpdatableFields().contains(printingNameFieldId) 
				&& demographics.containsKey(printingNameFieldId)) {
			@SuppressWarnings("unchecked")
			List<SimpleDto> value = (List<SimpleDto>) demographics.get(printingNameFieldId);
			value.forEach(dto -> {
				packetCreator.setPrintingName(dto.getLanguage(), dto.getValue());
			});				
		}
	}
	
	private void setDocuments(Map<String, DocumentDto> documents) {
		for(String fieldName : documents.keySet()) {
			packetCreator.setDocument(fieldName, documents.get(fieldName));
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
		for (Audit audit : audits) {
			AuditDto dto = new AuditDto();
			dto.setActionTimeStamp(audit.getActionTimeStamp());
			dto.setApplicationId(!audit.getApplicationId().equalsIgnoreCase("null") ? audit.getApplicationId() : null);
			dto.setApplicationName(
					!audit.getApplicationName().equalsIgnoreCase("null") ? audit.getApplicationName() : null);
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
		}
		packetCreator.setAudits(list);
	}
	
	private void addRegisteredDevices() {
		List<DeviceMetaInfo> capturedRegisteredDevices = new ArrayList<DeviceMetaInfo>();
		MosipDeviceSpecificationFactory.getDeviceRegistryInfo().forEach((deviceName, device) -> {
			DeviceMetaInfo registerdDevice = new DeviceMetaInfo();
			registerdDevice.setDeviceServiceVersion(device.getSerialVersion());
			registerdDevice.setDeviceCode(device.getDeviceCode());
			DigitalId digitalId = new DigitalId();
			digitalId.setDateTime(device.getTimestamp());
			digitalId.setDeviceProvider(device.getDeviceProviderName());
			digitalId.setDeviceProviderId(device.getDeviceProviderId());
			digitalId.setMake(device.getDeviceMake());
			digitalId.setModel(device.getDeviceModel());
			digitalId.setSerialNo(device.getSerialNumber());
			digitalId.setSubType(device.getDeviceSubType());
			digitalId.setType(device.getDeviceType());
			registerdDevice.setDigitalId(digitalId);
			capturedRegisteredDevices.add(registerdDevice);
		});
		this.packetCreator.setRegisteredDeviceDetails(capturedRegisteredDevices);
	}
	
	/*private void createAuditLog(RegistrationDTO registrationDTO) {
		auditLogControlDAO.save(Builder.build(AuditLogControl.class)
				.with(auditLogControl -> auditLogControl.setAuditLogFromDateTime(registrationDTO.getAuditLogStartTime()))
				.with(auditLogControl -> auditLogControl.setAuditLogToDateTime(registrationDTO.getAuditLogEndTime()))
				.with(auditLogControl -> auditLogControl.setRegistrationId(registrationDTO.getRegistrationId()))
				.with(auditLogControl -> auditLogControl.setAuditLogSyncDateTime(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime())))
				.with(auditLogControl -> auditLogControl.setCrDtime(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime())))
				.with(auditLogControl -> auditLogControl.setCrBy(SessionContext.userContext().getUserId()))
				.get());
	}*/
	
	private void setOtherDetails(RegistrationDTO registrationDTO) {
		packetCreator.setMetaInfo(PacketManagerConstants.META_CLIENT_VERSION, 
				softwareUpdateHandler.getCurrentVersion());
		packetCreator.setMetaInfo(PacketManagerConstants.META_REGISTRATION_TYPE, 
				registrationDTO.getRegistrationMetaDataDTO().getRegistrationCategory());
		packetCreator.setMetaInfo(PacketManagerConstants.META_PRE_REGISTRATION_ID, 
				registrationDTO.getPreRegistrationId());
		packetCreator.setMetaInfo(PacketManagerConstants.META_MACHINE_ID, 
				(String) ApplicationContext.map().get(RegistrationConstants.USER_STATION_ID));
		packetCreator.setMetaInfo(PacketManagerConstants.META_CENTER_ID, 
				(String) ApplicationContext.map().get(RegistrationConstants.USER_CENTER_ID));
		packetCreator.setMetaInfo(PacketManagerConstants.META_DONGLE_ID, 
				(String) ApplicationContext.map().get(RegistrationConstants.DONGLE_SERIAL_NUMBER));
		packetCreator.setMetaInfo(PacketManagerConstants.META_KEYINDEX,
				ApplicationContext.getStringValueFromApplicationMap(RegistrationConstants.KEY_INDEX));
		packetCreator.setMetaInfo(PacketManagerConstants.META_APPLICANT_CONSENT, 
				registrationDTO.getRegistrationMetaDataDTO().getConsentOfApplicant());
		
		RegistrationCenterDetailDTO registrationCenter = SessionContext.userContext().getRegistrationCenterDetailDTO();
		if (RegistrationConstants.ENABLE.equalsIgnoreCase(environment.getProperty(RegistrationConstants.GPS_DEVICE_DISABLE_FLAG))) {
			packetCreator.setMetaInfo(PacketManagerConstants.META_LATITUDE, registrationCenter.getRegistrationCenterLatitude());
			packetCreator.setMetaInfo(PacketManagerConstants.META_LONGITUDE, registrationCenter.getRegistrationCenterLongitude());
		}
		
		addRegisteredDevices();
		
		packetCreator.setOperationsInfo(PacketManagerConstants.META_OFFICER_ID, registrationDTO.getOsiDataDTO().getOperatorID());
		packetCreator.setOperationsInfo(PacketManagerConstants.META_OFFICER_BIOMETRIC_FILE, null);
		packetCreator.setOperationsInfo(PacketManagerConstants.META_SUPERVISOR_ID, registrationDTO.getOsiDataDTO().getSupervisorID());
		packetCreator.setOperationsInfo(PacketManagerConstants.META_SUPERVISOR_BIOMETRIC_FILE, null);
		packetCreator.setOperationsInfo(PacketManagerConstants.META_SUPERVISOR_PWD, 
				String.valueOf(registrationDTO.getOsiDataDTO().isSuperviorAuthenticatedByPassword()));
		packetCreator.setOperationsInfo(PacketManagerConstants.META_OFFICER_PWD, 
				String.valueOf(registrationDTO.getOsiDataDTO().isOperatorAuthenticatedByPassword()));
		packetCreator.setOperationsInfo(PacketManagerConstants.META_SUPERVISOR_PIN, null);
		packetCreator.setOperationsInfo(PacketManagerConstants.META_OFFICER_PIN, null);
		packetCreator.setOperationsInfo(PacketManagerConstants.META_SUPERVISOR_OTP, 
				String.valueOf(registrationDTO.getOsiDataDTO().isSuperviorAuthenticatedByPIN()));
		packetCreator.setOperationsInfo(PacketManagerConstants.META_OFFICER_OTP, 
				String.valueOf(registrationDTO.getOsiDataDTO().isOperatorAuthenticatedByPIN()));
		
		Map<String, String> checkSumMap = CheckSumUtil.getCheckSumMap();
		checkSumMap.forEach((key, value) -> packetCreator.setChecksum(key, value));
	}
	
	private String getPrintingNameFieldName(SchemaDto schema) {
		Optional<UiSchemaDTO> result = schema.getSchema().stream().filter(field -> 
			field.getSubType() != null && field.getSubType().equals("name")).findFirst();
		
		if(result.isPresent() && result.get() != null)
			return result.get().getId();
		
		return null;
	}
	
	private void validateIdObject(String schemaJson, Object idObject, String category) throws RegBaseCheckedException {
		LOGGER.debug(LOG_PKT_HANLDER, APPLICATION_NAME, APPLICATION_ID, "validateIdObject invoked >>>>> " + category);
		//LOGGER.debug(LOG_PKT_HANLDER, APPLICATION_NAME, APPLICATION_ID, "idObject >>>>> " + idObject);
		try {
			switch (category) {
			case RegistrationConstants.PACKET_TYPE_UPDATE:
				idObjectValidator.validateIdObject(schemaJson, idObject, Arrays.asList("UIN", "IDSchemaVersion"));
				break;
			case RegistrationConstants.PACKET_TYPE_LOST:
				idObjectValidator.validateIdObject(schemaJson, idObject, Arrays.asList("IDSchemaVersion"));
				break;
			case RegistrationConstants.PACKET_TYPE_NEW:
				idObjectValidator.validateIdObject(schemaJson, idObject);
				break;
			}
			regIdObjectMasterDataValidator.validateIdObject(idObject);
		} catch (IdObjectValidationFailedException | IdObjectIOException | InvalidIdSchemaException e) {
			LOGGER.error(LOG_PKT_HANLDER, APPLICATION_NAME, APPLICATION_ID, ExceptionUtils.getStackTrace(e));
			throw new RegBaseCheckedException(e.getErrorCode(), e.getErrorText());
		}		
	}
}
