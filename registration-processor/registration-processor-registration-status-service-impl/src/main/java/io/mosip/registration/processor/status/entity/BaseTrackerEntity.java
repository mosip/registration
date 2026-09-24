package io.mosip.registration.processor.status.entity;

import javax.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class BaseTrackerEntity {

    @Column(name = "transaction_id", nullable = false)
    @Id
    protected String transactionId;

    public BaseTrackerEntity() {
        super();
    }
}
