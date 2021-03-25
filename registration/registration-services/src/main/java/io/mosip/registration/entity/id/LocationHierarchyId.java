package io.mosip.registration.entity.id;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
@Data
public class LocationHierarchyId implements Serializable {

    @Column(name = "hierarchy_level", nullable = false, length = 36)
    private int hierarchyLevel;

    @Column(name = "hierarchy_level_name", nullable = false, length = 64)
    private String hierarchyLevelName;

    @Column(name = "lang_code", nullable = false, length = 3)
    private String langCode;
}
