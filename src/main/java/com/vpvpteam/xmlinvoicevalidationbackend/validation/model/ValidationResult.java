package com.vpvpteam.xmlinvoicevalidationbackend.validation.model;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
@Getter
@Setter
@NoArgsConstructor
public class ValidationResult {
    private String batchId; // ID загрузки/пакета
    private String status; // "OK", "WARNING", "ERROR", "PARTIAL"
    // Список поставщиков, которые встретились в batch (например NIP)
    private List<String> vendorIds = new ArrayList<>();
    // Список invoiceId для дедупликации: sellerTaxId + "|" + invoiceNumber
    private List<String> invoiceIds = new ArrayList<>();
    // Все найденные проблемы по batch
    private List<ValidationIssue> issues = new ArrayList<>();
    // Полезные агрегаты
    private int totalInvoices;
    private int validInvoices;
    private int invalidInvoices;
    private int duplicateInvoices;
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    public ValidationResult(String batchId,
                            String status,
                            List<String> vendorIds,
                            List<String> invoiceIds,
                            List<ValidationIssue> issues,
                            int totalInvoices,
                            int validInvoices,
                            int invalidInvoices,
                            int duplicateInvoices,
                            OffsetDateTime createdAt) {
        this.batchId = batchId;
        this.status = status;
        this.vendorIds = vendorIds == null ? new ArrayList<>() : new ArrayList<>(vendorIds);
        this.invoiceIds = invoiceIds == null ? new ArrayList<>() : new ArrayList<>(invoiceIds);
        this.issues = issues == null ? new ArrayList<>() : new ArrayList<>(issues);
        this.totalInvoices = totalInvoices;
        this.validInvoices = validInvoices;
        this.invalidInvoices = invalidInvoices;
        this.duplicateInvoices = duplicateInvoices;
        this.createdAt = createdAt == null ? OffsetDateTime.now() : createdAt;
    }
}