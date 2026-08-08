package com.vpvpteam.xmlinvoicevalidationbackend.validation.model;

/**
 * Composite identity of an invoice: seller tax id + invoice number.
 * Single source of truth for the string form used as a key in the database and API.
 */
public record InvoiceId(String sellerTaxId, String invoiceNumber) {
    private static final String SEPARATOR = "|";
    public static final InvoiceId NONE = new InvoiceId("", "");

    public String value() {
        return sellerTaxId + SEPARATOR + invoiceNumber;
    }
}