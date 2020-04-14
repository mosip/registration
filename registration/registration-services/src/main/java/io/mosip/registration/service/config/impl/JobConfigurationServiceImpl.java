package io.mosip.registration.service.config.impl;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.LoggerConstants;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dao.SyncJobConfigDAO;
import io.mosip.registration.dao.SyncJobControlDAO;
import io.mosip.registration.dao.SyncTransactionDAO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.SyncDataProcessDTO;
import io.mosip.registration.entity.SyncControl;
import io.mosip.registration.entity.SyncJobDef;
import io.mosip.registration.entity.SyncTransaction;
import io.mosip.registration.jobs.BaseJob;
import io.mosip.registration.jobs.JobProcessListener;
import io.mosip.registration.jobs.JobTriggerListener;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.config.JobConfigurationService;

/**
 * implementation class of {@link JobConfigurationService}
 * 
 * @author YASWANTH S
 *
 */
@Service
public class JobConfigurationServiceImpl extends BaseService implements JobConfigurationService {

	/**
	 * To Fetch Job Configuration details
	 */
	@Autowired
	private SyncJobConfigDAO jobConfigDAO;

	/**
	 * Scheduler factory bean which will take Job and Trigger details and run jobs
	 * implicitly
	 */
	private SchedulerFactoryBean schedulerFactoryBean;

	/**
	 * To get Sync Transactions
	 */
	@Autowired
	private SyncTransactionDAO syncJobTransactionDAO;

	/**
	 * To get last completed transactions
	 */
	@Autowired
	private SyncJobControlDAO syncJobDAO;

	/**
	 * LOGGER for logging
	 */
	private static final Logger LOGGER = AppConfig.getLogger(JobConfigurationServiceImpl.class);

	/**
	 * Active sync job map with key as jobID and value as SyncJob (Entity)
	 */
	private Map<String, SyncJobDef> syncActiveJobMap = new HashMap<>();

	/**
	 * Sync job map with key as jobID and value as SyncJob (Entity)
	 */
	private Map<String, SyncJobDef> syncJobMap = new HashMap<>();

	private boolean isSchedulerRunning = false;

	/**
	 * To send it in job detail as Base job needs application context
	 */
	@Autowired
	private ApplicationContext applicationContext;

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	/**
	 * To load in JobDetail
	 */
	private JobDataMap jobDataMap = null;

	/**
	 * Base Job
	 */
	private BaseJob baseJob;

	private List<String> restartableJobList;

