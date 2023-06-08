package io.mosip.registration.processor.core.queue.factory;

import java.util.List;

import io.mosip.registration.processor.core.spi.queue.MosipQueueConnectionFactory;

public class MosipQueueConnectionFactoryImpl implements MosipQueueConnectionFactory<MosipQueue> {

	@Override
	public MosipQueue createConnection(String typeOfQueue, String username, String password,
			String url, List<String> trustedPackages) {
		if(typeOfQueue.equalsIgnoreCase("ACTIVEMQ")) {
			return new MosipActiveMq(typeOfQueue, username, password, url, trustedPackages);
		}
		else {
			return null;
		}
	}
	

}
