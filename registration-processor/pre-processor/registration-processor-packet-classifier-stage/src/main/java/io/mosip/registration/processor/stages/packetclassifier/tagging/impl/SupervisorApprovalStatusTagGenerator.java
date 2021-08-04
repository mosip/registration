package io.mosip.registration.processor.stages.packetclassifier.tagging.impl;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.stages.packetclassifier.dto.FieldDTO;
import io.mosip.registration.processor.stages.packetclassifier.tagging.TagGenerator;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.service.SyncRegistrationService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression(value = "'${mosip.regproc.packet.classifier.tag-generators}'.contains('MosipSupervisorApprovalStatus')")
public class SupervisorApprovalStatusTagGenerator implements TagGenerator {

    /** Tag name that will be used while tagging supervisor approval status */
    @Value("${mosip.regproc.packet.classifier.tagging.supervisorapprovalstatus.tag-name:SUPERVISOR_APPROVAL_STATUS}")
	private String tagName;

	/**  The sync registration service */
	@Autowired
    private SyncRegistrationService<SyncResponseDto, SyncRegistrationDto> syncRegistrationService;
    
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
    public Map<String, String> generateTags(String workflowInstanceId, String registrationId, 
            String process, Map<String, FieldDTO> identityFieldMap,
            Map<String, String> metaInfoMap, int iteration) throws BaseCheckedException {
        Map<String, String> tags = new HashMap<String, String>();
        SyncRegistrationEntity regEntity = syncRegistrationService.findByWorkflowInstanceId(workflowInstanceId);
        if(regEntity == null)
            throw new BaseCheckedException(
                PlatformErrorMessages.RPR_PCM_SYNC_REGISTRATION_ENTITY_NOT_AVAILABLE.getCode(), 
                PlatformErrorMessages.RPR_PCM_SYNC_REGISTRATION_ENTITY_NOT_AVAILABLE.getMessage());
        tags.put(tagName, regEntity.getSupervisorStatus());
        return tags;
    }
    
}
