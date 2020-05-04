package io.mosip.registration.dao.impl;

import static io.mosip.registration.constants.LoggerConstants.LOG_REG_MASTER_SYNC;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dao.MasterSyncDao;
import io.mosip.registration.dto.mastersync.MasterDataResponseDto;
import io.mosip.registration.dto.response.SyncDataResponseDto;
import io.mosip.registration.entity.BiometricAttribute;
import io.mosip.registration.entity.BlacklistedWords;
import io.mosip.registration.entity.DocumentCategory;
import io.mosip.registration.entity.DocumentType;
import io.mosip.registration.entity.Gender;
import io.mosip.registration.entity.IndividualType;
import io.mosip.registration.entity.Language;
import io.mosip.registration.entity.Location;
import io.mosip.registration.entity.ReasonCategory;
import io.mosip.registration.entity.ReasonList;
import io.mosip.registration.entity.SyncControl;
import io.mosip.registration.entity.SyncJobDef;
import io.mosip.registration.entity.ValidDocument;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.repositories.BiometricAttributeRepository;
import io.mosip.registration.repositories.BlacklistedWordsRepository;
import io.mosip.registration.repositories.DocumentCategoryRepository;
import io.mosip.registration.repositories.DocumentTypeRepository;
import io.mosip.registration.repositories.GenderRepository;
import io.mosip.registration.repositories.IndividualTypeRepository;
import io.mosip.registration.repositories.LanguageRepository;
import io.mosip.registration.repositories.LocationRepository;
import io.mosip.registration.repositories.ReasonCategoryRepository;
import io.mosip.registration.repositories.ReasonListRepository;
import io.mosip.registration.repositories.SyncJobControlRepository;
import io.mosip.registration.repositories.SyncJobDefRepository;
import io.mosip.registration.repositories.ValidDocumentRepository;
import io.mosip.registration.util.mastersync.ClientSettingSyncHelper;

/**
 * The implementation class of {@link MasterSyncDao}
 * 
 * @author Sreekar Chukka
 *
 * @since 1.0.0
 */
@Repository
@Transactional
public class MasterSyncDaoImpl implements MasterSyncDao {

	/** Object for Sync Status Repository. */
	@Autowired
	private SyncJobControlRepository syncStatusRepository;

	/** Object for Sync Biometric Attribute Repository. */
	@Autowired
	private BiometricAttributeRepository biometricAttributeRepository;


	/** Object for Sync Blacklisted Words Repository. */
	@Autowired
	private BlacklistedWordsRepository blacklistedWordsRepository;


	/** Object for Sync Document Category Repository. */
	@Autowired
	private DocumentCategoryRepository documentCategoryRepository;

	/** Object for Sync Document Type Repository. */
	@Autowired
	private DocumentTypeRepository documentTypeRepository;

	/** Object for Sync Gender Type Repository. */
	@Autowired
	private GenderRepository genderRepository;

	/** Object for Sync Location Repository. */
	@Autowired
	private LocationRepository locationRepository;
	
	/** Object for Sync Reason Category Repository. */
	@Autowired
	private ReasonCategoryRepository reasonCategoryRepository;

	/** Object for Sync Reason List Repository. */
	@Autowired
	private ReasonListRepository reasonListRepository;

	/** Object for Sync Valid Document Repository. */
	@Autowired
	private ValidDocumentRepository validDocumentRepository;

	/** Object for Sync language Repository. */
	@Autowired
	private LanguageRepository languageRepository;

	/** Object for Sync Individual type Repository. */
	@Autowired
	private IndividualTypeRepository individualTypeRepository;

	/** Object for Sync screen auth Repository. */
	@Autowired
	private SyncJobDefRepository syncJobDefRepository;
	
	@Autowired
	private ClientSettingSyncHelper clientSettingSyncHelper;





