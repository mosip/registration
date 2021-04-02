package io.mosip.registration.processor.stages.packetclassifier.tagging.impl;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.exception.ParsingException;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.stages.packetclassifier.dto.FieldDTO;
import io.mosip.registration.processor.stages.packetclassifier.tagging.TagGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression(value = "'${mosip.regproc.packet.classifier.tag-generators}'.contains('MosipIDObjectFields')")
public class IDObjectFieldsTagGenerator implements TagGenerator {

    /**
     * These field names should be as in keys of registraion-processor-identity.json file Identity segment
     * and should have proper default source configured
     */
    @Value("#{'${mosip.regproc.packet.classifier.tagging.idobjectfields.mapping-field-names}'.split(',')}")
    private List<String> mappingFieldNames;

    /** The tag name that will be prefixed with every idobjectfield tags */
    @Value("${mosip.regproc.packet.classifier.tagging.idobjectfields.tag-name-prefix:ID_OBJECT-}")
    private String tagNamePrefix;

    /** The language that should be used when dealing with field type that has values in multiple languages */
    @Value("${mosip.primary-language}")
    private String tagLanguage;

    /** The tag value that will be used by default when the packet does not have value for the tag field */
    @Value("${mosip.regproc.packet.classifier.tagging.not-available-tag-value}")
    private String notAvailableTagValue;

    /** The constant for value label in JSON parsing */
    private static final String VALUE_LABEL = "value";

    /** The constant for language label in JSON parsing */
    private static final String LANGUAGE_LABEL = "language";

    /** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(IDObjectFieldsTagGenerator.class);

    /** Frequently used util methods are available in this bean */
    @Autowired
    private Utilities utility;
    
    /** This list will hold the actual field names once resolved using the mapping JSON */
    private List<String> requiredIDObjectFieldNames;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getRequiredIdObjectFieldNames() throws BaseCheckedException {
        try {
            org.json.simple.JSONObject identityMappingJson = utility.getRegistrationProcessorMappingJson(
                MappingJsonConstants.IDENTITY);
            requiredIDObjectFieldNames = new ArrayList<>();
            for(String field : mappingFieldNames) {
                String actualFieldName = JsonUtil.getJSONValue(
                    JsonUtil.getJSONObject(identityMappingJson, field), VALUE_LABEL);
                if(actualFieldName == null)
                    throw new BaseCheckedException(
                        PlatformErrorMessages.RPR_PCM_FIELD_NAME_NOT_AVAILABLE_IN_MAPPING_JSON.getCode(), 
                        PlatformErrorMessages.RPR_PCM_FIELD_NAME_NOT_AVAILABLE_IN_MAPPING_JSON.getMessage());
                requiredIDObjectFieldNames.add(actualFieldName);
            }
            return requiredIDObjectFieldNames;
        } catch (IOException e) {
            throw new BaseCheckedException(
                PlatformErrorMessages.RPR_PCM_ACCESSING_IDOBJECT_MAPPING_FILE_FAILED.getCode(), 
                PlatformErrorMessages.RPR_PCM_ACCESSING_IDOBJECT_MAPPING_FILE_FAILED.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> generateTags(String registrationId, String process, 
        Map<String, FieldDTO> idObjectFieldDTOMap, Map<String, String> metaInfoMap)  
                throws BaseCheckedException {
        try {
            Map<String, String> tags = new HashMap<String, String>();
            for(String fieldName : requiredIDObjectFieldNames) {
                String tagFieldValue = getValueBasedOnType(fieldName, idObjectFieldDTOMap.get(fieldName));
                tags.put(tagNamePrefix + fieldName, tagFieldValue);
            }
            return tags;
        } catch(JSONException e) {
            throw new ParsingException( 
                    PlatformErrorMessages.RPR_PCM_SCHEMA_DATA_TYPE_JSON_PARSING_FAILED.getMessage(), e);
        }
    }

    private String getValueBasedOnType(String fieldName, FieldDTO fieldDTO) throws JSONException, BaseCheckedException {
        if(fieldDTO == null || (fieldDTO.getValue() == null && fieldDTO.getType() != "string")) {
            regProcLogger.warn("{} --> {} Field name: {} setting value as {}", 
                PlatformErrorMessages.RPR_PCM_FIELD_DTO_OR_NON_STRING_FIELD_IS_NULL.getCode(), 
                PlatformErrorMessages.RPR_PCM_FIELD_DTO_OR_NON_STRING_FIELD_IS_NULL.getMessage(),
                fieldName, notAvailableTagValue);
            return notAvailableTagValue;
        }
        String type = fieldDTO.getType();
        String value = fieldDTO.getValue();
        switch (type) {
            case "simpleType":
                JSONArray jsonArray = new JSONArray(value);
                for(int i=0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    if(jsonObject.getString(LANGUAGE_LABEL).equals(tagLanguage)) {
                        return jsonObject.getString(VALUE_LABEL);
                    }
                }
                throw new BaseCheckedException(
                    PlatformErrorMessages.RPR_PCM_VALUE_NOT_AVAILABLE_IN_CONFIGURED_LANGUAGE.getCode(), 
                    PlatformErrorMessages.RPR_PCM_VALUE_NOT_AVAILABLE_IN_CONFIGURED_LANGUAGE.getMessage() +
                        " Field name: " + fieldName + " language: " + tagLanguage);
            case "string":
                return value;
            case "number":
                return value;
            case "documentType":
                return new JSONObject(value).getString(VALUE_LABEL);
            case "biometricsType":
                return new JSONObject(value).getString(VALUE_LABEL);
            default:
                throw new BaseCheckedException(
                    PlatformErrorMessages.RPR_PCM_UNKNOWN_SCHEMA_DATA_TYPE.getCode(), 
                    PlatformErrorMessages.RPR_PCM_UNKNOWN_SCHEMA_DATA_TYPE.getMessage() + 
                    " Field name: " + fieldName + " type: " + type);
        }
    }
    
}
