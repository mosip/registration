package io.mosip.registration.processor.core.code;

// TODO: Auto-generated Javadoc
/**
 * The Enum ModuleName.
 *
 * @author M1048399 Horteppa
 */
public enum ModuleName {

	PACKET_RECEIVER,

	SECUREZONE_NOTIFICATION,

	PACKET_UPLOAD,

	/** The packet validator. */
	PACKET_VALIDATOR,

	/** The packet validator. */
	PACKET_CLASSIFIER,

	/** The osi validator. */
	OSI_VALIDATOR,
	
	CMD_VALIDATOR,
	
	OPERATOR_VALIDATOR,
	
	SUPERVISOR_VALIDATOR,
	
	INTRODUCER_VALIDATOR,

	/** The demo dedupe. */
	DEMO_DEDUPE,

	/** The bio dedupe. */
	BIO_DEDUPE,

	/** The re processor. */
	RE_PROCESSOR,

	/** The biometric authentication. */
	BIOMETRIC_AUTHENTICATION,

	EXTERNAL,

	UIN_GENERATOR,
	
	BIOMETRIC_EXTRACTION,

	PRINT_STAGE,

	MESSAGE_SENDER,

	ABIS_HANDLER,

	MANUAL_VERIFICATION,

	DECRYPTOR,
	
	ENCRYPTOR,

	PRINT_SERVICE,

	SYNC_REGISTRATION_SERVICE,

	QUALITY_CLASSIFIER,

	ABIS_MIDDLEWARE,

	REQUEST_HANDLER_SERVICE,

	WORKFLOW_INTERNAL_ACTION,

	WORKFLOW_ACTION_SERVICE,

	WORKFLOW_ACTION_API,

	WORKFLOW_ACTION_JOB;
}
