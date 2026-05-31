package com.hethongtrongbanking.nienluancosonganh.entity;

import java.time.LocalDateTime;

import com.hethongtrongbanking.nienluancosonganh.enums.PaymentStatus;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "transaction_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus oldStatus;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus newStatus;

    // Ai doi trang thai + Co the null do AI/hethong tu doi
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private User changedBy;

    private String reason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @PrePersist
    protected void onCreate() {
        changedAt = LocalDateTime.now();
    }

}
