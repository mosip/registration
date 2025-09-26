package io.mosip.registration.processor.core.exception.util;

/**
 *
 * @author M1048399 Horteppa
 * 
 */
public enum PlatformSuccessMessages {

	// Packet Receiver Success messages.

	/** The rpr pkr packet receiver. */
	PACKET_RECEIVER_VALIDATION_SUCCESS(PlatformConstants.RPR_PACKET_RECEIVER_MODULE + "001",
			"Packet receiver validation success"),

	/** The rpr pkr packet receiver. */
	RPR_PKR_PACKET_RECEIVER(PlatformConstants.RPR_PACKET_RECEIVER_MODULE + "000",
			"Packet received and uploaded to landing zone"),

	// securezone notification success messages
	/** The rpr sez notification. */
	RPR_SEZ_SECUREZONE_NOTIFICATION(PlatformConstants.RPR_SECUREZONE_NOTIFICATION_MODULE + "000", "Securezone Notification Success"),

	RPR_PUM_PACKET_UPLOADER(PlatformConstants.RPR_PACKET_UPLOADER_MODULE + "000", "Packet uploaded to file system"),

	RPR_PUM_PACKET_ARCHIVED(PlatformConstants.RPR_PACKET_UPLOADER_MODULE + "001", "Packet successfully archived"),

	RPR_PUM_PACKET_UPLOADER_ALREADY_UPLOADED(PlatformConstants.RPR_PACKET_UPLOADER_MODULE + "002", "Packet already present in object store"),

	// Packet Validator Success messages
	/** The rpr pkr packet validate. */
	RPR_PKR_PACKET_VALIDATE(PlatformConstants.RPR_PACKET_VALIDATOR_MODULE + "000", "Packet Validation Success"),
	/** The reverse data sync success. */
	REVERSE_DATA_SYNC_SUCCESS(PlatformConstants.RPR_PACKET_VALIDATOR_MODULE + "017", "Reverse Data Sync Success"),
	// Packet Classifier Success messages
	/** The rpr pkr packet classifier. */
	RPR_PKR_PACKET_CLASSIFIER(PlatformConstants.RPR_PACKET_CLASSIFIER_MODULE + "000", "Packet Classifier Success"),

	RPR_PKR_ADDITIONAL_INFO_DELETED(PlatformConstants.RPR_PACKET_VALIDATOR_MODULE + "000", "Deleted additionalInfo from RegistrationList"),

	RPR_PKR_OPERATOR_VALIDATE(PlatformConstants.RPR_OVM_VALIDATOR_MODULE + "000", "OPERATOR Validation Success"),

	RPR_PKR_SUPERVISOR_VALIDATE(PlatformConstants.RPR_SVM_VALIDATOR_MODULE + "000", "SUPERVISOR Validation Success"),

	RPR_PKR_INTRODUCER_VALIDATE(PlatformConstants.RPR_IVM_VALIDATOR_MODULE + "000", "INTRODUCER Validation Success"),

	// CMD validator Success messages
	RPR_PKR_CMD_VALIDATE(PlatformConstants.RPR_CMD_VALIDATOR_MODULE + "000", "CMD Validation Success"),

	// DEMO-De-dupe Success Messages
	/** The rpr pkr demode-dupe validate. */
	RPR_PKR_DEMO_DE_DUP(PlatformConstants.RPR_DEMO_DEDUPE_MODULE + "000", "Demo-de-dupe Success"),

	RPR_PKR_DEMO_DE_DUP_POTENTIAL_DUPLICATION_FOUND(PlatformConstants.RPR_DEMO_DEDUPE_MODULE + "001",
			"Potential duplicate packet found for registration id : "),

	RPR_PKR_DEMO_DE_DUP_SKIP(PlatformConstants.RPR_DEMO_DEDUPE_MODULE + "002", "Demographic Deduplication Skipped"),

	// Biometric Authentication Success Messages
	RPR_PKR_BIOMETRIC_AUTHENTICATION(PlatformConstants.RPR_BIOMETRIC_AUTHENTICATION_MODULE + "000",
			"Biometric Authentication Success"),
	
	

	// Bio-De-dupe Success messages
	/** The Constant PACKET_BIODEDUPE_SUCCESS. */
	RPR_BIO_DEDUPE_SUCCESS(PlatformConstants.RPR_BIO_DEDUPE_STAGE_MODULE + "000", "Packet biodedupe successful"),

