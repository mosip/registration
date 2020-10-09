package io.mosip.registration.entity;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "key_alias", schema = "reg")
public class KeyAlias {
	
	@Id
	@Column(name = "id")
	private String id;
	
	@Column(name = "app_id")
	private String appId;
	
	@Column(name = "ref_id")
	private String refId;
	
	@Column(name = "key_gen_dtimes")
	private Timestamp keyGenDtimes;
	
	@Column(name = "key_expire_dtimes")
	private Timestamp keyExpireDtimes;
	
	@Column(name = "status_code")
	private String statusCode;
	
	@Column(name = "lang_code")
	private String langCode;
	
	@Column(name = "cr_by")
	private String createdBy;
	
	@Column(name = "cr_dtimes")
	private Timestamp createdDtimes;
	
	@Column(name = "upd_by")
	private String updatedBy;
	
	@Column(name = "upd_dtimes")
	private Timestamp updatedTimes;
	
	@Column(name = "is_deleted")
	private boolean isDeleted;
	
	@Column(name = "del_dtimes")
	private Timestamp deletedTimes;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getRefId() {
		return refId;
	}

	public void setRefId(String refId) {
		this.refId = refId;
	}

	public Timestamp getKeyGenDtimes() {
		return keyGenDtimes;
	}

	public void setKeyGenDtimes(Timestamp keyGenDtimes) {
		this.keyGenDtimes = keyGenDtimes;
	}

	public Timestamp getKeyExpireDtimes() {
		return keyExpireDtimes;
	}

	public void setKeyExpireDtimes(Timestamp keyExpireDtimes) {
		this.keyExpireDtimes = keyExpireDtimes;
	}

	public String getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}

	public String getLangCode() {
		return langCode;
	}

	public void setLangCode(String langCode) {
		this.langCode = langCode;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public Timestamp getCreatedDtimes() {
		return createdDtimes;
	}

	public void setCreatedDtimes(Timestamp createdDtimes) {
		this.createdDtimes = createdDtimes;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	public Timestamp getUpdatedTimes() {
		return updatedTimes;
	}

	public void setUpdatedTimes(Timestamp updatedTimes) {
		this.updatedTimes = updatedTimes;
	}

	public boolean isDeleted() {
		return isDeleted;
	}

	public void setDeleted(boolean isDeleted) {
		this.isDeleted = isDeleted;
	}

	public Timestamp getDeletedTimes() {
		return deletedTimes;
	}

	public void setDeletedTimes(Timestamp deletedTimes) {
		this.deletedTimes = deletedTimes;
	}
	
}