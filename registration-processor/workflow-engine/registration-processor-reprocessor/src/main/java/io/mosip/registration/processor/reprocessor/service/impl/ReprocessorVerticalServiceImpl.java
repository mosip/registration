package io.mosip.registration.processor.reprocessor.service.impl;

import io.mosip.registration.processor.reprocessor.service.ReprocessorVerticalService;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class ReprocessorVerticalServiceImpl implements ReprocessorVerticalService {

    /** The registration status service. */
    @Autowired
    RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;


    @Override
    @Async
    public List<InternalRegistrationStatusDto> fetchUnProcessedPackets(List<String> processList, Integer fetchSize, long elapseTime, Integer reprocessCount, List<String> status, List<String> excludeStageNames) {
        return registrationStatusService.getUnProcessedPackets(processList, fetchSize, elapseTime,
                reprocessCount, status, excludeStageNames);
    }
}
