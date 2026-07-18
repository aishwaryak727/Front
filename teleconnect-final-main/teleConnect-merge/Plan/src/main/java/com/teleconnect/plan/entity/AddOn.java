

package com.teleconnect.plan.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "add_on")
@Data
public class AddOn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "addOnId")
    private Integer addOnId;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private AddOnType type;

    @Column(name = "quota")
    private BigDecimal quota;

    @Column(name = "validityDays")
    private Integer validityDays;

    @Column(name = "price")
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private AddOnStatus status = AddOnStatus.A;

    public enum AddOnType {
        DataTopup, ISDPack, RoamingPack, SMSPack
    }

    public enum AddOnStatus {
        A, I
    }
}
