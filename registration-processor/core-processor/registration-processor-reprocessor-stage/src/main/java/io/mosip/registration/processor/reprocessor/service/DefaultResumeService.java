package io.mosip.registration.processor.reprocessor.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.exception.WorkflowActionException;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;

@Component
public class DefaultResumeService {
	@Autowired
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	@Autowired
	WorkflowActionService workflowActionService;
	
	@Value("${registration.processor.default.action.elapse.time}")
	private long elapseTime;
	
	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(DefaultResumeService.class);
	
	public void defaultResumeAction(MosipEventBus mosipEventBus) {
		regProcLogger.debug("processStopProcessing called for defaultResumeAction ");
		LogDescription description = new LogDescription();
		List<InternalRegistrationStatusDto> dtolist = null;
		List<String> statusList = new ArrayList<>();
		statusList.add(RegistrationTransactionStatusCode.SUCCESS.toString());
		statusList.add(RegistrationTransactionStatusCode.REPROCESS.toString());
		statusList.add(RegistrationTransactionStatusCode.IN_PROGRESS.toString());
		dtolist = registrationStatusService.getUnProcessedPackets(
				statusList,elapseTime);
		Map<String,List<String>> map=new HashMap<>();
		for(InternalRegistrationStatusDto dto:dtolist) {
			if(dto.getResumeTimeStamp().isBefore(LocalDateTime.now(ZoneId.of("UTC")))) {
				if(map.containsKey(dto.getDefaultResumeAction())) {
					List<String> ids=map.get(dto.getDefaultResumeAction());
					ids.add(dto.getRegistrationId());
					map.put(dto.getDefaultResumeAction(), ids);
				}
				else if(!map.containsKey(dto.getDefaultResumeAction())) {
					map.put(dto.getDefaultResumeAction(), Arrays.asList(dto.getRegistrationId()));
				}
			}
		}
		for(Entry<String,List<String>> entry:map.entrySet()) {
			try {
				workflowActionService.processWorkflowAction(entry.getValue(), entry.getKey(), mosipEventBus);
			} catch (WorkflowActionException e) {
				description.setCode(e.getErrorCode());
				description.setMessage(e.getMessage());
				regProcLogger.error("Error in  defaultResumeAction  for registration id  {} {} {} {}", entry.getValue(),
						e.getErrorCode(), e.getMessage(), ExceptionUtils.getStackTrace(e));
			}
		}

	}
}
