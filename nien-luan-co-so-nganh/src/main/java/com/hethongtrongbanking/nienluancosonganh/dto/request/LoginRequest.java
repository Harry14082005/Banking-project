package com.hethongtrongbanking.nienluancosonganh.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Username khong duoc de trong")
    private String username;

    @NotBlank(message = "Password khong duoc de trong")
    private String password;
}
