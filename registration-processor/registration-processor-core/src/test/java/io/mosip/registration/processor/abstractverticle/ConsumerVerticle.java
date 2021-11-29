package io.mosip.registration.processor.abstractverticle;

import java.net.URL;
import java.util.ArrayList;

import brave.Tracing;
import io.mosip.registration.processor.core.tracing.EventTracingHandler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import org.assertj.core.util.Objects;
import org.junit.Assert;
import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;

import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleManager;
import io.mosip.registration.processor.core.constant.RegistrationType;
import io.mosip.registration.processor.core.eventbus.MosipEventBusFactory;
import io.mosip.registration.processor.core.exception.UnsupportedEventBusTypeException;

public class ConsumerVerticle extends MosipVerticleManager {
	private MessageDTO messageDTO;
	public MosipEventBus mosipEventBus;
	private MosipEventBusFactory mosipEventBusFactory;

	public void start() throws UnsupportedEventBusTypeException {
		System.setProperty("org.vertx.logger-delegate-factory-class-name", SLF4JLogDelegateFactory.class.getName());

		mosipEventBusFactory = new MosipEventBusFactory();
		mosipEventBusFactory.setTracing(Tracing.newBuilder().build());
		this.mosipEventBus = mosipEventBusFactory.getEventBus(vertx, "vertx");
		this.messageDTO = new MessageDTO();
		this.messageDTO.setRid("1001");
		this.messageDTO.setRetryCount(0);
		this.messageDTO.setMessageBusAddress(MessageBusAddress.PACKET_VALIDATOR_BUS_IN);
		this.messageDTO.setIsValid(true);
		this.messageDTO.setInternalError(false);
		this.messageDTO.setReg_type(RegistrationType.NEW);
		this.busOutHaltAddresses = new ArrayList<String>();

		//this.consume(mosipEventBus, MessageBusAddress.PACKET_VALIDATOR_BUS_IN);
		//this.consumeAndSend(mosipEventBus, MessageBusAddress.PACKET_VALIDATOR_BUS_OUT, MessageBusAddress.RETRY_BUS);
	}

	@Override
	public MessageDTO process(MessageDTO object) {

		return object;
	}

	public MosipEventBus deployVerticle() {
		MosipEventBusFactory mosipEventBusFactoryLocal = new MosipEventBusFactory();
		mosipEventBusFactoryLocal.setTracing(Tracing.newBuilder().build());
		this.setMosipEventBusFactory(mosipEventBusFactoryLocal);
		MosipEventBus mosipEventBus = this.getEventBus(this,this.findUrl().toString());
		return mosipEventBus;
	}

	public URL findUrl()
	{
		ClassLoader loader=getClass().getClassLoader();
		URL url=loader.getResource("cluster.xml");
		return url;
	}
	
	@Override
	public Integer getEventBusPort() {
		return 5711;
	}

	@Override
	public String getEventBusType() {
		return "vertx";
	}

}