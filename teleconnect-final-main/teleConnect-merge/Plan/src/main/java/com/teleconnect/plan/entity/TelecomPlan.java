
package com.teleconnect.plan.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "telecom_plan")
@Data
public class TelecomPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "planId")
    private Integer planId;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private PlanType type;

    @Column(name = "dataGb")
    private BigDecimal dataGb;

    @Column(name = "voiceMinutes")
    private Integer voiceMinutes;

    @Column(name = "smsCount")
    private Integer smsCount;

    @Column(name = "validityDays")
    private Integer validityDays;

    @Column(name = "planPrice")
    private BigDecimal planPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PlanStatus status = PlanStatus.A;

    // ✅ ENUMS INSIDE CLASS (IMPORTANT)
    public enum PlanType {
        Prepaid, Postpaid
    }

    public enum PlanStatus {
        A, D, P
    }
}
