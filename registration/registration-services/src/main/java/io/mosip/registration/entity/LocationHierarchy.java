package io.mosip.registration.entity;

import io.mosip.registration.entity.id.LocationHierarchyId;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(schema = "reg", name = "loc_hierarchy_list")
@IdClass(LocationHierarchyId.class)
@Data
public class LocationHierarchy extends RegistrationCommonFields implements Serializable {

    @Id
    @AttributeOverrides({
            @AttributeOverride(name = "hierarchyLevel", column = @Column(name = "hierarchy_level", nullable = false)),
            @AttributeOverride(name = "hierarchyLevelName", column = @Column(name = "hierarchy_level_name", nullable = false, length = 64)),
            @AttributeOverride(name = "langCode", column = @Column(name = "lang_code", nullable = false, length = 3)) })
    private int hierarchyLevel;
    private String hierarchyLevelName;
    private String langCode;
}
