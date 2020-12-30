package io.mosip.registration.processor.abstractverticle;

import java.net.URL;

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
	private MosipEventBus mosipEventBus;

	public void start() throws UnsupportedEventBusTypeException {
		ConsumerVerticle consumerVerticle = new ConsumerVerticle();
		vertx.deployVerticle(consumerVerticle);
		this.mosipEventBus = (new MosipEventBusFactory()).getEventBus(vertx, "vertx");
		this.messageDTO = new MessageDTO();
		this.messageDTO.setRid("1001");
		this.messageDTO.setRetryCount(0);
		this.messageDTO.setMessageBusAddress(MessageBusAddress.PACKET_VALIDATOR_BUS_IN);
		this.messageDTO.setIsValid(true);
		this.messageDTO.setInternalError(false);
		this.messageDTO.setReg_type(RegistrationType.NEW);
		this.send(mosipEventBus, MessageBusAddress.DEMO_DEDUPE_BUS_IN, this.messageDTO);
		this.consume(mosipEventBus, MessageBusAddress.PACKET_VALIDATOR_BUS_IN);
		this.consumeAndSend(mosipEventBus, MessageBusAddress.PACKET_VALIDATOR_BUS_OUT, MessageBusAddress.RETRY_BUS);
	}

	@Override
	public MessageDTO process(MessageDTO object) {

		return object;
	}

	public MosipEventBus deployVerticle() {
		this.setMosipEventBusFactory(new MosipEventBusFactory());
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