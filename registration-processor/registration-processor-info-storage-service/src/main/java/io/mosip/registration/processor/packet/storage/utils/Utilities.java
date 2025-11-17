package io.mosip.registration.processor.packet.storage.utils;

import java.io.IOException;
import java.time.*;
import java.util.*;

import io.mosip.kernel.biometrics.commons.CbeffValidator;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.registration.processor.core.constant.*;
import io.mosip.registration.processor.core.exception.*;
import io.mosip.registration.processor.core.idrepo.dto.*;
import io.mosip.registration.processor.core.packet.dto.AdditionalInfoRequestDto;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.repositary.SyncRegistrationRepository;
import io.mosip.registration.processor.status.service.AdditionalInfoRequestService;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.abis.queue.dto.AbisQueueDetails;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.idrepo.dto.IdResponseDTO1;
import io.mosip.registration.processor.core.idrepo.dto.ResponseDTO;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.vid.VidResponseDTO;
import io.mosip.registration.processor.core.queue.factory.MosipQueue;
import io.mosip.registration.processor.core.spi.queue.MosipQueueConnectionFactory;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService;
import io.mosip.registration.processor.packet.storage.dto.ConfigEnum;
import io.mosip.registration.processor.packet.storage.exception.IdRepoAppException;
import io.mosip.registration.processor.packet.storage.exception.QueueConnectionNotFound;
import io.mosip.registration.processor.packet.storage.exception.VidCreationException;
import io.mosip.registration.processor.status.dao.RegistrationStatusDao;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.entity.RegistrationStatusEntity;
import lombok.Data;
import static io.mosip.kernel.core.util.DateUtils.*;

/**
 * The Class Utilities.
 *
 * @author Girish Yarru
 */
@Component

/**
 * Instantiates a new utilities.
 */
@Data
public class Utilities {
	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(Utilities.class);
	private static final String SOURCE = "source";
	private static final String PROCESS = "process";
	private static final String PROVIDER = "provider";
	public static final String EXCEPTION = "EXCEPTION";
	public static final String TRUE = "TRUE";

	private static Map<String, String> readerConfiguration;
	private static Map<String, String> writerConfiguration;

	/** The Constant UIN. */
	private static final String UIN = "UIN";

	/** The Constant FILE_SEPARATOR. */
	public static final String FILE_SEPARATOR = "\\";

	/** The Constant RE_PROCESSING. */
	private static final String RE_PROCESSING = "re-processing";

	/** The Constant HANDLER. */
	private static final String HANDLER = "handler";

	/** The Constant NEW_PACKET. */
	private static final String NEW_PACKET = "New-packet";

	@Value("${mosip.kernel.machineid.length}")
	private int machineIdLength;

	@Value("${mosip.kernel.registrationcenterid.length}")
	private int centerIdLength;

	@Autowired
	private ObjectMapper objMapper;

	@Autowired
	private IdRepoService idRepoService;

	/** The rest client service. */
	@Autowired
	private RegistrationProcessorRestClientService<Object> restClientService;

	/** The mosip connection factory. */
	@Autowired
	private MosipQueueConnectionFactory<MosipQueue> mosipConnectionFactory;

	@Autowired
	SyncRegistrationRepository<SyncRegistrationEntity, String> syncRegistrationRepository;

	@Autowired
	Utility utility;

	@Value("${provider.packetreader.mosip}")
	private String provider;

	/** The config server file storage URL. */
	@Value("${config.server.file.storage.uri}")
	private String configServerFileStorageURL;

	/** The get reg processor identity json. */
	@Value("${registration.processor.identityjson}")
	private String getRegProcessorIdentityJson;

	/** The get reg processor demographic identity. */
	@Value("${registration.processor.demographic.identity}")
	private String getRegProcessorDemographicIdentity;

	/** The get reg processor applicant type. */
	@Value("${registration.processor.applicant.type}")
	private String getRegProcessorApplicantType;

	/** The dob format. */
	@Value("${registration.processor.applicant.dob.format}")
	private String dobFormat;

	/** The elapse time. */
	@Value("${registration.processor.reprocess.elapse.time}")
	private long elapseTime;

	/** The registration processor abis json. */
	@Value("${registration.processor.abis.json}")
	private String registrationProcessorAbisJson;

	/** The id repo update. */
	@Value("${registration.processor.id.repo.update}")
	private String idRepoUpdate;

	/** The vid version. */
	@Value("${registration.processor.id.repo.vidVersion}")
	private String vidVersion;

	@Value("#{'${registration.processor.queue.trusted.packages}'.split(',')}")
	private List<String> trustedPackages;

	@Value("#{'${registration.processor.main-processes}'.split(',')}")
	private List<String> mainProcesses;

	/**
	 * Configuration property that defines the age limit used to determine
	 * whether an applicant is considered an infant or non-infant.
	 */
	@Value("${mosip.kernel.applicant.type.age.limit:5}")
	private String ageLimit;

	/**
	 * Buffer (in months) applied to the applicant's age limit to handle edge cases
	 * where applicants are close to the threshold. Default is {@code 0}.
	 *
	 * <p>Increasing this value provides a safety margin for age calculations near
	 * the eligibility boundary.</p>
	 */
	@Value("${registration.processor.applicant.type.age.limit.buffer:0}")
	private Integer ageLimitBuffer;

	/**
	 * Estimated time (in hours) the system takes to process a packet.
	 * <p>
	 * This value helps calculate the approximate packet creation time.
	 * It is used when no other data source is available to determine the
	 * packet creation time, apart from the identity update time.
	 * The system subtracts this duration from the identity update time
	 * to estimate when the packet was likely created, since the identity
	 * update time includes the packet processing duration as well.
	 * The default value is {@code 0}.
	 * </p>
	 */
	@Value("${registration.processor.expected-packet-processing-duration:0}")
	private Integer expectedPacketProcessingDurationHours;

	@Autowired
	private PriorityBasedPacketManagerService packetManagerService;

	/** The registration status dao. */
	@Autowired
	private RegistrationStatusDao registrationStatusDao;

	@Autowired
	private AdditionalInfoRequestService additionalInfoRequestService;

	/** The Constant INBOUNDQUEUENAME. */
	private static final String INBOUNDQUEUENAME = "inboundQueueName";

