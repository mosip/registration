package io.mosip.registration.processor.core.spi.stage;

import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;

public interface StageInfo {
	
	String[] getBasePackages();
	
	Class<? extends MosipVerticleAPIManager> getStageBeanClass();

}
