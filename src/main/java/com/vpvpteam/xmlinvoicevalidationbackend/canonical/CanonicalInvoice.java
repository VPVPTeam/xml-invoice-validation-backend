package com.vpvpteam.xmlinvoicevalidationbackend.canonical;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CanonicalInvoice {
    // Mandatory fields
    private InvoiceHeader header;
    private List<InvoiceLine> lines;
    private InvoiceTotals totals;

    // Optional fields
    private InvoiceOptionalFields optionalFields;
}
