package io.mosip.registration.processor.core.queue.factory;

import jakarta.jms.Message;
import jakarta.jms.MessageListener;

public class QueueListenerFactory {


	private QueueListenerFactory() {
	}

	public static MessageListener getListener(String queueName, QueueListener object) {
		if(queueName.equals("ACTIVEMQ")){
			return new ActiveMQMessageListener() {				
				@Override
				public void onMessage(Message message) {
					object.setListener(message);
				}
			};			
		}
		return null;
	}

}
