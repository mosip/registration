package io.mosip.registration.processor.status.service;

import io.mosip.registration.processor.core.packet.dto.SubWorkflowDto;

import java.util.List;

public interface SubWorkflowMappingService {

    /*public void saveSubWorkflowMapping(SubWorkflowDto subWorkflowDto);

    public List<SubWorkflowDto> getSubWorkflowMapping(String regId);*/

    public List<SubWorkflowDto> getWorkflowMappingByReqId(String infoRequestId);
    
    public List<SubWorkflowDto> getWorkflowMappingByRIdAndProcessAndIteration(String registrationId,String process,Integer iteration);
}
