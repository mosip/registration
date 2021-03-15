package io.mosip.registration.processor.stages.validator.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.constant.RegistrationType;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.PacketValidationDto;
import io.mosip.registration.processor.core.spi.packet.validator.PacketValidator;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService;
import io.mosip.registration.processor.packet.storage.dto.ValidatePacketResponse;
import io.mosip.registration.processor.packet.storage.exception.IdRepoAppException;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.stages.utils.ApplicantDocumentValidation;
import io.mosip.registration.processor.stages.utils.MandatoryValidation;
import io.mosip.registration.processor.stages.utils.MasterDataValidation;
import io.mosip.registration.processor.stages.utils.XSDValidation;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;

@Component
@RefreshScope
public class PacketValidatorImpl implements PacketValidator {

    private static Logger regProcLogger = RegProcessorLogger.getLogger(PacketValidatorImpl.class);
    private static final String VALIDATIONFALSE = "false";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    private static final String VALUE = "value";
    private static final String VALIDATEMASTERDATA = "registration.processor.validateMasterData";
    private static final String VALIDATEMANDATORY = "registration-processor.validatemandotary";
    private static final String VALIDATEAPPLICANTDOCUMENT = "registration.processor.validateApplicantDocument";

    @Autowired
    private PriorityBasedPacketManagerService packetManagerService;

    @Autowired
    private Utilities utility;

    @Autowired
    private IdRepoService idRepoService;

    @Autowired
    private Environment env;

    @Autowired
    private MandatoryValidation mandatoryValidation;
    
    @Autowired
    private XSDValidation xsdValidation;

    @Autowired
    private MasterDataValidation masterDataValidation;

    @Autowired
    private ApplicantDocumentValidation applicantDocumentValidation;
    

    @Override
    public boolean validate(String id, String process, PacketValidationDto packetValidationDto) throws ApisResourceAccessException, RegistrationProcessorCheckedException, IOException, JsonProcessingException, PacketManagerException {
        String uin = null;
        try {
            ValidatePacketResponse response = packetManagerService.validate(id, process, ProviderStageName.PACKET_VALIDATOR);
            if (!response.isValid()) {
                regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                        id, "ERROR =======>" + StatusUtil.PACKET_MANAGER_VALIDATION_FAILURE.getMessage());
                packetValidationDto.setPacketValidatonStatusCode(StatusUtil.PACKET_MANAGER_VALIDATION_FAILURE.getCode());
                packetValidationDto.setPacketValidaionFailureMessage(StatusUtil.PACKET_MANAGER_VALIDATION_FAILURE.getMessage());
                return false;
            }

            if (RegistrationType.NEW.name().equalsIgnoreCase(process)
                    && !individualBiometricsValidation(id, process)) {
                regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                        id, "ERROR =======>" + StatusUtil.BIOMETRICS_VALIDATION_FAILURE.getMessage());
                packetValidationDto.setPacketValidatonStatusCode(StatusUtil.BIOMETRICS_VALIDATION_FAILURE.getCode());
                packetValidationDto.setPacketValidaionFailureMessage(StatusUtil.BIOMETRICS_VALIDATION_FAILURE.getMessage());
                return false;
            }

