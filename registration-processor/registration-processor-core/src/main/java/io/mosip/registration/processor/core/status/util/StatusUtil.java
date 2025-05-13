package io.mosip.registration.processor.core.status.util;

import io.mosip.registration.processor.core.exception.util.PlatformConstants;

public enum StatusUtil {
	// Packet Receiver Stage
	PACKET_RECEIVED(StatusConstants.PACKET_RECEIVER_MODULE_SUCCESS + "001", "Packet has reached Packet Receiver"),
	PACKET_UPLOADED_TO_LANDING_ZONE(StatusConstants.PACKET_RECEIVER_MODULE_SUCCESS + "002",
			"Packet is Uploaded to Landing Zone"),
	VIRUS_SCANNER_FAILED(StatusConstants.PACKET_RECEIVER_MODULE_FAILURE + "001", "Packet is Virus Infected"),
	PACKET_DECRYPTION_FAILED(StatusConstants.PACKET_RECEIVER_MODULE_FAILURE + "002", "Packet Decryption Failed"),

	// securezone notification stage
	NOTIFICATION_RECEIVED_TO_SECUREZONE(StatusConstants.SECUREZONE_NOTIFICATION_SUCCESS + "001",
			"Notification received to securezone"),

	// Packet uploader stage
	PACKET_UPLOADED(StatusConstants.PACKET_UPLOADER_MODULE_SUCCESS + "001", "Packet is Uploaded to Packet Store"),
	PACKET_CLEANUP_FAILED(StatusConstants.PACKET_UPLOADER_MODULE_FAILED + "001",
			"Packet Clean Up Failed from Landing Zone"),
	PACKET_ARCHIVAL_FAILED(StatusConstants.PACKET_UPLOADER_MODULE_FAILED + "002", "Packet Archival Failed"),
	PACKET_UPLOAD_FAILED(StatusConstants.PACKET_UPLOADER_MODULE_FAILED + "003", "Packet Upload Failed"),
	PACKET_NOT_FOUND_LANDING_ZONE(StatusConstants.PACKET_UPLOADER_MODULE_FAILED + "004",
			"Packet Not Found in Landing Zone"),
	PACKET_HASHCODE_VALIDATION_FAILED(StatusConstants.PACKET_UPLOADER_MODULE_FAILED + "005",
			"Packet Hash Code Validation Failed"),
	VIRUS_SCANNER_FAILED_UPLOADER(StatusConstants.PACKET_UPLOADER_MODULE_FAILED + "006", "Packet is Virus Infected"),
	PACKET_UPLOAD_DECRYPTION_FAILED(StatusConstants.PACKET_UPLOADER_MODULE_FAILED + "007", "Packet Decryption Failed"),
	PACKET_ALREADY_UPLOADED(StatusConstants.PACKET_UPLOADER_MODULE_SUCCESS + "008",
			"Packet is already present in Packet Store"),
	PACKET_RETRY_CNT_EXCEEDED(StatusConstants.PACKET_UPLOADER_MODULE_FAILED + "009",
			"Retry count has exceeded the maximum limit specified"),

	// Quality checker stage
	INDIVIDUAL_BIOMETRIC_NOT_FOUND(StatusConstants.QUALITY_CHECKER_MODULE_SUCCESS + "001",
			"Individual Biometric Parameter Not Found in ID JSON so skipping biometric classification"),
	BIOMETRIC_QUALITY_CHECK_SUCCESS(StatusConstants.QUALITY_CHECKER_MODULE_SUCCESS + "002",
			"Biometric Quality Check is Successful"),
	BIOMETRIC_QUALITY_CHECK_FAILED(StatusConstants.QUALITY_CHECKER_MODULE_FAILED + "001",
			"Quality Score of Biometrics Captured is Below the Threshold"),

