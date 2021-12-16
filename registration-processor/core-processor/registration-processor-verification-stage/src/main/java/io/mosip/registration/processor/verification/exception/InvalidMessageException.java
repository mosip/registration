package io.mosip.registration.processor.verification.exception;

import io.mosip.kernel.core.exception.BaseUncheckedException;

public class InvalidMessageException extends BaseUncheckedException {

    public InvalidMessageException(String code, String message) {
        super(code, message);
    }
}
