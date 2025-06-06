package io.mosip.registration.processor.core.abstractverticle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.mosip.registration.processor.core.constant.RegistrationType;

// TODO: Auto-generated Javadoc
/**
 * This class contains the address values to be used in Registration process.
 *
 * @author Pranav Kumar
 * @author Mukul Puspam
 * @since 0.0.1
 */
public class MessageBusAddress implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The Constant BUS_OUT. */
	public static final String BUS_OUT = "bus-out";

	/**
	 * Instantiates a new message bus address.
	 */
	public MessageBusAddress() {
	}

	/**
	 * Instantiates a new message bus address.
	 *
	 * @param messageBusAddress the message bus address
	 * @param regType           the reg type
	 */
	public MessageBusAddress(MessageBusAddress messageBusAddress, String regType) {

		List<String> addressList = (List<String>)Arrays.asList(messageBusAddress.getAddress().split("-"));

		ArrayList<String> modifiableArrayList = new ArrayList<String>(addressList);

		modifiableArrayList.add(addressList.size() - 2, regType.toLowerCase());
		String modifiedAddress = null;

		if(messageBusAddress.getAddress().contains(BUS_OUT)) {
			modifiedAddress = String.join("-", modifiableArrayList);
		}else{
			modifiedAddress = messageBusAddress.getAddress();
		}

		this.address = modifiedAddress;
	}

	/** The address. */
	private String address;

	/**
	 * Instantiates a new message bus address.
	 *
	 * @param address
	 *            The bus address
	 */
	public MessageBusAddress(String address) {
		this.address = address;
	}

	/**
	 * Gets the address.
	 *
	 * @return The address
	 */
	public String getAddress() {
		return this.address;
	}

	/**
	 * Sets the address.
	 *
	 * @param address
	 *            the new address
	 */
	public void setAddress(String address) {
		this.address = address;
	}

	/** The Constant BATCH_BUS. */
	public static final MessageBusAddress BATCH_BUS = new MessageBusAddress("batch-bus");

	/** The Constant STRUCTURE_BUS_IN. */
	public static final MessageBusAddress PACKET_VALIDATOR_BUS_IN = new MessageBusAddress("packet-validator-bus-in");

	/** The Constant STRUCTURE_BUS_OUT. */
	public static final MessageBusAddress PACKET_VALIDATOR_BUS_OUT = new MessageBusAddress("packet-validator-bus-out");

	/** The Constant BIOMETRIC_BUS_IN. */
	public static final MessageBusAddress BIO_DEDUPE_BUS_IN = new MessageBusAddress("bio-dedupe-bus-in");

	/** The Constant BIOMETRIC_BUS_OUT. */
	public static final MessageBusAddress BIO_DEDUPE_BUS_OUT = new MessageBusAddress("bio-dedupe-bus-out");

	/** The Constant FAILURE_BUS. */
	public static final MessageBusAddress FAILURE_BUS = new MessageBusAddress("failure-bus-in");

	/** The Constant RETRY_BUS. */
	public static final MessageBusAddress RETRY_BUS = new MessageBusAddress("retry-bus-in");

	/** The Constant ERROR. */
	public static final MessageBusAddress ERROR = new MessageBusAddress("error-bus-in");

	/** The Constant QUALITY_CHECK_BUS. */
	public static final MessageBusAddress QUALITY_CHECK_BUS = new MessageBusAddress("quality_check_bus-in");

	/** The Constant VIRUS_SCAN_BUS_IN. */
	public static final MessageBusAddress VIRUS_SCAN_BUS_IN = new MessageBusAddress("virus-scanner-bus-in");

	/** The Constant VIRUS_SCAN_BUS_OUT. */
	public static final MessageBusAddress VIRUS_SCAN_BUS_OUT = new MessageBusAddress("virus-scanner-bus-out");

	/** The Constant FTP_SCAN_BUS. */
	public static final MessageBusAddress FTP_SCAN_BUS_OUT = new MessageBusAddress("ftp-scanner-stage");

	/** The Constant PACKET_RECEIVER_OUT. */
	public static final MessageBusAddress PACKET_RECEIVER_OUT = new MessageBusAddress("packet-receiver-bus-out");

	/** The Constant STRUCTURE_BUS_IN. */
	public static final MessageBusAddress CMD_VALIDATOR_BUS_IN = new MessageBusAddress("c-m-d-validator-bus-in");

	/** The Constant STRUCTURE_BUS_OUT. */
	public static final MessageBusAddress CMD_VALIDATOR_BUS_OUT = new MessageBusAddress("c-m-d-validator-bus-out");
	
	/** The Constant OPERATOR_BUS_IN. */
	public static final MessageBusAddress OPERATOR_VALIDATOR_BUS_IN = new MessageBusAddress("operator-validator-bus-in");

	/** The Constant OPERATOR_BUS_OUT. */
	public static final MessageBusAddress OPERATOR_VALIDATOR_BUS_OUT = new MessageBusAddress("operator-validator-bus-out");
	
	/** The Constant SUPERVISOR_BUS_IN. */
	public static final MessageBusAddress SUPERVISOR_VALIDATOR_BUS_IN = new MessageBusAddress("supervisor-validator-bus-in");

	/** The Constant SUPERVISOR_BUS_OUT. */
	public static final MessageBusAddress SUPERVISOR_VALIDATOR_BUS_OUT = new MessageBusAddress("supervisor-validator-bus-out");
	
	/** The Constant INTRODUCER_BUS_IN. */
	public static final MessageBusAddress INTRODUCER_VALIDATOR_BUS_IN = new MessageBusAddress("introducer-validator-bus-in");

	/** The Constant SUPERVISOR_BUS_OUT. */
	public static final MessageBusAddress INTRODUCER_VALIDATOR_BUS_OUT = new MessageBusAddress("introducer-validator-bus-out");

	/** The Constant DEMODEDUPE_BUS_IN. */
	public static final MessageBusAddress DEMO_DEDUPE_BUS_IN = new MessageBusAddress("demo-dedupe-bus-in");

	/** The Constant DEMODEDUPE_BUS_OUT. */
	public static final MessageBusAddress DEMO_DEDUPE_BUS_OUT = new MessageBusAddress("demo-dedupe-bus-out");

	/** The Constant MANUAL_VERIFICATION_BUS. */
	public static final MessageBusAddress MANUAL_ADJUDICATION_BUS_OUT = new MessageBusAddress("manual-adjudication-bus-out");

	/** The Constant MANUAL_VERIFICATION_BUS_IN. */
	public static final MessageBusAddress MANUAL_ADJUDICATION_BUS_IN = new MessageBusAddress("manual-adjudication-bus-in");

	/** The Constant MANUAL_VERIFICATION_BUS. */
	public static final MessageBusAddress VERIFICATION_BUS_OUT = new MessageBusAddress("verification-bus-out");

	/** The Constant MANUAL_VERIFICATION_BUS_IN. */
	public static final MessageBusAddress VERIFICATION_BUS_IN = new MessageBusAddress("verification-bus-in");


	/** The Constant UIN_GENERATION_BUS_IN. */
	public static final MessageBusAddress UIN_GENERATION_BUS_IN = new MessageBusAddress("uin-generator-bus-in");

	/** The Constant UIN_GENERATION_BUS_OUT. */
	public static final MessageBusAddress UIN_GENERATION_BUS_OUT = new MessageBusAddress("uin-generator-bus-out");
	
	/** The Constant BIOMETRIC_EXTRACTION_BUS_IN. */
	public static final MessageBusAddress BIOMETRIC_EXTRACTION_BUS_IN = new MessageBusAddress("biometric-extraction-bus-in");

	/** The Constant BIOMETRIC_EXTRACTION_BUS_OUT. */
	public static final MessageBusAddress BIOMETRIC_EXTRACTION_BUS_OUT = new MessageBusAddress("biometric-extraction-bus-out");

	
	/** The Constant FINALIZATION_BUS_IN. */
	public static final MessageBusAddress FINALIZATION_BUS_IN = new MessageBusAddress("finalization-bus-in");

	/** The Constant FINALIZATION_BUS_OUT. */
	public static final MessageBusAddress FINALIZATION_BUS_OUT = new MessageBusAddress("finalization-bus-out");

	/** The Constant PACKET_UPLOADER_IN. */
	public static final MessageBusAddress PACKET_UPLOADER_IN = new MessageBusAddress("packet-uploader-bus-in");

	/** The Constant PACKET_UPLOADER_OUT. */
	public static final MessageBusAddress PACKET_UPLOADER_OUT = new MessageBusAddress("packet-uploader-bus-out");

	/** The Constant MESSAGE_SENDER_BUS_IN. */
	public static final MessageBusAddress MESSAGE_SENDER_BUS = new MessageBusAddress("message-sender-bus-in");

	/** The Constant REGISTRATION_CONNECTOR_BUS_OUT. */
	public static final MessageBusAddress REGISTRATION_CONNECTOR_BUS_OUT = new MessageBusAddress(
			"registration-connector-bus-out");

	/** The Constant PRINTING_BUS_IN. */
	public static final MessageBusAddress CREDENTIAL_REQUESTOR_BUS_IN = new MessageBusAddress("credential-requestor-bus-in");

	/** The Constant PRINTING_BUS_OUT. */
	public static final MessageBusAddress CREDENTIAL_REQUESTOR_BUS_OUT = new MessageBusAddress("credential-requestor-bus-out");

	/** The Constant PRINTING_BUS_RESEND. */
	public static final MessageBusAddress PRINTING_BUS_RESEND = new MessageBusAddress("printing-bus-resend");

	/** The Constant EXTERNAL_STAGE_BUS_IN. */
	public static final MessageBusAddress EXTERNAL_STAGE_BUS_IN = new MessageBusAddress("external-bus-in");

	/** The Constant EXTERNAL_STAGE_BUS_OUT. */
	public static final MessageBusAddress EXTERNAL_STAGE_BUS_OUT = new MessageBusAddress("external-bus-out");

	/** The Constant ABIS_MIDDLEWARE_BUS_IN. */
	public static final MessageBusAddress ABIS_MIDDLEWARE_BUS_IN = new MessageBusAddress("abis-middle-ware-bus-in");

	/** The Constant ABIS_MIDDLEWARE_BUS_OUT. */
	public static final MessageBusAddress ABIS_MIDDLEWARE_BUS_OUT = new MessageBusAddress("abis-middle-ware-bus-out");

	/** The Constant ABIS_HANDLER_BUS_IN. */
	public static final MessageBusAddress ABIS_HANDLER_BUS_IN = new MessageBusAddress("abis-handler-bus-in");

	/** The Constant ABIS_HANDLER_BUS_OUT. */
	public static final MessageBusAddress ABIS_HANDLER_BUS_OUT = new MessageBusAddress("abis-handler-bus-out");

	/** The Constant BIOMETRIC_AUTHENTICATION_BUS_IN. */
	public static final MessageBusAddress BIOMETRIC_AUTHENTICATION_BUS_IN = new MessageBusAddress("biometric-authentication-bus-in");

	/** The Constant BIOMETRIC_AUTHENTICATION_BUS_OUT. */
	public static final MessageBusAddress BIOMETRIC_AUTHENTICATION_BUS_OUT = new MessageBusAddress("biometric-authentication-bus-out");

	/** The Constant QUALITY_CLASSIFIER_BUS_IN. */
	public static final MessageBusAddress QUALITY_CLASSIFIER_BUS_IN = new MessageBusAddress("quality-classifier-bus-in");

	/** The Constant QUALITY_CLASSIFIER_BUS_OUT. */
	public static final MessageBusAddress QUALITY_CLASSIFIER_BUS_OUT = new MessageBusAddress("quality-classifier-bus-out");

	/** The Constant SECUREZONE_NOTIFICATION_IN. */
	public static final MessageBusAddress SECUREZONE_NOTIFICATION_IN = new MessageBusAddress("securezone-notification-bus-in");

	/** The Constant SECUREZONE_NOTIFICATION_OUT. */
	public static final MessageBusAddress SECUREZONE_NOTIFICATION_OUT = new MessageBusAddress("securezone-notification-bus-out");

	/** The Constant STRUCTURE_BUS_IN. */
	public static final MessageBusAddress PACKET_CLASSIFIER_BUS_IN = new MessageBusAddress("packet-classifier-bus-in");

	/** The Constant STRUCTURE_BUS_OUT. */
	public static final MessageBusAddress PACKET_CLASSIFIER_BUS_OUT = new MessageBusAddress("packet-classifier-bus-out");

	/** The Constant WORKFLOW_INTERNAL_ACTION_ADDRESS. */
	public static final MessageBusAddress WORKFLOW_INTERNAL_ACTION_ADDRESS = new MessageBusAddress(
			"workflow-internal-action");
	
	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "MessageBusAddress{" + "address='" + address + '\'' + '}';
	}
}
