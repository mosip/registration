package io.mosip.registration.processor.packet.storage.exception;

import io.mosip.kernel.core.exception.BaseUncheckedException;

/**
 * The Class BiometricClassificationException.
 */
public class BiometricClassificationException extends BaseUncheckedException {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new Biometric not found exception.
     *
     * @param errorMessage the message
     * @param errorCode the code
     */

    public BiometricClassificationException(String errorCode, String errorMessage) {
        super(errorCode+ EMPTY_SPACE, errorMessage);
    }
}
