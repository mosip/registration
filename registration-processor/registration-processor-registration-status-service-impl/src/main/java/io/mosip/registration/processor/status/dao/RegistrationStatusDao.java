package io.mosip.registration.processor.status.dao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import io.mosip.registration.processor.core.workflow.dto.FilterInfo;
import io.mosip.registration.processor.core.workflow.dto.PaginationInfo;
import io.mosip.registration.processor.core.workflow.dto.SortInfo;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.entity.RegistrationStatusEntity;
import io.mosip.registration.processor.status.repositary.RegistrationRepositary;

/**
 * The Class RegistrationStatusDao.
 *
 * @author Shashank Agrawal
 * @author Jyoti Prakash Nayak
 */
@Component
public class RegistrationStatusDao {

	/** The registration status repositary. */
	@Autowired
	RegistrationRepositary<RegistrationStatusEntity, String> registrationStatusRepositary;

	/** The Constant AND. */
	public static final String AND = "AND";

	/** The Constant EMPTY_STRING. */
	public static final String EMPTY_STRING = " ";

	/** The Constant SELECT_DISTINCT. */
	public static final String SELECT_DISTINCT = "SELECT DISTINCT ";

	/** The Constant SELECT_DISTINCT. */
	public static final String SELECT = "SELECT ";

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

	public static final String SELECT_COUNT = "SELECT COUNT(*)";

	public static final String ORDER_BY = "order by ";

	public static final String CREATED_DATE_TIME = "createDateTime";

	public static final String UPDATED_DATE_TIME = "updateDateTime";

	/**
	 * Save.
	 *
	 * @param registrationStatusEntity
	 *            the registration status entity
	 * @return the registration status entity
	 */
	public RegistrationStatusEntity save(RegistrationStatusEntity registrationStatusEntity) {

		return registrationStatusRepositary.save(registrationStatusEntity);
	}

	/**
	 * Update.
	 *
	 * @param registrationStatusEntity
	 *            the registration status entity
	 * @return the registration status entity
	 */
	public RegistrationStatusEntity update(RegistrationStatusEntity registrationStatusEntity) {

		return registrationStatusRepositary.save(registrationStatusEntity);
	}

	/**
	 * Find by id.
	 *
	 * @param rid
	 *            the enrolment id
	 * @return the registration status entity
	 */
	public RegistrationStatusEntity find(String rid, String process, Integer iteration, String workflowInstanceId) {
		Map<String, Object> params = new HashMap<>();
		String className = RegistrationStatusEntity.class.getSimpleName();

		String alias = RegistrationStatusEntity.class.getName().toLowerCase().substring(0, 1);
		String queryStr = null;
		if (workflowInstanceId != null) {
			queryStr = SELECT_DISTINCT + alias + FROM + className + EMPTY_STRING + alias + WHERE + alias
					+ ".id.workflowInstanceId=:workflowInstanceId"
					+ EMPTY_STRING + AND + EMPTY_STRING + alias + ISACTIVE_COLON + ISACTIVE
					+ EMPTY_STRING + AND + EMPTY_STRING + alias + ISDELETED_COLON + ISDELETED;
			params.put("workflowInstanceId", workflowInstanceId);
		} else {
			params.put("registrationId", rid);
			queryStr = SELECT_DISTINCT + alias + FROM + className + EMPTY_STRING + alias + WHERE + alias
					+ ".regId=:registrationId" + EMPTY_STRING + AND + EMPTY_STRING + alias + ISACTIVE_COLON + ISACTIVE
					+ EMPTY_STRING + AND + EMPTY_STRING + alias + ISDELETED_COLON + ISDELETED;
		}

		params.put(ISACTIVE, Boolean.TRUE);
		params.put(ISDELETED, Boolean.FALSE);

		List<RegistrationStatusEntity> registrationStatusEntityList = registrationStatusRepositary
				.createQuerySelect(queryStr, params);

		return !registrationStatusEntityList.isEmpty() ? registrationStatusEntityList.get(0) : null;
	}

