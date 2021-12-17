package io.mosip.registration.processor.verification.response.dto;

import java.time.format.DateTimeFormatter;

import io.mosip.kernel.core.util.DateUtils;


public class VerificationResponseDTO {

	private String id;
	
	private String requestId;
	

//	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
	private String responsetime ;//= LocalDateTime.now(ZoneId.of("UTC"));
	
	private Integer returnValue;

	public VerificationResponseDTO()
	{
		super();
	}

	public VerificationResponseDTO(String id, String requestId, String responsetime, Integer returnValue) {
		super();
		this.id = id;
		this.requestId = requestId;
		
		this.returnValue = returnValue;
	//	this.analytics = analytics;
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		String  dateTime= DateUtils.getCurrentDateTimeString();
		this.responsetime = dateTime;
		
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	public String getResponsetime() {
		return responsetime;
	}

	public void setResponsetime(String responsetime) {
		this.responsetime = responsetime;
	}

	public Integer getReturnValue() {
		return returnValue;
	}

	public void setReturnValue(Integer returnValue) {
		this.returnValue = returnValue;
	}
	
}
