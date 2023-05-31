package io.mosip.registration.processor.packet.storage.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import io.mosip.kernel.biometrics.commons.CbeffValidator;
import io.mosip.kernel.biometrics.constant.BiometricFunction;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biosdk.provider.factory.BioAPIFactory;
import io.mosip.kernel.biosdk.provider.spi.iBioProviderApi;
import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.registration.processor.core.code.RegistrationExceptionTypeCode;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.exception.ValidationFailedException;
import io.mosip.registration.processor.core.idrepo.dto.Documents;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;

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

	@Autowired
	RegistrationExceptionMapperUtil registrationExceptionMapperUtil;

	public void authenticateBiometrics(String uin, String individualType,
			List<io.mosip.kernel.biometrics.entities.BIR> list, InternalRegistrationStatusDto registrationStatusDto,
			String errorMsg, String errorCode) throws Exception {
		try {
			regProcLogger.debug("BioSdkUtil :: authenticateBiometrics :: Fetching info from id repo based on uin ");
			boolean status = false;
			List<Documents> docs = utilities.retrieveIdrepoDocument(uin);
			String data = null;
			if (null == docs || (null == list || list.size() == 0)) {
				registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
						.getStatusCode(RegistrationExceptionTypeCode.BIOMETRIC_EXTRACTION_FAILED));
				registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());

				throw new BaseCheckedException("No document found for given user");
			}

			for (int i = 0; i < docs.size(); i++) {
				if (docs.get(i).getCategory().equalsIgnoreCase(MappingJsonConstants.INDIVIDUAL_BIOMETRICS)) {
					data = docs.get(i).getValue();
					break;
				}
			}

			if (null != data && !(data.isEmpty())) {
				BIR bir = CbeffValidator.getBIRFromXML(CryptoUtil.decodeURLSafeBase64(data));
				Map<BiometricType, List<BIR>> firstMp = getMapFromBirList(bir.getBirs());
				Map<BiometricType, List<BIR>> secondMp = getMapFromBirList(list);
				regProcLogger
						.debug("BioSdkUtil :: authenticateBiometrics :: BIR size fetch from ID repo " + firstMp.size());
				regProcLogger
						.debug("BioSdkUtil :: authenticateBiometrics :: BIR size fetch from packet " + secondMp.size());
				for (Map.Entry<BiometricType, List<BIR>> entry : secondMp.entrySet()) {
					iBioProviderApi bioProvider = bioApiFactory
							.getBioProvider(BiometricType.valueOf(entry.getKey().toString()), BiometricFunction.MATCH);
					if (null == firstMp.get(entry.getKey())) {
						status = false;

					} else
						status = bioProvider.verify(secondMp.get(entry.getKey()),firstMp.get(entry.getKey()),
								entry.getKey(), null);
					if (!status) {
						registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
								.getStatusCode(RegistrationExceptionTypeCode.VALIDATION_FAILED_EXCEPTION));
						registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());

						throw new ValidationFailedException(errorMsg, errorCode);
					}
				}
			}
			regProcLogger.debug(
					"BioSdkUtil :: authenticateBiometrics :: Authentication of biometrics done with status " + status);

		} catch (RestClientException restEx) {
			registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
					.getStatusCode(RegistrationExceptionTypeCode.CONNECTION_UNAVAILABLE_EXCEPTION));
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());

			regProcLogger.debug(
					"BioSdkUtil :: authenticateBiometrics :: Issue whilevalidating biometrics" + restEx.getMessage());
			throw restEx;
		}
	}

	private Map<BiometricType, List<BIR>> getMapFromBirList(List<BIR> lst) {

		Map<BiometricType, List<BIR>> mp = new TreeMap<>();
		lst.stream().forEach(b -> {
			if (null != mp && mp.containsKey(b.getBdbInfo().getType().get(0))) {
				List<BIR> l = mp.get(b.getBdbInfo().getType().get(0));
				l.add(b);
			} else {
				List<BIR> lstBir = new ArrayList<>();
				lstBir.add(b);
				mp.put(b.getBdbInfo().getType().get(0), lstBir);

			}

		});

		return mp;

	}

}
