package io.mosip.registration.processor.packet.storage.exception;

import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;

/**
 * The Class PacketDateComputationException.
 */
public class PacketDateComputationException extends BaseUncheckedException {
    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new identity not found exception.
     */
    public PacketDateComputationException() {
        super();
    }

    /**
     * Instantiates a new identity not found exception.
     *
     * @param errorMessage the error message
     */
    public PacketDateComputationException(String errorMessage) {
        super(PlatformErrorMessages.RPR_PIS_IDENTITY_NOT_FOUND.getCode()+ EMPTY_SPACE, errorMessage);
    }

    /**
     * Instantiates a new identity not found exception.
     *
     * @param message the message
     * @param cause the cause
     */
    public PacketDateComputationException(String message, Throwable cause) {
        super(PlatformErrorMessages.RPR_PIS_IDENTITY_NOT_FOUND.getCode() + EMPTY_SPACE, message, cause);
    }
}
