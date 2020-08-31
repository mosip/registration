package io.mosip.registration.processor.core.exception;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;

public class ObjectStoreNotAccessibleException extends BaseCheckedException {

    public ObjectStoreNotAccessibleException(String message) {
        super(PlatformErrorMessages.OBJECT_STORE_NOT_ACCESSIBLE.getCode(), message);
    }

    public ObjectStoreNotAccessibleException(String message, Throwable t) {
        super(PlatformErrorMessages.OBJECT_STORE_NOT_ACCESSIBLE.getCode(), message, t);
    }

    public ObjectStoreNotAccessibleException(String code,String message, Throwable t) {
        super(code, message, t);
    }
}
