package io.mosip.registration.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(schema = "reg", name = "user_token")
@Getter
@Setter
public class UserToken extends RegistrationCommonFields {

    @OneToOne
    @JoinColumn(name = "usr_id", nullable = false, insertable = false, updatable = false)
    private UserDetail userDetail;

    @Id
    @Column(name = "usr_id")
    private String usrId;
    @Column(name = "token")
    private String token;
    @Column(name = "refresh_token")
    private String refreshToken;
    @Column(name = "token_expiry")
    private long tokenExpiry;
    @Column(name = "rtoken_expiry")
    private long rtokenExpiry;
}
