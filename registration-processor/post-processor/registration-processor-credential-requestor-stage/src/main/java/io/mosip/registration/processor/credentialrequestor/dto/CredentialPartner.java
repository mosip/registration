package io.mosip.registration.processor.credentialrequestor.dto;

import lombok.Data;

import java.util.List;

@Data
public class CredentialPartner {
    private String id;
    private String partnerId;
    private String credentialType;
    private String template;
    private String appIdBasedCredentialIdSuffix;
    private List<String> process;
    private List<String> metaInfoFields;
}
