package io.mosip.registration.processor.core.queue.factory;

import java.util.List;

public abstract class MosipQueue{
	
	public abstract void createConnection(String username, String password, String brokerUrl,
			List<String> trustedPackage);
	
	public abstract String getQueueName();

}
