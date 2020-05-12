package io.mosip.registration.processor.packet.storage.utils;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import io.mosip.registration.processor.packet.utility.exception.ApiNotAccessibleException;
import io.mosip.registration.processor.packet.utility.exception.PacketDecryptionFailureException;
import org.apache.commons.io.IOUtils;
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
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.abis.queue.dto.AbisQueueDetails;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import io.mosip.registration.processor.core.constant.JsonConstant;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.PacketFiles;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorUnCheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.idrepo.dto.Documents;
import io.mosip.registration.processor.core.idrepo.dto.IdRequestDto;
import io.mosip.registration.processor.core.idrepo.dto.IdResponseDTO;
import io.mosip.registration.processor.core.idrepo.dto.IdResponseDTO1;
import io.mosip.registration.processor.core.idrepo.dto.RequestDto;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.packet.dto.PacketMetaInfo;
import io.mosip.registration.processor.core.packet.dto.vid.VidResponseDTO;
import io.mosip.registration.processor.core.queue.factory.MosipQueue;
import io.mosip.registration.processor.core.spi.packetmanager.PacketInfoManager;
import io.mosip.registration.processor.core.spi.queue.MosipQueueConnectionFactory;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.dao.PacketInfoDao;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;
import io.mosip.registration.processor.packet.storage.exception.IdRepoAppException;
import io.mosip.registration.processor.packet.storage.exception.IdentityNotFoundException;
import io.mosip.registration.processor.packet.storage.exception.ParsingException;
import io.mosip.registration.processor.packet.storage.exception.QueueConnectionNotFound;
import io.mosip.registration.processor.packet.storage.exception.VidCreationException;
import io.mosip.registration.processor.packet.utility.service.PacketReaderService;
import io.mosip.registration.processor.packet.utility.utils.IdSchemaUtils;
import io.mosip.registration.processor.status.dao.RegistrationStatusDao;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.entity.RegistrationStatusEntity;
import lombok.Data;

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

	@Autowired
	private ObjectMapper objMapper;

	
	/** The rest client service. */
	@Autowired
	private RegistrationProcessorRestClientService<Object> restClientService;

	/** The mosip connection factory. */
	@Autowired
	private MosipQueueConnectionFactory<MosipQueue> mosipConnectionFactory;

	/** The config server file storage URL. */
	@Value("${config.server.file.storage.uri}")
	private String configServerFileStorageURL;

	/** The get reg processor identity json. */
	@Value("${registration.processor.identityjson}")
	private String getRegProcessorIdentityJson;

	/** The get reg processor demographic identity. */
	@Value("${registration.processor.demographic.identity}")
	private String getRegProcessorDemographicIdentity;

	/** The get reg processor document category. */
	@Value("${registration.processor.document.category}")
	private String getRegProcessorDocumentCategory;

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
	
	/** The registration processor abis json. */
	@Value("${registration.processor.print.textfile}")
	private String registrationProcessorPrintTextFile;

	/** The id repo update. */
	@Value("${registration.processor.id.repo.update}")
	private String idRepoUpdate;

	/** The vid version. */
	@Value("${registration.processor.id.repo.vidVersion}")
	private String vidVersion;

	@Value("${registration.processor.default.source}")
	private String defaultSource;
	/** The packet info dao. */
	@Autowired
	private PacketInfoDao packetInfoDao;

	/** The registration status dao. */
	@Autowired
	private RegistrationStatusDao registrationStatusDao;

	/** The packet info manager. */
	@Autowired
	private PacketInfoManager<Identity, ApplicantInfoDto> packetInfoManager;

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

	/** The Constant FAIL_OVER. */
	private static final String FAIL_OVER = "failover:(";

	/** The Constant RANDOMIZE_FALSE. */
	private static final String RANDOMIZE_FALSE = ")?randomize=false";
	
	private static final String VALUE = "value";

	
	@Autowired
	private PacketReaderService packetReaderService;
	
	@Autowired
	private IdSchemaUtils idSchemaUtils;
	/**
	 * Gets the json.
	 *
	 * @param configServerFileStorageURL
	 *            the config server file storage URL
	 * @param uri
	 *            the uri
	 * @return the json
	 */
	public static String getJson(String configServerFileStorageURL, String uri) {
		RestTemplate restTemplate = new RestTemplate();
		return restTemplate.getForObject(configServerFileStorageURL + uri, String.class);
	}

	/**
	 * get applicant age by registration id. Checks the id json if dob or age
	 * present, if yes returns age if both dob or age are not present then retrieves
	 * age from id repo
	 *
	 * @param registrationId
	 *            the registration id
	 * @return the applicant age
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws PacketDecryptionFailureException
	 *             the packet decryption failure exception
	 * @throws RegistrationProcessorCheckedException 
	 * @throws io.mosip.registration.processor.packet.utility.exception.PacketDecryptionFailureException 
	 */
	public int getApplicantAge(String registrationId) throws IOException, ApisResourceAccessException,
			PacketDecryptionFailureException, io.mosip.kernel.core.exception.IOException, RegistrationProcessorCheckedException, ApiNotAccessibleException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				registrationId, "Utilities::getApplicantAge()::entry");

		JSONObject regProcessorIdentityJson = getRegistrationProcessorIdentityJson();
		String ageKey = JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.AGE), VALUE);
		String dobKey = JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.DOB), VALUE);

		
		String applicantDob = JsonUtil.getJSONValue(getDemographicIdentityJSONObject(registrationId,dobKey), dobKey);
	    Integer applicantAge = JsonUtil.getJSONValue(getDemographicIdentityJSONObject(registrationId,ageKey), ageKey);
		if (applicantDob != null) {
			return calculateAge(applicantDob);
		} else if (applicantAge != null) {
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
					registrationId, "Utilities::getApplicantAge()::exit when applicantAge is not null");
			return applicantAge;

		} else {
			
			Long uin = getUIn(registrationId);
			JSONObject identityJSONOject = retrieveIdrepoJson(uin);
			String idRepoApplicantDob = JsonUtil.getJSONValue(identityJSONOject, dobKey);
			if (idRepoApplicantDob != null) {
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
						registrationId, "Utilities::getApplicantAge()::exit when ID REPO applicantDob is not null");
				return calculateAge(idRepoApplicantDob);
			}
			Integer idRepoApplicantAge = JsonUtil.getJSONValue(identityJSONOject, ageKey);
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
					registrationId, "Utilities::getApplicantAge()::exit when ID REPO applicantAge is not null");
			return idRepoApplicantAge != null ? idRepoApplicantAge : 0;

		}

	}

	/**
	 * retrieving identity json ffrom id repo by UIN.
	 *
	 * @param uin
	 *            the uin
	 * @return the JSON object
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws IdRepoAppException
	 *             the id repo app exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public JSONObject retrieveIdrepoJson(Long uin) throws ApisResourceAccessException, IdRepoAppException, IOException {

		if (uin != null) {
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.UIN.toString(), "",
					"Utilities::retrieveIdrepoJson()::entry");
			List<String> pathSegments = new ArrayList<>();
			pathSegments.add(String.valueOf(uin));
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
			String response = objMapper.writeValueAsString(idResponseDto.getResponse().getIdentity());
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.UIN.toString(), "",
					"Utilities::retrieveIdrepoJson():: IDREPOGETIDBYUIN GET service call ended Successfully");
			try {
				return (JSONObject) new JSONParser().parse(response);
			} catch (org.json.simple.parser.ParseException e) {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.UIN.toString(), "",
						ExceptionUtils.getStackTrace(e));
				throw new IdRepoAppException("Error while parsing string to JSONObject",e);
			}


		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.UIN.toString(), "",
				"Utilities::retrieveIdrepoJson()::exit UIN is null");
		return null;
	}

	/**
	 * Returns all the list of queue details(inbound/outbound address,name,url,pwd)
	 * from abisJson Also validates the abis json fileds(null or not).
	 *
	 * @return the abis queue details
	 * @throws RegistrationProcessorCheckedException
	 *             the registration processor checked exception
	 */
	public List<AbisQueueDetails> getAbisQueueDetails() throws RegistrationProcessorCheckedException {
		List<AbisQueueDetails> abisQueueDetailsList = new ArrayList<>();
		String registrationProcessorAbis = Utilities.getJson(configServerFileStorageURL, registrationProcessorAbisJson);
		JSONObject regProcessorAbisJson;
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"Utilities::getAbisQueueDetails()::entry");

		try {
			regProcessorAbisJson = JsonUtil.objectMapperReadValue(registrationProcessorAbis, JSONObject.class);

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
				MosipQueue mosipQueue = mosipConnectionFactory.createConnection(typeOfQueue, userName, password,
						failOverBrokerUrl);
				if (mosipQueue == null)
					throw new QueueConnectionNotFound(
							PlatformErrorMessages.RPR_PIS_ABIS_QUEUE_CONNECTION_NULL.getMessage());

				abisQueueDetails.setMosipQueue(mosipQueue);
				abisQueueDetails.setInboundQueueName(inboundQueueName);
				abisQueueDetails.setOutboundQueueName(outboundQueueName);
				abisQueueDetails.setName(queueName);
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
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public JSONObject getRegistrationProcessorIdentityJson() throws IOException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"Utilities::getRegistrationProcessorIdentityJson()::entry");

		String getIdentityJsonString = Utilities.getJson(configServerFileStorageURL, getRegProcessorIdentityJson);
		ObjectMapper mapIdentityJsonStringToObject = new ObjectMapper();
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"Utilities::getRegistrationProcessorIdentityJson()::exit");
		return JsonUtil.getJSONObject(mapIdentityJsonStringToObject.readValue(getIdentityJsonString, JSONObject.class), MappingJsonConstants.IDENTITY);

	}

	/**
	 * Retrieves the identity json from HDFS by registrationId.
	 *
	 * @param registrationId
	 *            the registration id
	 * @return the demographic identity JSON object
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws PacketDecryptionFailureException
	 *             the packet decryption failure exception
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws ApiNotAccessibleException
	 */
	public JSONObject getDemographicIdentityJSONObject(String registrationId) throws IOException,
			PacketDecryptionFailureException, ApisResourceAccessException, io.mosip.kernel.core.exception.IOException, ApiNotAccessibleException {
		
	
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "Utilities::getDemographicIdentityJSONObject()::entry");
		
		

		InputStream idJsonStream = packetReaderService.getFile(registrationId, PacketFiles.ID.name(),defaultSource);
		byte[] bytearray = IOUtils.toByteArray(idJsonStream);
		String jsonString = new String(bytearray);
		JSONObject demographicIdentityJson = (JSONObject) JsonUtil.objectMapperReadValue(jsonString, JSONObject.class);
		JSONObject demographicIdentity = JsonUtil.getJSONObject(demographicIdentityJson,
				getRegProcessorDemographicIdentity);

		if (demographicIdentity == null) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, "Utilities::getDemographicIdentityJSONObject():: error with error message "
							+ PlatformErrorMessages.RPR_PIS_IDENTITY_NOT_FOUND.getMessage());
			throw new IdentityNotFoundException(PlatformErrorMessages.RPR_PIS_IDENTITY_NOT_FOUND.getMessage());
		}

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "Utilities::getDemographicIdentityJSONObject()::exit");

		
		return demographicIdentity;

	}

	/**
	 * Get UIN from identity json (used only for update/res update/activate/de
	 * activate packets).
	 *
	 * @param registrationId
	 *            the registration id
	 * @return the u in
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws PacketDecryptionFailureException
	 *             the packet decryption failure exception
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws RegistrationProcessorCheckedException 
	 * @throws io.mosip.registration.processor.packet.utility.exception.PacketDecryptionFailureException 
	 */
	public Long getUIn(String registrationId) throws IOException, ApiNotAccessibleException,
			io.mosip.kernel.core.exception.IOException, PacketDecryptionFailureException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "Utilities::getUIn()::entry");
		Number number;
		JSONObject demographicIdentity = getDemographicIdentityJSONObject(registrationId,UIN);

		number = JsonUtil.getJSONValue(demographicIdentity, UIN);

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "Utilities::getUIn()::exit");

		return number != null ? number.longValue() : null;

	}

	/**
	 * Gets the elapse status.
	 *
	 * @param registrationStatusDto
	 *            the registration status dto
	 * @param transactionType
	 *            the transaction type
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
	 * @param registrationId
	 *            the registration id
	 * @return the latest transaction id
	 */
	public String getLatestTransactionId(String registrationId) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "Utilities::getLatestTransactionId()::entry");
		RegistrationStatusEntity entity = registrationStatusDao.findById(registrationId);
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "Utilities::getLatestTransactionId()::exit");
		return entity != null ? entity.getLatestRegistrationTransactionId() : null;

	}

	/**
	 * retrieve UIN from IDRepo by registration id.
	 *
	 * @param regId
	 *            the reg id
	 * @return the JSON object
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws IdRepoAppException
	 *             the id repo app exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public JSONObject retrieveUIN(String regId) throws ApisResourceAccessException, IdRepoAppException, IOException {
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
				throw new IdRepoAppException("Error while parsing string to JSONObject",e);
			}

		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"Utilities::retrieveUIN()::exit regId is null");

		return null;
	}

	/**
	 * Gets the packet meta info.
	 *
	 * @param registrationId
	 *            the registration id
	 * @return the packet meta info
	 * @throws PacketDecryptionFailureException
	 *             the packet decryption failure exception
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws io.mosip.registration.processor.packet.utility.exception.PacketDecryptionFailureException 
	 */
	public PacketMetaInfo getPacketMetaInfo(String registrationId) throws ApiNotAccessibleException, 
			ApisResourceAccessException, io.mosip.kernel.core.exception.IOException, IOException, PacketDecryptionFailureException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "Utilities::getPacketMetaInfo():: entry");
		InputStream packetMetaInfoStream = packetReaderService.getFile(registrationId, PacketFiles.PACKET_META_INFO.name(),defaultSource);
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "Utilities::getPacketMetaInfo():: exit");
		return (PacketMetaInfo) JsonUtil.inputStreamtoJavaObject(packetMetaInfoStream, PacketMetaInfo.class);
	}

	/**
	 * Gets the all documents by reg id.
	 *
	 * @param regId
	 *            the reg id
	 * @return the all documents by reg id
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws PacketDecryptionFailureException
	 *             the packet decryption failure exception
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws RegistrationProcessorCheckedException 
	 * @throws io.mosip.registration.processor.packet.utility.exception.PacketDecryptionFailureException 
	 */
	public List<Documents> getAllDocumentsByRegId(String regId) throws IOException, PacketDecryptionFailureException,
			ApisResourceAccessException, io.mosip.kernel.core.exception.IOException, ApiNotAccessibleException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				regId, "Utilities::getAllDocumentsByRegId():: entry");

		List<Documents> applicantDocuments = new ArrayList<>();
		
		JSONObject regProcessorIdentityJson = getRegistrationProcessorIdentityJson();
		String proofOfAddressLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.POA), VALUE);
		String proofOfDateOfBirthLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.POB), VALUE);
		String proofOfIdentityLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.POI), VALUE);
		String proofOfRelationshipLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.POR), VALUE);
		String applicantBiometricLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.INDIVIDUAL_BIOMETRICS), VALUE);

		JSONObject proofOfAddress = JsonUtil.getJSONObject(getDemographicIdentityJSONObject(regId,proofOfAddressLabel), proofOfAddressLabel);
		JSONObject proofOfDateOfBirth = JsonUtil.getJSONObject(getDemographicIdentityJSONObject(regId,proofOfDateOfBirthLabel), proofOfDateOfBirthLabel);
		JSONObject proofOfIdentity = JsonUtil.getJSONObject(getDemographicIdentityJSONObject(regId,proofOfIdentityLabel), proofOfIdentityLabel);
		JSONObject proofOfRelationship = JsonUtil.getJSONObject(getDemographicIdentityJSONObject(regId,proofOfRelationshipLabel), proofOfRelationshipLabel);
		JSONObject applicantBiometric = JsonUtil.getJSONObject(getDemographicIdentityJSONObject(regId,applicantBiometricLabel), applicantBiometricLabel);
		if (proofOfAddress != null) {
			applicantDocuments
					.add(getIdDocumnet(regId, PacketFiles.DEMOGRAPHIC.name(), proofOfAddress, proofOfAddressLabel));
		}
		if (proofOfDateOfBirth != null) {
			applicantDocuments.add(
					getIdDocumnet(regId, PacketFiles.DEMOGRAPHIC.name(), proofOfDateOfBirth, proofOfDateOfBirthLabel));
		}
		if (proofOfIdentity != null) {
			applicantDocuments
					.add(getIdDocumnet(regId, PacketFiles.DEMOGRAPHIC.name(), proofOfIdentity, proofOfIdentityLabel));
		}
		if (proofOfRelationship != null) {
			applicantDocuments.add(getIdDocumnet(regId, PacketFiles.DEMOGRAPHIC.name(), proofOfRelationship,
					proofOfRelationshipLabel));
		}
		if (applicantBiometric != null) {
			applicantDocuments.add(
					getIdDocumnet(regId, PacketFiles.BIOMETRIC.name(), applicantBiometric, applicantBiometricLabel));
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				regId, "Utilities::getAllDocumentsByRegId():: exit");

		return applicantDocuments;
	}

	/**
	 * Gets the id documnet.
	 *
	 * @param registrationId
	 *            the registration id
	 * @param folderPath
	 *            the folder path
	 * @param idDocObj
	 *            the id doc obj
	 * @param idDocLabel
	 *            the id doc label
	 * @return the id documnet
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws PacketDecryptionFailureException
	 *             the packet decryption failure exception
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws RegistrationProcessorCheckedException 
	 * @throws io.mosip.registration.processor.packet.utility.exception.PacketDecryptionFailureException 
	 */
	private Documents getIdDocumnet(String registrationId, String folderPath, JSONObject idDocObj, String idDocLabel)
			throws IOException, PacketDecryptionFailureException, ApiNotAccessibleException,
			io.mosip.kernel.core.exception.IOException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "Utilities::getIdDocumnet():: entry");
     
		Documents documentsInfoDto = new Documents();
		String source=idSchemaUtils.getSource(idDocLabel);
		InputStream poiStream = packetReaderService.getFile(registrationId,idDocObj.get("value").toString(),source);
		documentsInfoDto.setValue(CryptoUtil.encodeBase64(IOUtils.toByteArray(poiStream)));
		documentsInfoDto.setCategory(idDocLabel);
		
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "Utilities::getIdDocumnet():: exit");

		return null;
	}

	/**
	 * Calculate age.
	 *
	 * @param applicantDob
	 *            the applicant dob
	 * @return the int
	 */
	private int calculateAge(String applicantDob) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"Utilities::calculateAge():: entry");

		DateFormat sdf = new SimpleDateFormat(dobFormat);
		Date birthDate = null;
		try {
			birthDate = sdf.parse(applicantDob);

		} catch (ParseException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", "Utilities::calculateAge():: error with error message "
							+ PlatformErrorMessages.RPR_SYS_PARSING_DATE_EXCEPTION.getMessage());
			throw new ParsingException(PlatformErrorMessages.RPR_SYS_PARSING_DATE_EXCEPTION.getCode(), e);
		}
		LocalDate ld = new java.sql.Date(birthDate.getTime()).toLocalDate();
		Period p = Period.between(ld, LocalDate.now());
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"Utilities::calculateAge():: exit");

		return p.getYears();

	}

	/**
	 * Validate abis queue json and return value.
	 *
	 * @param jsonObject
	 *            the json object
	 * @param key
	 *            the key
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
	 * Gets the uin by vid.
	 *
	 * @param vid
	 *            the vid
	 * @return the uin by vid
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws VidCreationException
	 *             the vid creation exception
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
	 * Link reg id wrt uin.
	 *
	 * @param registrationID
	 *            the registration ID
	 * @param uin
	 *            the uin
	 * @return true, if successful
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 */
	@SuppressWarnings("unchecked")
	public boolean linkRegIdWrtUin(String registrationID, String uin) throws ApisResourceAccessException {

		IdResponseDTO idResponse = null;
		RequestDto requestDto = new RequestDto();
		if (uin != null) {

			JSONObject identityObject = new JSONObject();
			identityObject.put(UIN, Long.parseLong(uin));

			requestDto.setRegistrationId(registrationID);
			requestDto.setIdentity(identityObject);

			IdRequestDto idRequestDTO = new IdRequestDto();
			idRequestDTO.setId(idRepoUpdate);
			idRequestDTO.setRequest(requestDto);
			idRequestDTO.setMetadata(null);
			idRequestDTO.setRequesttime(DateUtils.getUTCCurrentDateTimeString());
			idRequestDTO.setVersion(vidVersion);

			idResponse = (IdResponseDTO) restClientService.patchApi(ApiName.IDREPOSITORY, null, "", "", idRequestDTO,
					IdResponseDTO.class);

			if (idResponse != null && idResponse.getResponse() != null) {

				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationID, " UIN Linked with the RegID");

				return true;
			} else {

				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationID,
						" UIN not Linked with the RegID ");
				return false;
			}

		} else {

			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationID, " UIN is null ");
		}

		return false;
	}

	/**
	 * Retrieve idrepo json status.
	 *
	 * @param uin
	 *            the uin
	 * @return the string
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws IdRepoAppException
	 *             the id repo app exception
	 */
	public String retrieveIdrepoJsonStatus(Long uin) throws ApisResourceAccessException, IdRepoAppException {
		String response = null;
		if (uin != null) {
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.UIN.toString(), "",
					"Utilities::retrieveIdrepoJson()::entry");
			List<String> pathSegments = new ArrayList<>();
			pathSegments.add(String.valueOf(uin));
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
	/**
	 * Retrieves the identity json from HDFS by registrationId.
	 *
	 * @param registrationId
	 *            the registration id
	 * @return the demographic identity JSON object
	 * @throws RegistrationProcessorCheckedException 
	 * @throws io.mosip.kernel.core.exception.IOException 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws PacketDecryptionFailureException
	 *             the packet decryption failure exception
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws io.mosip.registration.processor.packet.utility.exception.PacketDecryptionFailureException 
	 */
	public JSONObject getDemographicIdentityJSONObject(String registrationId,String fieldLabel) throws io.mosip.kernel.core.exception.IOException, IOException, ApiNotAccessibleException, PacketDecryptionFailureException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "Utilities::getDemographicIdentityJSONObject()::entry");
		String source = idSchemaUtils.getSource(fieldLabel);
		JSONObject demographicIdentity=null;
		if(source!=null) {
		InputStream idJsonStream =packetReaderService.getFile(registrationId, PacketFiles.ID.name(), source);
		
		byte[] bytearray = IOUtils.toByteArray(idJsonStream);
		String jsonString = new String(bytearray);
		JSONObject demographicIdentityJson = (JSONObject) JsonUtil.objectMapperReadValue(jsonString, JSONObject.class);
		demographicIdentity = JsonUtil.getJSONObject(demographicIdentityJson,
				getRegProcessorDemographicIdentity);

		if (demographicIdentity == null) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, "Utilities::getDemographicIdentityJSONObject():: error with error message "
							+ PlatformErrorMessages.RPR_PIS_IDENTITY_NOT_FOUND.getMessage());
			throw new IdentityNotFoundException(PlatformErrorMessages.RPR_PIS_IDENTITY_NOT_FOUND.getMessage());
		}

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "Utilities::getDemographicIdentityJSONObject()::exit");
		}
		return demographicIdentity;

	}

}