            if (process.equalsIgnoreCase(RegistrationType.UPDATE.toString())
                    || process.equalsIgnoreCase(RegistrationType.RES_UPDATE.toString())) {
                uin = utility.getUIn(id, process, ProviderStageName.PACKET_VALIDATOR);
                if (uin == null) {
                    regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                            id, "ERROR =======>" + PlatformErrorMessages.RPR_PVM_INVALID_UIN.getMessage());
                    throw new IdRepoAppException(PlatformErrorMessages.RPR_PVM_INVALID_UIN.getMessage());
                }
                JSONObject jsonObject = utility.retrieveIdrepoJson(uin);
                if (jsonObject == null) {
                    regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                            id, "ERROR =======>" + PlatformErrorMessages.RPR_PIS_IDENTITY_NOT_FOUND.getMessage());
                    throw new IdRepoAppException(PlatformErrorMessages.RPR_PIS_IDENTITY_NOT_FOUND.getMessage());
                }
                String status = utility.retrieveIdrepoJsonStatus(uin);
                if (process.equalsIgnoreCase(RegistrationType.UPDATE.toString())
                        && status.equalsIgnoreCase(RegistrationType.DEACTIVATED.toString())) {
                    regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                            id, "ERROR =======>" + PlatformErrorMessages.RPR_PVM_UPDATE_DEACTIVATED.getMessage());
                    throw new RegistrationProcessorCheckedException(
                            PlatformErrorMessages.RPR_PVM_UPDATE_DEACTIVATED.getCode(), "UIN is Deactivated");
                }
            }

            if (!masterDataValidation(id, process, packetValidationDto)) {
                regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                        id, "ERROR =======>" + StatusUtil.MASTER_DATA_VALIDATION_FAILED.getMessage());
                packetValidationDto.setPacketValidaionFailureMessage(StatusUtil.MASTER_DATA_VALIDATION_FAILED.getMessage());
                packetValidationDto.setPacketValidatonStatusCode(StatusUtil.MASTER_DATA_VALIDATION_FAILED.getCode());
                return false;
            }

            // document validation
            if (!applicantDocumentValidation(id, process, packetValidationDto)) {
                regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                        id, "ERROR =======>" + StatusUtil.APPLICANT_DOCUMENT_VALIDATION_FAILED.getMessage());
                return false;
            }

            // check if uin is in idrepisitory
            if (RegistrationType.UPDATE.name().equalsIgnoreCase(process)
                    || RegistrationType.RES_UPDATE.name().equalsIgnoreCase(process)) {

                if (!uinPresentInIdRepo(String.valueOf(uin))) {
                    regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                            id, "ERROR =======>" + StatusUtil.UIN_NOT_FOUND_IDREPO.getMessage());
                    packetValidationDto.setPacketValidaionFailureMessage(StatusUtil.UIN_NOT_FOUND_IDREPO.getMessage());
                    packetValidationDto.setPacketValidatonStatusCode(StatusUtil.UIN_NOT_FOUND_IDREPO.getCode());
                    return false;
                }
            }

            if (RegistrationType.NEW.name().equalsIgnoreCase(process)
                    && !mandatoryValidation(id, process, packetValidationDto)) {
                regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                        id, "ERROR =======> " + StatusUtil.MANDATORY_VALIDATION_FAILED.getMessage());
                packetValidationDto.setPacketValidaionFailureMessage(StatusUtil.MANDATORY_VALIDATION_FAILED.getMessage());
                packetValidationDto.setPacketValidatonStatusCode(StatusUtil.MANDATORY_VALIDATION_FAILED.getCode());
                return false;
            }
            
            if ( !xsdValidation(id, process, packetValidationDto)) {
                regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                        id, "ERROR =======> " + StatusUtil.XSD_VALIDATION_EXCEPTION.getMessage());
                packetValidationDto.setPacketValidaionFailureMessage(StatusUtil.XSD_VALIDATION_EXCEPTION.getMessage());
                packetValidationDto.setPacketValidatonStatusCode(StatusUtil.XSD_VALIDATION_EXCEPTION.getCode());
                return false;
            }
        } catch (PacketManagerException e) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    id,
                    RegistrationStatusCode.FAILED.toString() + e.getMessage() + ExceptionUtils.getStackTrace(e));
            throw e;
        }

        packetValidationDto.setValid(true);
        return packetValidationDto.isValid();
    }

    private boolean xsdValidation(String id, String process, PacketValidationDto packetValidationDto) throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException, RegistrationProcessorCheckedException {
    	List<String> fields=Arrays.asList( MappingJsonConstants.INDIVIDUAL_BIOMETRICS, MappingJsonConstants.AUTHENTICATION_BIOMETRICS
    			, MappingJsonConstants.PARENT_OR_GUARDIAN_BIO, MappingJsonConstants.OFFICERBIOMETRICFILENAME, MappingJsonConstants.SUPERVISORBIOMETRICFILENAME);
    	for(String field:fields) {
    		String value=packetManagerService.getField(id, field, process, ProviderStageName.PACKET_VALIDATOR);
    		if(value!=null &&!value.isEmpty()) {
    			try {
    	            BiometricRecord biometricRecord = packetManagerService.getBiometricsByMappingJsonKey(
    	                    id, field, process, ProviderStageName.PACKET_VALIDATOR);
    	            if(!xsdValidation.validateXSD(biometricRecord)) {
    	            	return false;
    	            }
    	             } catch (Exception e) {
    	            throw new RegistrationProcessorCheckedException(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getCode(),
    	                    PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage(), e);
    	        }
    		}
    	}
		
		return true;
	}

	private boolean individualBiometricsValidation(String id, String process) throws RegistrationProcessorCheckedException {
        try {
            BiometricRecord biometricRecord = packetManagerService.getBiometricsByMappingJsonKey(
                    id, MappingJsonConstants.INDIVIDUAL_BIOMETRICS, process, ProviderStageName.PACKET_VALIDATOR);
            return (biometricRecord != null && biometricRecord.getSegments() != null) && biometricRecord.getSegments().size() > 0;
        } catch (Exception e) {
            throw new RegistrationProcessorCheckedException(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getCode(),
                    PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage(), e);
        }
    }

    private boolean masterDataValidation(String id, String process, PacketValidationDto packetValidationDto)
            throws ApisResourceAccessException, IOException, JsonProcessingException, PacketManagerException {
        if (env.getProperty(VALIDATEMASTERDATA).trim().equalsIgnoreCase(VALIDATIONFALSE)) {
            return true;
        }
        boolean result = masterDataValidation.validateMasterData(id, process);
        if (!result) {
            packetValidationDto.setPacketValidaionFailureMessage(StatusUtil.MASTER_DATA_VALIDATION_FAILED.getMessage());
            packetValidationDto.setPacketValidatonStatusCode(StatusUtil.MASTER_DATA_VALIDATION_FAILED.getCode());
        }
        return result;
    }

    private boolean uinPresentInIdRepo(String uin) throws ApisResourceAccessException, IOException {
        return idRepoService.findUinFromIdrepo(uin, utility.getGetRegProcessorDemographicIdentity()) != null;

    }

    private boolean mandatoryValidation(String rid, String process,
                                        PacketValidationDto packetValidationDto)
            throws IOException, ApisResourceAccessException, PacketManagerException, JsonProcessingException {
        if (env.getProperty(VALIDATEMANDATORY).trim().equalsIgnoreCase(VALIDATIONFALSE))
            return true;
        else {
            boolean result = mandatoryValidation.mandatoryFieldValidation(rid, process, packetValidationDto);

            if (!result) {
                packetValidationDto.setPacketValidaionFailureMessage(StatusUtil.MANDATORY_VALIDATION_FAILED.getMessage());
                packetValidationDto.setPacketValidatonStatusCode(StatusUtil.MANDATORY_VALIDATION_FAILED.getCode());
            }
            return result;
        }
    }

    private boolean applicantDocumentValidation(String registrationId, String process, PacketValidationDto packetValidationDto)
            throws ApisResourceAccessException, JsonProcessingException, PacketManagerException, IOException {
        if (env.getProperty(VALIDATEAPPLICANTDOCUMENT).trim().equalsIgnoreCase(VALIDATIONFALSE))
            return true;

        boolean result = applicantDocumentValidation.validateDocument(registrationId, process);
        if (!result) {
            packetValidationDto.setPacketValidaionFailureMessage(StatusUtil.APPLICANT_DOCUMENT_VALIDATION_FAILED.getMessage());
            packetValidationDto.setPacketValidatonStatusCode(StatusUtil.APPLICANT_DOCUMENT_VALIDATION_FAILED.getCode());
        }
        return result;
    }


}
