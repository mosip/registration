package io.mosip.registration.processor.stages.validator.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.registration.processor.core.exception.PacketManagerNonRecoverableException;
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
import io.mosip.registration.processor.core.packet.dto.packetvalidator.PacketValidationDto;
import io.mosip.registration.processor.core.spi.packet.validator.PacketValidator;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.packet.storage.dto.ValidatePacketResponse;
import io.mosip.registration.processor.packet.storage.exception.IdRepoAppException;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.packet.storage.utils.Utility;
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
    private Utilities utililites;

    @Autowired
    private Utility utility;

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
    public boolean validate(String id, String process, PacketValidationDto packetValidationDto, Map<String, String> metaInfo)
            throws ApisResourceAccessException, RegistrationProcessorCheckedException, IOException,
            JsonProcessingException, PacketManagerException {
        String uin = null;
        try {
            ValidatePacketResponse response = packetManagerService.validate(id, process, ProviderStageName.PACKET_VALIDATOR);
            String consentVal = packetManagerService.getField(id, MappingJsonConstants.CONSENT, process, ProviderStageName.PACKET_VALIDATOR);
            if (!response.isValid()) {
                regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                        LoggerFileConstant.REGISTRATIONID.toString(), id,
                        "ERROR =======>" + StatusUtil.PACKET_MANAGER_VALIDATION_FAILURE.getMessage());
                packetValidationDto.setPacketValidatonStatusCode(StatusUtil.PACKET_MANAGER_VALIDATION_FAILURE.getCode());
                packetValidationDto.setPacketValidaionFailureMessage(StatusUtil.PACKET_MANAGER_VALIDATION_FAILURE.getMessage());
                return false;
            }
            if (consentVal != null && StringUtils.isNotEmpty(consentVal) && consentVal.equalsIgnoreCase("N")) {
                regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                        LoggerFileConstant.REGISTRATIONID.toString(), id,
                        "ERROR =======>" + StatusUtil.PACKET_CONSENT_VALIDATION.getMessage());
                packetValidationDto.setPacketValidatonStatusCode(StatusUtil.PACKET_CONSENT_VALIDATION.getCode());
                packetValidationDto.setPacketValidaionFailureMessage(StatusUtil.PACKET_CONSENT_VALIDATION.getMessage());
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
                JSONObject jsonObject = utililites.retrieveIdrepoJson(uin);
                if (jsonObject == null) {
                    regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                            LoggerFileConstant.REGISTRATIONID.toString(), id,
                            "ERROR =======>" + PlatformErrorMessages.RPR_PIS_IDENTITY_NOT_FOUND.getMessage());
                    throw new IdRepoAppException(PlatformErrorMessages.RPR_PIS_IDENTITY_NOT_FOUND.getMessage());
                }
                String status = utililites.retrieveIdrepoJsonStatus(uin);
                if (process.equalsIgnoreCase(RegistrationType.UPDATE.toString())
                        && status.equalsIgnoreCase(RegistrationType.DEACTIVATED.toString())) {
                    regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                            LoggerFileConstant.REGISTRATIONID.toString(), id,
                            "ERROR =======>" + PlatformErrorMessages.RPR_PVM_UPDATE_DEACTIVATED.getMessage());
                    throw new RegistrationProcessorCheckedException(
                            PlatformErrorMessages.RPR_PVM_UPDATE_DEACTIVATED.getCode(), "UIN is Deactivated");
                }
            }

            // document validation - pass map to cache INDIVIDUAL_BIOMETRICS and INTRODUCER_BIO for reuse in biometricsXSDValidation
            Map<String, BiometricRecord> fetchedBiometrics = new HashMap<>();
            if (!applicantDocumentValidation(id, process, packetValidationDto, fetchedBiometrics)) {
                regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                        LoggerFileConstant.REGISTRATIONID.toString(), id,
                        "ERROR =======>" + StatusUtil.APPLICANT_DOCUMENT_VALIDATION_FAILED.getMessage());
                return false;
            }

            // check if uin is in idrepisitory
            if (RegistrationType.UPDATE.name().equalsIgnoreCase(process)
                    || RegistrationType.RES_UPDATE.name().equalsIgnoreCase(process)) {

                if (!utililites.uinPresentInIdRepo(String.valueOf(uin))) {
                    regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                            LoggerFileConstant.REGISTRATIONID.toString(), id,
                            "ERROR =======>" + StatusUtil.UIN_NOT_FOUND_IDREPO.getMessage());
                    packetValidationDto.setPacketValidaionFailureMessage(StatusUtil.UIN_NOT_FOUND_IDREPO.getMessage());
                    packetValidationDto.setPacketValidatonStatusCode(StatusUtil.UIN_NOT_FOUND_IDREPO.getCode());
                    return false;
                }
            }

            if (!biometricsXSDValidation(id, process, packetValidationDto, metaInfo, fetchedBiometrics)) {
                return false;
            }
        } catch(PacketManagerNonRecoverableException e){
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    id, RegistrationStatusCode.FAILED.toString() + e.getMessage() + ExceptionUtils.getStackTrace(e));
            throw e;
        } catch (PacketManagerException e) {
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

    private boolean biometricsXSDValidation(String id, String process, PacketValidationDto packetValidationDto, Map<String, String> metaInfoMap,
                                            Map<String, BiometricRecord> fetchedBiometrics)
            throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException,
            RegistrationProcessorCheckedException, JSONException {
        List<String> fields = Arrays.asList(MappingJsonConstants.INDIVIDUAL_BIOMETRICS,
                MappingJsonConstants.AUTHENTICATION_BIOMETRICS, MappingJsonConstants.INTRODUCER_BIO,
                MappingJsonConstants.OFFICERBIOMETRICFILENAME, MappingJsonConstants.SUPERVISORBIOMETRICFILENAME);

        if (metaInfoMap == null) {
            metaInfoMap = packetManagerService.getMetaInfo(id, process, ProviderStageName.PACKET_VALIDATOR);
        }
        final Map<String, String> finalMetaInfoMap = metaInfoMap;

        // Validate all biometric fields in parallel using virtual threads (fail-fast: cancel remaining on first failure)
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        try {
            List<CompletableFuture<Void>> futures = fields.stream()
                    .map(field -> CompletableFuture.runAsync(() -> {
                        try {
                            BiometricRecord biometricRecord = null;
                            if (field.equals(MappingJsonConstants.OFFICERBIOMETRICFILENAME)
                                    || field.equals(MappingJsonConstants.SUPERVISORBIOMETRICFILENAME)) {
                                String value = getOperationsDataFromMetaInfo(id, process, field, finalMetaInfoMap);
                                if (value != null && !value.isEmpty()) {
                                    biometricRecord = packetManagerService.getBiometrics(id, field, process,
                                            ProviderStageName.PACKET_VALIDATOR);
                                }
                            } else {
                                // For INDIVIDUAL_BIOMETRICS and INTRODUCER_BIO, reuse if already fetched by ApplicantDocumentValidation
                                if (fetchedBiometrics != null && fetchedBiometrics.containsKey(field)) {
                                    biometricRecord = fetchedBiometrics.get(field);
                                } else {
                                    biometricRecord = packetManagerService.getBiometricsByMappingJsonKey(id, field, process,
                                            ProviderStageName.PACKET_VALIDATOR);
                                }
                            }
                            if (biometricRecord != null) {
                                biometricsXSDValidator.validateXSD(biometricRecord);
                                biometricsSignatureValidator.validateSignature(id, process, biometricRecord, finalMetaInfoMap);
                            }
                        } catch (Exception e) {
                            throw new CompletionException(e);
                        }
                    }, executor))
                    .collect(Collectors.toList());

            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            } catch (CompletionException e) {
                // Interrupt remaining in-flight threads immediately (fail-fast)
                executor.shutdownNow();
                Throwable cause = e.getCause();
                if (cause instanceof CbeffException) {
                    regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                            LoggerFileConstant.REGISTRATIONID.toString(), id,
                            "ERROR =======> " + StatusUtil.XSD_VALIDATION_EXCEPTION.getMessage());
                    packetValidationDto.setPacketValidaionFailureMessage(
                            StatusUtil.XSD_VALIDATION_EXCEPTION.getMessage());
                    packetValidationDto.setPacketValidatonStatusCode(StatusUtil.XSD_VALIDATION_EXCEPTION.getCode());
                    return false;
                } else if (cause instanceof BiometricSignatureValidationException) {
                    regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                            LoggerFileConstant.REGISTRATIONID.toString(), id,
                            "ERROR =======> " + StatusUtil.BIOMETRICS_SIGNATURE_VALIDATION_FAILURE.getMessage());
                    packetValidationDto.setPacketValidaionFailureMessage(
                            StatusUtil.BIOMETRICS_SIGNATURE_VALIDATION_FAILURE.getMessage() + "--> "
                                    + cause.getMessage());
                    packetValidationDto.setPacketValidatonStatusCode(
                            StatusUtil.BIOMETRICS_SIGNATURE_VALIDATION_FAILURE.getCode());
                    return false;
                } else if (cause instanceof ApisResourceAccessException) {
                    throw (ApisResourceAccessException) cause;
                } else if (cause instanceof PacketManagerException) {
                    throw (PacketManagerException) cause;
                } else if (cause instanceof JsonProcessingException) {
                    throw (JsonProcessingException) cause;
                } else if (cause instanceof JSONException) {
                    throw (JSONException) cause;
                } else if (cause instanceof IOException) {
                    throw (IOException) cause;
                } else {
                    throw new RegistrationProcessorCheckedException(
                            PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getCode(),
                            PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage(), cause);
                }
            }
        } finally {
            executor.close(); // waits for any still-running threads to finish (fast after shutdownNow)
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
                    if (jsonObject.optString("label").equalsIgnoreCase(fileName)) {
                        value = jsonObject.optString("value");
                        break;
                    }
                }
            }
        }
        return value;
    }


    private boolean applicantDocumentValidation(String registrationId, String process,
                                                PacketValidationDto packetValidationDto,
                                                Map<String, BiometricRecord> fetchedBiometrics)
            throws ApisResourceAccessException, JsonProcessingException, PacketManagerException, IOException {
        String validateApplicant = env.getProperty(VALIDATEAPPLICANTDOCUMENT);
        if (validateApplicant != null && validateApplicant.trim().equalsIgnoreCase(VALIDATIONFALSE))
            return true;
        else {
            String validateApplicantDocument = env.getProperty(VALIDATEAPPLICANTDOCUMENTPROCESS);
            if (validateApplicantDocument != null && validateApplicantDocument.contains(process)) {
                boolean result = applicantDocumentValidation.validateDocument(registrationId, process, fetchedBiometrics);
                if (!result) {
                    packetValidationDto.setPacketValidaionFailureMessage(StatusUtil.APPLICANT_DOCUMENT_VALIDATION_FAILED.getMessage());
                    packetValidationDto.setPacketValidatonStatusCode(StatusUtil.APPLICANT_DOCUMENT_VALIDATION_FAILED.getCode());
                }
                return result;
            }
            return true;
        }
    }

}