	/** The Constant OUTBOUNDQUEUENAME. */
	private static final String OUTBOUNDQUEUENAME = "outboundQueueName";

	/** The Constant ABIS. */
	private static final String ABIS = "abis";

	/** The Constant USERNAME. */
	private static final String USERNAME = "userName";

	/** The Constant PASSWORD. */
	private static final String PASSWORD = "password";

	/** The Constant BROKERURL. */
	private static final String BROKERURL = "brokerUrl";

	/** The Constant TYPEOFQUEUE. */
	private static final String TYPEOFQUEUE = "typeOfQueue";

	/** The Constant NAME. */
	private static final String NAME = "name";

	/** The Constant NAME. */
	private static final String INBOUNDMESSAGETTL = "inboundMessageTTL";

	/** The Constant FAIL_OVER. */
	private static final String FAIL_OVER = "failover:(";

	/** The Constant RANDOMIZE_FALSE. */
	private static final String RANDOMIZE_FALSE = ")?randomize=false";

	private static final String VALUE = "value";

	private JSONObject mappingJsonObject = null;

	private JSONObject regProcessorAbisJson = null;

	public static void initialize(Map<String, String> reader, Map<String, String> writer) {
		readerConfiguration = reader;
		writerConfiguration = writer;
	}

	/**
	 * Gets the json.
	 *
	 * @param configServerFileStorageURL the config server file storage URL
	 * @param uri                        the uri
	 * @return the json
	 */
	public static String getJson(String configServerFileStorageURL, String uri) {
		RestTemplate restTemplate = new RestTemplate();
		return restTemplate.getForObject(configServerFileStorageURL + uri, String.class);
	}

	public String getDefaultSource(String process, ConfigEnum config) {
		Map<String, String> configMap = null;
		if (config.equals(ConfigEnum.READER))
			configMap = readerConfiguration;
		else if (config.equals(ConfigEnum.WRITER))
			configMap = writerConfiguration;

		if (configMap != null) {
			for (Map.Entry<String, String> entry : configMap.entrySet()) {
				String[] values = entry.getValue().split(",");
				String source = null;
				for (String val : values) {
					if (val.startsWith("process:") && val.contains(process))
						for (String sVal : values) {
							if (sVal.startsWith("source:")) {
								source = sVal.replace("source:", "");
								return source;
							}
						}
				}
			}
		}
		return null;
	}

	public String getSource(String packetSegment, String process, String field) throws IOException {
		String source = null;
		JSONObject jsonObject = getRegistrationProcessorMappingJson(packetSegment);
		Object obj = field == null ? jsonObject.get(PROVIDER) : getField(jsonObject, field);
		if (obj != null && obj instanceof ArrayList) {
			List<String> providerList = (List) obj;
			for (String value : providerList) {
				String[] values = value.split(",");
				for (String provider : values) {
					if (provider != null) {
						if (provider.startsWith(PROCESS) && provider.contains(process)) {
							for (String val : values) {
								if (val.startsWith(SOURCE)) {
									return val.replace(SOURCE + ":", "").trim();
								}
							}
						}
					}
				}
			}
		}

		return source;
	}

	private Object getField(JSONObject jsonObject, String field) {
		LinkedHashMap lm = (LinkedHashMap) jsonObject.get(field);
		return lm.get(PROVIDER);
	}

	public String getSourceFromIdField(String packetSegment, String process, String idField) throws IOException {
		JSONObject jsonObject = getRegistrationProcessorMappingJson(packetSegment);
		for (Object key : jsonObject.keySet()) {
			LinkedHashMap hMap = (LinkedHashMap) jsonObject.get(key);
			String value = (String) hMap.get(VALUE);
			if (value != null && value.contains(idField)) {
				return getSource(packetSegment, process, key.toString());
			}
		}
		return null;
	}

	/**
	 * retrieving json from id repo by UIN.
	 *
	 * @param uin the uin
	 * @return the JSON object
	 * @throws ApisResourceAccessException the apis resource access exception
	 * @throws IdRepoAppException          the id repo app exception
	 * @throws IOException                 Signals that an I/O exception has
	 *                                     occurred.
	 */
	private ResponseDTO retrieveIdrepoResponseObj(String uin, String queryParam, String queryParamValue)
			throws ApisResourceAccessException {
		if (uin != null) {
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.UIN.toString(), "",
					"Utilities::retrieveIdrepoResponseObj()::entry");
			List<String> pathSegments = new ArrayList<>();
			pathSegments.add(uin);
			IdResponseDTO1 idResponseDto;

			idResponseDto = (IdResponseDTO1) restClientService.getApi(ApiName.IDREPOGETIDBYUIN, pathSegments,
					null == queryParam ? "" : queryParam, null == queryParamValue ? "" : queryParamValue,
					IdResponseDTO1.class);
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.UIN.toString(), "",
					"Utilities::retrieveIdrepoDocument():: IDREPOGETIDBYUIN GET service call ended Successfully");

