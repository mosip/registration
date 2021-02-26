package io.mosip.registration.repositories;

import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;
import io.mosip.registration.entity.CenterMachine;
import io.mosip.registration.entity.id.CenterMachineId;

/**
 * To get the Center id based on station id
 * 
 * @author Dinesh Ashokan
 * @since 1.0.0
 *
 */
public interface CenterMachineRepository extends BaseRepository<CenterMachine, CenterMachineId> {
	
	/**
	 * Find active center id based on {@link CenterMachine} station id.
	 *
	 * @param machineId the user id
	 * @return center id
	 */
	CenterMachine findByIsActiveTrueAndCenterMachineIdMachineId(String machineId);

	/**
	 * find center machine mapping entry
	 * @param machineId
	 * @return
	 */
	CenterMachine findByCenterMachineIdMachineId(String machineId);
}