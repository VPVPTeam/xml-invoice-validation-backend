package com.vpvpteam.xmlinvoicevalidationbackend.canonical;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public final class InvoiceTotals {
    private String currencyCode; // Fa / KodWaluty
    private BigDecimal totalNet; // Fa / P_13_1
    private BigDecimal totalTax; // Fa / P_14_1
    private BigDecimal totalGross; // Fa / P_15
}
