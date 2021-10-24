package io.mosip.registration.processor.stages.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.packet.storage.utils.PacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.exception.IdentityNotFoundException;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import org.springframework.stereotype.Component;


/**
 * The Class ApplicantDocumentValidation.
 * 
 * @author Nagalakshmi
 */
@Component
public class ApplicantDocumentValidation {

	private static Logger regProcLogger = RegProcessorLogger.getLogger(ApplicantDocumentValidation.class);

	@Autowired
    private PriorityBasedPacketManagerService packetManagerService;

	@Autowired
	private Utilities utility;
	
	private static final String VALUE = "value";

	public boolean validateDocument(String registrationId, String process) throws IdentityNotFoundException, IOException, ApisResourceAccessException, PacketManagerException, JsonProcessingException {
		
		JSONObject docMappingJson = utility.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT);
		JSONObject identityMappingJson = utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);
		String proofOfAddressLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(docMappingJson, MappingJsonConstants.POA), VALUE);
		String proofOfDateOfBirthLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(docMappingJson, MappingJsonConstants.POB), VALUE);
		String proofOfIdentityLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(docMappingJson, MappingJsonConstants.POI), VALUE);
		String proofOfRelationshipLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(docMappingJson, MappingJsonConstants.POR), VALUE);
		String proofOfExceptionsLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(docMappingJson, MappingJsonConstants.POE), VALUE);
		String applicantBiometricLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(identityMappingJson, MappingJsonConstants.INDIVIDUAL_BIOMETRICS), VALUE);
		String introducerBiometricLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(identityMappingJson, MappingJsonConstants.PARENT_OR_GUARDIAN_BIO), VALUE);

		List<String> fields = new ArrayList<>();
		fields.add(proofOfAddressLabel);
		 fields.add(proofOfDateOfBirthLabel);
		 fields.add(proofOfIdentityLabel);
		 fields.add(proofOfRelationshipLabel);
		 fields.add(proofOfExceptionsLabel);
		 fields.add(applicantBiometricLabel);
		 fields.add(introducerBiometricLabel);

		Map<String, String> docFields = packetManagerService.getFields(registrationId, fields, process, ProviderStageName.PACKET_VALIDATOR);

        if (docFields.get(proofOfAddressLabel) != null) {
			if (packetManagerService.getDocument(registrationId, proofOfAddressLabel, process, ProviderStageName.PACKET_VALIDATOR) == null)
					return false;
		}
		if (docFields.get(proofOfDateOfBirthLabel) != null) {
			if (packetManagerService.getDocument(registrationId, proofOfDateOfBirthLabel, process, ProviderStageName.PACKET_VALIDATOR) == null)
				return false;
		}
		if (docFields.get(proofOfIdentityLabel) != null) {
			if (packetManagerService.getDocument(registrationId, proofOfIdentityLabel, process, ProviderStageName.PACKET_VALIDATOR) == null)
				return false;
		}
		if (docFields.get(proofOfRelationshipLabel) != null) {
			if (packetManagerService.getDocument(registrationId, proofOfRelationshipLabel, process, ProviderStageName.PACKET_VALIDATOR) == null)
				return false;
		}
		if (docFields.get(applicantBiometricLabel) != null) {
			BiometricRecord biometricRecord = packetManagerService.getBiometricsByMappingJsonKey(registrationId, MappingJsonConstants.INDIVIDUAL_BIOMETRICS, process, ProviderStageName.PACKET_VALIDATOR);
			if (biometricRecord == null || biometricRecord.getSegments() == null || biometricRecord.getSegments().size() == 0)
				return false;
		}
		if (docFields.get(introducerBiometricLabel) != null) {
			BiometricRecord biometricRecord = packetManagerService.getBiometricsByMappingJsonKey(registrationId, MappingJsonConstants.PARENT_OR_GUARDIAN_BIO, process, ProviderStageName.PACKET_VALIDATOR);
			if (biometricRecord == null || biometricRecord.getSegments() == null || biometricRecord.getSegments().size() == 0)
				return false;
		}
		if (docFields.get(proofOfExceptionsLabel) != null) {
			if (packetManagerService.getDocument(registrationId, proofOfExceptionsLabel, process, ProviderStageName.PACKET_VALIDATOR) == null)
				return false;
		}

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "ApplicantDocumentValidation::validateApplicantData::exit");
		return true;
	}

	
}
