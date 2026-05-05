package com.vpvpteam.xmlinvoicevalidationbackend.validation.service;

import com.vpvpteam.xmlinvoicevalidationbackend.formats.ksef.KsefInvoiceParser;
import com.vpvpteam.xmlinvoicevalidationbackend.formats.ksef.dto.KsefInvoiceXmlDto;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.Severity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.ValidationStage;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationBatch;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationIssue;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationOutput;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.XmlFileData;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.validator.TechnicalValidationOutput;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.validator.TechnicalValidator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service that validates a batch of XML invoice/invoices.
 * Orchestrates parsing, duplicate checks, technical validation and business validation.
 */
@Service
@AllArgsConstructor
public final class ValidationService {
    private final ValidationPersistenceService persistenceService;
    private final KsefInvoiceParser parser;
    private final TechnicalValidator technicalValidator;

    /**
     * Runs validation for a batch of XML inputs and returns ValidationOutput.
     */
    public ValidationOutput validateBatch(List<XmlFileData> xmlFilesData) {
        // 1a) Create ValidationBatch object.
        ValidationBatch batch = new ValidationBatch();

        // 1b) Create ValidationOutput object and attach batch.
        ValidationOutput output = new ValidationOutput(batch);

        // 2) Prepare accumulators for issues and summary id lists.
        List<ValidationIssue> allIssues = new ArrayList<>();
        Set<String> vendorIds = new LinkedHashSet<>();
        Set<String> invoiceIds = new LinkedHashSet<>();

        // 3) Keep a set of invoice ids seen in this batch for duplicate detection.
        Set<String> seenInvoiceIdsInBatch = new HashSet<>();

        // 4) Handle empty batch as a technical error and return early.
        if (xmlFilesData == null || xmlFilesData.isEmpty()) {
            allIssues.add(ValidationIssue.buildIssue(
                    "",
                    "",
                    "",
                    ValidationStage.TECHNICAL,
                    Severity.ERROR,
                    "TECH_EMPTY_BATCH",
                    "batch",
                    ValidationIssue.messageTechEmptyBatch()
            ));

            batch.setListOfVendorIds(List.of());
            batch.setListOfInvoiceIds(List.of());
            output.setIssues(allIssues);
            output.prepareFinalReport();
            return output;
        }

        // 5) Validate each XML invoice independently and merge issues if present.
        for (XmlFileData xmlFileData : xmlFilesData) {
            // 5.1) Parse XML into DTO. On parse failure, add issue and continue with next file.
            KsefInvoiceXmlDto dto;
            try {
                dto = parser.parse(xmlFileData.xmlInputStream());
            } catch (Exception ex) {
                allIssues.add(ValidationIssue.buildIssue(
                        xmlFileData.fileName(),
                        "",
                        "",
                        ValidationStage.TECHNICAL,
                        Severity.ERROR,
                        "TECH_XML_PARSE_ERROR",
                        "xml",
                        ValidationIssue.messageTechXmlParseError(xmlFileData.fileName(), ex)
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
                        xmlFileData.fileName(),
                        invoiceNumber,
                        sellerTaxId,
                        ValidationStage.BUSINESS,
                        Severity.WARNING,
                        "DUPLICATE_IN_BATCH",
                        "invoiceId",
                        ValidationIssue.messageDuplicateInBatch(sellerTaxId, invoiceNumber)
                ));

                output.increaseDuplicateInvoicesCount();
                continue;
            }

            // 5.5) Run technical validator and merge all returned issues.
            TechnicalValidationOutput techOutput = technicalValidator.validate(dto, xmlFileData.fileName());
            if (techOutput.getIssues() != null && !techOutput.getIssues().isEmpty()) {
                allIssues.addAll(techOutput.getIssues());
            }
        }

        // 6) Build final batch ValidationOutput from accumulated data.
        batch.setListOfVendorIds(new ArrayList<>(vendorIds));
        batch.setListOfInvoiceIds(new ArrayList<>(invoiceIds));
        output.setIssues(allIssues);
        output.prepareFinalReport();

        persistenceService.save(output);

        return output;
    }
}