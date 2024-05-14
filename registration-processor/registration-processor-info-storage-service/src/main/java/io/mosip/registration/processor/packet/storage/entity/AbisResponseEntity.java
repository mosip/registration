package io.mosip.registration.processor.packet.storage.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * The persistent class for the abis_response database table.
 * 
 */
@Entity
@Table(name = "abis_response", schema = "regprc")
public class AbisResponseEntity extends BasePacketEntity<AbisResponsePKEntity> implements Serializable {
	private static final long serialVersionUID = 1L;

	@Column(name = "cr_by")
	private String crBy;

	@Column(name = "cr_dtimes", updatable = false)

	private LocalDateTime crDtimes;

	@Column(name = "del_dtimes")
	private LocalDateTime delDtimes;

	@Column(name = "is_deleted")
	private Boolean isDeleted;

	@Column(name = "lang_code")
	private String langCode;

	@Column(name = "resp_dtimes")
	private LocalDateTime respDtimes;

	@Column(name = "resp_text")
	private byte[] respText;

	@Column(name = "status_code")
	private String statusCode;

	@Column(name = "status_comment")
	private String statusComment;

	@Column(name = "upd_by")
	private String updBy;

	@Column(name = "upd_dtimes")
	private LocalDateTime updDtimes;

	@Column(name = "abis_req_id")
	private String abisRequest;

	public AbisResponseEntity() {
	}

	public String getCrBy() {
		return this.crBy;
	}

	public void setCrBy(String crBy) {
		this.crBy = crBy;
	}

	public LocalDateTime getCrDtimes() {
		return this.crDtimes;
	}

	public void setCrDtimes(LocalDateTime crDtimes) {
		this.crDtimes = crDtimes;
	}

	public LocalDateTime getDelDtimes() {
		return this.delDtimes;
	}

	public void setDelDtimes(LocalDateTime delDtimes) {
		this.delDtimes = delDtimes;
	}

	public Boolean getIsDeleted() {
		return this.isDeleted;
	}

	public void setIsDeleted(Boolean isDeleted) {
		this.isDeleted = isDeleted;
	}

	public String getLangCode() {
		return this.langCode;
	}

	public void setLangCode(String langCode) {
		this.langCode = langCode;
	}

	public LocalDateTime getRespDtimes() {
		return this.respDtimes;
	}

	public void setRespDtimes(LocalDateTime respDtimes) {
		this.respDtimes = respDtimes;
	}

	public byte[] getRespText() {
		return respText.clone();
	}

	public void setRespText(byte[] respText) {
		this.respText = respText != null ? respText : null;
	}

	public String getStatusCode() {
		return this.statusCode;
	}

	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}

	public String getStatusComment() {
		return this.statusComment;
	}

	public void setStatusComment(String statusComment) {
		this.statusComment = statusComment;
	}

	public String getUpdBy() {
		return this.updBy;
	}

	public void setUpdBy(String updBy) {
		this.updBy = updBy;
	}

	public LocalDateTime getUpdDtimes() {
		return this.updDtimes;
	}

	public void setUpdDtimes(LocalDateTime updDtimes) {
		this.updDtimes = updDtimes;
	}

	public String getAbisRequest() {
		return this.abisRequest;
	}

	public void setAbisRequest(String abisRequest) {
		this.abisRequest = abisRequest;
	}

}