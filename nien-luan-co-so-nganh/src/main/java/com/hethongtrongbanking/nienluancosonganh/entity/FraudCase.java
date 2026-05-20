package com.hethongtrongbanking.nienluancosonganh.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import com.hethongtrongbanking.nienluancosonganh.Enum.FraudCaseStatus;

@Entity
@Table(name = "fraud_case")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    private Payment payment;

    // Vi pham rule nao, co the null neu AI bat
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fraud_rule_id")
    private FraudRule fraudRule;

    // Nhan vien nao da xu ly ho so nay, co the null neu chua ai xu ly
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    @Column(nullable = false)
    private String detectionLayer; // tang nao bat

    private Double riskScore;

    private String fraudType; // loai fraund
    private String patternMatched; // Mau vi pham (VD: high_frequency_spend, anomaly,...)
    private String reason;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FraudCaseStatus status;

    private String analystNote; // Ghi chu cua analyst

    private LocalDateTime resolvedAt; // Thoi gian xu ly xong

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = FraudCaseStatus.OPEN;
        }
    }

}
