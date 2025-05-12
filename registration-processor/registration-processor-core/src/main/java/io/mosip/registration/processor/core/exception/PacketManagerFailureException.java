package io.mosip.registration.processor.core.exception;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.BaseUncheckedException;

public class PacketManagerFailureException extends PacketManagerException {

    public PacketManagerFailureException(String errorCode, String message) {
        super(errorCode, message);
    }

    public PacketManagerFailureException(String errorCode, String message, Throwable t) {
        super(errorCode, message, t);
    }
}