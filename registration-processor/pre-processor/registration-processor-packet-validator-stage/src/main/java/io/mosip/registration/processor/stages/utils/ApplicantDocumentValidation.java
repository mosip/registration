package io.mosip.registration.processor.stages.utils;

import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.exception.IdentityNotFoundException;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


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

    public boolean validateDocument(String registrationId, String process, Map<String, BiometricRecord> fetchedBiometrics) throws IdentityNotFoundException, IOException, ApisResourceAccessException, PacketManagerException, JsonProcessingException {
        JSONObject docMappingJson = utility.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT);
        JSONObject identityMappingJson = utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);

        List<String> docFieldNames = new ArrayList<>();
        for (Object doc : docMappingJson.values()) {
            Map docMap = (LinkedHashMap) doc;
            docFieldNames.add(docMap.values().iterator().next().toString());
        }

        String applicantBiometricLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(identityMappingJson, MappingJsonConstants.INDIVIDUAL_BIOMETRICS), VALUE);
        String introducerBiometricLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(identityMappingJson, MappingJsonConstants.INTRODUCER_BIO), VALUE);

        // Single batch getFields call for all document + biometric presence checks
        List<String> allFields = new ArrayList<>(docFieldNames);
        allFields.add(applicantBiometricLabel);
        allFields.add(introducerBiometricLabel);
        Map<String, String> fieldValues = packetManagerService.getFields(registrationId, allFields, process, ProviderStageName.PACKET_VALIDATOR);

        // Validate documents
        for (String docValue : docFieldNames) {
            if (fieldValues.get(docValue) != null) {
                if (packetManagerService.getDocument(registrationId, docValue, process, ProviderStageName.PACKET_VALIDATOR) == null) {
                    regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                            LoggerFileConstant.REGISTRATIONID.toString(), registrationId, "Missing document : " + docValue);
                    return false;
                }
            }
        }

        // Validate INDIVIDUAL_BIOMETRICS presence
        if (fieldValues.get(applicantBiometricLabel) != null) {
            BiometricRecord biometricRecord = packetManagerService.getBiometricsByMappingJsonKey(registrationId, MappingJsonConstants.INDIVIDUAL_BIOMETRICS, process, ProviderStageName.PACKET_VALIDATOR);
            if (biometricRecord == null || biometricRecord.getSegments() == null || biometricRecord.getSegments().size() == 0) {
                regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                        LoggerFileConstant.REGISTRATIONID.toString(), registrationId, "Missing document : " + applicantBiometricLabel);
                return false;
            }
            if (fetchedBiometrics != null) fetchedBiometrics.put(MappingJsonConstants.INDIVIDUAL_BIOMETRICS, biometricRecord);
        }

        // Validate INTRODUCER_BIO presence
        if (fieldValues.get(introducerBiometricLabel) != null) {
            BiometricRecord biometricRecord = packetManagerService.getBiometricsByMappingJsonKey(registrationId, MappingJsonConstants.INTRODUCER_BIO, process, ProviderStageName.PACKET_VALIDATOR);
            if (biometricRecord == null || biometricRecord.getSegments() == null || biometricRecord.getSegments().size() == 0)
                return false;
            if (fetchedBiometrics != null) fetchedBiometrics.put(MappingJsonConstants.INTRODUCER_BIO, biometricRecord);
        }


        regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                registrationId, "ApplicantDocumentValidation::validateApplicantData::exit");
        return true;
    }


}