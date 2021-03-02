package io.mosip.registration.mdm.integrator;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.List;

import org.apache.http.ParseException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.mdm.dto.MDMRequestDto;
import io.mosip.registration.mdm.dto.MdmBioDevice;

/**
 * This class will work as a mediator for request and response between the
 * application and the MDM server
 * <p>
 * This class will be used to get the Device info of any biometric device based
 * on url
 * </p>
 * <p>
 * Upon finding the devices , these devices will be registered in the device
 * registry and from there we can find any particular device
 * </p>
 * <p>
 * This class will be used to capture the biometric details
 * </p>
 * 
 * @author YASWANTH S
 *
 */
public interface MosipDeviceSpecificationProvider {

	/**
	 * Get the spec implementation class version
	 * 
	 * @return mds spec version
	 */
	public String getSpecVersion();

	/**
	 * @param bioDevice Device Information
	 * 
	 * @param modality  Stream request information to be sent
	 * @return Input Stream from the MDS
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	public InputStream stream(MdmBioDevice bioDevice, String modality) throws RegBaseCheckedException;

	/**
	 * @param bioDevice     Device Information
	 * @param mdmRequestDto capture request information to be sent
	 * @return Capture response from the MDS
	 * @throws IOException
	 * @throws ParseException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 */
	public List<BiometricsDto> rCapture(MdmBioDevice bioDevice, MDMRequestDto mdmRequestDto)
			throws RegBaseCheckedException;

	/**
	 * @param deviceInfoResponse received from mds
	 * @return list of mdmBio Devices
	 */
	public List<MdmBioDevice> getMdmDevices(String deviceInfoResponse, int port);

	/**
	 * @param mdmBioDevice bio device cached from device info
	 * @return device was is Ready status or not
	 */
	public boolean isDeviceAvailable(MdmBioDevice mdmBioDevice);

}
