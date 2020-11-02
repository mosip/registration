package io.mosip.registration.processor.manual.verification.stage;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.core.common.rest.dto.BaseRestResponseDTO;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.manual.verification.constants.ManualVerificationConstants;
import io.mosip.registration.processor.manual.verification.dto.ManualVerificationDTO;
import io.mosip.registration.processor.manual.verification.dto.ManualVerificationDecisionDto;
import io.mosip.registration.processor.manual.verification.exception.handler.ManualVerificationExceptionHandler;
import io.mosip.registration.processor.manual.verification.request.dto.ManualVerificationAssignmentRequestDTO;
import io.mosip.registration.processor.manual.verification.request.dto.ManualVerificationDecisionRequestDTO;
import io.mosip.registration.processor.manual.verification.response.builder.ManualVerificationResponseBuilder;
import io.mosip.registration.processor.manual.verification.response.dto.ManualVerificationAssignResponseDTO;
import io.mosip.registration.processor.manual.verification.service.ManualVerificationService;
import io.mosip.registration.processor.manual.verification.util.ManualVerificationRequestValidator;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * This class sends message to next stage after successful completion of manual
 * verification.
 *
 * @author Pranav Kumar
 * @since 0.0.1
 */
@Component
public class ManualVerificationStage extends MosipVerticleAPIManager {

	@Autowired
	private ManualVerificationService manualAdjudicationService;

	/** The mosip event bus. */
	private MosipEventBus mosipEventBus;

	/**
	 * vertx Cluster Manager Url
	 */
	@Value("${vertx.cluster.configuration}")
	private String clusterManagerUrl;

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(ManualVerificationStage.class);

	/** The env. */
	@Autowired
	private Environment env;

	@Autowired
	ManualVerificationRequestValidator manualVerificationRequestValidator;

	@Autowired
	ManualVerificationExceptionHandler manualVerificationExceptionHandler;

	@Autowired
	ManualVerificationResponseBuilder manualVerificationResponseBuilder;

	/** Mosip router for APIs */
	@Autowired
	MosipRouter router;

	/**
	 * server port number
	 */
	@Value("${server.port}")
	private String port;

	/** worker pool size. */
	@Value("${worker.pool.size}")
	private Integer workerPoolSize;

	@Value("${server.servlet.path}")
	private String contextPath;

	private static final String APPLICATION_JSON = "application/json";

	/**
	 * Deploy stage.
	 */
	public void deployStage() {
		this.mosipEventBus = this.getEventBus(this, clusterManagerUrl, workerPoolSize);
	}

	@Override
	public void start() {
		router.setRoute(this.postUrl(vertx, null, MessageBusAddress.MANUAL_VERIFICATION_BUS));
		this.routes(router);
		this.createServer(router.getRouter(), Integer.parseInt(port));
	}

	private void routes(MosipRouter router) {
		

		router.post(contextPath + "/assignment");
		router.handler(this::processAssignment, handlerObj -> {
			manualVerificationExceptionHandler
					.setId(env.getProperty(ManualVerificationConstants.ASSIGNMENT_SERVICE_ID));
			manualVerificationExceptionHandler.setResponseDtoType(new ManualVerificationAssignResponseDTO());
			this.setResponseWithDigitalSignature(handlerObj,
					manualVerificationExceptionHandler.handler(handlerObj.failure()), APPLICATION_JSON);

		});

		router.post(contextPath + "/decision");
		router.handler(this::processDecision, handlerObj -> {
			manualVerificationExceptionHandler.setId(env.getProperty(ManualVerificationConstants.DECISION_SERVICE_ID));
			manualVerificationExceptionHandler.setResponseDtoType(new ManualVerificationAssignResponseDTO());
			this.setResponseWithDigitalSignature(handlerObj,
					manualVerificationExceptionHandler.handler(handlerObj.failure()), APPLICATION_JSON);

		});


	}

	

	public void processAssignment(RoutingContext ctx) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"ManualVerificationStage::processAssignment::entry");
		JsonObject obj = ctx.getBodyAsJson();
		ManualVerificationAssignmentRequestDTO pojo = Json.mapper.convertValue(obj.getMap(),
				ManualVerificationAssignmentRequestDTO.class);
		manualVerificationRequestValidator.validate(obj,
				env.getProperty(ManualVerificationConstants.ASSIGNMENT_SERVICE_ID));
		ManualVerificationDTO manualVerificationDTO = manualAdjudicationService.assignApplicant(pojo.getRequest());
		if (manualVerificationDTO != null) {
			BaseRestResponseDTO responseData = ManualVerificationResponseBuilder.buildManualVerificationSuccessResponse(
					manualVerificationDTO, env.getProperty(ManualVerificationConstants.ASSIGNMENT_SERVICE_ID),
					env.getProperty(ManualVerificationConstants.MVS_APPLICATION_VERSION),
					env.getProperty(ManualVerificationConstants.DATETIME_PATTERN));
			this.setResponseWithDigitalSignature(ctx, responseData, APPLICATION_JSON);
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					manualVerificationDTO.getMvUsrId(), "ManualVerificationStage::processAssignment::success");

		}

	}

	public void processDecision(RoutingContext ctx) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"ManualVerificationStage::processDecision::entry");
		JsonObject obj = ctx.getBodyAsJson();
		ManualVerificationDecisionRequestDTO pojo = Json.mapper.convertValue(obj.getMap(),
				ManualVerificationDecisionRequestDTO.class);
		manualVerificationRequestValidator.validate(obj,
				env.getProperty(ManualVerificationConstants.DECISION_SERVICE_ID));
		ManualVerificationDecisionDto updatedManualVerificationDTO = manualAdjudicationService
				.updatePacketStatus(pojo.getRequest(), this.getClass().getSimpleName());
		if (updatedManualVerificationDTO != null) {
			BaseRestResponseDTO responseData = ManualVerificationResponseBuilder.buildManualVerificationSuccessResponse(
					updatedManualVerificationDTO, env.getProperty(ManualVerificationConstants.DECISION_SERVICE_ID),
					env.getProperty(ManualVerificationConstants.MVS_APPLICATION_VERSION),
					env.getProperty(ManualVerificationConstants.DATETIME_PATTERN));
			this.setResponseWithDigitalSignature(ctx, responseData, APPLICATION_JSON);
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", "ManualVerificationStage::processDecision::success");
		}

	}

	

	public void sendMessage(MessageDTO messageDTO) {
		this.send(this.mosipEventBus, MessageBusAddress.MANUAL_VERIFICATION_BUS, messageDTO);
	}

	@Override
	public MessageDTO process(MessageDTO object) {
		return null;
	}
}
