package com.hethongtrongbanking.nienluancosonganh.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

//Tai khoan bank lien ket ttdung
@Entity
@Table(name = "bank_account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cc_num", nullable = false)
    private CardInfo cardInfo;

    @Column(nullable = false, unique = true)
    private String accountNumber; // STK

    @Column(nullable = false)
    private BigDecimal balance; // So du

    @Column(nullable = false)
    private String bankName;

    @Column(nullable = false)
    private Boolean isBlocked = false; // Tai khoan bi khoa khong

    private LocalDateTime blockedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
