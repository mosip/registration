package io.mosip.registration.service.bio;

import java.io.InputStream;
import java.util.List;

import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.mdm.dto.MDMRequestDto;
import io.mosip.registration.mdm.dto.MdmBioDevice;

/**
 * This class {@code BioService} handles all the biometric captures and
 * validations through MDM service
 * 
 * @author taleev.aalam
 * @since 1.0.0
 */
public interface BioService {

	/**
	 * checks if the MDM service is enabled
	 * 
	 * @return boolean the validation result. <code>true</code> if match is found,
	 *         else <code>false</code>
	 */
	boolean isMdmEnabled();

	public List<BiometricsDto> captureModality(MDMRequestDto mdmRequestDto) throws RegBaseCheckedException;

	public List<BiometricsDto> captureModalityForAuth(MDMRequestDto mdmRequestDto) throws RegBaseCheckedException;

	/**
	 * @param modality
	 *            modality to find device subId
	 * @return live stream
	 * @throws RegBaseCheckedException
	 */
	public InputStream getStream(String modality) throws RegBaseCheckedException;

	/**
	 * @param mdmBioDevice
	 *            bio Device info
	 * @param modality
	 *            modality to find device subId
	 * @return live stream
	 * @throws RegBaseCheckedException
	 */
	public InputStream getStream(MdmBioDevice mdmBioDevice, String modality) throws RegBaseCheckedException;
}