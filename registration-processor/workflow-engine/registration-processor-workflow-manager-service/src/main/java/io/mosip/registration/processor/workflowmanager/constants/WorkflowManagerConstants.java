package io.mosip.registration.processor.workflowmanager.constants;

public class WorkflowManagerConstants {

	/** The Constant USER. */
	public static final String USER = "MOSIP_SYSTEM";
	//Based on the below ceylon chime version, the required ceylon dependencies are zipped 
	//and uploaded to artifactory and downloaded and unzipped while starting the docker container
	public static final String CEYLON_SCHEDULER = "ceylon:herd.schedule.chime/0.2.0";
	public static final String TIMER_EVENT = "scheduler:workflowactionjob_timer";
	public static final String TYPE = "type";
	public static final String SECONDS = "seconds";
	public static final String MINUTES = "minutes";
	public static final String HOURS = "hours";
	public static final String DAY_OF_MONTH = "days of month";
	public static final String MONTHS = "months";
	public static final String DAYS_OF_WEEK = "days of week";
	public static final String TYPE_VALUE = "mosip.regproc.workflow-manager.action.job.type";
	public static final String SECONDS_VALUE = "mosip.regproc.workflow-manager.action.job.seconds";
	public static final String MINUTES_VALUE = "mosip.regproc.workflow-manager.action.job.minutes";
	public static final String HOURS_VALUE = "mosip.regproc.workflow-manager.action.job.hours";
	public static final String DAY_OF_MONTH_VALUE = "mosip.regproc.workflow-manager.action.job.days_of_month";
	public static final String MONTHS_VALUE = "mosip.regproc.workflow-manager.action.job.months";
	public static final String DAYS_OF_WEEK_VALUE = "mosip.regproc.workflow-manager.action.job.days_of_week";
	public static final String CHIME = "chime";
	public static final String OPERATION = "operation";
	public static final String OPERATION_VALUE = "create";
	public static final String NAME = "name";
	public static final String NAME_VALUE = "scheduler:workflowactionjob_timer";
	public static final String DESCRIPTION = "description";
	
}
