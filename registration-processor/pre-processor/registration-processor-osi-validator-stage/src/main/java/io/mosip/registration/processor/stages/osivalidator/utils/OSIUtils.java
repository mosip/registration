package io.mosip.registration.processor.stages.osivalidator.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.constant.JsonConstant;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.packet.dto.FieldValue;
import io.mosip.registration.processor.core.packet.dto.NewRegisteredDevice;
import io.mosip.registration.processor.core.packet.dto.RegOsiDto;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import org.apache.commons.collections.MapUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OSIUtils {
	@Value("${packet.default.source}")
	private String defaultSource;

	@Autowired
	private ObjectMapper objectMapper;

	public RegOsiDto getOSIDetailsFromMetaInfo(Map<String, String> metaInfo) throws IOException, ApisResourceAccessException, JsonProcessingException, PacketManagerException, JSONException {
		Map<String, String> allMap = getMetaMap(metaInfo);
		RegOsiDto regOsi = new RegOsiDto();
		regOsi.setOfficerHashedPin(allMap.get(JsonConstant.OFFICERPIN));
		regOsi.setOfficerHashedPwd(allMap.get(JsonConstant.OFFICERPWR));
		regOsi.setOfficerId(allMap.get(JsonConstant.OFFICERID));
		regOsi.setOfficerOTPAuthentication(allMap.get(JsonConstant.OFFICEROTPAUTHENTICATION));
		regOsi.setPreregId(allMap.get(JsonConstant.PREREGISTRATIONID));
		regOsi.setRegId(allMap.get(JsonConstant.REGISTRATIONID));
		regOsi.setSupervisorBiometricFileName(allMap.get(JsonConstant.SUPERVISORBIOMETRICFILENAME));
		regOsi.setSupervisorHashedPin(allMap.get(JsonConstant.OFFICERPHOTONAME));
		regOsi.setSupervisorHashedPwd(allMap.get(JsonConstant.SUPERVISORPWR));
		regOsi.setSupervisorId(allMap.get(JsonConstant.SUPERVISORID));
		regOsi.setSupervisorOTPAuthentication(allMap.get(JsonConstant.SUPERVISOROTPAUTHENTICATION));
		regOsi.setOfficerBiometricFileName(allMap.get(JsonConstant.OFFICERBIOMETRICFILENAME));
		regOsi.setRegcntrId(allMap.get(JsonConstant.CENTERID));
		regOsi.setMachineId(allMap.get(JsonConstant.MACHINEID));
		regOsi.setLatitude(allMap.get(JsonConstant.GEOLOCLATITUDE));
		regOsi.setLongitude(allMap.get(JsonConstant.GEOLOCLONGITUDE));
		regOsi.setPacketCreationDate(metaInfo.get(JsonConstant.CREATIONDATE));
		regOsi.setCapturedRegisteredDevices(getCapturedRegisteredDevices(metaInfo));
		
		return regOsi;
	}
	
	public Map<String, String> getMetaMap(Map<String, String> metaInfo) throws java.io.IOException, ApisResourceAccessException, PacketManagerException, JsonProcessingException, JSONException {
		Map<String, String> allMap = new HashMap<>();

		if (MapUtils.isNotEmpty(metaInfo)) {
			String operationsDataString = metaInfo.get(JsonConstant.OPERATIONSDATA);
			String metaDataString = metaInfo.get(JsonConstant.METADATA);
			if (StringUtils.isNotEmpty(operationsDataString)) {
				JSONArray jsonArray = new JSONArray(operationsDataString);
				addToMap(jsonArray, allMap);
			}
			if (StringUtils.isNotEmpty(metaDataString)) {
				JSONArray jsonArray = new JSONArray(metaDataString);
				addToMap(jsonArray, allMap);
			}
		}
		return allMap;
	}

	private void addToMap(JSONArray jsonArray, Map<String, String> allMap) throws JSONException, IOException {
		for (int i =0; i < jsonArray.length(); i++) {
			JSONObject jsonObject = (JSONObject) jsonArray.get(i);
			FieldValue fieldValue = objectMapper.readValue(jsonObject.toString(), FieldValue.class);
			allMap.put(fieldValue.getLabel(), fieldValue.getValue());
		}
	}

	private List<NewRegisteredDevice> getCapturedRegisteredDevices(Map<String, String> metaInfo) throws JSONException, IOException {
		List<NewRegisteredDevice> registeredDeviceList = new ArrayList<>();
		if (MapUtils.isNotEmpty(metaInfo)) {
			String capturedRegistereddeviceStr = metaInfo.get(JsonConstant.CAPTUREDREGISTEREDDEVICES);
			if (StringUtils.isNotEmpty(capturedRegistereddeviceStr)) {
				JSONArray jsonArray = new JSONArray(capturedRegistereddeviceStr);
				for (int i =0; i < jsonArray.length(); i++) {
					JSONObject jsonObject = (JSONObject) jsonArray.get(i);
					NewRegisteredDevice fieldValue = objectMapper.readValue(jsonObject.toString(), NewRegisteredDevice.class);
					registeredDeviceList.add(fieldValue);
				}
			}
		}
		return registeredDeviceList;
	}

}