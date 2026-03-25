package com.vpvpteam.xmlinvoicevalidationbackend.canonical;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Party {
    private String taxId; // PodmiotX / DaneIdentyfikacyjne / NIP
    private String name; // PodmiotX / DaneIdentyfikacyjne / Nazwa
    private Address address; // PodmiotX / Adres
}
