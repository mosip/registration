package io.mosip.registration.processor.stages.osivalidator.utils;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.mosip.kernel.core.exception.IOException;
import io.mosip.registration.processor.core.constant.JsonConstant;
import io.mosip.registration.processor.core.constant.PacketFiles;
import io.mosip.registration.processor.core.exception.PacketDecryptionFailureException;
import io.mosip.registration.processor.core.packet.dto.FieldValue;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.packet.dto.PacketMetaInfo;
import io.mosip.registration.processor.core.packet.dto.RegOsiDto;
import io.mosip.registration.processor.core.util.IdentityIteratorUtil;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.utility.exception.ApiNotAccessibleException;
import io.mosip.registration.processor.packet.utility.service.PacketReaderService;

@Service
public class OSIUtils {
	@Value("${registration.processor.default.source}")
	private String defaultSource;
	/** The adapter. */
	@Autowired
	private PacketReaderService packetReaderService;
	
	private IdentityIteratorUtil identityIteratorUtil = new IdentityIteratorUtil();
	
	public RegOsiDto getOSIDetailsFromMetaInfo(String registrationId,Identity identity) throws UnsupportedEncodingException {
		 
		RegOsiDto regOsi = new RegOsiDto();
		//regOsi.setIntroducerId("");// not found in json
		regOsi.setIntroducerTyp(getMetaDataValue(JsonConstant.INTRODUCERTYPE,identity));
		regOsi.setOfficerHashedPin(getMetaDataValue(JsonConstant.OFFICERPIN,identity));
		regOsi.setOfficerHashedPwd(getOsiDataValue(JsonConstant.OFFICERPWR,identity));
		regOsi.setOfficerId(getOsiDataValue(JsonConstant.OFFICERID,identity));
		regOsi.setOfficerOTPAuthentication(getOsiDataValue(JsonConstant.OFFICEROTPAUTHENTICATION,identity));
		regOsi.setPreregId(getMetaDataValue(JsonConstant.PREREGISTRATIONID,identity));
		regOsi.setRegId(getMetaDataValue(JsonConstant.REGISTRATIONID,identity));
		regOsi.setSupervisorBiometricFileName(getOsiDataValue(JsonConstant.SUPERVISORBIOMETRICFILENAME,identity));
		regOsi.setSupervisorHashedPin(getOsiDataValue(JsonConstant.OFFICERPHOTONAME,identity));
		regOsi.setSupervisorHashedPwd(getOsiDataValue(JsonConstant.SUPERVISORPWR,identity));
		regOsi.setSupervisorId(getOsiDataValue(JsonConstant.SUPERVISORID,identity));
		regOsi.setSupervisorOTPAuthentication(getOsiDataValue(JsonConstant.SUPERVISOROTPAUTHENTICATION,identity));
		regOsi.setOfficerBiometricFileName(getOsiDataValue(JsonConstant.OFFICERBIOMETRICFILENAME,identity));
		
		return regOsi;
	}
	
	public String getOsiDataValue(String label,Identity identity) throws UnsupportedEncodingException {
		List<FieldValue> osiData = identity.getOperationsData();
		return identityIteratorUtil.getMetadataLabelValue(osiData, label);

	}
	public String getMetaDataValue(String label,Identity identity) throws UnsupportedEncodingException {
		List<FieldValue> metadata = identity.getMetaData();
		return identityIteratorUtil.getMetadataLabelValue(metadata, label);

	}
	
	public Identity getIdentity(String registrationId)
			throws PacketDecryptionFailureException, ApiNotAccessibleException, IOException, java.io.IOException,
			io.mosip.registration.processor.packet.utility.exception.PacketDecryptionFailureException {
		InputStream packetMetaInfoStream = packetReaderService.getFile(registrationId,
				PacketFiles.PACKET_META_INFO.name(), defaultSource);
		PacketMetaInfo packetMetaInfo = (PacketMetaInfo) JsonUtil.inputStreamtoJavaObject(packetMetaInfoStream,PacketMetaInfo.class);
		return packetMetaInfo.getIdentity();

	}

}