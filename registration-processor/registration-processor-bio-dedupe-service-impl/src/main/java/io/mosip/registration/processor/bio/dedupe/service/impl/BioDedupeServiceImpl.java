package io.mosip.registration.processor.bio.dedupe.service.impl;

import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.cbeffutil.spi.CbeffUtil;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.bio.dedupe.exception.ABISAbortException;
import io.mosip.registration.processor.bio.dedupe.exception.ABISInternalError;
import io.mosip.registration.processor.bio.dedupe.exception.UnableToServeRequestABISException;
import io.mosip.registration.processor.bio.dedupe.exception.UnexceptedError;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorUnCheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.packet.dto.RegAbisRefDto;
import io.mosip.registration.processor.core.packet.dto.abis.AbisIdentifyRequestDto;
import io.mosip.registration.processor.core.packet.dto.abis.AbisIdentifyResponseDto;
import io.mosip.registration.processor.core.packet.dto.abis.AbisInsertRequestDto;
import io.mosip.registration.processor.core.packet.dto.abis.AbisInsertResponseDto;
import io.mosip.registration.processor.core.packet.dto.abis.CandidatesDto;
import io.mosip.registration.processor.core.packet.dto.abis.Flag;
import io.mosip.registration.processor.core.packet.dto.demographicinfo.DemographicInfoDto;
import io.mosip.registration.processor.core.spi.biodedupe.BioDedupeService;
import io.mosip.registration.processor.core.spi.packetmanager.PacketInfoManager;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;
import io.mosip.registration.processor.packet.storage.utils.BIRConverter;
import io.mosip.registration.processor.packet.storage.utils.PacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Class BioDedupeServiceImpl.
 *
 * @author Alok
 * @author Nagalakshmi
 *
 */
@Service
public class BioDedupeServiceImpl implements BioDedupeService {

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(BioDedupeServiceImpl.class);

	/** The rest client service. */
	@Autowired
	private RegistrationProcessorRestClientService<Object> restClientService;

	/** The packet info manager. */
	@Autowired
	private PacketInfoManager<Identity, ApplicantInfoDto> packetInfoManager;

	@Autowired
	private RegistrationStatusService registrationStatusService;

	/** The max results. */
	@Value("${registration.processor.abis.maxResults}")
	private String maxResults;

	/** The target FPIR. */
	@Value("${registration.processor.abis.targetFPIR}")
	private String targetFPIR;



	private static final String ABIS_INSERT = "mosip.abis.insert";

	private static final String ABIS_IDENTIFY = "mosip.abis.identify";

	private static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";

	@Autowired
	private Environment env;

	@Autowired
	private Utilities utility;

	@Autowired
	private PriorityBasedPacketManagerService packetManagerService;

	@Autowired
	private CbeffUtil cbeffutil;

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.processor.core.spi.biodedupe.BioDedupeService#
	 * insertBiometrics(java.lang.String)
	 */
	@Override
	public String insertBiometrics(String registrationId) throws ApisResourceAccessException, IOException {

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				registrationId, "BioDedupeServiceImpl::insertBiometrics()::entry");
		String insertStatus = "failure";
		String requestId = uuidGenerator();
		String referenceId = uuidGenerator();

		AbisInsertRequestDto abisInsertRequestDto = new AbisInsertRequestDto();
		abisInsertRequestDto.setId(ABIS_INSERT);
		abisInsertRequestDto.setRequestId(requestId);
		abisInsertRequestDto.setReferenceId(referenceId);
		abisInsertRequestDto.setReferenceURL("");
		abisInsertRequestDto.setRequesttime(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
		RegAbisRefDto regAbisRefDto = new RegAbisRefDto();
		regAbisRefDto.setAbis_ref_id(referenceId);
		regAbisRefDto.setReg_id(registrationId);
		String moduleId = PlatformSuccessMessages.RPR_BIO_DEDUPE_SUCCESS.getCode();
		String moduleName = ModuleName.BIO_DEDUPE.toString();
		packetInfoManager.saveAbisRef(regAbisRefDto, moduleId, moduleName);
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				registrationId,
				"BioDedupeServiceImpl::insertBiometrics():: BIODEDUPEPOTENTIAL POST SERVICE Started with request data : "
						+ JsonUtil.objectMapperObjectToJson(abisInsertRequestDto));

