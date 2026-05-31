package com.hethongtrongbanking.nienluancosonganh.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.hethongtrongbanking.nienluancosonganh.enums.Role;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users") // "user" bi cam trong postgreSQL nen phai users
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role; // ADMIN, ANALYST, VIEWER

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Cac ham bac buoc trong cua Spring Secutiry (UserDetails)

    // 1 nguoi dung co the co nhieu quyen -> dung Collection
    // GrantedAuthority nhan quyen han cua Spring Secutiry hieu
    // ham getAuthorities -> nguoi dung co nhung quyen han gi?
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    // Tai khoan co het han hop dong hong?
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    // Tai khoan da kich hoat chua?
    @Override
    public boolean isEnabled() {
        return this.isActive;
    }

    // Tai khoan co bi khoa vi phat khong?
    @Override
    public boolean isAccountNonLocked() {
        return this.isActive;
    }

    // mat khau het han hong?
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

}
