package io.mosip.registration.processor.core.constant;

public enum  ProviderStageName {

    PACKET_RECEIVER("packetreceiver"),
    PACKET_UPLOADER("packetuploader"),
    PACKET_VALIDATOR("packetvalidator"),
    QUALITY_CHECKER("qualitychecker"),
    OSI_VALIDATOR("osivalidator"),
    CMD_VALIDATOR("cmdvalidator"),
    OPERATOR_VALIDATOR("operatorvalidator"),
    SUPERVISOR_VALIDATOR("supervisorvalidator"),
    INTRODUCER_VALIDATOR("introducervalidator"),
    DEMO_DEDUPE("demodedupe"),
    CLASSIFICATION("classification"),
    BIO_DEDUPE("biodedupe"),
    BIO_AUTH("bioauth"),
    MANUAL_ADJUDICATION("manualadjudication"),
    VERIFICATION("verification"),
    UIN_GENERATOR("uingenerator"),
    WORKFLOW_MANAGER("workflowmanager"),
    MESSAGE_SENDER("messagesender");

    private String value;

    private ProviderStageName(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
