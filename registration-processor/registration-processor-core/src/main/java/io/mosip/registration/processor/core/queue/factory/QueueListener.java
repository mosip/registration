package io.mosip.registration.processor.core.queue.factory;

import jakarta.jms.Message;

public abstract class QueueListener {
	
	public abstract void setListener(Message message);
}