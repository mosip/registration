package io.mosip.registration.processor.securezone.notification.stage;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SecurezoneNotificationStage extends MosipVerticleAPIManager {

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
     * server port number.
     */
    @Value("${server.port}")
    private String port;

    /**
     * worker pool size.
     */
    @Value("${worker.pool.size}")
    private Integer workerPoolSize;

    /**
     * The mosip event bus.
     */
    private MosipEventBus mosipEventBus;

    /**
     * The context path.
     */
    @Value("${server.servlet.path}")
    private String contextPath;

    /**
     * Mosip router for APIs
     */
    @Autowired
    MosipRouter router;

    /**
     * Deploy verticle.
     */
    public void deployVerticle() {
        this.mosipEventBus = this.getEventBus(this, clusterManagerUrl, workerPoolSize);
    }

    @Override
    public void start() {
        router.setRoute(this.postUrl(vertx, MessageBusAddress.SECUREZONE_NOTIFICATION_IN, MessageBusAddress.SECUREZONE_NOTIFICATION_OUT));
        this.routes(router);
        this.createServer(router.getRouter(), Integer.parseInt(port));
    }

    /**
     * contains all the routes in this stage
     *
     * @param router
     */
    private void routes(MosipRouter router) {
        router.post(contextPath + "/notification");
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

        try {
            JsonObject obj = ctx.getBodyAsJson();

            MessageDTO messageDTO = new MessageDTO();
            messageDTO.setMessageBusAddress(MessageBusAddress.SECUREZONE_NOTIFICATION_IN);
            messageDTO.setInternalError(Boolean.FALSE);
            messageDTO.setRid(obj.getString("rid"));
            messageDTO.setIsValid(obj.getBoolean("isValid"));
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
        } catch (Exception e) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(),
                    ctx.getBodyAsString(), e.getStackTrace().toString());
            ctx.fail(e);
        }
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
        this.send(this.mosipEventBus, MessageBusAddress.PACKET_UPLOADER_OUT, messageDTO);
    }

    @Override
    public MessageDTO process(MessageDTO object) {
        return null;
    }
}
