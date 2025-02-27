package io.mosip.registration.processor.core.constant;

public enum APIAuthorityList {

	PACKETRECEIVER(new String[] { "REGISTRATION_ADMIN", "REGISTRATION_PROCESSOR", "REGISTRATION_OFFICER",
			"REGISTRATION_SUPERVISOR", "RESIDENT" }),

	SECUREZONENOTIFICATION(new String[] { "REGISTRATION_PROCESSOR" }),

	PACKETSYNC(new String[] { "REGISTRATION_ADMIN", "REGISTRATION_PROCESSOR", "REGISTRATION_OFFICER",
			"REGISTRATION_SUPERVISOR" }),

	REGISTRATIONSTATUS(new String[] { "REGISTRATION_ADMIN", "REGISTRATION_OFFICER", "REGISTRATION_SUPERVISOR","RESIDENT" }),

	MANUALVERIFICTION(new String[] { "REGISTRATION_ADMIN" }),

	PRINTSTAGE(new String[] { "REGISTRATION_PROCESSOR" }),

	BIODEDUPE(new String[] { "REGISTRATION_PROCESSOR" }),

	CONNECTORSTAGE(new String[] { "REGISTRATION_PROCESSOR" }),

	BIO(new String[] { "REGISTRATION_PROCESSOR" }),

	ABIS(new String[] { "REGISTRATION_PROCESSOR" }),

	PACKETGENERATOR(new String[] { "REGISTRATION_ADMIN", "REGISTRATION_PROCESSOR" }),

	REGISTRATIONTRANSACTION(new String[] { "REGISTRATION_ADMIN", "REGISTRATION_PROCESSOR"}),

	REQUESTHANDLER(new String[] { "REGISTRATION_ADMIN", "REGISTRATION_PROCESSOR" }),

	WORKFLOWACTION(new String[] { "REGISTRATION_PROCESSOR", "GLOBAL_ADMIN" }),

	WORKFLOWSEARCH(new String[] { "REGISTRATION_PROCESSOR", "GLOBAL_ADMIN" }),

    WORKFLOWINSTANCE(new String[] { "REGISTRATION_PROCESSOR", "GLOBAL_ADMIN" }),

	PACKETEXTERNALSTATUS(
			new String[] { "REGISTRATION_ADMIN", "REGISTRATION_OFFICER", "REGISTRATION_SUPERVISOR", "RESIDENT" });

	private final String[] list;

	private APIAuthorityList(String[] list) {
		this.list = list;
	}

	public String[] getList() {
		return this.list.clone();
	}
}