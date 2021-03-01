package io.mosip.registration.processor.stages.uingenerator.stageinfo;

import org.springframework.stereotype.Component;

import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.core.spi.stage.StageInfo;
import io.mosip.registration.processor.stages.uingenerator.stage.UinGeneratorStage;

@Component
public class UinGeneratorStageInfo implements StageInfo {


	@Override
	public String[] getBasePackages() {
		return new String[] {"io.mosip.registration.processor.core.config",
				"io.mosip.registration.processor.stages.uingenerator.config",
				"io.mosip.registration.processor.status.config", "io.mosip.registration.processor.rest.client.config",
				"io.mosip.registration.processor.packet.storage.config",
				"io.mosip.registration.processor.stages.config",
				"io.mosip.kernel.packetmanager.config",
				"io.mosip.registration.processor.packet.manager.config",
				"io.mosip.registration.processor.core.kernel.beans"};
	}

	@Override
	public Class<? extends MosipVerticleAPIManager> getStageBeanClass() {
		return UinGeneratorStage.class;
	}
}