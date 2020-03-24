package io.mosip.registration.entity.id;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import io.mosip.registration.entity.CenterMachine;
import lombok.Data;


/**
 * Composite key for {@link CenterMachine}
 *
 * @author Dinesh Ashokan
 * @version 1.0.0
 */
@Embeddable
@Data
public class CenterMachineId implements Serializable{
	
	
	private static final long serialVersionUID = 241072783610318336L;

	@Column(name = "machine_id")
	private String machineId;
	@Column(name = "regcntr_id")
	private String regCenterId;
	
	public String getMachineId() {
		return machineId;
	}
	public void setMachineId(String machineId) {
		this.machineId = machineId;
	}
	public String getRegCenterId() {
		return regCenterId;
	}
	public void setRegCenterId(String regCenterId) {
		this.regCenterId = regCenterId;
	}
		
}
