package io.mosip.registration.processor.core.status.util;

/**
 * 
 * @author Girish Yarru
 *
 */
public final class StatusConstants {
	private static final String RPR_REGISTRATION_PROCESSOR_PREFIX = "RPR-";
	private static final String SUCCESS = "SUCCESS-";
	private static final String FAILED = "FAILED-";
	private static final String SKIPPED = "SKIPPED-";
	private static final String SYSTEM_EXCEPTION = "SYS-EXCEPTION-001";
	public static final String SYSTEM_EXCEPTION_MESSAGE = "System Exception Occurred - Unable to Process Packet";

	// packet Receiver
	public static final String PACKET_RECEIVER_MODULE = RPR_REGISTRATION_PROCESSOR_PREFIX + "PKR-";
	public static final String PACKET_RECEIVER_MODULE_SUCCESS = PACKET_RECEIVER_MODULE + SUCCESS;
	public static final String PACKET_RECEIVER_MODULE_FAILURE = PACKET_RECEIVER_MODULE + FAILED;

	// securezone notification stage
	public static final String SECUREZONE_NOTIFICATION_MODULE = RPR_REGISTRATION_PROCESSOR_PREFIX + "SEN-";
	public static final String SECUREZONE_NOTIFICATION_SUCCESS = SECUREZONE_NOTIFICATION_MODULE + SUCCESS;
	public static final String SECUREZONE_NOTIFICATION_FAILED = SECUREZONE_NOTIFICATION_MODULE + FAILED;


	// packet Uploader Stage
	public static final String PACKET_UPLOADER_MODULE = RPR_REGISTRATION_PROCESSOR_PREFIX + "PKU-";
	public static final String PACKET_UPLOADER_MODULE_SUCCESS = PACKET_UPLOADER_MODULE + SUCCESS;
	public static final String PACKET_UPLOADER_MODULE_FAILED = PACKET_UPLOADER_MODULE + FAILED;

	// Quality checker stage
	public static final String QUALITY_CHECKER_MODULE = RPR_REGISTRATION_PROCESSOR_PREFIX + "QCK-";
	public static final String QUALITY_CHECKER_MODULE_SUCCESS = QUALITY_CHECKER_MODULE + SUCCESS;
	public static final String QUALITY_CHECKER_MODULE_FAILED = QUALITY_CHECKER_MODULE + FAILED;

	// packet validator stage
	public static final String PACKET_VALIDATOR_MODULE = RPR_REGISTRATION_PROCESSOR_PREFIX + "PKV-";
	public static final String PACKET_VALIDATOR_MODULE_SUCCESS = PACKET_VALIDATOR_MODULE + SUCCESS;
	public static final String PACKET_VALIDATOR_MODULE_FAILED = PACKET_VALIDATOR_MODULE + FAILED;

	// packet classifier stage
	public static final String PACKET_CLASSIFIER_MODULE = RPR_REGISTRATION_PROCESSOR_PREFIX + "PKC";
	public static final String PACKET_CLASSIFIER_MODULE_SUCCESS = PACKET_CLASSIFIER_MODULE + SUCCESS;
	public static final String PACKET_CLASSIFIER_MODULE_FAILED = PACKET_CLASSIFIER_MODULE + FAILED;

	// External stage
	public static final String EXTERNAL_SATGE_MODULE = RPR_REGISTRATION_PROCESSOR_PREFIX + "EXS-";
	public static final String EXTERNAL_SATGE_MODULE_SUCCESS = EXTERNAL_SATGE_MODULE + SUCCESS;
	public static final String EXTERNAL_SATGE_MODULE_FAILED = EXTERNAL_SATGE_MODULE + FAILED;

	// Operator Validator stage
	public static final String OVM_VALIDAOR_MODULE = RPR_REGISTRATION_PROCESSOR_PREFIX + "OVM-";
	public static final String OVM_VALIDAOR_MODULE_SUCCESS = OVM_VALIDAOR_MODULE + SUCCESS;
	public static final String OVM_VALIDAOR_MODULE_FAILED = OVM_VALIDAOR_MODULE + FAILED;
	
	// Supervisor Validator stage
	public static final String SVM_VALIDAOR_MODULE = RPR_REGISTRATION_PROCESSOR_PREFIX + "SVM-";
	public static final String SVM_VALIDAOR_MODULE_SUCCESS = SVM_VALIDAOR_MODULE + SUCCESS;
	public static final String SVM_VALIDAOR_MODULE_FAILED = SVM_VALIDAOR_MODULE + FAILED;
	
	// Introducer Validator stage
	public static final String IVM_VALIDAOR_MODULE = RPR_REGISTRATION_PROCESSOR_PREFIX + "IVM-";
	public static final String IVM_VALIDAOR_MODULE_SUCCESS = IVM_VALIDAOR_MODULE + SUCCESS;
	public static final String IVM_VALIDAOR_MODULE_FAILED = IVM_VALIDAOR_MODULE + FAILED;

	// CMD Validator stage
	public static final String CMD_VALIDAOR_MODULE = RPR_REGISTRATION_PROCESSOR_PREFIX + "CMD-";
	public static final String CMD_VALIDAOR_MODULE_SUCCESS = CMD_VALIDAOR_MODULE + SUCCESS;
	public static final String CMD_VALIDAOR_MODULE_FAILED = CMD_VALIDAOR_MODULE + FAILED;
	
	// Printing stage
	public static final String PRINT_STAGE_MODULE = RPR_REGISTRATION_PROCESSOR_PREFIX + "PPS-";
	public static final String PRINT_STAGE_MODULE_SUCCESS = PRINT_STAGE_MODULE + SUCCESS;
	public static final String PRINT_STAGE_MODULE_FAILED = PRINT_STAGE_MODULE + FAILED;

