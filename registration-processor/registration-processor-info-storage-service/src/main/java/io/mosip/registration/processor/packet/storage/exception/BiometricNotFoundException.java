package io.mosip.registration.processor.packet.storage.exception;

import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;

/**
 * The Class BiometricNotFoundException.
 */
public class BiometricNotFoundException extends BaseUncheckedException {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new Biometric not found exception.
     */
    public BiometricNotFoundException() {
        super();
    }

    /**
     * Instantiates a new Biometric not found exception.
     *
     * @param errorMessage the error message
     */
    public BiometricNotFoundException(String errorMessage) {
        super(PlatformErrorMessages.RPR_BIO_DEDUPE_NO_BIOMETRIC_FOUND.getCode()+ EMPTY_SPACE, errorMessage);
    }

    /**
     * Instantiates a new Biometric not found exception.
     *
     * @param message the message
     * @param cause the cause
     */
    public BiometricNotFoundException(String message, Throwable cause) {
        super(PlatformErrorMessages.RPR_BIO_DEDUPE_NO_BIOMETRIC_FOUND.getCode() + EMPTY_SPACE, message, cause);
    }
}