	public List<RegistrationStatusEntity> findAll(String rid) {
		Map<String, Object> params = new HashMap<>();
		String className = RegistrationStatusEntity.class.getSimpleName();

		String alias = RegistrationStatusEntity.class.getName().toLowerCase().substring(0, 1);

		String queryStr = SELECT_DISTINCT + alias + FROM + className + EMPTY_STRING + alias + WHERE + alias
				+ ".regId=:registrationId" + EMPTY_STRING + AND + EMPTY_STRING + alias + ISACTIVE_COLON + ISACTIVE
				+ EMPTY_STRING + AND + EMPTY_STRING + alias + ISDELETED_COLON + ISDELETED;

		params.put("registrationId", rid);
		params.put(ISACTIVE, Boolean.TRUE);
		params.put(ISDELETED, Boolean.FALSE);

		List<RegistrationStatusEntity> registrationStatusEntityList = registrationStatusRepositary
				.createQuerySelect(queryStr, params);

		return registrationStatusEntityList;
	}

	/**
	 * Gets the enrolment status by status code.
	 *
	 * @param status
	 *            the status
	 * @return the enrolment status by status code
	 */
	public List<RegistrationStatusEntity> getEnrolmentStatusByStatusCode(String status) {

		Map<String, Object> params = new HashMap<>();
		String className = RegistrationStatusEntity.class.getSimpleName();

		String alias = RegistrationStatusEntity.class.getName().toLowerCase().substring(0, 1);

		String queryStr = SELECT_DISTINCT + alias + FROM + className + EMPTY_STRING + alias + WHERE + alias
				+ ".statusCode=:statusCode" + EMPTY_STRING + AND + EMPTY_STRING + alias + ISACTIVE_COLON + ISACTIVE
				+ EMPTY_STRING + AND + EMPTY_STRING + alias + ISDELETED_COLON + ISDELETED;

		params.put("statusCode", status);
		params.put(ISACTIVE, Boolean.TRUE);
		params.put(ISDELETED, Boolean.FALSE);
		return registrationStatusRepositary.createQuerySelect(queryStr, params);
	}

	public Page<RegistrationStatusEntity> getPagedSearchResults(List<FilterInfo> filters, SortInfo sort,
			PaginationInfo pagination) {
		Map<String, Object> params = new HashMap<>();
		String className = RegistrationStatusEntity.class.getSimpleName();
		String queryStr=null;
		long rows = 0;
		String alias = RegistrationStatusEntity.class.getName().toLowerCase().substring(0, 1);
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
		if (sort != null) {
			sb.append(EMPTY_STRING + ORDER_BY + sort.getSortField() + EMPTY_STRING + sort.getSortType());
		}
		List<RegistrationStatusEntity> result = registrationStatusRepositary.createQuerySelect(sb.toString(), params,
			pagination.getPageFetch());

		rows = registrationStatusRepositary.count();
		return new PageImpl<>(result,
				PageRequest.of(pagination.getPageStart(), pagination.getPageFetch()),
				rows);

	}

	/**
	 * Gets the by ids.
	 *
	 * @param ids
	 *            the ids
	 * @return the by ids
	 */
	public List<RegistrationStatusEntity> getByIds(List<String> ids) {

		Map<String, Object> params = new HashMap<>();
		String className = RegistrationStatusEntity.class.getSimpleName();

		String alias = RegistrationStatusEntity.class.getName().toLowerCase().substring(0, 1);
		String queryStr = SELECT_DISTINCT + alias + FROM + className + EMPTY_STRING + alias + WHERE + alias
				+ ".id IN :ids" + EMPTY_STRING + AND + EMPTY_STRING + alias + ISACTIVE_COLON + ISACTIVE + EMPTY_STRING
				+ AND + EMPTY_STRING + alias + ISDELETED_COLON + ISDELETED;
		params.put("ids", ids);
		params.put(ISACTIVE, Boolean.TRUE);
		params.put(ISDELETED, Boolean.FALSE);

		return registrationStatusRepositary.createQuerySelect(queryStr, params);
	}

