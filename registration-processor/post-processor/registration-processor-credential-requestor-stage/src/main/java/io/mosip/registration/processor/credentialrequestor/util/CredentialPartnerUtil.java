package io.mosip.registration.processor.credentialrequestor.util;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.credentialrequestor.dto.CredentialPartnersList;
import io.mosip.registration.processor.packet.storage.utils.Utilities;

@Component
public class CredentialPartnerUtil {

    private static final Logger regProcLogger = RegProcessorLogger.getLogger(CredentialPartnerUtil.class);

    private CredentialPartnersList credentialPartners;

    @Value("${mosip.registration.processor.credential.partner-profiles}")
    private String partnerProfileFileName;

    @Value("${config.server.file.storage.uri}")
    private String configServerFileStorageURL;

    @PostConstruct
    public void loadPartnerDetails() throws RegistrationProcessorCheckedException {
        try {
            String partners = Utilities.getJson(configServerFileStorageURL, partnerProfileFileName);
            credentialPartners = JsonUtil.readValueWithUnknownProperties(partners, CredentialPartnersList.class);
        } catch (Exception e) {
            regProcLogger.error("Error loading credential Partners", e);
            throw new RegistrationProcessorCheckedException(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getCode(),
                    PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage(), e);
        }
    }

    public CredentialPartnersList getAllCredentialPartners() throws RegistrationProcessorCheckedException {
        if (credentialPartners == null)
            loadPartnerDetails();
        return credentialPartners;
    }
}
