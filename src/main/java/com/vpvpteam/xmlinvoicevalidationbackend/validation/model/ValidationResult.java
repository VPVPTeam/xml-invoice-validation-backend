package com.vpvpteam.xmlinvoicevalidationbackend.validation.model;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.Severity;
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

    private Severity status; // "OK", "WARNING", "ERROR"
    // Список поставщиков, которые встретились в batch (например NIP)
    private List<String> listOfVendorIds = new ArrayList<>();
    // Список invoiceId для дедупликации: sellerTaxId + "|" + invoiceNumber
    private List<String> listOfInvoiceIds = new ArrayList<>();
    // Все найденные проблемы по batch
    private List<ValidationIssue> issues = new ArrayList<>();
    // Полезные агрегаты
    private int totalInvoices;
    private int validInvoices;
    private int invalidInvoices;
    private int duplicateInvoices;
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    public ValidationResult(String batchId,
                            Severity status,
                            List<String> listOfVendorIds,
                            List<String> listOfInvoiceIds,
                            List<ValidationIssue> issues,
                            int totalInvoices,
                            int validInvoices,
                            int invalidInvoices,
                            int duplicateInvoices,
                            OffsetDateTime createdAt) {
        this.batchId = batchId;
        this.status = status;
        this.listOfVendorIds = listOfVendorIds == null ? new ArrayList<>() : new ArrayList<>(listOfVendorIds);
        this.listOfInvoiceIds = listOfInvoiceIds == null ? new ArrayList<>() : new ArrayList<>(listOfInvoiceIds);
        this.issues = issues == null ? new ArrayList<>() : new ArrayList<>(issues);
        this.totalInvoices = totalInvoices;
        this.validInvoices = validInvoices;
        this.invalidInvoices = invalidInvoices;
        this.duplicateInvoices = duplicateInvoices;
        this.createdAt = createdAt == null ? OffsetDateTime.now() : createdAt;
    }
}