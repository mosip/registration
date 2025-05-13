package io.mosip.registration.processor.core.exception.util;

// TODO: Auto-generated Javadoc
/**
 * The Enum RPRPlatformErrorMessages.
 *
 * @author M1047487
 */
public enum PlatformErrorMessages {

	/** The rpr pkr packet not yet sync. */
	// Packet Receiver Exception error code and message
	RPR_PKR_PACKET_NOT_YET_SYNC(PlatformConstants.RPR_PACKET_RECEIVER_MODULE + "001", "Packet Not Found in Sync Table"),

	/** The rpr pkr invalid packet size. */
	RPR_PKR_INVALID_PACKET_SIZE(PlatformConstants.RPR_PACKET_RECEIVER_MODULE + "002", "Invalid Packet Size"),
	RPR_WORKFLOW_SEARCH_API_FAILED(PlatformConstants.RPR_WORKFLOW_SEARCH_API + "000",
			"Process the workflow search failed"),

	/** The rpr pkr packet hash not equals synced hash. */
	RPR_PKR_PACKET_HASH_NOT_EQUALS_SYNCED_HASH(PlatformConstants.RPR_PACKET_RECEIVER_MODULE + "009",
			"Packet HashSequence did not match"),
	/** The prp pkr packet virus scan failed. */
	PRP_PKR_PACKET_VIRUS_SCAN_FAILED(PlatformConstants.RPR_PACKET_RECEIVER_MODULE + "010", "Virus was Found in Packet"),
	/** The prp pkr packet virus scanner service failed. */
	PRP_PKR_PACKET_VIRUS_SCANNER_SERVICE_FAILED(PlatformConstants.RPR_PACKET_RECEIVER_MODULE + "008",
			"Virus Scan Service is Not Responding"),

	/** The rpr pkr invalid packet format. */
	RPR_PKR_INVALID_PACKET_FORMAT(PlatformConstants.RPR_PACKET_RECEIVER_MODULE + "003", "Invalid Packet Format"),

	/** The rpr pkr validation exception. */
	RPR_PKR_VALIDATION_EXCEPTION(PlatformConstants.RPR_PACKET_RECEIVER_MODULE + "004", "Packet Validation Failed"),

	/** The rpr pkr duplicate packet recieved. */
	RPR_PKR_DUPLICATE_PACKET_RECIEVED(PlatformConstants.RPR_PACKET_RECEIVER_MODULE + "005",
			"Duplicate Request Received"),

	/** The rpr pkr packet not available. */
	RPR_PKR_PACKET_NOT_AVAILABLE(PlatformConstants.RPR_PACKET_RECEIVER_MODULE + "006",
			"Packet Not Available in Request"),

	/** The rpr pkr unknown exception. */
	RPR_PKR_UNKNOWN_EXCEPTION(PlatformConstants.RPR_PACKET_RECEIVER_MODULE + "007", "Unknown Exception Found"),

	/** The rpr pkr api resouce access failed. */
	RPR_PKR_API_RESOUCE_ACCESS_FAILED(PlatformConstants.RPR_PACKET_RECEIVER_MODULE + "011",
			"Unable to Access API Resource"),

	/** The rpr pkr data access exception. */
	RPR_PKR_DATA_ACCESS_EXCEPTION(PlatformConstants.RPR_PACKET_RECEIVER_MODULE + "012", "Unable to Access Database"),

	/** The rpr pkr invalid packet size synced. */
	RPR_PKR_INVALID_PACKET_SIZE_SYNCED(PlatformConstants.RPR_PACKET_RECEIVER_MODULE + "013",
			"Packet Size is Not Matching"),

	/** The rpr pkr invalid packet size synced. */
	UNEQUAL_PACKET_HASH_PR(PlatformConstants.RPR_PACKET_RECEIVER_MODULE + "014",
			"The Registration Packet HashSequence is not equal as synced packet HashSequence"),

	/** The rpr pkr invalid packet size synced. */
	RPR_PKR_DECRYPTION_FAILED(PlatformConstants.RPR_PACKET_RECEIVER_MODULE + "015", "Packet Decryption Failed"),

	/** The rpr pkr invalid packet size synced. */
	RPR_PKR_ADDITIONAL_INFOID_NOT_FOUND(PlatformConstants.RPR_PACKET_RECEIVER_MODULE + "016", "Additional info request id not found"),

