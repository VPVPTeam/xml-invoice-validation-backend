package com.vpvpteam.xmlinvoicevalidationbackend.formats.ksef.dto;

import com.vpvpteam.xmlinvoicevalidationbackend.util.FieldCheck;
import jakarta.annotation.Nullable;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * This class is just a Java copy of the KSeF XML structure.
 * We parse XML into this DTO first, then map it to our internal model.
 */
@Getter
@Setter
@JacksonXmlRootElement(localName = "Faktura")
public class KsefInvoiceXmlDto {
    @JacksonXmlProperty(localName = "Podmiot1")
    private Party seller;
    @JacksonXmlProperty(localName = "Podmiot2")
    private Party buyer;
    @JacksonXmlProperty(localName = "Podmiot3")
    @Nullable
    private Party thirdParty;

    /**
     * Main invoice body (Fa) with dates, totals and invoice lines.
     */
    @JacksonXmlProperty(localName = "Fa")
    private InvoiceBody invoiceBody;

    /**
     * Party block (seller / buyer / third party).
     */
    @Getter
    @Setter
    public static class Party {
        @JacksonXmlProperty(localName = "DaneIdentyfikacyjne")
        private IdentificationData identificationData;
        @JacksonXmlProperty(localName = "Adres")
        private Address address;
    }

    /**
     * Party identity data from XML.
     * NIP is kept as taxId in Java.
     */
    @Getter
    @Setter
    public static class IdentificationData {
        @JacksonXmlProperty(localName = "NIP")
        private String taxId;
        @JacksonXmlProperty(localName = "Nazwa")
        private String name;
    }

    /**
     * Party address.
     * AdresL2 is optional.
     */
    @Getter
    @Setter
    public static class Address {
        @JacksonXmlProperty(localName = "KodKraju")
        private String countryCode;
        @JacksonXmlProperty(localName = "AdresL1")
        private String addressLine1;
        @JacksonXmlProperty(localName = "AdresL2")
        @Nullable
        private String addressLine2;
    }

    /**
     * Invoice body fields:
     * currency, dates, invoice number, total net/tax/gross and invoice line list.
     */
    @Getter
    @Setter
    public static class InvoiceBody {
        @JacksonXmlProperty(localName = "KodWaluty")
        private String currencyCode;
        @JacksonXmlProperty(localName = "P_1")
        private String issueDate;
        @JacksonXmlProperty(localName = "P_2")
        private String invoiceNumber;
        @JacksonXmlProperty(localName = "P_6")
        private String saleDate;
        @JacksonXmlProperty(localName = "P_13_1")
        private BigDecimal totalNet;
        @JacksonXmlProperty(localName = "P_14_1")
        private BigDecimal totalTax;
        @JacksonXmlProperty(localName = "P_15")
        private BigDecimal totalGross;

        /**
         * FaWiersz comes as repeated XML elements (no wrapping container).
         */
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "FaWiersz")
        private List<InvoiceLine> lines;
    }

    /**
     * Single invoice line item:
     * line number, item name, quantity, unit price, line net value, tax rate.
     */
    @Getter
    @Setter
    public static class InvoiceLine {
        @JacksonXmlProperty(localName = "NrWierszaFa")
        private int lineNumber;
        @JacksonXmlProperty(localName = "P_7")
        private String productName;
        @JacksonXmlProperty(localName = "P_8A")
        private String unitOfMeasure;
        @JacksonXmlProperty(localName = "P_8B")
        private BigDecimal quantity;
        @JacksonXmlProperty(localName = "P_9A")
        private BigDecimal unitNetPrice;
        @JacksonXmlProperty(localName = "P_11")
        private BigDecimal netValue;
        @JacksonXmlProperty(localName = "P_12")
        private BigDecimal taxRate;
    }

    /**
     * Reads seller tax id safely; returns UNKNOWN on any read problem.
     */
    public String safeSellerTaxId() {
        try {
            String taxId = this.getSeller().getIdentificationData().getTaxId();
            return FieldCheck.notBlank(taxId) ? taxId : "UNKNOWN";
        } catch (Exception ex) {
            return "UNKNOWN";
        }
    }

    /**
     * Reads invoice number safely; returns UNKNOWN on any read problem.
     */
    public String safeInvoiceNumber() {
        try {
            String invoiceNumber = this.getInvoiceBody().getInvoiceNumber();
            return FieldCheck.notBlank(invoiceNumber) ? invoiceNumber : "UNKNOWN";
        } catch (Exception ex) {
            return "UNKNOWN";
        }
    }
}
