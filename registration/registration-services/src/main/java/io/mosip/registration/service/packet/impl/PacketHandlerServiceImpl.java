package io.mosip.registration.service.packet.impl;

import static io.mosip.registration.constants.LoggerConstants.LOG_PKT_HANLDER;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;
import static io.mosip.registration.exception.RegistrationExceptionConstants.REG_PACKET_CREATION_ERROR_CODE;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
import io.mosip.registration.dao.AuditLogControlDAO;
import io.mosip.registration.dao.PolicySyncDAO;
import io.mosip.registration.dao.RegistrationDAO;
import io.mosip.registration.dto.ErrorResponseDTO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.SuccessResponseDTO;
import io.mosip.registration.dto.demographic.ValuesDTO;
import io.mosip.registration.entity.AuditLogControl;
import io.mosip.registration.entity.KeyStore;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.packetmanager.spi.PacketCreator;
import io.mosip.registration.packetmananger.dto.AuditDto;
import io.mosip.registration.packetmananger.dto.DocumentDto;
import io.mosip.registration.packetmananger.exception.PacketCreatorException;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.IdentitySchemaService;
import io.mosip.registration.service.external.StorageService;
import io.mosip.registration.service.packet.PacketHandlerService;
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
			String schema = identitySchemaService.getIDSchema(registrationDTO.getIdSchemaVersion());
			
			//TODO validate idObject with IDSchema
			
			packetCreator.initialize();		
			setDemographics(registrationDTO.getDemographics());
			setDocuments(registrationDTO.getDocuments());
			
			packetCreator.setAcknowledgement(registrationDTO.getAcknowledgeReceiptName(), registrationDTO.getAcknowledgeReceipt());			
			packetCreator.setAudits(registrationDTO.getAuditDTOs());
			byte[] packetZip = packetCreator.createPacket(registrationDTO.getRegistrationId(), registrationDTO.getIdSchemaVersion(),
					schema, categoryPacketMapping, getPublicKeyToEncrypt(), null);				
			
			String filePath = savePacketToDisk(registrationDTO.getRegistrationId(), packetZip);
			registrationDAO.save(filePath, registrationDTO);			
			createAuditLog(registrationDTO);			
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
}