	/**
	 * create a parser based on provided definition
	 */
	private static CronParser cronParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));

	@Autowired
	private JobTriggerListener commonTriggerListener;
	@Autowired
	private JobProcessListener jobProcessListener;

	private List<String> offlineJobs;

	private List<String> unTaggedJobs;

	private static Map<String, SyncJobDef> parentJobMap = new HashMap<>();

	/**
	 * Active sync job map with key as jobID and value as SyncJob (Entity)
	 */
	private Map<String, SyncJobDef> syncActiveJobMapExecutable = new HashMap<>();

	public static Map<String, SyncJobDef> getParentJobMap() {
		return parentJobMap;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.JobConfigurationService#initiateJobs()
	 */
	@PostConstruct
	public void initiateJobs() {
		LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Jobs initiation was started");

		try {

			offlineJobs = getGlobalConfigValueOf(RegistrationConstants.offlineJobs) != null
					? Arrays.asList(getGlobalConfigValueOf(RegistrationConstants.offlineJobs).split(","))
					: offlineJobs;
			unTaggedJobs = getGlobalConfigValueOf(RegistrationConstants.unTaggedJobs) != null
					? Arrays.asList(getGlobalConfigValueOf(RegistrationConstants.unTaggedJobs).split(","))
					: unTaggedJobs;

			// it contains the list of job id, once this job is successfully completed then
			// application should be restarted to pick the updated config.
			restartableJobList = getGlobalConfigValueOf(RegistrationConstants.restartableJobs) != null
					? Arrays.asList(getGlobalConfigValueOf(RegistrationConstants.restartableJobs).split(","))
					: restartableJobList;

			/* Get All Jobs */
			List<SyncJobDef> jobDefs = getJobs();

			if (!isNull(jobDefs) && !isEmpty(jobDefs)) {

				/* Set Job-map and active sync-job-map */
				setSyncJobMap(jobDefs);

				/* Get Scheduler frequency from global param */
				String syncDataFreq = getGlobalConfigValueOf(RegistrationConstants.SYNC_DATA_FREQ);

				if (!isNull(syncDataFreq)) {

					LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
							RegistrationConstants.APPLICATION_ID, "Updating Sync Frequency : " + syncDataFreq);

					List<SyncJobDef> jobsToBeUpdated = new LinkedList<>();

					/* Store the jobs to be updated */
					for (SyncJobDef syncJobDef : jobDefs) {
						if (!syncDataFreq.equals(syncJobDef.getSyncFreq())) {
							syncJobDef.setSyncFreq(syncDataFreq);

							jobsToBeUpdated.add(syncJobDef);
						}

					}
					if (!isNull(jobsToBeUpdated) && !isEmpty(jobsToBeUpdated)) {
						/* Update Jobs */
						updateJobs(jobsToBeUpdated);

						/* Refresh The sync job map and sync active job map as we have updated jobs */
						setSyncJobMap(jobsToBeUpdated);
					}
				}

			}

			if (!syncActiveJobMap.isEmpty()) {

				/* Check and Execute missed triggers */
				executeMissedTriggers(syncActiveJobMapExecutable);

				schedulerFactoryBean = getSchedulerFactoryBean(String.valueOf(syncActiveJobMapExecutable.size()));

				startScheduler();

			}

		} catch (RuntimeException runtimeException) {
			LOGGER.error(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));

		} catch (Exception exception) {
			LOGGER.error(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					exception.getMessage() + ExceptionUtils.getStackTrace(exception));

		}

		LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Jobs initiation was completed");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.JobConfigurationService#startJobs(org.
	 * springframework.context.ApplicationContext)
	 */
	public ResponseDTO startScheduler() {
		LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "start jobs invocation started");

		ResponseDTO responseDTO = new ResponseDTO();

		/* Check Whether Scheduler is running or not */
		if (isSchedulerRunning()) {
			return setErrorResponse(responseDTO, RegistrationConstants.SYNC_DATA_PROCESS_ALREADY_STARTED, null);
		} else if (RegistrationConstants.ENABLE.equalsIgnoreCase(
				getGlobalConfigValueOf(RegistrationConstants.IS_REGISTRATION_JOBS_SCHEDULER_ENABLED))) {

			try {
				schedulerFactoryBean.start();
				isSchedulerRunning = true;

				/* Job Data Map */
				Map<String, Object> jobDataAsMap = new WeakHashMap<>();
				jobDataAsMap.put("applicationContext", applicationContext);
				jobDataAsMap.putAll(syncJobMap);

				jobDataMap = new JobDataMap(jobDataAsMap);

				loadScheduler(responseDTO);
			} catch (RuntimeException runtimeException) {
				LOGGER.error(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
						RegistrationConstants.APPLICATION_ID,
						runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
				setErrorResponse(responseDTO, RegistrationConstants.START_SCHEDULER_ERROR_MESSAGE, null);
			}

		} else {

			// Configuration was disabled to run the jobs scheduler
			return setErrorResponse(responseDTO, RegistrationConstants.START_SCHEDULER_ERROR_MESSAGE, null);
		}

		LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "start jobs invocation ended");

		return responseDTO;

	}

	private void loadScheduler(ResponseDTO responseDTO) {
		LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Loading Scheduler started");

		syncActiveJobMapExecutable.forEach((jobId, syncJob) -> {

			try {
				LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
						RegistrationConstants.APPLICATION_ID, "Checking the Job to load in scheduler : " + jobId);

				if (!isNull(syncJob.getApiName()) && responseDTO.getErrorResponseDTOs() == null && isSchedulerRunning()
						&& !schedulerFactoryBean.getScheduler().checkExists(new JobKey(jobId))) {

					// Get Job instance through application context
					baseJob = (BaseJob) applicationContext.getBean(syncJob.getApiName());

					JobDetail jobDetail = JobBuilder.newJob(baseJob.jobClass()).withIdentity(syncJob.getId())
							.usingJobData(jobDataMap).build();

					CronTrigger trigger = (CronTrigger) TriggerBuilder.newTrigger().forJob(jobDetail)
							.withIdentity(syncJob.getId())
							.withSchedule(CronScheduleBuilder.cronSchedule(syncJob.getSyncFreq())).build();

					schedulerFactoryBean.getScheduler().scheduleJob(jobDetail, trigger);

					LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
							RegistrationConstants.APPLICATION_ID, "Scheduler loaded the job :" + jobId);

				}
			} catch (SchedulerException | RuntimeException exception) {
				LOGGER.error(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
						RegistrationConstants.APPLICATION_ID,
						exception.getMessage() + ExceptionUtils.getStackTrace(exception));

				/* Stop, Clear Scheduler and set Error response */
				setStartExceptionError(responseDTO);

			}

			if (isSchedulerRunning()) {
				setSuccessResponse(responseDTO, RegistrationConstants.BATCH_JOB_START_SUCCESS_MESSAGE, null);
			}

		}

		);

		LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Loading Scheduler completed");

	}

	private void setStartExceptionError(ResponseDTO responseDTO) {

		try {
			/* Clear Scheduler */
			clearScheduler();

		} catch (SchedulerException schedulerException) {
			LOGGER.error(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					schedulerException.getMessage() + ExceptionUtils.getStackTrace(schedulerException));
		}

		/* Error Response */
		setErrorResponse(responseDTO, RegistrationConstants.START_SCHEDULER_ERROR_MESSAGE, null);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.service.config.JobConfigurationService#stopScheduler()
	 */
	public ResponseDTO stopScheduler() {
		LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "stop jobs invocation started");

		ResponseDTO responseDTO = new ResponseDTO();

		try {
			if (schedulerFactoryBean.isRunning()) {

				/* Clear and Stop Scheduler */
				clearScheduler();

				setSuccessResponse(responseDTO, RegistrationConstants.BATCH_JOB_STOP_SUCCESS_MESSAGE, null);

			} else {
				setErrorResponse(responseDTO, RegistrationConstants.SYNC_DATA_PROCESS_ALREADY_STOPPED, null);

			}
		} catch (RuntimeException | SchedulerException schedulerException) {
			LOGGER.error(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					schedulerException.getMessage() + ExceptionUtils.getStackTrace(schedulerException));
			setErrorResponse(responseDTO, RegistrationConstants.STOP_SCHEDULER_ERROR_MESSAGE, null);

		}

		LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "stop jobs invocation ended");

		return responseDTO;
	}

	private void clearScheduler() throws SchedulerException {

		LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Clearing and stopping the Scheduler");

		/* Clear Scheduler */
		schedulerFactoryBean.getScheduler().clear();
		schedulerFactoryBean.stop();
		isSchedulerRunning = false;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.JobConfigurationService#
	 * getCurrentRunningJobDetails()
	 */
	public ResponseDTO getCurrentRunningJobDetails() {
		LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "get current running job details started");

		ResponseDTO responseDTO = new ResponseDTO();

		try {

			if (schedulerFactoryBean != null && isSchedulerRunning()) {
				// Get currently executing jobs from scheduler factory
				List<JobExecutionContext> executingJobList = schedulerFactoryBean.getScheduler()
						.getCurrentlyExecutingJobs();

				if (isNull(executingJobList) || isEmpty(executingJobList)) {
					setErrorResponse(responseDTO, RegistrationConstants.NO_JOBS_RUNNING, null);
				} else {
					List<SyncDataProcessDTO> dataProcessDTOs = executingJobList.stream().map(jobExecutionContext -> {

						SyncJobDef syncJobDef = syncJobMap.get(jobExecutionContext.getJobDetail().getKey().getName());

						return constructDTO(syncJobDef.getId(), syncJobDef.getName(), RegistrationConstants.JOB_RUNNING,
								Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()).toString());

					}).collect(Collectors.toList());

					setResponseDTO(dataProcessDTOs, responseDTO, null, RegistrationConstants.NO_JOBS_RUNNING);

				}
			} else {
				setErrorResponse(responseDTO, RegistrationConstants.NO_JOBS_RUNNING, null);
			}

		} catch (SchedulerException schedulerException) {
			LOGGER.error(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					schedulerException.getMessage() + ExceptionUtils.getStackTrace(schedulerException));

			setErrorResponse(responseDTO, RegistrationConstants.CURRENT_JOB_DETAILS_ERROR_MESSAGE, null);

		}

		LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "get current running job details ended");

		return responseDTO;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.service.config.JobConfigurationService#executeJob(java.
	 * lang.String, java.lang.String)
	 */

	public ResponseDTO executeJob(String jobId, String triggerPoint) {

		LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Execute job started : " + jobId);
		ResponseDTO responseDTO = new ResponseDTO();
		if (jobId != null && triggerPoint != null) {
			try {
				SyncJobDef syncJobDef = syncActiveJobMap.get(jobId);

				if (syncJobDef != null && !isNull(syncJobDef.getApiName())) {
					// Get Job using application context and api name
					baseJob = (BaseJob) applicationContext.getBean(syncJobDef.getApiName());

					BaseJob.removeCompletedJobInMap(jobId);

					baseJob.setApplicationContext(applicationContext);

					// Job Invocation
					responseDTO = baseJob.executeJob(triggerPoint, jobId);

				} else {
					setErrorResponse(responseDTO, RegistrationConstants.EXECUTE_JOB_ERROR_MESSAGE, null);
				}
				return responseDTO;

			} catch (RuntimeException runtimeException) {
				LOGGER.error(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
						RegistrationConstants.APPLICATION_ID,
						runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));

				setErrorResponse(responseDTO, RegistrationConstants.EXECUTE_JOB_ERROR_MESSAGE, null);
			}
		} else {

			LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					"Unable to execute job as job id or trigger point value was null");

			setErrorResponse(responseDTO, RegistrationConstants.EXECUTE_JOB_ERROR_MESSAGE, null);

		}

		LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Execute job ended : " + jobId);

		return responseDTO;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.config.JobConfigurationService#
	 * getLastCompletedSyncJobs()
	 */
	@Override
	public ResponseDTO getLastCompletedSyncJobs() {

		LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "get Last Completed Jobs Started");

		ResponseDTO responseDTO = new ResponseDTO();

		/* Fetch Sync control records */
		List<SyncControl> syncControls = syncJobDAO.findAll();

		if (!isNull(syncControls) && !isEmpty(syncControls)) {
			List<SyncDataProcessDTO> syncDataProcessDTOs = syncControls.stream().map(syncControl -> {

				String jobName = (syncJobMap.get(syncControl.getSyncJobId()) == null)
						? RegistrationConstants.JOB_UNKNOWN
						: syncJobMap.get(syncControl.getSyncJobId()).getName();

				String lastUpdTimes = (syncControl.getUpdDtimes() == null) ? syncControl.getCrDtime().toString()
						: syncControl.getUpdDtimes().toString();

				return constructDTO(syncControl.getSyncJobId(), jobName, RegistrationConstants.JOB_COMPLETED,
						lastUpdTimes);

			}).collect(Collectors.toList());

			setResponseDTO(syncDataProcessDTOs, responseDTO, null, RegistrationConstants.NO_JOB_COMPLETED);

		} else {
			setErrorResponse(responseDTO, RegistrationConstants.NO_JOB_COMPLETED, null);
		}

		LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "get Last Completed Jobs Ended");

		return responseDTO;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.config.JobConfigurationService#
	 * getSyncJobsTransaction()
	 */
	@Override
	public ResponseDTO getSyncJobsTransaction() {

		LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "get Sync Transaction Started");

		ResponseDTO responseDTO = new ResponseDTO();

		String val = getGlobalConfigValueOf(RegistrationConstants.SYNC_TRANSACTION_NO_OF_DAYS_LIMIT);

		if (val != null) {
			int syncTransactionConfiguredDays = Integer.parseInt(val);

			/* Get Calendar instance */
			Calendar cal = Calendar.getInstance();
			cal.setTime(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
			cal.add(Calendar.DATE, -syncTransactionConfiguredDays);

			/* To-Date */
			Timestamp req = new Timestamp(cal.getTimeInMillis());

			/* Get All sync Transaction Details from DataBase */
			List<SyncTransaction> syncTransactionList = syncJobTransactionDAO.getSyncTransactions(req,
					RegistrationConstants.JOB_TRIGGER_POINT_USER);

			if (!isNull(syncTransactionList) && !isEmpty(syncTransactionList)) {

				List<SyncDataProcessDTO> syncDataProcessDTOs = syncTransactionList.stream().map(syncTransaction -> {

					String jobName = (syncJobMap.get(syncTransaction.getSyncJobId()) == null)
							? RegistrationConstants.JOB_UNKNOWN
							: syncJobMap.get(syncTransaction.getSyncJobId()).getName();

					return constructDTO(syncTransaction.getSyncJobId(), jobName, syncTransaction.getStatusCode(),
							syncTransaction.getCrDtime().toString());

				}).collect(Collectors.toList());

				setResponseDTO(syncDataProcessDTOs, responseDTO, null, RegistrationConstants.NO_JOBS_TRANSACTION);

			} else {
				setErrorResponse(responseDTO, RegistrationConstants.NO_JOBS_TRANSACTION, null);
			}
		}

		LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "get Sync Transaction Ended");

		return responseDTO;
	}

	private SyncDataProcessDTO constructDTO(String jobId, String jobName, String statusCode, String crDtimes) {
		/* create new Sync Data Process DTO */
		return new SyncDataProcessDTO(jobId, jobName, statusCode, crDtimes);

	}

	private void setResponseDTO(List<SyncDataProcessDTO> syncDataProcessDTOs, ResponseDTO responseDTO,
			String successMsg, String errorMsg) {

		/* Set Response DTO with Error or Success result */

		if (isNull(syncDataProcessDTOs) || isEmpty(syncDataProcessDTOs)) {
			setErrorResponse(responseDTO, errorMsg, null);

		} else {
			Map<String, Object> attributes = new WeakHashMap<>();
			attributes.put(RegistrationConstants.SYNC_DATA_DTO, syncDataProcessDTOs);

			setSuccessResponse(responseDTO, successMsg, attributes);
		}
	}

	private List<SyncJobDef> getJobs() {

		LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Fetching sync_job_defs from db");
		return jobConfigDAO.getAll();
	}

	private void setSyncJobMap(List<SyncJobDef> syncJobDefs) {
		syncJobDefs.forEach(syncJob -> {

			/* All Jobs */
			syncJobMap.put(syncJob.getId(), syncJob);

			/* Active Jobs Map */
			if (syncJob.getIsActive()) {
				syncActiveJobMap.put(syncJob.getId(), syncJob);
			}

		});

		syncActiveJobMapExecutable = syncActiveJobMap;

		syncActiveJobMap.forEach((jobID, syncJobDef) -> {
			if (!isNull(syncJobDef.getParentSyncJobId())) {
				if (syncActiveJobMap.get(syncJobDef.getParentSyncJobId()) == null) {
					syncActiveJobMapExecutable.remove(jobID);
				} else {
					parentJobMap.put(jobID, syncActiveJobMap.get(syncJobDef.getParentSyncJobId()));
					syncActiveJobMapExecutable.remove(syncJobDef.getParentSyncJobId());
				}
			}
		});
	}

	private void updateJobs(final List<SyncJobDef> syncJobDefs) {

		LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Updating Jobs into DB");

		jobConfigDAO.updateAll(syncJobDefs);

		LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Updating Jobs in sync active job map");

	}

	private void executeMissedTrigger(final String jobId, final String syncFrequency) {

		LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Started Checking whether Trigger missed for  : " + jobId);

		ExecutionTime executionTime = getExecutionTime(syncFrequency);

		Instant last = getLast(executionTime);
		Instant next = getNext(executionTime);

		/* Check last and next has values present */
		if (last != null && next != null) {

			/* Get all Transactions in between last and next crDtimes */
			List<SyncTransaction> syncTransactions = syncJobTransactionDAO.getAll(jobId, Timestamp.from(last),
					Timestamp.from(next));

			LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, "Completed Checking whether Trigger missed for  : " + jobId);

			/* Execute the Job if it was not started on previous pre-scheduled time */
			if ((isNull(syncTransactions) || isEmpty(syncTransactions))) {
				executeJob(jobId, RegistrationConstants.JOB_TRIGGER_POINT_SYSTEM);

			}
		}

	}

	private Instant getLast(ExecutionTime executionTime) {
		ZonedDateTime currentTime = ZonedDateTime.now(ZoneId.of("UTC"));

		Optional<ZonedDateTime> lastDate = executionTime.lastExecution(currentTime);
		Instant last = null;
		if (lastDate.isPresent()) {
			last = lastDate.get().toInstant();

		}

		return last;

	}

	private Instant getNext(ExecutionTime executionTime) {
		ZonedDateTime currentTime = ZonedDateTime.now(ZoneId.of("UTC"));

		Optional<ZonedDateTime> nextDate = executionTime.nextExecution(currentTime);
		Instant next = null;
		if (nextDate.isPresent()) {
			next = nextDate.get().toInstant();

		}

		return next;

	}

	private ExecutionTime getExecutionTime(String syncFrequency) {
		return ExecutionTime.forCron(cronParser.parse(syncFrequency));
	}

	private void executeMissedTriggers(Map<String, SyncJobDef> map) {

		LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Started invoking Missed Trigger Jobs");

		map.forEach((jobId, syncJob) -> {
			if (!isNull(syncJob.getSyncFreq()) && !isNull(syncJob.getApiName())) {
				/* An A-sync task to complete missed trigger */
				new Thread(() -> executeMissedTrigger(jobId, syncJob.getSyncFreq())).start();
			}

		});

		LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Completed invoking Missed Trigger Jobs");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.service.config.JobConfigurationService#executeAllJobs()
	 */
	@Override
	public ResponseDTO executeAllJobs() {

		LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Started execute all jobs");
		ResponseDTO responseDTO = new ResponseDTO();
		BaseJob.successJob.clear();
		BaseJob.clearCompletedJobMap();
		List<String> failureJobs = new LinkedList<>();

		for (Entry<String, SyncJobDef> syncJob : syncActiveJobMapExecutable.entrySet()) {
			LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, "Validating job to execute : " + syncJob.getKey());

			if ((offlineJobs == null || !offlineJobs.contains(syncJob.getKey())
					&& (unTaggedJobs == null || !unTaggedJobs.contains(syncJob.getKey())))
					&& !isNull(syncJob.getValue().getApiName())) {

				String triggerPoint = getUserIdFromSession().equals(RegistrationConstants.JOB_TRIGGER_POINT_SYSTEM)
						? RegistrationConstants.JOB_TRIGGER_POINT_SYSTEM
						: RegistrationConstants.JOB_TRIGGER_POINT_USER;

				executeJob(syncJob.getKey(), triggerPoint);

			}
		}

		/* Child Job's check */
		BaseJob.getCompletedJobMap().forEach((jobId, status) -> {
			if (!status.equalsIgnoreCase(RegistrationConstants.JOB_EXECUTION_SUCCESS)) {
				failureJobs.add(syncActiveJobMap.get(jobId).getName());
			}
		});

		if (!isEmpty(failureJobs)) {
			setErrorResponse(responseDTO, failureJobs.toString().replace("[", "").replace("]", ""), null);
		}
		LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "completed execute all jobs");

		return responseDTO;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.config.JobConfigurationService#isRestart()
	 */
	@Override
	public ResponseDTO isRestart() {

		LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Checking for is to be re-start started");

		ResponseDTO responseDTO = new ResponseDTO();
		/* Fetch completed job map */
		Map<String, String> completedSyncJobMap = BaseJob.getCompletedJobMap();

		/* Compare with restart-able job list */
		for (String jobId : restartableJobList) {

			/* Check the job completed with success/failure */
			if (RegistrationConstants.JOB_EXECUTION_SUCCESS.equals(completedSyncJobMap.get(jobId))) {

				/* Store job info in attributes of response */
				Map<String, Object> successJobAttribute = new WeakHashMap<>();
				successJobAttribute.put(RegistrationConstants.JOB_ID, jobId);

				return setSuccessResponse(responseDTO,
						syncActiveJobMap.get(jobId).getName() + " " + RegistrationConstants.OTP_VALIDATION_SUCCESS,
						successJobAttribute);
			}
		}
		LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Checking for is to be re-start completed");
		return responseDTO;
	}

	@Override
	public ResponseDTO getRestartTime() {

		LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Getting Re-start time started");

		ResponseDTO responseDTO = new ResponseDTO();

		String syncDataFreq = getGlobalConfigValueOf(RegistrationConstants.SYNC_DATA_FREQ);
		if (syncDataFreq != null) {
			ExecutionTime executionTime = getExecutionTime(syncDataFreq);
			Instant last = getLast(executionTime);
			Instant next = getNext(executionTime);

			if (last != null && next != null) {
				setSuccessResponse(responseDTO, String.valueOf((Duration.between(last, next).toMillis()) / 5), null);
			}
		}
		LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Getting Re-start time completed");

		return responseDTO;
	}

	/**
	 * scheduler factory bean used to schedule the batch jobs
	 * 
	 * @return scheduler factory which includes job detail and trigger detail
	 * @throws Exception
	 */
	private SchedulerFactoryBean getSchedulerFactoryBean(String count) throws Exception {

		LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Initializing Scheduler Factory Bean started");

		SchedulerFactoryBean schFactoryBean = new SchedulerFactoryBean();
		schFactoryBean.setGlobalTriggerListeners(new TriggerListener[] { commonTriggerListener });
		schFactoryBean.setGlobalJobListeners(new JobListener[] { jobProcessListener });
		Properties quartzProperties = new Properties();
		quartzProperties.put("org.quartz.threadPool.threadCount", count);
		schFactoryBean.setQuartzProperties(quartzProperties);
		schFactoryBean.afterPropertiesSet();

		LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Initializing Scheduler Factory Bean completed");

		return schFactoryBean;
	}

	@Override
	public boolean isSchedulerRunning() {
		return isSchedulerRunning;
	}

	@Override
	public Map<String, SyncJobDef> getActiveSyncJobMap() {
		return syncActiveJobMap;
	}

	public List<String> getOfflineJobs() {
		return offlineJobs;
	}

	public List<String> getUnTaggedJobs() {
		return unTaggedJobs;
	}

	@Override
	public SyncControl getSyncControlOfJob(String syncJobId) {

		LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Getting Job info in sync control Started");

		SyncControl syncControl = null;
		if (syncJobId != null) {
			syncControl = syncJobDAO.findBySyncJobId(syncJobId);
		}
		LOGGER.info(LoggerConstants.BATCH_JOBS_CONFIG_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Getting Job info in sync control Completed");

		return syncControl;
	}

}
