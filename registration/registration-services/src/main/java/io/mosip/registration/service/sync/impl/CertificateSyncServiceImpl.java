package io.mosip.registration.service.sync.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

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

    @Value("${mosip.registration.mdm.trust.domain.rcapture:DEVICE}")
    private String rCaptureTrustDomain;

    @Value("${mosip.registration.mdm.trust.domain.digitalId:FTM}")
    private String digitalIdTrustDomain;

    @Value("${mosip.registration.mdm.trust.domain.deviceinfo:DEVICE}")
    private String deviceInfoTrustDomain;

    private static ObjectMapper mapper = new ObjectMapper();


    static  {
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

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

        LOGGER.info("Network available cacerts sync started");
        try {
            LinkedHashMap<String, Object> certResponse = (LinkedHashMap<String, Object>) serviceDelegateUtil
                    .get(GET_CA_CERTIFICATE, requestParamMap, false, triggerPoint);

            if (null == certResponse.get(RegistrationConstants.RESPONSE))
                return getHttpResponseErrors(responseDTO, certResponse);

            LinkedHashMap<String, Object> responseMap = (LinkedHashMap<String, Object>) certResponse
                    .get(RegistrationConstants.RESPONSE);

           if(null == responseMap.get(CERT_LIST)) { return responseDTO; }

            List<CaCertificateDto> certs = mapper.convertValue(responseMap.get(CERT_LIST), new TypeReference<List<CaCertificateDto>>() {});

           //Data Fix : As createdDateTime is null sometimes
            certs.forEach(c -> {
                if(c.getCreatedtimes() == null)
                    c.setCreatedtimes(LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC));
            });
            certs.sort((CaCertificateDto d1, CaCertificateDto d2) -> d1.getCreatedtimes().compareTo(d2.getCreatedtimes()));

            List<String> trustedDomains = new ArrayList<>();
            trustedDomains.add(rCaptureTrustDomain);
            trustedDomains.add(deviceInfoTrustDomain);
            trustedDomains.add(digitalIdTrustDomain);

            for(CaCertificateDto cert : certs) {
                if(trustedDomains.contains(cert.getPartnerDomain().toUpperCase())) {
                    CACertificateRequestDto caCertificateRequestDto = new CACertificateRequestDto();
                    caCertificateRequestDto.setCertificateData(cert.getCertData());
                    caCertificateRequestDto.setPartnerDomain(cert.getPartnerDomain());
                    CACertificateResponseDto caCertificateResponseDto = partnerCertificateManagerService.uploadCACertificate(caCertificateRequestDto);
                    LOGGER.debug(caCertificateResponseDto.getStatus());
                }
            }
            return saveLastSuccessfulSyncTime(responseDTO, triggerPoint,
                    responseMap.get(LAST_SYNC_TIME) == null ? null : responseMap.get(LAST_SYNC_TIME).toString());

        } catch (Throwable t) {
            LOGGER.error("", t);
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
        LOGGER.info("Saved CA certificate lastSyncTime completed successfully.");
        return responseDTO;
    }
}
