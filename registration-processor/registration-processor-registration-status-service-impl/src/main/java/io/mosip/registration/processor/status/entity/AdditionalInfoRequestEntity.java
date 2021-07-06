package io.mosip.registration.processor.status.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * The persistent class for the additional_info_request table.
 * 
 */
@Entity
@Table(name = "additional_info_request", schema = "regprc")
public class AdditionalInfoRequestEntity extends BasePacketEntity<AdditionalInfoRequestPKEntity> implements Serializable  {
	private static final long serialVersionUID = 1L;

	@Column(name = "reg_id")
	private String regId;

	@Column(name = "additional_info_process")
	private String additionalInfoProcess;

	@Column(name = "additional_info_iteration")
	private int additionalInfoIteration;

	@Column(name = "timestamp", updatable = false)
	private LocalDateTime timestamp;

	public AdditionalInfoRequestEntity() {

	}

	public String getRegId() {
		return regId;
	}

	public void setRegId(String regId) {
		this.regId = regId;
	}

	public String getAdditionalInfoProcess() {
		return additionalInfoProcess;
	}

	public void setAdditionalInfoProcess(String additionalInfoProcess) {
		this.additionalInfoProcess = additionalInfoProcess;
	}

	public int getAdditionalInfoIteration() {
		return additionalInfoIteration;
	}

	public void setAdditionalInfoIteration(int additionalInfoIteration) {
		this.additionalInfoIteration = additionalInfoIteration;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AdditionalInfoRequestEntity that = (AdditionalInfoRequestEntity) o;
		return additionalInfoIteration == that.additionalInfoIteration &&
				additionalInfoProcess.equals(that.additionalInfoProcess) &&
				regId.equals(that.regId) &&
				timestamp.equals(that.timestamp);
	}

	@Override
	public int hashCode() {
		return Objects.hash(regId, additionalInfoProcess, additionalInfoIteration, timestamp);
	}
}