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
		List<RegistrationStatusEntity> registrationStatusEntityList =null;
		if (workflowInstanceId != null) {
			registrationStatusEntityList=registrationStatusRepositary.findByWorkflowInstanceId(workflowInstanceId);
		} else {
			registrationStatusEntityList=registrationStatusRepositary.findByRegId(rid);
		}
		return !registrationStatusEntityList.isEmpty() ? registrationStatusEntityList.get(0) : null;
	}

	public List<RegistrationStatusEntity> findAll(String rid) {

		return registrationStatusRepositary.findByRegId(rid);
	}
	
	/**
	 * Gets the enrolment status by status code.
	 *
	 * @param status
	 *            the status
	 * @return the enrolment status by status code
	 */
	public List<RegistrationStatusEntity> getEnrolmentStatusByStatusCode(String status) {

		return registrationStatusRepositary.findByStatusCode(status);
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
		rows = result.size();

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
		return registrationStatusRepositary.findByRegIds(ids);
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
			Integer reprocessCount, List<String> status, List<String> excludeStageNames) {

		LocalDateTime timeDifference = LocalDateTime.now().minusSeconds(elapseTime);
		List<String> statusCodes=new ArrayList<>();
		statusCodes.add(RegistrationStatusCode.PAUSED.toString());
		statusCodes.add(RegistrationStatusCode.RESUMABLE.toString());
		statusCodes.add(RegistrationStatusCode.PAUSED_FOR_ADDITIONAL_INFO.toString());
		statusCodes.add(RegistrationStatusCode.REJECTED.toString());
		statusCodes.add(RegistrationStatusCode.FAILED.toString());
		statusCodes.add(RegistrationStatusCode.PROCESSED.toString());

		return registrationStatusRepositary.getUnProcessedPackets(status, reprocessCount, timeDifference, 
			statusCodes, fetchSize, excludeStageNames);
	}

	public Integer getUnProcessedPacketsCount(long elapseTime, Integer reprocessCount, List<String> status, 
			List<String> excludeStageNames) {
		LocalDateTime timeDifference = LocalDateTime.now().minusSeconds(elapseTime);
		List<String> statusCodes=new ArrayList<>();
		statusCodes.add(RegistrationStatusCode.PAUSED.toString());
		return registrationStatusRepositary.getUnProcessedPacketsCount(status, reprocessCount, timeDifference, 
			statusCodes, excludeStageNames);
	}

	public Boolean checkUinAvailabilityForRid(String rid) {
		return !registrationStatusRepositary.findByRegIdANDByStatusCode(rid, RegistrationStatusCode.PROCESSED.toString()).isEmpty();

	}

	/**
	 * Gets the by ids.
	 *
	 * @param ids
	 *            the ids
	 * @return the by ids
	 */
	public List<RegistrationStatusEntity> getByIdsAndTimestamp(List<String> ids) {

		return registrationStatusRepositary.findByRegIdsOrderbyCreatedDateTime(ids);
	}

	public List<RegistrationStatusEntity> getActionablePausedPackets(Integer fetchSize) {
		
		List<String> statusCodes = new ArrayList<String>();
		statusCodes.add(RegistrationStatusCode.PAUSED.toString());
		statusCodes.add(RegistrationStatusCode.PAUSED_FOR_ADDITIONAL_INFO.toString());

		return registrationStatusRepositary.getActionablePausedPackets(statusCodes, fetchSize);
	}

	public List<RegistrationStatusEntity> getResumablePackets(Integer fetchSize) {

		return registrationStatusRepositary.getResumablePackets(RegistrationStatusCode.RESUMABLE.toString(), fetchSize);
	}

	public List<RegistrationStatusEntity> findByIdAndProcessAndIteration(String id, String process, int iteration)
	{
		return registrationStatusRepositary.getByIdAndProcessAndIteration(id, process, iteration);
	}
}