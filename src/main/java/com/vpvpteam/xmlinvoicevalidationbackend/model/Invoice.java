package com.vpvpteam.xmlinvoicevalidationbackend.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Pola wymagane prawnie na fakturze VAT zgodnie z Art. 106e ust. 1
 * ustawy z dnia 11 marca 2004 r. o podatku od towarów i usług.
 *
 * Mapowanie XML KSeF → pola Java:
 *
 *   Fa/P_1              → issueDate           (data wystawienia)
 *   Fa/P_2              → invoiceNumber       (kolejny numer faktury)
 *   Podmiot1/NIP        → sellerTaxId         (NIP sprzedawcy)
 *   Podmiot1/PelnaNazwa → sellerName          (nazwa sprzedawcy)
 *   Podmiot1/Adres      → sellerAddress*      (adres sprzedawcy)
 *   Podmiot2/NIP|BrakID → buyerTaxId          (NIP nabywcy, jeśli dotyczy)
 *   Podmiot2/nazwa      → buyerName           (nazwa/imię i nazwisko nabywcy)
 *   Podmiot2/Adres      → buyerAddress*       (adres nabywcy)
 *   Fa/P_6              → saleDate            (data sprzedaży / dostawy / usługi)
 *   Fa/KodWaluty        → currency            (waluta)
 *   Fa/P_13_x           → totalNet            (suma netto wg stawek)
 *   Fa/P_14_x           → totalTax            (suma VAT wg stawek)
 *   Fa/P_15             → totalGross          (kwota należności ogółem)
 *   Fa/FaWiersze        → items               (pozycje faktury → InvoiceItem)
 */
@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Art. 106e ust. 1 pkt 1 ── data wystawienia (XML: Fa/P_1)
    @NotNull
    @Column(nullable = false)
    private LocalDate issueDate;

    // ── Art. 106e ust. 1 pkt 2 ── kolejny numer faktury (XML: Fa/P_2)
    @NotBlank
    @Column(nullable = false, unique = true)
    private String invoiceNumber;

    // ── Art. 106e ust. 1 pkt 3 ── NIP sprzedawcy (XML: Podmiot1/NIP)
    @NotBlank
    @Size(min = 10, max = 10)
    @Column(nullable = false, length = 10)
    private String sellerTaxId;

    // ── Art. 106e ust. 1 pkt 3 ── nazwa sprzedawcy (XML: Podmiot1/PelnaNazwa)
    @NotBlank
    @Column(nullable = false)
    private String sellerName;

    // ── Art. 106e ust. 1 pkt 3 ── adres sprzedawcy (XML: Podmiot1/Adres)
    @NotBlank
    @Column(nullable = false)
    private String sellerAddress;

    // ── Art. 106e ust. 1 pkt 5 ── NIP nabywcy (XML: Podmiot2/NIP lub BrakID)
    // Nullable – osoba fizyczna nieprowadząca działalności nie musi mieć NIP
    @Column(length = 10)
    private String buyerTaxId;

    // ── Art. 106e ust. 1 pkt 3 ── nazwa / imię i nazwisko nabywcy
    @NotBlank
    @Column(nullable = false)
    private String buyerName;

    // ── Art. 106e ust. 1 pkt 3 ── adres nabywcy (XML: Podmiot2/Adres)
    @NotBlank
    @Column(nullable = false)
    private String buyerAddress;

    // ── Art. 106e ust. 1 pkt 6 ── data dokonania/zakończenia dostawy/usługi (XML: Fa/P_6)
    @NotNull
    @Column(nullable = false)
    private LocalDate saleDate;

    // ── Art. 106e ust. 1 pkt 11 ── waluta (XML: Fa/KodWaluty)
    @NotBlank
    @Size(min = 3, max = 3)
    @Column(nullable = false, length = 3)
    private String currency;

    // ── Art. 106e ust. 1 pkt 10 ── suma netto z podziałem na stawki (XML: Fa/P_13_x)
    @NotNull
    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal totalNet;

    // ── Art. 106e ust. 1 pkt 10 ── suma podatku z podziałem na stawki (XML: Fa/P_14_x)
    @NotNull
    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal totalTax;

    // ── Art. 106e ust. 1 pkt 11 ── kwota należności ogółem (XML: Fa/P_15)
    @NotNull
    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal totalGross;

    // ── Art. 106e ust. 1 pkt 7–9 ── pozycje faktury (XML: Fa/FaWiersze/FaWiersz)
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceItem> items = new ArrayList<>();

    public Invoice() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getSellerTaxId() {
        return sellerTaxId;
    }

    public void setSellerTaxId(String sellerTaxId) {
        this.sellerTaxId = sellerTaxId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public String getSellerAddress() {
        return sellerAddress;
    }

    public void setSellerAddress(String sellerAddress) {
        this.sellerAddress = sellerAddress;
    }

    public String getBuyerTaxId() {
        return buyerTaxId;
    }

    public void setBuyerTaxId(String buyerTaxId) {
        this.buyerTaxId = buyerTaxId;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public String getBuyerAddress() {
        return buyerAddress;
    }

    public void setBuyerAddress(String buyerAddress) {
        this.buyerAddress = buyerAddress;
    }

    public LocalDate getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDate saleDate) {
        this.saleDate = saleDate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getTotalNet() {
        return totalNet;
    }

    public void setTotalNet(BigDecimal totalNet) {
        this.totalNet = totalNet;
    }

    public BigDecimal getTotalTax() {
        return totalTax;
    }

    public void setTotalTax(BigDecimal totalTax) {
        this.totalTax = totalTax;
    }

    public BigDecimal getTotalGross() {
        return totalGross;
    }

    public void setTotalGross(BigDecimal totalGross) {
        this.totalGross = totalGross;
    }

    public List<InvoiceItem> getItems() {
        return items;
    }

    public void setItems(List<InvoiceItem> items) {
        this.items = items;
    }

    public void addItem(InvoiceItem item) {
        items.add(item);
        item.setInvoice(this);
    }
}
