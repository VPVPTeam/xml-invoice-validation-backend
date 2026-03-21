package com.vpvpteam.xmlinvoicevalidationbackend.canonical;
import jakarta.annotation.Nullable;

public class Address {
    // Mandatory fields
    private String countryCode; // PodmiotX / Adres / KodKraju
    private String addressLine1; // PodmiotX / Adres / AdresL1

    // Optional fields
    @Nullable
    private String addressLine2; // PodmiotX / Adres / AdresL2
}
