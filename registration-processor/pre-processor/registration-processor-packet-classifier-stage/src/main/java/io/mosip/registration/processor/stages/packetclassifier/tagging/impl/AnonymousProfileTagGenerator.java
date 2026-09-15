package io.mosip.registration.processor.stages.packetclassifier.tagging.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.stages.packetclassifier.dto.FieldDTO;
import io.mosip.registration.processor.stages.packetclassifier.tagging.TagGenerator;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.service.AnonymousProfileService;

/**
 * Builds the anonymous profile JSON and stores it as a packet tag so that
 * WorkflowInternalActionVerticle can retrieve it without re-building from raw
 * fields. The tag name is configurable so countries can customise the key.
 *
 * Exceptions are caught internally — a failure here must never block the rest
 * of the packet classification pipeline.
 */
@Component
public class AnonymousProfileTagGenerator implements TagGenerator {

    private static final Logger regProcLogger =
            RegProcessorLogger.getLogger(AnonymousProfileTagGenerator.class);

    private static final String VALUE_LABEL = "value";

    @Value("${mosip.regproc.packet.classifier.tagging.anonymous-profile.tag-name:ANONYMOUS}")
    private String tagName;

    /**
     * Identity mapping JSON keys required to build the anonymous profile.
     * Add more keys in configuration if the anonymous profile is extended later.
     */
    @Value("#{T(java.util.Arrays).asList('${mosip.regproc.packet.classifier.tagging.anonymous-profile.mapping-field-names:dob,gender,email,phone,preferredLanguage,locationHierarchyForProfiling}')}")
    private List<String> mappingFieldNames;

    @Autowired
    private AnonymousProfileService anonymousProfileService;

    @Autowired
    private PriorityBasedPacketManagerService priorityBasedPacketManagerService;

    @Autowired
    private Utilities utility;

    /**
     * Resolves configured identity mapping keys to actual packet field names,
     * same approach as {@link IDObjectFieldsTagGenerator}.
     */
    @Override
    public List<String> getRequiredIdObjectFieldNames() throws BaseCheckedException {
        try {
            org.json.simple.JSONObject identityMappingJson =
                    utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);
            Map<String, String> requiredIDObjectFieldNamesMap = new HashMap<>();
            for (String field : mappingFieldNames) {
                String actualFieldName = JsonUtil.getJSONValue(
                        JsonUtil.getJSONObject(identityMappingJson, field),
                        VALUE_LABEL);
                if (actualFieldName == null)
                    throw new BaseCheckedException(
                            PlatformErrorMessages.RPR_PCM_FIELD_NAME_NOT_AVAILABLE_IN_MAPPING_JSON.getCode(),
                            PlatformErrorMessages.RPR_PCM_FIELD_NAME_NOT_AVAILABLE_IN_MAPPING_JSON.getMessage());
                // locationHierarchyForProfiling maps to comma-separated actual field names
                // (e.g. zone,postalCode). Other keys map to a single field; split is a no-op.
                for (String resolvedName : actualFieldName.split(",")) {
                    String trimmedName = resolvedName.trim();
                    if (!trimmedName.isEmpty()) {
                        requiredIDObjectFieldNamesMap.put(trimmedName, field);
                    }
                }
            }
            return requiredIDObjectFieldNamesMap.keySet().stream().collect(Collectors.toList());
        } catch (IOException e) {
            throw new BaseCheckedException(
                    PlatformErrorMessages.RPR_PCM_ACCESSING_IDOBJECT_MAPPING_FILE_FAILED.getCode(),
                    PlatformErrorMessages.RPR_PCM_ACCESSING_IDOBJECT_MAPPING_FILE_FAILED.getMessage(), e);
        }
    }

    /**
     * Builds the anonymous profile from the pre-fetched identity fields and
     * independently fetched biometrics. Returns a single-entry map
     * {@code {tagName → profileJson}} on success, or an empty map on any error.
     */
    @Override
    public Map<String, String> generateTags(String workflowInstanceId, String registrationId,
            String process, Map<String, FieldDTO> idObjectFieldDTOMap,
            Map<String, String> metaInfoMap, int iteration) throws BaseCheckedException {
        try {
            Map<String, String> allFieldMap = new HashMap<>();
            Map<String, String> fieldTypeMap = new HashMap<>();
            for (Map.Entry<String, FieldDTO> entry : idObjectFieldDTOMap.entrySet()) {
                allFieldMap.put(entry.getKey(), entry.getValue().getValue());
                if (entry.getValue().getType() != null) {
                    fieldTypeMap.put(entry.getKey(), entry.getValue().getType());
                }
            }

            BiometricRecord biometricRecord = priorityBasedPacketManagerService.getBiometrics(
                    registrationId, MappingJsonConstants.INDIVIDUAL_BIOMETRICS,
                    process, ProviderStageName.CLASSIFICATION);

            String anonymousProfileJson = anonymousProfileService.buildJsonStringFromPacketInfo(
                    biometricRecord, allFieldMap, fieldTypeMap, metaInfoMap,
                    RegistrationStatusCode.PROCESSING.toString(),
                    ModuleName.PACKET_CLASSIFIER.toString());

            if (anonymousProfileJson != null && !anonymousProfileJson.isEmpty()) {
                Map<String, String> tags = new HashMap<>();
                tags.put(tagName, anonymousProfileJson);
                return tags;
            }
        } catch (Exception e) {
            regProcLogger.warn(
                    "AnonymousProfileTagGenerator: profile build failed for {}; packet classification continues. Error: {}",
                    registrationId, e.getMessage());
        }
        return Collections.emptyMap();
    }
}
