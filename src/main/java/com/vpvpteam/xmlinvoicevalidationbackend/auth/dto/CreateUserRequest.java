package com.vpvpteam.xmlinvoicevalidationbackend.auth.dto;

import com.vpvpteam.xmlinvoicevalidationbackend.auth.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email must be a valid address")
        @Size(max = 255)
        String email,

        @NotBlank(message = "Password must not be blank")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password,

        @NotNull(message = "Role must be provided")
        Role role) {
}