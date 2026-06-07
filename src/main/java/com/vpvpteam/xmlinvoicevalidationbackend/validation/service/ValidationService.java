package com.vpvpteam.xmlinvoicevalidationbackend.validation.service;

import com.vpvpteam.xmlinvoicevalidationbackend.formats.FormatProcessor;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.Severity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.ValidationStage;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.message.DuplicateIssueMessages;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.message.TechnicalIssueMessages;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationBatch;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationIssue;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationOutput;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.XmlFileData;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.validator.TechnicalValidationOutput;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service that validates a batch of XML invoice/invoices.
 * Orchestrates parsing, duplicate checks, technical validation and business validation.
 */
@Service
public final class ValidationService {
    private final Map<String, FormatProcessor> processors;
    private final ValidationPersistenceService persistenceService;

    /**
     * Runs validation for a batch of XML inputs and returns ValidationOutput.
     */
    public ValidationService(List<FormatProcessor> processorList, ValidationPersistenceService persistenceService) {
        this.processors = processorList.stream()
                .collect(Collectors.toMap(FormatProcessor::getFormatName,
                        Function.identity()));
        this.persistenceService = persistenceService;
    }

    public Set<String> getSupportedFormats() {
        return Collections.unmodifiableSet(processors.keySet());
    }

    public ValidationOutput validateBatch(List<XmlFileData> xmlFilesData, String format) {
        FormatProcessor processor = processors.get(format);

        ValidationBatch batch = new ValidationBatch();
        ValidationOutput output = new ValidationOutput(batch);

        List<ValidationIssue> allIssues = new ArrayList<>();
        Set<String> vendorIds = new LinkedHashSet<>();
        Set<String> invoiceIds = new LinkedHashSet<>();
        Set<String> seenInvoiceIdsInBatch = new HashSet<>();

        if (xmlFilesData == null || xmlFilesData.isEmpty()) {
            allIssues.add(new ValidationIssue(
                    "", "", "",
                    ValidationStage.TECHNICAL,
                    Severity.ERROR,
                    "TECH_EMPTY_BATCH",
                    "batch",
                    TechnicalIssueMessages.emptyBatch()
            ));

            batch.setListOfVendorIds(List.of());
            batch.setListOfInvoiceIds(List.of());
            output.setIssues(allIssues);
            output.prepareFinalReport();
            return output;
        }

        for (XmlFileData xmlFileData : xmlFilesData) {
            TechnicalValidationOutput techOutput;
            try {
                techOutput = processor.process(xmlFileData.xmlInputStream(), xmlFileData.fileName());
            } catch (Exception ex) {
                allIssues.add(new ValidationIssue(
                        xmlFileData.fileName(),
                        "",
                        "",
                        ValidationStage.TECHNICAL,
                        Severity.ERROR,
                        "TECH_XML_PARSE_ERROR",
                        "xml",
                        TechnicalIssueMessages.xmlParseError(xmlFileData.fileName(), ex)
                ));
                continue;
            }

            String sellerTaxId = techOutput.getSellerTaxId();
            String invoiceNumber = techOutput.getInvoiceNumber();
            String invoiceId = sellerTaxId + "|" + invoiceNumber;

            vendorIds.add(sellerTaxId);
            invoiceIds.add(invoiceId);

            if (!seenInvoiceIdsInBatch.add(invoiceId)) {
                allIssues.add(new ValidationIssue(
                        xmlFileData.fileName(),
                        invoiceNumber,
                        sellerTaxId,
                        ValidationStage.BUSINESS,
                        Severity.WARNING,
                        "DUPLICATE_IN_BATCH",
                        "invoiceId",
                        DuplicateIssueMessages.duplicateInBatch(sellerTaxId, invoiceNumber)
                ));
                output.increaseDuplicateInvoicesCount();
                continue;
            }

            if (techOutput.hasErrors()) {
                allIssues.addAll(techOutput.getIssues());
            }
        }

        batch.setListOfVendorIds(new ArrayList<>(vendorIds));
        batch.setListOfInvoiceIds(new ArrayList<>(invoiceIds));
        output.setIssues(allIssues);
        output.prepareFinalReport();

        persistenceService.save(output);

        return output;
    }
}