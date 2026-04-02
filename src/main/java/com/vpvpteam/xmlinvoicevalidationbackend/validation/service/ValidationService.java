package com.vpvpteam.xmlinvoicevalidationbackend.validation.service;

import com.vpvpteam.xmlinvoicevalidationbackend.formats.ksef.KsefInvoiceParser;
import com.vpvpteam.xmlinvoicevalidationbackend.formats.ksef.dto.KsefInvoiceXmlDto;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.Severity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.ValidationStage;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationIssue;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationResult;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.validators.TechnicalValidationOutput;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.validators.TechnicalValidator;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

@Service
public class ValidationService {

    private static final String UNKNOWN = "UNKNOWN";

    private final KsefInvoiceParser parser;
    private final TechnicalValidator technicalValidator;

    public ValidationService(KsefInvoiceParser parser, TechnicalValidator technicalValidator) {
        this.parser = parser;
        this.technicalValidator = technicalValidator;
    }

    /**
     * Batch validation:
     * 1) parse XML -> DTO
     * 2) technicalValidator.validate(dto) (он уже внутри может вызвать mapper)
     * 3) собираем общий ValidationResult по batch
     */
    public ValidationResult validateBatch(List<InputStream> xmlInputs, String batchId) {
        ValidationResult result = new ValidationResult();
        result.setBatchId(batchId);

        List<ValidationIssue> allIssues = new ArrayList<>();
        Set<String> vendorIds = new LinkedHashSet<>();
        Set<String> invoiceIds = new LinkedHashSet<>();

        // Нужно для определения дублей внутри одного batch
        Set<String> seenInvoiceIdsInBatch = new HashSet<>();

        if (xmlInputs == null || xmlInputs.isEmpty()) {
            // Пустой batch -> техническая ошибка
            allIssues.add(buildTechnicalIssue(
                    "UNKNOWN|UNKNOWN",
                    UNKNOWN,
                    UNKNOWN,
                    Severity.ERROR,
                    "TECH_EMPTY_BATCH",
                    "batch",
                    "Batch has no XML files to validate"
            ));

            result.setListOfVendorIds(List.of());
            result.setListOfInvoiceIds(List.of());
            result.setIssues(allIssues);
            result.setStatus(resolveStatus(allIssues));
            return result;
        }

        for (InputStream xmlInput: xmlInputs) {
            // 1) Parsing
            KsefInvoiceXmlDto dto;
            try {
                dto = parser.parse(xmlInput);
            } catch (Exception ex) {
                allIssues.add(buildTechnicalIssue(
                        "UNKNOWN|UNKNOWN",
                        UNKNOWN,
                        UNKNOWN,
                        Severity.ERROR,
                        "TECH_XML_PARSE_ERROR",
                        "xml",
                        "Cannot parse XML: " + safeMessage(ex)
                ));
                continue;
            }

            String sellerTaxId = extractSellerTaxId(dto);
            String invoiceNumber = extractInvoiceNumber(dto);
            String invoiceId = buildInvoiceId(sellerTaxId, invoiceNumber);

            // FIXME: vendorIds.add(sellerTaxId) - нужен ли
            vendorIds.add(sellerTaxId);
            invoiceIds.add(invoiceId);

            // 2) Duplicate check in current batch (warning)
            // Это не останавливает техническую валидацию
            if (!seenInvoiceIdsInBatch.add(invoiceId)) {
                allIssues.add(buildBusinessIssue(
                        invoiceId,
                        sellerTaxId,
                        invoiceNumber,
                        Severity.WARNING,
                        "BIZ_DUPLICATE_IN_BATCH",
                        "invoiceId",
                        "Duplicate invoice in the same batch"
                ));
            }

            // 3) Technical validation:
            // Важно: теперь сервис НЕ вызывает mapper напрямую.
            // Это ответственность TechnicalValidator.
            TechnicalValidationOutput techOutput = technicalValidator.validate(dto);
            if (techOutput.getIssues() != null && !techOutput.getIssues().isEmpty()) {
                allIssues.addAll(techOutput.getIssues());
            }
        }

        result.setListOfVendorIds(new ArrayList<>(vendorIds));
        result.setListOfInvoiceIds(new ArrayList<>(invoiceIds));
        result.setIssues(allIssues);
        result.setStatus(resolveStatus(allIssues));

        return result;
    }

    /**
     * Статус по вашим правилам:
     * - no issues -> OK
     * - есть хотя бы один ERROR -> ERROR
     * - иначе (есть WARNING) -> WARNING
     */
    private Severity resolveStatus(List<ValidationIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return Severity.OK;
        }

        boolean hasError = issues.stream().anyMatch(i -> i.getSeverity() == Severity.ERROR);
        if (hasError) {
            return Severity.ERROR;
        }

        boolean hasWarning = issues.stream().anyMatch(i -> i.getSeverity() == Severity.WARNING);
        return hasWarning ? Severity.WARNING : Severity.OK;
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
        return sellerTaxId + "|" + invoiceNumber;
    }

    private String safeMessage(Exception ex) {
        String msg = ex.getMessage();
        return (msg == null || msg.isBlank()) ? ex.getClass().getSimpleName() : msg;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}