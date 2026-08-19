package com.vpvpteam.xmlinvoicevalidationbackend.validation.validator.business;

import com.vpvpteam.xmlinvoicevalidationbackend.canonical.Address;
import com.vpvpteam.xmlinvoicevalidationbackend.canonical.CanonicalFieldRegistry;
import com.vpvpteam.xmlinvoicevalidationbackend.canonical.CanonicalInvoice;
import com.vpvpteam.xmlinvoicevalidationbackend.canonical.InvoiceHeader;
import com.vpvpteam.xmlinvoicevalidationbackend.canonical.InvoiceTotals;
import com.vpvpteam.xmlinvoicevalidationbackend.canonical.Party;
import com.vpvpteam.xmlinvoicevalidationbackend.exceptions.UnsupportedFieldPathException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FieldValueExtractorTest {

    private final FieldValueExtractor extractor = new FieldValueExtractor();

    @Test
    void extract_withHeaderField_returnsValue() {
        CanonicalInvoice invoice = invoice();

        String value = extractor.extract(invoice, CanonicalFieldRegistry.HEADER_INVOICE_NUMBER);

        assertThat(value).isEqualTo("5860135336");
    }

    @Test
    void extract_withNestedField_returnsValue() {
        CanonicalInvoice invoice = invoice();

        String value = extractor.extract(invoice, CanonicalFieldRegistry.HEADER_SELLER_ADDRESS_COUNTRY_CODE);

        assertThat(value).isEqualTo("PL");
    }

    @Test
    void extract_withNumericField_returnsStringRepresentation() {
        CanonicalInvoice invoice = invoice();

        String value = extractor.extract(invoice, CanonicalFieldRegistry.TOTALS_TOTAL_GROSS);

        assertThat(value).isEqualTo("3201.10");
    }

    @Test
    void extract_withMissingValue_returnsNull() {
        CanonicalInvoice invoice = invoice();
        invoice.getTotals().setCurrencyCode(null);

        String value = extractor.extract(invoice, CanonicalFieldRegistry.TOTALS_CURRENCY_CODE);

        assertThat(value).isNull();
    }

    @Test
    void extract_withMissingIntermediateObject_returnsNull() {
        CanonicalInvoice invoice = invoice();
        invoice.getHeader().setSeller(null);

        String value = extractor.extract(invoice, CanonicalFieldRegistry.HEADER_SELLER_TAX_ID);

        assertThat(value).isNull();
    }

    @Test
    void extract_withUnsupportedFieldPath_throws() {
        CanonicalInvoice invoice = invoice();

        assertThatThrownBy(() -> extractor.extract(invoice, "header.unknownField"))
                .isInstanceOf(UnsupportedFieldPathException.class);
    }

    @Test
    void extract_withNullFieldPath_throws() {
        CanonicalInvoice invoice = invoice();

        assertThatThrownBy(() -> extractor.extract(invoice, null))
                .isInstanceOf(UnsupportedFieldPathException.class);
    }

    private static CanonicalInvoice invoice() {
        Address sellerAddress = new Address();
        sellerAddress.setCountryCode("PL");
        sellerAddress.setAddressLine1("ul. Testowa 1");

        Party seller = new Party();
        seller.setTaxId("5211146938");
        seller.setName("Vendor A Sp. z o.o.");
        seller.setAddress(sellerAddress);

        InvoiceHeader header = new InvoiceHeader();
        header.setInvoiceNumber("5860135336");
        header.setIssueDate("2025-03-14");
        header.setSeller(seller);

        InvoiceTotals totals = new InvoiceTotals();
        totals.setCurrencyCode("PLN");
        totals.setTotalNet(new BigDecimal("2602.52"));
        totals.setTotalTax(new BigDecimal("598.58"));
        totals.setTotalGross(new BigDecimal("3201.10"));

        CanonicalInvoice invoice = new CanonicalInvoice();
        invoice.setHeader(header);
        invoice.setTotals(totals);

        return invoice;
    }
}