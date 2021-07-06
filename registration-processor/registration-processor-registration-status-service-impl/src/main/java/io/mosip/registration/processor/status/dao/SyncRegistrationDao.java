package io.mosip.registration.processor.status.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.repositary.SyncRegistrationRepository;

/**
 * The Class SyncRegistrationDao.
 *
 * @author Girish Yarru
 */
@Component
public class SyncRegistrationDao {

	/** The registration sync status. */
	@Autowired
	SyncRegistrationRepository<SyncRegistrationEntity, String> syncRegistrationRepository;

	/** The Constant AND. */
	public static final String AND = "AND";

	/** The Constant EMPTY_STRING. */
	public static final String EMPTY_STRING = " ";

	/** The Constant SELECT_DISTINCT. */
	public static final String SELECT_DISTINCT = "SELECT DISTINCT ";

	/** The Constant FROM. */
	public static final String FROM = " FROM  ";

	/** The Constant WHERE. */
	public static final String WHERE = " WHERE ";

	/** The Constant ISACTIVE. */
	public static final String ISACTIVE = "isActive";

	/** The Constant ISDELETED. */
	public static final String ISDELETED = "isDeleted";

	/** The Constant ISACTIVE_COLON. */
	public static final String ISACTIVE_COLON = ".isActive=:";

	/** The Constant ISDELETED_COLON. */
	public static final String ISDELETED_COLON = ".isDeleted=:";

	/**
	 * Save.
	 *
	 * @param syncRegistrationEntity
	 *            the sync registration entity
	 * @return the sync registration entity
	 */
	public SyncRegistrationEntity save(SyncRegistrationEntity syncRegistrationEntity) {

		return syncRegistrationRepository.save(syncRegistrationEntity);
	}

	/**
	 * Update.
	 *
	 * @param syncRegistrationEntity
	 *            the sync registration entity
	 * @return the sync registration entity
	 */
	public SyncRegistrationEntity update(SyncRegistrationEntity syncRegistrationEntity) {

		return syncRegistrationRepository.save(syncRegistrationEntity);
	}

	/**
	 * Find by id.
	 *
	 * @param registrationId
	 *            the registration id
	 * @return the sync registration entity
	 */
	public List<SyncRegistrationEntity> findById(String registrationId) {
		Map<String, Object> params = new HashMap<>();
		String className = SyncRegistrationEntity.class.getSimpleName();

		String alias = SyncRegistrationEntity.class.getName().toLowerCase().substring(0, 1);

		String queryStr = SELECT_DISTINCT + alias + FROM + className + EMPTY_STRING + alias + WHERE + alias
				+ ".registrationId=:registrationId" + EMPTY_STRING + AND + EMPTY_STRING + alias + ISDELETED_COLON
				+ ISDELETED;

		params.put("registrationId", registrationId);
		params.put(ISDELETED, Boolean.FALSE);

		List<SyncRegistrationEntity> syncRegistrationEntityList = syncRegistrationRepository.createQuerySelect(queryStr,
				params);

		return syncRegistrationEntityList;
	}


	public SyncRegistrationEntity findByWorkflowInstanceId(String workflowInstanceId) {
		Map<String, Object> params = new HashMap<>();
		String className = SyncRegistrationEntity.class.getSimpleName();

		String alias = SyncRegistrationEntity.class.getName().toLowerCase().substring(0, 1);

		String queryStr = SELECT_DISTINCT + alias + FROM + className + EMPTY_STRING + alias + WHERE + alias
				+ ".id.workflowInstanceId=:workflowInstanceId" + EMPTY_STRING + AND + EMPTY_STRING + alias + ISDELETED_COLON
				+ ISDELETED;

		params.put("workflowInstanceId", workflowInstanceId);
		params.put(ISDELETED, Boolean.FALSE);

		List<SyncRegistrationEntity> syncRegistrationEntityList = syncRegistrationRepository.createQuerySelect(queryStr,
				params);

		return !CollectionUtils.isEmpty(syncRegistrationEntityList) ? syncRegistrationEntityList.iterator().next() : null;
	}

	public SyncRegistrationEntity findByRegistrationIdIdAndAdditionalInfoReqId(String registrationId, String additionalInfoReqId) {
		Map<String, Object> params = new HashMap<>();
		String className = SyncRegistrationEntity.class.getSimpleName();

		String alias = SyncRegistrationEntity.class.getName().toLowerCase().substring(0, 1);

		String queryStr = SELECT_DISTINCT + alias + FROM + className + EMPTY_STRING + alias + WHERE + alias
				+ ".registrationId=:registrationId" + EMPTY_STRING + AND + EMPTY_STRING + alias +".additionalInfoReqId=:additionalInfoReqId"+ EMPTY_STRING + AND + EMPTY_STRING + alias + ISDELETED_COLON
				+ ISDELETED;

		params.put("registrationId", registrationId);
		params.put("additionalInfoReqId", additionalInfoReqId);
		params.put(ISDELETED, Boolean.FALSE);

		List<SyncRegistrationEntity> syncRegistrationEntityList = syncRegistrationRepository.createQuerySelect(queryStr,
				params);

		return !CollectionUtils.isEmpty(syncRegistrationEntityList) ? syncRegistrationEntityList.get(0) : null;

	}
	
