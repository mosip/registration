package io.mosip.registration.processor.stages.cmdvalidator.utils;

/**
 * The Class StatusMessage.
 */
public class StatusMessage {

	/**
	 * Instantiates a new status message.
	 */
	private StatusMessage() {

	}

	/** The Constant MACHINE_ID_NOT_FOUND. */
	public static final String MACHINE_ID_NOT_FOUND = "The Machine ID was not found in Master DB for Registration ID";

	/** The Constant MACHINE_NOT_ACTIVE. */
	public static final String MACHINE_NOT_ACTIVE = "The Machine ID was not active when Registration ID  was created";

	/** The Constant CENTER_ID_NOT_FOUND. */
	public static final String CENTER_ID_NOT_FOUND = "The Center ID was not found in Master DB for Registration ID";

	/** The Constant CENTER_NOT_ACTIVE. */
	public static final String CENTER_NOT_ACTIVE = "The Center was not active when Registration ID was created";

	/** The Constant CENTER_MACHINE_USER_MAPPING_NOT_FOUND. */
	public static final String CENTER_MACHINE_USER_MAPPING_NOT_FOUND = "The Center-Machine-User Mapping for Center, Machine & supervisor/officer was not found";

	/** The Constant GPS_DATA_NOT_PRESENT. */
	public static final String GPS_DATA_NOT_PRESENT = "The GPS Details for the Packet is Not Present";

	/** The Constant PARENT_UIN_NOT_FOUND_IN_TABLE. */
	public static final String PARENT_UIN_NOT_FOUND_IN_TABLE = "The UIN of Parent is not present in individual_demographic_dedup for Packet";

	/** The Constant DEVICE_NOT_FOUND. */
	public static final String DEVICE_NOT_FOUND = "was not available for Registration ID";

	/** The Constant DEVICE_WAS_IN_ACTIVE. */
	public static final String DEVICE_WAS_IN_ACTIVE = "was inactive for a Packet with Registration ID";

	/** The Constant DEVICE_ID. */
	public static final String DEVICE_ID = "The Device ID";

	/** The Constant CENTER_ID. */
	public static final String CENTER_ID = "and Center ID ";

	/** The Constant THE_CENTER_ID. */
	public static final String THE_CENTER_ID = "The Center ID";

	/** The Constant CENTER_NOT_FOUND. */
	public static final String CENTER_NOT_FOUND = " was not available for Registration ID";

	/** The Constant TIMESTAMP_VALIDATION1. */
	public static final String TIMESTAMP_VALIDATION1 = "The Packet with Registration ID";

	/** The Constant TIMESTAMP_VALIDATION2. */
	public static final String TIMESTAMP_VALIDATION2 = " was not created in Working Hours of the Center with Center ID";

	public static final String PARENT_UIN_NOT_AVAIALBLE = "Parent UIN not available";

	public static final String PACKET_CREATION_DATE_NOT_PRESENT_IN_PACKET = "packet creationDate is null in packet";

	public static final String AUTHENTICATION_FAILED = "Authentication failed";

    public static final String IDA_AUTHENTICATION_FAILURE = "IDA Authentication failed for the Supervisor/Officer";
}
