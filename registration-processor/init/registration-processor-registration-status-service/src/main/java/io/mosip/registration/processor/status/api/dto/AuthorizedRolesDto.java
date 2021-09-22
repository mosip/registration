package io.mosip.registration.processor.status.api.dto;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;


@Component("authorizedRoles")
@ConfigurationProperties(prefix = "mosip.role.registration")
@Getter
@Setter
public class AuthorizedRolesDto {
//Internal auth delegate service controller

  private List<String> postauth;

  private List<String> getgetcertificate;

  ////Registration external status controller

  private List<String> postexternalstatussearch;

  private List<String> postpacketexternalstatus;

  ////Registartion Status controller

  private List<String> postsearch;

  private List<String> postlostridsearch;

  //Registration Sync controller

  private List<String> postsync;

  private List<String> postsyncV2;

}