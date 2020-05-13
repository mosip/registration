package io.mosip.registration.packetmanager.util;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import io.mosip.kernel.cbeffutil.container.impl.CbeffContainerImpl;
import io.mosip.kernel.core.cbeffutil.common.CbeffValidator;
import io.mosip.kernel.core.cbeffutil.entity.BIR;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.BIRType;
import io.mosip.kernel.core.idobjectvalidator.constant.IdObjectValidatorSupportedOperations;
import io.mosip.kernel.core.idobjectvalidator.exception.IdObjectIOException;
import io.mosip.kernel.core.idobjectvalidator.exception.IdObjectValidationFailedException;
import io.mosip.kernel.core.idobjectvalidator.spi.IdObjectValidator;
import io.mosip.kernel.core.util.HMACUtils;
import io.mosip.registration.packetmananger.constants.PacketManagerConstants;

@Component
public class PacketManagerHelper {
	
	@Autowired
	@Qualifier("schema")
	private IdObjectValidator idObjectValidator;
	
	public boolean validateIdObject(String identitySchema, Object idObject, String operation, 
			List<String> fields) {
		try {
			Map<String, Object> identity = new HashMap<String, Object>();
			identity.put("identity", idObject);
			return idObjectValidator.validateIdObject(identity, getOperationType(operation, false));
		} catch (IdObjectValidationFailedException e) {
			e.printStackTrace();
		} catch (IdObjectIOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
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
	
	private IdObjectValidatorSupportedOperations getOperationType(String registrationCategory, 
			boolean isChildRegistration) {
		if (registrationCategory.equalsIgnoreCase(PacketManagerConstants.PACKET_TYPE_NEW)) {
			return IdObjectValidatorSupportedOperations.NEW_REGISTRATION;
		} else if (registrationCategory.equalsIgnoreCase(PacketManagerConstants.PACKET_TYPE_UPDATE)) {
			return IdObjectValidatorSupportedOperations.UPDATE_UIN;
		} else if (registrationCategory.equalsIgnoreCase(PacketManagerConstants.PACKET_TYPE_LOST)) {
			return IdObjectValidatorSupportedOperations.LOST_UIN;
		} else if (isChildRegistration) {
			return IdObjectValidatorSupportedOperations.CHILD_REGISTRATION;
		}
		return null;
	}

}
