package com.hethongtrongbanking.nienluancosonganh.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.hethongtrongbanking.nienluancosonganh.enums.PaymentStatus;

// Bang quan trong nhat: Luu tru giao dich thanh toan
@Entity
@Table(name = "payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK -> CardInfo (The nao thuc hien giao dich nay)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cc_num", nullable = false)
    private CardInfo cardInfo;

    // FK -> ModelVersion (Giao dich nay bi AI nao cham diem, co the Null)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_version_id")
    private ModelVersion modelVersion;

    @Column(nullable = false)
    private BigDecimal amt; // So tien quet

    private String merchant; // Ten cua hang (VD: Shopee, Winmart)
    private String category; // Loai giao dich (VD: shopping_net, grocery)

    // Toa do GPS hien tai cua Khach hang luc quet the
    private Double lat;
    private Double lon;

    // Toa do GPS cua Cua hang
    // (Sau nay AI se dung toa do nay va toa do nha de tinh khoan cach di chuyen)
    private Double merchLat;
    private Double merchLon;

    private Long cityPop; // Dan so noi quet the
    private Long unixTime; // Thoi gian giao dich (Chuan Unix - AI rat thich dung cai nay)

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status; // Trang thai: PENDING, APPROVED, BLOCKED, UNDER_REVIEW

    private String statusReason; // Ly do (VD: "Sai CVV", "Diem AI qua cao")
    private String fraudType; // Phan loai loi (VD: AMOUNT_LIMIT)

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        // Mac dinh moi vao luon la PENDING
        if (status == null) {
            status = PaymentStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
