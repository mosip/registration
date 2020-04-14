package io.mosip.registration.repositories;
import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;
import io.mosip.registration.entity.MachineMaster;
import io.mosip.registration.entity.id.RegMachineSpecId;

/**
 * The repository interface for {@link MachineMaster} entity
 * @author Yaswanth S
 * @since 1.0.0
 *
 */
public interface MachineMasterRepository extends BaseRepository<MachineMaster, RegMachineSpecId>{
	
		
	/**
	 * Find machine based on  machine name.
	 * 
	 * @param machineName
	 * @param langCode
	 * @return
	 */
	MachineMaster findByIsActiveTrueAndNameAndRegMachineSpecIdLangCode(String machineName, String langCode);
	
}
