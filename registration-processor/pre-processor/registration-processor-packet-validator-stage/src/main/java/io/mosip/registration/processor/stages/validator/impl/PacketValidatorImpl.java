package io.mosip.registration.processor.stages.validator.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.kernel.core.idobjectvalidator.constant.IdObjectValidatorSupportedOperations;
import io.mosip.kernel.core.idobjectvalidator.exception.IdObjectIOException;
import io.mosip.kernel.core.idobjectvalidator.exception.IdObjectValidationFailedException;
import io.mosip.kernel.core.idobjectvalidator.exception.InvalidIdSchemaException;
import io.mosip.kernel.core.idobjectvalidator.spi.IdObjectValidator;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.idobjectvalidator.impl.IdObjectCompositeValidator;
import io.mosip.kernel.packetmanager.exception.ApiNotAccessibleException;
import io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException;
import io.mosip.kernel.packetmanager.spi.PacketReaderService;
import io.mosip.kernel.packetmanager.util.IdSchemaUtils;
import io.mosip.registration.processor.core.constant.PacketFiles;
import io.mosip.registration.processor.core.constant.RegistrationType;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.packet.dto.PacketMetaInfo;
import io.mosip.registration.processor.core.packet.dto.applicantcategory.ApplicantTypeDocument;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService;
import io.mosip.registration.processor.packet.storage.exception.IdRepoAppException;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.PacketValidationDto;
import io.mosip.registration.processor.core.exception.PacketValidatorException;
import io.mosip.registration.processor.stages.utils.ApplicantDocumentValidation;
import io.mosip.registration.processor.stages.utils.CheckSumValidation;
import io.mosip.registration.processor.stages.utils.FilesValidation;
import io.mosip.registration.processor.stages.utils.IdObjectsSchemaValidationOperationMapper;
import io.mosip.registration.processor.stages.utils.MandatoryValidation;
import io.mosip.registration.processor.stages.utils.MasterDataValidation;
import io.mosip.registration.processor.core.spi.packet.validator.PacketValidator;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.repositary.RegistrationRepositary;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
@RefreshScope
public class PacketValidatorImpl implements PacketValidator {
	
	private static Logger regProcLogger = RegProcessorLogger.getLogger(PacketValidatorImpl.class);
	
	@Autowired
	private PacketReaderService packetReaderService;
	
	@Autowired
	IdObjectsSchemaValidationOperationMapper idObjectsSchemaValidationOperationMapper;
	
	@Autowired
	IdObjectValidator idObjectSchemaValidator;
	
	@Autowired
	private Utilities utility;

	@Autowired
	private FilesValidation filesValidation;

	@Autowired
	private CheckSumValidation checkSumValidation;

	@Autowired
	private MandatoryValidation mandatoryValidation;
	
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
	
	@Autowired
	private IdSchemaUtils idSchemaUtils;
	
	@Value("${packet.default.source}")
	private String source;

	@Value("${registration.processor.sourcepackets}")
	private String sourcepackets;

	@Autowired
	private ApplicantDocumentValidation applicantDocumentValidation;

