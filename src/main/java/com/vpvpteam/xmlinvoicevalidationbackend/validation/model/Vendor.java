package com.vpvpteam.xmlinvoicevalidationbackend.validation.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public final class Vendor {
    private Long id;

    @NotBlank
    @Size(max = 50)
    private String taxId;

    @NotBlank
    @Size(max = 255)
    private String name;
}