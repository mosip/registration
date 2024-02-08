package io.mosip.registration.processor.stages.introducervalidator;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.collections.CollectionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.bioapi.exception.BiometricException;
import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.code.RegistrationExceptionTypeCode;
import io.mosip.registration.processor.core.constant.JsonConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.constant.RegistrationType;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.IntroducerOnHoldException;
import io.mosip.registration.processor.core.exception.PacketDecryptionFailureException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.exception.ValidationFailedException;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.FieldValue;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService;
import io.mosip.registration.processor.packet.storage.utils.BioSdkUtil;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;

@Service
public class IntroducerValidator {

	private static Logger regProcLogger = RegProcessorLogger.getLogger(IntroducerValidator.class);

	public static final String INDIVIDUAL_TYPE_UIN = "UIN";

	@Autowired
	RegistrationExceptionMapperUtil registrationExceptionMapperUtil;

	@Autowired
	private PriorityBasedPacketManagerService packetManagerService;

	@Autowired
	private IdRepoService idRepoService;

	@Autowired
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	@Autowired
	ObjectMapper mapper;

	

	@Autowired
	private Utilities utility;
	
	@Autowired
	private BioSdkUtil bioUtil;

	@Value("#{T(java.util.Arrays).asList('${mosip.regproc.common.before-cbeff-others-attibute.reg-client-versions:}')}")
	private List<String> regClientVersionsBeforeCbeffOthersAttritube;

	/**
	 * Checks if is valid introducer.
	 *
	 * @param registrationId        the registration id
	 * @param registrationStatusDto
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws BiometricException
	 * @throws io.mosip.kernel.core.exception.IOException
	 * @throws Exception 
	 * @throws PacketDecryptionFailureException
	 * @throws RegistrationProcessorCheckedException
	 */
	public void validate(String registrationId, InternalRegistrationStatusDto registrationStatusDto) throws Exception {

		regProcLogger.debug("validate called for registrationId {}", registrationId);

		String introducerUIN = packetManagerService.getFieldByMappingJsonKey(registrationId,
				MappingJsonConstants.INTRODUCER_UIN, registrationStatusDto.getRegistrationType(),
				ProviderStageName.INTRODUCER_VALIDATOR);
		String introducerRID = packetManagerService.getFieldByMappingJsonKey(registrationId,
				MappingJsonConstants.INTRODUCER_RID, registrationStatusDto.getRegistrationType(),
				ProviderStageName.INTRODUCER_VALIDATOR);

		if (isValidIntroducer(introducerUIN, introducerRID)) {
			registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
					.getStatusCode(RegistrationExceptionTypeCode.INTRODUCER_UIN_AND_RID_NOT_IN_PACKET));
			registrationStatusDto.setStatusCode(RegistrationStatusCode.REJECTED.toString());
			regProcLogger.debug("validate called for registrationId {} {}", registrationId,
					StatusUtil.UIN_RID_NOT_FOUND.getMessage());
			throw new BaseCheckedException(StatusUtil.UIN_RID_NOT_FOUND.getMessage(),
					StatusUtil.UIN_RID_NOT_FOUND.getCode());
		}

