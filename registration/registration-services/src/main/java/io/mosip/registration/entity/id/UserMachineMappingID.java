package io.mosip.registration.entity.id;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import io.mosip.registration.entity.UserMachineMapping;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * composite key for {@link UserMachineMapping}
 * 
 * @author Sreekar Chukka
 * @since 1.0.0
 */
@Embeddable
@Data
@Getter
@Setter
public class UserMachineMappingID implements Serializable {

	private static final long serialVersionUID = -1883492292190913762L;

	@Column(name = "usr_id")
	private String usrId;
	@Column(name = "regcntr_id")
	private String cntrId;
	@Column(name = "machine_id")
	private String machineId;

	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cntrId == null) ? 0 : cntrId.hashCode());
		result = prime * result + ((machineId == null) ? 0 : machineId.hashCode());
		result = prime * result + ((usrId == null) ? 0 : usrId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UserMachineMappingID other = (UserMachineMappingID) obj;
		if (cntrId == null) {
			if (other.cntrId != null)
				return false;
		} else if (!cntrId.equals(other.cntrId))
			return false;
		if (machineId == null) {
			if (other.machineId != null)
				return false;
		} else if (!machineId.equals(other.machineId))
			return false;
		if (usrId == null) {
			if (other.usrId != null)
				return false;
		} else if (!usrId.equals(other.usrId))
			return false;
		return true;
	}
}