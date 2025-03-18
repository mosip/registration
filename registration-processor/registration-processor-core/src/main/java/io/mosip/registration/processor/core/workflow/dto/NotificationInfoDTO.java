package io.mosip.registration.processor.core.workflow.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class NotificationInfoDTO implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 5493632810187324004L;

    private String name;
    private String phone;
    private String email;
    }