package io.mosip.registration.processor.stages.packetclassifier.tagging.impl;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.constant.JsonConstant;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.FieldValue;
import io.mosip.registration.processor.packet.storage.exception.ParsingException;
import io.mosip.registration.processor.stages.packetclassifier.dto.FieldDTO;
import io.mosip.registration.processor.stages.packetclassifier.tagging.TagGenerator;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression(value = "'${mosip.regproc.packet.classifier.tag-generators}'.contains('MosipMetaInfo')")
public class MetaInfoTagGenerator implements TagGenerator {

    /** The tag name that will be prefixed with every metainfo operationsData tags */
    @Value("${mosip.regproc.packet.classifier.tagging.metainfo.operationsdata.tag-name-prefix:META_INFO-OPERATIONS_DATA-}")
    private String operationsDataTagNamePrefix;

    /** The tag name that will be prefixed with every metainfo metaData tags */
    @Value("${mosip.regproc.packet.classifier.tagging.metainfo.metadata.tag-name-prefix:META_INFO-META_DATA-}")
    private String metaDataTagNamePrefix;

    /** The tag name that will be prefixed with every metainfo capturedRegisteredDevices tags */
    @Value("${mosip.regproc.packet.classifier.tagging.metainfo.capturedregistereddevices.tag-name-prefix:META_INFO-CAPTURED_REGISTERED_DEVICES-}")
    private String capturedRegisteredDevicesTagNamePrefix;

    /** The labels on metainfo.operationsData array that needs to be tagged */
    @Value("#{T(java.util.Arrays).asList('${mosip.regproc.packet.classifier.tagging.metainfo.operationsdata.tag-labels:}')}")
    private List<String> operationsDataTagLabels;

    /** The labels on metainfo.metaData array that needs to be tagged */
    @Value("#{T(java.util.Arrays).asList('${mosip.regproc.packet.classifier.tagging.metainfo.metadata.tag-labels:}')}")
    private List<String> metaDataTagLabels;

    /** The serial numbers of devices type on metainfo.capturedRegisteredDevices array that needs to be tagged */
    @Value("#{T(java.util.Arrays).asList('${mosip.regproc.packet.classifier.tagging.metainfo.capturedregistereddevices.device-types:}')}")
    private List<String> capturedRegisteredDeviceTypes;

    /** The tag value that will be used by default when the packet does not have value for the tag field */
    @Value("${mosip.regproc.packet.classifier.tagging.not-available-tag-value}")
    private String notAvailableTagValue;

    /** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(MetaInfoTagGenerator.class);

    /** Mapper utility used for unmarshalling JSON to Java objects  */
    @Autowired
    private ObjectMapper objectMapper;

