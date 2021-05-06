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

import io.mosip.registration.processor.core.workflow.dto.SearchDto;
import io.mosip.registration.processor.core.workflow.dto.SearchFilter;
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
	 * @param enrolmentId
	 *            the enrolment id
	 * @return the registration status entity
	 */
	public RegistrationStatusEntity findById(String enrolmentId) {
		Map<String, Object> params = new HashMap<>();
		String className = RegistrationStatusEntity.class.getSimpleName();

		String alias = RegistrationStatusEntity.class.getName().toLowerCase().substring(0, 1);

		String queryStr = SELECT_DISTINCT + alias + FROM + className + EMPTY_STRING + alias + WHERE + alias
				+ ".id=:registrationId" + EMPTY_STRING + AND + EMPTY_STRING + alias + ISACTIVE_COLON + ISACTIVE
				+ EMPTY_STRING + AND + EMPTY_STRING + alias + ISDELETED_COLON + ISDELETED;

		params.put("registrationId", enrolmentId);
		params.put(ISACTIVE, Boolean.TRUE);
		params.put(ISDELETED, Boolean.FALSE);

		List<RegistrationStatusEntity> registrationStatusEntityList = registrationStatusRepositary
				.createQuerySelect(queryStr, params);

		return !registrationStatusEntityList.isEmpty() ? registrationStatusEntityList.get(0) : null;
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

	public Page<RegistrationStatusEntity> nativeRegistrationQuerySearch(SearchDto searchDto) {
		Map<String, Object> params = new HashMap<>();
		String className = RegistrationStatusEntity.class.getSimpleName();
		String queryStr=null;
		long rows = 0;
		String alias = RegistrationStatusEntity.class.getName().toLowerCase().substring(0, 1);
		params.put(ISDELETED, Boolean.FALSE);
		queryStr = SELECT + alias + FROM + className + EMPTY_STRING + alias + WHERE + alias + ISDELETED_COLON
				+ ISDELETED;
		StringBuilder sb = new StringBuilder(queryStr);
		if (!searchDto.getFilters().isEmpty()) {
			Iterator<SearchFilter> searchIterator = searchDto.getFilters().iterator();
		while (searchIterator.hasNext()) {
			SearchFilter searchFilter = searchIterator.next();

			sb.append(EMPTY_STRING + AND + EMPTY_STRING + alias + "." + searchFilter.getColumnName() + "=:"
					+ searchFilter.getColumnName());
		}
	}
		sb.append(EMPTY_STRING + ORDER_BY + searchDto.getSort().get(0).getSortField() + EMPTY_STRING
				+ searchDto.getSort().get(0).getSortType());
		setRegistrationQueryParams(params, searchDto.getFilters());
		List<RegistrationStatusEntity> result = registrationStatusRepositary.createQuerySelect(sb.toString(), params,
				searchDto.getPagination().getPageFetch());
		rows = registrationStatusRepositary.count();
		return new PageImpl<>(result,
				PageRequest.of(searchDto.getPagination().getPageStart(), searchDto.getPagination().getPageFetch()),
				rows);

	}

	private void setRegistrationQueryParams(Map<String, Object> params, List<SearchFilter> list) {
		Iterator<SearchFilter> searchIter = list.iterator();
		while (searchIter.hasNext()) {
			SearchFilter searchFilter = searchIter.next();
			switch (searchFilter.getColumnName()) {
			case "statusCode":
				params.put("statusCode", searchFilter.getValue());
				break;
			case "isActive":
				params.put(ISACTIVE, Boolean.valueOf(searchFilter.getValue()));
				break;
			case "id":
				params.put("id", searchFilter.getValue());
				break;
			default:
				break;
			}
		}

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

		String queryStr = SELECT + alias + FROM + className + EMPTY_STRING + alias + WHERE + alias + ".id = :rid " + AND
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

	public List<RegistrationStatusEntity> getResumablePackets(Integer fetchSize) {
		Map<String, Object> params = new HashMap<>();
		String className = RegistrationStatusEntity.class.getSimpleName();
		String alias = RegistrationStatusEntity.class.getName().toLowerCase().substring(0, 1);
		

		String queryStr = SELECT_DISTINCT + alias + FROM + className + EMPTY_STRING + alias + WHERE + alias
				+ ".statusCode =:status" +  EMPTY_STRING + AND + EMPTY_STRING + alias
				+ ".resumeTimeStamp < now()" + EMPTY_STRING + AND + EMPTY_STRING + alias
				+ ".defaultResumeAction is not null" + EMPTY_STRING + ORDER_BY + EMPTY_STRING + UPDATED_DATE_TIME;

		params.put("status", RegistrationStatusCode.PAUSED.toString());
		

		return registrationStatusRepositary.createQuerySelect(queryStr, params, fetchSize);
	}

}