	// packet validator stage
	PACKET_STRUCTURAL_VALIDATION_SUCCESS(StatusConstants.PACKET_VALIDATOR_MODULE_SUCCESS + "001",
			"Packet Validation is Successful"),
	FILE_VALIDATION_FAILED(StatusConstants.PACKET_VALIDATOR_MODULE_FAILED + "001", "File Validation Failed"),
	SCHEMA_VALIDATION_FAILED(StatusConstants.PACKET_VALIDATOR_MODULE_FAILED + "002", "Schema Validation Failed"),
	CHECKSUM_VALIDATION_FAILED(StatusConstants.PACKET_VALIDATOR_MODULE_FAILED + "003", "Check Sum Validation Failed"),
	INDIVIDUAL_BIOMETRIC_VALIDATION_FAILED(StatusConstants.PACKET_VALIDATOR_MODULE_FAILED + "004",
			"Individual Biometric Validation Failed"),
	APPLICANT_DOCUMENT_VALIDATION_FAILED(StatusConstants.PACKET_VALIDATOR_MODULE_FAILED + "005",
			"Applicant Document Validation Failed"),
	MASTER_DATA_VALIDATION_FAILED(StatusConstants.PACKET_VALIDATOR_MODULE_FAILED + "006",
			"Master Data Validation Failed"),
	ACTIVATE_DEACTIVATE_FAILED(StatusConstants.PACKET_VALIDATOR_MODULE_FAILED + "007",
			"Packet Validation for Activate/Deactivate Packet Failed"),
	UIN_NOT_FOUND_IDREPO(StatusConstants.PACKET_VALIDATOR_MODULE_FAILED + "008", "UIN is Not Found in ID Repository"),
	MANDATORY_VALIDATION_FAILED(StatusConstants.PACKET_VALIDATOR_MODULE_FAILED + "009",
			"Mandatory Fields are Not Present in ID Object"),
	RID_AND_TYPE_SYNC_FAILED(StatusConstants.PACKET_VALIDATOR_MODULE_FAILED + "010",
			"RID & Type not matched from sync table"),
	PACKET_REJECTED(StatusConstants.PACKET_VALIDATOR_MODULE_FAILED + "011", "Rejected by Supervisor"),
	PACKET_MANAGER_VALIDATION_FAILURE(StatusConstants.PACKET_VALIDATOR_MODULE_FAILED + "012",
			"Packet validation failed in packet manager"),
	BIOMETRICS_VALIDATION_FAILURE(StatusConstants.PACKET_VALIDATOR_MODULE_FAILED + "013",
			"Biometric file validation failed"),
	PACKET_MANAGER_EXCEPTION(StatusConstants.PACKET_VALIDATOR_MODULE_FAILED + "014",
			"Exception occured in packet manager."),
	XSD_VALIDATION_EXCEPTION(StatusConstants.PACKET_VALIDATOR_MODULE_FAILED + "015", "XSD validation failed."),
	BIOMETRICS_SIGNATURE_VALIDATION_FAILURE(StatusConstants.PACKET_VALIDATOR_MODULE_FAILED + "016",
			"Biometric Signature validation failed"),
	PACKET_CONSENT_VALIDATION(StatusConstants.PACKET_VALIDATOR_MODULE_FAILED + "017",
			"Consent is not agreed for the packet to process further"),
	PACKET_MANAGER_NON_RECOVERABLE_EXCEPTION(StatusConstants.PACKET_VALIDATOR_MODULE_FAILED + "018",
			"Packet manager non recoverable exception"),

	// packet classifier stage
	PACKET_CLASSIFICATION_SUCCESS(StatusConstants.PACKET_CLASSIFIER_MODULE_SUCCESS + "001",
			"Packet Classification is Successful"),

	// External stage
	EXTERNAL_STAGE_SUCCESS(StatusConstants.EXTERNAL_SATGE_MODULE_SUCCESS + "001",
			"Packet processing in External stage is sucessful"),
	EXTERNAL_STAGE_FAILED(StatusConstants.EXTERNAL_SATGE_MODULE_SUCCESS + "001",
			"Packet processing in External stage failed"),

