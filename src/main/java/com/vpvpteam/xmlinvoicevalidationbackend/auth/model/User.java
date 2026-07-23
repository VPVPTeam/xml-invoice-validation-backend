package com.vpvpteam.xmlinvoicevalidationbackend.auth.model;

import com.vpvpteam.xmlinvoicevalidationbackend.auth.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public final class User {
    private Long id;
    private String email;
    private Role role;
    private boolean active;
    private OffsetDateTime createdAt;
}