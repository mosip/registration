package io.mosip.registration.processor.reprocessor.service;


import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;

import java.util.List;

public interface ReprocessorVerticalService {

    List<InternalRegistrationStatusDto> fetchUnProcessedPackets(List<String> processList, Integer fetchSize, long elapseTime, Integer reprocessCount,
                                                                List<String> status, List<String> excludeStageNames);
}
