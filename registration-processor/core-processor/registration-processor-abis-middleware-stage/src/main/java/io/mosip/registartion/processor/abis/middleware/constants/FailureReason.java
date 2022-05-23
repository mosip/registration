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
	UNEXPECTED_ERROR("3","Unexpected error"),
	UNABLE_TO_SERVE("4","Unable to serve the request - invalid request structure"),
	MISSING_REFERENCEID("5","missing referenceId (in request body)"),
	MISSING_REQUESTID("6","missing requestId (in request body)"),
	UNABLE_TO_FETCH("7","unable to fetch biometric details (using referenceURL)"),
	MISSING_REFERENCEURL("8","missing reference URL (in request body)"),
	MISSING_REQUESTTIME("9","missing requesttime (in request body)"),
	REFID_ALREADY_EXISTS("10","referenceId already exists (in ABIS)"),
	EMPTY_CBEFF("11","CBEFF has no data"),
	REFID_NOT_FOUND("12","referenceId not found (in ABIS)"),
	INVALID_VERSION("13","invalid version"),
	INVALID_ID("14","invalid id"),
	INVALID_REQ_FORMAT("15","invalid requesttime format"),
	INVALID_CBEFF("16","invalid CBEFF format"),
	DATASHARE_EXPIRED("17","data share URL has expired");

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
