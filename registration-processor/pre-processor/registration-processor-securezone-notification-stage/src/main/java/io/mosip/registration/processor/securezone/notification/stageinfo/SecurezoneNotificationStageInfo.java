package io.mosip.registration.processor.securezone.notification.stageinfo;

import org.springframework.stereotype.Component;

import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.core.spi.stage.StageInfo;
import io.mosip.registration.processor.securezone.notification.stage.SecurezoneNotificationStage;

@Component
public class SecurezoneNotificationStageInfo implements StageInfo {


	@Override
	public String[] getBasePackages() {
		return new String[] {"io.mosip.registration.processor.core.config",
                "io.mosip.registration.processor.securezone.notification.config",
                "io.mosip.registration.processor.packet.manager.config",
                "io.mosip.registration.processor.status.config", "io.mosip.registration.processor.rest.client.config",
                "io.mosip.registration.processor.core.kernel.beans"};
	}

	@Override
	public Class<? extends MosipVerticleAPIManager> getStageBeanClass() {
		return SecurezoneNotificationStage.class;
	}
}