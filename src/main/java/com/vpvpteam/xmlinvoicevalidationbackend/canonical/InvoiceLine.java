package com.vpvpteam.xmlinvoicevalidationbackend.canonical;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class InvoiceLine {
    private int lineNumber; // Fa / FaWiersz / NrWierszaFa
    private String productName; // Fa / FaWiersz / P_7
    private String unitOfMeasure; // Fa / FaWiersz / P_8A
    private BigDecimal quantity; // Fa / FaWiersz / P_8B
    private BigDecimal unitNetPrice; // Fa / FaWiersz / P_9A
    private BigDecimal netValue; // Fa / FaWiersz / P_11
    private BigDecimal taxRate; // Fa / FaWiersz / P_12
}
