package com.hethongtrongbanking.nienluancosonganh.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "model_version")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ModelVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String version;

    private LocalDateTime trainedAt;

    // So lan daon dung / Tong so lan doan
    private Double accuracy;

    // Bat dung trom/ Tong trom thuc te (quan trong nhat)
    private Double recall;

    // Bat dung trom / Tong so lan daon duoc bao la trom
    private Double precisionScore;

    @Column(nullable = false)
    private Boolean isActive = false; // phien ban nao duoc dung thuc te thi moi = true

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

}