	// CMD Validator stage
	CMD_VALIDATION_SUCCESS(StatusConstants.CMD_VALIDAOR_MODULE_SUCCESS + "001", "CMD Validation is Successful"),
	GPS_DETAILS_NOT_FOUND(StatusConstants.CMD_VALIDAOR_MODULE_FAILED + "001", "GPS Details are Not Found in Packet"),
	CENTER_ID_NOT_FOUND(StatusConstants.CMD_VALIDAOR_MODULE_FAILED + "002", "Center ID Not Found in Master DB - "),
	CENTER_ID_INACTIVE(StatusConstants.CMD_VALIDAOR_MODULE_FAILED + "003",
			"Center was InActive during Packet Creation - "),
	MACHINE_ID_NOT_FOUND(StatusConstants.CMD_VALIDAOR_MODULE_FAILED + "004", "Machine ID Not Found in Master DB - "),
	MACHINE_ID_NOT_ACTIVE(StatusConstants.CMD_VALIDAOR_MODULE_FAILED + "005",
			"Machine ID was InActive during Packet Creation - "),
	CENTER_DEVICE_MAPPING_NOT_FOUND(StatusConstants.CMD_VALIDAOR_MODULE_FAILED + "006",
			"Center-Device Mapping Not Found - "),
	CENTER_DEVICE_MAPPING_INACTIVE(StatusConstants.CMD_VALIDAOR_MODULE_FAILED + "007",
			"Center-Device Mapping was InActive during Packet Creation - "),
	DEVICE_NOT_FOUND_MASTER_DB(StatusConstants.CMD_VALIDAOR_MODULE_FAILED + "008", "Device Not Found in Master DB - "),
	DEVICE_VALIDATION_FAILED(StatusConstants.CMD_VALIDAOR_MODULE_FAILED + "009", "Device Validation Failed"),
	PACKET_CREATION_WORKING_HOURS(StatusConstants.CMD_VALIDAOR_MODULE_FAILED + "010",
			"Packet was Not Created during Working Hours - "),
	REGISTRATION_CENTER_TIMESTAMP_FAILURE(StatusConstants.CMD_VALIDAOR_MODULE_FAILED + "011",
			"Registration Center timestamp failed"),
	FAILED_TO_GET_MACHINE_DETAIL(StatusConstants.CMD_VALIDAOR_MODULE_FAILED + "012",
			"Failed to Get machine id details "),
	FAILED_TO_GET_CENTER_DETAIL(StatusConstants.CMD_VALIDAOR_MODULE_FAILED + "013", "Failed to Get center id details "),
	VALIDATION_FAILED_EXCEPTION(StatusConstants.CMD_VALIDAOR_MODULE_FAILED + "014",
			"Exception occured due to validation failure."),
	MACHINE_ID_NOT_FOUND_MASTER_DB(StatusConstants.CMD_VALIDAOR_MODULE_FAILED + "015",
			"MachineId not found in master db - "),
	TIMESTAMP_NOT_VALID(StatusConstants.CMD_VALIDAOR_MODULE_FAILED + "016",
			"digital id timestamp is not within acctable time range of packet creation time"),
	DEVICE_HOTLISTED(StatusConstants.CMD_VALIDAOR_MODULE_FAILED + "017", "Device has been hot listed"),
	DEVICE_SIGNATURE_VALIDATION_FAILED(StatusConstants.CMD_VALIDAOR_MODULE_FAILED + "018",
			"DigitalId signature Validation Failed"),
	CMD_LANGUAGE_NOT_SET(StatusConstants.CMD_VALIDAOR_MODULE_FAILED + "019",
			"Mandatory/Optional Language not set for CMD validation."),

	// Operator Validator stage
	OPERATOR_VALIDATION_SUCCESS(StatusConstants.OVM_VALIDAOR_MODULE_SUCCESS + "001",
			"OPERATOR Validation is Successful"),
	OFFICER_NOT_ACTIVE(StatusConstants.OVM_VALIDAOR_MODULE_FAILED + "001", "OfficerId is inActive"),
	OPERATOR_PACKET_CREATION_DATE_NOT_FOUND_IN_PACKET(StatusConstants.OVM_VALIDAOR_MODULE_FAILED + "002",
			"Packet Creation Date is NULL"),
	OPERATOR_PASSWORD_OTP_FAILURE(StatusConstants.OVM_VALIDAOR_MODULE_FAILED + "003",
			"Password or OTP Verification Failed for Officer - "),
	OFFICER_WAS_INACTIVE(StatusConstants.OVM_VALIDAOR_MODULE_FAILED + "004",
			"Officer was Not Active during Packet Creation - "),
	OFFICER_NOT_FOUND_PACKET(StatusConstants.OVM_VALIDAOR_MODULE_FAILED + "005", "Officer ID is NULL"),

	// Supervisor Validator stage
	SUPERVISOR_VALIDATION_SUCCESS(StatusConstants.SVM_VALIDAOR_MODULE_SUCCESS + "001",
			"SUPERVISOR Validation is Successful"),
	SUPERVISOR_NOT_ACTIVE(StatusConstants.SVM_VALIDAOR_MODULE_FAILED + "001", "SupervisorId is inActive"),
	SUPERVISOR_PACKET_CREATION_DATE_NOT_FOUND_IN_PACKET(StatusConstants.SVM_VALIDAOR_MODULE_FAILED + "002",
			"Packet Creation Date is NULL"),
	SUPERVISOR_PASSWORD_OTP_FAILURE(StatusConstants.SVM_VALIDAOR_MODULE_FAILED + "003",
			"Password or OTP Verification Failed for Officer - "),
	SUPERVISOR_WAS_INACTIVE(StatusConstants.SVM_VALIDAOR_MODULE_FAILED + "004",
			"Supervisor was Not Active during Packet Creation - "),
	PASSWORD_OTP_FAILURE_SUPERVISOR(StatusConstants.SVM_VALIDAOR_MODULE_FAILED + "005",
			"Password or OTP Verification Failed for Supervisor - "),
	SUPERVISOR_NOT_FOUND_PACKET(StatusConstants.OVM_VALIDAOR_MODULE_FAILED + "006", "Supervisor ID is NULL"),

