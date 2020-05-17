package io.mosip.registration.processor.stages.utils;

import java.io.IOException;

import org.json.simple.JSONObject;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.packetmanager.exception.ApiNotAccessibleException;
import io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException;
import io.mosip.kernel.packetmanager.spi.PacketReaderService;
import io.mosip.kernel.packetmanager.util.IdSchemaUtils;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.util.IdentityIteratorUtil;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.exception.IdentityNotFoundException;
import io.mosip.registration.processor.packet.storage.utils.Utilities;

/**
 * The Class ApplicantDocumentValidation.
 * 
 * @author Nagalakshmi
 */
public class ApplicantDocumentValidation {

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(ApplicantDocumentValidation.class);

	/** The identity iterator. */
	IdentityIteratorUtil identityIterator = new IdentityIteratorUtil();

	/** The utility. */
	private Utilities utility;
	
	private PacketReaderService packetReaderService;
	
	private IdSchemaUtils idSchemaUtils;
	
	private static final String VALUE = "value";

	/**
	 * Instantiates a new applicant document validation.
	 *
	 * @param utilities
	 *            the utilities
	 * @param env
	 *            the env
	 * @param applicantTypeDocument
	 *            the applicant type document
	 */
	public ApplicantDocumentValidation(Utilities utilities, 
			IdSchemaUtils idSchemaUtils,PacketReaderService packetReaderService) {
		
		this.utility = utilities;
		this.idSchemaUtils = idSchemaUtils;
		this.packetReaderService=packetReaderService;
	}

	/**
	 * Validate document.
	 *
	 * @param registrationId the registration id
	 * @param jsonString     the json string
	 * @return true, if successful
	 * @throws ApisResourceAccessException           the apis resource access
	 *                                               exception
	 * @throws IOException                           Signals that an I/O exception
	 *                                               has occurred.
	 * @throws                                       io.mosip.kernel.core.exception.IOException
	 * @throws PacketDecryptionFailureException
	 * @throws RegistrationProcessorCheckedException
	 * @throws                                       io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException
	 * @throws io.mosip.kernel.core.exception.IOException 
	 * @throws SecurityException                     the security exception
	 * @throws IllegalArgumentException              the illegal argument exception
	 */
	public boolean validateDocument(String registrationId)
			throws   PacketDecryptionFailureException, IdentityNotFoundException,
			 ApiNotAccessibleException, IOException, io.mosip.kernel.core.exception.IOException
			 {
		
		JSONObject regProcessorIdentityJson = utility.getRegistrationProcessorMappingJson();
		String proofOfAddressLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.POA), VALUE);
		String proofOfDateOfBirthLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.POB), VALUE);
		String proofOfIdentityLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.POI), VALUE);
		String proofOfRelationshipLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.POR), VALUE);
		String proofOfExceptionsLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.POE), VALUE);
		String applicantBiometricLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.INDIVIDUAL_BIOMETRICS), VALUE);
		String introducerBiometricLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.PARENT_OR_GUARDIAN_BIO), VALUE);
		
		JSONObject proofOfAddress = JsonUtil.getJSONObject(utility.getDemographicIdentityJSONObject(registrationId,proofOfAddressLabel), proofOfAddressLabel);
		JSONObject proofOfDateOfBirth = JsonUtil.getJSONObject(utility.getDemographicIdentityJSONObject(registrationId,proofOfDateOfBirthLabel), proofOfDateOfBirthLabel);
		JSONObject proofOfIdentity = JsonUtil.getJSONObject(utility.getDemographicIdentityJSONObject(registrationId,proofOfIdentityLabel), proofOfIdentityLabel);
		JSONObject proofOfRelationship = JsonUtil.getJSONObject(utility.getDemographicIdentityJSONObject(registrationId,proofOfRelationshipLabel), proofOfRelationshipLabel);
		JSONObject applicantBiometric = JsonUtil.getJSONObject(utility.getDemographicIdentityJSONObject(registrationId,applicantBiometricLabel), applicantBiometricLabel);
		JSONObject proofOfExceptions = JsonUtil.getJSONObject(utility.getDemographicIdentityJSONObject(registrationId,proofOfExceptionsLabel), proofOfExceptionsLabel);
		JSONObject introducerBiometric = JsonUtil.getJSONObject(utility.getDemographicIdentityJSONObject(registrationId,introducerBiometricLabel), introducerBiometricLabel);
		
		if (proofOfAddress != null && proofOfAddress.get("value")!=null) {
			String source=idSchemaUtils.getSource(proofOfAddressLabel);
			if(source!=null) {
			if(! packetReaderService.checkFileExistence(registrationId,proofOfAddress.get("value").toString(),source)) {
				return false;
			}
			}
		}
		if (proofOfDateOfBirth != null && proofOfDateOfBirth.get("value")!=null) {
			String source=idSchemaUtils.getSource(proofOfDateOfBirthLabel);
			if(source!=null) {
			if(! packetReaderService.checkFileExistence(registrationId,proofOfDateOfBirth.get("value").toString(),source)) {
				return false;
			}
			}
		}
		if (proofOfIdentity != null && proofOfIdentity.get("value")!=null) {
			String source=idSchemaUtils.getSource(proofOfIdentityLabel);
			if(source!=null) {
			if(! packetReaderService.checkFileExistence(registrationId,proofOfIdentity.get("value").toString(),source)) {
				return false;
			}
			}
		}
		if (proofOfRelationship != null && proofOfRelationship.get("value")!=null) {
			String source=idSchemaUtils.getSource(proofOfRelationshipLabel);
			if(source!=null) {
			if(! packetReaderService.checkFileExistence(registrationId,proofOfRelationship.get("value").toString(),source)) {
				return false;
			}
			}
		}
		if (applicantBiometric != null && applicantBiometric.get("value")!=null) {
			String source=idSchemaUtils.getSource(applicantBiometricLabel);
			if(source!=null) {
			if(! packetReaderService.checkFileExistence(registrationId,applicantBiometric.get("value").toString(),source)) {
				return false;
			}
			}
		}
		if (introducerBiometric != null && introducerBiometric.get("value")!=null) {
			String source=idSchemaUtils.getSource(introducerBiometricLabel);
			if(source!=null) {
			if(! packetReaderService.checkFileExistence(registrationId,introducerBiometric.get("value").toString(),source)) {
				return false;
			}
			}
		}
		if (proofOfExceptions != null && proofOfExceptions.get("value")!=null) {
			String source=idSchemaUtils.getSource(proofOfExceptionsLabel);
			if(source!=null) {
				if(! packetReaderService.checkFileExistence(registrationId,proofOfExceptions.get("value").toString(),source)) {
				return false;
			}
			}
		}
		

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "ApplicantDocumentValidation::validateApplicantData::exit");
		return true;
	}

	
}
