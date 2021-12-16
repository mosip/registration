package io.mosip.registration.processor.core.util.exception;

import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;

public class LanguagesUtilException extends BaseUncheckedException {

    /**
     * The Constant serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new field not found exception.
     */
    public LanguagesUtilException() {
        super();
    }

    /**
     * Instantiates a new field not found exception.
     *
     * @param errorMessage the error message
     */
    public LanguagesUtilException(String errorMessage) {
        super(PlatformErrorMessages.RPR_UTL_LANGUAGE_EXCEPTION.getCode() + EMPTY_SPACE, errorMessage);
    }

    /**
     * Instantiates a new field not found exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public LanguagesUtilException(String message, Throwable cause) {
        super(PlatformErrorMessages.RPR_UTL_LANGUAGE_EXCEPTION.getCode() + EMPTY_SPACE, message, cause);
    }
}