		if ((introducerUIN == null || introducerUIN.isEmpty())
				&& isValidIntroducerRid(introducerRID, registrationId, registrationStatusDto)) {

			introducerUIN = idRepoService.getUinByRid(introducerRID, utility.getGetRegProcessorDemographicIdentity());

			if (introducerUIN == null) {
				registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
						.getStatusCode(RegistrationExceptionTypeCode.INTRODUCER_UIN_NOT_AVAIALBLE));
				registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
				regProcLogger.debug("validate called for registrationId {} {}", registrationId,
						StatusUtil.INTRODUCER_UIN_NOT_FOUND.getMessage());
				throw new BaseCheckedException(StatusUtil.INTRODUCER_UIN_NOT_FOUND.getMessage(),
						StatusUtil.INTRODUCER_UIN_NOT_FOUND.getCode());
			}

		}
		if (introducerUIN != null && !introducerUIN.isEmpty()) {
			validateIntroducerBiometric(registrationId, registrationStatusDto, introducerUIN);
		} else {
			throw new ValidationFailedException(StatusUtil.INTRODUCER_AUTHENTICATION_FAILED.getMessage(),
					StatusUtil.INTRODUCER_AUTHENTICATION_FAILED.getCode());
		}

		regProcLogger.debug("validate call ended for registrationId {}", registrationId);
	}

	private boolean isValidIntroducer(String introducerUIN, String introducerRID) {
		return ((introducerUIN == null && introducerRID == null) || ((introducerUIN != null && introducerUIN.isEmpty())
				&& (introducerRID != null && introducerRID.isEmpty())));
	}

	/**
	 * Validate introducer rid.
	 *
	 * @param introducerRid         the introducer rid
	 * @param registrationId        the registration id
	 * @param registrationStatusDto
	 * @return true, if successful
	 * @throws BaseCheckedException
	 */
	private boolean isValidIntroducerRid(String introducerRid, String registrationId,
			InternalRegistrationStatusDto registrationStatusDto) throws BaseCheckedException {

		List<InternalRegistrationStatusDto> internalRegistrationStatusDtoList= registrationStatusService.getAllRegistrationStatuses(introducerRid);
			InternalRegistrationStatusDto introducerRegistrationStatusDto=CollectionUtils.isNotEmpty(internalRegistrationStatusDtoList) ?
					internalRegistrationStatusDtoList.stream().filter(s -> RegistrationType.NEW.name().equalsIgnoreCase(s.getRegistrationType())).collect(Collectors.toList()).iterator().next()
					: null;
		if (introducerRegistrationStatusDto != null) {
			if (introducerRegistrationStatusDto.getStatusCode().equals(RegistrationStatusCode.PROCESSING.toString())) {

				registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
						.getStatusCode(RegistrationExceptionTypeCode.ON_HOLD_INTRODUCER_PACKET));

				registrationStatusDto.setStatusComment(StatusUtil.PACKET_ON_HOLD.getMessage());
				registrationStatusDto.setSubStatusCode(StatusUtil.PACKET_ON_HOLD.getCode());
				registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
				regProcLogger.debug("isValidIntroducerRid call ended for registrationId {} {}", registrationId,
						StatusUtil.PACKET_ON_HOLD.getMessage());
				throw new IntroducerOnHoldException(StatusUtil.PACKET_ON_HOLD.getCode(),
						StatusUtil.PACKET_ON_HOLD.getMessage());

			} else if (introducerRegistrationStatusDto.getStatusCode()
					.equals(RegistrationStatusCode.REJECTED.toString())
					|| introducerRegistrationStatusDto.getStatusCode()
							.equals(RegistrationStatusCode.FAILED.toString())) {

				registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
						.getStatusCode(RegistrationExceptionTypeCode.OSI_FAILED_REJECTED_INTRODUCER));

				registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
				regProcLogger.debug("isValidIntroducerRid call ended for registrationId {} {}", registrationId,
						StatusUtil.CHILD_PACKET_REJECTED.getMessage());
				throw new BaseCheckedException(StatusUtil.CHILD_PACKET_REJECTED.getMessage(),
						StatusUtil.CHILD_PACKET_REJECTED.getCode());
			} else {
				return true;
			}

		} else {
			registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
					.getStatusCode(RegistrationExceptionTypeCode.ON_HOLD_INTRODUCER_PACKET));

			registrationStatusDto.setStatusComment(StatusUtil.PACKET_IS_ON_HOLD.getMessage());
			registrationStatusDto.setSubStatusCode(StatusUtil.PACKET_IS_ON_HOLD.getCode());
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
			regProcLogger.debug("isValidIntroducerRid call ended for registrationId {} {}", registrationId,
					StatusUtil.PACKET_ON_HOLD.getMessage());
			throw new IntroducerOnHoldException(StatusUtil.PACKET_ON_HOLD.getCode(),
					StatusUtil.PACKET_ON_HOLD.getMessage());
		  }
	}

	private void validateIntroducerBiometric(String registrationId, InternalRegistrationStatusDto registrationStatusDto,
			String introducerUIN)
			throws Exception {
		BiometricRecord biometricRecord = packetManagerService.getBiometricsByMappingJsonKey(registrationId,
				MappingJsonConstants.INTRODUCER_BIO, registrationStatusDto.getRegistrationType(),
				ProviderStageName.INTRODUCER_VALIDATOR);
		if (biometricRecord != null && biometricRecord.getSegments() != null) {
			biometricRecord = filterExceptionBiometrics(biometricRecord, registrationId,
					registrationStatusDto.getRegistrationType());
			if (biometricRecord != null && biometricRecord.getSegments() != null) {
				validateUserBiometric(registrationId, introducerUIN, biometricRecord.getSegments(), INDIVIDUAL_TYPE_UIN,
						registrationStatusDto);
			} else {
				registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
						.getStatusCode(RegistrationExceptionTypeCode.INTRODUCER_BIOMETRIC_ALL_EXCEPTION_IN_PACKET));
				registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
				regProcLogger.debug("validateIntroducerBiometric call ended for registrationId {} {}", registrationId,
						StatusUtil.INTRODUCER_BIOMETRIC_ALL_EXCEPTION_IN_PACKET.getMessage());
				throw new BaseCheckedException(StatusUtil.INTRODUCER_BIOMETRIC_ALL_EXCEPTION_IN_PACKET.getMessage(),
						StatusUtil.INTRODUCER_BIOMETRIC_ALL_EXCEPTION_IN_PACKET.getCode());
			}

		} else {
			registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
					.getStatusCode(RegistrationExceptionTypeCode.INTRODUCER_BIOMETRIC_NOT_IN_PACKET));
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
			regProcLogger.debug("validateIntroducerBiometric call ended for registrationId {} {}", registrationId,
					StatusUtil.INTRODUCER_BIOMETRIC_FILE_NAME_NOT_FOUND.getMessage());
			throw new BaseCheckedException(StatusUtil.INTRODUCER_BIOMETRIC_FILE_NAME_NOT_FOUND.getMessage(),
					StatusUtil.INTRODUCER_BIOMETRIC_FILE_NAME_NOT_FOUND.getCode());
		}
	}
	
	
	/**
	 * Validate user.
	 *
	 * @param userId                the userid
	 * @param registrationId        the registration id
	 * @param list                  biometric data as BIR object
	 * @param individualType        user type
	 * @param registrationStatusDto
	 * @throws Exception 
	 * @throws  
	 * @throws BiometricException
	 */

	private void validateUserBiometric(String registrationId, String userId, List<BIR> list, String individualType,
			InternalRegistrationStatusDto registrationStatusDto) throws Exception {
		regProcLogger.info("validateUserBiometric call started for registrationId {}", registrationId);
		bioUtil.authenticateBiometrics(userId, individualType, list, registrationStatusDto,StatusUtil.INTRODUCER_AUTHENTICATION_FAILED.getCode(),StatusUtil.INTRODUCER_AUTHENTICATION_FAILED.getCode());
		regProcLogger.debug("validateUserBiometric call ended for registrationId {}", registrationId);
	}

	private BiometricRecord filterExceptionBiometrics(BiometricRecord biometricRecord, String id, String process)
			throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException,
			JSONException {
		String version = getRegClientVersionFromMetaInfo(id, process,
				packetManagerService.getMetaInfo(id, process, ProviderStageName.INTRODUCER_VALIDATOR));
		if (regClientVersionsBeforeCbeffOthersAttritube.contains(version)) {
			return biometricRecord;
		}
		List<BIR> segments = biometricRecord.getSegments().stream().filter(bio -> {
			Map<String, String> othersMap = bio.getOthers().entrySet().stream()
					.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
			return (othersMap == null || !othersMap.containsKey("EXCEPTION")) ? true
					: !(Boolean.parseBoolean(othersMap.get("EXCEPTION")));
		}).collect(Collectors.toList());
		if (segments != null) {
			segments = segments.stream().filter(bio -> !bio.getBdbInfo().getType().get(0).name()
					.equalsIgnoreCase(BiometricType.EXCEPTION_PHOTO.name())).collect(Collectors.toList());
		}
		BiometricRecord biorecord = new BiometricRecord();
		biorecord.setSegments(segments);
		return biorecord;
	}

	private String getRegClientVersionFromMetaInfo(String id, String process, Map<String, String> metaInfoMap)
			throws ApisResourceAccessException, PacketManagerException, IOException, JSONException {
		String metadata = metaInfoMap.get(JsonConstant.METADATA);
		String version = null;
		if (StringUtils.isNotEmpty(metadata)) {
			JSONArray jsonArray = new JSONArray(metadata);

			for (int i = 0; i < jsonArray.length(); i++) {
				if (!jsonArray.isNull(i)) {
					org.json.JSONObject jsonObject = (org.json.JSONObject) jsonArray.get(i);
					FieldValue fieldValue = mapper.readValue(jsonObject.toString(), FieldValue.class);
					if (fieldValue.getLabel().equalsIgnoreCase(JsonConstant.REGCLIENTVERSION)) {
						version = fieldValue.getValue();
						break;
					}
				}
			}
		}
		return version;
	}
}