	@Autowired
	private MasterDataValidation masterDataValidation;

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
	public boolean validate(String rid, String regType, PacketValidationDto packetValidationDto) throws PacketValidatorException
			 {
		String uin = null;
		JSONObject demographicIdentity = null;
		boolean isvalidated=true;
		try {
			InputStream packetMetaInfoStream = packetReaderService.getFile(rid,
					PacketFiles.PACKET_META_INFO.name(),source);
			PacketMetaInfo packetMetaInfo = (PacketMetaInfo) JsonUtil.inputStreamtoJavaObject(packetMetaInfoStream,
					PacketMetaInfo.class);
			if (!fileValidation(rid, packetMetaInfo, packetValidationDto)) {
				return false;
			}

		Identity identity = packetMetaInfo.getIdentity();
		InputStream idJsonStream = packetReaderService.getFile(rid, PacketFiles.ID.name(),source);

		byte[] bytearray = IOUtils.toByteArray(idJsonStream);
		String jsonString = new String(bytearray);
		ObjectMapper mapper = new ObjectMapper();
		JSONObject idObject = mapper.readValue(bytearray, JSONObject.class);

		if (!schemaValidation(rid, idObject, packetValidationDto)) {
			return false;
		}

		if (!checkSumValidation(rid, identity, packetValidationDto))
			return false;
		
		demographicIdentity = utility.getDemographicIdentityJSONObject(rid);
		if (!individualBiometricsValidation(rid, demographicIdentity, packetValidationDto))
			return false;

		if (regType.equalsIgnoreCase(RegistrationType.UPDATE.toString())
				|| regType.equalsIgnoreCase(RegistrationType.RES_UPDATE.toString())) {
			uin = utility.getUIn(rid);
			if (uin == null)
				throw new IdRepoAppException(PlatformErrorMessages.RPR_PVM_INVALID_UIN.getMessage());
			JSONObject jsonObject = utility.retrieveIdrepoJson(uin);
			if (jsonObject == null)
				throw new IdRepoAppException(PlatformErrorMessages.RPR_PIS_IDENTITY_NOT_FOUND.getMessage());
			String status = utility.retrieveIdrepoJsonStatus(uin);
			if (regType.equalsIgnoreCase(RegistrationType.UPDATE.toString())
					&& status.equalsIgnoreCase(RegistrationType.DEACTIVATED.toString())) {
				throw new RegistrationProcessorCheckedException(
						PlatformErrorMessages.RPR_PVM_UPDATE_DEACTIVATED.getCode(), "UIN is Deactivated");

			}
		}
		if (!applicantDocumentValidation(jsonString, rid, packetValidationDto)) {
			packetValidationDto.setPacketValidaionFailureMessage(" applicant document validation failed ");
			return false;
		}
		if (!masterDataValidation(jsonString, packetValidationDto)) {
			packetValidationDto.setPacketValidaionFailureMessage(" master data validation failed ");
			return false;
		}
		// check if uin is in idrepisitory
		if (RegistrationType.UPDATE.name().equalsIgnoreCase(regType)
				|| RegistrationType.RES_UPDATE.name().equalsIgnoreCase(regType)) {

			if (!uinPresentInIdRepo(String.valueOf(uin))) {
				packetValidationDto.setPacketValidaionFailureMessage(StatusUtil.UIN_NOT_FOUND_IDREPO.getMessage());
				return false;
			}
		}

		if (RegistrationType.NEW.name().equalsIgnoreCase(regType)
				&& !mandatoryValidation(rid, packetValidationDto)) {
			packetValidationDto.setPacketValidaionFailureMessage(StatusUtil.MANDATORY_VALIDATION_FAILED.getMessage());
			return false;
		}
		
			 
		} catch (IOException | io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException
				| io.mosip.kernel.core.exception.IOException | ApiNotAccessibleException | ApisResourceAccessException |
				IdObjectValidationFailedException | IdObjectIOException | RegistrationProcessorCheckedException | InvalidIdSchemaException e) {
			throw new PacketValidatorException (e);
		}
		
		return isvalidated;
	}

	private boolean individualBiometricsValidation(String registrationId,
			JSONObject demographicIdentity, PacketValidationDto packetValidationDto) throws RegistrationProcessorCheckedException {
			try {
				JSONObject identityJsonObject = JsonUtil.getJSONObject(demographicIdentity, INDIVIDUALBIOMETRICS);
				if (identityJsonObject != null) {
					String cbefFile = (String) identityJsonObject.get(VALUE);
					if (cbefFile == null) {
						packetValidationDto
								.setPacketValidaionFailureMessage(StatusUtil.INDIVIDUAL_BIOMETRIC_VALIDATION_FAILED.getMessage());
						packetValidationDto
								.setPacketValidatonStatusCode(StatusUtil.INDIVIDUAL_BIOMETRIC_VALIDATION_FAILED.getCode());
						return false;
					}
					InputStream idJsonStream = packetReaderService.getFile(registrationId, cbefFile,source);
					if (idJsonStream != null)
						return true;

				}
			} catch (IOException | io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException
					| io.mosip.kernel.core.exception.IOException | ApiNotAccessibleException e) {
				throw new RegistrationProcessorCheckedException(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getCode(),
						PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage(), e);
			}

			return true;
		}
	

	private boolean fileValidation(String rid, PacketMetaInfo packetMetaInfo,
			PacketValidationDto packetValidationDto) throws PacketDecryptionFailureException, IOException, ApiNotAccessibleException {
			if (env.getProperty(VALIDATEFILE).trim().equalsIgnoreCase(VALIDATIONFALSE)) {
				packetValidationDto.setFilesValidated(true);
				return packetValidationDto.isFilesValidated();
			}
			packetValidationDto.setFilesValidated(
					filesValidation.filesValidation(rid, packetMetaInfo, source, packetValidationDto));
			if (!packetValidationDto.isFilesValidated()) {
				packetValidationDto.setPacketValidaionFailureMessage(StatusUtil.FILE_VALIDATION_FAILED.getMessage());
				packetValidationDto.setPacketValidatonStatusCode(StatusUtil.FILE_VALIDATION_FAILED.getCode());
			}
			return packetValidationDto.isFilesValidated();

		}
	