	// Introducer Validator stage
	INTRODUCER_VALIDATION_SUCCESS(StatusConstants.IVM_VALIDAOR_MODULE_SUCCESS + "001",
			"INTRODUCER Validation is Successful"),
	INTRODUCER_AUTHENTICATION_FAILED(StatusConstants.IVM_VALIDAOR_MODULE_FAILED + "001",
			"INTRODUCER Biometric Authentication Failed - "),
	UIN_RID_NOT_FOUND(StatusConstants.IVM_VALIDAOR_MODULE_FAILED + "002",
			"UIN or RID of Introducer Not Found in Packet"),
	INTRODUCER_UIN_NOT_FOUND(StatusConstants.IVM_VALIDAOR_MODULE_FAILED + "003",
			"Introducer UIN not Found for the Given RID"),
	INTRODUCER_BIOMETRIC_FILE_NAME_NOT_FOUND(StatusConstants.IVM_VALIDAOR_MODULE_FAILED + "004",
			"Introducer Biometric File Name Not Found"),
	PACKET_ON_HOLD(StatusConstants.IVM_VALIDAOR_MODULE_FAILED + "005",
			"Packet On-Hold as Introducer packet is not processed yet."),
	CHILD_PACKET_REJECTED(StatusConstants.IVM_VALIDAOR_MODULE_FAILED + "006",
			"Packet Rejected as Parent Packet is Rejected"),
	PACKET_IS_ON_HOLD(StatusConstants.IVM_VALIDAOR_MODULE_FAILED + "007",
			"Packet is on Hold due to parent packet processing"),
	INTRODUCER_BIOMETRIC_ALL_EXCEPTION_IN_PACKET(StatusConstants.IVM_VALIDAOR_MODULE_FAILED + "008",
			"Introducer Biometrics all exceptions"),
	// printing stage
	PRINT_REQUEST_SUCCESS(StatusConstants.PRINT_STAGE_MODULE_SUCCESS + "001", "Print request submitted"),
	PDF_ADDED_TO_QUEUE_FAILED(StatusConstants.PRINT_STAGE_MODULE_FAILED + "001",
			"PDF was not added to Queue due to Queue Failure"),
	PRINT_POST_COMPLETED(StatusConstants.PRINT_STAGE_MODULE_SUCCESS + "002", "Printing and Post Completed"),
	RESEND_UIN_CARD(StatusConstants.PRINT_STAGE_MODULE_SUCCESS + "003", "Re-Sent UIN Card for Printing"),
	PDF_GENERATION_FAILED(StatusConstants.PRINT_STAGE_MODULE_SUCCESS + "004", "Pdf Generation failed for "),
	TEMPLATE_PROCESSING_FAILED(StatusConstants.PRINT_STAGE_MODULE_SUCCESS + "005", "Pdf Generation failed for "),
	QUEUE_CONNECTION_NOT_FOUND(StatusConstants.PRINT_STAGE_MODULE_SUCCESS + "006", "Queue Connection not found "),
	QUEUE_CONNECTION_UNAVAILABLE(StatusConstants.PRINT_STAGE_MODULE_SUCCESS + "007",
			"Queue Connection unavailable for "),
	PDF_SIGNTURED_FAILED(StatusConstants.PRINT_STAGE_MODULE_SUCCESS + "008", "Pdf Signature failed "),
	PRINT_REQUEST_FAILED(StatusConstants.PRINT_STAGE_MODULE_FAILED + "009", "Print request failed"),
	UIN_NOT_FOUND_IN_DATABASE(StatusConstants.PRINT_STAGE_MODULE_FAILED + "010", "UIN not found in database"),
	/** The rpr prt vid exception. */
	VID_NOT_AVAILABLE(PlatformConstants.RPR_PRINTING_MODULE + "011", "vid not available"),

	// Abis middleware stage
	INSERT_IDENTIFY_REQUEST_SUCCESS(StatusConstants.ABIS_MIDDLEWARE_MODULE_SUCCESS + "001",
			"Insert or Identify Request sent to ABIS Queue is succesful"),
	INSERT_IDENTIFY_REQUEST_FAILED(StatusConstants.ABIS_MIDDLEWARE_MODULE_FAILED + "001",
			"Insert or Identify Request sent to ABIS Queue is Unsuccesful"),
	INSERT_IDENTIFY_RESPONSE_SUCCESS(StatusConstants.ABIS_MIDDLEWARE_MODULE_SUCCESS + "002",
			"Recived sucessful response from ABIS"),
	INSERT_IDENTIFY_RESPONSE_FAILED(StatusConstants.ABIS_MIDDLEWARE_MODULE_SUCCESS + "002",
			"Received failed response from ABIS - "),

