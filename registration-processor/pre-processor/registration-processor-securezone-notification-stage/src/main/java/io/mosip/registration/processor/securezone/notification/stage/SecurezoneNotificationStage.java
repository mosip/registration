package io.mosip.registration.processor.securezone.notification.stage;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.code.RegistrationExceptionTypeCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionTypeCode;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.RegistrationType;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.SubWorkflowDto;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.status.util.TrimExceptionMessage;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.status.service.SubWorkflowMappingService;
import io.mosip.registration.processor.status.service.SyncRegistrationService;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@Configuration
@ComponentScan(basePackages = {
		"io.mosip.registration.processor.core.config",
        "io.mosip.registration.processor.securezone.notification.config",
        "io.mosip.registration.processor.packet.manager.config",
        "io.mosip.registration.processor.status.config", 
        "io.mosip.registration.processor.rest.client.config",
        "io.mosip.registration.processor.core.kernel.beans"
})
public class SecurezoneNotificationStage extends MosipVerticleAPIManager {
	
	private static final String STAGE_PROPERTY_PREFIX = "mosip.regproc.securezone.notification.";

    /**
     * The reg proc logger.
     */
    private static Logger regProcLogger = RegProcessorLogger.getLogger(SecurezoneNotificationStage.class);

    /**
     * The cluster url.
     */
    @Value("${vertx.cluster.configuration}")
    private String clusterManagerUrl;

    /**
     * worker pool size.
     */
    @Value("${worker.pool.size}")
    private Integer workerPoolSize;

    /**
     * The mosip event bus.
     */
    private MosipEventBus mosipEventBus;

    /** After this time intervel, message should be considered as expired (In seconds). */
    @Value("${mosip.regproc.securezone.notification.message.expiry-time-limit}")
    private Long messageExpiryTimeLimit;

    /** The Constant USER. */
    private static final String USER = "MOSIP_SYSTEM";

    /**
     * Mosip router for APIs
     */
    @Autowired
    private MosipRouter router;

    /** The registration status service. */
    @Autowired
    private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

    @Autowired
    private SyncRegistrationService<SyncResponseDto, SyncRegistrationDto> syncRegistrationService;
    
    @Autowired
    private SubWorkflowMappingService subWorkflowMappingService;
    
    /** The core audit request builder. */
    @Autowired
    private AuditLogRequestBuilder auditLogRequestBuilder;

    @Autowired
    private RegistrationExceptionMapperUtil registrationStatusMapperUtil;

    /**
     * Deploy verticle.
     */
    public void deployVerticle() {
        this.mosipEventBus = this.getEventBus(this, clusterManagerUrl, workerPoolSize);
        this.consumeAndSend(mosipEventBus, MessageBusAddress.SECUREZONE_NOTIFICATION_IN,
                MessageBusAddress.SECUREZONE_NOTIFICATION_OUT, messageExpiryTimeLimit);
    }
    
    @Override
    protected String getPropertyPrefix() {
    	return STAGE_PROPERTY_PREFIX;
    }

    @Override
    public void start() {
        router.setRoute(this.postUrl(vertx, MessageBusAddress.SECUREZONE_NOTIFICATION_IN, MessageBusAddress.SECUREZONE_NOTIFICATION_OUT));
        this.routes(router);
        this.createServer(router.getRouter(), getPort());
    }

    /**
     * contains all the routes in this stage
     *
     * @param router
     */
    private void routes(MosipRouter router) {
        router.post(getServletPath() + "/notification");
        router.handler(this::processURL, this::failure);
    }

    /**
     * method to process the context received.
     *
     * @param ctx the ctx
     */
    public void processURL(RoutingContext ctx) {
        regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
                "SecurezoneNotificationStage::processURL()::entry");

        InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();
        MessageDTO messageDTO = new MessageDTO();
        TrimExceptionMessage trimMessage = new TrimExceptionMessage();
        LogDescription description = new LogDescription();
        boolean isTransactionSuccessful = false;

