package io.mosip.registration.processor.manual.verification;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.jms.Message;
import javax.jms.TextMessage;

import org.apache.activemq.command.ActiveMQBytesMessage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.Gson;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorUnCheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.queue.factory.MosipQueue;
import io.mosip.registration.processor.core.queue.factory.QueueListener;
import io.mosip.registration.processor.core.spi.queue.MosipQueueConnectionFactory;
import io.mosip.registration.processor.core.spi.queue.MosipQueueManager;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.manual.verification.constants.ManualVerificationConstants;
import io.mosip.registration.processor.manual.verification.dto.QueueDetails;
import io.mosip.registration.processor.manual.verification.request.dto.ManualAdjudicationRequestDTO;
import io.mosip.registration.processor.manual.verification.response.dto.ManualAdjudicationResponseDTO;
import io.mosip.registration.processor.manual.verification.service.ManualVerificationService;
import io.mosip.registration.processor.packet.storage.exception.QueueConnectionNotFound;

@Component
public class Listener {


	private static final String TEXT_MESSAGE = "text";

	@Value("${activemq.message.format}")
	private String messageFormat;
	
	@Value("${config.server.file.storage.uri}")
	private String configServerFileStorageURL;

	@Value("${registration.processor.manual.adjudication.json}")
	private String registrationProcessorJson;

	/** The abis service. */
	@Autowired
	ManualVerificationService manualVerificationService;

	/** The mosip queue manager. */
	@Autowired
	private MosipQueueManager<MosipQueue, byte[]> mosipQueueManager;

	/** The Constant ID. */
	private static final String ID = "id";
	
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

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(Listener.class);

	/** The is connection. */
	boolean isConnection = false;

	List<String> outboundads=new ArrayList<>();
	List<MosipQueue> que=new ArrayList<>();
	
	/** The mosip connection factory. */
	@Autowired
	private MosipQueueConnectionFactory<MosipQueue> mosipConnectionFactory;

	/**
	 * Run queue.
	 *
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws RegistrationProcessorCheckedException
	 */
	public void runQueue() throws RegistrationProcessorCheckedException {
		List<QueueDetails> queueDetails = getQueueDetails();
		if (queueDetails != null && !queueDetails.isEmpty()) {

			for (int i = 0; i < queueDetails.size(); i++) {
				String outBoundAddress = queueDetails.get(i).getOutboundQueueName();
				MosipQueue queue = queueDetails.get(i).getMosipQueue();
				outboundads.add(outBoundAddress);
				que.add(queue);
				QueueListener listener = new QueueListener() {
					@Override
					public void setListener(Message message) {
						consumeLogic(message, outBoundAddress, queue);
					}
				};
				mosipQueueManager.consume(queueDetails.get(i).getMosipQueue(),
						queueDetails.get(i).getInboundQueueName(), listener);
			}

			isConnection = true;
		} else {
			throw new QueueConnectionNotFound(PlatformErrorMessages.RPR_PRT_QUEUE_CONNECTION_NULL.getMessage());
		}

	}

	/**
	 * Consume logic.
	 *
	 * @param message
	 *            the message
	 * @param outboundAddress
	 *            the outboundAddress
	 * @param queue
	 *            the queue
	 * @return true, if successful
	 */
	@SuppressWarnings("unchecked")
	public boolean consumeLogic(Message message, String outboundAddress, MosipQueue queue) {

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"AbisMessageQueueImpl::consumeLogic()::Entry()");
		boolean isrequestAddedtoQueue = false;
		String response = null;
		String request = null;
		try {
			if (messageFormat.equalsIgnoreCase(TEXT_MESSAGE)) {
				TextMessage textMessage = (TextMessage) message;
				request =textMessage.getText();
			} else
				request = new String(((ActiveMQBytesMessage) message).getContent().data);

			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
					"---request received ---" + request);
			JSONObject object = JsonUtil.objectMapperReadValue(request, JSONObject.class);
			Map map = new Gson().fromJson(request, Map.class);
			final ObjectMapper mapper = new ObjectMapper();
			mapper.findAndRegisterModules();
			mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

