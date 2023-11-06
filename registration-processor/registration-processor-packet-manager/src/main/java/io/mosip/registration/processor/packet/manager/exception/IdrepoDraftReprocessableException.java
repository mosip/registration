package io.mosip.registration.processor.packet.manager.exception;

import io.mosip.kernel.core.exception.BaseCheckedException;

public class IdrepoDraftReprocessableException extends BaseCheckedException {

	public IdrepoDraftReprocessableException(String errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }
}
