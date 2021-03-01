package io.mosip.registration.dao.impl;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.util.Objects;
import java.util.Optional;

import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.entity.CenterMachine;
import io.mosip.registration.entity.MachineMaster;
import io.mosip.registration.repositories.CenterMachineRepository;
import io.mosip.registration.repositories.MachineMasterRepository;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.dao.RegistrationCenterDAO;
import io.mosip.registration.dto.RegistrationCenterDetailDTO;
import io.mosip.registration.entity.RegistrationCenter;
import io.mosip.registration.repositories.RegistrationCenterRepository;

/**
 * The implementation class of {@link RegistrationCenterDAO}.
 *
 * @author Sravya Surampalli
 * @since 1.0.0
 */
@Repository
public class RegistrationCenterDAOImpl implements RegistrationCenterDAO {

	/**
	 * Instance of LOGGER
	 */
	private static final Logger LOGGER = AppConfig.getLogger(RegistrationCenterDAOImpl.class);

	/** The registrationCenter repository. */
	@Autowired
	private RegistrationCenterRepository registrationCenterRepository;

	@Autowired
	private MachineMasterRepository machineMasterRepository;

	@Autowired
	private CenterMachineRepository centerMachineRepository;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mosip.registration.dao.RegistrationCenterDAO#getRegistrationCenterDetails
	 * (java.lang.String)
	 */
	public RegistrationCenterDetailDTO getRegistrationCenterDetails(String centerId,String langCode) {

		LOGGER.info("REGISTRATION - CENTER_NAME - REGISTRATION_CENTER_DAO_IMPL", APPLICATION_NAME,
				APPLICATION_ID, "Fetching Registration Center details");

		Optional<RegistrationCenter> registrationCenter = registrationCenterRepository
				.findByIsActiveTrueAndRegistartionCenterIdIdAndRegistartionCenterIdLangCode(centerId,langCode);
		RegistrationCenterDetailDTO registrationCenterDetailDTO = new RegistrationCenterDetailDTO();
		if (registrationCenter.isPresent()) {
			registrationCenterDetailDTO
					.setRegistrationCenterId(registrationCenter.get().getRegistartionCenterId().getId());
			registrationCenterDetailDTO.setRegistrationCenterName(registrationCenter.get().getName());
			registrationCenterDetailDTO.setRegsitrationCenterTypeCode(registrationCenter.get().getCenterTypeCode());
			registrationCenterDetailDTO.setRegistrationCenterAddrLine1(registrationCenter.get().getAddressLine1());
			registrationCenterDetailDTO.setRegistrationCenterAddrLine2(registrationCenter.get().getAddressLine2());
			registrationCenterDetailDTO.setRegistrationCenterAddrLine3(registrationCenter.get().getAddressLine3());
			registrationCenterDetailDTO.setRegistrationCenterLatitude(registrationCenter.get().getLatitude());
			registrationCenterDetailDTO.setRegistrationCenterLongitude(registrationCenter.get().getLongitude());
			registrationCenterDetailDTO.setRegistrationCenterLocationCode(registrationCenter.get().getLocationCode());
			registrationCenterDetailDTO.setRegistrationCenterContactPhone(registrationCenter.get().getContactPhone());
			registrationCenterDetailDTO.setRegistrationCenterWorkingHours(registrationCenter.get().getWorkingHours());
			registrationCenterDetailDTO.setRegistrationCenterNumberOfKiosks(registrationCenter.get().getNumberOfKiosks());
			registrationCenterDetailDTO.setRegistrationCenterPerKioskProcessTime(registrationCenter.get().getPerKioskProcessTime());
			registrationCenterDetailDTO.setRegistrationCenterHolidayLocCode(registrationCenter.get().getHolidayLocationCode());
		}

		LOGGER.info("REGISTRATION - CENTER_NAME - REGISTRATION_CENTER_DAO_IMPL", APPLICATION_NAME,
				APPLICATION_ID, "Registration Center details fetched successfully");

		return registrationCenterDetailDTO;
	}

	@Override
	public boolean isMachineCenterActive(String machineId) {
		LOGGER.info("REGISTRATION - CENTER_NAME - REGISTRATION_CENTER_DAO_IMPL", APPLICATION_NAME,
				APPLICATION_ID, "checking Registration Center details");

		if(machineId == null) {
			String machineName = RegistrationSystemPropertiesChecker.getMachineId();
			MachineMaster machineMaster = machineMasterRepository.findByNameIgnoreCaseAndRegMachineSpecIdLangCode(machineName.toLowerCase(),
					ApplicationContext.applicationLanguage());

			if (machineMaster != null && machineMaster.getRegMachineSpecId().getId() != null) {
				machineId = machineMaster.getRegMachineSpecId().getId();
			}
		}

		CenterMachine centerMachine = centerMachineRepository.findByCenterMachineIdMachineId(machineId);
		if(centerMachine != null &&
				registrationCenterRepository.findByIsActiveTrueAndRegistartionCenterIdIdAndRegistartionCenterIdLangCode(
						centerMachine.getCenterMachineId().getRegCenterId(),
						ApplicationContext.applicationLanguage()).isPresent()) {
			return true;
		}
		return false;
	}

}
