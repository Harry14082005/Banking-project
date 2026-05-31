package com.hethongtrongbanking.nienluancosonganh.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Username khong duoc de trong")
    @Size(min = 3, max = 50, message = "Username phai tu 3-50 ky tu")
    private String username;

    @NotBlank(message = "Password khong duoc de trong")
    @Size(min = 6, message = "Password phai it nhat 6 ky tu")
    private String password;

    @NotBlank(message = "Email khong duoc de trong")
    @Email(message = "Email khong hop le")
    private String email;

    private String phone;
}
