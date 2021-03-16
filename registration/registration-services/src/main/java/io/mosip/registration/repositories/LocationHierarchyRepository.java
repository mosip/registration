package io.mosip.registration.repositories;

import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;
import io.mosip.registration.entity.LocationHierarchy;
import io.mosip.registration.entity.id.LocationHierarchyId;

import java.util.List;

public interface LocationHierarchyRepository extends BaseRepository<LocationHierarchy, LocationHierarchyId> {

    List<LocationHierarchy> findAllByIsActiveTrueAndLangCode(String langCode);
}
