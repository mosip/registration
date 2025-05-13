package io.mosip.registration.processor.stages.validator.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.mosip.registration.processor.core.exception.PacketManagerNonRecoverableException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.cbeffutil.exception.CbeffException;
import io.mosip.kernel.core.exception.BiometricSignatureValidationException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.constant.JsonConstant;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.constant.RegistrationType;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.FieldValue;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.PacketValidationDto;
import io.mosip.registration.processor.core.spi.packet.validator.PacketValidator;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.packet.storage.dto.ValidatePacketResponse;
import io.mosip.registration.processor.packet.storage.exception.IdRepoAppException;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.stages.utils.ApplicantDocumentValidation;
import io.mosip.registration.processor.stages.utils.BiometricsXSDValidator;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;

@Component
@RefreshScope
public class PacketValidatorImpl implements PacketValidator {

	private static Logger regProcLogger = RegProcessorLogger.getLogger(PacketValidatorImpl.class);
	private static final String VALIDATIONFALSE = "false";
	public static final String APPROVED = "APPROVED";
	public static final String REJECTED = "REJECTED";
	private static final String VALIDATEAPPLICANTDOCUMENT = "mosip.regproc.packet.validator.validate-applicant-document";
    private static final String VALIDATEAPPLICANTDOCUMENTPROCESS = "mosip.regproc.packet.validator.validate-applicant-document.processes";

	@Autowired
	private PriorityBasedPacketManagerService packetManagerService;

	@Autowired
	private Utilities utility;

	@Autowired
	private Environment env;
	
	@Autowired
	ObjectMapper mapper;

	@Autowired
	private BiometricsXSDValidator biometricsXSDValidator;
	
	@Autowired
	private BiometricsSignatureValidator biometricsSignatureValidator;

	@Autowired
	private ApplicantDocumentValidation applicantDocumentValidation;

	@Override
	public boolean validate(String id, String process, PacketValidationDto packetValidationDto)
			throws ApisResourceAccessException, RegistrationProcessorCheckedException, IOException,
			JsonProcessingException, PacketManagerException {
		String uin = null;
		try {
			ValidatePacketResponse response = packetManagerService.validate(id, process,
					ProviderStageName.PACKET_VALIDATOR);
			if (!response.isValid()) {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), id,
						"ERROR =======>" + StatusUtil.PACKET_MANAGER_VALIDATION_FAILURE.getMessage());
				packetValidationDto
						.setPacketValidatonStatusCode(StatusUtil.PACKET_MANAGER_VALIDATION_FAILURE.getCode());
				packetValidationDto
						.setPacketValidaionFailureMessage(StatusUtil.PACKET_MANAGER_VALIDATION_FAILURE.getMessage());
				return false;
			}
			
