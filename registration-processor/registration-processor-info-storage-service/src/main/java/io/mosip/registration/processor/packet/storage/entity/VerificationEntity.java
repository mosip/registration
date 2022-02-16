package io.mosip.registration.processor.packet.storage.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.sql.Timestamp;

/**
 * The persistent class for the reg_verification database table.
 *
 * @author Monobikash
 * @since 1.2.0
 *
 */
@Entity
@Table(name = "reg_verification", schema = "regprc")
public class VerificationEntity extends BasePacketEntity<VerificationPKEntity> implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The reg id. */
	@Column(name = "reg_id")
	private String regId;

	/** The reg id. */
	@Column(name = "verification_req_id")
	private String requestId;

	@Column(name = "matched_type")
	private String matchType;

	/** The mv usr id. */
	@Column(name = "verification_usr_id")
	private String verificationUsrId;

	@Column(name="response_text",nullable = true)
	private String reponseText;

	/** The reason code. */
	@Column(name = "reason_code")
	private String reasonCode;

	/** The status code. */
	@Column(name = "status_code")
	private String statusCode;

	/** The status comment. */
	@Column(name = "status_comment")
	private String statusComment;

	/** The cr by. */
	@Column(name = "cr_by")
	private String crBy;

	/** The cr dtimes. */
	@Column(name = "cr_dtimes", updatable = false)
	private Timestamp crDtimes;

	/** The del dtimes. */
	@Column(name = "del_dtimes")
	private Timestamp delDtimes;

	/** The is active. */
	@Column(name = "is_active")
	private Boolean isActive;

	/** The is deleted. */
	@Column(name = "is_deleted")
	private Boolean isDeleted;

	/** The upd by. */
	@Column(name = "upd_by")
	private String updBy;

	/** The upd dtimes. */
	@Column(name = "upd_dtimes")
	private Timestamp updDtimes;

	public String getRegId() {
		return regId;
	}

	public void setRegId(String regId) {
		this.regId = regId;
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	public String getMatchType() {
		return matchType;
	}

	public void setMatchType(String matchType) {
		this.matchType = matchType;
	}

	public String getVerificationUsrId() {
		return verificationUsrId;
	}

	public void setVerificationUsrId(String verificationUsrId) {
		this.verificationUsrId = verificationUsrId;
	}

	public String getReponseText() {
		return reponseText;
	}

	public void setReponseText(String reponseText) {
		this.reponseText = reponseText;
	}

	public String getReasonCode() {
		return reasonCode;
	}

	public void setReasonCode(String reasonCode) {
		this.reasonCode = reasonCode;
	}

	public String getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}

	public String getStatusComment() {
		return statusComment;
	}

	public void setStatusComment(String statusComment) {
		this.statusComment = statusComment;
	}

	public String getCrBy() {
		return crBy;
	}

	public void setCrBy(String crBy) {
		this.crBy = crBy;
	}

	public Timestamp getCrDtimes() {
		return crDtimes;
	}

	public void setCrDtimes(Timestamp crDtimes) {
		this.crDtimes = crDtimes;
	}

	public Timestamp getDelDtimes() {
		return delDtimes;
	}

	public void setDelDtimes(Timestamp delDtimes) {
		this.delDtimes = delDtimes;
	}

	public Boolean getActive() {
		return isActive;
	}

	public void setActive(Boolean active) {
		isActive = active;
	}

	public Boolean getDeleted() {
		return isDeleted;
	}

	public void setDeleted(Boolean deleted) {
		isDeleted = deleted;
	}

	public String getUpdBy() {
		return updBy;
	}

	public void setUpdBy(String updBy) {
		this.updBy = updBy;
	}

	public Timestamp getUpdDtimes() {
		return updDtimes;
	}

	public void setUpdDtimes(Timestamp updDtimes) {
		this.updDtimes = updDtimes;
	}
}