		AbisInsertResponseDto authResponseDTO = (AbisInsertResponseDto) restClientService
				.postApi(ApiName.BIODEDUPEINSERT, "", "", abisInsertRequestDto, AbisInsertResponseDto.class);

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				registrationId,
				"BioDedupeServiceImpl::insertBiometrics():: BIODEDUPEPOTENTIAL POST SERVICE ended with reponse data : "
						+ JsonUtil.objectMapperObjectToJson(authResponseDTO));

		if (authResponseDTO.getReturnValue().equalsIgnoreCase("1"))
			insertStatus = "success";
		else
			throwException(authResponseDTO.getFailureReason(), referenceId, requestId);
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				registrationId, "BioDedupeServiceImpl::insertBiometrics()::exit");

		return insertStatus;

	}

	/**
	 * Throw exception.
	 *
	 * @param failureReason
	 *            the failure reason
	 * @param referenceId
	 * @param requestId
	 *            the request id
	 */
	private void throwException(String failureReason, String referenceId, String requestId) {

		if (failureReason.equalsIgnoreCase("1"))
			throw new ABISInternalError(
					PlatformErrorMessages.RPR_BDD_ABIS_INTERNAL_ERROR.getMessage() + referenceId + " " + requestId);

		else if (failureReason.equalsIgnoreCase("2"))
			throw new ABISAbortException(
					PlatformErrorMessages.RPR_BDD_ABIS_ABORT.getMessage() + referenceId + " " + requestId);

		else if (failureReason.equalsIgnoreCase("3"))
			throw new UnexceptedError(
					PlatformErrorMessages.RPR_BDD_UNEXCEPTED_ERROR.getMessage() + referenceId + " " + requestId);

		else if (failureReason.equalsIgnoreCase("4"))
			throw new UnableToServeRequestABISException(
					PlatformErrorMessages.RPR_BDD_UNABLE_TO_SERVE_REQUEST.getMessage() + referenceId + " " + requestId);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.processor.core.spi.biodedupe.BioDedupeService#
	 * performDedupe(java.lang.String)
	 */
	@Override
	public List<String> performDedupe(String registrationId) throws ApisResourceAccessException, IOException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				registrationId, "BioDedupeServiceImpl::performDedupe()::entry");
		List<String> duplicates = new ArrayList<>();
		List<String> abisResponseDuplicates = new ArrayList<>();

		String requestId = uuidGenerator();

		String referenceId = packetInfoManager.getReferenceIdByRid(registrationId).get(0);

		AbisIdentifyRequestDto identifyRequestDto = new AbisIdentifyRequestDto();
		Flag flag = new Flag();
		identifyRequestDto.setId(ABIS_IDENTIFY);
		identifyRequestDto.setVersion("1.0");
		identifyRequestDto.setRequestId(requestId);
		identifyRequestDto.setReferenceId(referenceId);
		identifyRequestDto.setRequesttime(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
		flag.setMaxResults(maxResults);
		flag.setTargetFPIR(targetFPIR);
		identifyRequestDto.setFlags(flag);

		// call Identify Api to get duplicate ids
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				registrationId,
				"BioDedupeServiceImpl::performDedupe():: BIODEDUPEPOTENTIAL POST SERVICE Start with request data :  "
						+ JsonUtil.objectMapperObjectToJson(identifyRequestDto));

		AbisIdentifyResponseDto responsedto = (AbisIdentifyResponseDto) restClientService
				.postApi(ApiName.BIODEDUPEPOTENTIAL, "", "", identifyRequestDto, AbisIdentifyResponseDto.class);
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				registrationId,
				"BioDedupeServiceImpl::performDedupe():: BIODEDUPEPOTENTIAL POST SERVICE ended with response data : "
						+ JsonUtil.objectMapperObjectToJson(responsedto));

		if (responsedto != null) {

			if (responsedto.getReturnValue().equalsIgnoreCase("2")) {
				throwException(responsedto.getFailureReason(), referenceId, requestId);
			}

			if (responsedto.getCandidateList() != null) {
				getDuplicateCandidates(duplicates, abisResponseDuplicates, responsedto);
			}
		}

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				registrationId, "BioDedupeServiceImpl::performDedupe()::exit");
		return duplicates;
	}

	/**
	 * Gets the duplicate candidates.
	 *
	 * @param duplicates
	 *            the duplicates
	 * @param abisResponseDuplicates
	 *            the abis response duplicates
	 * @param responsedto
	 *            the responsedto
	 * @return the duplicate candidates
	 */
	private void getDuplicateCandidates(List<String> duplicates, List<String> abisResponseDuplicates,
			AbisIdentifyResponseDto responsedto) {
		CandidatesDto[] candidateList = responsedto.getCandidateList().getCandidates();

		for (CandidatesDto candidate : candidateList) {

				List<String> regIdList = packetInfoManager.getRidByReferenceId(candidate.getReferenceId());
				if (!regIdList.isEmpty()) {
					String regId = regIdList.get(0);
					abisResponseDuplicates.add(regId);
				}

		}

		for (String duplicateReg : abisResponseDuplicates) {
			List<DemographicInfoDto> demoList = packetInfoManager.findDemoById(duplicateReg);
			if (!demoList.isEmpty()) {
				if (registrationStatusService.checkUinAvailabilityForRid(demoList.get(0).getRegId())) {
					duplicates.add(duplicateReg);
				}
			}
		}
	}

	/**
	 * Uuid generator.
	 *
	 * @return the string
	 */
	private String uuidGenerator() {
		return UUID.randomUUID().toString();
	}

	/*
	 * (non-Javadoc) get cbef file based on registration Id
	 * 
	 * @see io.mosip.registration.processor.core.spi.biodedupe.BioDedupeService#
	 * getFileByRegId( java.lang.String)
	 */
	@Override
	public byte[] getFileByRegId(String registrationId, String process) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				registrationId, "BioDedupeServiceImpl::getFileByRegId()::entry");
		byte[] file = getFile(registrationId, process);
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				registrationId, "BioDedupeServiceImpl::getFileByRegId()::exit");
		return file;
	}

	/*
	 * (non-Javadoc) get cbef file based on abisRefId
	 * 
	 * @see
	 * io.mosip.registration.processor.core.spi.biodedupe.BioDedupeService#getFile(
	 * java.lang.String)
	 */
	@Override
	public byte[] getFileByAbisRefId(String abisRefId, String process) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), abisRefId,
				"BioDedupeServiceImpl::getFileByAbisRefId()::entry");
		String registrationId = "";
		try {
			List<String> registrationIds = packetInfoManager.getRidByReferenceId(abisRefId);
			if (registrationIds == null || registrationIds.isEmpty()) {
				throw new RegistrationProcessorUnCheckedException(
						PlatformErrorMessages.REGISTRATION_ID_NOT_FOUND.getCode(),
						PlatformErrorMessages.REGISTRATION_ID_NOT_FOUND.getMessage());
			}
			registrationId = registrationIds.get(0);
		} catch (RegistrationProcessorUnCheckedException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					abisRefId, ExceptionUtils.getStackTrace(e));
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), abisRefId,
				"BioDedupeServiceImpl::getFileByAbisRefId()::exit");
		return getFile(registrationId, process);
	}

	private byte[] getFile(String registrationId, String process) {
		byte[] file = null;
		if (registrationId == null || registrationId.isEmpty()) {
			return file;
		}

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				registrationId, "BioDedupeServiceImpl::getFile()::entry");
		try {
			BiometricRecord biometricRecord = packetManagerService.getBiometricsByMappingJsonKey(
					registrationId, MappingJsonConstants.INDIVIDUAL_BIOMETRICS, process, ProviderStageName.BIO_DEDUPE);
			file = cbeffutil.createXML(BIRConverter.convertSegmentsToBIRList(biometricRecord.getSegments()));


		} catch (UnsupportedEncodingException exp) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId,
					PlatformErrorMessages.UNSUPPORTED_ENCODING.getMessage() + ExceptionUtils.getStackTrace(exp));
		} catch (IOException | io.mosip.kernel.core.exception.IOException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId,
					PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage() + ExceptionUtils.getStackTrace(e));
		} catch (Exception e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId,
					PlatformErrorMessages.UNKNOWN_EXCEPTION.getMessage() + ExceptionUtils.getStackTrace(e));
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				registrationId, "BioDedupeServiceImpl::getFile()::exit");
		return file;

	}

}