        try {
            JsonObject obj = ctx.getBodyAsJson();

            messageDTO.setMessageBusAddress(MessageBusAddress.SECUREZONE_NOTIFICATION_IN);
            messageDTO.setInternalError(Boolean.FALSE);
            messageDTO.setRid(obj.getString("rid"));
            messageDTO.setReg_type(obj.getString("reg_type"));
            messageDTO.setIsValid(obj.getBoolean("isValid"));
            messageDTO.setSource(obj.getString("source"));
            messageDTO.setIteration(obj.getInteger("iteration"));

            if(validateWorkflow(messageDTO)) {
            registrationStatusDto = registrationStatusService.getRegistrationStatus(
                    messageDTO.getRid(), messageDTO.getReg_type(), messageDTO.getIteration());

            if (registrationStatusDto != null && messageDTO.getRid().equalsIgnoreCase(registrationStatusDto.getRegistrationId())) {
                registrationStatusDto
                        .setLatestTransactionTypeCode(RegistrationTransactionTypeCode.SECUREZONE_NOTIFICATION.toString());
                registrationStatusDto.setRegistrationStageName(getStageName());


                registrationStatusDto
                        .setLatestTransactionStatusCode(RegistrationTransactionStatusCode.SUCCESS.toString());
                messageDTO.setIsValid(Boolean.TRUE);
                registrationStatusDto.setStatusComment(StatusUtil.NOTIFICATION_RECEIVED_TO_SECUREZONE.getMessage());
                registrationStatusDto.setSubStatusCode(StatusUtil.NOTIFICATION_RECEIVED_TO_SECUREZONE.getCode());
                registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());

                isTransactionSuccessful = true;
                description.setMessage(
                        PlatformSuccessMessages.RPR_SEZ_SECUREZONE_NOTIFICATION.getMessage() + " -- " + messageDTO.getRid());
                description.setCode(PlatformSuccessMessages.RPR_SEZ_SECUREZONE_NOTIFICATION.getCode());

                regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
                        LoggerFileConstant.REGISTRATIONID.toString(), messageDTO.getRid(),
                        description.getCode() + description.getMessage());
            } else {
                isTransactionSuccessful = false;
                messageDTO.setIsValid(Boolean.FALSE);
                regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                        LoggerFileConstant.REGISTRATIONID.toString(), messageDTO.getRid(),
                        "Transaction failed. RID not found in registration table.");
            }
            if (messageDTO.getIsValid()) {
                sendMessage(messageDTO);
                this.setResponse(ctx,
                        "Packet with registrationId '" + messageDTO.getRid() + "' has been forwarded to next stage");

                regProcLogger.info(obj.getString("rid"),
                        "Packet with registrationId '" + messageDTO.getRid() + "' has been forwarded to next stage", null,
                        null);
            } else {
                this.setResponse(ctx,
                        "Packet with registrationId '" + obj.getString("rid") + "' has not been uploaded to file System");

                regProcLogger.info(obj.getString("rid"),
                        "Packet with registrationId '" + messageDTO.getRid() + "' has not been uploaded to file System",
                        null, null);
            }
            }
            else {
            	this.setResponse(ctx,
                        "Packet with registrationId '" + obj.getString("rid") + "' The  additional info  is invalid.");

                regProcLogger.info(obj.getString("rid"),
                        "Packet with registrationId '" + messageDTO.getRid() + "' The  additional info  is invalid.",
                        null, null);
            }

            
        } catch (TablenotAccessibleException e) {
            registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
            registrationStatusDto.setStatusComment(
                    trimMessage.trimExceptionMessage(StatusUtil.DB_NOT_ACCESSIBLE.getMessage() + e.getMessage()));
            registrationStatusDto.setSubStatusCode(StatusUtil.DB_NOT_ACCESSIBLE.getCode());
            registrationStatusDto.setLatestTransactionStatusCode(
                    registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.DATA_ACCESS_EXCEPTION));
            isTransactionSuccessful = false;
            description.setMessage(PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage());
            description.setCode(PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getCode());
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    description.getCode() + " -- " + messageDTO.getRid(),
                    PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage() + e.getMessage()
                            + ExceptionUtils.getStackTrace(e));
            messageDTO.setIsValid(Boolean.FALSE);
            messageDTO.setInternalError(Boolean.TRUE);
            messageDTO.setRid(registrationStatusDto.getRegistrationId());
            ctx.fail(e);
        } catch (Exception e) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(),
                    ctx.getBodyAsString(), ExceptionUtils.getStackTrace(e));
            messageDTO.setIsValid(Boolean.FALSE);
            isTransactionSuccessful = false;
            description.setCode(PlatformErrorMessages.RPR_SECUREZONE_FAILURE.getCode());
            description.setMessage(PlatformErrorMessages.RPR_SECUREZONE_FAILURE.getMessage());
            ctx.fail(e);
        } finally {
            if (messageDTO.getInternalError()) {
                registrationStatusDto.setUpdatedBy(USER);
                int retryCount = registrationStatusDto.getRetryCount() != null
                        ? registrationStatusDto.getRetryCount() + 1
                        : 1;
                registrationStatusDto.setRetryCount(retryCount);
            }
            /** Module-Id can be Both Success/Error code */
            String moduleId = isTransactionSuccessful
                    ? PlatformSuccessMessages.RPR_SEZ_SECUREZONE_NOTIFICATION.getCode()
                    : description.getCode();
            String moduleName = ModuleName.SECUREZONE_NOTIFICATION.toString();
            registrationStatusService.updateRegistrationStatus(registrationStatusDto, moduleId, moduleName);
            if (isTransactionSuccessful)
                description.setMessage(PlatformSuccessMessages.RPR_SEZ_SECUREZONE_NOTIFICATION.getMessage());
            String eventId = isTransactionSuccessful ? EventId.RPR_401.toString()
                    : EventId.RPR_405.toString();
            String eventName = isTransactionSuccessful ? EventName.GET.toString()
                    : EventName.EXCEPTION.toString();
            String eventType = isTransactionSuccessful ? EventType.BUSINESS.toString()
                    : EventType.SYSTEM.toString();

            auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
                    moduleId, moduleName, messageDTO.getRid());
        }
    }

    private boolean validateWorkflow(MessageDTO messageDTO) {
    	boolean isValid=false;
		
			List<SubWorkflowDto> swfDtos=subWorkflowMappingService.
					getSubWorkflowMappingByRegIdAndProcessAndIteration(messageDTO.getRid(), 
            				messageDTO.getReg_type(), messageDTO.getIteration());
			if(swfDtos!=null &&swfDtos.size()==1) {
				SyncRegistrationEntity syncResponse=syncRegistrationService.findByAdditionalInfoReqId(swfDtos.get(0).getAdditionalInfoReqId());
				if(syncResponse==null) {
					isValid= true;
				}
			}else {
            	messageDTO.setIsValid(Boolean.FALSE);
            	isValid=false;
            	if(swfDtos.size()==0) {
            		regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                     LoggerFileConstant.REGISTRATIONID.toString(), messageDTO.getRid(),
                     "Additional_info_id not present db");
            	}else {
            		regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                    LoggerFileConstant.REGISTRATIONID.toString(), messageDTO.getRid(),
                    "Additional_info_id not valid");
            	}
			}
			
		
		return isValid;
	}

	/**
     * This is for failure handler
     *
     * @param routingContext
     */
    private void failure(RoutingContext routingContext) {
        this.setResponse(routingContext, routingContext.failure().getMessage());
    }

    /**
     * sends messageDTO to camel bridge.
     *
     * @param messageDTO the message DTO
     */
    public void sendMessage(MessageDTO messageDTO) {
        this.send(this.mosipEventBus, MessageBusAddress.SECUREZONE_NOTIFICATION_OUT, messageDTO);
    }

    @Override
    public MessageDTO process(MessageDTO object) {
        return null;
    }
}
