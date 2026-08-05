package io.mosip.registration.processor.packet.storage.utils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.idrepo.dto.ResponseDTO;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.status.entity.RegistrationStatusEntity;
import io.mosip.registration.processor.status.repositary.RegistrationRepositary;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;

/**
 * Shared stale-reprocess detection logic used by CreateDraftStage and FinalizationStage.
 *
 * A packet is considered stale when a newer committed packet for the same UIN
 * has already been processed after the current packet was created.
 */
@Component
public class StaleReprocessChecker {

    private static final Logger regProcLogger = RegProcessorLogger.getLogger(StaleReprocessChecker.class);

    @Autowired
    private RegistrationProcessorRestClientService<Object> registrationProcessorRestClientService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RegistrationRepositary<RegistrationStatusEntity, String> registrationRepositary;

    /**
     * Returns true if a later-committed packet for the same UIN has already been
     * processed after {@code currentPktCrDtimes}, making this packet obsolete.
     * Returns false (safe-to-proceed) if the UIN is unknown, the check service is
     * unavailable, or no qualifying committed packet is found.
     */
    @SuppressWarnings("unchecked")
    public boolean isStaleReprocess(String uin, LocalDateTime currentPktCrDtimes, String currentRegId) {
        if (StringUtils.isEmpty(uin) || currentPktCrDtimes == null) {
            return false;
        }
        try {
            List<String> pathSegments = new ArrayList<>();
            pathSegments.add(uin);
            ResponseWrapper<ResponseDTO> response = (ResponseWrapper<ResponseDTO>)
                    registrationProcessorRestClientService.getApi(
                            ApiName.IDREPOGETIDBYUIN, pathSegments, "type", "demo", ResponseWrapper.class);
            if (response == null || response.getResponse() == null) {
                return false;
            }
            ResponseDTO idRepoResponse = objectMapper.convertValue(response.getResponse(), ResponseDTO.class);
            if (idRepoResponse == null || StringUtils.isEmpty(idRepoResponse.getEntity())) {
                return false;
            }
            String lastCommittedRegId = idRepoResponse.getEntity();
            if (lastCommittedRegId.equals(currentRegId)) {
                return false;
            }
            List<LocalDateTime> times = registrationRepositary.findPktCrDtimesByRegId(
                    lastCommittedRegId, RegistrationStatusCode.PROCESSED.toString());
            if (times == null || times.isEmpty()) {
                return false;
            }
            return times.get(0).isAfter(currentPktCrDtimes);
        } catch (Exception e) {
            regProcLogger.warn("SESSIONID", "REGISTRATIONID", currentRegId,
                    "StaleReprocessChecker :: isStaleReprocess check failed (fail-safe): " + e.getMessage());
            return false;
        }
    }
}