	private boolean schemaValidation(String rid, JSONObject idObject, PacketValidationDto packetValidationDto)
			throws ApisResourceAccessException, IOException, IdObjectValidationFailedException, IdObjectIOException,
			PacketDecryptionFailureException, io.mosip.kernel.core.exception.IOException,
			RegistrationProcessorCheckedException, ApiNotAccessibleException, InvalidIdSchemaException {

		if (env.getProperty(VALIDATESCHEMA).trim().equalsIgnoreCase(VALIDATIONFALSE)) {
			packetValidationDto.setSchemaValidated(true);
			return packetValidationDto.isSchemaValidated();
		}
		IdObjectValidatorSupportedOperations operation = idObjectsSchemaValidationOperationMapper.getOperation(rid);
		String schema=idSchemaUtils.getIdSchema();

		packetValidationDto.setSchemaValidated(idObjectSchemaValidator.validateIdObject(schema,packetReaderService.getCompleteIdObject(rid, sourcepackets), operation));

		if (!packetValidationDto.isSchemaValidated()) {
			packetValidationDto.setPacketValidaionFailureMessage(StatusUtil.SCHEMA_VALIDATION_FAILED.getMessage());
			packetValidationDto.setPacketValidatonStatusCode(StatusUtil.SCHEMA_VALIDATION_FAILED.getCode());

		}

		return packetValidationDto.isSchemaValidated();

	}
	
	private boolean checkSumValidation(String rid, Identity identity, PacketValidationDto packetValidationDto)
			throws IOException, PacketDecryptionFailureException, io.mosip.kernel.core.exception.IOException, ApiNotAccessibleException {
		if (env.getProperty(VALIDATECHECKSUM).trim().equalsIgnoreCase(VALIDATIONFALSE)) {
			packetValidationDto.setCheckSumValidated(true);
			return packetValidationDto.isCheckSumValidated();
		}
		packetValidationDto.setCheckSumValidated(
				checkSumValidation.checksumvalidation(rid, identity, source, packetValidationDto));
		if (!packetValidationDto.isCheckSumValidated()) {
			packetValidationDto.setPacketValidaionFailureMessage(StatusUtil.CHECKSUM_VALIDATION_FAILED.getMessage());
			packetValidationDto.setPacketValidatonStatusCode(StatusUtil.CHECKSUM_VALIDATION_FAILED.getCode());
		}

		return packetValidationDto.isCheckSumValidated();

	}
	
	private boolean applicantDocumentValidation(String jsonString, String registrationId,
			PacketValidationDto packetValidationDto)
			throws IOException, PacketDecryptionFailureException, io.mosip.kernel.core.exception.IOException, ApiNotAccessibleException {
		if (env.getProperty(VALIDATEAPPLICANTDOCUMENT).trim().equalsIgnoreCase(VALIDATIONFALSE)) {
			packetValidationDto.setApplicantDocumentValidation(true);
			return packetValidationDto.isApplicantDocumentValidation();
		}
		
		packetValidationDto.setApplicantDocumentValidation(
				applicantDocumentValidation.validateDocument(registrationId));
		if (!packetValidationDto.isApplicantDocumentValidation()) {
			packetValidationDto.setPacketValidaionFailureMessage(StatusUtil.APPLICANT_DOCUMENT_VALIDATION_FAILED.getMessage());
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
		
		packetValidationDto.setMasterDataValidation(masterDataValidation.validateMasterData(jsonString));
		if (!packetValidationDto.isMasterDataValidation()) {
			packetValidationDto.setPacketValidaionFailureMessage(StatusUtil.MASTER_DATA_VALIDATION_FAILED.getMessage());
			packetValidationDto.setPacketValidatonStatusCode(StatusUtil.MASTER_DATA_VALIDATION_FAILED.getCode());
		}

		return packetValidationDto.isMasterDataValidation();

	}
	
	private boolean uinPresentInIdRepo(String uin) throws ApisResourceAccessException, IOException {
		return idRepoService.findUinFromIdrepo(uin, utility.getGetRegProcessorDemographicIdentity()) != null;

	}
	
	private boolean mandatoryValidation(String rid,
			PacketValidationDto packetValidationDto) throws IOException,
			PacketDecryptionFailureException, io.mosip.kernel.core.exception.IOException, ApiNotAccessibleException {
		if (env.getProperty(VALIDATEMANDATORY).trim().equalsIgnoreCase(VALIDATIONFALSE))
			return true;
		packetValidationDto.setMandatoryValidation(
				mandatoryValidation.mandatoryFieldValidation(rid, source, packetValidationDto));
		if (!packetValidationDto.isMandatoryValidation()) {
			packetValidationDto.setPacketValidaionFailureMessage(StatusUtil.MANDATORY_VALIDATION_FAILED.getMessage());
			packetValidationDto.setPacketValidatonStatusCode(StatusUtil.MANDATORY_VALIDATION_FAILED.getCode());
		}
		return packetValidationDto.isMandatoryValidation();
	}
	
	
}
