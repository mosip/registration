package io.mosip.registration.processor.print.service.exception;

import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;

public class PDFSignatureException extends BaseUncheckedException{


	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new file not found in destination exception.
	 */
	public PDFSignatureException() {
		super();

	}

	/**
	 * Instantiates a new file not found in destination exception.
	 *
	 * @param errorMessage the error message
	 */
	public PDFSignatureException(String errorMessage) {
		super(PlatformErrorMessages.RPR_PRT_PDF_SIGNATURE_EXCEPTION.getCode(), errorMessage);
	}

	/**
	 * Instantiates a new file not found in destination exception.
	 *
	 * @param message the message
	 * @param cause the cause
	 */
	public PDFSignatureException(String message, Throwable cause) {
		super(PlatformErrorMessages.RPR_PRT_PDF_SIGNATURE_EXCEPTION.getCode() + EMPTY_SPACE, message, cause);

	}
}