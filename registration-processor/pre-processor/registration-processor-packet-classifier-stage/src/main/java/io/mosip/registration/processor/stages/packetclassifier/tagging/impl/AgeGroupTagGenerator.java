package io.mosip.registration.processor.stages.packetclassifier.tagging.impl;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.stages.packetclassifier.dto.FieldDTO;
import io.mosip.registration.processor.stages.packetclassifier.tagging.TagGenerator;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /** Different age group that should be used for age group tagging */
    @Value("#{'${mosip.regproc.packet.classifier.tagging.agegroup.group-names:CHILD,ADULT,SENIOR_CITIZEN}'.split(',')}")
    private List<String> ageGroupNames;

    /** Below age ranges that corresponding to the group names, if first value is CHILD in group-names 
     * and if first value is 18 in below-ranges, then age below 18 will be tagged as CHILD */
    @Value("#{'${mosip.regproc.packet.classifier.tagging.agegroup.below-ranges:18,60,200}'.split(',')}")
    private List<Integer> ageBelowRanges;
    
    /** Frequently used util methods are available in this bean */
    @Autowired
    private Utilities utility;

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
            Map<String, FieldDTO> idObjectFieldDTOMap, Map<String, String> metaInfoMap) 
                throws BaseCheckedException {
        try {
            String ageGroup = "";
            int age = utility.getApplicantAge(registrationId, process);
            for(int i=0; i < ageBelowRanges.size(); i++) {
                if(age < ageBelowRanges.get(i))  {
                    ageGroup = ageGroupNames.get(i);
                    break;
                }
            }
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