			//Check consent
			if(!checkConsentForPacket(id,process,ProviderStageName.PACKET_VALIDATOR))
			{
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), id,
						"ERROR =======>" + StatusUtil.PACKET_CONSENT_VALIDATION.getMessage());
				packetValidationDto
						.setPacketValidatonStatusCode(StatusUtil.PACKET_CONSENT_VALIDATION.getCode());
				packetValidationDto
						.setPacketValidaionFailureMessage(StatusUtil.PACKET_CONSENT_VALIDATION.getMessage());
				return false;
			}
			
			

			if (process.equalsIgnoreCase(RegistrationType.UPDATE.toString())
					|| process.equalsIgnoreCase(RegistrationType.RES_UPDATE.toString())) {
				uin = utility.getUIn(id, process, ProviderStageName.PACKET_VALIDATOR);
				if (uin == null) {
					regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), id,
							"ERROR =======>" + PlatformErrorMessages.RPR_PVM_INVALID_UIN.getMessage());
					throw new IdRepoAppException(PlatformErrorMessages.RPR_PVM_INVALID_UIN.getMessage());
				}
				JSONObject jsonObject = utility.retrieveIdrepoJson(uin);
				if (jsonObject == null) {
					regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), id,
							"ERROR =======>" + PlatformErrorMessages.RPR_PIS_IDENTITY_NOT_FOUND.getMessage());
					throw new IdRepoAppException(PlatformErrorMessages.RPR_PIS_IDENTITY_NOT_FOUND.getMessage());
				}
				String status = utility.retrieveIdrepoJsonStatus(uin);
				if (process.equalsIgnoreCase(RegistrationType.UPDATE.toString())
						&& status.equalsIgnoreCase(RegistrationType.DEACTIVATED.toString())) {
					regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), id,
							"ERROR =======>" + PlatformErrorMessages.RPR_PVM_UPDATE_DEACTIVATED.getMessage());
					throw new RegistrationProcessorCheckedException(
							PlatformErrorMessages.RPR_PVM_UPDATE_DEACTIVATED.getCode(), "UIN is Deactivated");
				}
			}

			// document validation
			if (!applicantDocumentValidation(id, process, packetValidationDto)) {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), id,
						"ERROR =======>" + StatusUtil.APPLICANT_DOCUMENT_VALIDATION_FAILED.getMessage());
				return false;
			}

			// check if uin is in idrepisitory
			if (RegistrationType.UPDATE.name().equalsIgnoreCase(process)
					|| RegistrationType.RES_UPDATE.name().equalsIgnoreCase(process)) {

				if (!utility.uinPresentInIdRepo(String.valueOf(uin))) {
					regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), id,
							"ERROR =======>" + StatusUtil.UIN_NOT_FOUND_IDREPO.getMessage());
					packetValidationDto.setPacketValidaionFailureMessage(StatusUtil.UIN_NOT_FOUND_IDREPO.getMessage());
					packetValidationDto.setPacketValidatonStatusCode(StatusUtil.UIN_NOT_FOUND_IDREPO.getCode());
					return false;
				}
			}

			if (!biometricsXSDValidation(id, process, packetValidationDto)) {
				return false;
			}
		}catch(PacketManagerNonRecoverableException e){
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					id, RegistrationStatusCode.FAILED.toString() + e.getMessage() + ExceptionUtils.getStackTrace(e));
			throw e;
		}catch (PacketManagerException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					id, RegistrationStatusCode.FAILED.toString() + e.getMessage() + ExceptionUtils.getStackTrace(e));
			throw e;
		} catch (JSONException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					id, RegistrationStatusCode.FAILED.toString() + e.getMessage() + ExceptionUtils.getStackTrace(e));
			packetValidationDto.setPacketValidaionFailureMessage(
					StatusUtil.JSON_PARSING_EXCEPTION.getMessage() + e.getMessage());
			packetValidationDto.setPacketValidatonStatusCode(StatusUtil.JSON_PARSING_EXCEPTION.getCode());
		}

		packetValidationDto.setValid(true);
		return packetValidationDto.isValid();
	}

	private boolean biometricsXSDValidation(String id, String process, PacketValidationDto packetValidationDto)
			throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException,
			RegistrationProcessorCheckedException, JSONException {
		List<String> fields = Arrays.asList(MappingJsonConstants.INDIVIDUAL_BIOMETRICS,
				MappingJsonConstants.AUTHENTICATION_BIOMETRICS, MappingJsonConstants.INTRODUCER_BIO,
				MappingJsonConstants.OFFICERBIOMETRICFILENAME, MappingJsonConstants.SUPERVISORBIOMETRICFILENAME);
		
		Map<String, String> metaInfoMap = packetManagerService.getMetaInfo(id, process,
				ProviderStageName.PACKET_VALIDATOR);
		
		for (String field : fields) {
			BiometricRecord biometricRecord = null;
			if (field.equals(MappingJsonConstants.OFFICERBIOMETRICFILENAME)
					|| field.equals(MappingJsonConstants.SUPERVISORBIOMETRICFILENAME)) {
				String value = getOperationsDataFromMetaInfo(id, process, field, metaInfoMap);
				if (value != null && !value.isEmpty()) {
					biometricRecord = packetManagerService.getBiometrics(id, field, process,
							ProviderStageName.PACKET_VALIDATOR);
				}
			} else {
				String value = packetManagerService.getField(id, field, process, ProviderStageName.PACKET_VALIDATOR);
				if (value != null && !value.isEmpty()) {
					biometricRecord = packetManagerService.getBiometricsByMappingJsonKey(id, field, process,
							ProviderStageName.PACKET_VALIDATOR);
				}
			}

			if (biometricRecord != null) {
				try {
					biometricsXSDValidator.validateXSD(biometricRecord);
					biometricsSignatureValidator.validateSignature(id, process, biometricRecord, metaInfoMap);
				} catch (Exception e) {
					if (e instanceof CbeffException) {
						regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
								LoggerFileConstant.REGISTRATIONID.toString(), id,
								"ERROR =======> " + StatusUtil.XSD_VALIDATION_EXCEPTION.getMessage());
						packetValidationDto.setPacketValidaionFailureMessage(
								StatusUtil.XSD_VALIDATION_EXCEPTION.getMessage());
						packetValidationDto.setPacketValidatonStatusCode(StatusUtil.XSD_VALIDATION_EXCEPTION.getCode());
						return false;
					} else if (e instanceof BiometricSignatureValidationException) {
						regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
								LoggerFileConstant.REGISTRATIONID.toString(), id,
								"ERROR =======> " + StatusUtil.BIOMETRICS_SIGNATURE_VALIDATION_FAILURE.getMessage());
						packetValidationDto.setPacketValidaionFailureMessage(
								StatusUtil.BIOMETRICS_SIGNATURE_VALIDATION_FAILURE.getMessage() + "--> "
										+ e.getMessage());
						packetValidationDto.setPacketValidatonStatusCode(
								StatusUtil.BIOMETRICS_SIGNATURE_VALIDATION_FAILURE.getCode());
						return false;
					} else {
						throw new RegistrationProcessorCheckedException(
								PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getCode(),
								PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage(), e);
					}
				}
			}
		}
		return true;
	}
	
	private String getOperationsDataFromMetaInfo(String id, String process, String fileName,
			Map<String, String> metaInfoMap)
			throws ApisResourceAccessException, PacketManagerException, IOException, JSONException {
		String metadata = metaInfoMap.get(JsonConstant.OPERATIONSDATA);
		String value = null;
		if (StringUtils.isNotEmpty(metadata)) {
			JSONArray jsonArray = new JSONArray(metadata);

			for (int i = 0; i < jsonArray.length(); i++) {
				if (!jsonArray.isNull(i)) {
					org.json.JSONObject jsonObject = (org.json.JSONObject) jsonArray.get(i);
					FieldValue fieldValue = mapper.readValue(jsonObject.toString(), FieldValue.class);
					if (fieldValue.getLabel().equalsIgnoreCase(fileName)) {
						value = fieldValue.getValue();
						break;
					}
				}
			}
		}
		return value;
	}


	private boolean applicantDocumentValidation(String registrationId, String process,
			PacketValidationDto packetValidationDto)
			throws ApisResourceAccessException, JsonProcessingException, PacketManagerException, IOException {
		String validateApplicant=env.getProperty(VALIDATEAPPLICANTDOCUMENT);
		if (validateApplicant!=null && validateApplicant.trim().equalsIgnoreCase(VALIDATIONFALSE))
			return true;
		else {
			String validateApplicantDocument=env.getProperty(VALIDATEAPPLICANTDOCUMENTPROCESS);
			if(validateApplicantDocument!=null && validateApplicantDocument.contains(process)) {
				boolean result = applicantDocumentValidation.validateDocument(registrationId, process);
				if (!result) {
					packetValidationDto.setPacketValidaionFailureMessage(StatusUtil.APPLICANT_DOCUMENT_VALIDATION_FAILED.getMessage());
					packetValidationDto.setPacketValidatonStatusCode(StatusUtil.APPLICANT_DOCUMENT_VALIDATION_FAILED.getCode());
				}
				return result;
			}
			return true;
		}
	}
	
	private boolean checkConsentForPacket(String id, String process, ProviderStageName stageName)
			throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {

		String val = packetManagerService.getField(id, MappingJsonConstants.CONSENT, process, stageName);
		if (null!=val && StringUtils.isNotEmpty(val)) {
			if (val.equalsIgnoreCase("N"))
				return false;
		}
		return true;

	}
	
	

}