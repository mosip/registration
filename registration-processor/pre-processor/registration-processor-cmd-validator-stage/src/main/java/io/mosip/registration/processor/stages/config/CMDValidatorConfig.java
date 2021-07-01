package io.mosip.registration.processor.stages.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.mosip.registration.processor.packet.storage.utils.OSIUtils;
import io.mosip.registration.processor.stages.app.CMDValidationProcessor;
import io.mosip.registration.processor.stages.cmdvalidator.CenterValidator;
import io.mosip.registration.processor.stages.cmdvalidator.DeviceValidator;
import io.mosip.registration.processor.stages.cmdvalidator.MachineValidator;

@Configuration
public class CMDValidatorConfig {
	
	@Bean
	public CMDValidationProcessor getCMDValidationProcessor() {
		return new CMDValidationProcessor();
	}
	
	@Bean
	public CenterValidator getCenterValidator() {
		return new CenterValidator();
	}
	
	@Bean
	public MachineValidator getMachineValidator() {
		return new MachineValidator();
	}
	
	@Bean
	public DeviceValidator getDeviceValidator() {
		return new DeviceValidator();
	}
	
	@Bean
	public OSIUtils getOSIUtils() {
		return new OSIUtils();
	}

}
