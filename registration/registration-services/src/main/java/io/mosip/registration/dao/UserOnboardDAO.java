package io.mosip.registration.dao;

import java.sql.Timestamp;
import java.util.List;

import io.mosip.kernel.packetmanager.dto.BiometricsDto;
import io.mosip.registration.dto.biometric.BiometricDTO;
import io.mosip.registration.entity.UserBiometric;
import io.mosip.registration.entity.UserMachineMapping;
import io.mosip.registration.exception.RegBaseCheckedException;

/**
 * This class is used to save the biometric details of the registration officer.
 * This class is used to get the station id by providing the respective machine's mac address,
 * center id by providing the respective station id.
 *
 * @author Sreekar Chukka
 * @since 1.0.0
 */
public interface UserOnboardDAO {

	/**
	 * This method is used to insert the biometric details of the user into the {@link UserBiometric} table.
	 *
	 * @param biometricDTO 
	 * 				the biometric DTO of the user
	 * @return the success/error response.
	 */
	String insert(BiometricDTO biometricDTO);
	
	String insert(List<BiometricsDto> biometrics);
	
	/**
	 * This method is used to get Station ID by giving machine name.
	 *
	 * @param machineName
	 * 				machine name
	 * @return station ID
	 * @throws RegBaseCheckedException 
	 * 				the reg base checked exception
	 */
	String getStationID(String machineName) throws RegBaseCheckedException;
	
	/**
	 * This method is used to get center ID using stationID.
	 *
	 * @param stationID 
	 * 				the station ID
	 * @return center ID
	 * @throws RegBaseCheckedException 
	 * 				the reg base checked exception
	 */
	String getCenterID(String stationID) throws RegBaseCheckedException;
	
	
	/**
	 * This method is used to save user to {@link UserMachineMapping} table.
	 *
	 * @return the success/error response.
	 */
	String save();
	
	
	/**
	 * Gets the last updated operator bio-metric date time.
	 *
	 * @param usrID the usr ID
	 * @return the last updated operator bio-metric date time
	 */
	Timestamp getLastUpdatedTime(String usrId);
}