	// System Exceptions
	// Bio dedupe stage
	BIO_DEDUPE_INPROGRESS(StatusConstants.BIO_DEDUPE_MODULE_SUCCESS + "001", "Biometric Deduplication In-Progress"),
	BIO_DEDUPE_SUCCESS(StatusConstants.BIO_DEDUPE_MODULE_SUCCESS + "002", "Biometric Deduplication is Successful"),
	BIO_DEDUPE_POTENTIAL_MATCH(StatusConstants.BIO_DEDUPE_MODULE_FAILED + "001",
			"Potential Biometric Match Found while Processing Packet"),
	LOST_PACKET_BIOMETRICS_NOT_FOUND(StatusConstants.BIO_DEDUPE_MODULE_FAILED + "002",
			"No Match was Found for the Biometrics Received"),
	LOST_PACKET_UNIQUE_MATCH_FOUND(StatusConstants.BIO_DEDUPE_MODULE_SUCCESS + "003",
			"Unique Match was Found for the Biometrics Received"),
	LOST_PACKET_MULTIPLE_MATCH_FOUND(StatusConstants.BIO_DEDUPE_MODULE_FAILED + "003",
			"Multiple Match was Found for the Biometrics Received"),

	// Biometric authentication stage
	BIOMETRIC_AUTHENTICATION_FAILED(StatusConstants.BIO_METRIC_AUTHENTICATION_MODULE_FAILED + "001",
			"Biometric Authentication has Failed"),
	BIOMETRIC_AUTHENTICATION_SUCCESS(StatusConstants.BIO_METRIC_AUTHENTICATION_MODULE_SUCCESS + "001",
			"Biometric Authentication is Successful"),
	BIOMETRIC_FILE_NOT_FOUND(StatusConstants.SYSTEM_EXCEPTION_CODE, "Biometric File Not Found"),
	BIOMETRIC_AUTHENTICATION_FAILED_FILE_NOT_FOUND(StatusConstants.SYSTEM_EXCEPTION_CODE,
			"Biometric Authentication Failed File is not present inside identity json"),
	INDIVIDUAL_BIOMETRIC_AUTHENTICATION_FAILED(StatusConstants.BIO_METRIC_AUTHENTICATION_MODULE_FAILED + "001",
			"Individual authentication failed"),
	BIOMETRIC_AUTHENTICATION_SKIPPED(StatusConstants.BIO_METRIC_AUTHENTICATION_MODULE_SUCCESS + "002",
			"Biometric Authentication is Skipped "),

	// Demo dedupe stage
	DEMO_DEDUPE_SUCCESS(StatusConstants.DEMO_DEDUPE_MODULE_SUCCESS + "001", "Demo Dedupe is Successful"),
	POTENTIAL_MATCH_FOUND_IN_ABIS(StatusConstants.DEMO_DEDUPE_MODULE_FAILED + "001",
			"Biometric Duplicate was Found in ABIS"),
	POTENTIAL_MATCH_FOUND(StatusConstants.DEMO_DEDUPE_MODULE_FAILED + "002", "Potential Demo Match was Found"),
	DEMO_DEDUPE_SKIPPED(StatusConstants.DEMO_DEDUPE_MODULE_SKIPPED + "003", "Demographic Deduplication Skipped"),

	// Manual verification stage
	MANUAL_VERIFIER_APPROVED_PACKET(StatusConstants.MANUAL_VERIFICATION_MODULE_SUCCESS + "001",
			"Match Not Found by Manual Verifier"),
	MANUAL_VERIFIER_REJECTED_PACKET(StatusConstants.MANUAL_VERIFICATION_MODULE_FAILED + "002",
			"Match Found by Manual Verifier"),
	RPR_MANUAL_VERIFICATION_RESEND(StatusConstants.MANUAL_VERIFICATION_MODULE_FAILED + "003",
			"Error in manual verification"),
	RPR_MANUAL_VERIFICATION_SENT_TO_QUEUE(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "002",
			"Manual verification request sent to queue"),

	// Uin generator stage
	UIN_GENERATED_SUCCESS(StatusConstants.UIN_GENERATOR_MODULE_SUCCESS + "001", "UIN Generated Successfully"),
	UIN_DATA_UPDATION_SUCCESS(StatusConstants.UIN_GENERATOR_MODULE_SUCCESS + "002", "UIN Data is drafted Successfully"),
	UIN_ACTIVATED_SUCCESS(StatusConstants.UIN_GENERATOR_MODULE_SUCCESS + "003", "UIN is Activated"),
	UIN_DEACTIVATION_SUCCESS(StatusConstants.UIN_GENERATOR_MODULE_SUCCESS + "004", "UIN is Deactivated"),
	LINK_RID_FOR_LOST_PACKET_SUCCESS(StatusConstants.UIN_GENERATOR_MODULE_SUCCESS + "005",
			"RID linked Successfully for Lost UIN Packet"),

