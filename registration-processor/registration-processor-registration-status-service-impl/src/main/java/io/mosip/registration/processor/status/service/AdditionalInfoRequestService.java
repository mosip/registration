package io.mosip.registration.processor.status.service;

import java.util.List;

import io.mosip.registration.processor.core.packet.dto.AdditionalInfoRequestDto;

public interface AdditionalInfoRequestService {

    public void addAdditionalInfoRequest(AdditionalInfoRequestDto additionalInfoRequestDto);

    public AdditionalInfoRequestDto getAdditionalInfoRequestByReqId(String infoRequestId);
    
    public AdditionalInfoRequestDto getAdditionalInfoRequestByRegIdAndProcessAndIteration(
        String regId, String additionalInfoProcess, int additionalInfoIteration);

    public List<AdditionalInfoRequestDto> getAdditionalInfoRequestByRegIdAndProcess(String regId, 
        String additionalInfoProcess);

    public  List<AdditionalInfoRequestDto> getAdditionalInfoByRid(String regId);

}