    private static String TAG_VALUE_DELIMITER = "-";

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
    Map<String, FieldDTO> idObjectFieldDTOMap, Map<String, String> metaInfoMap, int iteration) 
                throws BaseCheckedException {
        try {
            Map<String, String> operationsDataMap = generateTagsFromOpertationsData(metaInfoMap);
            Map<String, String> metaDataMap = generateTagsFromMetaData(metaInfoMap);
            Map<String, String> capturedRegisteredDevicesMap =
                generateTagsFromCapturedRegisteredDevices(metaInfoMap);
            Map<String, String> allTags = new HashMap<String, String>();
            allTags.putAll(operationsDataMap);
            allTags.putAll(metaDataMap);
            allTags.putAll(capturedRegisteredDevicesMap);
            return allTags;
        } catch (JSONException e) {
            throw new ParsingException( 
                PlatformErrorMessages.RPR_PCM_META_INFO_JSON_PARSING_FAILED.getMessage(), e);
        } catch (IOException e) {
            throw new BaseCheckedException(
                PlatformErrorMessages.RPR_PCM_DATA_OBJECT_MAPPING_FAILED.getCode(), 
                PlatformErrorMessages.RPR_PCM_DATA_OBJECT_MAPPING_FAILED.getMessage(), e);
        }
    }

	private Map<String, String> generateTagsFromOpertationsData(Map<String, String> metaInfoMap)
			throws JSONException, IOException, BaseCheckedException {
        Map<String, String> tags = new HashMap<String, String>();
        String operationsDataString = metaInfoMap.get(JsonConstant.OPERATIONSDATA);
        if(operationsDataString == null)
            throw new BaseCheckedException(
                PlatformErrorMessages.RPR_PCM_OPERATIONS_DATA_ENTRY_NOT_AVAILABLE.getCode(), 
                PlatformErrorMessages.RPR_PCM_OPERATIONS_DATA_ENTRY_NOT_AVAILABLE.getMessage());
        JSONArray operationsDataJsonArray = new JSONArray(operationsDataString);
        Map<String, String> operationsDataMap = getMapFromLabelValueArray(operationsDataJsonArray);
        operationsDataTagLabels.forEach(tagLabel -> {
            if(operationsDataMap.containsKey(tagLabel)) {
                String tagLabelValue = operationsDataMap.get(tagLabel);
                tags.put(operationsDataTagNamePrefix + tagLabel, tagLabelValue);
            } else {
                regProcLogger.warn("{} --> {}, for label {}, setting tag value as {}", 
                    PlatformErrorMessages.RPR_PCM_OPERATIONS_DATA_ENTRY_NOT_AVAILABLE.getCode(), 
                    PlatformErrorMessages.RPR_PCM_OPERATIONS_DATA_ENTRY_NOT_AVAILABLE.getMessage(),
                    tagLabel, notAvailableTagValue);
                tags.put(operationsDataTagNamePrefix + tagLabel, notAvailableTagValue);
            }

        });
        return tags;
    }

	private Map<String, String> generateTagsFromMetaData(Map<String, String> metaInfoMap)
			throws JSONException, IOException, BaseCheckedException {
        Map<String, String> tags = new HashMap<String, String>();
        String metaDataString = metaInfoMap.get(JsonConstant.METADATA);
        if(metaDataString == null)
            throw new BaseCheckedException(
                PlatformErrorMessages.RPR_PCM_META_DATA_ENTRY_NOT_AVAILABLE.getCode(), 
                PlatformErrorMessages.RPR_PCM_META_DATA_ENTRY_NOT_AVAILABLE.getMessage());
        JSONArray metaDataJsonArray = new JSONArray(metaDataString);
        Map<String, String> metaDataMap = getMapFromLabelValueArray(metaDataJsonArray);
        metaDataTagLabels.forEach(tagLabel -> {
            if(metaDataMap.containsKey(tagLabel)) {
                String tagLabelValue = metaDataMap.get(tagLabel);
                tags.put(metaDataTagNamePrefix + tagLabel, tagLabelValue);
            } else {
                regProcLogger.warn("{} --> {}, for label {}, setting tag value as {}", 
                    PlatformErrorMessages.RPR_PCM_META_DATA_ENTRY_NOT_AVAILABLE.getCode(), 
                    PlatformErrorMessages.RPR_PCM_META_DATA_ENTRY_NOT_AVAILABLE.getMessage(),
                    tagLabel, notAvailableTagValue);
                tags.put(metaDataTagNamePrefix + tagLabel, notAvailableTagValue);
            }
        });
        return tags;
    }

	private Map<String, String> generateTagsFromCapturedRegisteredDevices(Map<String, String> metaInfoMap)
			throws JSONException, BaseCheckedException {
        Map<String, String> tags = new HashMap<String, String>();
        String capturedRegisteredDevicesString = metaInfoMap.get(JsonConstant.CAPTUREDREGISTEREDDEVICES);
        if(capturedRegisteredDevicesString == null)
            throw new BaseCheckedException(
                PlatformErrorMessages.RPR_PCM_CAPTURED_REGISTERED_DEVICES_ENTRY_NOT_AVAILABLE.getCode(), 
                PlatformErrorMessages.RPR_PCM_CAPTURED_REGISTERED_DEVICES_ENTRY_NOT_AVAILABLE.getMessage());
        JSONArray capturedRegisteredDevicesJsonArray = new JSONArray(capturedRegisteredDevicesString);
        for(String deviceType : capturedRegisteredDeviceTypes) {
            for(int i=0; i< capturedRegisteredDevicesJsonArray.length(); i++) {
                JSONObject digitalId = capturedRegisteredDevicesJsonArray.getJSONObject(i)
                    .getJSONObject(JsonConstant.DIGITALID);
                if(digitalId.getString(JsonConstant.DIGITALIDTYPE).equals(deviceType))
                    tags.put(capturedRegisteredDevicesTagNamePrefix + deviceType,
                        digitalId.getString(JsonConstant.DIGITALIDMAKE) + TAG_VALUE_DELIMITER +
                        digitalId.getString(JsonConstant.DIGITALIDMODEL) + TAG_VALUE_DELIMITER +
                        digitalId.getString(JsonConstant.DIGITALIDSERIALNO));
            }
            if(!tags.containsKey(capturedRegisteredDevicesTagNamePrefix + deviceType)) {
                regProcLogger.warn("{} --> {}, for deviceType {}, setting tag value as {}", 
                    PlatformErrorMessages.RPR_PCM_CAPTURED_REGISTERED_DEVICES_ENTRY_NOT_AVAILABLE.getCode(), 
                    PlatformErrorMessages.RPR_PCM_CAPTURED_REGISTERED_DEVICES_ENTRY_NOT_AVAILABLE.getMessage(),
                    deviceType, notAvailableTagValue);
                tags.put(capturedRegisteredDevicesTagNamePrefix + deviceType, notAvailableTagValue);
            }
        }
        return tags;
    }

	private Map<String, String> getMapFromLabelValueArray(JSONArray jsonArray) throws IOException, JSONException {
        Map<String, String> map = new HashMap<String, String>();
		for (int i =0; i < jsonArray.length(); i++) {
			JSONObject jsonObject = (JSONObject) jsonArray.get(i);
			FieldValue fieldValue = objectMapper.readValue(jsonObject.toString(), FieldValue.class);
			map.put(fieldValue.getLabel(), fieldValue.getValue());
        }
        return map;
	}
}
