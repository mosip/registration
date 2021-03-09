package io.mosip.registration.processor.stages.executor;

import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

class CustomEnvironment extends StandardEnvironment{
	
	private MutablePropertySources propertySources = new MutablePropertySources();
	
	@Override
	public void customizePropertySources(MutablePropertySources propertySources) {
		this.propertySources = propertySources;
	}
	
	public MutablePropertySources getPropertySources() {
		return propertySources;
	}

}
