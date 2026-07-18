package com.teleconnect.subscriber.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sim_line")
@Data
public class SimLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer lineId;

    @Column(nullable = false)
    private Integer accountId;

    @Column(nullable = false, unique = true, length = 20)
    private String msisdn;

    @Column(nullable = false, unique = true, length = 22)
    private String iccid;

    @Column(nullable = false)
    private LocalDate activationDate = LocalDate.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceType serviceType = ServiceType.VoiceData;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SimStatus status = SimStatus.Active;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum ServiceType { Voice, Data, VoiceData }
    public enum SimStatus   { Active, Deactivated, Suspended, PortedOut }
}