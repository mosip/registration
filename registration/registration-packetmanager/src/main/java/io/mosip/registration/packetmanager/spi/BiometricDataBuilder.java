package io.mosip.registration.packetmanager.spi;

import io.mosip.kernel.core.cbeffutil.entity.BIR;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;

public interface BiometricDataBuilder {
	
	/**
	 * 
	 * @param bdb
	 * @param formatType
	 * @param qualityScore
	 * @param type
	 * @param subType
	 * @return
	 */
	public BIR buildBIR(byte[] bdb, double qualityScore, SingleType type, String subType);

}
