package io.mosip.registration.service.sync.impl;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.keymanagerservice.repository.CACertificateStoreRepository;
import io.mosip.kernel.partnercertservice.dto.CACertificateRequestDto;
import io.mosip.kernel.partnercertservice.dto.CACertificateResponseDto;
import io.mosip.kernel.partnercertservice.service.spi.PartnerCertificateManagerService;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dao.MasterSyncDao;
import io.mosip.registration.dto.CaCertificateDto;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.entity.SyncControl;
import io.mosip.registration.entity.SyncTransaction;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.jobs.SyncManager;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.sync.CertificateSyncService;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.mosip.registration.constants.LoggerConstants.LOG_REG_MASTER_SYNC;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

@Service
public class CertificateSyncServiceImpl extends BaseService implements CertificateSyncService {

    private static final Logger LOGGER = AppConfig.getLogger(CertificateSyncServiceImpl.class);
    private static final String GET_CA_CERTIFICATE = "cacerts";
    private static final String LAST_UPDATED = "lastupdated";
    private static final String LAST_SYNC_TIME = "lastSyncTime";
    private static final String CERT_LIST = "certificateDTOList";

    @Autowired
    private ServiceDelegateUtil serviceDelegateUtil;

    @Autowired
    private CACertificateStoreRepository certificateStoreRepository;

    @Autowired
    private MasterSyncDao masterSyncDao;

    @Autowired
    private SyncManager syncManager;

    @Autowired
    private PartnerCertificateManagerService partnerCertificateManagerService;

    @Override
    public ResponseDTO getCACertificates(String triggerPoint) {
        ResponseDTO responseDTO = new ResponseDTO();
        Map<String, String> requestParamMap = new LinkedHashMap<>();
        SyncControl syncControl = masterSyncDao.syncJobDetails(RegistrationConstants.OPT_TO_REG_CCS_J00017);
        if (syncControl != null) {
            requestParamMap.put(LAST_UPDATED, DateUtils.formatToISOString(
                    LocalDateTime.ofInstant(syncControl.getLastSyncDtimes().toInstant(), ZoneOffset.ofHours(0))));
        }

        if(!RegistrationAppHealthCheckUtil.isNetworkAvailable())
            return setErrorResponse(responseDTO, RegistrationConstants.NO_INTERNET, null);

        LOGGER.info("", RegistrationConstants.APPLICATION_NAME,
                RegistrationConstants.APPLICATION_ID, "Network available cacerts sync started");
        try {
            LinkedHashMap<String, Object> certResponse = (LinkedHashMap<String, Object>) serviceDelegateUtil
                    .get(GET_CA_CERTIFICATE, requestParamMap, false, triggerPoint);

            if (null == certResponse.get(RegistrationConstants.RESPONSE))
                return getHttpResponseErrors(responseDTO, certResponse);

            LinkedHashMap<String, Object> responseMap = (LinkedHashMap<String, Object>) certResponse
                    .get(RegistrationConstants.RESPONSE);

            List<CaCertificateDto> certs = (List<CaCertificateDto>) responseMap.get(CERT_LIST);
            if(null == certs) { return responseDTO; }

            String lastSyncTime = responseMap.get(LAST_SYNC_TIME).toString();
            certs.sort((CaCertificateDto d1, CaCertificateDto d2) -> d1.getCreatedtimes().compareTo(d2.getCreatedtimes()));
            for(CaCertificateDto cert : certs) {
                CACertificateRequestDto caCertificateRequestDto = new CACertificateRequestDto();
                caCertificateRequestDto.setCertificateData(cert.getCertData());
                caCertificateRequestDto.setPartnerDomain(cert.getPartnerDomain());
                CACertificateResponseDto caCertificateResponseDto = partnerCertificateManagerService.uploadCACertificate(caCertificateRequestDto);
                LOGGER.debug("", RegistrationConstants.APPLICATION_NAME,
                        RegistrationConstants.APPLICATION_ID,  caCertificateResponseDto.getStatus());
            }
            return saveLastSuccessfulSyncTime(responseDTO, triggerPoint, lastSyncTime);

        } catch (Throwable t) {
            LOGGER.error("", RegistrationConstants.APPLICATION_NAME,
                    RegistrationConstants.APPLICATION_ID, ExceptionUtils.getStackTrace(t));
        }
        return responseDTO;
    }

    private ResponseDTO saveLastSuccessfulSyncTime(ResponseDTO responseDTO, String triggerPoint, String lastSyncTime)
            throws RegBaseCheckedException {
        responseDTO = setSuccessResponse(responseDTO, RegistrationConstants.MASTER_SYNC_SUCCESS, null);
        SyncTransaction syncTransaction = syncManager.createSyncTransaction(
                RegistrationConstants.JOB_EXECUTION_SUCCESS, RegistrationConstants.JOB_EXECUTION_SUCCESS,
                triggerPoint, RegistrationConstants.OPT_TO_REG_CCS_J00017);
        syncManager.updateClientSettingLastSyncTime(syncTransaction, getTimestamp(lastSyncTime));
        LOGGER.info("", APPLICATION_NAME, APPLICATION_ID,
                "Saved CA certificate lastSyncTime completed successfully.");
        return responseDTO;
    }
}