	/** The rpr rgs registration table not accessible. */
	// Registration Status Exception error code and message
	RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "001",
			"Unable to Access Registration Table"),

	/** The rpr rgs transaction table not accessible. */
	RPR_RGS_TRANSACTION_TABLE_NOT_ACCESSIBLE(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "002",
			"Unable to Access Registration Transaction Table"),
	
	/** The rpr rgs anonymous profile table not accessible. */
	RPR_RGS_ANONYMOUS_PROFILE_TABLE_NOT_ACCESSIBLE(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "038",
			"Unable to anonymous profile  Table"),

	/** The rpr rgs invalid synctype. */
	RPR_RGS_INVALID_SYNCTYPE(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "003", "Invalid Sync Type"),

	/** The rpr rgs invalid languagecode. */
	RPR_RGS_INVALID_LANGUAGECODE(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "004",
			"Invalid Language Code - Language Code must be of Three Characters"),

	/** The rpr rgs invalid regid parentregid. */
	RPR_RGS_INVALID_REGID_PARENTREGID(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "005",
			"Invalid Request Value - RID and Parent RID are Same"),

	/** The rpr rgs empty registrationid. */
	RPR_RGS_EMPTY_REGISTRATIONID(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "006",
			"Invalid Request Value - RID cannot be NULL"),

	/** The rpr rgs invalid registrationid timestamp. */
	RPR_RGS_INVALID_REGISTRATIONID_TIMESTAMP(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "007",
			"Invalid Request Value - Invalid Timestamp in RID"),

	/** The rpr rgs invalid registrationid. */
	RPR_RGS_INVALID_REGISTRATIONID(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "008",
			"Invalid Request Value - RID must be Numberic"),

	/** The rpr rgs invalid registrationid length. */
	RPR_RGS_INVALID_REGISTRATIONID_LENGTH(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "009",
			"Invalid Request Value - RID Length is not as per Configuration"),

	/** The rpr rgs invalid prid timestamp. */
	RPR_RGS_INVALID_PRID_TIMESTAMP(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "010",
			"Invalid Request Value - Invalid Timestamp in Parent RID"),

	/** The rpr rgs invalid prid. */
	RPR_RGS_INVALID_PRID(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "011",
			"Invalid Request Value - Parent RID must be Numeric"),

	/** The rpr rgs invalid prid length. */
	RPR_RGS_INVALID_PRID_LENGTH(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "012",
			"Invalid Request Value - Parent RID Length is not as per Configuration"),

	/** The missing input parameter. */
	RPR_RGS_MISSING_INPUT_PARAMETER(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "013",
			"Missing Request Value - %s"),

	/** The invalid input parameter. */
	RPR_RGS_INVALID_INPUT_PARAMETER(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "014",
			"Invalid Request Value - %s"),

	/** The data validation failed. */
	RPR_RGS_DATA_VALIDATION_FAILED(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "015",
			"Invalid Request Value - Input Data is Incorrect"),

	/** The rpr rgs json mapping exception. */
	RPR_RGS_JSON_MAPPING_EXCEPTION(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "016", "JSON Mapping Failed"),

	/** The rpr rgs json parsing exception. */
	RPR_RGS_JSON_PARSING_EXCEPTION(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "017", "JSON Parsing Failed"),

	/** The rpr rgs unknown exception. */
	RPR_RGS_UNKNOWN_EXCEPTION(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "018", "Unknown Exception Found"),

	/** The rpr rgs missing input parameter version. */
	RPR_RGS_MISSING_INPUT_PARAMETER_VERSION(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "019",
			"Missing Request Value - version"),

	/** The rpr rgs missing input parameter timestamp. */
	RPR_RGS_MISSING_INPUT_PARAMETER_TIMESTAMP(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "020",
			"Missing Request Parameter - requesttime"),

	/** The rpr rgs missing input parameter id. */
	RPR_RGS_MISSING_INPUT_PARAMETER_ID(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "021",
			"Missing Request Parameter - id"),

	/** The rpr rgs invalid input parameter version. */
	RPR_RGS_INVALID_INPUT_PARAMETER_VERSION(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "022",
			"Invalid Request Parameter - version"),

	/** The rpr rgs invalid input parameter timestamp. */
	RPR_RGS_INVALID_INPUT_PARAMETER_TIMESTAMP(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "023",
			"Invalid Request Parameter - requesttime"),

	/** The rpr rgs invalid input parameter id. */
	RPR_RGS_INVALID_INPUT_PARAMETER_ID(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "024",
			"Invalid Request Parameter - id"),

	/** The rpr rgs registration status not exist. */
	RPR_RGS_REGISTRATION_STATUS_NOT_EXIST(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "025",
			"Invalid Request Value - Status Code is NULL"),

	/** The rpr rgs invalid supervisor status. */
	RPR_RGS_INVALID_SUPERVISOR_STATUS(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "026",
			"Invalid Request Value - Supervisor Status can be APPROVED/REJECTED"),

	/** The rpr rgs invalid hashvalue. */
	RPR_RGS_INVALID_HASHVALUE(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "027",
			"Invalid Request Value - Hash Sequence is NULL"),

	/** The rpr rgs decryption failed. */
	RPR_RGS_DECRYPTION_FAILED(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "028", "Request Decryption Failed"),

	/** The rpr rgs registration sync service failed. */
	RPR_RGS_REGISTRATION_SYNC_SERVICE_FAILED(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "029", "SYNC Failed"),

	/** The rpr rgs data access exception. */
	RPR_RGS_DATA_ACCESS_EXCEPTION(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "030",
			"DataAccessLayerException while syncing Registartion Id's"),
	/** The rpr rgs rid not found. */
	RPR_RGS_RID_NOT_FOUND(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "031", "RID Not Found"),
	
	RPR_RGS_INVALID_ADDITIONAL_INFORMATION(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "036", "additionalInfoReqId is invalid"),

	RPR_RGS_PACKETID_NOT_FOUND(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "037", "PacketId Not Found"),

	RPR_RGS_INVALID_SEARCH(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "032", "plz narrow down your search"),

	RPR_RGS_SORTING_VALIDATION_FAILED(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "033",
			"sorting not supported for more than 1 field - %s"),

	RPR_RGS_DATE_VALIDATION_FAILED(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "034",
			"searching between date should be less then 30 days - %s"),
	
	/** The rpr rgs io exception */
	RPR_RGS_IOEXCEPTION(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "035" , "IO Exception"),

	LINK_FOR_USERID_INDIVIDUALID_FAILED_STATUS_EXCEPTION(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "035",
			"Unable to get the IndividualId for UserId"),

	UNABLE_TO_ACCESS_API(PlatformConstants.RPR_REGISTRATION_STATUS_MODULE + "036",
			"Unable to access API"),

	/** The rpr pis registration table not accessible. */
	// Packet Info Storage Exception error code and message
	RPR_PIS_REGISTRATION_TABLE_NOT_ACCESSIBLE(PlatformConstants.RPR_PACKET_INFO_STORAGE_MODULE + "001",
			"Unable to Access Registration Table"),
	/** The rpr pis identity not found. */
	RPR_PIS_IDENTITY_NOT_FOUND(PlatformConstants.RPR_PACKET_INFO_STORAGE_MODULE + "002",
			"Unable to Find Identity Field in ID JSON"),
	/** The rpr pis unable to insert data. */
	RPR_PIS_UNABLE_TO_INSERT_DATA(PlatformConstants.RPR_PACKET_INFO_STORAGE_MODULE + "003",
			"Unable to Insert Data in DB"),
	/** The rpr pis file not found in Packet Store. */
	RPR_PIS_FILE_NOT_FOUND_IN_PACKET_STORE(PlatformConstants.RPR_PACKET_INFO_STORAGE_MODULE + "004",
			"Unable to Find File in Packet Store"),

	/** The rpr pis abis queue connection null. */
	RPR_PIS_ABIS_QUEUE_CONNECTION_NULL(PlatformConstants.RPR_PACKET_INFO_STORAGE_MODULE + "005",
			"Unable to Find ABIS Queue Connection"),

	GET_FIELD_ERROR(PlatformConstants.RPR_PACKET_INFO_STORAGE_MODULE + "006",
			"Not able to read field from packet manager"),

	GET_DOC_ERROR(PlatformConstants.RPR_PACKET_INFO_STORAGE_MODULE + "007",
			"Not able to read document from packet manager"),

	GET_BIOMETRIC_ERROR(PlatformConstants.RPR_PACKET_INFO_STORAGE_MODULE + "008",
			"Not able to read biometric from packet manager"),

	GET_AUDIT_ERROR(PlatformConstants.RPR_PACKET_INFO_STORAGE_MODULE + "009",
			"Not able to read audit from packet manager"),

	GET_METAINFO_ERROR(PlatformConstants.RPR_PACKET_INFO_STORAGE_MODULE + "010",
			"Not able to read meta info from packet manager"),

	VALIDATE_ERROR(PlatformConstants.RPR_PACKET_INFO_STORAGE_MODULE + "011",
			"Not able to validate packet using packet manager provider."),

	PACKET_MANAGER_EXCEPTION(PlatformConstants.RPR_PACKET_INFO_STORAGE_MODULE + "012",
			"Exception occured in packet manager."),

	PACKET_MANAGER_NON_RECOVERABLE_EXCEPTION(PlatformConstants.RPR_PACKET_INFO_STORAGE_MODULE + "013",
			"Non recoverable error occurred in packet manager."),

	/** The rpr fac connection not available. */
	// File adaptor Exception error code and message
	RPR_FAC_CONNECTION_NOT_AVAILABLE(PlatformConstants.RPR_FILESYSTEM_ADAPTOR_CEPH_MODULE + "001",
			"Unable to Find Connection Parameter for Packet Store"),

	/** The rpr fac invalid connection parameters. */
	RPR_FAC_INVALID_CONNECTION_PARAMETERS(PlatformConstants.RPR_FILESYSTEM_ADAPTOR_CEPH_MODULE + "002",
			"Invalid Connection Parameter for Packet Store"),

	/** The rpr fac packet not available. */
	RPR_FAC_PACKET_NOT_AVAILABLE(PlatformConstants.RPR_FILESYSTEM_ADAPTOR_CEPH_MODULE + "003",
			"Packet Not Found in Packet Store"),

	/** The rpr pkm file path not accessible. */
	// Packet Manager Exception error code and message
	RPR_PKM_FILE_PATH_NOT_ACCESSIBLE(PlatformConstants.RPR_PACKET_MANAGER_MODULE + "002",
			"Unable to Access the Folder Path"),

	/** The rpr pkm file not found in destination. */
	RPR_PKM_FILE_NOT_FOUND_IN_DESTINATION(PlatformConstants.RPR_PACKET_MANAGER_MODULE + "003",
			"Unable to Find File in Destination Folder"),

	/** The rpr pkm file not found in source. */
	RPR_PKM_FILE_NOT_FOUND_IN_SOURCE(PlatformConstants.RPR_PACKET_MANAGER_MODULE + "004",
			"Unable to Find File in Source Folder"),

	/** The rpr pkm file not found in source. */
	RPR_PKM_JSCH_NOT_CONNECTED(PlatformConstants.RPR_PACKET_MANAGER_MODULE + "005", "JSCH Connection Failed"),

	/** The rpr pkm file not found in source. */
	RPR_PKM_SFTP_FILE_OPERATION_FAILED(PlatformConstants.RPR_PACKET_MANAGER_MODULE + "006", "SFTP Operation Failed"),

	/** The rpr pkm file not found in source. */
	RPR_PKM_PWD_PPK_NOT_PRESENT(PlatformConstants.RPR_PACKET_MANAGER_MODULE + "007",
			"Both DMZ password and PPK file name are not available in config"),

	// Registration processor camel bridge Exception error code and message

	/** The rpr cmb deployment failure. */
	RPR_CMB_DEPLOYMENT_FAILURE(PlatformConstants.RPR_CAMEL_BRIDGE_MODULE + "001", "Camel Bridge Deployment Failure"),

	/** The rpr cmb unsupported encoding. */
	RPR_CMB_UNSUPPORTED_ENCODING(PlatformConstants.RPR_CAMEL_BRIDGE_MODULE + "002", "Unsupported Failure"),

	/** The rpr cmb configuration server failure exception. */
	RPR_CMB_CONFIGURATION_SERVER_FAILURE_EXCEPTION(PlatformConstants.RPR_CAMEL_BRIDGE_MODULE + "003",
			"Configuration Server Failure"),

	/** The rpr cmb malformed cluster manager url exception. */
	RPR_CMB_MALFORMED_URL_EXCEPTION(PlatformConstants.RPR_CAMEL_BRIDGE_MODULE + "004",
			"Malformed Cluster Manager URL Exception"),

	/** The rpr cmb unknown host exception. */
	RPR_CMB_UNKNOWN_HOST_EXCEPTION(PlatformConstants.RPR_CAMEL_BRIDGE_MODULE + "005", "Unknown Host Exception"),


	RPR_CMB_WORKFLOW_COMMAND_NOT_SUPPORTED(PlatformConstants.RPR_CAMEL_BRIDGE_MODULE + "006",
			"Workflow command not supported"),

	/** The rpr qcr registration table not accessible. */
	// Quality Checker Exception error code and message
	RPR_QCR_REGISTRATION_TABLE_NOT_ACCESSIBLE(PlatformConstants.RPR_QUALITY_CHECKER_MODULE + "001",
			"Unable to Access Registration Table"),

	/** The rpr qcr result not found. */
	RPR_QCR_RESULT_NOT_FOUND(PlatformConstants.RPR_QUALITY_CHECKER_MODULE + "002", "Result Not Found"),

	/** The rpr qcr invalid qc user id. */
	RPR_QCR_INVALID_QC_USER_ID(PlatformConstants.RPR_QUALITY_CHECKER_MODULE + "003", "Invalid QC User ID"),

	/** The rpr qcr invalid registration id. */
	RPR_QCR_INVALID_REGISTRATION_ID(PlatformConstants.RPR_QUALITY_CHECKER_MODULE + "004",
			"Invalid Registration ID - RID is NULL"),

	/** The rpr qcr file name missing. */
	RPR_QCR_FILENAME_MISSING(PlatformConstants.RPR_QUALITY_CHECKER_MODULE + "006",
			"Unable to Find Biometric File Name in ID JSON"),

	/** The rpr qcr bio file missing. */
	RPR_QCR_BIO_FILE_MISSING(PlatformConstants.RPR_QUALITY_CHECKER_MODULE + "007",
			"Unable to Find Biometric File in Packet"),

	/** The rpr qcr biometric exception. */
	RPR_QCR_BIOMETRIC_EXCEPTION(PlatformConstants.RPR_QUALITY_CHECKER_MODULE + "007",
			"Biometric Exception received form IDA"),

	/** The rpr qcr biometric exception. */
	RPR_QCR_BIOMETRIC_TYPE_EXCEPTION(PlatformConstants.RPR_QUALITY_CHECKER_MODULE + "008",
			"Requested biometric type not found"),

	/** The rpr qcr biometric exception. */
	INDIVIDUAL_BIOMETRIC_NOT_FOUND(PlatformConstants.RPR_QUALITY_CHECKER_MODULE + "009",
			"Individual Biometric Parameter Not Found in ID JSON so skipping biometric classification"),

	/** The rpr qcr biometric exception. */
	BIOMETRIC_QUALITY_CHECK_FAILED(PlatformConstants.RPR_QUALITY_CHECKER_MODULE + "010",
			"Quality Score of Biometrics Captured is Below the Threshold"),

	/** The rpr qcr packet store not accessible. */
	RPR_QCR_PACKET_STORE_NOT_ACCESSIBLE(PlatformConstants.RPR_QUALITY_CHECKER_MODULE + "011",
			"The Packet store set by the System is not accessible"),

	/** The packet upload failed. */
	PACKET_UPLOAD_FAILED("", "Packet Upload failed"),

	/** The structural validation failed. */
	// Stages - Packet validator Exception error code and message
	STRUCTURAL_VALIDATION_FAILED(PlatformConstants.RPR_PACKET_VALIDATOR_MODULE + "015", "Structural Validation Failed"),

	/** The rpr pvm data not available. */
	RPR_PVM_DATA_NOT_AVAILABLE(PlatformConstants.RPR_PACKET_VALIDATOR_MODULE + "002",
			"Data Not Available in Master DB"),
	/** The rpr pvm update packet deactivated. */
	RPR_PVM_UPDATE_DEACTIVATED(PlatformConstants.RPR_PACKET_VALIDATOR_MODULE + "014", "UIN is Deactivated"),

	/** The rpr pvm identity not found. */
	RPR_PVM_IDENTITY_NOT_FOUND(PlatformConstants.RPR_PACKET_VALIDATOR_MODULE + "003",
			"Attribute Not Available in ID JSON for Master Data Validation"),

	/** The rpr pvm resource not found. */
	RPR_PVM_RESOURCE_NOT_FOUND(PlatformConstants.RPR_PACKET_VALIDATOR_MODULE + "004",
			"Resource Not Found for Master Data Validation"),

	/** The rpr pvm identity invalid. */
	RPR_PVM_IDENTITY_INVALID(PlatformConstants.RPR_PACKET_VALIDATOR_MODULE + "005",
			"Invalid Attribute Value for Master Data Validation"),

	/** The rpr pvm api resouce access failed. */
	RPR_PVM_API_RESOUCE_ACCESS_FAILED(PlatformConstants.RPR_PACKET_VALIDATOR_MODULE + "006",
			"Unable to Access API Resource"),

	/** The rpr pvm base unchecked exception. */
	RPR_PVM_BASE_UNCHECKED_EXCEPTION(PlatformConstants.RPR_PACKET_VALIDATOR_MODULE + "007",
			"ID Schema Validation Failed"),

	/** The rpr pvm base checked exception. */
	RPR_PVM_BASE_CHECKED_EXCEPTION(PlatformConstants.RPR_PACKET_VALIDATOR_MODULE + "013",
			"ID Schema Validation Failed"),

	/** The rpr pvm mandatory field missing. */
	RPR_PVM_MANDATORY_FIELD_MISSING(PlatformConstants.RPR_PACKET_VALIDATOR_MODULE + "010",
			"Mandatory Field Validation Failed"),

	/** The rpr pvm record not matched from sync table. */
	RPR_PVM_RECORD_NOT_MATCHED_FROM_SYNC_TABLE(PlatformConstants.RPR_PACKET_VALIDATOR_MODULE + "011",
			"RID or Registration Type Mismatch"),

	/** The rpr pvm invalid uin. */
	RPR_PVM_INVALID_UIN(PlatformConstants.RPR_PACKET_VALIDATOR_MODULE + "012", "Invalid UIN"),

	/** The rpr pvm document type invalid. */
	RPR_PVM_DOCUMENT_TYPE_INVALID(PlatformConstants.RPR_PACKET_VALIDATOR_MODULE + "008",
			"Invalid Document Type for Document Validation"),

	/** The rpr pvm idjson not found. */
	RPR_PVM_IDJSON_NOT_FOUND(PlatformConstants.RPR_PACKET_VALIDATOR_MODULE + "009", "ID JSON Not Found"),

	/** The rpr pvm applicantdocument validation failed. */
	RPR_PVM_APPLICANTDOCUMENT_VALIDATION_FAILED(PlatformConstants.RPR_PACKET_VALIDATOR_MODULE + "010",
			"Applicant Document Validation Failed"),

	RPR_PVM_PACKET_REJECTED(PlatformConstants.RPR_PACKET_VALIDATOR_MODULE + "011",
			"Rejected by Supervisor"),

	RPR_PVM_PACKET_CREATED_DATE_TIME_EMPTY_OR_NULL(PlatformConstants.RPR_PACKET_VALIDATOR_MODULE + "017",
			"Packet Created Date time is Null or Empty"),

	RPR_PVM_PACKET_CREATED_DATE_TIME_PARSE_EXCEPTION(PlatformConstants.RPR_PACKET_VALIDATOR_MODULE + "018",
			"Packet Created Date time is not in correct format"),

	RPR_PVM_INVALID_ARGUMENT_EXCEPTION(PlatformConstants.RPR_PACKET_VALIDATOR_MODULE + "019",
			"Invalid Argument"),


	/** The packet classification failed. */
	PACKET_CLASSIFICATION_FAILED(PlatformConstants.RPR_PACKET_CLASSIFIER_MODULE + "000", "Packet Classification failed"),

	/** The rpr pcm base unchecked exception. */
	RPR_PCM_BASE_UNCHECKED_EXCEPTION(PlatformConstants.RPR_PACKET_CLASSIFIER_MODULE + "001",
			"Packet Classification failed"),

	/** The rpr pcm base checked exception. */
	RPR_PCM_BASE_CHECKED_EXCEPTION(PlatformConstants.RPR_PACKET_CLASSIFIER_MODULE + "002",
			"Tag generation failed"),

	/** The rpr pcm idobject required field name fetching failed. */
	RPR_PCM_COLLECT_IDOBJECT_FIELD_FAILED(PlatformConstants.RPR_PACKET_CLASSIFIER_MODULE + "003",
			"Getting the required Id object field names from tag generator failed"),

	/** The rpr pcm accessing idobject mapping file failed. */
	RPR_PCM_ACCESSING_IDOBJECT_MAPPING_FILE_FAILED(PlatformConstants.RPR_PACKET_CLASSIFIER_MODULE + "004",
			"Accessing Idobject mapping file filed"),

	/** The rpr pcm unknown schema data type found. */
	RPR_PCM_UNKNOWN_SCHEMA_DATA_TYPE(PlatformConstants.RPR_PACKET_CLASSIFIER_MODULE + "005",
			"Field's schema data type is not supported"),

	/** The rpr pcm schema data type json parsing failed. */
	RPR_PCM_SCHEMA_DATA_TYPE_JSON_PARSING_FAILED(PlatformConstants.RPR_PACKET_CLASSIFIER_MODULE + "006",
			"JSON parsing of field value according to the schema type failed"),

	/** The rpr pcm data object mapping failed. */
	RPR_PCM_DATA_OBJECT_MAPPING_FAILED(PlatformConstants.RPR_PACKET_CLASSIFIER_MODULE + "007",
			"JSON parsing to java object failed"),

	/** The rpr pcm meta info json parsing failed. */
	RPR_PCM_META_INFO_JSON_PARSING_FAILED(PlatformConstants.RPR_PACKET_CLASSIFIER_MODULE + "008",
			"JSON parsing of meta info failed"),

	/** The rpr pcm mapping field name is not present in identity mapping json. */
	RPR_PCM_FIELD_NAME_NOT_AVAILABLE_IN_MAPPING_JSON(PlatformConstants.RPR_PACKET_CLASSIFIER_MODULE + "009",
			"Mapping field name is not present in identity mapping json"),

	/** The rpr pcm value not available in configured language. */
	RPR_PCM_VALUE_NOT_AVAILABLE_IN_CONFIGURED_LANGUAGE(PlatformConstants.RPR_PACKET_CLASSIFIER_MODULE + "010",
			"Value not available in configured language for field"),

	/** The rpr pcm fieldDTO or non string field value is null. */
	RPR_PCM_FIELD_DTO_OR_NON_STRING_FIELD_IS_NULL(PlatformConstants.RPR_PACKET_CLASSIFIER_MODULE + "011",
			"FieldDTO or non string field value is null"),

	/** The rpr pcm sync registration entity not available null. */
	RPR_PCM_SYNC_REGISTRATION_ENTITY_NOT_AVAILABLE(PlatformConstants.RPR_PACKET_CLASSIFIER_MODULE + "012",
			"sync registration entity not available"),

	/** The rpr pcm exception biometrics entry not avaiable in metainfo map. */
	RPR_PCM_EXCEPTION_BIOMETRICS_ENTRY_NOT_AVAILABLE(PlatformConstants.RPR_PACKET_CLASSIFIER_MODULE + "013",
			"ExceptionBiometrics entry not available in metainfo map"),

	/** The rpr pcm operations data entry not avaiable in metainfo map. */
	RPR_PCM_OPERATIONS_DATA_ENTRY_NOT_AVAILABLE(PlatformConstants.RPR_PACKET_CLASSIFIER_MODULE + "014",
			"Operations data entry not avaiable in metainfo map"),

	/** The rpr pcm meta data entry not avaiable in metainfo map. */
	RPR_PCM_META_DATA_ENTRY_NOT_AVAILABLE(PlatformConstants.RPR_PACKET_CLASSIFIER_MODULE + "015",
			"Meta data entry not avaiable in metainfo map"),

	/** The rpr pcm ageGroupRangeMap does not contain group for given age. */
	RPR_PCM_AGE_GROUP_NOT_FOUND(PlatformConstants.RPR_PACKET_CLASSIFIER_MODULE + "016",
			"Age Group Range Map configuration does not contain age group for given age"),

	/** The rpr pcm Captured Registered Devices entry not avaiable in metainfo map. */
	RPR_PCM_CAPTURED_REGISTERED_DEVICES_ENTRY_NOT_AVAILABLE(PlatformConstants.RPR_PACKET_CLASSIFIER_MODULE + "017",
			"Captured registered devices entry not avaiable in metainfo map"),

	/** The rpr pcm exception biometrics entry not avaiable in metainfo map. */
	RPR_PCM_EXCEPTION_BIOMETRICS_APPLICANT_ENTRY_NOT_AVAILABLE(PlatformConstants.RPR_PACKET_CLASSIFIER_MODULE + "018",
			"ExceptionBiometrics applicant entry not available in metainfo map"),

	/** The unsupported encoding. */
	// UIN check - JSON file encoding failed.
	UNSUPPORTED_ENCODING(PlatformConstants.RPR_BIO_DEDUPE_SERVICE_MODULE + "005", "Json Object Parsing Failed"),
	
	OSI_VALIDATION_BIO_TYPE_EXCEPTION(PlatformConstants.RPR_CMD_VALIDATOR_MODULE + "007", "Bio Type Exception"),

	/** The cmd validation failed. */
	// Stages - CMD Exception error code and message
	CMD_VALIDATION_FAILED(PlatformConstants.RPR_CMD_VALIDATOR_MODULE + "000", "CMD Validation Failed"),

	/** The cmd base unchecked exception. */
	CMD_BASE_UNCHECKED_EXCEPTION(PlatformConstants.RPR_CMD_VALIDATOR_MODULE + "001",
			"CMD Validation Failed"),

	/** The cmd base checked exception. */
	CMD_BASE_CHECKED_EXCEPTION(PlatformConstants.RPR_CMD_VALIDATOR_MODULE + "002",
			"CMD Validation Failed"),
	
	/** The operator base unchecked exception. */
	OPERATOR_BASE_UNCHECKED_EXCEPTION(PlatformConstants.RPR_OVM_VALIDATOR_MODULE + "001",
			"operator Validation Failed"),

	/** The operator base checked exception. */
	OPERATOR_BASE_CHECKED_EXCEPTION(PlatformConstants.RPR_OVM_VALIDATOR_MODULE + "002",
			"operator Validation Failed"),
	
	/** The Operator validation failed. */
	OPERATOR_VALIDATION_FAILED(PlatformConstants.RPR_OVM_VALIDATOR_MODULE + "000", "Operator Validation Failed"),
	
	LINK_FOR_USERID_INDIVIDUALID_FAILED_OVM_EXCEPTION(PlatformConstants.RPR_OVM_VALIDATOR_MODULE + "003",
			"Unable to get the IndividualId for UserId"),

	/** The Supervisor base unchecked exception. */
	SUPERVISOR_BASE_UNCHECKED_EXCEPTION(PlatformConstants.RPR_SVM_VALIDATOR_MODULE + "001",
			"Supervisor Validation Failed"),

	/** The Supervisor base checked exception. */
	SUPERVISOR_BASE_CHECKED_EXCEPTION(PlatformConstants.RPR_SVM_VALIDATOR_MODULE + "002",
			"Supervisor Validation Failed"),
	
	/** The Supervisor validation failed. */
	SUPERVISOR_VALIDATION_FAILED(PlatformConstants.RPR_SVM_VALIDATOR_MODULE + "000", "Supervisor Validation Failed"),
	
	LINK_FOR_USERID_INDIVIDUALID_FAILED_SVM_EXCEPTION(PlatformConstants.RPR_SVM_VALIDATOR_MODULE + "003",
			"Unable to get the IndividualId for UserId"),

	/** The INTRODUCER base unchecked exception. */
	INTRODUCER_BASE_UNCHECKED_EXCEPTION(PlatformConstants.RPR_IVM_VALIDATOR_MODULE + "001",
			"INTRODUCER Validation Failed"),

	/** The INTRODUCER base checked exception. */
	INTRODUCER_BASE_CHECKED_EXCEPTION(PlatformConstants.RPR_IVM_VALIDATOR_MODULE + "002",
			"INTRODUCER Validation Failed"),
	
	/** The INTRODUCER validation failed. */
	INTRODUCER_VALIDATION_FAILED(PlatformConstants.RPR_IVM_VALIDATOR_MODULE + "000", "INTRODUCER Validation Failed"),

	/** The INTRODUCER validation failed. */
	INTRODUCER_ON_HOLD(PlatformConstants.RPR_IVM_VALIDATOR_MODULE + "001", "Packet on hold as introducer packet is not processed yet"),

	/** The packet demo dedupe failed. */
	// Stages - Demo-Dedupe error code and message
	PACKET_DEMO_DEDUPE_FAILED(PlatformConstants.RPR_DEMO_DEDUPE_MODULE+"000", "Demo Dedupe Failed"),

	/** The packet demo packet store not accessible. */
	PACKET_DEMO_PACKET_STORE_NOT_ACCESSIBLE(PlatformConstants.RPR_DEMO_DEDUPE_MODULE+"003", "Unable to Access Packet Store"),

	/** The packet bio dedupe cbeff not present. */
	PACKET_BIO_DEDUPE_CBEFF_NOT_PRESENT(PlatformConstants.RPR_BIO_DEDUPE_STAGE_MODULE + "001",
			"Unable to Find Applicant CBEFF for Adult"),

	/** The registration id not found. */
	REGISTRATION_ID_NOT_FOUND(PlatformConstants.RPR_BIO_DEDUPE_STAGE_MODULE + "002",
			"RegistrationId not found for given bio ref Id"),

	/** The rpr bio lost packet multiple match found. */
	RPR_BIO_LOST_PACKET_MULTIPLE_MATCH_FOUND(PlatformConstants.RPR_BIO_DEDUPE_STAGE_MODULE + "003",
			"Multiple Match was Found for the Biometrics Received"),

	RPR_BIO_API_RESOUCE_ACCESS_FAILED(PlatformConstants.RPR_BIO_DEDUPE_STAGE_MODULE + "004",
			"Unable to Access API Resource"),

	/** The abis reference id not found. */
	// stages - Abis Middleware
	ABIS_REFERENCE_ID_NOT_FOUND(PlatformConstants.RPR_ABIS_MIDDLEWARE + "001", "Unable to Find ABIS Reference ID"),

	/** The latest transaction id not found. */
	LATEST_TRANSACTION_ID_NOT_FOUND(PlatformConstants.RPR_ABIS_MIDDLEWARE + "002",
			"Unable to Find Latest Transaction ID"),

	/** The identify requests not found. */
	IDENTIFY_REQUESTS_NOT_FOUND(PlatformConstants.RPR_ABIS_MIDDLEWARE + "003", "Unable to Find Identify Request"),

	/** The abis queue json validation failed. */
	ABIS_QUEUE_JSON_VALIDATION_FAILED(PlatformConstants.RPR_ABIS_MIDDLEWARE + "004",
			"Unable to Find ABIS Connection Properties"),

	/** The unknown exception occured. */
	UNKNOWN_EXCEPTION_OCCURED(PlatformConstants.RPR_ABIS_MIDDLEWARE + "005", "Unknown Exception Found"),

	/** ABIS_BATCH_ID_NOT_FOUND. */
	ABIS_BATCH_ID_NOT_FOUND(PlatformConstants.RPR_ABIS_MIDDLEWARE + "006", "Unable to Find ABIS Batch ID"),

	/** The abis queue not found. */
	ABIS_QUEUE_NOT_FOUND(PlatformConstants.RPR_ABIS_MIDDLEWARE + "007", "Unable to Connect with ABIS Queue"),

	/** The system exception occured. */
	SYSTEM_EXCEPTION_OCCURED(PlatformConstants.RPR_ABIS_MIDDLEWARE + "008", "Internal error occured"),

	DUPLICATE_INSERT_RESPONSE(PlatformConstants.RPR_ABIS_MIDDLEWARE + "009",
			"Duplicate Insert Response received from abis for same request id : "),

	DUPLICATE_IDENTITY_RESPONSE(PlatformConstants.RPR_ABIS_MIDDLEWARE + "010",
			"Duplicate Identify Response received from abis for same request id : "),

	/** The packet bio dedupe failed. */
	// Stages - Bio-Dedupe error code and message
	PACKET_BIO_DEDUPE_FAILED(PlatformConstants.RPR_BIO_DEDUPE_SERVICE_MODULE + "001", "Bio Dedupe Failed"),

	/** The reverse data sync failed. */
	PACKET_BDD_PACKET_STORE_NOT_ACCESSIBLE(PlatformConstants.RPR_BIO_DEDUPE_SERVICE_MODULE + "002",
			"Unable to Access Packet from Packet Store"),

	/** The rpr bio biometric insertion to abis. */
	RPR_BIO_BIOMETRIC_INSERTION_TO_ABIS(PlatformConstants.RPR_BIO_DEDUPE_SERVICE_MODULE + "003",
			"Biometric Insertion Failed in ABIS"),

	/** The rpr abis internal error. */
	RPR_ABIS_INTERNAL_ERROR(PlatformConstants.RPR_BIO_DEDUPE_SERVICE_MODULE + "004", "ABIS Internal Error Occurred"),

	DATASHARE_ERROR(PlatformConstants.RPR_BIO_DEDUPE_SERVICE_MODULE + "005", "Datashare exception occured"),

	/** The reverse data sync failed. */
	// Stages - Packet-Validator error message
	REVERSE_DATA_SYNC_FAILED(PlatformConstants.RPR_PACKET_VALIDATOR_MODULE + "016", "Reverse Data Sync Failed"),

	/** The Biometric-Authentication stage failed. */
	// stages - Biometric-Authentication stage error code and message
	BIOMETRIC_AUTHENTICATION_FAILED(PlatformConstants.RPR_BIOMETRIC_AUTHENTICATION_MODULE,
			"Biometric Authentication Failed"),

	/** The Biometric-Authentication io exception. */
	BIOMETRIC_AUTHENTICATION_IOEXCEPTION(PlatformConstants.RPR_BIOMETRIC_AUTHENTICATION_MODULE + "001", "IO Exception"),

	/** The Biometric-Authentication api resource exception. */
	BIOMETRIC_AUTHENTICATION_API_RESOURCE_EXCEPTION(PlatformConstants.RPR_BIOMETRIC_AUTHENTICATION_MODULE + "002",
			"Unable to Access API Resource"),
	
	BIOMETRIC_AUTHENTICATION_TABLE_NOT_ACCESSIBLE(PlatformConstants.RPR_BIOMETRIC_AUTHENTICATION_MODULE + "004",
			"Unable to Access Registration Table"),
	
	BIOMETRIC_AUTHENTICATION_AUTH_SYSTEM_EXCEPTION(PlatformConstants.RPR_BIOMETRIC_AUTHENTICATION_MODULE + "003",
			"Request could not be processed. Please try again"),

	/** The rpr psj Packet Store not accessible. */
	// Packet scanner job Exception error code and message
	RPR_PSJ_PACKET_STORE_NOT_ACCESSIBLE(PlatformConstants.RPR_PACKET_SCANNER_JOB_MODULE + "002",
			"The Packet store set by the System is not accessible"),

	/** The rpr psj retry folder not accessible. */
	RPR_PSJ_RETRY_FOLDER_NOT_ACCESSIBLE(PlatformConstants.RPR_PACKET_SCANNER_JOB_MODULE + "003",
			"The Retry folder set by the System is not accessible"),

	/** The rpr psj virus scan folder not accessible. */
	RPR_PSJ_VIRUS_SCAN_FOLDER_NOT_ACCESSIBLE(PlatformConstants.RPR_PACKET_SCANNER_JOB_MODULE + "004",
			"The Virus scanner set by the System is not accessible"),

	/** The rpr psj space unavailable for retry folder. */
	RPR_PSJ_SPACE_UNAVAILABLE_FOR_RETRY_FOLDER(PlatformConstants.RPR_PACKET_SCANNER_JOB_MODULE + "005",
			"There is no space available in retry folder to upload the packet"),

	/** The rpr psj virus scan failed. */
	RPR_PSJ_VIRUS_SCAN_FAILED(PlatformConstants.RPR_PACKET_SCANNER_JOB_MODULE + "006", "Virus scan is failed"),

	/** The rpr psj ftp folder not accessible. */
	RPR_PSJ_FTP_FOLDER_NOT_ACCESSIBLE(PlatformConstants.RPR_PACKET_SCANNER_JOB_MODULE + "007",
			"The FTP folder set by the System is not accessible"),

	/** The rpr psj api resouce access failed. */
	RPR_PSJ_API_RESOUCE_ACCESS_FAILED(PlatformConstants.RPR_PACKET_SCANNER_JOB_MODULE + "008",
			"Not able to access the API resource"),

	/** The rpr pdj packet not available. */
	// packet decryption job Exception error code and message
	RPR_PDS_PACKET_NOT_AVAILABLE(PlatformConstants.RPR_PACKET_DECRYPTION_MODULE + "001", "Packet not available"),

	/** The rpr pdj file path not accessible. */
	RPR_PDS_FILE_PATH_NOT_ACCESSIBLE(PlatformConstants.RPR_PACKET_DECRYPTION_MODULE + "002",
			"The File path set by the System is not accessible"),

	/** The rpr pdj packet decryption failure. */
	RPR_PDS_PACKET_DECRYPTION_FAILURE(PlatformConstants.RPR_PACKET_DECRYPTION_MODULE + "003",
			"The Decryption for the Packet has failed"),

	/** The rpr pdj packet decryption failure. */
	RPR_PDS_IO_EXCEPTION(PlatformConstants.RPR_PACKET_DECRYPTION_MODULE + "004",
			"Exception while reading packet inputStream"),

	/** The rpr pds date time exception. */
	RPR_PDS_DATE_TIME_EXCEPTION(PlatformConstants.RPR_PACKET_DECRYPTION_MODULE + "005",
			"Error while parsing packet timestamp"),

	/** The rpr rct unknown resource exception. */
	RPR_RCT_UNKNOWN_RESOURCE_EXCEPTION(PlatformConstants.RPR_REST_CLIENT_MODULE + "001", "Unknown resource provided"),

	/** The rpr mvs invalid file request. */
	RPR_MVS_INVALID_FILE_REQUEST(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "001", "Invalid file requested"),

	/** The rpr mvs file not present. */
	RPR_MVS_FILE_NOT_PRESENT(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "002", "Requested file is not present"),

	/** The rpr mvs invalid status update. */
	RPR_MVS_INVALID_STATUS_UPDATE(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "003", "Invalid status update"),

	/** The rpr mvs invalid field. */
	RPR_MVS_INVALID_FIELD(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "005", " fields can not be empty"),

	/** The rpr mvs no assigned record. */
	RPR_MVS_NO_ASSIGNED_RECORD(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "004", "No Assigned Record Found"),

	/** The rpr mvs file not found in packet store. */
	RPR_MVS_FILE_NOT_FOUND_IN_PACKET_STORE(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "018",
			"Packet Not Found in Packet Store"),

	/** The rpr mvs missing input parameter version. */
	RPR_MVS_MISSING_INPUT_PARAMETER_VERSION(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "019",
			"Missing Input Parameter - version"),

	/** The rpr mvs missing input parameter timestamp. */
	RPR_MVS_MISSING_INPUT_PARAMETER_TIMESTAMP(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "006",
			"Missing Input Parameter - requesttime"),

	/** The rpr mvs missing input parameter id. */
	RPR_MVS_MISSING_INPUT_PARAMETER_ID(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "007",
			"Missing Input Parameter - id"),

	/** The rpr mvs invalid input parameter version. */
	RPR_MVS_INVALID_INPUT_PARAMETER_VERSION(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "008",
			"Invalid Input Parameter - version"),

	/** The rpr mvs invalid input parameter timestamp. */
	RPR_MVS_INVALID_INPUT_PARAMETER_TIMESTAMP(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "009",
			"Invalid Input Parameter - requesttime"),

	/** The rpr mvs invalid input parameter id. */
	RPR_MVS_INVALID_INPUT_PARAMETER_ID(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "010",
			"Invalid Input Parameter - id"),

	/** The rpr mvs invalid argument exception. */
	RPR_MVS_INVALID_ARGUMENT_EXCEPTION(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "011",
			"Invalid Argument Exception"),

	/** The rpr mvs unknown exception. */
	RPR_MVS_UNKNOWN_EXCEPTION(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "012", "Unknown Exception"),

	/** The rpr mvs decode exception. */
	RPR_MVS_DECODE_EXCEPTION(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "013", "Request Decoding Exception"),
	/** The rpr mvs no user id present. */
	RPR_MVS_NO_USER_ID_PRESENT(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "014",
			"User Id does not exists master list"),

	/** The rpr mvs no user id should not empty or null. */
	RPR_MVS_NO_USER_ID_SHOULD_NOT_EMPTY_OR_NULL(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "017",
			"User Id should not empty or null "),

	/** The rpr mvs no registration id should not empty or null. */
	RPR_MVS_NO_RID_SHOULD_NOT_EMPTY_OR_NULL(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "022",
			"Registration Id should not empty or null "),

	/** The rpr mvs no matched ref id found for given ref id. */
	RPR_MVS_NO_MATCHEDRID_FOUND_FOR_GIVEN_RID(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "023",
			"No matched reference id found for given RID"),
	/** The rpr mvs user status not active. */
	RPR_MVS_USER_STATUS_NOT_ACTIVE(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "015",
			"User is not in ACTIVE status"),

	/** The rpr mvs no match type present. */
	RPR_MVS_NO_MATCH_TYPE_PRESENT(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "020", "Match Type is Invalid"),

	/** The rpr mvs reg id should not empty or null. */
	RPR_MVS_REG_ID_SHOULD_NOT_EMPTY_OR_NULL(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "016",
			"Reg Id should not be null or empty"),

	/** The rpr manual verification rejected. */
	RPR_MANUAL_VERIFICATION_REJECTED(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "021",
			"Manual verification rejected"),

	/** The rpr manual verification rejected. */
	RPR_MANUAL_VERIFICATION_RESEND(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "025",
			"Manual verification resend to queue."),

	/** The rpr table not accessible. */
	RPR_TABLE_NOT_ACCESSIBLE(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "022",
			"TablenotAccessibleException in Manual verification"),

	/** The rpr table not accessible. */
	RPR_INVALID_MESSSAGE(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "023",
			"Invalid message received from queue"),

	/** The rpr table not accessible. */
	RPR_INVALID_RID_FOUND(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "024",
			"Multiple rids found for a reference id"),
	
	MANUAL_VERIFICATION_FAILED(PlatformConstants.RPR_MANUAL_ADJUDICATION_MODULE + "025",
			"Manual adjudication failed"),

	/** The rpr tem not found. */
	// Registration processor Message sender Exception error code
	RPR_TEM_NOT_FOUND(PlatformConstants.RPR_MESSAGE_SENDER_TEMPLATE + "001", "Template was Not Found"),

	/** The rpr tem processing failure. */
	RPR_TEM_PROCESSING_FAILURE(PlatformConstants.RPR_MESSAGE_SENDER_TEMPLATE + "002",
			"The Processing of Template Failed "),
	/** The rpr tem packet store not accessible. */
	RPR_TEM_PACKET_STORE_NOT_ACCESSIBLE(PlatformConstants.RPR_MESSAGE_SENDER_TEMPLATE + "003",
			"The Packet store set by the System is not accessible"),
	/** The rpr sms template generation failure. */
	RPR_SMS_TEMPLATE_GENERATION_FAILURE(PlatformConstants.RPR_MESSAGE_SENDER_TEMPLATE + "001",
			"Template Generation failed"),

	/** The rpr sms phone number not found. */
	RPR_SMS_PHONE_NUMBER_NOT_FOUND(PlatformConstants.RPR_MESSAGE_SENDER_TEMPLATE + "002", "Phone number was not found"),

	/** The rpr eml emailid not found. */
	RPR_EML_EMAILID_NOT_FOUND(PlatformConstants.RPR_MESSAGE_SENDER_TEMPLATE + "001", "Email Id was not found"),

	/** The rpr tem configuration not found. */
	RPR_TEM_CONFIGURATION_NOT_FOUND(PlatformConstants.RPR_MESSAGE_SENDER_TEMPLATE + "003",
			"The Configuration and Language code not found"),

	/** The rpr notification failed for lost. */
	RPR_NOTIFICATION_FAILED_FOR_LOST(PlatformConstants.RPR_MESSAGE_SENDER_TEMPLATE + "004",
			"Unable to Send Notification - UIN was not found for the Lost Packet"),

	/** The rpr template configuration not found. */
	RPR_TEMPLATE_CONFIGURATION_NOT_FOUND(PlatformConstants.RPR_MESSAGE_SENDER_TEMPLATE + "005",
			"Template configuration and language not found"),

	/** The rpr message sender stage failed. */
	RPR_MESSAGE_SENDER_STAGE_FAILED(PlatformConstants.RPR_MESSAGE_SENDER_TEMPLATE + "006",
			"Message Sender Stage failed"),

	/** The rpr email phone template notification missing. */
	RPR_EMAIL_PHONE_TEMPLATE_NOTIFICATION_MISSING(PlatformConstants.RPR_MESSAGE_SENDER_TEMPLATE + "007",
			"Email ID or Phone or Template or Notification Type is Missing"),

	/** The rpr message sender email failed. */
	RPR_MESSAGE_SENDER_EMAIL_FAILED(PlatformConstants.RPR_MESSAGE_SENDER_TEMPLATE + "008", "Email Failed"),

	/** The rpr message sender sms failed. */
	RPR_MESSAGE_SENDER_SMS_FAILED(PlatformConstants.RPR_MESSAGE_SENDER_TEMPLATE + "009", "SMS failed"),

	/** The rpr pum packet not found exception. */
	RPR_PUM_PACKET_NOT_FOUND_EXCEPTION(PlatformConstants.RPR_PACKET_UPLOADER_MODULE + "001",
			"Packet not found in LANDING_ZONE "),

	/** The rpr pum packet deletion info. */
	RPR_PUM_PACKET_DELETION_INFO(PlatformConstants.RPR_PACKET_UPLOADER_MODULE + "002",
			"File is Already exists in File Store And its now Deleted from landing zone"),

	/** The rpr pum packet store not accessible. */
	RPR_PUM_PACKET_STORE_NOT_ACCESSIBLE(PlatformConstants.RPR_PACKET_UPLOADER_MODULE + "003",
			"The Packet store set by the System is not accessible"),

	/** The prp pkr packet virus scan failed. */
	RPR_PUM_PACKET_VIRUS_SCAN_FAILED(PlatformConstants.RPR_PACKET_UPLOADER_MODULE + "004",
			"The Registration Packet virus scan failed"),

	/** The rpr pum packet virus scanner service failed. */
	RPR_PUM_PACKET_VIRUS_SCANNER_SERVICE_FAILED(PlatformConstants.RPR_PACKET_UPLOADER_MODULE + "005",
			"Virus scanner service failed"),

	/** The rpr pkm file not found in source. */
	RPR_PUM_JSCH_NOT_CONNECTED(PlatformConstants.RPR_PACKET_UPLOADER_MODULE + "006", "The JSCH connection failed"),

	/** The rpr pkm file not found in source. */
	RPR_PUM_NGINX_ACCESS_FAILED(PlatformConstants.RPR_PACKET_UPLOADER_MODULE + "007",
			"Could not get the packet from nginx Url"),

	/** The rpr pum packet not yet sync. */
	RPR_PUM_PACKET_NOT_YET_SYNC(PlatformConstants.RPR_PACKET_UPLOADER_MODULE + "008",
			"Registration packet is not in Sync with Sync table"),

	/** The rpr pum packet decryption failed. */
	RPR_PUM_PACKET_DECRYPTION_FAILED(PlatformConstants.RPR_PACKET_UPLOADER_MODULE + "009",
			"Registration packet decryption failed"),

	/** The rpr pum packet deletion failed. */
	RPR_PUM_PACKET_DELETION_FAILED(PlatformConstants.RPR_PACKET_UPLOADER_MODULE + "010",
			"packet upload failed during cleanup"),

	/** The rpr pum packet archival failed. */
	RPR_PUM_PACKET_ARCHIVAL_FAILED(PlatformConstants.RPR_PACKET_UPLOADER_MODULE + "010",
			"packet upload failed during archival"),

	/** The rpr pum packet not found exception. */
	RPR_PUM_PACKET_UPLOAD_FAILURE(PlatformConstants.RPR_PACKET_UPLOADER_MODULE + "011",
			"Failure in uploading the packet to Packet Store"),
    
	RPR_PUM_PACKET_RETRY_CNT_FAILURE(PlatformConstants.RPR_PACKET_UPLOADER_MODULE + "012",
            "packet upload failed due to Retry count has exceeded the max limit specified"),

	/** The rpr bdd abis internal error. */
	RPR_BDD_ABIS_INTERNAL_ERROR(PlatformConstants.RPR_BIO_DEDUPE_SERVICE_MODULE + "001",
			"ABIS for the Reference ID and Request ID threw an Internal Error"),

	/** The rpr bdd abis abort. */
	RPR_BDD_ABIS_ABORT(PlatformConstants.RPR_BIO_DEDUPE_SERVICE_MODULE + "002",
			"ABIS for the Reference ID and Request ID was Abort"),

	/** The rpr bdd unexcepted error. */
	RPR_BDD_UNEXCEPTED_ERROR(PlatformConstants.RPR_BIO_DEDUPE_SERVICE_MODULE + "003",
			"ABIS for the Reference ID and Request ID was Not able to Access Biometric Data"),

	/** The rpr bdd unable to serve request. */
	RPR_BDD_UNABLE_TO_SERVE_REQUEST(PlatformConstants.RPR_BIO_DEDUPE_SERVICE_MODULE + "004",
			"ABIS for the Reference ID and Request ID was Unable to Execute the Request"),

	/** The unknown exception. */
	UNKNOWN_EXCEPTION(PlatformConstants.RPR_BIO_DEDUPE_SERVICE_MODULE + "005",
			"un expected exception occured in biodedupe service"),

	/** The connection to MOSIP underlying queue not available. */
	RPR_MQI_CONNECTION_UNAVAILABLE(PlatformConstants.RPR_MOSIP_QUEUE_MODULE + "001",
			"The connection to underlying queue not available"),

	/** The rpr mqi unable to send to queue. */
	RPR_MQI_UNABLE_TO_SEND_TO_QUEUE(PlatformConstants.RPR_MOSIP_QUEUE_MODULE + "003",
			"Unable to send message to to queue"),

	/** The rpr mqi unable to consume from queue. */
	RPR_MQI_UNABLE_TO_CONSUME_FROM_QUEUE(PlatformConstants.RPR_MOSIP_QUEUE_MODULE + "004",
			"Unable to consume message from queue"),

	/** The rpr mqi no files found in queue. */
	RPR_MQI_NO_FILES_FOUND_IN_QUEUE(PlatformConstants.RPR_MOSIP_QUEUE_MODULE + "005",
			"There is no file available in queue"),

	/** The rpr mqi invalid connection. */
	RPR_MQI_INVALID_CONNECTION(PlatformConstants.RPR_MOSIP_QUEUE_MODULE + "002",
			"Connection not obtained from ConnectionFactory"),

	/** The missing input parameter. */
	RPR_BDD_MISSING_INPUT_PARAMETER(PlatformConstants.RPR_BIO_DEDUPE_SERVICE_MODULE + "005",
			"Missing Input Parameter - %s"),

	/** The invalid input parameter. */
	RPR_BDD_INVALID_INPUT_PARAMETER(PlatformConstants.RPR_BIO_DEDUPE_SERVICE_MODULE + "006",
			"Invalid Input Parameter - %s"),

	/** The data validation failed. */
	RPR_BDD_DATA_VALIDATION_FAILED(PlatformConstants.RPR_BIO_DEDUPE_SERVICE_MODULE + "007",
			"Input Data Validation Failed"),

	/** The rpr bdd json mapping exception. */
	RPR_BDD_JSON_MAPPING_EXCEPTION(PlatformConstants.RPR_BIO_DEDUPE_SERVICE_MODULE + "008",
			"Json Data Mapping Exception"),

	/** The rpr bdd json parsing exception. */
	RPR_BDD_JSON_PARSING_EXCEPTION(PlatformConstants.RPR_BIO_DEDUPE_SERVICE_MODULE + "009",
			"Json Data Parsing Exception"),

	/** The rpr bdd unknown exception. */
	RPR_BDD_UNKNOWN_EXCEPTION(PlatformConstants.RPR_BIO_DEDUPE_SERVICE_MODULE + "010", "Unknown Exception Occurs"),

	/** The rpr mvs file not present. */
	RPR_BDD_FILE_NOT_PRESENT(PlatformConstants.RPR_BIO_DEDUPE_SERVICE_MODULE + "011", "Requested file is not present"),

	/** *** System Exception ****. */

	RPR_SYS_UNEXCEPTED_EXCEPTION(PlatformConstants.RPR_SYSTEM_EXCEPTION + "001", "Unexpected exception"),

	/** The rpr sys bad gateway. */
	RPR_SYS_BAD_GATEWAY(PlatformConstants.RPR_SYSTEM_EXCEPTION + "002", "Bad Gateway"),

	/** The rpr sys service unavailable. */
	RPR_SYS_SERVICE_UNAVAILABLE(PlatformConstants.RPR_SYSTEM_EXCEPTION + "003", "Service Unavailable"),

	/** The rpr sys server error. */
	RPR_SYS_SERVER_ERROR(PlatformConstants.RPR_SYSTEM_EXCEPTION + "004", "Internal Server Error"),

	/** The rpr sys timeout exception. */
	RPR_SYS_TIMEOUT_EXCEPTION(PlatformConstants.RPR_SYSTEM_EXCEPTION + "005", "Timeout Error"),

	/** The rpr sys identity json mapping exception. */
	RPR_SYS_IDENTITY_JSON_MAPPING_EXCEPTION(PlatformConstants.RPR_SYSTEM_EXCEPTION + "006",
			"Error while mapping Identity Json"),

	/** The rpr sys instantiation exception. */
	RPR_SYS_INSTANTIATION_EXCEPTION(PlatformConstants.RPR_SYSTEM_EXCEPTION + "007",
			"Error while creating object of JsonValue class"),

	/** The rpr sys no such field exception. */
	RPR_SYS_NO_SUCH_FIELD_EXCEPTION(PlatformConstants.RPR_SYSTEM_EXCEPTION + "008", "Could not find the field"),

	/** The rpr sys json parsing exception. */
	RPR_SYS_JSON_PARSING_EXCEPTION(PlatformConstants.RPR_SYSTEM_EXCEPTION + "009", "Error while parsing Json"),

	/** The rpr sys unable to convert stream to bytes. */
	RPR_SYS_UNABLE_TO_CONVERT_STREAM_TO_BYTES(PlatformConstants.RPR_SYSTEM_EXCEPTION + "010",
			"Error while converting inputstream to bytes"),

	/** The rpr sys parsing date exception. */
	RPR_SYS_PARSING_DATE_EXCEPTION(PlatformConstants.RPR_SYSTEM_EXCEPTION + "011", "Error while parsing date "),

	/** The rpr sys io exception. */
	RPR_SYS_IO_EXCEPTION(PlatformConstants.RPR_SYSTEM_EXCEPTION + "012", "IO EXCEPTION "),

	/** The rpr sys data access exception. */
	RPR_SYS_DATA_ACCESS_EXCEPTION(PlatformConstants.RPR_SYSTEM_EXCEPTION + "013", "Data Access Exception"),

	/** The rpr sys api resource exception. */
	RPR_SYS_API_RESOURCE_EXCEPTION(PlatformConstants.RPR_SYSTEM_EXCEPTION + "014", "API Resource Exception"),

	/** The rpr sys illegal access exception. */
	RPR_SYS_ILLEGAL_ACCESS_EXCEPTION(PlatformConstants.RPR_SYSTEM_EXCEPTION + "015", "ILLEGAL ACCESS Exception"),

	/** The rpr sys Invocation target exception. */
	RPR_SYS_INVOCATION_TARGET_EXCEPTION(PlatformConstants.RPR_SYSTEM_EXCEPTION + "016", "Invocation target Exception"),

	/** The rpr sys Invocation target exception. */
	RPR_SYS_INTROSPECTION_EXCEPTION(PlatformConstants.RPR_SYSTEM_EXCEPTION + "017", "Introspection Exception"),

	/** The packet store not accessible. */
	OBJECT_STORE_NOT_ACCESSIBLE(PlatformConstants.RPR_SYSTEM_EXCEPTION + "018",
			"Unable to Access Object Store"),

	/** The packet tags copying failed. */
	RPR_SYS_PACKET_TAGS_COPYING_FAILED(PlatformConstants.RPR_SYSTEM_EXCEPTION + "019",
			"Packet tags copying to message event failed"),

	/** The message expired. */
	RPR_SYS_MESSAGE_EXPIRED(PlatformConstants.RPR_SYSTEM_EXCEPTION + "020",
			"Message expired as per the last hop timestamp"),

	/** The stage processing failed. */
	RPR_SYS_STAGE_PROCESSING_FAILED(PlatformConstants.RPR_SYSTEM_EXCEPTION + "021",
			"Stage processing of message failed with an exception"),

	/** The rpr sys connection exception. */
	RPR_SYS_QUEUE_CONNECTION_EXCEPTION(PlatformConstants.RPR_SYSTEM_EXCEPTION + "022",
			"Error while connecting to active mq"),



	// Cbeff Util Exceptions
	/** The rpr utl biometric tag match. */
	RPR_UTL_BIOMETRIC_TAG_MATCH(PlatformConstants.RPR_UTIL + "001", "Both Files have same biometrics"),

	/** The rpr utl cbeff version mismatch. */
	RPR_UTL_CBEFF_VERSION_MISMATCH(PlatformConstants.RPR_UTIL + "002", "Both Files have different versions"),

	/** The rpr utl digital sign exception. */
	RPR_UTL_DIGITAL_SIGN_EXCEPTION(PlatformConstants.RPR_UTIL + "003", "Failed to generate digital signature"),

	/** The rpr utl language exception. */
	RPR_UTL_LANGUAGE_EXCEPTION(PlatformConstants.RPR_UTIL + "004", "Failed to retrieve language details."),

	/** The rpr prt pdf not generated. */
	// Printing stage exceptions
	RPR_PRT_PDF_NOT_GENERATED(PlatformConstants.RPR_PRINTING_MODULE + "001", "Error while generating PDF for UIN Card"),

	/** The rpr prt uin not found in database. */
	RPR_PRT_UIN_NOT_FOUND_IN_DATABASE(PlatformConstants.RPR_PRINTING_MODULE + "002", "UIN not found in database"),

	/** The rpr prt pdf generation failed. */
	RPR_PRT_PDF_GENERATION_FAILED(PlatformConstants.RPR_PRINTING_MODULE + "003", "PDF Generation Failed"),

	/** The rpr prt queue connection null. */
	RPR_PRT_QUEUE_CONNECTION_NULL(PlatformConstants.RPR_PRINTING_MODULE + "004", "Queue connection is null"),

	/** The rpr prt qrcode not generated. */
	RPR_PRT_QRCODE_NOT_GENERATED(PlatformConstants.RPR_PRINTING_MODULE + "005", "Error while generating QR Code"),

	/** The rpr prt applicant photo not set. */
	RPR_PRT_APPLICANT_PHOTO_NOT_SET(PlatformConstants.RPR_PRINTING_MODULE + "006",
			"Error while setting applicant photo"),

	/** The rpr prt qrcode not set. */
	RPR_PRT_QRCODE_NOT_SET(PlatformConstants.RPR_PRINTING_MODULE + "007", "Error while setting qrCode for uin card"),

	/** The rpr prt idrepo response null. */
	RPR_PRT_IDREPO_RESPONSE_NULL(PlatformConstants.RPR_PRINTING_MODULE + "008", "ID Repo response is null"),

	/** The rpr prt idrepo documents absent. */
	RPR_PRT_IDREPO_DOCUMENT_ABSENT(PlatformConstants.RPR_PRINTING_MODULE + "009", "ID Repo response has no documents"),

	/** The print and postal acknowledment generation failed. */
	RPR_PRT_PRINT_POST_ACK_FAILED(PlatformConstants.RPR_PRINTING_MODULE + "010",
			"Error while getting response from Print and Postal Service Provider"),

	/** The print validation failed. */
	RPR_PRT_DATA_VALIDATION_FAILED(PlatformConstants.RPR_PRINTING_MODULE + "011", "Error while print data validation"),

	/** The rpr prt cardtype validation failed. */
	RPR_PRT_CARDTYPE_VALIDATION_FAILED(PlatformConstants.RPR_PRINTING_MODULE + "012",
			"Invalid CardType : Enter UIN or MASKED_UIN"),

	/** The rpr prt idtype validation failed. */
	RPR_PRT_IDTYPE_VALIDATION_FAILED(PlatformConstants.RPR_PRINTING_MODULE + "013",
			"Invalid IdType : Enter UIN or VID or RID"),

	/** The rpr prt uin validation failed. */
	RPR_PRT_UIN_VALIDATION_FAILED(PlatformConstants.RPR_PRINTING_MODULE + "014", "UIN is not valid"),

	/** The rpr prt vid validation failed. */
	RPR_PRT_VID_VALIDATION_FAILED(PlatformConstants.RPR_PRINTING_MODULE + "015", "VID is not valid"),

	/** The rpr prt rid validation failed. */
	RPR_PRT_RID_VALIDATION_FAILED(PlatformConstants.RPR_PRINTING_MODULE + "016", "RID is not valid"),

	/** The rpr prt vid not generated. */
	RPR_PRT_VID_NOT_GENERATED(PlatformConstants.RPR_PRINTING_MODULE + "017", "Error while creating VID"),

	/** The rpr prt vid exception. */
	RPR_PRT_VID_EXCEPTION(PlatformConstants.RPR_PRINTING_MODULE + "018",
			"Could not generate/regenerate VID as per policy,Please use existing VID"),

	/** The missing input parameter. */
	RPR_PRT_MISSING_INPUT_PARAMETER(PlatformConstants.RPR_PRINTING_MODULE + "019", "Missing Input Parameter - %s"),

	/** The missing input parameter. */
	RPR_PRT_INVALID_INPUT_PARAMETER(PlatformConstants.RPR_PRINTING_MODULE + "019", "Invalid Input Parameter - %s"),

	/** The missing input parameter. */
	RPR_PRT_PDF_NOT_ADDED(PlatformConstants.RPR_PRINTING_MODULE + "020",
			"Pdf was not added to queue due to queue failure"),

	/** The missing input parameter. */
	RPR_PRT_RESEND_UIN_CARD(PlatformConstants.RPR_PRINTING_MODULE + "021", "Re-Send uin card for printing"),

	/** The rpr prt qr code generation error. */
	RPR_PRT_QR_CODE_GENERATION_ERROR(PlatformConstants.RPR_PRINTING_MODULE + "022", "Error while QR Code Generation"),

	/** The rpr prt vid creation error. */
	RPR_PRT_VID_CREATION_ERROR(PlatformConstants.RPR_PRINTING_MODULE + "023", "Error while creating VID"),


	RPR_PRT_PDF_SIGNATURE_EXCEPTION(PlatformConstants.RPR_PRINTING_MODULE + "024", "PDF Signature error"),
	
	RPR_PRT_PRINT_REQUEST_FAILED(PlatformConstants.RPR_PRINTING_MODULE + "025", "Print request failed"),

	RPR_PRT_API_RESOUCE_ACCESS_FAILED(PlatformConstants.RPR_PRINTING_MODULE + "026",
			"Unable to Access API Resource"),

	/** The rpr prt vid exception. */
	RPR_PRT_VID_NOT_AVAILABLE_EXCEPTION(PlatformConstants.RPR_PRINTING_MODULE + "027",
			"vid not available"),

	/** The rpr prt print issuer not found in property. */
	RPR_PRT_ISSUER_NOT_FOUND_IN_PROPERTY(PlatformConstants.RPR_PRINTING_MODULE + "028", "Print issuer property is not found"),

	RPR_PRT_PARSING_ADDITIONAL_CRED_CONFIG(PlatformConstants.RPR_PRINTING_MODULE + "029", "issue with metaInfoFields in credential partner profiles."),

	/** The rpr rgs registration connector not accessible. */
	RPR_RGS_REGISTRATION_CONNECTOR_NOT_ACCESSIBLE("", "Registration connector stage is not accessible "),

	/** The rpr pvm packet store not accessible. */
	RPR_PVM_PACKET_STORE_NOT_ACCESSIBLE(PlatformConstants.RPR_PACKET_VALIDATOR_MODULE + "001",
			"The Packet store set by the System is not accessible"),

	/** The rpr ugs packet store not accessible. */
	RPR_UGS_PACKET_STORE_NOT_ACCESSIBLE(PlatformConstants.RPR_UIN_GENERATOR_STAGE + "001",
			"The Packet store set by the System is not accessible"),
	/** The rpr ugs json parser error. */
	RPR_UGS_JSON__PARSER_ERROR(PlatformConstants.RPR_UIN_GENERATOR_STAGE + "002", "Error while parsing Json"),
	/** The rpr ugs api resource exception. */
	RPR_UGS_API_RESOURCE_EXCEPTION(PlatformConstants.RPR_UIN_GENERATOR_STAGE + "003",
			"Not able to access the API resource"),
	/** The rpr ugs io exception. */
	RPR_UGS_IO_EXCEPTION(PlatformConstants.RPR_UIN_GENERATOR_STAGE + "004", "IO exception"),
	/** The rpr ugs vid exception. */
	RPR_UGS_VID_EXCEPTION(PlatformConstants.RPR_UIN_GENERATOR_STAGE + "005", "VID status is not active"),

	/** The rpr ugs uin update failure. */
	RPR_UGS_UIN_UPDATE_FAILURE(PlatformConstants.RPR_UIN_GENERATOR_STAGE + "006", "UIN updation failure"),

	/** The uin already activated. */
	UIN_ALREADY_ACTIVATED(PlatformConstants.RPR_UIN_GENERATOR_STAGE + "007", "UIN is already Activated"),

	/** The uin already deactivated. */
	UIN_ALREADY_DEACTIVATED(PlatformConstants.RPR_UIN_GENERATOR_STAGE + "008", "UIN already deactivated"),

	/** The uin activated failed. */
	UIN_ACTIVATED_FAILED(PlatformConstants.RPR_UIN_GENERATOR_STAGE + "009", "UIN Activation Failed"),

	/** The uin reactivation failed. */
	UIN_REACTIVATION_FAILED(PlatformConstants.RPR_UIN_GENERATOR_STAGE + "010", "UIN Reactivation  failed"),

	/** The uin deactivation failed. */
	UIN_DEACTIVATION_FAILED(PlatformConstants.RPR_UIN_GENERATOR_STAGE + "011", "UIN Deactivation  failed"),

	/** The link rid for lost packet failed. */
	LINK_RID_FOR_LOST_PACKET_FAILED(PlatformConstants.RPR_UIN_GENERATOR_STAGE + "012",
			"UIn not found the the matched RID"),

	/** The uin generation failed. */
	UIN_GENERATION_FAILED(PlatformConstants.RPR_UIN_GENERATOR_STAGE + "013", "UIN Generation failed"),

	VID_CREATION_FAILED(PlatformConstants.RPR_UIN_GENERATOR_STAGE + "014", "VID creation failed"),


	DRAFT_CHECK_FAILED(PlatformConstants.RPR_UIN_GENERATOR_STAGE + "015", "Could not check if draft is present or not"),

	/** The rpr pgs file not present. */
	RPR_PGS_FILE_NOT_PRESENT(PlatformConstants.RPR_PACKET_GENERATOR_MODULE + "001",
			"The Packet store set by the System is not accessible"),

	/** The rpr pgs invalid key illegal argument. */
	RPR_PGS_INVALID_KEY_ILLEGAL_ARGUMENT(PlatformConstants.RPR_PACKET_GENERATOR_MODULE + "002",
			"The key is invalid or illegal argument"),

	/** The rpr pgs api resource not available. */
	RPR_PGS_API_RESOURCE_NOT_AVAILABLE(PlatformConstants.RPR_PACKET_GENERATOR_MODULE + "003",
			"The Api resource is not available"),

	/** The rpr pgs reg base exception. */
	RPR_PGS_REG_BASE_EXCEPTION(PlatformConstants.RPR_PACKET_GENERATOR_MODULE + "004", "reg Based checked exception"),

	/** The rpr pgs json processing exception. */
	RPR_PGS_JSON_PROCESSING_EXCEPTION(PlatformConstants.RPR_PACKET_GENERATOR_MODULE + "005",
			"Exception while parsing object to JSON"),

	/** The rpr pgs json validator error code. */
	RPR_PGS_JSON_VALIDATOR_ERROR_CODE(PlatformConstants.RPR_PACKET_GENERATOR_MODULE + "006",
			"Exception while validating ID json file"),

	/** The rpr pgs encryptor invlaid data exception. */
	RPR_PGS_ENCRYPTOR_INVLAID_DATA_EXCEPTION(PlatformConstants.RPR_PACKET_GENERATOR_MODULE + "007",
			"Exception occured while encryting the data"),

	/** The rpr pgs encryptor invlaid key exception. */
	RPR_PGS_ENCRYPTOR_INVLAID_KEY_EXCEPTION(PlatformConstants.RPR_PACKET_GENERATOR_MODULE + "008",
			"Exception occured while encryting the packet Invalid Key"),

	/** The rpr pgs packet meta convertor exception. */
	RPR_PGS_PACKET_META_CONVERTOR_EXCEPTION(PlatformConstants.RPR_PACKET_GENERATOR_MODULE + "009",
			"Packet meta info converter error"),

	/** The missing input parameter. */
	RPR_PGS_MISSING_INPUT_PARAMETER(PlatformConstants.RPR_PACKET_GENERATOR_MODULE + "010",
			"Missing Input Parameter - %s"),

	/** The invalid input parameter. */
	RPR_PGS_INVALID_INPUT_PARAMETER(PlatformConstants.RPR_PACKET_GENERATOR_MODULE + "011",
			"Invalid Input Parameter - %s"),
	/** The data validation failed. */
	RPR_PGS_DATA_VALIDATION_FAILED(PlatformConstants.RPR_PACKET_GENERATOR_MODULE + "012",
			"Input Data Validation Failed"),

	/** The rpr pgs vid exception. */
	RPR_PGS_VID_EXCEPTION(PlatformConstants.RPR_PACKET_GENERATOR_MODULE + "013",
			"Could not generate/regenerate VID as per policy,Please use existing VID"),

	/** The rpr pgs vid creation exception. */
	RPR_PGS_VID_CREATION_EXCEPTION(PlatformConstants.RPR_PACKET_GENERATOR_MODULE + "014", "VID creation Exception"),

	/** The rpr pgs id object exception. */
	RPR_PGS_ID_OBJECT_EXCEPTION(PlatformConstants.RPR_PACKET_GENERATOR_MODULE + "015", "ID Object Validation failed"),

	/** The rpr pgs id type exception. */
	RPR_PGS_ID_TYPE_EXCEPTION(PlatformConstants.RPR_PACKET_GENERATOR_MODULE + "016",
			"Invalid Input Value - ID Type: Enter UIN or RID"),

	/** The rpr pgs contact type exception. */
	RPR_PGS_CONTACT_TYPE_EXCEPTION(PlatformConstants.RPR_PACKET_GENERATOR_MODULE + "017",
			"Invalid Input Value - Contact Type : Enter Email or Phone"),
	/** The rpr pgs name exception. */
	RPR_PGS_NAME_EXCEPTION(PlatformConstants.RPR_PACKET_GENERATOR_MODULE + "018",
			"Invalid Input Value - Name cannot be NULL or Empty"),
	/** The rpr pgs contactvalue exception. */
	RPR_PGS_CONTACTVALUE_EXCEPTION(PlatformConstants.RPR_PACKET_GENERATOR_MODULE + "019",
			"Invalid Input Value - contactValue cannot be NULL or Empty"),
	/** The rpr pgs postalcode exception. */
	RPR_PGS_POSTALCODE_EXCEPTION(PlatformConstants.RPR_PACKET_GENERATOR_MODULE + "020",
			"Invalid Input Value - postalCode cannot be NULL or Empty"),

	/** The rpr pgs no records exception. */
	RPR_PGS_NO_RECORDS_EXCEPTION(PlatformConstants.RPR_PACKET_GENERATOR_MODULE + "021", "No Records Found"),

	/** The rpr pgs no records exception. */
	RPR_PGS_MULTIPLE_RECORDS_EXCEPTION(PlatformConstants.RPR_PACKET_GENERATOR_MODULE + "022", "Multiple Records Found"),

	/** The rpr ugs api resource exception. */
	RPR_PGS_API_RESOURCE_EXCEPTION(PlatformConstants.RPR_PACKET_GENERATOR_MODULE + "023",
			"Not able to access the API resource"),
	/** The rpr ugs io exception. */
	RPR_PGS_IO_EXCEPTION(PlatformConstants.RPR_PACKET_GENERATOR_MODULE + "024", "IO exception"),

	/** The rpr pis identity not found. */
	RPR_PGS_IDENTITY_NOT_FOUND(PlatformConstants.RPR_PACKET_GENERATOR_MODULE + "025",
			"Unable to Find Identity Field in ID JSON"),

	/** Idrepo draft creation exception. */
	IDREPO_DRAFT_EXCEPTION(PlatformConstants.RPR_PACKET_GENERATOR_MODULE + "026",
			"Exception while updating idrepo draft"),

	/** invalid token present in header. */
	RPR_AUT_INVALID_TOKEN(PlatformConstants.RPR_AUTHENTICATION + "01", "Invalid Token Present"),

	/** Access denied for the token present. */
	RPR_AUT_ACCESS_DENIED(PlatformConstants.RPR_AUTHENTICATION + "02", "Access Denied For Role - %s"),

	/** Reprocessor Stage Failed. */
	REPROCESSOR_VERTICLE_FAILED(PlatformConstants.RPR_REPROCESSOR_VERTICLE + "000", "Reprocessor Verticle Failed"),

	RPR_WIA_DATE_TIME_EXCEPTION(PlatformConstants.RPR_WORKFLOW_INTERNAL_ACTION + "000",
			"Error while parsing event or resume timestamp"),

	RPR_WORKFLOW_INTERNAL_ACTION_FAILED(PlatformConstants.RPR_WORKFLOW_INTERNAL_ACTION + "001",
			"Work flow internal action failed"),

	RPR_WIA_UNKNOWN_WORKFLOW_ACTION(PlatformConstants.RPR_WORKFLOW_INTERNAL_ACTION + "002", "Unknown Exception"),

	RPR_WIA_ADDITIONALINFOPROCESS_NOT_FOUND(PlatformConstants.RPR_WORKFLOW_INTERNAL_ACTION + "003",
			"AdditionalInfoProcess not found"),

	RPR_WIA_ADDITIONALINFOPROCESS_CANNOT_REQUEST(PlatformConstants.RPR_WORKFLOW_INTERNAL_ACTION + "004",
			"AdditionalInfoProcess cannot be paused for one more additional info so it is failed"),

	RPR_WAA_MISSING_INPUT_PARAMETER(PlatformConstants.RPR_WORKFLOW_ACTION_API + "000",
			"Missing Request Value - %s"),

	RPR_WAA_INVALID_INPUT_PARAMETER(PlatformConstants.RPR_WORKFLOW_ACTION_API + "001",
			"Invalid Request Value - %s"),

    RPR_WIN_MISSING_INPUT_PARAMETER(PlatformConstants.RPR_WORKFLOW_INSTANCE_API + "000",
            "Missing Request Value - %s"),

    RPR_WIN_INVALID_INPUT_PARAMETER(PlatformConstants.RPR_WORKFLOW_INSTANCE_API + "001",
            "Invalid Request Value - %s"),

	RPR_WAA_UNKNOWN_EXCEPTION(PlatformConstants.RPR_WORKFLOW_ACTION_API + "002", "Unknown Exception"),

    RPR_WIN_UNKNOWN_EXCEPTION(PlatformConstants.RPR_WORKFLOW_INSTANCE_API + "002", "Unknown Exception"),

	RPR_WAS_UNKNOWN_WORKFLOW_ACTION(PlatformConstants.RPR_WORKFLOW_ACTION_SERVICE + "000",
			"Workflow Action not supported"),

	RPR_WAS_REMOVE_HOTLISTED_TAG_FAILED(PlatformConstants.RPR_WORKFLOW_ACTION_SERVICE + "001",
			"Removing hotlisted tag failed"),

	RPR_WAA_WORKFLOW_ID_NOT_FOUND(PlatformConstants.RPR_WORKFLOW_ACTION_API + "003",
			"Workflow id  %s  not found in registration table"),

	RPR_WAS_UNKNOWN_EXCEPTION(PlatformConstants.RPR_WORKFLOW_ACTION_SERVICE + "003", "Unknown Exception"),

    RPR_WIS_UNKNOWN_EXCEPTION(PlatformConstants.RPR_WORKFLOW_INSTANCE_SERVICE + "000", "Unknown Exception"),

	RPR_WIS_ALREADY_PRESENT_EXCEPTION(PlatformConstants.RPR_WORKFLOW_INSTANCE_SERVICE + "001", "WorkflowInstance already present"),

	RPR_WAS_REPROCESS_FAILED(PlatformConstants.RPR_WORKFLOW_ACTION_SERVICE + "004",
			"When REPROCESS_FAILED then Resume should not occur"),


	RPR_WAA_NOT_PAUSED(PlatformConstants.RPR_WORKFLOW_ACTION_API + "004",
			"Workflow id  %s is not PAUSED"),

    RPR_WIN_VALIDATION_SUCCESS(PlatformConstants.RPR_WORKFLOW_INSTANCE_API + "002", "Workflow instance request validated successfully"),

	RPR_WAA_VALIDATION_SUCCESS(PlatformConstants.RPR_WORKFLOW_ACTION_API + "005", "Workflow id validated successfully"),

	RPR_WORKFLOW_ACTION_JOB_FAILED(PlatformConstants.RPR_WORKFLOW_ACTION_JOB, "Workflow action job  failed"),

	/** The external stage failed. */
	EXTERNAL_STAGE_FAILED("", "External Stage Failed"),

	/** internal error for unknown reason. */
	INTERNAL_ERROR_UNKNOWN(PlatformConstants.ABIS + "01", "internal error for reason - %s"),

	/** request aborted. */
	ABORTED(PlatformConstants.ABIS + "02", "request aborted"),

	/** Unexpected error - Unable to access biometric data. */
	UNEXPECTED_ERROR(PlatformConstants.ABIS + "03", "Unexpected error - Unable to access biometric data for - %s"),

	/** Unable to serve the request. */
	UNABLE_TO_SERVE_REQUEST(PlatformConstants.ABIS + "04", "Unable to serve the request"),

	/** Invalid request. */
	INVALID_REQUEST(PlatformConstants.ABIS + "05", "Invalid request / Missing mandatory fields - %S"),

	/** Unauthorized Access. */
	UNAUTHORIZED_ACCESS(PlatformConstants.ABIS + "06", "Unauthorized Access"),

	/** Unable to fetch biometric details. */
	UNABLE_TO_FETCH_BIO_INFO(PlatformConstants.ABIS + "07", "Unable to fetch biometric details"),

	/** The missing mandatory fields. */
	MISSING_MANDATORY_FIELDS(PlatformConstants.ABIS + "08", "Mandatory request fields are missing"),

	/** The transactions not available. */
	TRANSACTIONS_NOT_AVAILABLE(PlatformConstants.REGISTRATION_TRANSACTIONS_SERVICE + "001", "RID Not Found"),

	/** The rpr rts unknown exception. */
	RPR_RTS_UNKNOWN_EXCEPTION(PlatformConstants.REGISTRATION_TRANSACTIONS_SERVICE + "002", "Unknown Exception Occured"),

	/** The rpr rts invalid request. */
	RPR_RTS_INVALID_REQUEST(PlatformConstants.REGISTRATION_TRANSACTIONS_SERVICE + "003", "Invalid request"),

	/** The rpr rts data population exception. */
	RPR_RTS_DATA_POPULATION_EXCEPTION(PlatformConstants.REGISTRATION_TRANSACTIONS_SERVICE + "004",
			"globalMessages not found for input langCode"),

	/** The rpr rhs reg base exception. */
	RPR_RHS_REG_BASE_EXCEPTION(PlatformConstants.RPR_PACKET_REQUEST_HANDLER_MODULE + "004",
			"reg Based checked exception"),

	/** The rpr details not found. */
	RPR_DETAILS_NOT_FOUND(PlatformConstants.RPR_ABIS_HANDLER + "001", "Abis Queue details not found"),

	/** The rpr no record found. */
	RPR_NO_RECORD_FOUND(PlatformConstants.RPR_ABIS_HANDLER + "002",
			"Potential Match Records are Not Found for Demo Dedupe Potential Match"),

	/** The rpr error in abis handler identify request. */
	RPR_ERROR_IN_ABIS_HANDLER_IDENTIFY_REQUEST(PlatformConstants.RPR_ABIS_HANDLER + "004",
			"Internal Error occured in Abis Handler identify request"),

	/** The rpr demo sending for manual. */
	RPR_DEMO_SENDING_FOR_MANUAL(PlatformConstants.RPR_DEMO_DEDUPE_MODULE + "001",
			"ABIS response Details found. Hence sending to manual adjudication"),
	
	RPR_DEMO_API_RESOUCE_ACCESS_FAILED(PlatformConstants.RPR_DEMO_DEDUPE_MODULE + "002",
			"Unable to Access API Resource"),

	RPR_SECUREZONE_FAILURE(PlatformConstants.RPR_SECUREZONE_NOTIFICATION_MODULE + "001",
			"Exception occured in securezone notification stage. Check logs for more info"),

	RPR_SECUREZONE_DUPLICATE_PACKET(PlatformConstants.RPR_SECUREZONE_NOTIFICATION_MODULE + "002",
			"Duplicate Packet against same additional info request Id."),

	RPR_AUTH_SYSTEM_EXCEPTION(PlatformConstants.RPR_AUTHENTICATION + "03", "Request could not be processed. Please try again"),

	/** The queue json validation failed. */
	QUEUE_JSON_VALIDATION_FAILED(PlatformConstants.RPR_MA_MIDDLEWARE + "004",
			"Unable to find Connection Properties"),

	RPR_BIOMETRIC_EXTRACTION_FAILED(PlatformConstants.RPR_BIOMETRIC_EXTRACTION_STAGE + "000",
			"biometric extraction failed"),

	RPR_BIOMETRIC_EXTRACTION_DRAFT_REQUEST_UNAVAILABLE(PlatformConstants.RPR_BIOMETRIC_EXTRACTION_STAGE + "001",
			"Draft request is unavaialble in id-repo draft repository."),
	/** The rpr ugs api resource exception. */
	RPR_BIOMETRIC_EXTRACTION_API_RESOURCE_EXCEPTION(PlatformConstants.RPR_BIOMETRIC_EXTRACTION_STAGE + "002",
			"Not able to access the API resource"),
	RPR_BIOMETRIC_EXTRACTION_NULL_RESPONSE(PlatformConstants.RPR_BIOMETRIC_EXTRACTION_STAGE + "003",
			"null response from idrepo "),
	RPR_PMS_BIOMETRIC_EXTRACTION_NULL_RESPONSE(PlatformConstants.RPR_BIOMETRIC_EXTRACTION_STAGE + "003",
			"null response from partner service "),
	/** The rpr finalization exception. */
	RPR_FINALIZATION_FAILED(PlatformConstants.RPR_FINALIZATION_STAGE + "001",
			"finalization failed"),
	RPR_FINALIZATION_FAILED_NULL_RESPONSE(PlatformConstants.RPR_FINALIZATION_STAGE + "002",
			"null response from idrepo "),
	RPR_FINALIZATION_STAGE_DRAFT_REQUEST_UNAVAILABLE(PlatformConstants.RPR_FINALIZATION_STAGE + "003",
			"Draft request is unavaialble in id-repo draft repository."),
	RPR_FINALIZATION_STAGE_API_RESOURCE_EXCEPTION(PlatformConstants.RPR_FINALIZATION_STAGE + "004",
			"Not able to access the API resource"),

	INDIVIDUAL_BIOMETRIC_AUTHENTICATION_FAILED(PlatformConstants.RPR_BIOMETRIC_AUTHENTICATION_MODULE + "004",
			"Bio authentication failed")
	;



	/** The error message. */
	private final String errorMessage; 

	/** The error code. */
	private final String errorCode;

	/**
	 * Instantiates a new platform error messages.
	 *
	 * @param errorCode
	 *            the error code
	 * @param errorMsg
	 *            the error msg
	 */
	private PlatformErrorMessages(String errorCode, String errorMsg) {
		this.errorCode = errorCode;
		this.errorMessage = errorMsg;
	}

	/**
	 * Gets the error message.
	 *
	 * @return the error message
	 */
	public String getMessage() {
		return this.errorMessage;
	}

	/**
	 * Gets the error code.
	 *
	 * @return the error code
	 */
	public String getCode() {
		return this.errorCode;
	}

}