	/**
	 * Gets the un processed packets.
	 *
	 * @param fetchSize
	 *            the fetch size
	 * @param elapseTime
	 *            the elapse time
	 * @param reprocessCount
	 *            the reprocess count
	 * @param status
	 *            the status
	 * @return the un processed packets
	 */
	public List<RegistrationStatusEntity> getUnProcessedPackets(Integer fetchSize, long elapseTime,
			Integer reprocessCount, List<String> status) {

		Map<String, Object> params = new HashMap<>();
		String className = RegistrationStatusEntity.class.getSimpleName();
		String alias = RegistrationStatusEntity.class.getName().toLowerCase().substring(0, 1);
		LocalDateTime timeDifference = LocalDateTime.now().minusSeconds(elapseTime);
		List<String> statusCodes=new ArrayList<>();
		statusCodes.add(RegistrationStatusCode.PAUSED.toString());
		statusCodes.add(RegistrationStatusCode.RESUMABLE.toString());
		statusCodes.add(RegistrationStatusCode.PAUSED_FOR_ADDITIONAL_INFO.toString());
		statusCodes.add(RegistrationStatusCode.REJECTED.toString());
		statusCodes.add(RegistrationStatusCode.FAILED.toString());
		statusCodes.add(RegistrationStatusCode.PROCESSED.toString());
		String queryStr = SELECT_DISTINCT + alias + FROM + className + EMPTY_STRING + alias + WHERE + alias
				+ ".latestTransactionStatusCode IN :status" + EMPTY_STRING + AND + EMPTY_STRING + alias
				+ ".regProcessRetryCount<=" + ":reprocessCount" + EMPTY_STRING + AND + EMPTY_STRING + alias
				+ ".latestTransactionTimes<" + ":timeDifference"+ EMPTY_STRING + AND + EMPTY_STRING+ alias
				+ ".statusCode  NOT IN :statusCode ";

		params.put("status", status);
		params.put("statusCode", statusCodes);
		params.put("reprocessCount", reprocessCount);
		params.put("timeDifference", timeDifference);

		return registrationStatusRepositary.createQuerySelect(queryStr, params, fetchSize);
	}

	public Integer getUnProcessedPacketsCount(long elapseTime, Integer reprocessCount, List<String> status) {

		Map<String, Object> params = new HashMap<>();
		String className = RegistrationStatusEntity.class.getSimpleName();
		String alias = RegistrationStatusEntity.class.getName().toLowerCase().substring(0, 1);
		LocalDateTime timeDifference = LocalDateTime.now().minusSeconds(elapseTime);
		List<String> statusCodes=new ArrayList<>();
		statusCodes.add(RegistrationStatusCode.PAUSED.toString());
		String queryStr = SELECT_DISTINCT + alias + FROM + className + EMPTY_STRING + alias + WHERE + alias
				+ ".latestTransactionStatusCode IN :status" + EMPTY_STRING + AND + EMPTY_STRING + alias
				+ ".regProcessRetryCount<=" + ":reprocessCount" + EMPTY_STRING + AND + EMPTY_STRING + alias
				+ ".latestTransactionTimes<" + ":timeDifference"+ EMPTY_STRING + AND + EMPTY_STRING+ alias
				+ ".statusCode  NOT IN :statusCode ";

		params.put("status", status);
		params.put("statusCode", statusCodes);
		params.put("reprocessCount", reprocessCount);
		params.put("timeDifference", timeDifference);
		List<RegistrationStatusEntity> unprocessedPackets = registrationStatusRepositary.createQuerySelect(queryStr,
				params);

		return unprocessedPackets.size();

	}

