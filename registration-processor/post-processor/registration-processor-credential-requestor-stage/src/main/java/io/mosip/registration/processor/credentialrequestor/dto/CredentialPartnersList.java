package io.mosip.registration.processor.credentialrequestor.dto;

import lombok.Data;

import java.util.List;

@Data
public class CredentialPartnersList {
    private List<CredentialPartner> partners;
}
