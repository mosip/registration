package io.mosip.registration.processor.packet.storage.utils;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.mosip.kernel.biometrics.commons.CbeffValidator;
import io.mosip.kernel.biometrics.constant.BiometricFunction;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biosdk.provider.factory.BioAPIFactory;
import io.mosip.kernel.biosdk.provider.spi.iBioProviderApi;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.idrepo.dto.Documents;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;

@Component
public class BioSdkUtil {

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(BioSdkUtil.class);

	/** The utilities. */
	@Autowired
	Utilities utilities;

	/** The bio api factory. */
	@Autowired(required = false)
	private BioAPIFactory bioApiFactory;

	/** modality arrays that needs to be tagged */
	@Value("#{'${mosip.regproc.quality.classifier.tagging.quality.modalities}'.split(',')}")
	private List<String> modalities;

	public boolean authenticateBiometrics(String uin, String individualType,
			List<io.mosip.kernel.biometrics.entities.BIR> list) throws Exception {
		regProcLogger.info("BioSdkUtil :: authenticateBiometrics :: Fetching info from id repo based on uin ");
		boolean status = false;
		List<Documents> docs = utilities.retrieveIdrepoDocument(uin);
		String data = null;
		for (int i = 0; i < docs.size(); i++) {
			if (docs.get(i).getCategory().equalsIgnoreCase(MappingJsonConstants.INDIVIDUAL_BIOMETRICS)) {
				data = docs.get(i).getValue();
				break;
			}
		}
		if (null != data && !(data.isEmpty())) {
			BIR bir = CbeffValidator.getBIRFromXML(CryptoUtil.decodeBase64(data.toString()));
			for (int i = 0; i < modalities.size(); i++) {
				iBioProviderApi bioProvider = bioApiFactory.getBioProvider(
						BiometricType.valueOf(modalities.get(i).toUpperCase()), BiometricFunction.MATCH);
				status = bioProvider.verify(bir.getBirs(), list, BiometricType.valueOf(modalities.get(i).toUpperCase()),
						null);
				if (!status)
					break;
			}
		}

		return status;
	}

}
