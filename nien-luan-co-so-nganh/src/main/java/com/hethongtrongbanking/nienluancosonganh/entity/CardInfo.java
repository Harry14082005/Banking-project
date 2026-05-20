package com.hethongtrongbanking.nienluancosonganh.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "card_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardInfo {

    @Id
    @Column(unique = true, nullable = false)
    private String ccNum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // FK -> User

    @Column(nullable = false)
    private String full_name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String phone;

    private String dob;

    private Double homeLat;
    private Double homeLon;
    private Long cityPop;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

// Co the dung private Long userId; thay cho @Column(name = "user_id")
// khi do can querry them
// // Cách 1: Lưu ID thuần (Tư duy Database)
// private Long userId; // Chỉ lưu số 5, muốn biết tên user phải query thêm

// // Cách 2: Lưu Object (Tư duy OOP - Java) ← Cách mình dùng
// @ManyToOne
// @JoinColumn(name = "user_id")
// private User user; // JPA tự hiểu "user_id" là FK, nhưng Java xài được nguyên
// Object

// CardInfo card = cardInfoRepository.findById("4532...");
// Cách 1 (userId thuần) → Phải query thêm
// Long userId = card.getUserId();
// User user = userRepository.findById(userId); // Phải gọi thêm
// String name = user.getUsername();
// // Cách 2 (Object) → Gọi thẳng luôn
// String name = card.getUser().getUsername(); // Gọn hơn nhiều!
