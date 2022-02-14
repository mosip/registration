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

    public boolean validateDocument(String registrationId, String process) throws IdentityNotFoundException, IOException, ApisResourceAccessException, PacketManagerException, JsonProcessingException {
		// validate all documents from mapping json
        JSONObject docMappingJson = utility.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT);

        for (Object doc : docMappingJson.values()) {
            Map docMap = (LinkedHashMap) doc;
            String docValue = docMap.values().iterator().next().toString();
            String idObjectField = packetManagerService.getField(registrationId, docValue, process, ProviderStageName.PACKET_VALIDATOR);
            if (idObjectField != null) {
                if (packetManagerService.getDocument(registrationId, docValue, process, ProviderStageName.PACKET_VALIDATOR) == null) {
                    regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                            LoggerFileConstant.REGISTRATIONID.toString(), registrationId, "Missing document : " + docValue);
                    return false;
                }
            }
        }

        // validate INDIVIDUAL_BIOMETRICS and INTRODUCER_BIO.
		JSONObject identityMappingJson = utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);

        String applicantBiometricLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(identityMappingJson, MappingJsonConstants.INDIVIDUAL_BIOMETRICS), VALUE);
        String introducerBiometricLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(identityMappingJson, MappingJsonConstants.INTRODUCER_BIO), VALUE);

        List<String> fields = new ArrayList<>();
        fields.add(applicantBiometricLabel);
        fields.add(introducerBiometricLabel);

        Map<String, String> docFields = packetManagerService.getFields(registrationId, fields, process, ProviderStageName.PACKET_VALIDATOR);

        if (docFields.get(applicantBiometricLabel) != null) {
            BiometricRecord biometricRecord = packetManagerService.getBiometricsByMappingJsonKey(registrationId, MappingJsonConstants.INDIVIDUAL_BIOMETRICS, process, ProviderStageName.PACKET_VALIDATOR);
            if (biometricRecord == null || biometricRecord.getSegments() == null || biometricRecord.getSegments().size() == 0) {
                regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                        LoggerFileConstant.REGISTRATIONID.toString(), registrationId, "Missing document : " + applicantBiometricLabel);
                return false;
            }
        }
        if (docFields.get(introducerBiometricLabel) != null) {
            BiometricRecord biometricRecord = packetManagerService.getBiometricsByMappingJsonKey(registrationId, MappingJsonConstants.INTRODUCER_BIO, process, ProviderStageName.PACKET_VALIDATOR);
            if (biometricRecord == null || biometricRecord.getSegments() == null || biometricRecord.getSegments().size() == 0)
                return false;
        }


        regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                registrationId, "ApplicantDocumentValidation::validateApplicantData::exit");
        return true;
    }


}