package com.hethongtrongbanking.nienluancosonganh.entity;

import java.time.LocalDateTime;

import com.hethongtrongbanking.nienluancosonganh.Enum.FraudCaseStatus;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fraud_case_id", nullable = false)
    private FraudCase fraudCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyst_id", nullable = false)
    private User analyst;

    @Enumerated(EnumType.STRING)
    private FraudCaseStatus oldStatus;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FraudCaseStatus newStatus;

    @Column(nullable = false)
    private String action; // APPROVE, REJECT

    private String note;

    private String ipAddress; // IP de truy vet neu noi bo gian lan

    @Column(nullable = false, updatable = false)
    private LocalDateTime executedAt;

    @PrePersist
    protected void onCreate() {
        executedAt = LocalDateTime.now();
    }

}
