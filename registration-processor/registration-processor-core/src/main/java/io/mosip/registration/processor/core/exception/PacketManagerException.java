package io.mosip.registration.processor.core.exception;

import io.mosip.kernel.core.exception.BaseCheckedException;

public class PacketManagerException extends BaseCheckedException {

    public PacketManagerException(String errorCode, String message) {
        super(errorCode, message);
    }

    public PacketManagerException(String errorCode, String message, Throwable t) {
        super(errorCode, message, t);
    }
}