	// Abis middleware stage
	public static final String ABIS_MIDDLEWARE_MODULE = RPR_REGISTRATION_PROCESSOR_PREFIX + "AMW-";
	public static final String ABIS_MIDDLEWARE_MODULE_SUCCESS = ABIS_MIDDLEWARE_MODULE + SUCCESS;
	public static final String ABIS_MIDDLEWARE_MODULE_FAILED = ABIS_MIDDLEWARE_MODULE + FAILED;

	// Bio dedupe stage
	public static final String BIO_DEDUPE_MODULE = RPR_REGISTRATION_PROCESSOR_PREFIX + "BDP-";
	public static final String BIO_DEDUPE_MODULE_SUCCESS = BIO_DEDUPE_MODULE + SUCCESS;
	public static final String BIO_DEDUPE_MODULE_FAILED = BIO_DEDUPE_MODULE + FAILED;

	// Biometric authentication stage
	public static final String BIO_METRIC_AUTHENTICATION_MODULE = RPR_REGISTRATION_PROCESSOR_PREFIX + "BAU-";
	public static final String BIO_METRIC_AUTHENTICATION_MODULE_SUCCESS = BIO_METRIC_AUTHENTICATION_MODULE + SUCCESS;
	public static final String BIO_METRIC_AUTHENTICATION_MODULE_FAILED = BIO_METRIC_AUTHENTICATION_MODULE + FAILED;

	// Demodedupe stage
	public static final String DEMO_DEDUPE_MODULE = RPR_REGISTRATION_PROCESSOR_PREFIX + "DDP-";
	public static final String DEMO_DEDUPE_MODULE_SUCCESS = DEMO_DEDUPE_MODULE + SUCCESS;
	public static final String DEMO_DEDUPE_MODULE_FAILED = DEMO_DEDUPE_MODULE + FAILED;
	public static final String DEMO_DEDUPE_MODULE_SKIPPED = DEMO_DEDUPE_MODULE + SKIPPED;

	// Manual verification stage
	public static final String MANUAL_VERIFICATION_MODULE = RPR_REGISTRATION_PROCESSOR_PREFIX + "MNV-";
	public static final String MANUAL_VERIFICATION_MODULE_SUCCESS = MANUAL_VERIFICATION_MODULE + SUCCESS;
	public static final String MANUAL_VERIFICATION_MODULE_FAILED = MANUAL_VERIFICATION_MODULE + FAILED;

	// Uin generation stage
	public static final String UIN_GENERATOR_MODULE = RPR_REGISTRATION_PROCESSOR_PREFIX + "UIN-";
	public static final String UIN_GENERATOR_MODULE_SUCCESS = UIN_GENERATOR_MODULE + SUCCESS;
	public static final String UIN_GENERATOR_MODULE_FAILED = UIN_GENERATOR_MODULE + FAILED;
	
	// Biometric extraction stage
	public static final String BIOMETRIC_EXTRACTION_MODULE = RPR_REGISTRATION_PROCESSOR_PREFIX + "BEN-";
	public static final String BIOMETRIC_EXTRACTION_MODULE_SUCCESS = BIOMETRIC_EXTRACTION_MODULE + SUCCESS;
	public static final String BIOMETRIC_EXTRACTION_MODULE_FAILED = BIOMETRIC_EXTRACTION_MODULE + FAILED;
	
	// Biometric extraction stage
		public static final String FINALIZATION_MODULE = RPR_REGISTRATION_PROCESSOR_PREFIX + "FIN-";
		public static final String FINALIZATION_MODULE_SUCCESS = FINALIZATION_MODULE + SUCCESS;
		public static final String FINALIZATION_MODULE_FAILED = FINALIZATION_MODULE + FAILED;

	public static final String RE_PROCESS_MODULE = RPR_REGISTRATION_PROCESSOR_PREFIX + "RPS-";
	public static final String RE_PROCESS_MODULE_SUCCESS = RE_PROCESS_MODULE + SUCCESS;
	public static final String RE_PROCESS_MODULE_FAILED = RE_PROCESS_MODULE + FAILED;

	// System Exceptions
	public static final String SYSTEM_EXCEPTION_CODE = RPR_REGISTRATION_PROCESSOR_PREFIX + SYSTEM_EXCEPTION;

	// Message sender stage
	public static final String MESSAGE_SENDER_MODULE = RPR_REGISTRATION_PROCESSOR_PREFIX + "MSS-";
	public static final String MESSAGE_SENDER_NOTIF_SUCCESS_CODE = MESSAGE_SENDER_MODULE + SUCCESS;
	public static final String MESSAGE_SENDER__FAILED_CODE = MESSAGE_SENDER_MODULE + FAILED;
	
	//Request Handler
	public static final String REQUEST_HANDLER_MODULE = RPR_REGISTRATION_PROCESSOR_PREFIX + "RHM-";
	public static final String REQUEST_HANDLER_MODULE_SUCCESS = REQUEST_HANDLER_MODULE + SUCCESS;
	public static final String REQUEST_HANDLER_MODULE_FAILED = REQUEST_HANDLER_MODULE + FAILED;

	public static final String WORKFLOW_INTERNAL_ACTION = RPR_REGISTRATION_PROCESSOR_PREFIX + "WIA-";

	public static final String WORKFLOW_ACTION_SERVICE = RPR_REGISTRATION_PROCESSOR_PREFIX + "WAS-";


}
