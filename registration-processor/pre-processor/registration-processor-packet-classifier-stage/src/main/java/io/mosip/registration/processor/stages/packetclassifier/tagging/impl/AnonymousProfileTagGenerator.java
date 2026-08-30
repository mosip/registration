package io.mosip.registration.processor.stages.packetclassifier.tagging.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
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

    @Value("${mosip.regproc.packet.classifier.tagging.anonymous-profile.tag-name:anonymous}")
    private String tagName;

    @Autowired
    private AnonymousProfileService anonymousProfileService;

    @Autowired
    private PriorityBasedPacketManagerService priorityBasedPacketManagerService;

    /**
     * No additional fields required — the processor already fetches the full
     * default-schema field set and passes it via idObjectFieldDTOMap.
     */
    @Override
    public List<String> getRequiredIdObjectFieldNames() throws BaseCheckedException {
        return Collections.emptyList();
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
            // Reconstruct plain field maps from the DTO map provided by the processor
            Map<String, String> allFieldMap = new HashMap<>();
            Map<String, String> fieldTypeMap = new HashMap<>();
            for (Map.Entry<String, FieldDTO> entry : idObjectFieldDTOMap.entrySet()) {
                allFieldMap.put(entry.getKey(), entry.getValue().getValue());
                if (entry.getValue().getType() != null) {
                    fieldTypeMap.put(entry.getKey(), entry.getValue().getType());
                }
            }

            // Biometrics are fetched here; failure is non-fatal
            BiometricRecord biometricRecord = null;
            try {
                biometricRecord = priorityBasedPacketManagerService.getBiometrics(
                        registrationId, MappingJsonConstants.INDIVIDUAL_BIOMETRICS,
                        process, ProviderStageName.CLASSIFICATION);
            } catch (Exception e) {
                regProcLogger.warn("AnonymousProfileTagGenerator: biometrics fetch failed for {}: {}",
                        registrationId, e.getMessage());
            }

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
