package io.mosip.registration.packetmanager.util;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.mosip.kernel.cbeffutil.impl.CbeffImpl;
import io.mosip.kernel.core.cbeffutil.entity.BIR;
import io.mosip.kernel.core.util.HMACUtils;
import io.mosip.registration.packetmananger.constants.PacketManagerConstants;

@Component
public class PacketManagerHelper {
	
	@Autowired
	private CbeffImpl xmlBuilder;
	
	public byte[] getXMLData(List<BIR> birs) throws Exception {
		byte[] xmlBytes = null;
		try(InputStream file = getClass().getClassLoader().getResourceAsStream(PacketManagerConstants.CBEFF_SCHEMA_FILE_PATH)) {
			byte[] bytesArray = new byte[(int) file.available()];
			file.read(bytesArray);
			xmlBytes = xmlBuilder.createXML(birs, bytesArray);
		}
		return xmlBytes;
	}
	
	public byte[] generateHash(List<String> order, Map<String, byte[]> data) {
		if(order != null && !order.isEmpty()) {
			for(String name : order) {
				HMACUtils.update(data.get(name));
			}			
			return HMACUtils.digestAsPlainText(HMACUtils.updatedHash()).getBytes();
		}
		return null;
	}

}