	public Boolean checkUinAvailabilityForRid(String rid) {
		Boolean uinAvailable = false;
		Map<String, Object> params = new HashMap<>();
		String className = RegistrationStatusEntity.class.getSimpleName();
		String alias = RegistrationStatusEntity.class.getName().toLowerCase().substring(0, 1);

		String queryStr = SELECT + alias + FROM + className + EMPTY_STRING + alias + WHERE + alias + ".regId = :rid " + AND
				+ " " + alias + ".statusCode = :status_Code";
		params.put("rid", rid);
		params.put("status_Code", "PROCESSED");
		List<RegistrationStatusEntity> unprocessedPackets = registrationStatusRepositary.createQuerySelect(queryStr,
				params);
		if (!unprocessedPackets.isEmpty()) {
			uinAvailable = true;
		}
		return uinAvailable;

	}

	/**
	 * Gets the by ids.
	 *
	 * @param ids
	 *            the ids
	 * @return the by ids
	 */
	public List<RegistrationStatusEntity> getByIdsAndTimestamp(List<String> ids) {

		Map<String, Object> params = new HashMap<>();
		String className = RegistrationStatusEntity.class.getSimpleName();

		String alias = RegistrationStatusEntity.class.getName().toLowerCase().substring(0, 1);
		String queryStr = SELECT_DISTINCT + alias + FROM + className + EMPTY_STRING + alias + WHERE + alias
				+ ".id IN :ids" + EMPTY_STRING + AND + EMPTY_STRING + alias + ISACTIVE_COLON + ISACTIVE + EMPTY_STRING
				+ AND + EMPTY_STRING + alias + ISDELETED_COLON + ISDELETED + EMPTY_STRING + ORDER_BY + EMPTY_STRING
				+ CREATED_DATE_TIME;
		params.put("ids", ids);
		params.put(ISACTIVE, Boolean.TRUE);
		params.put(ISDELETED, Boolean.FALSE);

		return registrationStatusRepositary.createQuerySelect(queryStr, params);
	}

	public List<RegistrationStatusEntity> getActionablePausedPackets(Integer fetchSize) {
		Map<String, Object> params = new HashMap<>();
		String className = RegistrationStatusEntity.class.getSimpleName();
		String alias = RegistrationStatusEntity.class.getName().toLowerCase().substring(0, 1);
		

		String queryStr = SELECT_DISTINCT + alias + FROM + className + EMPTY_STRING + alias + WHERE + alias
				+ ".statusCode IN :statusCodes" + EMPTY_STRING + AND + EMPTY_STRING + alias
				+ ".resumeTimeStamp < now()" + EMPTY_STRING + AND + EMPTY_STRING + alias
				+ ".defaultResumeAction is not null" + EMPTY_STRING + ORDER_BY + EMPTY_STRING + UPDATED_DATE_TIME;
		List<String> statusCodes = new ArrayList<String>();
		statusCodes.add(RegistrationStatusCode.PAUSED.toString());
		statusCodes.add(RegistrationStatusCode.PAUSED_FOR_ADDITIONAL_INFO.toString());
		params.put("statusCodes", statusCodes);

		return registrationStatusRepositary.createQuerySelect(queryStr, params, fetchSize);
	}

	public List<RegistrationStatusEntity> getResumablePackets(Integer fetchSize) {
		Map<String, Object> params = new HashMap<>();
		String className = RegistrationStatusEntity.class.getSimpleName();
		String alias = RegistrationStatusEntity.class.getName().toLowerCase().substring(0, 1);

		String queryStr = SELECT_DISTINCT + alias + FROM + className + EMPTY_STRING + alias + WHERE + alias
				+ ".statusCode =:status" + EMPTY_STRING
				+ ORDER_BY + EMPTY_STRING + UPDATED_DATE_TIME;

		params.put("status", RegistrationStatusCode.RESUMABLE.toString());

		return registrationStatusRepositary.createQuerySelect(queryStr, params, fetchSize);
	}

}