			if (idResponseDto == null) {
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.UIN.toString(), "",
						"Utilities::retrieveIdrepoResponseObj()::exit idResponseDto is null");
				return null;
			}
			if (!idResponseDto.getErrors().isEmpty()) {
				List<ErrorDTO> error = idResponseDto.getErrors();
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.UIN.toString(), "",
						"Utilities::retrieveIdrepoResponseObj():: error with error message "
								+ error.get(0).getMessage());
				throw new IdRepoAppException(error.get(0).getMessage());
			}

			return idResponseDto.getResponse();
		}
		return null;
	}

	/**
	 * retrieving identity json ffrom id repo by UIN.
	 *
	 * @param uin the uin
	 * @return the JSON object
	 * @throws ApisResourceAccessException the apis resource access exception
	 * @throws IdRepoAppException          the id repo app exception
	 * @throws IOException                 Signals that an I/O exception has
	 *                                     occurred.
	 */
	public JSONObject retrieveIdrepoJson(String uin)
			throws ApisResourceAccessException, IdRepoAppException, IOException {

		if (uin != null) {
			ResponseDTO idResponseDto = retrieveIdrepoResponseObj(uin, null, null);
			if (idResponseDto != null) {
				String response = objMapper.writeValueAsString(idResponseDto.getIdentity());
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.UIN.toString(), "",
						"Utilities::retrieveIdrepoJson():: IDREPOGETIDBYUIN GET service call ended Successfully");
				try {
					return (JSONObject) new JSONParser().parse(response);
				} catch (org.json.simple.parser.ParseException e) {
					regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.UIN.toString(), "",
							ExceptionUtils.getStackTrace(e));
					throw new IdRepoAppException("Error while parsing string to JSONObject", e);
				}
			} else {
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.UIN.toString(), "",
						"Utilities::retrieveIdrepoJson():: IDREPOGETIDBYUIN GET service Returned NULL");
			}

		}
		return null;
	}

	/**
	 * retrieving identity json ffrom id repo by UIN.
	 *
	 * @param uin the uin
	 * @return the JSON object
	 * @throws ApisResourceAccessException the apis resource access exception
	 * @throws IdRepoAppException          the id repo app exception
	 * @throws IOException                 Signals that an I/O exception has
	 *                                     occurred.
	 */
	public List<io.mosip.registration.processor.core.idrepo.dto.Documents> retrieveIdrepoDocument(String uin)
			throws ApisResourceAccessException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.UIN.toString(), "",
				"Utilities::retrieveIdrepoDocument()::entry");
		ResponseDTO idResponseDto = retrieveIdrepoResponseObj(uin, "type", "all");
		if (idResponseDto != null) {
			return idResponseDto.getDocuments();
		}
		return null;
	}

	/**
	 * Check if uin is present in idrepo
	 *
	 * @param uin
	 * @return
	 * @throws ApisResourceAccessException
	 * @throws IOException
	 */
	public boolean uinPresentInIdRepo(String uin) throws ApisResourceAccessException, IOException {
		return idRepoService.findUinFromIdrepo(uin, getGetRegProcessorDemographicIdentity()) != null;
	}

	/**
	 * Check if uin is missing from Id
	 *
	 * @param errorCode
	 * @param id
	 * @param idType
	 * @return
	 */
	public boolean isUinMissingFromIdAuth(String errorCode, String id, String idType) {
		if (errorCode.equalsIgnoreCase("IDA-MLC-018") && idType != null && idType.equalsIgnoreCase("UIN")) {
			try {
				return uinPresentInIdRepo(id);
			} catch (IOException | ApisResourceAccessException exception) {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.UIN.toString(), "",
						ExceptionUtils.getStackTrace(exception));
				// in case of exception return true so that the request is marked for reprocess
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns all the list of queue details(inbound/outbound address,name,url,pwd)
	 * from abisJson Also validates the abis json fileds(null or not).
	 *
	 * @return the abis queue details
	 * @throws RegistrationProcessorCheckedException the registration processor
	 *                                               checked exception
	 */
	public List<AbisQueueDetails> getAbisQueueDetails() throws RegistrationProcessorCheckedException {
		List<AbisQueueDetails> abisQueueDetailsList = new ArrayList<>();

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"Utilities::getAbisQueueDetails()::entry");

		try {
			if(regProcessorAbisJson==null) {
				String registrationProcessorAbis = Utilities.getJson(configServerFileStorageURL, registrationProcessorAbisJson);
				regProcessorAbisJson = JsonUtil.objectMapperReadValue(registrationProcessorAbis, JSONObject.class);
			}
			JSONArray regProcessorAbisArray = JsonUtil.getJSONArray(regProcessorAbisJson, ABIS);

			for (Object jsonObject : regProcessorAbisArray) {
				AbisQueueDetails abisQueueDetails = new AbisQueueDetails();
				JSONObject json = new JSONObject((Map) jsonObject);
				String userName = validateAbisQueueJsonAndReturnValue(json, USERNAME);
				String password = validateAbisQueueJsonAndReturnValue(json, PASSWORD);
				String brokerUrl = validateAbisQueueJsonAndReturnValue(json, BROKERURL);
				String failOverBrokerUrl = FAIL_OVER + brokerUrl + "," + brokerUrl + RANDOMIZE_FALSE;
				String typeOfQueue = validateAbisQueueJsonAndReturnValue(json, TYPEOFQUEUE);
				String inboundQueueName = validateAbisQueueJsonAndReturnValue(json, INBOUNDQUEUENAME);
				String outboundQueueName = validateAbisQueueJsonAndReturnValue(json, OUTBOUNDQUEUENAME);
				String queueName = validateAbisQueueJsonAndReturnValue(json, NAME);
				int inboundMessageTTL = validateAbisQueueJsonAndReturnIntValue(json, INBOUNDMESSAGETTL);
				MosipQueue mosipQueue = mosipConnectionFactory.createConnection(typeOfQueue, userName, password,
						failOverBrokerUrl, trustedPackages);
				if (mosipQueue == null)
					throw new QueueConnectionNotFound(
							PlatformErrorMessages.RPR_PIS_ABIS_QUEUE_CONNECTION_NULL.getMessage());

				abisQueueDetails.setMosipQueue(mosipQueue);
				abisQueueDetails.setInboundQueueName(inboundQueueName);
				abisQueueDetails.setOutboundQueueName(outboundQueueName);
				abisQueueDetails.setName(queueName);
				abisQueueDetails.setInboundMessageTTL(inboundMessageTTL);
				abisQueueDetailsList.add(abisQueueDetails);

			}
		} catch (IOException e) {
			throw new RegistrationProcessorCheckedException(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getCode(),
					PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage(), e);
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"Utilities::getAbisQueueDetails()::exit");

		return abisQueueDetailsList;

	}

	/**
	 * Gets registration processor mapping json from config and maps to
	 * RegistrationProcessorIdentity java class.
	 *
	 * @return the registration processor identity json
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public JSONObject getRegistrationProcessorMappingJson(String packetSegment) throws IOException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"Utilities::getRegistrationProcessorMappingJson()::entry");

		if (mappingJsonObject == null) {
			String mappingJsonString = Utilities.getJson(configServerFileStorageURL, getRegProcessorIdentityJson);
			mappingJsonObject = objMapper.readValue(mappingJsonString, JSONObject.class);

		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"Utilities::getRegistrationProcessorMappingJson()::exit");
		return JsonUtil.getJSONObject(mappingJsonObject, packetSegment);

	}

	public String getMappingJsonValue(String key, String packetSegment) throws IOException {
		JSONObject jsonObject = getRegistrationProcessorMappingJson(packetSegment);
		Object obj = jsonObject.get(key);
		if (obj instanceof LinkedHashMap) {
			LinkedHashMap hm = (LinkedHashMap) obj;
			return hm.get("value") != null ? hm.get("value").toString() : null;
		}
		return jsonObject.get(key) != null ? jsonObject.get(key).toString() : null;

	}

	/**
	 * Gets the elapse status.
	 *
	 * @param registrationStatusDto the registration status dto
	 * @param transactionType       the transaction type
	 * @return the elapse status
	 */
	public String getElapseStatus(InternalRegistrationStatusDto registrationStatusDto, String transactionType) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"Utilities::getElapseStatus()::entry");

		if (registrationStatusDto.getLatestTransactionTypeCode().equalsIgnoreCase(transactionType)) {
			LocalDateTime createdDateTime = registrationStatusDto.getCreateDateTime();
			LocalDateTime currentDateTime = LocalDateTime.now();
			Duration duration = Duration.between(createdDateTime, currentDateTime);
			long secondsDiffernce = duration.getSeconds();
			if (secondsDiffernce > elapseTime) {
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
						"Utilities::getElapseStatus()::exit and value is:  " + RE_PROCESSING);

				return RE_PROCESSING;
			} else {
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
						"Utilities::getElapseStatus()::exit and value is:  " + HANDLER);

				return HANDLER;
			}
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"Utilities::getElapseStatus()::exit and value is:  " + NEW_PACKET);

		return NEW_PACKET;
	}

	/**
	 * Gets the latest transaction id.
	 *
	 * @param registrationId the registration id
	 * @return the latest transaction id
	 */
	public String getLatestTransactionId(String registrationId, String process, int iteration,
			String workflowInstanceId) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "Utilities::getLatestTransactionId()::entry");
		RegistrationStatusEntity entity = registrationStatusDao.find(registrationId, process, iteration,
				workflowInstanceId);
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "Utilities::getLatestTransactionId()::exit");
		return entity != null ? entity.getLatestRegistrationTransactionId() : null;

	}

	/**
	 * retrieve UIN from IDRepo by registration id.
	 *
	 * @param regId the reg id
	 * @return the JSON object
	 * @throws ApisResourceAccessException the apis resource access exception
	 * @throws IdRepoAppException          the id repo app exception
	 * @throws IOException                 Signals that an I/O exception has
	 *                                     occurred.
	 */
	public JSONObject idrepoRetrieveIdentityByRid(String regId) throws ApisResourceAccessException, IdRepoAppException, IOException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				regId, "Utilities::retrieveUIN()::entry");

		if (regId != null) {
			List<String> pathSegments = new ArrayList<>();
			pathSegments.add(regId);
			IdResponseDTO1 idResponseDto;
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, "Utilities::retrieveUIN():: RETRIEVEIDENTITYFROMRID GET service call Started");

			idResponseDto = (IdResponseDTO1) restClientService.getApi(ApiName.RETRIEVEIDENTITYFROMRID, pathSegments, "",
					"", IdResponseDTO1.class);
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.UIN.toString(), "",
					"Utilities::retrieveUIN():: RETRIEVEIDENTITYFROMRID GET service call ended successfully");

			if (!idResponseDto.getErrors().isEmpty()) {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), regId,
						"Utilities::retrieveUIN():: error with error message "
								+ PlatformErrorMessages.RPR_PVM_INVALID_UIN.getMessage() + " "
								+ idResponseDto.getErrors().toString());
				throw new IdRepoAppException(
						PlatformErrorMessages.RPR_PVM_INVALID_UIN.getMessage() + idResponseDto.getErrors().toString());
			}
			String response = objMapper.writeValueAsString(idResponseDto.getResponse().getIdentity());
			try {
				return (JSONObject) new JSONParser().parse(response);
			} catch (org.json.simple.parser.ParseException e) {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.UIN.toString(), "",
						ExceptionUtils.getStackTrace(e));
				throw new IdRepoAppException("Error while parsing string to JSONObject", e);
			}

		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"Utilities::retrieveUIN()::exit regId is null");

		return null;
	}

	/**
	 * Validate abis queue json and return value.
	 *
	 * @param jsonObject the json object
	 * @param key        the key
	 * @return the string
	 */
	private String validateAbisQueueJsonAndReturnValue(JSONObject jsonObject, String key) {

		String value = JsonUtil.getJSONValue(jsonObject, key);
		if (value == null) {

			throw new RegistrationProcessorUnCheckedException(
					PlatformErrorMessages.ABIS_QUEUE_JSON_VALIDATION_FAILED.getCode(),
					PlatformErrorMessages.ABIS_QUEUE_JSON_VALIDATION_FAILED.getMessage() + "::" + key);
		}

		return value;
	}

	/**
	 * Validate abis queue json and return long value.
	 *
	 * @param jsonObject the json object
	 * @param key        the key
	 * @return the long value
	 */
	private int validateAbisQueueJsonAndReturnIntValue(JSONObject jsonObject, String key) {

		Integer value = JsonUtil.getJSONValue(jsonObject, key);
		if (value == null) {

			throw new RegistrationProcessorUnCheckedException(
					PlatformErrorMessages.ABIS_QUEUE_JSON_VALIDATION_FAILED.getCode(),
					PlatformErrorMessages.ABIS_QUEUE_JSON_VALIDATION_FAILED.getMessage() + "::" + key);
		}

		return value.intValue();
	}

	/**
	 * Gets the uin by vid.
	 *
	 * @param vid the vid
	 * @return the uin by vid
	 * @throws ApisResourceAccessException the apis resource access exception
	 * @throws VidCreationException        the vid creation exception
	 */
	@SuppressWarnings("unchecked")
	public String getUinByVid(String vid) throws ApisResourceAccessException, VidCreationException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"Utilities::getUinByVid():: entry");
		List<String> pathSegments = new ArrayList<>();
		pathSegments.add(vid);
		String uin = null;
		VidResponseDTO response;
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"Stage::methodname():: RETRIEVEIUINBYVID GET service call Started");

		response = (VidResponseDTO) restClientService.getApi(ApiName.GETUINBYVID, pathSegments, "", "",
				VidResponseDTO.class);
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.UIN.toString(), "",
				"Utilities::getUinByVid():: RETRIEVEIUINBYVID GET service call ended successfully");

		if (!response.getErrors().isEmpty()) {
			throw new VidCreationException(PlatformErrorMessages.RPR_PGS_VID_EXCEPTION.getMessage(),
					"VID creation exception");

		} else {
			uin = response.getResponse().getUin();
		}
		return uin;
	}

	/**
	 * Retrieve idrepo json status.
	 *
	 * @param uin the uin
	 * @return the string
	 * @throws ApisResourceAccessException the apis resource access exception
	 * @throws IdRepoAppException          the id repo app exception
	 */
	public String retrieveIdrepoJsonStatus(String uin) throws ApisResourceAccessException, IdRepoAppException {
		String response = null;
		if (uin != null) {
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.UIN.toString(), "",
					"Utilities::retrieveIdrepoJson()::entry");
			List<String> pathSegments = new ArrayList<>();
			pathSegments.add(uin);
			IdResponseDTO1 idResponseDto;

			idResponseDto = (IdResponseDTO1) restClientService.getApi(ApiName.IDREPOGETIDBYUIN, pathSegments, "", "",
					IdResponseDTO1.class);
			if (idResponseDto == null) {
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.UIN.toString(), "",
						"Utilities::retrieveIdrepoJson()::exit idResponseDto is null");
				return null;
			}
			if (!idResponseDto.getErrors().isEmpty()) {
				List<ErrorDTO> error = idResponseDto.getErrors();
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.UIN.toString(), "",
						"Utilities::retrieveIdrepoJson():: error with error message " + error.get(0).getMessage());
				throw new IdRepoAppException(error.get(0).getMessage());
			}

			response = idResponseDto.getResponse().getStatus();

			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.UIN.toString(), "",
					"Utilities::retrieveIdrepoJson():: IDREPOGETIDBYUIN GET service call ended Successfully");
		}

		return response;
	}

	public String getRefId(String id, String refId) {
		if (StringUtils.isNotEmpty(refId))
			return refId;

		String centerId = id.substring(0, centerIdLength);
		String machineId = id.substring(centerIdLength, centerIdLength + machineIdLength);
		return centerId + "_" + machineId;
	}

