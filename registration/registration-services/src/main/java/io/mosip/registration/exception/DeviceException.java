package io.mosip.registration.exception;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

public class DeviceException extends BaseCheckedException  {

    /**
     * Instance of {@link Logger}
     */
    private static final Logger LOGGER = AppConfig.getLogger(DeviceException.class);

    /**
     * Constructs a new checked exception
     */
    public DeviceException() {
        super();
    }

    /**
     * Constructs a new checked exception with the specified detail message and
     * error code.
     *
     * @param errorCode
     *            the error code
     * @param errorMessage
     *            the detail message.
     */
    public DeviceException(String errorCode, String errorMessage) {
        super(errorCode, errorMessage);
        LOGGER.error("REGISTRATION - CHECKED_EXCEPTION", APPLICATION_NAME,
                APPLICATION_ID, errorCode + "-->" + errorMessage);
    }

    /**
     * Constructs a new checked exception with the specified detail message and
     * error code.
     *
     * @param errorCode
     *            the error code
     * @param errorMessage
     *            the detail message
     * @param throwable
     *            the specified cause
     */
    public DeviceException(String errorCode, String errorMessage, Throwable throwable) {
        super(errorCode, errorMessage, throwable);
        LOGGER.error("REGISTRATION - CHECKED_EXCEPTION", APPLICATION_NAME, APPLICATION_ID,
                errorCode + "-->" + errorMessage + "-->" + ExceptionUtils.getStackTrace(throwable));
    }
}
