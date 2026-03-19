package com.vpvpteam.xmlinvoicevalidationbackend.canonical;
import java.util.List;

public class CanonicalInvoice {
    private InvoiceHeader header;
    private List<InvoiceLine> lines;
    private InvoiceTotals totals;
}
