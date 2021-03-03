package io.mosip.registration.exception;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

public class PreConditionCheckException extends RegBaseCheckedException {

    private static final Logger LOGGER = AppConfig.getLogger(PreConditionCheckException.class);

    /**
     * Default constructor
     */
    public PreConditionCheckException() {
        super();
    }

    /**
     *
     * @param errorCode
     * @param errorMessage
     */
    public PreConditionCheckException(String errorCode, String errorMessage) {
        super(errorCode, errorMessage);
        LOGGER.error("REGISTRATION - CHECKED_EXCEPTION", APPLICATION_NAME,
                APPLICATION_ID, errorCode + "-->" + errorMessage);
    }
}
