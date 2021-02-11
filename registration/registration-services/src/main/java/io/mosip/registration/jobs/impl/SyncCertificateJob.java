package io.mosip.registration.jobs.impl;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.jobs.BaseJob;
import io.mosip.registration.service.sync.CertificateSyncService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;


/**
 * Job to sync CA certificates used for device trust validation
 *
 * @author Anusha
 * @since 1.1.5
 */
@Component
public class SyncCertificateJob extends BaseJob {

    private static final Logger LOGGER = AppConfig.getLogger(SyncCertificateJob.class);

    @Autowired
    private CertificateSyncService certificateSyncService;


    @Override
    public ResponseDTO executeJob(String triggerPoint, String jobId) {
        LOGGER.info("", RegistrationConstants.APPLICATION_NAME,
                RegistrationConstants.APPLICATION_ID, "execute Job started");

        try {
            this.responseDTO = certificateSyncService.getCACertificates(triggerPoint);
        } catch (Throwable exception) {
            LOGGER.error("", APPLICATION_NAME, APPLICATION_ID, ExceptionUtils.getStackTrace(exception));
            this.responseDTO.setSuccessResponseDTO(null);
        }
        syncTransactionUpdate(responseDTO, triggerPoint, jobId);

        LOGGER.info("", RegistrationConstants.APPLICATION_NAME,
                RegistrationConstants.APPLICATION_ID, "execute job ended");

        return responseDTO;
    }

    @Async
    @Override
    protected void executeInternal(JobExecutionContext context) {
        LOGGER.info("", RegistrationConstants.APPLICATION_NAME,
                RegistrationConstants.APPLICATION_ID, "job execute internal started");
        this.responseDTO = new ResponseDTO();

        try {
            this.jobId = loadContext(context);
            certificateSyncService = applicationContext.getBean(CertificateSyncService.class);

            // Execute Parent Job
            this.responseDTO = executeParentJob(jobId);

            // Execute Current Job
            if (responseDTO.getSuccessResponseDTO() != null) {
                this.responseDTO = certificateSyncService.getCACertificates(triggerPoint);
            }

        } catch (Throwable t) {
            LOGGER.error("", RegistrationConstants.APPLICATION_NAME,
                    RegistrationConstants.APPLICATION_ID, ExceptionUtils.getStackTrace(t));
            responseDTO.setSuccessResponseDTO(null);
        }

        syncTransactionUpdate(responseDTO, triggerPoint, jobId);

        LOGGER.info("", RegistrationConstants.APPLICATION_NAME,
                RegistrationConstants.APPLICATION_ID, "job execute internal Ended");
    }
}
