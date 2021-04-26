package io.mosip.registration.processor.status.service.impl;

import io.mosip.registration.processor.core.packet.dto.SubWorkflowDto;
import io.mosip.registration.processor.status.entity.SubWorkflowMappingEntity;
import io.mosip.registration.processor.status.entity.SubWorkflowPKEntity;
import io.mosip.registration.processor.status.repositary.BaseRegProcRepository;
import io.mosip.registration.processor.status.repositary.RegistrationRepositary;
import io.mosip.registration.processor.status.service.SubWorkflowMappingService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class SubWorkflowMappingServiceImpl implements SubWorkflowMappingService {

    @Autowired
    private BaseRegProcRepository<SubWorkflowMappingEntity, String> subWorkflowRepository;

   /* @Override
    public void saveSubWorkflowMapping(SubWorkflowDto dto) {
        SubWorkflowPKEntity s = new SubWorkflowPKEntity();
        s.setAdditionalInfoReqId(dto.getAdditionalInfoReqId());
        s.setRegId(dto.getRegId());
        SubWorkflowMappingEntity subWorkflowMappingEntity = new SubWorkflowMappingEntity();
        subWorkflowMappingEntity.setId(s);
        subWorkflowMappingEntity.setIteration(dto.getIteration());
        subWorkflowMappingEntity.setProcess(dto.getProcess());
        subWorkflowMappingEntity.setTimestamp(dto.getTimestamp());
        subWorkflowRepository.save(subWorkflowMappingEntity);
    }

    @Override
    public List<SubWorkflowDto> getSubWorkflowMapping(String regId) {
        List<SubWorkflowMappingEntity> subWorkflowMappingEntity = subWorkflowRepository.getSubWorkflowMapping(regId);
        List<SubWorkflowDto> subWorkflowDtos = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(subWorkflowMappingEntity))
            subWorkflowMappingEntity.forEach(m -> subWorkflowDtos.add(new SubWorkflowDto(m.getId().getRegId(),
                    m.getId().getAdditionalInfoReqId(), m.getProcess(), m.getIteration(), m.getTimestamp())));

        return subWorkflowDtos;
    }*/

    @Override
    public List<SubWorkflowDto> getWorkflowMappingByReqId(String infoRequestId) {
        List<SubWorkflowMappingEntity> subWorkflowMappingEntity = subWorkflowRepository.workflowMappingByReqId(infoRequestId);
        List<SubWorkflowDto> subWorkflowDtos = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(subWorkflowMappingEntity))
            subWorkflowMappingEntity.forEach(m -> subWorkflowDtos.add(new SubWorkflowDto(m.getId().getRegId(),
                    m.getId().getAdditionalInfoReqId(), m.getProcess(), m.getIteration(), m.getTimestamp())));

        return subWorkflowDtos;
    }
}
