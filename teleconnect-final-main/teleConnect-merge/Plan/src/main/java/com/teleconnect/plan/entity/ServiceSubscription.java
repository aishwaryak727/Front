
package com.teleconnect.plan.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "service_subscription")
@Data
public class ServiceSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscriptionId")
    private Integer subscriptionId;

    @Column(name = "lineId")
    private Integer lineId;

    @Column(name = "planId")
    private Integer planId;

    @Column(name = "addOnId")
    private Integer addOnId;   

    @Column(name = "activationDate")
    private LocalDate activationDate;

    @Column(name = "expiryDate")
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "renewalType")
    private RenewalType renewalType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status = Status.A;  
    public enum RenewalType {
        AutoRenew, Manual
    }

    public enum Status {
        A, E, S
    }
}
