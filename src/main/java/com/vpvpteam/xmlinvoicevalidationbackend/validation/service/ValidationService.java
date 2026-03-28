package com.vpvpteam.xmlinvoicevalidationbackend.validation.service;

import com.vpvpteam.xmlinvoicevalidationbackend.canonical.CanonicalInvoice;
import com.vpvpteam.xmlinvoicevalidationbackend.formats.ksef.KsefInvoiceMapper;
import com.vpvpteam.xmlinvoicevalidationbackend.formats.ksef.KsefInvoiceParser;
import com.vpvpteam.xmlinvoicevalidationbackend.formats.ksef.dto.KsefInvoiceXmlDto;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.Severity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.ValidationStage;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationIssue;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ValidationService {
    private static final String UNKNOWN = "UNKNOWN";
    private final KsefInvoiceParser parser;
    private final KsefInvoiceMapper mapper;

    public ValidationService(KsefInvoiceParser parser, KsefInvoiceMapper mapper) {
        this.parser = parser;
        this.mapper = mapper;
    }

    /**
     * Batch validation:
     * - парсинг XML -> DTO
     * - маппинг DTO -> CanonicalInvoice (technical validation)
     * - формирование ValidationResult + ValidationIssue
     */
    public ValidationResult validateBatch(List<InputStream> xmlInputs, String batchId) {
        ValidationResult result = new ValidationResult();
        result.setBatchId(batchId);
        List<ValidationIssue> issues = new ArrayList<>();
        Set<String> vendorIds = new LinkedHashSet<>();
        Set<String> invoiceIds = new LinkedHashSet<>();

        // для дублей внутри одного batch
        Set<String> seenInCurrentBatch = new HashSet<>();
        for (int i = 0; i < xmlInputs.size(); i++) {
            InputStream xmlInput = xmlInputs.get(i);
            KsefInvoiceXmlDto dto;
            try {
                dto = parser.parse(xmlInput);
            } catch (Exception ex) {
                issues.add(buildTechnicalIssue(
                        UNKNOWN,
                        UNKNOWN,
                        UNKNOWN,
                        Severity.ERROR,
                        "xml",
                        "Cannot parse XML: " + safeMessage(ex),
                        ""
                ));
                continue;
            }

            String sellerTaxId = extractSellerTaxId(dto);
            String invoiceNumber = extractInvoiceNumber(dto);
            String invoiceId = buildInvoiceId(sellerTaxId, invoiceNumber);
            vendorIds.add(sellerTaxId);
            invoiceIds.add(invoiceId);

            // Дубль в рамках текущего batch (warning)
            if (!seenInCurrentBatch.add(invoiceId)) {
                issues.add(buildBusinessIssue(
                        invoiceId,
                        sellerTaxId,
                        invoiceNumber,
                        Severity.WARNING,
                        "BIZ_DUPLICATE_IN_BATCH",
                        "invoiceId",
                        "Duplicate invoice in the same batch"
                ));
                // не прерываем, продолжаем technical validation
            }

            // Техническая валидация = попытка создать CanonicalInvoice
            try {
                CanonicalInvoice canonicalInvoice = mapper.toCanonical(dto);
                if (canonicalInvoice == null) {
                    issues.add(buildTechnicalIssue(
                            invoiceId,
                            sellerTaxId,
                            invoiceNumber,
                            Severity.ERROR,
                            "TECH_CANONICAL_MAPPING_FAILED",
                            "canonicalInvoice",
                            "CanonicalInvoice is null after mapping"
                    ));
                }
            } catch (Exception ex) {
                String fieldPath = tryExtractFieldPath(ex);
                issues.add(buildTechnicalIssue(
                        invoiceId,
                        sellerTaxId,
                        invoiceNumber,
                        Severity.ERROR,
                        "TECH_CANONICAL_MAPPING_FAILED",
                        fieldPath,
                        "Cannot create CanonicalInvoice: " + safeMessage(ex)
                ));
            }
        }

        result.setListOfVendorIds(new ArrayList<>(vendorIds));
        result.setListOfInvoiceIds(new ArrayList<>(invoiceIds));
        result.setIssues(issues);
        result.setStatus(resolveStatus(issues));
        // здесь подключите репозиторий, когда он появится:
        // validationResultRepository.save(result);

        // {PRINT: validationresult}
        return result;

    }

    private Severity resolveStatus(List<ValidationIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return Severity.OK;
        } else {
            return Severity.ISSUE;
        }
    }

    private ValidationIssue buildTechnicalIssue(
            String invoiceId,
            String sellerTaxId,
            String invoiceNumber,
            Severity severity,
            String ruleKey,
            String fieldPath,
            String details
    ) {
        ValidationIssue issue = new ValidationIssue();
        issue.setInvoiceId(invoiceId);
        issue.setSeverity(severity);
        issue.setStage(ValidationStage.TECHNICAL);
        issue.setRuleKey(ruleKey);
        issue.setFieldPath(fieldPath);
        // Формат, который вы просили
        issue.setMessage(String.format(
                "%s | %s | %s | Technical validation failed: field '%s' is missing/invalid or cannot be read. %s (rule: %s)",
                severity, sellerTaxId, invoiceNumber, fieldPath, details, ruleKey
        ));
        return issue;
    }

    private ValidationIssue buildBusinessIssue(
            String invoiceId,
            String sellerTaxId,
            String invoiceNumber,
            Severity severity,
            String ruleKey,
            String fieldPath,
            String details
    ) {
        ValidationIssue issue = new ValidationIssue();
        issue.setInvoiceId(invoiceId);
        issue.setSeverity(severity);
        issue.setStage(ValidationStage.BUSINESS);
        issue.setRuleKey(ruleKey);
        issue.setFieldPath(fieldPath);
        issue.setMessage(String.format(
                "%s | %s | %s | Field '%s' violates business rule. %s (rule: %s)",
                severity, sellerTaxId, invoiceNumber, fieldPath, details, ruleKey
        ));
        return issue;
    }

    private String extractSellerTaxId(KsefInvoiceXmlDto dto) {
        try {
            String taxId = dto.getSeller().getIdentificationData().getTaxId();
            return isBlank(taxId) ? UNKNOWN : taxId.trim();
        } catch (Exception ex) {
            return UNKNOWN;
        }
    }

    private String extractInvoiceNumber(KsefInvoiceXmlDto dto) {
        try {
            String invoiceNumber = dto.getInvoiceBody().getInvoiceNumber();
            return isBlank(invoiceNumber) ? UNKNOWN : invoiceNumber.trim();
        } catch (Exception ex) {
            return UNKNOWN;
        }
    }

    private String buildInvoiceId(String sellerTaxId, String invoiceNumber) {
        // можно без разделителя, но с "|" безопаснее для последующего разбора
        return sellerTaxId + "|" + invoiceNumber;
    }

    private String tryExtractFieldPath(Exception ex) {
        String msg = safeMessage(ex);
        // если mapper кидает "Missing required field: Fa.P_2", вытащим путь:
        String prefix = "Missing required field:";
        if (msg.startsWith(prefix)) {
            return msg.substring(prefix.length()).trim();
        }
        return "canonicalInvoice";
    }

    private String safeMessage(Exception ex) {
        String msg = ex.getMessage();
        return (msg == null || msg.isBlank()) ? ex.getClass().getSimpleName() : msg;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
