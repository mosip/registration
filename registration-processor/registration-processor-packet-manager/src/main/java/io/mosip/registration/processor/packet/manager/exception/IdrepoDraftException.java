package io.mosip.registration.processor.packet.manager.exception;

import io.mosip.kernel.core.exception.BaseCheckedException;

public class IdrepoDraftException extends BaseCheckedException {

    public IdrepoDraftException(String errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }
}
