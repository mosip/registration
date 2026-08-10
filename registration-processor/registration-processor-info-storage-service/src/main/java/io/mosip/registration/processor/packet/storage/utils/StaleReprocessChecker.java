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
     * Returns {@link StaleCheckResult#STALE} if a later-committed packet for the same UIN was
     * processed after {@code currentPktCrDtimes}, {@link StaleCheckResult#NOT_STALE} when a
     * successful lookup confirms no newer committed packet exists, and
     * {@link StaleCheckResult#UNAVAILABLE} when the check cannot be completed (API error,
     * null response, etc.) — callers must requeue rather than proceed.
     */
    @SuppressWarnings("unchecked")
    public StaleCheckResult checkStaleReprocess(String uin, LocalDateTime currentPktCrDtimes, String currentRegId) {
        if (StringUtils.isEmpty(uin) || currentPktCrDtimes == null) {
            return StaleCheckResult.NOT_STALE;
        }
        try {
            List<String> pathSegments = new ArrayList<>();
            pathSegments.add(uin);
            ResponseWrapper<ResponseDTO> response = (ResponseWrapper<ResponseDTO>)
                    registrationProcessorRestClientService.getApi(
                            ApiName.IDREPOGETIDBYUIN, pathSegments, "type", "demo", ResponseWrapper.class);
            if (response == null) {
                regProcLogger.warn("SESSIONID", "REGISTRATIONID", currentRegId,
                        "StaleReprocessChecker :: null response from ID Repo (URL not configured?) — result UNAVAILABLE.");
                return StaleCheckResult.UNAVAILABLE;
            }
            if (response.getResponse() == null) {
                // ID Repo returned an error response — identity not yet committed (e.g. NEW packet with allocated-but-unpublished UIN)
                regProcLogger.info("SESSIONID", "REGISTRATIONID", currentRegId,
                        "StaleReprocessChecker :: no committed identity found for UIN — result NOT_STALE.");
                return StaleCheckResult.NOT_STALE;
            }
            ResponseDTO idRepoResponse = objectMapper.convertValue(response.getResponse(), ResponseDTO.class);
            if (idRepoResponse == null || StringUtils.isEmpty(idRepoResponse.getEntity())) {
                return StaleCheckResult.NOT_STALE;
            }
            String lastCommittedRegId = idRepoResponse.getEntity();
            if (lastCommittedRegId.equals(currentRegId)) {
                return StaleCheckResult.NOT_STALE;
            }
            List<LocalDateTime> times = registrationRepositary.findPktCrDtimesByRegId(
                    lastCommittedRegId, RegistrationStatusCode.PROCESSED.toString());
            if (times == null || times.isEmpty()) {
                return StaleCheckResult.NOT_STALE;
            }
            return times.get(0).isAfter(currentPktCrDtimes) ? StaleCheckResult.STALE : StaleCheckResult.NOT_STALE;
        } catch (Exception e) {
            regProcLogger.warn("SESSIONID", "REGISTRATIONID", currentRegId,
                    "StaleReprocessChecker :: checkStaleReprocess failed — result UNAVAILABLE: " + e.getMessage());
            return StaleCheckResult.UNAVAILABLE;
        }
    }
}
