package io.mosip.registration.packetmanager.util;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import io.mosip.kernel.cbeffutil.container.impl.CbeffContainerImpl;
import io.mosip.kernel.core.cbeffutil.common.CbeffValidator;
import io.mosip.kernel.core.cbeffutil.entity.BIR;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.BIRType;
import io.mosip.kernel.core.util.HMACUtils;
import io.mosip.registration.packetmananger.constants.PacketManagerConstants;

@Component
public class PacketManagerHelper {
	
	public byte[] getXMLData(List<BIR> birs) throws Exception {
		byte[] xmlBytes = null;
		try(InputStream xsd = getClass().getClassLoader().getResourceAsStream(PacketManagerConstants.CBEFF_SCHEMA_FILE_PATH)) {
			CbeffContainerImpl cbeffContainer = new CbeffContainerImpl();
			BIRType bir = cbeffContainer.createBIRType(birs);
			xmlBytes = CbeffValidator.createXMLBytes(bir, IOUtils.toByteArray(xsd));
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
