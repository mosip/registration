package io.mosip.registration.repositories;

import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;
import io.mosip.registration.entity.UserToken;

public interface UserTokenRepository extends BaseRepository<UserToken, String> {

    UserToken findByUsrId(String userId);

    UserToken findByTokenExpiryGreaterThan(long currentTimeInSeconds);

    UserToken findByRtokenExpiryGreaterThan(long currentTimeInSeconds);
}
