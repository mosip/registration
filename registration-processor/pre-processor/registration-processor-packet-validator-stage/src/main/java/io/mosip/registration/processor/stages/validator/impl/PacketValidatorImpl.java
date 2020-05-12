package io.mosip.registration.processor.stages.validator.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.kernel.core.idobjectvalidator.constant.IdObjectValidatorSupportedOperations;
import io.mosip.kernel.core.idobjectvalidator.exception.IdObjectIOException;
import io.mosip.kernel.core.idobjectvalidator.exception.IdObjectValidationFailedException;
import io.mosip.kernel.core.idobjectvalidator.spi.IdObjectValidator;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.code.RegistrationExceptionTypeCode;
import io.mosip.registration.processor.core.constant.JsonConstant;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.PacketFiles;
import io.mosip.registration.processor.core.constant.RegistrationType;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.FieldValue;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.packet.dto.PacketMetaInfo;
import io.mosip.registration.processor.core.packet.dto.applicantcategory.ApplicantTypeDocument;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.status.util.TrimExceptionMessage;
import io.mosip.registration.processor.core.util.IdentityIteratorUtil;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService;
import io.mosip.registration.processor.packet.storage.exception.IdRepoAppException;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.packet.utility.exception.ApiNotAccessibleException;
import io.mosip.registration.processor.packet.utility.exception.PacketDecryptionFailureException;
import io.mosip.registration.processor.packet.utility.service.PacketReaderService;
import io.mosip.registration.processor.stages.dto.PacketValidationDto;
import io.mosip.registration.processor.stages.exception.PacketValidatorException;
import io.mosip.registration.processor.stages.utils.ApplicantDocumentValidation;
import io.mosip.registration.processor.stages.utils.CheckSumValidation;
import io.mosip.registration.processor.stages.utils.FilesValidation;
import io.mosip.registration.processor.stages.utils.IdObjectsSchemaValidationOperationMapper;
import io.mosip.registration.processor.stages.utils.MandatoryValidation;
import io.mosip.registration.processor.stages.utils.MasterDataValidation;
import io.mosip.registration.processor.stages.validator.PacketValidator;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.repositary.RegistrationRepositary;
@Qualifier("packetValidatorImpl")
public class PacketValidatorImpl implements PacketValidator{
	
	private static Logger regProcLogger = RegProcessorLogger.getLogger(PacketValidatorImpl.class);
	
	@Autowired
	private PacketReaderService packetReaderService;
	
	@Autowired
	IdObjectsSchemaValidationOperationMapper idObjectsSchemaValidationOperationMapper;
	
	@Autowired(required = false)
	@Qualifier("referenceValidator")
	@Lazy
	IdObjectValidator idObjectValidator;
	
	@Autowired
	private Utilities utility;
	
	@Autowired
	private Environment env;
	
	@Autowired
	ApplicantTypeDocument applicantTypeDocument;
	
	@Autowired
	private IdRepoService idRepoService;
	
	@Autowired
	private RegistrationRepositary<SyncRegistrationEntity, String> registrationRepositary;
	
	@Autowired
	private RegistrationProcessorRestClientService<Object> registrationProcessorRestService;
	
	@Autowired
	RegistrationExceptionMapperUtil registrationStatusMapperUtil;
	
	@Value("${registration.processor.default.source}")
	private String source;

	/** The Constant APPLICANT_TYPE. */
	public static final String APPLICANT_TYPE = "applicantType";

	private static final String VALIDATIONFALSE = "false";

	/** The Constant APPROVED. */
	public static final String APPROVED = "APPROVED";

	/** The Constant REJECTED. */
	public static final String REJECTED = "REJECTED";
	
	private static final String INDIVIDUALBIOMETRICS = "individualBiometrics";

	private static final String VALUE = "value";

	private static final String VALIDATESCHEMA = "registration.processor.validateSchema";

	private static final String VALIDATEFILE = "registration.processor.validateFile";

	private static final String VALIDATECHECKSUM = "registration.processor.validateChecksum";

	private static final String VALIDATEAPPLICANTDOCUMENT = "registration.processor.validateApplicantDocument";

	private static final String VALIDATEMASTERDATA = "registration.processor.validateMasterData";

	private static final String VALIDATEMANDATORY = "registration-processor.validatemandotary";

