package io.mosip.registration.processor.status.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import io.mosip.registration.processor.core.packet.dto.AdditionalInfoRequestDto;
import io.mosip.registration.processor.status.entity.AdditionalInfoRequestEntity;
import io.mosip.registration.processor.status.entity.AdditionalInfoRequestPKEntity;
import io.mosip.registration.processor.status.repositary.BaseRegProcRepository;
import io.mosip.registration.processor.status.service.AdditionalInfoRequestService;

public class AdditionalInfoRequestServiceImpl implements AdditionalInfoRequestService {

    @Autowired
    private BaseRegProcRepository<AdditionalInfoRequestEntity, String> additionalInfoRequestRepository;

    @Override
    public AdditionalInfoRequestDto getAdditionalInfoRequestByReqId(String infoRequestId) {
        List<AdditionalInfoRequestEntity> additionalInfoRequestEntityList = 
			additionalInfoRequestRepository.getAdditionalInfoRequestByReqId(infoRequestId);
		List<AdditionalInfoRequestDto> additionalInfoRequestDtos = 
			convertToAdditionalInfoRequestDtos(additionalInfoRequestEntityList);
		if(additionalInfoRequestDtos.isEmpty())
			return null;
        return additionalInfoRequestDtos.get(0);
    }
    
    @Override
   	public  AdditionalInfoRequestDto getAdditionalInfoRequestByRegIdAndProcessAndIteration(
			String regId, String additionalInfoProcess, int additionalInfoIteration) {
   		 List<AdditionalInfoRequestEntity> additionalInfoRequestEntityList = additionalInfoRequestRepository
				.getAdditionalInfoRequestByRegIdAndProcessAndIteration(regId, additionalInfoProcess, 
					additionalInfoIteration);
		List<AdditionalInfoRequestDto> additionalInfoRequestDtos = 
			convertToAdditionalInfoRequestDtos(additionalInfoRequestEntityList);
		if(additionalInfoRequestDtos.isEmpty())
			return null;
        return additionalInfoRequestDtos.get(0);
   	}

	@Override
	public  List<AdditionalInfoRequestDto> getAdditionalInfoByRid(String regId) {
		List<AdditionalInfoRequestEntity> additionalInfoRequestEntityList =
				additionalInfoRequestRepository.getAdditionalInfoByRegId(regId);
		List<AdditionalInfoRequestDto> additionalInfoRequestDtos =
				convertToAdditionalInfoRequestDtos(additionalInfoRequestEntityList);
		return additionalInfoRequestDtos;
	}

    @Override
	public List<AdditionalInfoRequestDto> getAdditionalInfoRequestByRegIdAndProcess(String regId, 
			String additionalInfoProcess) {
		List<AdditionalInfoRequestEntity> additionalInfoRequestEntityList = additionalInfoRequestRepository
				.getAdditionalInfoRequestByRegIdAndProcess(regId, additionalInfoProcess);
		return convertToAdditionalInfoRequestDtos(additionalInfoRequestEntityList);
	}

	@Override
	public void addAdditionalInfoRequest(AdditionalInfoRequestDto additionalInfoRequestDto) {
		AdditionalInfoRequestPKEntity additionalInfoRequestPKEntity = new AdditionalInfoRequestPKEntity();
		additionalInfoRequestPKEntity.setAdditionalInfoReqId(additionalInfoRequestDto.getAdditionalInfoReqId());
		additionalInfoRequestPKEntity.setWorkflowInstanceId(additionalInfoRequestDto.getWorkflowInstanceId());
        AdditionalInfoRequestEntity additionalInfoRequestEntity = new AdditionalInfoRequestEntity();
		additionalInfoRequestEntity.setId(additionalInfoRequestPKEntity);
		additionalInfoRequestEntity.setRegId(additionalInfoRequestDto.getRegId());
		additionalInfoRequestEntity.setAdditionalInfoIteration(
			additionalInfoRequestDto.getAdditionalInfoIteration());
		additionalInfoRequestEntity.setAdditionalInfoProcess(
			additionalInfoRequestDto.getAdditionalInfoProcess());
		additionalInfoRequestEntity.setTimestamp(additionalInfoRequestDto.getTimestamp());
        additionalInfoRequestRepository.save(additionalInfoRequestEntity);
	}

	private List<AdditionalInfoRequestDto> convertToAdditionalInfoRequestDtos(
		List<AdditionalInfoRequestEntity> additionalInfoRequestEntityList) {
			List<AdditionalInfoRequestDto> additionalInfoRequestDtos = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(additionalInfoRequestEntityList))
			additionalInfoRequestEntityList.forEach(m -> additionalInfoRequestDtos.add(
				new AdditionalInfoRequestDto(m.getId().getAdditionalInfoReqId(), 
					m.getId().getWorkflowInstanceId(), m.getRegId(),
                    m.getAdditionalInfoProcess(), m.getAdditionalInfoIteration(),
					m.getTimestamp())));
		return additionalInfoRequestDtos;		
	}

}
