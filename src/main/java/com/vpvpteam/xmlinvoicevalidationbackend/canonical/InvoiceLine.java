package com.vpvpteam.xmlinvoicevalidationbackend.canonical;
import java.math.BigDecimal;

public class InvoiceLine {
    private int lineNumber;
    private String productName;
    private String unitOfMeasure;
    private BigDecimal quantity;
    private BigDecimal unitNetPrice;
    private BigDecimal netValue;
    private BigDecimal taxRate;
}