	/** The Constant PACKET_BIOMETRIC_POTENTIAL_MATCH. */
	RPR_BIO_METRIC_POTENTIAL_MATCH(PlatformConstants.RPR_BIO_DEDUPE_STAGE_MODULE + "000",
			"Potential match found while processing bio dedupe"),

	RPR_BIO_LOST_PACKET_UNIQUE_MATCH_FOUND(PlatformConstants.RPR_BIO_DEDUPE_STAGE_MODULE + "001",
			"Unique Match was Found for the Biometrics Received"),

	RPR_RE_PROCESS_SUCCESS(PlatformConstants.RPR_REPROCESSOR_VERTICLE + "000", "Reprocessor Success"),

	RPR_RE_PROCESS_FAILED(PlatformConstants.RPR_REPROCESSOR_VERTICLE + "002", "Reprocessor FAILED"),

	RPR_SENT_TO_REPROCESS_SUCCESS(PlatformConstants.RPR_REPROCESSOR_VERTICLE + "001", "sent to reprocess Success"),

	RPR_SENT_TO_REPROCESS_RESTART_FROM_STAGE_SUCCESS(PlatformConstants.RPR_REPROCESSOR_VERTICLE + "002",
			"sent to reprocess restart from stage Success"),

	RPR_WORKFLOW_INTERNAL_ACTION_SUCCESS(PlatformConstants.RPR_WORKFLOW_INTERNAL_ACTION + "000",
			"Workflow internal action completed successfully"),

	RPR_WORKFLOW_ACTION_SERVICE_SUCCESS(PlatformConstants.RPR_WORKFLOW_ACTION_SERVICE + "000",
			"Processed the workflow action - %s"),

    RPR_WORKFLOW_INSTANCE_SERVICE_SUCCESS(PlatformConstants.RPR_WORKFLOW_INSTANCE_SERVICE + "000",
            "Processed the workflow instance"),

	RPR_WORKFLOW_ACTION_API_SUCCESS(PlatformConstants.RPR_WORKFLOW_ACTION_API + "000",
			"Process the workflow action success"),

    RPR_WORKFLOW_INSTANCE_API_SUCCESS(PlatformConstants.RPR_WORKFLOW_INSTANCE_API + "000",
            "Process the workflow instance success"),

	RPR_WORKFLOW_SEARCH_API_SUCCESS(PlatformConstants.RPR_WORKFLOW_SEARCH_API + "000",
			"Process the workflow search success"),
	RPR_WORKFLOW_ACTION_JOB_SUCCESS(PlatformConstants.RPR_WORKFLOW_ACTION_JOB + "000", "Workflow action job success"),

	RPR_EXTERNAL_STAGE_SUCCESS(PlatformConstants.RPR_EXTERNAL_STAGE + "000", "External stage  Success"),

	RPR_UIN_GENERATOR_STAGE_SUCCESS(PlatformConstants.RPR_UIN_GENERATOR_STAGE + "000", "UIN Generator  Success"),

	RPR_BIOMETRIC_EXTRACTION_SUCCESS(PlatformConstants.RPR_BIOMETRIC_EXTRACTION_STAGE + "000",
			"biometric extraction success"),

	RPR_FINALIZATION_SUCCESS(PlatformConstants.RPR_FINALIZATION_STAGE + "000",
			"Finalization success"),

	RPR_UIN_DATA_UPDATION_SUCCESS(PlatformConstants.RPR_UIN_GENERATOR_STAGE + "001", "UIN Generator  Success"),

	RPR_UIN_ACTIVATED_SUCCESS(PlatformConstants.RPR_UIN_GENERATOR_STAGE + "002", "UIN Generator  Success"),

	RPR_UIN_DEACTIVATION_SUCCESS(PlatformConstants.RPR_UIN_GENERATOR_STAGE + "003", "UIN Generator  Success"),

	RPR_LINK_RID_FOR_LOST_PACKET_SUCCESS(PlatformConstants.RPR_UIN_GENERATOR_STAGE + "004", "UIN Generator  Success"),

	RPR_QUALITY_CHECK_SUCCESS(PlatformConstants.RPR_QUALITY_CHECKER_MODULE + "000", "Quality check  Success"),

	RPR_PRINT_STAGE_REQUEST_SUCCESS(PlatformConstants.RPR_PRINTING_MODULE + "000",
			"Print request submitted"),

	RPR_PRINT_STAGE_SUCCESS(PlatformConstants.RPR_PRINTING_MODULE + "001", "Print and Post Completed"),

