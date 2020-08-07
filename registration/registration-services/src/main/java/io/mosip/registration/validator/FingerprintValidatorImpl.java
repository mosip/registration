package io.mosip.registration.validator;

import static io.mosip.registration.constants.LoggerConstants.LOG_REG_FINGERPRINT_FACADE;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.mosip.kernel.core.cbeffutil.entity.BDBInfo;
import io.mosip.kernel.core.cbeffutil.entity.BIR;
import io.mosip.kernel.core.cbeffutil.entity.BIR.BIRBuilder;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.LoggerConstants;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.UserDetailDAO;
import io.mosip.registration.dto.AuthTokenDTO;
import io.mosip.registration.dto.AuthenticationValidatorDTO;
import io.mosip.registration.dto.biometric.FingerprintDetailsDTO;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.entity.UserBiometric;
import io.mosip.registration.service.security.impl.AuthenticationServiceImpl;

/**
 * This class is for validating Fingerprint Authentication
 * 
 * @author SaravanaKumar G
 *
 */
@Service("fingerprintValidator")
public class FingerprintValidatorImpl extends AuthenticationBaseValidator {

	@Autowired
	private UserDetailDAO userDetailDAO;

	/*
	 * @Autowired
	 * 
	 * @Qualifier("finger") IBioApi ibioApi;
	 */

	/**
	 * Instance of LOGGER
	 */
	private static final Logger LOGGER = AppConfig.getLogger(AuthenticationServiceImpl.class);

	/**
	 * Validate the Fingerprint with the AuthenticationValidatorDTO as input
	 */
	@Override
	public boolean validate(AuthenticationValidatorDTO authenticationValidatorDTO) {
		LOGGER.info(LoggerConstants.FINGER_PRINT_AUTHENTICATION, APPLICATION_NAME, APPLICATION_ID,
				"Validating Scanned Finger");
		/*if (ibioApi instanceof BioApiImpl) {
			ApplicationContext.map().put(RegistrationConstants.DEDUPLICATION_FINGERPRINT_ENABLE_FLAG,
					RegistrationConstants.DISABLE);
		}
		if (!authenticationValidatorDTO.isAuthValidationFlag()) {
			if ((String
					.valueOf(ApplicationContext.map().get(RegistrationConstants.DEDUPLICATION_FINGERPRINT_ENABLE_FLAG)))
							.equalsIgnoreCase(RegistrationConstants.DISABLE))
				return false;
		}

		if (RegistrationConstants.SINGLE.equals(authenticationValidatorDTO.getAuthValidationType())) {
			return validateOneToManyFP(authenticationValidatorDTO.getUserId(),
					authenticationValidatorDTO.getFingerPrintDetails().get(0));
		} else if (RegistrationConstants.MULTIPLE.equals(authenticationValidatorDTO.getAuthValidationType())) {
			return validateManyToManyFP(authenticationValidatorDTO.getUserId(),
					authenticationValidatorDTO.getFingerPrintDetails());
		}*/
		return false;
	}

