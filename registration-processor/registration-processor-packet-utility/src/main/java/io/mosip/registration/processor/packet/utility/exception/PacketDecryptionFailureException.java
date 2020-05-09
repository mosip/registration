package io.mosip.registration.processor.packet.utility.exception;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.registration.processor.packet.utility.constants.PacketUtilityErrorCodes;

/**
 * PacketDecryptionFailureException class.
 *
 * @author Jyoti Prakash Nayak
 */
public class PacketDecryptionFailureException extends BaseCheckedException{

	/** Serializable version Id. */
	private static final long serialVersionUID = 1L;

	public PacketDecryptionFailureException() {
		super(PacketUtilityErrorCodes.PACKET_DECRYPTION_FAILURE_EXCEPTION.getErrorCode(),
				PacketUtilityErrorCodes.PACKET_DECRYPTION_FAILURE_EXCEPTION.getErrorMessage());
	}

	/**
	 * @param message
	 *            Message providing the specific context of the error.
	 * @param cause
	 *            Throwable cause for the specific exception
	 */
	public PacketDecryptionFailureException(String message, Throwable cause) {
		super(PacketUtilityErrorCodes.PACKET_DECRYPTION_FAILURE_EXCEPTION.getErrorCode(), message, cause);

	}

	public PacketDecryptionFailureException(String errorMessage) {
		super(PacketUtilityErrorCodes.PACKET_DECRYPTION_FAILURE_EXCEPTION.getErrorCode(), errorMessage);
	}

}