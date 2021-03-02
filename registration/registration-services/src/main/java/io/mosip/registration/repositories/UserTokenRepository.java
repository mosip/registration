package io.mosip.registration.repositories;

import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;
import io.mosip.registration.entity.UserToken;

public interface UserTokenRepository extends BaseRepository<UserToken, String> {

    UserToken findByUsrIdAndUserDetailIsActiveTrue(String userId);

    UserToken findTopByTokenExpiryGreaterThanAndUserDetailIsActiveTrueOrderByTokenExpiryDesc(long currentTimeInSeconds);

    UserToken findTopByRtokenExpiryGreaterThanAndUserDetailIsActiveTrueOrderByRtokenExpiryDesc(long currentTimeInSeconds);

    void deleteByUsrId(String usrId);
}