			String id = (String) object.get(ID);
			if (id.matches(ManualVerificationConstants.MANUAL_ADJUDICATION_ID)) {
				ManualAdjudicationResponseDTO manualAdjudication= mapper.convertValue(map, ManualAdjudicationResponseDTO.class);
				manualVerificationService.saveToDB(manualAdjudication);
				
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), "",
						"---Inserted response obtained from queue to reg_manual_verification---" + manualAdjudication.toString());
				response = "Result:Successfully inserted record to DB";
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), "",
						"---Response to queue---" + response);
			}
			else
			{
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), "",
						"---Inavalid id---");
				response = "Result:'Invalid id'";
			}

		//	if (messageFormat.equalsIgnoreCase(TEXT_MESSAGE))
		//		isrequestAddedtoQueue = mosipQueueManager.send(queue, response, outboundAddress);
		//	else
				isrequestAddedtoQueue = mosipQueueManager.send(queue, response.getBytes("UTF-8"), outboundAddress);

		} catch (Exception e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					e.getMessage(), Arrays.toString(e.getStackTrace()));
		}

		return isrequestAddedtoQueue;
	}

	public void sendAdjudicationRequest(ManualAdjudicationRequestDTO req) {
		final ObjectMapper mapper = new ObjectMapper();
		mapper.findAndRegisterModules();
		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		for (int i = 0; i < que.size(); i++) {
			try {
				System.out.println("Response " + mapper.writeValueAsString(req));
				boolean b=mosipQueueManager.send(que.get(i),  mapper.writeValueAsString(req).getBytes("UTF-8"), outboundads.get(i));
				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"Sent to queue", "Sent successfully to "+que.get(i).getQueueName());
				System.out.println("Sent successfully to "+que.get(i).getQueueName());
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Returns all the list of queue details(inbound/outbound address,name,url,pwd)
	 * from Json Also validates the json fileds(null or not).
	 *
	 * @return the queue details
	 * @throws RegistrationProcessorCheckedException
	 *             the registration processor checked exception
	 */
	public List<QueueDetails> getQueueDetails() throws RegistrationProcessorCheckedException {
		List<QueueDetails> queueDetailsList = new ArrayList<>();
		RestTemplate restTemplate = new RestTemplate();
		String registrationProcessorAbis=restTemplate.getForObject(configServerFileStorageURL + registrationProcessorJson, String.class);
		///////////// read from local file
		/*String registrationProcessorAbis = "";
		try {
			BufferedReader br = new BufferedReader(new FileReader("C:\\Users\\M1053288\\Desktop\\doc.txt"));

			String st;
			while ((st = br.readLine()) != null) {
				registrationProcessorAbis = registrationProcessorAbis + st;

			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}*/
		//////////////////////
		JSONObject regProcessorAbisJson;
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"getQueueDetails()::entry");

		try {
			regProcessorAbisJson = JsonUtil.objectMapperReadValue(registrationProcessorAbis, JSONObject.class);

			JSONArray regProcessorAbisArray = JsonUtil.getJSONArray(regProcessorAbisJson, ABIS);

			for (Object jsonObject : regProcessorAbisArray) {
				QueueDetails queueDetails = new QueueDetails();
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

				queueDetails.setMosipQueue(mosipQueue);
				queueDetails.setInboundQueueName(inboundQueueName);
				queueDetails.setOutboundQueueName(outboundQueueName);
				queueDetails.setName(queueName);
				queueDetailsList.add(queueDetails);

			}
		} catch (IOException e) {
			throw new RegistrationProcessorCheckedException(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getCode(),
					PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage(), e);
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"getQueueDetails::exit");

		return queueDetailsList;

	}

	/**
	 * Validate queue json and return value.
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
					PlatformErrorMessages.QUEUE_JSON_VALIDATION_FAILED.getCode(),
					PlatformErrorMessages.QUEUE_JSON_VALIDATION_FAILED.getMessage() + "::" + key);
		}

		return value;
	}
	
	
}
