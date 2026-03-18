package com.vpvpteam.xmlinvoicevalidationbackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Pozycja faktury – pola wymagane zgodnie z Art. 106e ust. 1 pkt 7–9
 * ustawy o VAT.
 *
 * Mapowanie XML KSeF → pola Java:
 *
 *   FaWiersz/NrWierszaFa → lineNumber     (numer wiersza)
 *   FaWiersz/P_7         → productName    (nazwa towaru / usługi)
 *   FaWiersz/P_8A        → unitOfMeasure  (jednostka miary)
 *   FaWiersz/P_8B        → quantity       (ilość)
 *   FaWiersz/P_9A        → unitNetPrice   (cena jednostkowa netto)
 *   FaWiersz/P_11        → netValue       (wartość netto pozycji)
 *   FaWiersz/P_12        → taxRate        (stawka VAT w %)
 */
@Entity
@Table(name = "invoice_items")
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    // ── Art. 106e ust. 1 pkt 7 ── numer wiersza (XML: NrWierszaFa)
    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer lineNumber;

    // ── Art. 106e ust. 1 pkt 7 ── nazwa towaru lub usługi (XML: P_7)
    @NotBlank
    @Column(nullable = false)
    private String productName;

    // ── Art. 106e ust. 1 pkt 8 ── jednostka miary (XML: P_8A)
    @NotBlank
    @Column(nullable = false)
    private String unitOfMeasure;

    // ── Art. 106e ust. 1 pkt 8 ── ilość (XML: P_8B)
    @NotNull
    @Positive
    @Column(nullable = false, precision = 16, scale = 6)
    private BigDecimal quantity;

    // ── Art. 106e ust. 1 pkt 9 ── cena jednostkowa netto (XML: P_9A)
    @NotNull
    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal unitNetPrice;

    // ── Art. 106e ust. 1 pkt 9 ── wartość netto pozycji (XML: P_11)
    @NotNull
    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal netValue;

    // ── Art. 106e ust. 1 pkt 9 ── stawka podatku VAT w % (XML: P_12)
    @NotNull
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;

    public InvoiceItem() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitNetPrice() {
        return unitNetPrice;
    }

    public void setUnitNetPrice(BigDecimal unitNetPrice) {
        this.unitNetPrice = unitNetPrice;
    }

    public BigDecimal getNetValue() {
        return netValue;
    }

    public void setNetValue(BigDecimal netValue) {
        this.netValue = netValue;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }
}
