package io.mosip.registration.processor.status.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * The persistent class for the sub_workflow_mapping table.
 * 
 */
@Entity
@Table(name = "sub_workflow_mapping", schema = "regprc")
public class SubWorkflowMappingEntity extends BasePacketEntity<SubWorkflowPKEntity> implements Serializable  {
	private static final long serialVersionUID = 1L;

	@Column(name = "process")
	private String process;

	@Column(name = "iteration")
	private int iteration;

	@Column(name = "timestamp", updatable = false)
	private LocalDateTime timestamp;

	@Column(name = "parent_process")
	private String parentProcess;

	@Column(name = "parent_iteration")
	private int parentIteration;

	public SubWorkflowMappingEntity() {

	}

	public String getProcess() {
		return process;
	}

	public void setProcess(String process) {
		this.process = process;
	}

	public int getIteration() {
		return iteration;
	}

	public void setIteration(int iteration) {
		this.iteration = iteration;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public String getParentProcess() {
		return parentProcess;
	}

	public void setParentProcess(String parentProcess) {
		this.parentProcess = parentProcess;
	}

	public int getParentIteration() {
		return parentIteration;
	}

	public void setParentIteration(int parentIteration) {
		this.parentIteration = parentIteration;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SubWorkflowMappingEntity that = (SubWorkflowMappingEntity) o;
		return iteration == that.iteration &&
				process.equals(that.process) &&
				timestamp.equals(that.timestamp);
	}

	@Override
	public int hashCode() {
		return Objects.hash(process, iteration, timestamp);
	}
}