	RPR_MESSAGE_SENDER_STAGE_SUCCESS(PlatformConstants.RPR_MESSAGE_SENDER_TEMPLATE + "001",
			"Message Sender Stage success"),

	RPR_ABIS_HANDLER_STAGE_SUCCESS(PlatformConstants.RPR_ABIS_HANDLER + "000", "ABIS hanlder stage success"),

	RPR_ABIS_MIDDLEWARE_STAGE_SUCCESS(PlatformConstants.RPR_ABIS_MIDDLEWARE + "000",
			"Abis insertRequests sucessfully sent to Queue"),

	RPR_MANUAL_VERIFICATION_APPROVED(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "000",
			"Manual verification approved"),

	RPR_MANUAL_VERIFICATION_RESEND(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "002",
			"Manual verification resend"),

	RPR_MANUAL_VERIFICATION_SENT(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "001",
			"Manual verification Sent to queue"),

	RPR_VERIFICATION_SENT(PlatformConstants.RPR_VERIFICATION_MODULE + "000",
			"Sent for Verification"),

	RPR_VERIFICATION_SUCCESS(PlatformConstants.RPR_VERIFICATION_MODULE + "001",
			"Verification successful"),

	RPR_DECRYPTION_SUCCESS(PlatformConstants.RPR_PACKET_DECRYPTION_MODULE + "000", "Decryption success"),
	
	RPR_ENCRYPTION_SUCCESS(PlatformConstants.RPR_PACKET_DECRYPTION_MODULE + "000", "Encryption success"),

	RPR_PRINT_SERVICE_SUCCESS(PlatformConstants.RPR_PRINTING_MODULE + "002", "Pdf generated and sent to print stage"),

	RPR_SYNC_REGISTRATION_SERVICE_SUCCESS(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "000", "SYNC successfull"),

	RPR_REQUEST_HANDLER_LOST_PACKET_SUCCESS(PlatformConstants.RPR_PACKET_REQUEST_HANDLER_MODULE + "000",
			"Lost packet id value fetched successfully"),

	PACKET_MARK_AS_PAUSED(PlatformConstants.RPR_CAMEL_BRIDGE_MODULE + "000",
			"Packet paused because of pause settings match"),
	PACKET_COMPLETE_AS_PROCESSED(PlatformConstants.RPR_CAMEL_BRIDGE_MODULE + "001",
			"Packet processing completed with processed status"),
	PACKET_COMPLETE_AS_REJECTED(PlatformConstants.RPR_CAMEL_BRIDGE_MODULE + "002",
			"Packet processing completed with reject status"),
	PACKET_COMPLETE_AS_FAILED(PlatformConstants.RPR_CAMEL_BRIDGE_MODULE + "003",
			"Packet processing completed with failed status"),
	PACKET_MARK_AS_REPROCESS(PlatformConstants.RPR_CAMEL_BRIDGE_MODULE + "004", "Packet marked for reprocessing"),
	PACKET_PROCESSING_COMPLETED(PlatformConstants.RPR_CAMEL_BRIDGE_MODULE + "009",
			"Packet processing completed with action code : "),

	PAUSE_AND_REQUEST_ADDITIONAL_INFO(PlatformConstants.RPR_CAMEL_BRIDGE_MODULE + "005",
			"packet paused and request additional info"),

	PACKET_RESTART_PARENT_FLOW(PlatformConstants.RPR_CAMEL_BRIDGE_MODULE + "006",
			"Packet parent flow restart initiated"),

	PACKET_COMPLETE_AS_REJECTED_WITHOUT_PARENT_FLOW(PlatformConstants.RPR_CAMEL_BRIDGE_MODULE + "007",
			"Packet processing completed with reject status without Parent flow"),

	PACKET_ANONYMOUS_PROFILE(PlatformConstants.RPR_CAMEL_BRIDGE_MODULE + "008",
			"Packet anonymous profile flow initiated");

	/** The success message. */
	private final String successMessage;

	/** The success code. */
	private final String successCode;

	/**
	 * Instantiates a new platform success messages.
	 *
	 * @param errorCode
	 *            the error code
	 * @param errorMsg
	 *            the error msg
	 */
	private PlatformSuccessMessages(String errorCode, String errorMsg) {
		this.successCode = errorCode;
		this.successMessage = errorMsg;
	}

	/**
	 * Gets the message.
	 *
	 * @return the message
	 */
	public String getMessage() {
		return this.successMessage;
	}

	/**
	 * Gets the code.
	 *
	 * @return the code
	 */
	public String getCode() {
		return this.successCode;
	}

}
