package io.mosip.registration.processor.status.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public class RegistrationTransactionDto {
	
	/** The transaction id. */
	private String id;

	/** The registration id. */
	private String registrationId;
	
	/** The trntypecode. */
	private String transactionTypeCode;
	
	/** The parentid. */
	private String parentTransactionId;

	/** The status code. */
	private String statusCode;
	
	/** The sub status code. */
	private String subStatusCode;

	/** The status comment. */
	private String statusComment;

	/** The reference id. */
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
	private LocalDateTime createdDateTimes;

	public RegistrationTransactionDto(String id, String registrationId, String transactionTypeCode,
			String parentTransactionId, String statusCode, String subStatusCode, String statusComment,
			LocalDateTime createdDateTimes) {

		this.id = id;
		this.registrationId = registrationId;
		this.transactionTypeCode = transactionTypeCode;
		this.parentTransactionId = parentTransactionId;
		this.statusCode = statusCode;
		this.subStatusCode = subStatusCode;
		this.statusComment = statusComment;
		this.createdDateTimes = createdDateTimes;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getRegistrationId() {
		return registrationId;
	}

	public void setRegistrationId(String registrationId) {
		this.registrationId = registrationId;
	}

	public String getTransactionTypeCode() {
		return transactionTypeCode;
	}

	public void setTransactionTypeCode(String transactionTypeCode) {
		this.transactionTypeCode = transactionTypeCode;
	}

	public String getParentTransactionId() {
		return parentTransactionId;
	}

	public void setParentTransactionId(String parentTransactionId) {
		this.parentTransactionId = parentTransactionId;
	}

	public String getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}

	public String getSubStatusCode() {
		return subStatusCode;
	}

	public void setSubStatusCode(String subStatusCode) {
		this.subStatusCode = subStatusCode;
	}

	public String getStatusComment() {
		return statusComment;
	}

	public void setStatusComment(String statusComment) {
		this.statusComment = statusComment;
	}

	public LocalDateTime getCreatedDateTimes() {
		return createdDateTimes;
	}

	public void setCreatedDateTimes(LocalDateTime createdDateTimes) {
		this.createdDateTimes = createdDateTimes;
	}

	
}
