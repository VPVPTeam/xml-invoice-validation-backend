package com.vpvpteam.xmlinvoicevalidationbackend.canonical;

import java.util.List;

public class CanonicalInvoice {
    // Mandatory fields
    private InvoiceHeader header;
    private List<InvoiceLine> lines;
    private InvoiceTotals totals;

    // Optional fields
    private InvoiceOptionalFields optionalFields;
}
