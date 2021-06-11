package io.mosip.registration.processor.status.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import io.mosip.registration.processor.core.packet.dto.SubWorkflowDto;
import io.mosip.registration.processor.status.entity.SubWorkflowMappingEntity;
import io.mosip.registration.processor.status.entity.SubWorkflowPKEntity;
import io.mosip.registration.processor.status.repositary.BaseRegProcRepository;
import io.mosip.registration.processor.status.service.SubWorkflowMappingService;

public class SubWorkflowMappingServiceImpl implements SubWorkflowMappingService {

    @Autowired
    private BaseRegProcRepository<SubWorkflowMappingEntity, String> subWorkflowRepository;

    @Override
    public List<SubWorkflowDto> getWorkflowMappingByReqId(String infoRequestId) {
        List<SubWorkflowMappingEntity> subWorkflowMappingEntity = subWorkflowRepository.workflowMappingByReqId(infoRequestId);
        List<SubWorkflowDto> subWorkflowDtos = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(subWorkflowMappingEntity))
            subWorkflowMappingEntity.forEach(m -> subWorkflowDtos.add(new SubWorkflowDto(m.getId().getRegId(),
                    m.getId().getAdditionalInfoReqId(), m.getProcess(), m.getIteration(), m.getTimestamp(),m.getParentProcess(), m.getParentIteration())));

        return subWorkflowDtos;
    }
    
    @Override
   	public  List<SubWorkflowDto>  getSubWorkflowMappingByRegIdAndProcessAndIteration(String regId, String process,
   			int iteration) {
   		 List<SubWorkflowMappingEntity> subWorkflowEntity = subWorkflowRepository.workflowMappingByRegIdAndProcessAndIteration(regId,process,iteration);
   		 List<SubWorkflowDto> subWorkflowDtos = new ArrayList<>();
   	        if (CollectionUtils.isNotEmpty(subWorkflowEntity))
   	        	subWorkflowEntity.forEach(m -> subWorkflowDtos.add(new SubWorkflowDto(m.getId().getRegId(),
   	                    m.getId().getAdditionalInfoReqId(), m.getProcess(), m.getIteration(), m.getTimestamp(),m.getParentProcess(), m.getParentIteration())));

   	        return subWorkflowDtos;
   	}
    @Override
	public List<SubWorkflowDto> getSubWorkflowMappingByRegIdAndProcess(String regId, String process) {
		List<SubWorkflowMappingEntity> subWorkflowEntity = subWorkflowRepository
				.workflowMappingByRegIdAndProcess(regId, process);
		List<SubWorkflowDto> subWorkflowDtos = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(subWorkflowEntity))
			subWorkflowEntity.forEach(m -> subWorkflowDtos
					.add(new SubWorkflowDto(m.getId().getRegId(), m.getId().getAdditionalInfoReqId(), m.getProcess(),
							m.getIteration(), m.getTimestamp(), m.getParentProcess(), m.getParentIteration())));
		return subWorkflowDtos;
	}

	@Override
	public void addSubWorkflowMapping(SubWorkflowDto subWorkflowDto) {
		SubWorkflowPKEntity subWorkflowPKEntity = new SubWorkflowPKEntity();
		subWorkflowPKEntity.setAdditionalInfoReqId(subWorkflowDto.getAdditionalInfoReqId());
		subWorkflowPKEntity.setRegId(subWorkflowDto.getRegId());
        SubWorkflowMappingEntity subWorkflowMappingEntity = new SubWorkflowMappingEntity();
		subWorkflowMappingEntity.setId(subWorkflowPKEntity);
		subWorkflowMappingEntity.setIteration(subWorkflowDto.getIteration());
		subWorkflowMappingEntity.setProcess(subWorkflowDto.getProcess());
		subWorkflowMappingEntity.setTimestamp(subWorkflowDto.getTimestamp());
		subWorkflowMappingEntity.setParentIteration(subWorkflowDto.getParentIteration());
		subWorkflowMappingEntity.setParentProcess(subWorkflowDto.getParentProcess());
        subWorkflowRepository.save(subWorkflowMappingEntity);
	}

}
