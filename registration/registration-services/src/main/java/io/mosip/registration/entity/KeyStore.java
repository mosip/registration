package io.mosip.registration.entity;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * The Entity Class for KeyStore.
 *
 * @author Brahmananda Reddy
 * @since 1.0.0
 */
@Entity
@Table(name = "key_store", schema = "reg")
public class KeyStore {
	
	@Id
	@Column(name = "id")
	private String id;
	
	@Column(name = "master_key")
	private String masterKey;
	
	@Column(name = "private_key")
	private String privateKey;
	
	@Column(name = "certificate_data")
	private String certificateData;
	
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

	public String getMasterKey() {
		return masterKey;
	}

	public void setMasterKey(String masterKey) {
		this.masterKey = masterKey;
	}

	public String getPrivateKey() {
		return privateKey;
	}

	public void setPrivateKey(String privateKey) {
		this.privateKey = privateKey;
	}

	public String getCertificateData() {
		return certificateData;
	}

	public void setCertificateData(String certificateData) {
		this.certificateData = certificateData;
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
