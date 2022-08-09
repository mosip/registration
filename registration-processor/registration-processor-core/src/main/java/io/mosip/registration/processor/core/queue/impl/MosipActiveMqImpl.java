package io.mosip.registration.processor.core.queue.impl;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.queue.factory.MosipActiveMq;
import io.mosip.registration.processor.core.queue.factory.MosipQueue;
import io.mosip.registration.processor.core.queue.factory.QueueListener;
import io.mosip.registration.processor.core.queue.factory.QueueListenerFactory;
import io.mosip.registration.processor.core.queue.impl.exception.ConnectionUnavailableException;
import io.mosip.registration.processor.core.queue.impl.exception.InvalidConnectionException;
import io.mosip.registration.processor.core.queue.impl.exception.QueueConnectionException;
import io.mosip.registration.processor.core.spi.queue.MosipQueueManager;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;


/**
 * This class is ActiveMQ implementation for Mosip Queue
 *
 * @author Mukul Puspam
 * @since 0.8.0
 */
public class MosipActiveMqImpl implements MosipQueueManager<MosipQueue, byte[]> {

    /**
     * The reg proc logger.
     */
    private static Logger regProcLogger = RegProcessorLogger.getLogger(MosipActiveMqImpl.class);

    private Connection connection;
    private Session session;
    private Destination destination;
    private static final String LINE_SEPERATOR = "----------------";
    @Value("${registration.processor.queue.connection.retry.count:10}")
    private int retryCount;

