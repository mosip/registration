package io.mosip.registration.processor.status.service;

import java.util.List;

import io.mosip.registration.processor.core.packet.dto.SubWorkflowDto;

public interface SubWorkflowMappingService {

	public void addSubWorkflowMapping(SubWorkflowDto subWorkflowDto);

    public List<SubWorkflowDto> getWorkflowMappingByReqId(String infoRequestId);
    
    public List<SubWorkflowDto> getSubWorkflowMappingByRegIdAndProcessAndIteration(String regId,String process,int iteration);

	public List<SubWorkflowDto> getSubWorkflowMappingByRegIdAndProcess(String regId, String process);

}