	/**
	 * logger for logging
	 */
	private static final Logger LOGGER = AppConfig.getLogger(MasterSyncDaoImpl.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.dao.MasterSyncDao#getMasterSyncStatus()
	 */
	@Override
	public SyncControl syncJobDetails(String synccontrol) {

		SyncControl syncControlResonse = null;

		LOGGER.info(RegistrationConstants.MASTER_SYNC_JOD_DETAILS, APPLICATION_NAME, APPLICATION_ID,
				"DAO findByID method started");

		try {
			// find the user
			syncControlResonse = syncStatusRepository.findBySyncJobId(synccontrol);

		} catch (RuntimeException runtimeException) {
			LOGGER.error(RegistrationConstants.MASTER_SYNC_JOD_DETAILS, APPLICATION_NAME, APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
			throw new RegBaseUncheckedException(RegistrationConstants.MASTER_SYNC_JOD_DETAILS,
					runtimeException.getMessage());
		}

		LOGGER.info(RegistrationConstants.MASTER_SYNC_JOD_DETAILS, APPLICATION_NAME, APPLICATION_ID,
				"DAO findByID method ended");

		return syncControlResonse;
	}

	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.dao.MasterSyncDao#findLocationByLangCode(java.lang.
	 * String, java.lang.String)
	 */
	@Override
	public List<Location> findLocationByLangCode(String hierarchyCode, String langCode) {
		return locationRepository.findByIsActiveTrueAndHierarchyNameAndLangCode(hierarchyCode, langCode);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.dao.MasterSyncDao#findLocationByParentLocCode(java.lang
	 * .String)
	 */
	@Override
	public List<Location> findLocationByParentLocCode(String parentLocCode, String langCode) {
		return locationRepository.findByIsActiveTrueAndParentLocCodeAndLangCode(parentLocCode, langCode);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.dao.MasterSyncDao#getAllReasonCatogery()
	 */
	@Override
	public List<ReasonCategory> getAllReasonCatogery(String langCode) {
		return reasonCategoryRepository.findByIsActiveTrueAndLangCode(langCode);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.dao.MasterSyncDao#getReasonList(java.util.List)
	 */
	@Override
	public List<ReasonList> getReasonList(String langCode, List<String> reasonCat) {
		return reasonListRepository.findByIsActiveTrueAndLangCodeAndReasonCategoryCodeIn(langCode, reasonCat);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.dao.MasterSyncDao#getBlackListedWords(java.lang.String)
	 */
	@Override
	public List<BlacklistedWords> getBlackListedWords(String langCode) {
		return blacklistedWordsRepository.findBlackListedWordsByIsActiveTrueAndLangCode(langCode);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.dao.MasterSyncDao#getDocumentCategories(java.lang.
	 * String)
	 */
	@Override
	public List<DocumentType> getDocumentTypes(List<String> docCode, String langCode) {
		return documentTypeRepository.findByIsActiveTrueAndLangCodeAndCodeIn(langCode, docCode);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.dao.MasterSyncDao#getGenderDtls(java.lang.String)
	 */
	@Override
	public List<Gender> getGenderDtls(String langCode) {

		return genderRepository.findByIsActiveTrueAndLangCode(langCode);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.dao.MasterSyncDao#getValidDocumets(java.lang.String)
	 */
	@Override
	public List<ValidDocument> getValidDocumets(String docCategoryCode) {
		return validDocumentRepository.findByIsActiveTrueAndDocCategoryCode(docCategoryCode);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.dao.MasterSyncDao#getIndividulType(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public List<IndividualType> getIndividulType(String code, String langCode) {
		return individualTypeRepository.findByIndividualTypeIdCodeAndIndividualTypeIdLangCodeAndIsActiveTrue(code,
				langCode);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.dao.MasterSyncDao#getIndividulType(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public List<IndividualType> getIndividulType(String langCode) {
		return individualTypeRepository.findByIndividualTypeIdLangCodeAndIsActiveTrue(
				langCode);
	}
	
	

	public List<SyncJobDef> getSyncJobs() {
		return syncJobDefRepository.findAllByIsActiveTrue();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.dao.MasterSyncDao#getBiometricType(java.lang.
	 * String, java.lang.String)
	 */
	@Override
	public List<BiometricAttribute> getBiometricType(String langCode, List<String> biometricType) {
		return biometricAttributeRepository.findByLangCodeAndBiometricTypeCodeIn(langCode, biometricType);
	}

	public List<Language> getActiveLanguages() {

		return languageRepository.findAllByIsActiveTrue();
	}

	public List<Gender> getGenders() {
		return genderRepository.findAllByIsActiveTrue();
	}

	public List<DocumentCategory> getDocumentCategory() {
		return documentCategoryRepository.findAllByIsActiveTrue();
	}

	public List<Location> getLocationDetails() {
		return locationRepository.findAllByIsActiveTrue();
	}

	/**
	 * All the master data such as Location, gender,Registration center, Document types,category etc., 
	 * will be saved in the DB(These details will be getting from the MasterSync service)
	 *
	 * @param masterSyncDto
	 *            All the master details will be available in the {@link MasterDataResponseDto}
	 * @return the string
	 * 			- Returns the Success or Error response
	 */
	
	@Override
	public String saveSyncData(SyncDataResponseDto syncDataResponseDto) {
		String syncStatusMessage = null;
		try {
			syncStatusMessage = clientSettingSyncHelper.saveClientSettings(syncDataResponseDto);
			return syncStatusMessage;
		} catch (Exception runtimeException) {			
			LOGGER.error(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));			
			syncStatusMessage = runtimeException.getMessage();
		}
		throw new RegBaseUncheckedException(RegistrationConstants.MASTER_SYNC_EXCEPTION, syncStatusMessage);
	}

}