	UIN_ALREADY_ACTIVATED(StatusConstants.UIN_GENERATOR_MODULE_FAILED + "001", "UIN is already Activated"),
	UIN_ACTIVATED_FAILED(StatusConstants.UIN_GENERATOR_MODULE_FAILED + "002", "UIN Activation Failed"),
	UIN_ALREADY_DEACTIVATED(StatusConstants.UIN_GENERATOR_MODULE_FAILED + "003", "UIN already deactivated"),

	UIN_GENERATION_FAILED(StatusConstants.UIN_GENERATOR_MODULE_FAILED + "004", "UIN Generation failed - "),
	UIN_DATA_UPDATION_FAILED(StatusConstants.UIN_GENERATOR_MODULE_FAILED + "005", "UIN Updation failed - "),
	UIN_REACTIVATION_FAILED(StatusConstants.UIN_GENERATOR_MODULE_FAILED + "006", "UIN Reactivation  failed - "),
	UIN_DEACTIVATION_FAILED(StatusConstants.UIN_GENERATOR_MODULE_FAILED + "007", "UIN Deactivation  failed - "),
	LINK_RID_FOR_LOST_PACKET_FAILED(StatusConstants.UIN_GENERATOR_MODULE_FAILED + "008",
			"UIn not found the the matched RID"),
	IDREPO_DRAFT_EXCEPTION(StatusConstants.UIN_GENERATOR_MODULE_FAILED + "008",
			"Exception occured updating idrepo draft."),

	IDREPO_DRAFT_REPROCESSABLE_EXCEPTION(StatusConstants.UIN_GENERATOR_MODULE_FAILED + "009",
			"Exception occured updating idrepo draft, which can be reprocessed"),

	// Biometric extraction stage
	BIOMETRIC_EXTRACTION_SUCCESS(StatusConstants.BIOMETRIC_EXTRACTION_MODULE_SUCCESS + "001",
			"biometric extaction was successful"),
	BIOMETRIC_EXTRACTION_DRAFT_REQUEST_UNAVAILABLE(StatusConstants.BIOMETRIC_EXTRACTION_MODULE_FAILED + "001",
			"Draft request is unavaialble in id-repo draft repository."),
	BIOMETRIC_EXTRACTION_IDREPO_DRAFT_EXCEPTION(StatusConstants.BIOMETRIC_EXTRACTION_MODULE_FAILED + "002",
			"Exception occured updating idrepo draft."),

	BIOMETRIC_EXTRACTION_IDREPO_DRAFT_REPROCESSABLE_EXCEPTION(
			StatusConstants.BIOMETRIC_EXTRACTION_MODULE_FAILED + "003",
			"Exception occured updating idrepo draft, which can be reprocessed"),

	FINALIZATION_SUCCESS(StatusConstants.FINALIZATION_MODULE_SUCCESS + "001",
			"idrepo draft was published  successfuly"),
	FINALIZATION_FAILURE(StatusConstants.FINALIZATION_MODULE_FAILED + "001", "Draft request failed to publish."),
	FINALIZATION_DRAFT_REQUEST_UNAVAILABLE(StatusConstants.FINALIZATION_MODULE_FAILED + "002",
			"Draft request is unavaialble in id-repo draft repository."),

	FINALIZATION_IDREPO_DRAFT_EXCEPTION(StatusConstants.FINALIZATION_MODULE_FAILED + "003",
			"Exception occured updating idrepo draft."),

	FINALIZATION_IDREPO_DRAFT_REPROCESSABLE_EXCEPTION(StatusConstants.FINALIZATION_MODULE_FAILED + "004",
			"Exception occured updating idrepo draft, which can be reprocessed"),

	// Request handler service
	// 1)Resident UIN update
	RESIDENT_UPDATE_SUCCES(StatusConstants.REQUEST_HANDLER_MODULE_SUCCESS + "001",
			"Resident Uin data updated sucessfully"),
	RESIDENT_UPDATE_FAILED(StatusConstants.REQUEST_HANDLER_MODULE_FAILED + "001", "Resident Uin update failed"),
	INVALID_REQUEST(StatusConstants.REQUEST_HANDLER_MODULE_FAILED + "001", "Invalid Request Value - "),
	INVALID_CENTER(StatusConstants.REQUEST_HANDLER_MODULE_FAILED + "002", "Invalid Request Value - "),
	INVALID_MACHINE(StatusConstants.REQUEST_HANDLER_MODULE_FAILED + "003", "Invalid Request Value - "),

