package io.mosip.registration.processor.core.constant;

public enum  ProviderStageName {

    PACKET_RECEIVER("packetreceiver"),
    PACKET_UPLOADER("packetuploader"),
    PACKET_VALIDATOR("packetvalidator"),
    QUALITY_CHECKER("qualitychecker"),
    OSI_VALIDATOR("osivalidator"),
    DEMO_DEDUPE("demodedupe"),
    CLASSIFICATION("classification"),
    BIO_DEDUPE("classifier"),
    BIO_AUTH("bioauth"),
    MANUAL_VERIFICATION("manualverification"),
    UIN_GENERATOR("uingenerator"),
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
