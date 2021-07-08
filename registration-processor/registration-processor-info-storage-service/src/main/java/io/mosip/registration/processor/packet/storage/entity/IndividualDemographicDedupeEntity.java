package io.mosip.registration.processor.packet.storage.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 
 * @author Girish Yarru
 *
 */
@Entity
@Table(name = "individual_demographic_dedup", schema = "regprc")
public class IndividualDemographicDedupeEntity extends BasePacketEntity<IndividualDemographicDedupePKEntity>
		implements Serializable {
	private static final long serialVersionUID = 1L;

	/** The reg id. */
	@Column(name = "reg_id", nullable = false)
	private String regId;


	@Column(name = "process")
	private String process;

	@Column(name = "iteration")
	private Integer iteration;

	@Column(name = "name")
	private String name;

	@Column(name = "dob")
	private String dob;

	@Column(name = "gender")
	private String gender;

	@Column(name = "mobile_number")
	private String phone;

	@Column(name = "email")
	private String email;

	@Column(name = "pincode")
	private String postalCode;

	@Column(name = "is_active")
	private Boolean isActive;

	@Column(name = "cr_by", nullable = false)
	private String crBy = "SYSTEM";

	@Column(name = "cr_dtimes", updatable = false)
	private LocalDateTime crDtimes;

	@Column(name = "upd_by")
	private String updBy = "MOSIP_SYSTEM";

	@Column(name = "upd_dtimes")
	private LocalDateTime updDtimes;

	@Column(name = "is_deleted")
	private Boolean isDeleted;

	@Column(name = "del_dtimes")
	private LocalDateTime delDtimes;

	public IndividualDemographicDedupeEntity() {
		super();
	}

	/**
	 * Gets the reg id.
	 *
	 * @return the reg id
	 */
	public String getRegId() {
		return this.regId;
	}

	/**
	 * Sets the reg id.
	 *
	 * @param regId the new reg id
	 */
	public void setRegId(String regId) {
		this.regId = regId;
	}

	public String getName() {
		return name;
	}

	public void setName(String fullName) {
		this.name = fullName;
	}

	public String getDob() {
		return dob; // new Date(dob.getTime());
	}

	public void setDob(String dob) {
		this.dob = dob; // new Date(dob.getTime());
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String genderCode) {
		this.gender = genderCode;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public Boolean getIsActive() {
		return isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}

	public String getCrBy() {
		return crBy;
	}

	public void setCrBy(String crBy) {
		this.crBy = crBy;
	}

	public LocalDateTime getCrDtimes() {
		return crDtimes;
	}

	public void setCrDtimes(LocalDateTime crDtimes) {
		this.crDtimes = crDtimes;
	}

	public String getUpdBy() {
		return updBy;
	}

	public void setUpdBy(String updBy) {
		this.updBy = updBy;
	}

	public LocalDateTime getUpdDtimes() {
		return updDtimes;
	}

	public void setUpdDtimes(LocalDateTime updDtimes) {
		this.updDtimes = updDtimes;
	}

	public Boolean getIsDeleted() {
		return isDeleted;
	}

	public void setIsDeleted(Boolean isDeleted) {
		this.isDeleted = isDeleted;
	}

	public LocalDateTime getDelDtimes() {
		return delDtimes;
	}

	public void setDelDtimes(LocalDateTime delDtimes) {
		this.delDtimes = delDtimes;
	}

	public String getProcess() {
		return process;
	}

	public void setProcess(String process) {
		this.process = process;
	}

	public Integer getIteration() {
		return iteration;
	}

	public void setIteration(Integer iteration) {
		this.iteration = iteration;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		IndividualDemographicDedupeEntity that = (IndividualDemographicDedupeEntity) o;
		return Objects.equals(regId, that.regId) &&
				Objects.equals(process, that.process) &&
				Objects.equals(iteration, that.iteration) &&
				Objects.equals(name, that.name) &&
				Objects.equals(dob, that.dob) &&
				Objects.equals(gender, that.gender) &&
				Objects.equals(phone, that.phone) &&
				Objects.equals(email, that.email) &&
				Objects.equals(postalCode, that.postalCode) &&
				Objects.equals(isActive, that.isActive) &&
				Objects.equals(crBy, that.crBy) &&
				Objects.equals(crDtimes, that.crDtimes) &&
				Objects.equals(updBy, that.updBy) &&
				Objects.equals(updDtimes, that.updDtimes) &&
				Objects.equals(isDeleted, that.isDeleted) &&
				Objects.equals(delDtimes, that.delDtimes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(regId, process, iteration, name, dob, gender, phone, email, postalCode, isActive, crBy, crDtimes, updBy, updDtimes, isDeleted, delDtimes);
	}
}
