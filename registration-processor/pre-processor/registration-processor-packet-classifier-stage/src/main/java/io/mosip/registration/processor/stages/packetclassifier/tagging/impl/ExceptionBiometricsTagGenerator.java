package io.mosip.registration.processor.stages.packetclassifier.tagging.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.registration.processor.core.constant.JsonConstant;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.FieldValue;
import io.mosip.registration.processor.packet.storage.exception.ParsingException;
import io.mosip.registration.processor.stages.packetclassifier.dto.FieldDTO;
import io.mosip.registration.processor.stages.packetclassifier.tagging.TagGenerator;

@Component
@ConditionalOnExpression(value = "'${mosip.regproc.packet.classifier.tag-generators}'.contains('MosipExceptionBiometrics')")
public class ExceptionBiometricsTagGenerator implements TagGenerator {

    /** Tag name that will used be while tagging exception biometrics */
    @Value("${mosip.regproc.packet.classifier.tagging.exceptionbiometrics.tag-name:EXCEPTION_BIOMETRICS}")
    private String tagName;

    /** This mapping will contain the short words for each missing biometrics, the values will used for concatenating in the tags */
    @Value("#{${mosip.regproc.packet.classifier.tagging.exceptionbiometrics.bio-value-mapping:{'leftLittle':'LL','leftRing':'LR','leftMiddle':'LM','leftIndex':'LI','leftThumb':'LT','rightLittle':'RL','rightRing':'RR','rightMiddle':'RM','rightIndex':'RI','rightThumb':'RT','leftEye':'LE','rightEye':'RE'}}}")
    private Map<String,String> bioValueMapping;

    /** The tag value that will be used by default when the packet does not have value for the tag field */
    @Value("${mosip.regproc.packet.classifier.tagging.not-available-tag-value}")
    private String notAvailableTagValue;

	@Value("#{T(java.util.Arrays).asList('${mosip.regproc.packet.classifier.tagging.exceptionbiometrics.before-exception-metainfo-change.reg-client-versions:}')}")
	private List<String> regClientVersionsBeforeExceptionMetaInfoChange;

    /** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(ExceptionBiometricsTagGenerator.class);

    private static String BIOMETRICS_DELIMITER = ",";

	@Autowired
	ObjectMapper mapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getRequiredIdObjectFieldNames() throws BaseCheckedException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> generateTags(String workflowInstanceId, String registrationId, String process,
            Map<String, FieldDTO> idObjectFieldDTOMap, Map<String, String> metaInfoMap, int iteration) throws BaseCheckedException {
        try {
            Map<String, String> tags = new HashMap<String, String>(1);
            String exceptionBiometricsString = metaInfoMap.get(JsonConstant.EXCEPTIONBIOMETRICS);
            if(exceptionBiometricsString == null) {
                regProcLogger.warn("{} --> {}, setting tag value as {}", 
                    PlatformErrorMessages.RPR_PCM_EXCEPTION_BIOMETRICS_ENTRY_NOT_AVAILABLE.getCode(), 
                    PlatformErrorMessages.RPR_PCM_EXCEPTION_BIOMETRICS_ENTRY_NOT_AVAILABLE.getMessage(),
                    notAvailableTagValue);
                tags.put(tagName, notAvailableTagValue);
                return tags;
            }
            JSONObject exceptionBiometricsJsonObject = new JSONObject(exceptionBiometricsString);
			String regClientVersion = getRegClientVersionFromMetaInfo(registrationId, process, metaInfoMap);
			JSONObject applicantJsonObject = null;
			// if the version is not found in metainfo then consider it is as older and
			// expect applicant to be available inside the exception biometrics
			if (regClientVersion == null || (regClientVersion != null
					&& regClientVersionsBeforeExceptionMetaInfoChange.contains(regClientVersion))) {
				if (!exceptionBiometricsJsonObject.has(JsonConstant.EXCEPTIONBIOMETRICSAPPLICANT)) {
					regProcLogger.warn("{} --> {}, setting tag value as {}",
							PlatformErrorMessages.RPR_PCM_EXCEPTION_BIOMETRICS_APPLICANT_ENTRY_NOT_AVAILABLE.getCode(),
							PlatformErrorMessages.RPR_PCM_EXCEPTION_BIOMETRICS_APPLICANT_ENTRY_NOT_AVAILABLE
									.getMessage(),
							notAvailableTagValue);
					tags.put(tagName, notAvailableTagValue);
					return tags;
				}
				applicantJsonObject = exceptionBiometricsJsonObject
						.getJSONObject(JsonConstant.EXCEPTIONBIOMETRICSAPPLICANT);

			} else {
				if (!exceptionBiometricsJsonObject.has(JsonConstant.INDIVIDUAL_BIOMETRICS)) {
					regProcLogger.warn("{} --> {}, setting tag value as {}",
							PlatformErrorMessages.RPR_PCM_EXCEPTION_BIOMETRICS_APPLICANT_ENTRY_NOT_AVAILABLE.getCode(),
							PlatformErrorMessages.RPR_PCM_EXCEPTION_BIOMETRICS_APPLICANT_ENTRY_NOT_AVAILABLE
									.getMessage(),
							notAvailableTagValue);
					tags.put(tagName, notAvailableTagValue);
					return tags;
				}
				applicantJsonObject = exceptionBiometricsJsonObject
						.getJSONObject(JsonConstant.INDIVIDUAL_BIOMETRICS);
			}

                
            if (applicantJsonObject == null || applicantJsonObject.length() == 0)
                tags.put(tagName, "");
            else {
                StringJoiner exceptionBiometricStringJoiner = new StringJoiner(BIOMETRICS_DELIMITER);
                for(Map.Entry<String,String> entry : bioValueMapping.entrySet()) {
                    if(applicantJsonObject.has(entry.getKey()))
                        exceptionBiometricStringJoiner.add(entry.getValue());
                }
                tags.put(tagName, exceptionBiometricStringJoiner.toString());
            }

            return tags;
		} catch (JSONException | JsonProcessingException e) {
            throw new ParsingException( 
                PlatformErrorMessages.RPR_PCM_META_INFO_JSON_PARSING_FAILED.getMessage(), e);
        }
    }

	private String getRegClientVersionFromMetaInfo(String id, String process, Map<String, String> metaInfoMap)
			throws JSONException, JsonProcessingException
	{
		String metadata = metaInfoMap.get(JsonConstant.METADATA);
		String version = null;
		if (StringUtils.isNotEmpty(metadata)) {
			JSONArray jsonArray = new JSONArray(metadata);

			for (int i = 0; i < jsonArray.length(); i++) {
				if (!jsonArray.isNull(i)) {
					org.json.JSONObject jsonObject = (org.json.JSONObject) jsonArray.get(i);
					FieldValue fieldValue = mapper.readValue(jsonObject.toString(), FieldValue.class);
					if (fieldValue.getLabel().equalsIgnoreCase(JsonConstant.REGCLIENTVERSION)) {
						version = fieldValue.getValue();
						break;
					}
				}
			}
		}
		return version;
	}
}
