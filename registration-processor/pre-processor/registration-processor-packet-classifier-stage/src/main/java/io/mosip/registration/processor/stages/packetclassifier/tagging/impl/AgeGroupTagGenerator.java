package io.mosip.registration.processor.stages.packetclassifier.tagging.impl;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.stages.packetclassifier.dto.FieldDTO;
import io.mosip.registration.processor.stages.packetclassifier.tagging.TagGenerator;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression(value = "'${mosip.regproc.packet.classifier.tag-generators}'.contains('MosipAgeGroup')")
public class AgeGroupTagGenerator implements TagGenerator {

    /** Tag name that will be used while tagging age group */
    @Value("${mosip.regproc.packet.classifier.tagging.agegroup.tag-name:AGE_GROUP}")
    private String tagName;

    /** Below age ranges map should contain proper age group name and age range, any overlap of the age 
     * range will result in a random behaviour of tagging. In range, upper and lower values are inclusive. */
    @Value("#{${mosip.regproc.packet.classifier.tagging.agegroup.ranges:{'CHILD':'0-17','ADULT':'18-59','SENIOR_CITIZEN':'60-200'}}}")
    private Map<String,String> ageGroupRangeMap;
    
    /** The tag value that will be used by default when the packet does not have value for the tag field */
    @Value("${mosip.regproc.packet.classifier.tagging.not-available-tag-value}")
    private String notAvailableTagValue;
    
    /** Frequently used util methods are available in this bean */
    @Autowired
    private Utilities utility;

    private static String RANGE_DELIMITER = "-";

    private Map<String, int[]> parsedAgeGroupRangemap;

    @PostConstruct
    private void generateParsedAgeGroupRangeMap() {
        parsedAgeGroupRangemap = new HashMap<>();
        for (Map.Entry<String,String> entry : ageGroupRangeMap.entrySet()) {
            String[] range = entry.getValue().split(RANGE_DELIMITER);
            int[] rangeArray = new int[2];
            rangeArray[0] = Integer.parseInt(range[0]);
            rangeArray[1] = Integer.parseInt(range[1]);
            parsedAgeGroupRangemap.put(entry.getKey(), rangeArray);
        }
    }

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
            String ageGroup = "";
            int age = utility.getApplicantAge(registrationId, process, ProviderStageName.CLASSIFICATION);
            
			if (age == -1) {
				ageGroup = notAvailableTagValue;
			} else {
				for (Map.Entry<String, int[]> entry : parsedAgeGroupRangemap.entrySet()) {
					if (age >= entry.getValue()[0] && age <= entry.getValue()[1]) {
						ageGroup = entry.getKey();
						break;
					}
				}
			}

            if(ageGroup == null || ageGroup.trim().isEmpty())
                throw new BaseCheckedException(
                    PlatformErrorMessages.RPR_PCM_AGE_GROUP_NOT_FOUND.getCode(), 
                    PlatformErrorMessages.RPR_PCM_AGE_GROUP_NOT_FOUND.getMessage() + " Age: " + age); 

            Map<String, String> tags = new HashMap<String, String>();
            tags.put(tagName, ageGroup);
            return tags;
        } catch (IOException e) {
            throw new BaseCheckedException(
                PlatformErrorMessages.RPR_PCM_ACCESSING_IDOBJECT_MAPPING_FILE_FAILED.getCode(), 
                PlatformErrorMessages.RPR_PCM_ACCESSING_IDOBJECT_MAPPING_FILE_FAILED.getMessage(), e);
        }
    }
}
