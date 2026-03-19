package com.vpvpteam.xmlinvoicevalidationbackend.canonical;
import java.time.LocalDate;

public class InvoiceHeader {
    private String invoiceNumber;
    private LocalDate issueDate;
    private LocalDate saleDate;
    private Party seller;
    private Party buyer;
}
