package io.mosip.registration.processor.stages.packetclassifier.tagging.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONException;
import org.json.JSONObject;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

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
import io.mosip.registration.processor.stages.packetclassifier.utility.PacketClassifierUtility;

@Component
@ConditionalOnExpression(value = "'${mosip.regproc.packet.classifier.tag-generators}'.contains('MosipIDObjectDataAvailability')")
public class IDObjectDataAvailabilityTagGenerator implements TagGenerator {

    /**
     * The keys of this map will used as tag names and values should be single field or a boolean mvel 
     * expression continaing multiple fields. These field names in expression should be as in keys of 
     * registraion-processor-identity.json file Identity segment and should have proper default source 
     * configured
     */
    @Value("#{${mosip.regproc.packet.classifier.tagging.idobject-data-availability.availability-expression-map:{}}}")
    private Map<String,String> availabilityExpressionMap;

    /** The tag value that will be used by default when the packet does not have value for the tag field */
    @Value("${mosip.regproc.packet.classifier.tagging.not-available-tag-value}")
    private String notAvailableTagValue;

    /** The constant for value label in JSON parsing */
    private static final String VALUE_LABEL = "value";

    /** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(
        IDObjectDataAvailabilityTagGenerator.class);

    /** Frequently used util methods are available in this bean */
    @Autowired
    private Utilities utility;
    
    @Autowired
    private PacketClassifierUtility classifierUtility;
    
    /** 
     * This map will hold the actual field names after resolving, using mapping JSON as keys and 
     * configured field names as values 
     */
    private Map<String, String> requiredIDObjectFieldNamesMap;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getRequiredIdObjectFieldNames() throws BaseCheckedException {
        try {
            org.json.simple.JSONObject identityMappingJson = 
                utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);
            requiredIDObjectFieldNamesMap = new HashMap<>();
            for(Map.Entry<String, String> expressionEntry : availabilityExpressionMap.entrySet()) {
                ParserContext parserContext = ParserContext.create();
                MVEL.analysisCompile(expressionEntry.getValue(), parserContext);
                Map<String, Class> expressionVariablesMap = parserContext.getInputs();
                for(Map.Entry<String, Class> variableEntry: expressionVariablesMap.entrySet()) {
                    String actualFieldName = JsonUtil.getJSONValue(
                        JsonUtil.getJSONObject(identityMappingJson, variableEntry.getKey()), 
                            VALUE_LABEL);
                    if(actualFieldName == null)
                        throw new BaseCheckedException(
                            PlatformErrorMessages.RPR_PCM_FIELD_NAME_NOT_AVAILABLE_IN_MAPPING_JSON.getCode(), 
                            PlatformErrorMessages.RPR_PCM_FIELD_NAME_NOT_AVAILABLE_IN_MAPPING_JSON.getMessage());
                    requiredIDObjectFieldNamesMap.put(actualFieldName, variableEntry.getKey());
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
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> generateTags(String workflowInstanceId, String registrationId, String process, 
        Map<String, FieldDTO> idObjectFieldDTOMap, Map<String, String> metaInfoMap, int iteration)  
                throws BaseCheckedException {
        try {
            Map<String, Boolean> fieldAvailabilityMap = new HashMap<String, Boolean>();
            for(Map.Entry<String, String> entry : requiredIDObjectFieldNamesMap.entrySet()) {
                String tagFieldValue = getValueBasedOnType(entry.getKey(), 
                    idObjectFieldDTOMap.get(entry.getKey()));
                if(tagFieldValue == null || tagFieldValue.trim().length() == 0 || 
                        tagFieldValue.equals(notAvailableTagValue))
                    fieldAvailabilityMap.put(entry.getValue(), false);
                else
                    fieldAvailabilityMap.put(entry.getValue(), true);
            }

            Map<String, String> tags = new HashMap<String, String>();
            for(Map.Entry<String, String> entry : availabilityExpressionMap.entrySet()) {
                Boolean result = (Boolean) MVEL.eval(entry.getValue(), fieldAvailabilityMap);   
                tags.put(entry.getKey(), result.toString());
            }
            return tags;
        } catch(JSONException e) {
            throw new ParsingException( 
                    PlatformErrorMessages.RPR_PCM_SCHEMA_DATA_TYPE_JSON_PARSING_FAILED.getMessage(), e);
        }
    }

    private String getValueBasedOnType(String fieldName, FieldDTO fieldDTO) throws JSONException, BaseCheckedException {
        if(fieldDTO == null || (fieldDTO.getValue() == null && !"string".equalsIgnoreCase(fieldDTO.getType()))) {
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
            	return classifierUtility.getLanguageBasedValueForSimpleType(value);
            case "string":
                return value;
            case "number":
                return value;
            case "documentType":
                JSONObject documentTypeJSON = new JSONObject(value);
                if(documentTypeJSON.isNull(VALUE_LABEL))
                    return null;
                return documentTypeJSON.getString(VALUE_LABEL);
            case "biometricsType":
                JSONObject biometricsTypeJSON = new JSONObject(value);
                if(biometricsTypeJSON.isNull(VALUE_LABEL))
                    return null;
                return biometricsTypeJSON.getString(VALUE_LABEL);
            default:
                throw new BaseCheckedException(
                    PlatformErrorMessages.RPR_PCM_UNKNOWN_SCHEMA_DATA_TYPE.getCode(), 
                    PlatformErrorMessages.RPR_PCM_UNKNOWN_SCHEMA_DATA_TYPE.getMessage() + 
                    " Field name: " + fieldName + " type: " + type);
        }
    }
}