public String getInternalProcess(Map<String, String> additionalProcessMap, String externalProcess){
		if (externalProcess == null) return "";
		String internalProcess = additionalProcessMap.get(externalProcess);
		return internalProcess != null ? internalProcess : "";
	}

	public int getIterationForSyncRecord(Map<String, String> additionalProcessMap, String process, String additionalRequestId) throws IOException {
		if(mainProcesses.contains(process) || mainProcesses.contains(getInternalProcess(additionalProcessMap, process)))
			return 1;
		AdditionalInfoRequestDto additionalInfoRequestDto = additionalInfoRequestService
				.getAdditionalInfoRequestByReqId(additionalRequestId);
		if (additionalInfoRequestDto == null)
			throw new AdditionalInfoIdNotFoundException();

		return additionalInfoRequestDto.getAdditionalInfoIteration();
	}

	/**
	 * Determines whether the applicant was an infant at the time their last packet was processed in the system
	 *
	 * @param registrationId
	 * @param registrationType
	 * @param stageName
	 * @return
	 * @throws ApisResourceAccessException
	 * @throws IOException
	 * @throws JsonProcessingException
	 * @throws PacketManagerException
	 */
	public boolean wasInfantWhenLastPacketProcessed(String registrationId, String registrationType, ProviderStageName stageName) throws ApisResourceAccessException, IOException,
			JsonProcessingException, PacketManagerException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "utility::wasInfantWhenLastPacketProcessed()::entry");

		String packetUin = utility.getUIn(registrationId, registrationType, stageName);
		if (packetUin == null || packetUin.trim().isEmpty()) {
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
					LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId,
					"UIN not found in the packet");
			return false;
		}
		// Get created date and time from idrepo using above UIN
		JSONObject idvidResponse = idRepoService.getIdJsonFromIDRepo(packetUin, getGetRegProcessorDemographicIdentity());

		LocalDate lastPacketProcessedDate = resolveLastPacketProcessedDate(registrationId, packetUin, idvidResponse);

		if (lastPacketProcessedDate == null) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
					"Unable to compute the creation date of the last processed packet");
			throw new PacketDateComputationException(PlatformErrorMessages.RPR_BDD_UNABLE_TO_COMPUTE_CREATION_DATE.getCode(),
					PlatformErrorMessages.RPR_BDD_UNABLE_TO_COMPUTE_CREATION_DATE.getMessage());
		}

		LocalDate dobOfApplicant = getDateOfBirthFromIdrepo(registrationId, idvidResponse);
		if (dobOfApplicant == null) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
					"Unable to obtain the date of birth");
			throw new PacketDateComputationException(PlatformErrorMessages.RPR_BDD_UNABLE_TO_COMPUTE_CREATION_DATE.getCode(),
					PlatformErrorMessages.RPR_BDD_UNABLE_TO_COMPUTE_CREATION_DATE.getMessage());
		}
		int age = calculateAgeAtLastPacketProcessing(dobOfApplicant, lastPacketProcessedDate, registrationId);

		regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "utility::wasInfantWhenLastPacketProcessed()::exit. age: {}", age);

		return age < getEffectiveAgeLimit();
	}


	/**
	 * Attempts to resolve the last packet processed date using multiple strategies in order
	 *
	 * @param registrationId
	 * @param packetUin
	 * @param idvidResponse
	 * @return
	 * @throws ApisResourceAccessException
	 * @throws IOException
	 */
	private LocalDate resolveLastPacketProcessedDate(String registrationId, String packetUin, JSONObject idvidResponse) throws ApisResourceAccessException, IOException {
		// 1. Try direct lookup
		LocalDate date = getLastProcessedPacketCreatedDate(registrationId, idvidResponse);
		if (date != null) {
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
					"resolveLastPacketProcessedDate() :: Successfully resolved packet creation date from idvidResponse. date : {}", date);
			return date;
		}
		// Fetching last processed rid from idVidMetadata
		IdVidMetadataResponse idVidMetadataResponse = getIdVidMetadata(packetUin, null);
		if (idVidMetadataResponse == null) {
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
					"resolveLastPacketProcessedDate() :: idVidMetadataResponse is null");
			return null;
		}

		// 2. Try from packetId (yyyyMMddHHmmss)
		date = getPacketCreatedDateFromSyncRegistration(idVidMetadataResponse.getRid());
		if (date != null) {
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
					"resolveLastPacketProcessedDate() :: Successfully resolved packet creation date from packetId. date : {}", date);
			return date;
		}

		// 3. Try from RID directly (yyyyMMddHHmmss)
		date = getPacketCreatedDateFromRid(idVidMetadataResponse.getRid());
		if (date != null) {
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
					"resolveLastPacketProcessedDate() :: Successfully resolved packet creation date from RID. date : {}", date);
			return date;
		}

		// 4. Fallback to IdRepo update date
		LocalDate approxCreatedDateTime = computePacketCreatedFromIdentityUpdate(idVidMetadataResponse, registrationId);
		if (approxCreatedDateTime != null) {
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
					"resolveLastPacketProcessedDate() :: Resolved packet creation date from identity update. approxCreatedDateTime : {}", approxCreatedDateTime);
			return approxCreatedDateTime;
		}

		regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
				"resolveLastPacketProcessedDate() :: Unable to resolve packet creation date");
		return null;
	}

	 /**
	 * Retrieves the effective age limit for the applicant by adding the configured buffer (in months)
	 * to the base age limit. This helps prevent exclusion of applicants who are close to the age threshold
	 * due to edge cases in age calculation
	 */
	public int getEffectiveAgeLimit() {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"utility::getEffectiveAgeLimit():: ageLimit: {}, ageLimitBuffer: {} ", ageLimit, ageLimitBuffer);
		return Integer.parseInt(ageLimit) + ageLimitBuffer;
	}

	/**
	 * Retrieves the created date of the last packet that was processed for the applicant
	 *
	 * @param rid
	 * @param idvidResponse
	 * @return
	 * @throws IOException
	 */
	private LocalDate getLastProcessedPacketCreatedDate(String rid, JSONObject idvidResponse) throws IOException {

		String packetCreatedDateTimeIsoFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"utility::getLastProcessedPacketCreatedDate()::entry");

		String packetCreatedOnFieldName = getMappedFieldName(MappingJsonConstants.PACKET_CREATED_ON);

		// Check if the response object itself is null
		if (idvidResponse == null) {
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
					"getLastProcessedPacketCreatedDate() :: idvidResponse is null");
			return null;
		}

		// Check if the key exists in the response
		if (!idvidResponse.containsKey(packetCreatedOnFieldName)) {
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
					"getLastProcessedPacketCreatedDate() :: fieldName does not exist in idvidResponse : {} ", packetCreatedOnFieldName);
			return null;
		}

		// Safely get the value
		String packetCreatedOn = JsonUtil.getJSONValue(idvidResponse, packetCreatedOnFieldName);

		// Check if the value itself is null
		if (packetCreatedOn == null) {
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
					"getLastProcessedPacketCreatedDate() :: fieldValue does not exist in idvidResponse : {} ", packetCreatedOnFieldName);
			return null;
		}
		LocalDate packetCreatedDate = parseToLocalDate(packetCreatedOn, packetCreatedDateTimeIsoFormat);
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"utility::getLastProcessedPacketCreatedDate()::exit");
		return packetCreatedDate;
	}

	public String getMappedFieldName(String key) throws IOException {
		JSONObject regProcessorIdentityJson = getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);
		return JsonUtil.getJSONValue(
				JsonUtil.getJSONObject(regProcessorIdentityJson, key),
				MappingJsonConstants.VALUE);

	}

	/**
	 * Extract Date of Birth from IDRepo using RID
	 *
	 * @param rid
	 * @param idvidResponse
	 * @return
	 * @throws IOException
	 */
	public LocalDate getDateOfBirthFromIdrepo(String rid, JSONObject idvidResponse) throws IOException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"utility::getDateOfBirthFromIdrepo()::entry");

		// Step 2: Fetch DOB dynamically via mapping
		String dateOfBirthFieldName = getMappedFieldName(MappingJsonConstants.DOB);

		// Check if the key exists in the response
		if (!idvidResponse.containsKey(dateOfBirthFieldName)) {
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
					"utility::getDateOfBirthFromIdrepo():: fieldName does not exist in idvidResponse : {} ", dateOfBirthFieldName);
			return null;
		}

		// Safely get the value
		String dobValue = JsonUtil.getJSONValue(idvidResponse, dateOfBirthFieldName);

		// Check if the value itself is null
		if (dobValue == null) {
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
					"utility::getDateOfBirthFromIdrepo():: fieldValue does not exist in idvidResponse : {} ", dateOfBirthFieldName);
			return null;
		}

		LocalDate dob = parseToLocalDate(dobValue, dobFormat);
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"utility::getDateOfBirthFromIdrepo()::exit with dob");
		return dob;
	}

	/**
	 * Calculates the age of an applicant at the time of the last packet processing
	 *
	 * @param dateOfBirth
	 * @param lastPacketProcessingDate
	 * @param rid
	 * @return
	 */
	public int calculateAgeAtLastPacketProcessing(LocalDate dateOfBirth, LocalDate lastPacketProcessingDate, String rid) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"utility::calculateAgeAtLastPacketProcessing():: entry");

		// Calculate the period between the two dates
		Period period = Period.between(dateOfBirth, lastPacketProcessingDate);

		// Extract years from the period
		int ageInYears = period.getYears();

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"utility::calculateAgeAtLastPacketProcessing():: exit");

		// Return age in years (as per the original method signature)
		return ageInYears;
	}

	/**
	 * Obtain the IdVidMetadata for applicant
	 *
	 * @param individualId
	 * @param idType
	 * @return
	 * @throws IOException
	 * @throws ApisResourceAccessException
	 */
	public IdVidMetadataResponse getIdVidMetadata(String individualId, String idType) throws IOException, ApisResourceAccessException {
		IdVidMetadataRequest idVidMetadataRequest = new IdVidMetadataRequest();
		idVidMetadataRequest.setIndividualId(individualId);
		idVidMetadataRequest.setIdType(idType);
		IdVidMetadataResponse idVidMetadataResponse = idRepoService.searchIdVidMetadata(idVidMetadataRequest);
		return idVidMetadataResponse;
	}

	/**
	 * Retrieves the packet creation date for the given RID from the sync registration table
	 *
	 * @param rid
	 * @return
	 */
	public LocalDate getPacketCreatedDateFromSyncRegistration(String rid) {

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
				LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"utility::getPacketCreatedDateFromSyncRegistration():: entry");

		// Fetch latest packetId based on createOn
		String packetId = syncRegistrationRepository.findByRegistrationId(rid).stream()
				.max(Comparator.comparing(SyncRegistrationEntity::getCreateDateTime)) // latest record
				.map(SyncRegistrationEntity::getPacketId) // extract packetId
				.orElse(null); // if no records, return null

		LocalDate packetCreatedDate = extractPacketCreatedDate(packetId);

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
				LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"utility::getPacketCreatedDateFromSyncRegistration():: exit with packetCreatedDate: " + packetCreatedDate);

		return packetCreatedDate;
	}

	private LocalDate extractPacketCreatedDate(String id) {
		String packetCreatedDateFormat = "yyyyMMddHHmmss";

		if (id == null || id.length() < 14) {
			regProcLogger.debug("Provided id is null or shorter than 14 characters. Cannot extract packet creation date. id : {} " , id);
			return null;
		}

		String dateStr = id.substring(id.length() - 14);
		return parseToLocalDate(dateStr, packetCreatedDateFormat);
	}


	/**
	 * Wrapper that returns LocalDate only (ignores time)
	 *
	 * @param dateString
	 * @param dateFormat
	 * @return
	 */
	public LocalDate parseToLocalDate(String dateString, String dateFormat) {

		// Log the incoming parameters for debugging
		regProcLogger.debug("Attempting to parse date: {} with format: {}", dateString, dateFormat);

		LocalDateTime ldt = null;

		try {
			// Parse the date string into LocalDateTime
			ldt = parseUTCToLocalDateTime(dateString, dateFormat);

			// Perform validation if parsing was successful
			if (ldt != null) {
				LocalDateTime now = LocalDateTime.now();

				// Check if date is in the future
				if (after(ldt, now)) {
					regProcLogger.error("Parsed LocalDateTime occurs in the future: {}", ldt);
					return null;
				}

				// Check if date is older than 100 years
				LocalDateTime hundredYearsAgo = now.minusYears(200);
				if (before(ldt, hundredYearsAgo)) {
					regProcLogger.error("Date is older than 200 years : {}", ldt);
					return null;
				}
			}
		} catch (io.mosip.kernel.core.exception.ParseException e) {
				regProcLogger.debug("Failed to parse date: {} with format: {}", dateString, dateFormat, e);
				return null;
		}

		return (ldt != null) ? ldt.toLocalDate() : null;
	}

	/**
	 * Extracts the packet creation date from the given Registration ID (RID) by interpreting its last 14 digits as a timestamp in the format {@code yyyyMMddHHmmss}
	 *
	 * @param rid
	 * @return
	 */
	public LocalDate getPacketCreatedDateFromRid(String rid) {

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
				LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"utility::getPacketCreatedDateFromRid():: entry");

		LocalDate packetCreatedDate = extractPacketCreatedDate(rid);

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
				LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"utility::getPacketCreatedDateFromRid():: exit with packetCreatedDate: " + packetCreatedDate);

		return packetCreatedDate;
	}

	/**
	 * Computes the approximate packet creation date (for the packet that updated the identity)
	 *
	 * @param idVidMetadataDTO
	 * @param processingRid
	 * @return
	 */
	public LocalDate computePacketCreatedFromIdentityUpdate(IdVidMetadataResponse idVidMetadataDTO, String processingRid) {

		String packetCreatedDateTimeIsoFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
		String updatedOn = idVidMetadataDTO.getUpdatedOn();
		regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), processingRid,
				"computePacketCreatedFromIdentityUpdate() :: expectedPacketProcessingDurationHours: {}", expectedPacketProcessingDurationHours);

		if (updatedOn == null) {
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), processingRid,
					"computePacketCreatedFromIdentityUpdate() :: updatedOn is null for RID hence falling back to createdOn.");
			updatedOn = idVidMetadataDTO.getCreatedOn();
		}

		try {
			LocalDateTime referenceDateTime = parseUTCToLocalDateTime(updatedOn, packetCreatedDateTimeIsoFormat);
			LocalDateTime adjustedDateTime = referenceDateTime.minusHours(expectedPacketProcessingDurationHours);

			return adjustedDateTime.toLocalDate();

		} catch (io.mosip.kernel.core.exception.ParseException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), processingRid,
					"computePacketCreatedFromIdentityUpdate() :: Failed to parse date {} using format {}. Exception: {}",
					updatedOn, packetCreatedDateTimeIsoFormat, e.getMessage());

		} catch (Exception e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), processingRid,
					"computePacketCreatedFromIdentityUpdate() :: Unexpected error while computing packet creation date. Exception: {}",
					e.getMessage());
		}
		return null;
	}

	/**
	 * Get the BiometricRecord from the IdRepo for a given UIN
	 *
	 * @param uin
	 * @param rid
	 * @return
	 * @throws ApisResourceAccessException
	 * @throws IOException
	 */
	public BiometricRecord getBiometricRecordfromIdrepo(String uin, String rid) throws ApisResourceAccessException, IOException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"utility::getBiometricRecordfromIdrepo():: entry");

		// Step 1: Retrieve all documents from IDRepo
		List<Documents> documents = retrieveIdrepoDocument(uin);

		// Step 2: Load mapping JSON and extract the label for individual biometrics
		JSONObject regProcessorIdentityJson = getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);
		String individualBiometricsLabel = JsonUtil.getJSONValue(
				JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.INDIVIDUAL_BIOMETRICS),
				MappingJsonConstants.VALUE);

		// Step 3: Find the biometric document
		String biometricDoc = null;
		for (Documents doc : documents) {
			if (doc.getCategory() != null && doc.getCategory().equalsIgnoreCase(individualBiometricsLabel)) {
				biometricDoc = doc.getValue();
				break;
			}
		}

		if (biometricDoc == null) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
					"No biometric document found in IDRepo");
			throw new BiometricClassificationException(PlatformErrorMessages.RPR_BDD_UNABLE_TO_FETCH_BIOMETRIC_INFO.getCode(),
					PlatformErrorMessages.RPR_BDD_UNABLE_TO_FETCH_BIOMETRIC_INFO.getMessage());
		}

		try {
			// Step 4: Decode and convert to BiometricRecord
			byte[] bi = CryptoUtil.decodeURLSafeBase64(biometricDoc);

			BIR birs = CbeffValidator.getBIRFromXML(bi);

			BiometricRecord biometricRecord = new BiometricRecord();
			// Copy "others" metadata if present
			if(birs.getOthers() != null) {
				HashMap<String, String> others = new HashMap<>();
				birs.getOthers().entrySet().forEach(e -> {
					others.put(e.getKey(), e.getValue());
				});
				biometricRecord.setOthers(others);
			}

			biometricRecord.setSegments(birs.getBirs());

			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
					"utility::getBiometricRecordfromIdrepo():: exit");
			return biometricRecord;

		} catch (Exception e) {
			// Any other error during XML parsing or processing
			String errorMsg = "Unexpected error while getting biometric document";
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					rid, errorMsg, e);
			throw new BiometricClassificationException(
					PlatformErrorMessages.RPR_BDD_UNABLE_TO_FETCH_BIOMETRIC_INFO.getCode(),
					PlatformErrorMessages.RPR_BDD_UNABLE_TO_FETCH_BIOMETRIC_INFO.getMessage());
		}
	}

	/**
	 * Checks whether all biometric segments are marked as exceptions excluding FACE and EXCEPTION_PHOTO types, which cannot be marked as exceptions
	 *
	 * @param birs
	 * @param rid
	 * @return
	 * @throws BiometricClassificationException
	 */
	public boolean allBiometricHaveException(List<BIR> birs, String rid) throws BiometricClassificationException {

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"utility::allBiometricHaveException():: entry");

		if (birs == null || birs.isEmpty()) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
					"utility::allBiometricHaveException():: Biometric list is null or empty");
			throw new BiometricClassificationException(
					PlatformErrorMessages.RPR_BDD_UNABLE_TO_FETCH_BIOMETRIC_INFO.getCode(),
					PlatformErrorMessages.RPR_BDD_UNABLE_TO_FETCH_BIOMETRIC_INFO.getMessage());
		}

		boolean hasOthers = hasBiometricWithOthers(birs);
		regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"utility::allBiometricHaveException():: hasOthers: {} ",hasOthers);

		for (BIR bir : birs) {
			BiometricType type = bir.getBdbInfo().getType().get(0);
			boolean isFaceOrExceptionPhoto = type == BiometricType.FACE || type == BiometricType.EXCEPTION_PHOTO;

			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
					"utility::allBiometricHaveException():: Checking biometric type: {} ",type);

			if (hasOthers) {
				if (!isFaceOrExceptionPhoto) {
					String exceptionValue = bir.getOthers().get(EXCEPTION);
					regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
							"utility::allBiometricHaveException()::  Biometric type: {},  exceptionValue: {}", type, exceptionValue);

					if (exceptionValue == null || !exceptionValue.equalsIgnoreCase(TRUE)) {
						regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
								"utility::allBiometricHaveException():: Biometric type: {} does not have exception", type);
						return false;
					}
				}
			} else {
				if (!isFaceOrExceptionPhoto) {
					regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
							"utility::allBiometricHaveException():: Biometric type: {} does not have exception", type);
					return false;
				}
			}
		}

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"utility::allBiometricHaveException():: exit");
		return true;
	}

	/**
	 * Checks whether any biometric record in the given list contains non-empty "others" data,
	 * 	which can be used to determine if the biometric was generated using the new or old version
	 *
	 * @param bir
	 * @return
	 */
	public boolean hasBiometricWithOthers(List<BIR> bir){
		if (bir == null || bir.isEmpty()) {
			return false;
		}
		return bir.stream()
				.anyMatch(bi -> bi.getOthers() != null && !bi.getOthers().isEmpty());
	}

	/**
	 * Checks whether all biometric segments are marked as exceptions for a registration Id
	 *
	 * @param rid
	 * @param registrationType
	 * @param stageName
	 * @return
	 * @throws BiometricClassificationException
	 */
	public boolean allBiometricHaveException(String rid, String registrationType, ProviderStageName stageName) throws BiometricClassificationException {
		try {
			String uin = utility.getUIn(rid, registrationType, stageName);
			BiometricRecord bm = getBiometricRecordfromIdrepo(uin, rid);
			return allBiometricHaveException(bm.getSegments(), rid);
		} catch (Exception e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
					"utility::allBiometricHaveException():: Error while classifying biometric exceptions", e);
			throw new BiometricClassificationException(
					PlatformErrorMessages.RPR_BDD_UNABLE_TO_FETCH_BIOMETRIC_INFO.getCode(),
					PlatformErrorMessages.RPR_BDD_UNABLE_TO_FETCH_BIOMETRIC_INFO.getMessage());
		}
	}

	/**
	 * Retrieves the packet creation date from the packet for a given registrationId
	 *
	 * @param rid
	 * @param process
	 * @param stageName
	 * @return
	 * @throws PacketManagerException
	 * @throws ApisResourceAccessException
	 * @throws IOException
	 * @throws JsonProcessingException
	 */
	public String retrieveCreatedDateFromPacket(String rid, String process, ProviderStageName stageName)
			throws PacketManagerException, ApisResourceAccessException, IOException, JsonProcessingException {

		Map<String, String> metaInfo = packetManagerService.getMetaInfo(rid, process, stageName);
		String packetCreatedDateTime = metaInfo.get(JsonConstant.CREATIONDATE);

		if (packetCreatedDateTime != null && !packetCreatedDateTime.isEmpty()) {
			return packetCreatedDateTime;
		}

		regProcLogger.error(
				LoggerFileConstant.SESSIONID.toString(),
				LoggerFileConstant.REGISTRATIONID.toString(),
				" -- " + rid,
				PlatformErrorMessages.RPR_PVM_PACKET_CREATED_DATE_TIME_EMPTY_OR_NULL.getMessage()
		);

		return null;
	}
}