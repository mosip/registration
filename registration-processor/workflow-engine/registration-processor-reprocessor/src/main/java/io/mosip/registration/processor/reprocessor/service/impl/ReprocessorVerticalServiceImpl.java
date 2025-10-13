package io.mosip.registration.processor.reprocessor.service.impl;

import io.mosip.registration.processor.reprocessor.service.ReprocessorVerticalService;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class ReprocessorVerticalServiceImpl implements ReprocessorVerticalService {

    /** The registration status service. */
    @Autowired
    private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

    @Override
    @Async
    public CompletableFuture<List<InternalRegistrationStatusDto>> fetchUnProcessedPackets(List<String> processList, Integer fetchSize, long elapseTime, Integer reprocessCount, List<String> trnStatusList, List<String> excludeStageNames, List<String> skipRegIds, List<String> statusList) {
        List<InternalRegistrationStatusDto> result =  registrationStatusService.getUnProcessedPackets(processList, fetchSize, elapseTime,
                reprocessCount, trnStatusList, excludeStageNames, skipRegIds, statusList);
        return CompletableFuture.completedFuture(result != null ? result : Collections.emptyList());
    }
}
