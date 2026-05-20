package com.hethongtrongbanking.nienluancosonganh.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fraud_rule")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

// Cau hinh luat phat hien gian lan - hard rule

public class FraudRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String ruleName;

    @Column(nullable = false)
    private String ruleType; // amount limit, geo anomaly, velocity, time anomaly

    private BigDecimal thresholdValue; // Nguong vi pham

    private String description;

    @Column(nullable = false)
    private Boolean isActive = true; // luat nay co duoc bat hay khong

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