    /**
     * The method to set up session and destination
     *
     * @param mosipActiveMq The Mosip ActiveMq instance
     */
    private void setup(MosipActiveMq mosipActiveMq) {
        regProcLogger.debug(LINE_SEPERATOR, LINE_SEPERATOR, "In ActiveMq setUp ", LINE_SEPERATOR);
        try {
            ActiveMQConnection activemQConn = (ActiveMQConnection) connection;
            if (activemQConn == null || activemQConn.isClosed()) {
                regProcLogger.debug(LINE_SEPERATOR, LINE_SEPERATOR, "-----INITIAL CONNECTION-----",
                        LINE_SEPERATOR + this.connection);
                regProcLogger.debug(LINE_SEPERATOR, LINE_SEPERATOR, "-----INITIAL SESSION-----",
                        LINE_SEPERATOR + this.session);
                connection = mosipActiveMq.getActiveMQConnectionFactory().createConnection();
                activemQConn = (ActiveMQConnection) connection;
                activemQConn.addTransportListener(new TransportExceptionListener());
                if (session == null) {
                    connection.start();
                    this.session = this.connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    regProcLogger.debug(LINE_SEPERATOR, LINE_SEPERATOR, "-----NEW CONNECTION-----",
                            LINE_SEPERATOR + this.connection);
                    regProcLogger.debug(LINE_SEPERATOR, LINE_SEPERATOR, "-----NEW SESSION-----",
                            LINE_SEPERATOR + this.session);
                    if (!((ActiveMQConnection) connection).isStarted() || session == null) {
                        regProcLogger.error("Activemq connection is not created. Retrying.....");
                        throw new QueueConnectionException("session is "+session);
                       
                    }
                }
            }

        } catch (JMSException e) {
            regProcLogger.error(LINE_SEPERATOR, LINE_SEPERATOR, "-----EXCEPTION While starting connection -----",
                    LINE_SEPERATOR + ExceptionUtils.getFullStackTrace(e));
            throw new ConnectionUnavailableException(PlatformErrorMessages.RPR_MQI_CONNECTION_UNAVAILABLE.getMessage(),
                    e);
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * io.mosip.registration.processor.core.spi.queue.MosipQueueManager#send(java.
     * lang.Object, java.lang.Object, java.lang.String)
     */
    @Override
    public Boolean send(MosipQueue mosipQueue, byte[] message, String address) {
        return send(mosipQueue, message, address, 0);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * io.mosip.registration.processor.core.spi.queue.MosipQueueManager#send(java.
     * lang.Object, java.lang.Object, java.lang.String, long)
     */
    @Override
    public Boolean send(MosipQueue mosipQueue, byte[] message, String address, int messageTTL) {
        regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
                "", "MosipActiveMqImpl::send()::entry");

        boolean flag = false;
        initialSetup(mosipQueue);
        try {
            destination = session.createQueue(address);
            MessageProducer messageProducer = session.createProducer(destination);
            BytesMessage byteMessage = session.createBytesMessage();
            byteMessage.writeObject(message);
            if(messageTTL > 0)
                messageProducer.setTimeToLive(messageTTL * (long)1000);
            messageProducer.send(byteMessage);
            flag = true;
        } catch (JMSException e) {
            regProcLogger.error("*******SEND EXCEPTION *****", "*******SEND EXCEPTION *****",
                    "*******SEND EXCEPTION *****", ExceptionUtils.getFullStackTrace(e));
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    "", "MosipActiveMqImpl::send():: error with error message "
                            + PlatformErrorMessages.RPR_MQI_UNABLE_TO_SEND_TO_QUEUE.getMessage());
            throw new ConnectionUnavailableException(
                    PlatformErrorMessages.RPR_MQI_UNABLE_TO_SEND_TO_QUEUE.getMessage());
        }
        regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
                "", "MosipActiveMqImpl::send()::exit");

        return flag;
    }

    @Override
    public Boolean send(MosipQueue mosipQueue, String message, String address) {
        return send(mosipQueue, message, address, 0);
    }

    @Override
    public Boolean send(MosipQueue mosipQueue, String message, String address, int messageTTL) {
        boolean flag = false;
        initialSetup(mosipQueue);
        try {
            // fix for activemq connection issue
            if (session == null)
                initialSetup(mosipQueue);
            destination = session.createQueue(address);
            MessageProducer messageProducer = session.createProducer(destination);
            TextMessage textMessage = session.createTextMessage();
            textMessage.setText(message);
            if(messageTTL > 0)
                messageProducer.setTimeToLive(messageTTL * (long)1000);
            messageProducer.send(textMessage);
            flag = true;
        } catch (JMSException e) {
            regProcLogger.error("*******SEND EXCEPTION *****", "*******SEND EXCEPTION *****",
                    "*******SEND EXCEPTION *****", ExceptionUtils.getFullStackTrace(e));
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    "", "MosipActiveMqImpl::send():: error with error message "
                            + PlatformErrorMessages.RPR_MQI_UNABLE_TO_SEND_TO_QUEUE.getMessage());
            throw new ConnectionUnavailableException(
                    PlatformErrorMessages.RPR_MQI_UNABLE_TO_SEND_TO_QUEUE.getMessage());
        }
        regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
                "", "MosipActiveMqImpl::send()::exit");

        return flag;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * io.mosip.registration.processor.core.spi.queue.MosipQueueManager#consume(java
     * .lang.Object, java.lang.String)
     */
    @Override
    public byte[] consume(MosipQueue mosipQueue, String address, QueueListener object) {
        regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
                "", "MosipActiveMqImpl::consume()::entry");

        MosipActiveMq mosipActiveMq = (MosipActiveMq) mosipQueue;
        ActiveMQConnectionFactory activeMQConnectionFactory = mosipActiveMq.getActiveMQConnectionFactory();
        if (activeMQConnectionFactory == null) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    "", "MosipActiveMqImpl::consume():: error with error message "
                            + PlatformErrorMessages.RPR_MQI_INVALID_CONNECTION.getMessage());

            throw new InvalidConnectionException(PlatformErrorMessages.RPR_MQI_INVALID_CONNECTION.getMessage());
        }
        if (destination == null) {
            setup(mosipActiveMq);
        }
        MessageConsumer consumer;
        try {
            if (session == null) {
                regProcLogger.error("Session is null. System will retry to create session");
                setup(mosipActiveMq);
            }
            destination = session.createQueue(address);
            consumer = session.createConsumer(destination);
            consumer.setMessageListener(QueueListenerFactory.getListener(mosipQueue.getQueueName(), object));
        } catch (JMSException | NullPointerException e) {
            regProcLogger.error("*******CONSUME EXCEPTION *****", "*******CONSUME EXCEPTION *****",
                    "*******CONSUME EXCEPTION *****", ExceptionUtils.getFullStackTrace(e));
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    "", "MosipActiveMqImpl::consume():: error with error message "
                            + PlatformErrorMessages.RPR_MQI_UNABLE_TO_CONSUME_FROM_QUEUE.getMessage());

            if (e instanceof NullPointerException && retryCount > 0) {
                regProcLogger.warn(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                        "", "Could not obtain queue connection. System will retry for "+ retryCount +" more times.");
                retryCount = retryCount - 1;
                consume(mosipQueue, address, object);
            } else {
                throw new ConnectionUnavailableException(
                        PlatformErrorMessages.RPR_MQI_UNABLE_TO_CONSUME_FROM_QUEUE.getMessage());
            }
        }
        regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
                "", "MosipActiveMqImpl::consume()::exit");

        return null;
    }

    private void initialSetup(MosipQueue mosipQueue) {
        regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
                "", "MosipActiveMqImpl::send()::entry");

        MosipActiveMq mosipActiveMq = (MosipActiveMq) mosipQueue;
        ActiveMQConnectionFactory activeMQConnectionFactory = mosipActiveMq.getActiveMQConnectionFactory();
        if (activeMQConnectionFactory == null) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    "", "MosipActiveMqImpl::send():: error with error message "
                            + PlatformErrorMessages.RPR_MQI_INVALID_CONNECTION.getMessage());
            throw new InvalidConnectionException(PlatformErrorMessages.RPR_MQI_INVALID_CONNECTION.getMessage());
        }
        setup(mosipActiveMq);
    }

}
