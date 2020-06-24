package io.mosip.registration.mdm.integrator;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.List;

import org.apache.http.ParseException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.mosip.kernel.packetmanager.dto.BiometricsDto;
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
 * Upon findin the devices , these devices will be regiestered in the device
 * registery and from there we can find any particular device
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
	 * @param bioDevice
	 *            Device Information
	 * 
	 * @param registrationStreamRequestDto
	 *            Stream request information to be sent
	 * @return Input Stream from the MDS
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	public InputStream stream(MdmBioDevice bioDevice, String modality) throws MalformedURLException, IOException;

	/**
	 * @param BioDevice
	 *            Device Information
	 * @param registrationCaptureRequestDto
	 *            capture request information to be sent
	 * @return Capture response from the MDS
	 * @throws IOException
	 * @throws ParseException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 */
	public List<BiometricsDto> rCapture(MdmBioDevice bioDevice, MDMRequestDto mdmRequestDto)
			throws JsonParseException, JsonMappingException, ParseException, IOException;

	/**
	 * @param deviceInfoResponse
	 *            received from mds
	 * @return list of mdmBio Devices
	 */
	public List<MdmBioDevice> getMdmDevices(String deviceInfoResponse, int port);

}
