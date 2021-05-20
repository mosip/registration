package io.mosip.registration.processor.status.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import io.mosip.registration.processor.core.workflow.dto.FilterInfo;
import io.mosip.registration.processor.core.workflow.dto.SortInfo;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.repositary.RegistrationRepositary;

/**
 * The Class SyncRegistrationDao.
 *
 * @author Girish Yarru
 */
@Component
public class SyncRegistrationDao {

	/** The registration sync status. */
	@Autowired
	RegistrationRepositary<SyncRegistrationEntity, String> syncRegistrationRepository;

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

	/** The Constant ISACTIVE_COLON. */
	public static final String ISACTIVE_COLON = ".isActive=:";

	/** The Constant ISDELETED_COLON. */
	public static final String ISDELETED_COLON = ".isDeleted=:";

	@Value("${registration.processor.max.registrationid:5}")
	private int maxResultFetch;

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
	public SyncRegistrationEntity findById(String registrationId) {
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
	 * @param registrationId the registration id
	 * @return the sync registration entity
	 */
	public boolean deleteAdditionalInfo(SyncRegistrationEntity syncEntity) {
		syncEntity.setOptionalValues(null);
		SyncRegistrationEntity updatedEntity = syncRegistrationRepository.update(syncEntity);

		return updatedEntity != null ? true : false;
	}

	public List<String> getSearchResults(List<FilterInfo> filters, List<SortInfo> sort) {
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
				sb.append(EMPTY_STRING + AND + EMPTY_STRING + alias + "." + filterInfo.getColumnName() + "=:"
						+ filterInfo.getColumnName());
				params.put(filterInfo.getColumnName(), filterInfo.getValue());
			}
		}
		if (!sort.isEmpty()) {
			sb.append(EMPTY_STRING + ORDER_BY + sort.get(0).getSortField() + EMPTY_STRING + sort.get(0).getSortType());
		}
		List<SyncRegistrationEntity> result = syncRegistrationRepository.createQuerySelect(sb.toString(), params,
				maxResultFetch);

		for (SyncRegistrationEntity syncRegEn : result) {
			registrationIdlist.add(syncRegEn.getRegistrationId());
		}
		return registrationIdlist;
	}

}