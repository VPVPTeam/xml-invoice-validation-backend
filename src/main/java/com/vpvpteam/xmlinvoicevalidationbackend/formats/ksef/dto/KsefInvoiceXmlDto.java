package com.vpvpteam.xmlinvoicevalidationbackend.formats.ksef.dto;

import jakarta.annotation.Nullable;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

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
    @JacksonXmlProperty(localName = "Fa")
    private InvoiceBody invoiceBody;

    // Party (seller / buyer / third party)
    @Getter
    @Setter
    public static class Party {
        @JacksonXmlProperty(localName = "DaneIdentyfikacyjne")
        private IdentificationData identificationData;
        @JacksonXmlProperty(localName = "Adres")
        private Address address;
    }

    // IdentificationData
    @Getter
    @Setter
    public static class IdentificationData {
        @JacksonXmlProperty(localName = "NIP")
        private String taxId;
        @JacksonXmlProperty(localName = "Nazwa")
        private String name;
    }

    // Address
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

    // Invoice body
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
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "FaWiersz")
        private List<InvoiceLine> lines;
    }

    // Invoice line
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
}
