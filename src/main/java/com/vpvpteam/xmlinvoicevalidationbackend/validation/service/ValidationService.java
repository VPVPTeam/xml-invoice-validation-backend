package com.vpvpteam.xmlinvoicevalidationbackend.validation.service;

import com.vpvpteam.xmlinvoicevalidationbackend.formats.ksef.KsefInvoiceParser;
import com.vpvpteam.xmlinvoicevalidationbackend.formats.ksef.dto.KsefInvoiceXmlDto;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.Severity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.ValidationStage;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationIssue;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationOutput;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.validators.TechnicalValidationOutput;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.validators.TechnicalValidator;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

/**
 * Service that validates a batch of XML invoice/invoices.
 * Orchestrates parsing, duplicate checks, technical validation and business validation.
 */
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
     * Runs validation for a batch of XML inputs and returns ValidationResult.
     */
    public ValidationOutput validateBatch(List<InputStream> xmlInputs, String batchId) {
        // 1) Create ValidationResult object and attach batch id.
        ValidationOutput result = new ValidationOutput();
        result.setBatchId(batchId);

        // 2) Prepare accumulators for issues and summary id lists.
        List<ValidationIssue> allIssues = new ArrayList<>();
        Set<String> vendorIds = new LinkedHashSet<>();
        Set<String> invoiceIds = new LinkedHashSet<>();

        // 3) Keep a set of invoice ids seen in this batch for duplicate detection.
        Set<String> seenInvoiceIdsInBatch = new HashSet<>();

        // 4) Handle empty batch as a technical error and return early.
        if (xmlInputs == null || xmlInputs.isEmpty()) {
            allIssues.add(ValidationIssue.buildIssue(
                    "UNKNOWN",
                    "UNKNOWN",
                    ValidationStage.TECHNICAL,
                    Severity.ERROR,
                    "TECH_EMPTY_BATCH",
                    "batch",
                    ValidationIssue.messageTechEmptyBatch()
            ));

            result.setListOfVendorIds(List.of());
            result.setListOfInvoiceIds(List.of());
            result.setIssues(allIssues);
            result.resolveStatus();
            return result;
        }

        // 5) Validate each XML invoice independently and merge issues if present.
        for (InputStream xmlInput : xmlInputs) {
            // 5.1) Parse XML into DTO. On parse failure, add issue and continue with next file.
            KsefInvoiceXmlDto dto;
            try {
                dto = parser.parse(xmlInput);
            } catch (Exception ex) {
                allIssues.add(ValidationIssue.buildIssue(
                        "UNKNOWN",
                        "UNKNOWN",
                        ValidationStage.TECHNICAL,
                        Severity.ERROR,
                        "TECH_XML_PARSE_ERROR",
                        "xml",
                        ValidationIssue.messageTechXmlParseError(ex)
                ));
                continue;
            }

            // 5.2) Extract safe identifiers used for deduplication and reporting.
            String sellerTaxId = dto.safeSellerTaxId();
            String invoiceNumber = dto.safeInvoiceNumber();
            String invoiceId = sellerTaxId + "|" + invoiceNumber;

            // 5.3) Collect ids for batch-level summary.
            vendorIds.add(sellerTaxId);
            invoiceIds.add(invoiceId);

            // 5.4) Detect duplicates inside the same batch.
            //      Duplicate is a warning and does stop technical validation.
            if (!seenInvoiceIdsInBatch.add(invoiceId)) {
                allIssues.add(ValidationIssue.buildIssue(
                        invoiceNumber,
                        sellerTaxId,
                        ValidationStage.BUSINESS,
                        Severity.WARNING,
                        "DUPLICATE_IN_BATCH",
                        "invoiceId",
                        ValidationIssue.messageDuplicateInBatch(sellerTaxId, invoiceNumber)
                ));
                continue;
            }

            // 5.5) Run technical validator and merge all returned issues.
            TechnicalValidationOutput techOutput = technicalValidator.validate(dto);
            if (techOutput.getIssues() != null && !techOutput.getIssues().isEmpty()) {
                allIssues.addAll(techOutput.getIssues());
            }
        }

        // 6) Build final batch ValidationResult from accumulated data.
        result.setListOfVendorIds(new ArrayList<>(vendorIds));
        result.setListOfInvoiceIds(new ArrayList<>(invoiceIds));
        result.setIssues(allIssues);
        result.resolveStatus();

        return result;
    }
}