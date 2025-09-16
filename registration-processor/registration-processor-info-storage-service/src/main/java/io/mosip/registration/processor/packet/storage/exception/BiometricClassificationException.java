package io.mosip.registration.processor.packet.storage.exception;

import io.mosip.kernel.core.exception.BaseUncheckedException;

/**
 * The Class BiometricClassificationException.
 */
public class BiometricClassificationException extends BaseUncheckedException {

    public BiometricClassificationException(String message) {
        super(message);
    }

    public BiometricClassificationException(String message, Throwable cause) {
        super(message, String.valueOf(cause));
    }
}