	// 2)PacketGeneration
	PACKET_GENERATION_SUCCESS(StatusConstants.REQUEST_HANDLER_MODULE_SUCCESS + "001", "Packet generated sucessfully"),
	PACKET_GENERATION_FAILED(StatusConstants.REQUEST_HANDLER_MODULE_FAILED + "001", "Packet generated failed"),

	// 3)Uin card reprint
	UIN_CARD_REPRINT_SUCCESS(StatusConstants.REQUEST_HANDLER_MODULE_SUCCESS + "001", "UIN card reprint success"),
	UIN_CARD_REPRINT_FAILED(StatusConstants.REQUEST_HANDLER_MODULE_FAILED + "001", "UIN card reprint failed"),

	// System Exceptions
	VIRUS_SCANNER_SERVICE_NOT_ACCESSIBLE(StatusConstants.SYSTEM_EXCEPTION_CODE,
			"Virus Scanner Service is not accessible"),
	DB_NOT_ACCESSIBLE(StatusConstants.SYSTEM_EXCEPTION_CODE, "Databse Not Accessible"),
	PACKET_NOT_FOUND_PACKET_STORE(StatusConstants.SYSTEM_EXCEPTION_CODE, "Packet not found in File System"),
	OBJECT_STORE_EXCEPTION(StatusConstants.SYSTEM_EXCEPTION_CODE, "Object store exception occured"),
	JSCH_EXCEPTION_OCCURED(StatusConstants.SYSTEM_EXCEPTION_CODE, "JSCH Connection Exception Occurred"),
	NGINX_ACCESS_EXCEPTION(StatusConstants.SYSTEM_EXCEPTION_CODE, "NGINX url is not accessible"),
	IO_EXCEPTION(StatusConstants.SYSTEM_EXCEPTION_CODE, "IO Exception Occurred"),
	BIO_METRIC_EXCEPTION(StatusConstants.SYSTEM_EXCEPTION_CODE, "Biometric Exception Occurred in IDA "),
	BIO_METRIC_FILE_MISSING(StatusConstants.SYSTEM_EXCEPTION_CODE, "Applicant biometric fileName/file is missing"),
	BIO_METRIC_TYPE_EXCEPTION(StatusConstants.SYSTEM_EXCEPTION_CODE, "Requested biometric type not found"),

	UNKNOWN_EXCEPTION_OCCURED(StatusConstants.SYSTEM_EXCEPTION_CODE, "Unknown exception occured "),
	API_RESOUCE_ACCESS_FAILED(StatusConstants.SYSTEM_EXCEPTION_CODE, "Unable to access API resource"),
	AUTH_SYSTEM_EXCEPTION(StatusConstants.SYSTEM_EXCEPTION_CODE, "Auth System Exception"),
	JSON_PARSING_EXCEPTION(StatusConstants.SYSTEM_EXCEPTION_CODE, "Error Occurred while Parsing JSON"),
	BASE_CHECKED_EXCEPTION(StatusConstants.SYSTEM_EXCEPTION_CODE, "Packet validation failed "),
	BASE_UNCHECKED_EXCEPTION(StatusConstants.SYSTEM_EXCEPTION_CODE, "Packet validation failed "),

	OFFICER_AUTHENTICATION_FAILED(StatusConstants.SYSTEM_EXCEPTION_CODE, "Officer Authentication Failed: "),
	SUPERVISOR_AUTHENTICATION_FAILED(StatusConstants.SYSTEM_EXCEPTION_CODE, "Supervisor Authentication Failed: "),

	IDENTIFY_RESPONSE_FAILED(StatusConstants.SYSTEM_EXCEPTION_CODE, "Identify Response Failed for Request ID - "),
	INSERT_RESPONSE_FAILED(StatusConstants.SYSTEM_EXCEPTION_CODE, "Insert Response Failed for Request ID - "),
	SYSTEM_EXCEPTION_OCCURED(StatusConstants.SYSTEM_EXCEPTION_CODE, "Internal error occured - "),

	CBEF_NOT_FOUND(StatusConstants.SYSTEM_EXCEPTION_CODE, "Unable to Find Applicant CBEFF for Adult"),

	IIEGAL_ARGUMENT_EXCEPTION(StatusConstants.SYSTEM_EXCEPTION_CODE, "Illegal Argument Exception Occurred - "),
	DEMO_DEDUPE_FAILED_IN_ABIS(StatusConstants.SYSTEM_EXCEPTION_CODE, "Demo Dedupe Failed  in ABIS"),
	RE_PROCESS_FAILED(StatusConstants.RE_PROCESS_MODULE_FAILED + "001",
			"Reprocess count has exceeded the configured attempts"),
	RE_PROCESS_COMPLETED(StatusConstants.RE_PROCESS_MODULE_SUCCESS + "001", "Reprocess Completed"),
	RE_PROCESS_RESTART_FROM_STAGE(StatusConstants.RE_PROCESS_MODULE_SUCCESS + "002",
			"Reprocess restart from stage Completed"),