	public SyncRegistrationEntity findByRegistrationIdIdAndRegType(String registrationId, String registrationType) {
		Map<String, Object> params = new HashMap<>();
		String className = SyncRegistrationEntity.class.getSimpleName();

		String alias = SyncRegistrationEntity.class.getName().toLowerCase().substring(0, 1);

		String queryStr = SELECT_DISTINCT + alias + FROM + className + EMPTY_STRING + alias + WHERE + alias
				+ ".registrationId=:registrationId" + EMPTY_STRING + AND + EMPTY_STRING + alias +".registrationType=:registrationType"+ EMPTY_STRING + AND + EMPTY_STRING + alias + ISDELETED_COLON
				+ ISDELETED;

		params.put("registrationId", registrationId);
		params.put("registrationType", registrationType);
		params.put(ISDELETED, Boolean.FALSE);

		List<SyncRegistrationEntity> syncRegistrationEntityList = syncRegistrationRepository.createQuerySelect(queryStr,
				params);

		return !CollectionUtils.isEmpty(syncRegistrationEntityList) ? syncRegistrationEntityList.get(0) : null;

	}
	
	/**
	 * Gets the by ids.
	 *
	 * @param ids
	 *            the ids
	 * @return the by ids
	 */
	public List<SyncRegistrationEntity> getByIds(List<String> ids) {

		Map<String, Object> params = new HashMap<>();
		String className = SyncRegistrationEntity.class.getSimpleName();

		String alias = SyncRegistrationEntity.class.getName().toLowerCase().substring(0, 1);
		String queryStr = SELECT_DISTINCT + alias + FROM + className + EMPTY_STRING + alias + WHERE + alias
				+ ".registrationId IN :ids" + EMPTY_STRING + AND + EMPTY_STRING + alias + ISDELETED_COLON + ISDELETED;
		params.put("ids", ids);
		params.put(ISDELETED, Boolean.FALSE);

		return syncRegistrationRepository.createQuerySelect(queryStr, params);
	}

	/**
	 * Delete additionalInfo from table based on ID.
	 *
	 * @param syncEntity the registration id
	 * @return the sync registration entity
	 */
	public boolean deleteAdditionalInfo(SyncRegistrationEntity syncEntity) {
		syncEntity.setOptionalValues(null);
		SyncRegistrationEntity updatedEntity = syncRegistrationRepository.update(syncEntity);

		return updatedEntity != null ? true : false;
	}

	public SyncRegistrationEntity findByPacketId(String packetId) {
		Map<String, Object> params = new HashMap<>();
		String className = SyncRegistrationEntity.class.getSimpleName();

		String alias = SyncRegistrationEntity.class.getName().toLowerCase().substring(0, 1);

		String queryStr = SELECT_DISTINCT + alias + FROM + className + EMPTY_STRING + alias + WHERE + alias
				+ ".packetId=:packetId" + EMPTY_STRING + AND + EMPTY_STRING + alias + ISDELETED_COLON
				+ ISDELETED;

		params.put("packetId", packetId);
		params.put(ISDELETED, Boolean.FALSE);

		List<SyncRegistrationEntity> syncRegistrationEntityList = syncRegistrationRepository.createQuerySelect(queryStr,
				params);

		return !CollectionUtils.isEmpty(syncRegistrationEntityList) ? syncRegistrationEntityList.get(0) : null;
	}

	public List<SyncRegistrationEntity> findByAdditionalInfoReqId(String additionalInfoReqId) {
		Map<String, Object> params = new HashMap<>();
		String className = SyncRegistrationEntity.class.getSimpleName();

		String alias = SyncRegistrationEntity.class.getName().toLowerCase().substring(0, 1);

		String queryStr = SELECT_DISTINCT + alias + FROM + className + EMPTY_STRING + alias + WHERE + alias
				+ ".additionalInfoReqId=:additionalInfoReqId" + EMPTY_STRING + AND + EMPTY_STRING + alias + ISDELETED_COLON
				+ ISDELETED;

		params.put("additionalInfoReqId", additionalInfoReqId);
		params.put(ISDELETED, Boolean.FALSE);

		List<SyncRegistrationEntity> syncRegistrationEntityList = syncRegistrationRepository.createQuerySelect(queryStr,
				params);

		return syncRegistrationEntityList;
	}
}