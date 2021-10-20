package io.mosip.registration.processor.transaction.api.dto;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component("authorizedTransactionRoles")
@ConfigurationProperties(prefix = "mosip.role.registration")
@Getter
@Setter
public class AuthorizedRolesDto {
 //Registration Transaction controller

    private List<String> getsearchrid;

}