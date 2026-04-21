package com.vpvpteam.xmlinvoicevalidationbackend.canonical;
import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvoiceHeader {
    // Mandatory fields
    private String invoiceNumber; // Fa / P_2
    private String issueDate; // Fa / P_1
    private Party seller; // Podmiot1
    private Party buyer; // Podmiot2

    // Optional fields
    @Nullable
    private Party thirdParty; // Podmiot3
}