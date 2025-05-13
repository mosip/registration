package io.mosip.registration.processor.core.exception;

public class PacketManagerNonRecoverableException extends PacketManagerException {

    public PacketManagerNonRecoverableException(String errorCode, String message) {
        super(errorCode, message);
    }

    public PacketManagerNonRecoverableException(String errorCode, String message, Throwable t) {
        super(errorCode, message, t);
    }
}