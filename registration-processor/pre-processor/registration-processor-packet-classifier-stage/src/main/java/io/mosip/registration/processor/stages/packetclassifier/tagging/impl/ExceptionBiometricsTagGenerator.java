package io.mosip.registration.processor.stages.packetclassifier.tagging.impl;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.registration.processor.core.constant.JsonConstant;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.packet.storage.exception.ParsingException;
import io.mosip.registration.processor.stages.packetclassifier.dto.FieldDTO;
import io.mosip.registration.processor.stages.packetclassifier.tagging.TagGenerator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression(value = "'${mosip.regproc.packet.classifier.tag-generators}'.contains('MosipExceptionBiometrics')")
public class ExceptionBiometricsTagGenerator implements TagGenerator {

    /** Tag name that will used be while tagging exception biometrics */
    @Value("${mosip.regproc.packet.classifier.tagging.exceptionbiometrics.tag-name:EXCEPTION_BIOMETRICS}")
    private String tagName;

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
    public Map<String, String> generateTags(String registrationId, String process,
            Map<String, FieldDTO> idObjectFieldDTOMap, Map<String, String> metaInfoMap) throws BaseCheckedException {
        try {
            String exceptionBiometricsString = metaInfoMap.get(JsonConstant.EXCEPTIONBIOMETRICS);
            if(exceptionBiometricsString == null)
                throw new BaseCheckedException(
                    PlatformErrorMessages.RPR_PCM_EXCEPTION_BIOMETRICS_ENTRY_NOT_AVAILABLE.getCode(), 
                    PlatformErrorMessages.RPR_PCM_EXCEPTION_BIOMETRICS_ENTRY_NOT_AVAILABLE.getMessage());
            JSONArray metaDataJsonArray = new JSONArray(exceptionBiometricsString);
            Map<String, String> tags = new HashMap<String, String>(1);
            if (metaDataJsonArray.length() > 0)
                tags.put(tagName, "true");
            else
                tags.put(tagName, "false");
            return tags;
        } catch (JSONException e) {
            throw new ParsingException( 
                PlatformErrorMessages.RPR_PCM_META_INFO_JSON_PARSING_FAILED.getMessage(), e);
        }
    }
    
}
