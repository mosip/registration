package io.mosip.registration.processor.qc.users.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.MappedSuperclass;


@MappedSuperclass
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class BaseQcuserEntity<C> {
	@EmbeddedId
	protected C id;

	public C getId() {
		return id;
	}

	public void setId(C id) {
		this.id = id;
	}
}
