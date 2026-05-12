package com.vpvpteam.xmlinvoicevalidationbackend.validation.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Vendor {
    private Long id;
    private String taxId;
    private String name;
}