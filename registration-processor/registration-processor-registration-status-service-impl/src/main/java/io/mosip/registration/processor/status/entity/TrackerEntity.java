package io.mosip.registration.processor.status.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**	
 * The Class TransactionEntity.
 */
@Entity
@Table(name = "registration_tracker", schema = "regprc")
public class TrackerEntity extends BaseTrackerEntity {

	/** The registration id. */
	@Column(name = "reg_id")
	private String registrationId;

	/** The remarks. */
	@Column(name = "transaction_flow_id")
	private String transactionFlowId;

	/** The status code. */
	@Column(name = "status_code")
	private String statusCode;

	/**  is deleted?. */
	@Column(name = "is_deleted", length = 32)
	private Boolean isDeleted ;

	/** The update date time. */
	@Column(name = "del_dtimes")
	private LocalDateTime deleteDateTime;

	/** The created by. */
	@Column(name = "cr_by")
	private String createdBy = "MOSIP_SYSTEM";

	/** The create date time. */
	@Column(name = "cr_dtimes")
	private LocalDateTime createDateTime;

	/** The updated by. */
	@Column(name = "upd_by", length = 32)
	private String updatedBy = "MOSIP_SYSTEM";

	/** The update date time. */
	@Column(name = "upd_dtimes")
	private LocalDateTime updateDateTime;
	/**
	 * Instantiates a new transaction entity.
	 */
	public TrackerEntity() {
		super();
	}

	/**
	 * Instantiates a new transaction entity.
	 *
	 * @param transactionId            the transaction id
	 * @param registrationId            the registration id
	 * @param statusCode            the status code
	 */
	public TrackerEntity(String transactionId, String registrationId, String transactionFlowId, String statusCode) {
		this.registrationId = registrationId;
		this.transactionId = transactionId;
		this.transactionFlowId = transactionFlowId;
		this.statusCode = statusCode;
	}

	/**
	 * Get Enrolment_Id from transaction table.
	 * 
	 * @return the enrolmentId
	 */
	public String getRegistrationId() {
		return registrationId;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	/**
	 * Set RegistrationId to transaction table.
	 *
	 * @param registrationId
	 *            the new registration id
	 */
	public void setRegistrationId(String registrationId) {
		this.registrationId = registrationId;
	}

	public String getTransactionFlowId() {
		return transactionFlowId;
	}

	public void setTransactionFlowId(String transactionFlowId) {
		this.transactionFlowId = transactionFlowId;
	}

	/**
	 * Get Status Code from transaction table.
	 * 
	 * @return the statusCode
	 */
	public String getStatusCode() {
		return statusCode;
	}

	/**
	 * Set status Code to transaction table.
	 * 
	 * @param statusCode
	 *            the statusCode to set
	 */
	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}
	
	/**
	 * Gets the checks if is deleted.
	 *
	 * @return the checks if is deleted
	 */
	public Boolean getIsDeleted() {
		return isDeleted;
	}

	/**
	 * Sets the checks if is deleted.
	 *
	 * @param isDeleted the new checks if is deleted
	 */
	public void setIsDeleted(Boolean isDeleted) {
		this.isDeleted = isDeleted;
	}

	/**
	 * Gets the delete date time.
	 *
	 * @return the delete date time
	 */
	public LocalDateTime getDeleteDateTime() {
		return deleteDateTime;
	}

	/**
	 * Sets the delete date time.
	 *
	 * @param deleteDateTime the new delete date time
	 */
	public void setDeleteDateTime(LocalDateTime deleteDateTime) {
		this.deleteDateTime = deleteDateTime;
	}

	/**
	 * Get created By from transaction table.
	 * 
	 * @return the createdBy
	 */
	public String getCreatedBy() {
		return createdBy;
	}

	/**
	 * Set created By to transaction table.
	 * 
	 * @param createdBy
	 *            the createdBy to set
	 */
	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	/**
	 * Get create Date Time from transaction table.
	 * 
	 * @return the createDateTime
	 */

	public LocalDateTime getCreateDateTime() {
		return createDateTime;
	}

	/**
	 * Set create Date Time to transaction table.
	 * 
	 * @param createDateTime
	 *            the createDateTime to set
	 */
	public void setCreateDateTime(LocalDateTime createDateTime) {
		this.createDateTime = createDateTime;
	}

	/**
	 * Get updated By from transaction table.
	 * 
	 * @return the updatedBy
	 */
	public String getUpdatedBy() {
		return updatedBy;
	}

	/**
	 * Set updated By to transaction table.
	 * 
	 * @param updatedBy
	 *            the updatedBy to set
	 */
	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	/**
	 * Get update Date Time By from transaction table.
	 * 
	 * @return the updateDateTime
	 */
	public LocalDateTime getUpdateDateTime() {
		return updateDateTime;
	}

	/**
	 * Set update Date Time to transaction table.
	 * 
	 * @param updateDateTime
	 *            the updateDateTime to set
	 */
	public void setUpdateDateTime(LocalDateTime updateDateTime) {
		this.updateDateTime = updateDateTime;
	}

}
