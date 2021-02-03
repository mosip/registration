package io.mosip.registration.processor.packet.storage.dto;

public enum  ProviderStageName {

    PACKET_RECEIVER("packetreceiver"),
    PACKET_UPLOADER("packetuploader"),
    PACKET_VALIDATOR("packetvalidator"),
    OSI_VALIDATOR("osivalidator"),
    DEMO_DEDUPE("demodedupe"),
    BIO_DEDUPE("biodedupe"),
    MANUAL_VERIFICATION("manualverification"),
    UIN_GENERATOR("uingenerator");

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
