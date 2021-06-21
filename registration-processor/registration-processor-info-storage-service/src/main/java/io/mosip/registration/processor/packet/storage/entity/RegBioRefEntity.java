package io.mosip.registration.processor.packet.storage.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "reg_bio_ref", schema = "regprc")
public class RegBioRefEntity extends BasePacketEntity<RegBioRefPKEntity> implements Serializable {
	private static final long serialVersionUID = 1L;

	@Column(name = "bio_ref_id")
	private String bioRefId;

	@Column(name = "cr_by", nullable = false)
	private String crBy = "SYSTEM";

	@Column(name = "del_dtimes")
	private LocalDateTime delDtimes;

	@Column(name = "is_active")
	private Boolean isActive;

	@Column(name = "is_deleted")
	private Boolean isDeleted;

	@Column(name = "upd_by")
	private String updBy = "MOSIP_SYSTEM";

	@Column(name = "upd_dtimes")
	private LocalDateTime updDtimes;

	@Column(name = "reg_type")
	private String regType;
	
	@Column(name = "iteration")
	private int iteration;
	
	public RegBioRefEntity() {
	}

	public String getBioRefId() {
		return this.bioRefId;
	}

	public void setBioRefId(String bioRefId) {
		this.bioRefId = bioRefId;
	}

	public String getCrBy() {
		return this.crBy;
	}

	public void setCrBy(String crBy) {
		this.crBy = crBy;
	}

	public LocalDateTime getDelDtimes() {
		return this.delDtimes;
	}

	public void setDelDtimes(LocalDateTime delDtimes) {
		this.delDtimes = delDtimes;
	}

	public Boolean getIsActive() {
		return this.isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}

	public Boolean getIsDeleted() {
		return this.isDeleted;
	}

	public void setIsDeleted(Boolean isDeleted) {
		this.isDeleted = isDeleted;
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
	public String getRegType() {
		return regType;
	}

	public void setRegType(String regType) {
		this.regType = regType;
	}

	public int getIteration() {
		return iteration;
	}

	public void setIteration(int iteration) {
		this.iteration = iteration;
	}
}