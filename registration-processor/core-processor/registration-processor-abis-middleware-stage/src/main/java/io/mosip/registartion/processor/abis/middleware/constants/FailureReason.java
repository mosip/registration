package io.mosip.registartion.processor.abis.middleware.constants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Dhanendra
 *
 */
public enum FailureReason {
	
	INTERNAL_ERROR("1", "Internal error - Unknown"),
	ABORTED("2","Aborted"),
	UNEXPECTED_ERROR("3","Unexpected error - Unable to access biometric data"),
	UNABLE_TO_SERVE("4","Unable to serve the request"),
	INVALID_REQUEST("5","Invalid request / Missing mandatory fields"),
	UNAUTHORIZED_ACCESS("6","Unauthorized Access"),
	UNABLE_TO_FETCH("7","Unable to fetch biometric details");

	private String key;
	private String value;
	private static final Map<String, String> failureMap = Collections.unmodifiableMap(initializeMapping());
	 
	private static Map<String, String> initializeMapping() {
	       Map<String, String> failureMap = new HashMap<String, String>();
	       for (FailureReason s : FailureReason.values()) {
	           failureMap.put(s.key, s.value);
	       }
	       return failureMap;
	 }
	 FailureReason(String key, String value) {
	       this.key = key;
	       this.value = value;
	 }
	 public static String getValueFromKey(String key) {
	       return failureMap.get(key);
	 }

}