	@Override
	public boolean validate(InternalRegistrationStatusDto registrationStatusDto, PacketMetaInfo packetMetaInfo,
			MessageDTO object, IdentityIteratorUtil identityIteratorUtil, PacketValidationDto packetValidationDto,
			TrimExceptionMessage trimMessage,LogDescription description) throws PacketValidatorException
			 {
		Long uin = null;
		JSONObject demographicIdentity = null;
		String registrationId = registrationStatusDto.getRegistrationId();
		boolean isvalidated=true;
		try {
			if (!fileValidation(packetMetaInfo, registrationStatusDto, packetValidationDto)) {
				isvalidated= false;
			}

		Identity identity = packetMetaInfo.getIdentity();
		InputStream idJsonStream = packetReaderService.getFile(registrationId, PacketFiles.ID.name(),source);

		byte[] bytearray = IOUtils.toByteArray(idJsonStream);
		String jsonString = new String(bytearray);
		ObjectMapper mapper = new ObjectMapper();
		JSONObject idObject = mapper.readValue(bytearray, JSONObject.class);

		if (!schemaValidation(idObject, registrationStatusDto, packetValidationDto)) {
			isvalidated= false;
		}

		if (!checkSumValidation(identity, registrationStatusDto, packetValidationDto))
			isvalidated= false;
		
		demographicIdentity = utility.getDemographicIdentityJSONObject(registrationId);
		if (!individualBiometricsValidation(registrationStatusDto, demographicIdentity, packetValidationDto))
			isvalidated= false;

		List<FieldValue> metadataList = identity.getMetaData();
		if (object.getReg_type().toString().equalsIgnoreCase(RegistrationType.UPDATE.toString())
				|| object.getReg_type().toString().equalsIgnoreCase(RegistrationType.RES_UPDATE.toString())) {
			uin = utility.getUIn(registrationId);
			if (uin == null)
				throw new IdRepoAppException(PlatformErrorMessages.RPR_PVM_INVALID_UIN.getMessage());
			JSONObject jsonObject = utility.retrieveIdrepoJson(uin);
			if (jsonObject == null)
				throw new IdRepoAppException(PlatformErrorMessages.RPR_PIS_IDENTITY_NOT_FOUND.getMessage());
			String status = utility.retrieveIdrepoJsonStatus(uin);
			if (object.getReg_type().toString().equalsIgnoreCase(RegistrationType.UPDATE.toString())
					&& status.equalsIgnoreCase(RegistrationType.DEACTIVATED.toString())) {
				throw new RegistrationProcessorCheckedException(
						PlatformErrorMessages.RPR_PVM_UPDATE_DEACTIVATED.getCode(), "UIN is Deactivated");

			}
		}
		if (!applicantDocumentValidation(jsonString, registrationId, packetValidationDto)) {
			packetValidationDto.setPacketValidaionFailure(" applicant document validation failed ");
			isvalidated= false;
		}
		if (!masterDataValidation(jsonString, packetValidationDto)) {
			packetValidationDto.setPacketValidaionFailure(" master data validation failed ");
			isvalidated= false;
		}
		// check if uin is in idrepisitory
		if (RegistrationType.UPDATE.name().equalsIgnoreCase(object.getReg_type().name())
				|| RegistrationType.RES_UPDATE.name().equalsIgnoreCase(object.getReg_type().name())) {

			if (!uinPresentInIdRepo(String.valueOf(uin))) {
				packetValidationDto.setPacketValidaionFailure(StatusUtil.UIN_NOT_FOUND_IDREPO.getMessage());
				isvalidated= false;
			}
		}

		if (RegistrationType.NEW.name().equalsIgnoreCase(registrationStatusDto.getRegistrationType())
				&& !mandatoryValidation(registrationStatusDto, packetValidationDto)) {
			packetValidationDto.setPacketValidaionFailure(StatusUtil.MANDATORY_VALIDATION_FAILED.getMessage());
			isvalidated= false;
		}
		// Check RegId & regType are same or not From PacketMetaInfo by comparing with
				// Sync list table
		isvalidated= validateRegIdAndTypeFromSyncTable(object.getRid(), metadataList, identityIteratorUtil,
				packetValidationDto);
			 
		} catch(ApisResourceAccessException e) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
			registrationStatusDto.setStatusComment(trimMessage
					.trimExceptionMessage(StatusUtil.API_RESOUCE_ACCESS_FAILED.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.API_RESOUCE_ACCESS_FAILED.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(registrationStatusMapperUtil
					.getStatusCode(RegistrationExceptionTypeCode.APIS_RESOURCE_ACCESS_EXCEPTION));
			description.setCode(PlatformErrorMessages.RPR_PVM_API_RESOUCE_ACCESS_FAILED.getCode());
			description.setMessage(PlatformErrorMessages.RPR_PVM_API_RESOUCE_ACCESS_FAILED.getMessage());
			object.setIsValid(Boolean.FALSE);
			object.setInternalError(Boolean.TRUE);
			throw new PacketValidatorException(e);
		}
		catch (BaseCheckedException e) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
			registrationStatusDto.setStatusComment(
					trimMessage.trimExceptionMessage(StatusUtil.BASE_CHECKED_EXCEPTION.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.BASE_CHECKED_EXCEPTION.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.BASE_CHECKED_EXCEPTION));
			packetValidationDto.setTransactionSuccessful(false);
			description.setMessage(PlatformErrorMessages.RPR_PVM_BASE_CHECKED_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.RPR_PVM_BASE_CHECKED_EXCEPTION.getCode());
			object.setIsValid(Boolean.FALSE);
			object.setInternalError(Boolean.TRUE);
			object.setRid(registrationStatusDto.getRegistrationId());
			throw new PacketValidatorException(e);
		}  catch (BaseUncheckedException e) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
			registrationStatusDto.setStatusComment(trimMessage
					.trimExceptionMessage(StatusUtil.BASE_UNCHECKED_EXCEPTION.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.BASE_UNCHECKED_EXCEPTION.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.BASE_UNCHECKED_EXCEPTION));
			packetValidationDto.setTransactionSuccessful(false);
			description.setMessage(PlatformErrorMessages.RPR_PVM_BASE_UNCHECKED_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.RPR_PVM_BASE_UNCHECKED_EXCEPTION.getCode());
			object.setIsValid(Boolean.FALSE);
			object.setInternalError(Boolean.TRUE);
			object.setRid(registrationStatusDto.getRegistrationId());
			throw new PacketValidatorException(e);
		} catch (IOException exc) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
			registrationStatusDto.setStatusComment(
					trimMessage.trimExceptionMessage(StatusUtil.IO_EXCEPTION.getMessage() + exc.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.IO_EXCEPTION.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.IOEXCEPTION));
			packetValidationDto.setTransactionSuccessful(false);
			description.setMessage(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.STRUCTURAL_VALIDATION_FAILED.getCode());
			object.setIsValid(Boolean.FALSE);
			object.setInternalError(Boolean.TRUE);
			object.setRid(registrationStatusDto.getRegistrationId());
			throw new PacketValidatorException(exc);
		}catch (Exception ex) {
			registrationStatusDto.setStatusComment(trimMessage
					.trimExceptionMessage(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getMessage() + ex.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.EXCEPTION));
			packetValidationDto.setTransactionSuccessful(false);
			description.setMessage(PlatformErrorMessages.STRUCTURAL_VALIDATION_FAILED.getMessage());
			description.setCode(PlatformErrorMessages.STRUCTURAL_VALIDATION_FAILED.getCode());
			object.setIsValid(Boolean.FALSE);
			object.setInternalError(Boolean.TRUE);
			object.setRid(registrationStatusDto.getRegistrationId());
			throw new PacketValidatorException(ex);
		}
		
		return isvalidated;
	}

	private boolean individualBiometricsValidation(InternalRegistrationStatusDto registrationStatusDto,
			JSONObject demographicIdentity, PacketValidationDto packetValidationDto) throws RegistrationProcessorCheckedException,
	io.mosip.registration.processor.packet.utility.exception.PacketDecryptionFailureException {
			try {
				String registrationId = registrationStatusDto.getRegistrationId();

				JSONObject identityJsonObject = JsonUtil.getJSONObject(demographicIdentity, INDIVIDUALBIOMETRICS);
				if (identityJsonObject != null) {
					String cbefFile = (String) identityJsonObject.get(VALUE);
					if (cbefFile == null) {
						packetValidationDto
								.setPacketValidaionFailure(StatusUtil.INDIVIDUAL_BIOMETRIC_VALIDATION_FAILED.getMessage());
						packetValidationDto
								.setPacketValidatonStatusCode(StatusUtil.INDIVIDUAL_BIOMETRIC_VALIDATION_FAILED.getCode());
						return false;
					}
					InputStream idJsonStream = packetReaderService.getFile(registrationId, cbefFile,source);
					if (idJsonStream != null)
						return true;

				}
			} catch (IOException | io.mosip.registration.processor.packet.utility.exception.PacketDecryptionFailureException 
					| io.mosip.kernel.core.exception.IOException | ApiNotAccessibleException e) {
				throw new RegistrationProcessorCheckedException(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getCode(),
						PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage(), e);
			}

			return true;
		}
	

	private boolean fileValidation(PacketMetaInfo packetMetaInfo, InternalRegistrationStatusDto registrationStatusDto,
			PacketValidationDto packetValidationDto) throws PacketDecryptionFailureException, ApisResourceAccessException, IOException, ApiNotAccessibleException {
			if (env.getProperty(VALIDATEFILE).trim().equalsIgnoreCase(VALIDATIONFALSE)) {
				packetValidationDto.setFilesValidated(true);
				return packetValidationDto.isFilesValidated();
			}
			FilesValidation filesValidation = new FilesValidation(packetReaderService, registrationStatusDto,source);
			packetValidationDto.setFilesValidated(
					filesValidation.filesValidation(registrationStatusDto.getRegistrationId(), packetMetaInfo));
			if (!packetValidationDto.isFilesValidated()) {
				packetValidationDto.setPacketValidaionFailure(StatusUtil.FILE_VALIDATION_FAILED.getMessage());
				packetValidationDto.setPacketValidatonStatusCode(StatusUtil.FILE_VALIDATION_FAILED.getCode());
			}
			return packetValidationDto.isFilesValidated();

		}
	
	private boolean schemaValidation(JSONObject idObject, InternalRegistrationStatusDto registrationStatusDto,
			PacketValidationDto packetValidationDto)
			throws ApisResourceAccessException, IOException, IdObjectValidationFailedException, IdObjectIOException,
			PacketDecryptionFailureException, io.mosip.kernel.core.exception.IOException,
			RegistrationProcessorCheckedException, 
			io.mosip.registration.processor.core.exception.PacketDecryptionFailureException, ApiNotAccessibleException {

		if (env.getProperty(VALIDATESCHEMA).trim().equalsIgnoreCase(VALIDATIONFALSE)) {
			packetValidationDto.setSchemaValidated(true);
			return packetValidationDto.isSchemaValidated();
		}
		IdObjectValidatorSupportedOperations operation = idObjectsSchemaValidationOperationMapper
				.getOperation(registrationStatusDto.getRegistrationId());
		packetValidationDto.setSchemaValidated(idObjectValidator.validateIdObject(idObject, operation));

		if (!packetValidationDto.isSchemaValidated()) {
			packetValidationDto.setPacketValidaionFailure(StatusUtil.SCHEMA_VALIDATION_FAILED.getMessage());
			packetValidationDto.setPacketValidatonStatusCode(StatusUtil.SCHEMA_VALIDATION_FAILED.getCode());

		}

		return packetValidationDto.isSchemaValidated();

	}
	
	private boolean checkSumValidation(Identity identity, InternalRegistrationStatusDto registrationStatusDto,
			PacketValidationDto packetValidationDto) throws IOException, PacketDecryptionFailureException,
			ApisResourceAccessException, io.mosip.kernel.core.exception.IOException, ApiNotAccessibleException {
		if (env.getProperty(VALIDATECHECKSUM).trim().equalsIgnoreCase(VALIDATIONFALSE)) {
			packetValidationDto.setCheckSumValidated(true);
			return packetValidationDto.isCheckSumValidated();
		}
		CheckSumValidation checkSumValidation = new CheckSumValidation(packetReaderService, registrationStatusDto,source);
		packetValidationDto.setCheckSumValidated(
				checkSumValidation.checksumvalidation(registrationStatusDto.getRegistrationId(), identity));
		if (!packetValidationDto.isCheckSumValidated()) {
			packetValidationDto.setPacketValidaionFailure(StatusUtil.CHECKSUM_VALIDATION_FAILED.getMessage());
			packetValidationDto.setPacketValidatonStatusCode(StatusUtil.CHECKSUM_VALIDATION_FAILED.getCode());
		}

		return packetValidationDto.isCheckSumValidated();

	}
	
	private boolean applicantDocumentValidation(String jsonString, String registrationId,
			PacketValidationDto packetValidationDto)
			throws IOException, ApisResourceAccessException, org.json.simple.parser.ParseException,
			PacketDecryptionFailureException, io.mosip.kernel.core.exception.IOException,
			RegistrationProcessorCheckedException, io.mosip.registration.processor.core.exception.PacketDecryptionFailureException, ApiNotAccessibleException {
		if (env.getProperty(VALIDATEAPPLICANTDOCUMENT).trim().equalsIgnoreCase(VALIDATIONFALSE)) {
			packetValidationDto.setApplicantDocumentValidation(true);
			return packetValidationDto.isApplicantDocumentValidation();
		}
		ApplicantDocumentValidation applicantDocumentValidation = new ApplicantDocumentValidation(utility, env,
				applicantTypeDocument);
		packetValidationDto.setApplicantDocumentValidation(
				applicantDocumentValidation.validateDocument(registrationId, jsonString));
		if (!packetValidationDto.isApplicantDocumentValidation()) {
			packetValidationDto.setPacketValidaionFailure(StatusUtil.APPLICANT_DOCUMENT_VALIDATION_FAILED.getMessage());
			packetValidationDto.setPacketValidatonStatusCode(StatusUtil.APPLICANT_DOCUMENT_VALIDATION_FAILED.getCode());
		}
		return packetValidationDto.isApplicantDocumentValidation();

	}
	
	private boolean masterDataValidation(String jsonString, PacketValidationDto packetValidationDto)
			throws ApisResourceAccessException, IOException {
		if (env.getProperty(VALIDATEMASTERDATA).trim().equalsIgnoreCase(VALIDATIONFALSE)) {
			packetValidationDto.setMasterDataValidation(true);
			return packetValidationDto.isMasterDataValidation();
		}
		MasterDataValidation masterDataValidation = new MasterDataValidation(env, registrationProcessorRestService,
				utility);
		packetValidationDto.setMasterDataValidation(masterDataValidation.validateMasterData(jsonString));
		if (!packetValidationDto.isMasterDataValidation()) {
			packetValidationDto.setPacketValidaionFailure(StatusUtil.MASTER_DATA_VALIDATION_FAILED.getMessage());
			packetValidationDto.setPacketValidatonStatusCode(StatusUtil.MASTER_DATA_VALIDATION_FAILED.getCode());
		}

		return packetValidationDto.isMasterDataValidation();

	}
	
	private boolean uinPresentInIdRepo(String uin) throws ApisResourceAccessException, IOException {
		return idRepoService.findUinFromIdrepo(uin, utility.getGetRegProcessorDemographicIdentity()) != null;

	}
	
	private boolean mandatoryValidation(InternalRegistrationStatusDto registrationStatusDto,
			PacketValidationDto packetValidationDto) throws IOException, JSONException,
			PacketDecryptionFailureException, ApisResourceAccessException, io.mosip.kernel.core.exception.IOException, ApiNotAccessibleException {
		if (env.getProperty(VALIDATEMANDATORY).trim().equalsIgnoreCase(VALIDATIONFALSE))
			return true;
		MandatoryValidation mandatoryValidation = new MandatoryValidation(packetReaderService, registrationStatusDto,
				utility,source);
		packetValidationDto.setMandatoryValidation(
				mandatoryValidation.mandatoryFieldValidation(registrationStatusDto.getRegistrationId()));
		if (!packetValidationDto.isMandatoryValidation()) {
			packetValidationDto.setPacketValidaionFailure(StatusUtil.MANDATORY_VALIDATION_FAILED.getMessage());
			packetValidationDto.setPacketValidatonStatusCode(StatusUtil.MANDATORY_VALIDATION_FAILED.getCode());
		}
		return packetValidationDto.isMandatoryValidation();
	}
	
	private boolean validateRegIdAndTypeFromSyncTable(String regId, List<FieldValue> metadataList,
			IdentityIteratorUtil identityIteratorUtil, PacketValidationDto packetValidationDto) {
		String regType = identityIteratorUtil.getFieldValue(metadataList, JsonConstant.REGISTRATIONTYPE);
		List<SyncRegistrationEntity> syncRecordList = registrationRepositary.getSyncRecordsByRegIdAndRegType(regId,
				regType.toUpperCase());

		if (syncRecordList != null && !syncRecordList.isEmpty()) {
			packetValidationDto.setRIdAndTypeSynched(true);
			return packetValidationDto.isRIdAndTypeSynched();
		}
		packetValidationDto.setPacketValidaionFailure(StatusUtil.RID_AND_TYPE_SYNC_FAILED.getMessage());
		packetValidationDto.setPacketValidatonStatusCode(StatusUtil.RID_AND_TYPE_SYNC_FAILED.getCode());
		regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), regId,
				PlatformErrorMessages.RPR_PVM_RECORD_NOT_MATCHED_FROM_SYNC_TABLE.getCode(),
				PlatformErrorMessages.RPR_PVM_RECORD_NOT_MATCHED_FROM_SYNC_TABLE.getMessage());
		return packetValidationDto.isRIdAndTypeSynched();
	}
}
