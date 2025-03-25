package io.mosip.registration.processor.status.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import io.mosip.registration.processor.core.workflow.dto.SortInfo;
import io.mosip.registration.processor.status.dto.FilterInfo;
import io.mosip.registration.processor.status.entity.SaltEntity;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.repositary.SaltRepository;
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

	@Autowired
	SaltRepository saltRepository;

	/** The Constant AND. */
	public static final String AND = "AND";

	/** The Constant EMPTY_STRING. */
	public static final String EMPTY_STRING = " ";

	public static final String SELECT = "SELECT ";

	/** The Constant SELECT_DISTINCT. */
	public static final String SELECT_DISTINCT = "SELECT DISTINCT ";

	/** The Constant FROM. */
	public static final String FROM = " FROM  ";

	/** The Constant WHERE. */
	public static final String WHERE = " WHERE ";

	public static final String ORDER_BY = "order by ";

	/** The Constant ISACTIVE. */
	public static final String ISACTIVE = "isActive";

	/** The Constant ISDELETED. */
	public static final String ISDELETED = "isDeleted";

	public static final String BETWEEN = "BETWEEN ";

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

		return syncRegistrationRepository.findByRegistrationId(registrationId);
	}


	public SyncRegistrationEntity findByWorkflowInstanceId(String workflowInstanceId) {
		List<SyncRegistrationEntity> syncRegistrationEntityList = syncRegistrationRepository.findByworkflowInstanceId(workflowInstanceId);

		return !CollectionUtils.isEmpty(syncRegistrationEntityList) ? syncRegistrationEntityList.iterator().next() : null;
	}

	public SyncRegistrationEntity findByRegistrationIdIdAndAdditionalInfoReqId(String registrationId, String additionalInfoReqId) {
		

		List<SyncRegistrationEntity> syncRegistrationEntityList = syncRegistrationRepository.findByRegistrationIdIdANDAdditionalInfoReqId(registrationId,
				additionalInfoReqId);

		return !CollectionUtils.isEmpty(syncRegistrationEntityList) ? syncRegistrationEntityList.get(0) : null;

	}

	public SyncRegistrationEntity findByRegistrationIdIdAndRegType(String registrationId, String registrationType) {
		List<SyncRegistrationEntity> syncRegistrationEntityList = syncRegistrationRepository.findByRegistrationIdIdANDRegType(registrationId,
				registrationType);

		return !CollectionUtils.isEmpty(syncRegistrationEntityList) ? syncRegistrationEntityList.get(0) : null;

	}

	public SyncRegistrationEntity findByRegistrationIdAndRegTypeAndAdditionalInfoReqId(String registrationId, String registrationType, String additionalInfoReqId) {
		List<SyncRegistrationEntity> syncRegistrationEntityList = syncRegistrationRepository.findByRegistrationIdAndRegTypeAndAdditionalInfoReqId(registrationId,
				registrationType, additionalInfoReqId);

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

		return syncRegistrationRepository.findByRegistrationIds(ids);
	}

	/**
	 * Delete additionalInfo from table based on ID.
	 *
	 * @param syncEntity the sync registration entity from registration_list table.
	 * @return the sync registration entity
	 */
	public boolean deleteAdditionalInfo(SyncRegistrationEntity syncEntity) {
		syncEntity.setOptionalValues(null);
		SyncRegistrationEntity updatedEntity = syncRegistrationRepository.update(syncEntity);

		return updatedEntity != null ? true : false;
	}

	public SyncRegistrationEntity findByPacketId(String packetId) {
		List<SyncRegistrationEntity> syncRegistrationEntityList = syncRegistrationRepository.findByPacketId(packetId);

		return !CollectionUtils.isEmpty(syncRegistrationEntityList) ? syncRegistrationEntityList.get(0) : null;
	}

	public List<SyncRegistrationEntity> findByAdditionalInfoReqId(String additionalInfoReqId) {
		return syncRegistrationRepository.findByAdditionalInfoReqId(additionalInfoReqId);
	}

	public List<SyncRegistrationEntity> getByPacketIds(List<String> packetIds) {
		return syncRegistrationRepository.findByPacketIds(packetIds);
	}

	public List<SyncRegistrationEntity> getSearchResults(List<FilterInfo> filters, List<SortInfo> sort) {
		Map<String, Object> params = new HashMap<>();
		List<String> registrationIdlist = new ArrayList<String>();
		String className = SyncRegistrationEntity.class.getSimpleName();
		String queryStr=null;
		String alias = SyncRegistrationEntity.class.getName().toLowerCase().substring(0, 1);
		params.put(ISDELETED, Boolean.FALSE);
		queryStr = SELECT + alias + FROM + className + EMPTY_STRING + alias + WHERE + alias + ISDELETED_COLON
				+ ISDELETED;
		StringBuilder sb = new StringBuilder(queryStr);
		if (!filters.isEmpty()) {
			Iterator<FilterInfo> searchIterator = filters.iterator();
			while (searchIterator.hasNext()) {
				FilterInfo filterInfo = searchIterator.next();
				if (filterInfo.getType().equalsIgnoreCase("between")) {
					sb.append(EMPTY_STRING + AND + EMPTY_STRING + alias + "." + filterInfo.getColumnName()
							+ EMPTY_STRING + BETWEEN + "'" + filterInfo.getFromValue() + "'" + EMPTY_STRING + AND
							+ EMPTY_STRING + "'" + filterInfo.getToValue() + "'");

				} else {
					sb.append(EMPTY_STRING + AND + EMPTY_STRING + alias + "." + filterInfo.getColumnName() + "=:"
							+ filterInfo.getColumnName());
					params.put(filterInfo.getColumnName(), filterInfo.getValue());
				}
			}
		}
		if (!sort.isEmpty()) {
			sb.append(EMPTY_STRING + ORDER_BY + sort.get(0).getSortField() + EMPTY_STRING + sort.get(0).getSortType());
		}
		List<SyncRegistrationEntity> result = syncRegistrationRepository.createQuerySelect(sb.toString(), params);

		return result;
	}

	/**
	 *
	 * @param id
	 * @return
	 */
	public String getSaltValue(Long id) {
		SaltEntity saltEntity = saltRepository.findSaltById(id);
		return saltEntity.getSalt();
	}

}