	/**
	 * Validate one finger print values with all the fingerprints from the table.
	 * 
	 * @param userId
	 * @param capturedFingerPrintDto
	 * @return
	 */
	private boolean validateOneToManyFP(String userId, FingerprintDetailsDTO capturedFingerPrintDto) {
		List<UserBiometric> userFingerprintDetails = userDetailDAO.getUserSpecificBioDetails(userId,
				RegistrationConstants.FIN);
		List<FingerprintDetailsDTO> fingerList = new ArrayList<FingerprintDetailsDTO>();
		fingerList.add(capturedFingerPrintDto);
		return validateFpWithBioApi(fingerList, userFingerprintDetails);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.service.bio.BioService#validateFpWithBioApi(io.mosip.
	 * registration.dto.biometric.FingerprintDetailsDTO, java.util.List)
	 */
	private boolean validateFpWithBioApi(List<FingerprintDetailsDTO> capturedFingerPrintDto,
			List<UserBiometric> userFingerprintDetails) {
		boolean flag = true;

		/*BIR[] capturedBir = new BIR[capturedFingerPrintDto.size()];

		int i = 0;
		for (FingerprintDetailsDTO userBiometric : capturedFingerPrintDto) {
			capturedBir[i] = new BIRBuilder().withBdb(userBiometric.getFingerPrintISOImage())
					.withBdbInfo(
							new BDBInfo.BDBInfoBuilder().withType(Collections.singletonList(SingleType.FINGER)).build())
					.build();
			i++;
		}

		BIR[] registeredBir = new BIR[userFingerprintDetails.size()];
		Score[] scores = null;
		flag = false;
		i = 0;
		for (UserBiometric userBiometric : userFingerprintDetails) {
			registeredBir[i] = new BIRBuilder().withBdb(userBiometric.getBioIsoImage())
					.withBdbInfo(
							new BDBInfo.BDBInfoBuilder().withType(Collections.singletonList(SingleType.FINGER)).build())
					.build();
			i++;
		}
		try {
			CompositeScore compositeScore = ibioApi.compositeMatch(capturedBir, registeredBir, null);
			scores = compositeScore.getIndividualScores();
			int reqScore = 80;
			for (Score score : scores) {
				if (score.getScaleScore() >= reqScore) {
					flag = true;
					break;
				}
			}
		} catch (BiometricException exception) {
			LOGGER.error(LOG_REG_FINGERPRINT_FACADE, APPLICATION_NAME, APPLICATION_ID,
					String.format("Exception while validating the finger print with bio api: %s caused by %s",
							exception.getMessage(), exception.getCause()));
			return false;
		} catch (RuntimeException exception) {
			LOGGER.error(LOG_REG_FINGERPRINT_FACADE, APPLICATION_NAME, APPLICATION_ID,
					String.format("Exception while validating the finger print with bio api: %s caused by %s Runtime",
							exception.getMessage(), exception.getCause()));
			return false;
		}*/
		

		return flag;

	}

	/**
	 * Validate all the user input finger details with all the finger details form
	 * the DB.
	 * 
	 * @param capturedFingerPrintDetails
	 * @return
	 */
	private boolean validateManyToManyFP(String userId, List<FingerprintDetailsDTO> capturedFingerPrintDetails) {
		Boolean isMatchFound = false;
			isMatchFound = validateFpWithBioApi(capturedFingerPrintDetails,
					userDetailDAO.getUserSpecificBioDetails(userId, RegistrationConstants.FIN));
				SessionContext.map().put(RegistrationConstants.DUPLICATE_FINGER, "Duplicate found");
		return isMatchFound;

	}

	@Override
	public AuthTokenDTO validate(String userId, String otp, boolean haveToSaveAuthToken) {
		return null;
	}

	@Override
	public boolean bioMerticsValidator(List<BiometricsDto> listOfBiometrics) {

		List<UserBiometric> userDetailsRecorded = userDetailDAO
				.getUserSpecificBioDetails(SessionContext.userContext().getUserId(), RegistrationConstants.FIN);
		boolean flag = false;
		for (BiometricsDto biometricDTO : listOfBiometrics) {
			BIR capturedBir = new BIRBuilder().withBdb(biometricDTO.getAttributeISO())
					.withBdbInfo(
							new BDBInfo.BDBInfoBuilder().withType(Collections.singletonList(SingleType.FINGER)).build())
					.build();

			BIR[] registeredBir = new BIR[userDetailsRecorded.size()];
			int i = 0;
			for (UserBiometric userBiometric : userDetailsRecorded) {
				registeredBir[i] = new BIRBuilder().withBdb(userBiometric.getBioIsoImage()).withBdbInfo(
						new BDBInfo.BDBInfoBuilder().withType(Collections.singletonList(SingleType.FINGER)).build())
						.build();
				i++;
			}
			try {
				/*Response<MatchDecision[]> scores = ibioApi.match(capturedBir, registeredBir, null);
				System.out.println(scores);*/

			} catch (Exception exception) {
				LOGGER.error(LOG_REG_FINGERPRINT_FACADE, APPLICATION_NAME, APPLICATION_ID,
						String.format("Exception while validating the iris with bio api: %s caused by %s",
								exception.getMessage(), exception.getCause()));
				return false;

			}
		}
		return flag;

	}
}
