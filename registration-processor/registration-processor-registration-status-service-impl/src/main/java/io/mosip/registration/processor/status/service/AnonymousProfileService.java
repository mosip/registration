package io.mosip.registration.processor.status.service;

import org.springframework.stereotype.Service;

@Service
public interface AnonymousProfileService {

	/**
	 * save anonymous profile
	 * @param id
	 * @param processStage
	 * @param profileJson
	 */
	public void saveAnonymousProfile(String regId, String processStage,String profileJson); 
}