	// Message sender stage
	NOTIFICATION_SUCESSFUL(StatusConstants.MESSAGE_SENDER_NOTIF_SUCCESS_CODE + "001", "Notification Sent Successfully"),
	TEMPLATE_CONFIGURATION_NOT_FOUND(StatusConstants.MESSAGE_SENDER__FAILED_CODE + "002",
			"Template configuration and language not found"),
	EMAIL_PHONE_TEMPLATE_NOTIFICATION_MISSING(StatusConstants.MESSAGE_SENDER__FAILED_CODE + "003",
			"Email ID or Phone or Template or Notification Type is Missing"),
	NOTIFICATION_FAILED_FOR_LOST(StatusConstants.MESSAGE_SENDER__FAILED_CODE + "004",
			"Unable to Send Notification - UIN was not found for the Lost Packet"),

	MESSAGE_SENDER_NOTIF_SUCC(StatusConstants.MESSAGE_SENDER_NOTIF_SUCCESS_CODE + "001",
			"Email and SMS Notification were sent"),
	MESSAGE_SENDER_NOT_CONFIGURED(StatusConstants.MESSAGE_SENDER_NOTIF_SUCCESS_CODE + "002",
			"Notification was not sent as notification type was not set"),
	MESSAGE_SENDER_EMAIL_SUCCESS(StatusConstants.MESSAGE_SENDER_NOTIF_SUCCESS_CODE + "003",
			"Email Notification was sent"),
	MESSAGE_SENDER_SMS_SUCCESS(StatusConstants.MESSAGE_SENDER_NOTIF_SUCCESS_CODE + "004", "SMS Notification was sent"),
	MESSAGE_SENDER_EMAIL_FAILED(StatusConstants.MESSAGE_SENDER__FAILED_CODE + "005",
			"Notification was not sent as the required mode of channel was not available"),
	MESSAGE_SENDER_SMS_FAILED(StatusConstants.MESSAGE_SENDER__FAILED_CODE + "006",
			"Notification was not sent as the required mode of channel was not available"),
	MESSAGE_SENDER_NOTIFICATION_FAILED(StatusConstants.MESSAGE_SENDER__FAILED_CODE + "006",
			"Notification was not sent as the required mode of channel was not available"),
	VID_CREATION_FAILED(StatusConstants.UIN_GENERATOR_MODULE_FAILED + "009", "VID creation failed -"),

	WORKFLOW_INTERNAL_ACTION_SUCCESS(StatusConstants.WORKFLOW_INTERNAL_ACTION + "001",
			"Packet workflow internal action completed successfully"),

	WORKFLOW_INTERNAL_ACTION_REJECTED_ITERATIONS_EXCEEDED_LIMIT(StatusConstants.WORKFLOW_INTERNAL_ACTION + "002",
			"Packet rejected as number of iterations exceeded permited limit."),

	WORKFLOW_ACTION_SERVICE_SUCCESS(StatusConstants.WORKFLOW_ACTION_SERVICE + "001",
			"Packet workflow resume  successfully"),

    WORKFLOW_INSTANCE_SERVICE_SUCCESS(StatusConstants.WORKFLOW_INSTANCE_SERVICE + "001",
            "Packet workflow instance created successfully"),
	MANUAL_ADJUDICATION_FAILED(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "000",
			"manual verification failed -"),
	MANUAL_ADJUDICATION_RID_SHOULD_NOT_EMPTY_OR_NULL(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "001",
			"Registration Id should not empty or null "),
	MANUAL_ADJUDICATION_MATCHEDRID_FOUND_FOR_GIVEN_RID(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "002",
			"No matched reference id found for given RID"),
	VERIFICATION_SUCCESS(StatusConstants.VERIFICATION_STAGE + "001", "Verification success"),
	VERIFICATION_FAILED(StatusConstants.VERIFICATION_STAGE + "002", "Verification failed"),
	VERIFICATION_SENT(StatusConstants.VERIFICATION_STAGE + "003", "Sent for verification"),
	VERIFICATION_RESEND(StatusConstants.VERIFICATION_STAGE + "004", "Resend for verification");

	private final String statusComment;
	private final String statusCode;

	private StatusUtil(String statusCode, String statusComment) {
		this.statusCode = statusCode;
		this.statusComment = statusComment;
	}

	public String getMessage() {
		return this.statusComment;
	}

	/**
	 * Gets the error code.
	 *
	 * @return the error code
	 */
	public String getCode() {
		return this.statusCode;
	}

}