package io.mosip.registration.processor.stages.uingenerator.exception;

import io.mosip.kernel.core.exception.BaseCheckedException;

public class IdrepoDraftException extends BaseCheckedException {

    public IdrepoDraftException(String errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }
}
