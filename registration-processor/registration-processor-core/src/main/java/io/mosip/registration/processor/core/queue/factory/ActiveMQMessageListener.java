package io.mosip.registration.processor.core.queue.factory;

import jakarta.jms.Message;
import jakarta.jms.MessageListener;

public abstract class ActiveMQMessageListener implements MessageListener {

	@Override
	public abstract void onMessage(Message message);

}
