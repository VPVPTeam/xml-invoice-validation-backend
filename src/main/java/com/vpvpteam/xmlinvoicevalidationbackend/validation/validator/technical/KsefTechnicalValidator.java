package com.vpvpteam.xmlinvoicevalidationbackend.validation.validator.technical;

import com.vpvpteam.xmlinvoicevalidationbackend.canonical.CanonicalInvoice;
import com.vpvpteam.xmlinvoicevalidationbackend.formats.ksef.KsefInvoiceMapper;
import com.vpvpteam.xmlinvoicevalidationbackend.formats.ksef.dto.KsefInvoiceXmlDto;
import com.vpvpteam.xmlinvoicevalidationbackend.util.FieldCheck;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.Severity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.ValidationStage;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.message.TechnicalIssueMessages;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationIssue;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Technical validator for KSeF invoice DTO.
 * Checks required XML fields and then tries canonical mapping.
 */
@Component
public final class KsefTechnicalValidator implements TechnicalValidator<KsefInvoiceXmlDto> {
    private final KsefInvoiceMapper mapper;

    public KsefTechnicalValidator(KsefInvoiceMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Runs technical validation for Invoice DTO and returns output (success or failure)
     */
    @Override
    public TechnicalValidationOutput validate(KsefInvoiceXmlDto dto, String xmlFileName) {
        // 1) Create issue list
        //    We collect all problems here and return them in one output object.
        List<ValidationIssue> issues = new ArrayList<>();


        // 2) Validate DTO presence.
        //    If DTO is null, we create a technical issue.
        checkRequired(dto != null, xmlFileName, "Faktura", "", "", issues);
        if (dto == null) {
            return TechnicalValidationOutput.failure(issues);
        }

        // 3) Extract safe identifiers.
        //    If DTO is invalid/null, helper methods return "UNKNOWN" instead of throwing.
        String sellerTaxId = dto.safeSellerTaxId();
        String invoiceNumber = dto.safeInvoiceNumber();

        // 4) Validate nested sections only when DTO exists.
        validateParty(dto.getSeller(), xmlFileName, "Podmiot1", sellerTaxId, invoiceNumber, issues);
        validateParty(dto.getBuyer(), xmlFileName, "Podmiot2", sellerTaxId, invoiceNumber, issues);
        validateBody(dto.getInvoiceBody(), xmlFileName, sellerTaxId, invoiceNumber, issues);

        // 5) Fail before mapping:
        //    if at least one technical ERROR exists, do not run mapper.
        //    Mapper expects all required fields to be present.
        if (issues.stream().anyMatch(i -> i.getSeverity() == Severity.ERROR)) {
            return TechnicalValidationOutput.failure(issues);
        }

        // 6) Try mapping DTO to canonical invoice only after required-field checks pass above.
        //    On mapping exception, convert it into a technical issue.
        try {
            CanonicalInvoice canonicalInvoice = mapper.toCanonical(dto);
            return TechnicalValidationOutput.success(canonicalInvoice);
        } catch (Exception ex) {
            issues.add(new ValidationIssue(
                    xmlFileName,
                    invoiceNumber,
                    sellerTaxId,
                    ValidationStage.TECHNICAL,
                    Severity.ERROR,
                    "TECH_CANONICAL_MAPPING_FAILED",
                    "canonicalInvoice",
                    TechnicalIssueMessages.canonicalMappingFailed(xmlFileName, ex)
            ));
            return TechnicalValidationOutput.failure(issues);
        }
    }

    /**
     * Validates required fields in invoice body and invoice lines.
     */
    private void validateBody(KsefInvoiceXmlDto.InvoiceBody body,
                              String xmlFileName,
                              String sellerTaxId,
                              String invoiceNumber,
                              List<ValidationIssue> issues) {
        // 1) InvoiceBody node must exist.
        //    If absent, we stop InvoiceBody checks because there is nothing to inspect.
        checkRequired(body != null, xmlFileName, "Fa", sellerTaxId, invoiceNumber, issues);
        if (body == null) return;

        // 2) Validate required InvoiceBody fields:
        //    invoice identifiers/dates, currency, and totals block values. They must neither be blank nor null.
        checkRequired(FieldCheck.notNullNorBlank(body.getInvoiceNumber()), xmlFileName, "Fa.P_2", sellerTaxId, invoiceNumber, issues);
        checkRequired(FieldCheck.notNullNorBlank(body.getIssueDate()), xmlFileName, "Fa.P_1", sellerTaxId, invoiceNumber, issues);
        checkRequired(FieldCheck.notNullNorBlank(body.getCurrencyCode()), xmlFileName, "Fa.KodWaluty", sellerTaxId, invoiceNumber, issues);
        checkRequired(body.getTotalNet() != null, xmlFileName, "Fa.P_13_1", sellerTaxId, invoiceNumber, issues);
        checkRequired(body.getTotalTax() != null, xmlFileName, "Fa.P_14_1", sellerTaxId, invoiceNumber, issues);
        checkRequired(body.getTotalGross() != null, xmlFileName, "Fa.P_15", sellerTaxId, invoiceNumber, issues);

        // 3) Validate invoice lines list presence.
        //    Empty/missing line list (null) is treated as technical error.
        List<KsefInvoiceXmlDto.InvoiceLine> lines = body.getLines();
        checkRequired(lines != null && !lines.isEmpty(), xmlFileName, "Fa.FaWiersz", sellerTaxId, invoiceNumber, issues);
        if (lines == null) return;

        // 4) Validate each line item with indexed field path (FaWiersz[i]).
        // FIXME: String p = "Fa.FaWiersz[" + i + "]"; ГЛЯЕМ ПОТОМ, КОГДА БУДЕМ ВАЛИДИРОВАТЬ ЕСТЬ ЛИ ПОЛЯ "10", "20"... ИТД
        for (int i = 0; i < lines.size(); i++) {
            KsefInvoiceXmlDto.InvoiceLine line = lines.get(i);
            String fieldPath = "Fa.FaWiersz[" + i + "]";

            // 4.1) Ensure InvoiceLine node itself exists before checking its fields.
            checkRequired(line != null, xmlFileName, fieldPath, sellerTaxId, invoiceNumber, issues);
            if (line == null) continue;

            // 4.2) Validate required fields inside an InvoiceLine.
            //      They must neither be blank nor null.
            checkRequired(line.getLineNumber() > 0, xmlFileName, fieldPath + ".NrWierszaFa", sellerTaxId, invoiceNumber, issues);
            checkRequired(FieldCheck.notNullNorBlank(line.getProductName()), xmlFileName, fieldPath + ".P_7", sellerTaxId, invoiceNumber, issues);
            checkRequired(FieldCheck.notNullNorBlank(line.getUnitOfMeasure()), xmlFileName, fieldPath + ".P_8A", sellerTaxId, invoiceNumber, issues);
            checkRequired(line.getQuantity() != null, xmlFileName, fieldPath + ".P_8B", sellerTaxId, invoiceNumber, issues);
            checkRequired(line.getUnitNetPrice() != null, xmlFileName, fieldPath + ".P_9A", sellerTaxId, invoiceNumber, issues);
            checkRequired(line.getNetValue() != null, xmlFileName, fieldPath + ".P_11", sellerTaxId, invoiceNumber, issues);
            checkRequired(line.getTaxRate() != null, xmlFileName, fieldPath + ".P_12", sellerTaxId, invoiceNumber, issues);
        }
    }

    /**
     * Validates required fields for party block.
     */
    private void validateParty(KsefInvoiceXmlDto.Party party,
                               String xmlFileName,
                               String basePath,
                               String sellerTaxId,
                               String invoiceNumber,
                               List<ValidationIssue> issues) {
        // 1) Validate Party node existence (seller/buyer path is passed by caller).
        checkRequired(party != null, xmlFileName, basePath, sellerTaxId, invoiceNumber, issues);
        if (party == null) return;

        // 2) Validate identification section and mandatory identity values (NIP, Nazwa).
        KsefInvoiceXmlDto.IdentificationData id = party.getIdentificationData();
        checkRequired(id != null, xmlFileName, basePath + ".DaneIdentyfikacyjne", sellerTaxId, invoiceNumber, issues);
        if (id != null) {
            checkRequired(FieldCheck.notNullNorBlank(id.getTaxId()), xmlFileName, basePath + ".DaneIdentyfikacyjne.NIP", sellerTaxId, invoiceNumber, issues);
            checkRequired(FieldCheck.notNullNorBlank(id.getName()), xmlFileName, basePath + ".DaneIdentyfikacyjne.Nazwa", sellerTaxId, invoiceNumber, issues);
        }

        // 3) Validate address section and minimum required address fields.
        KsefInvoiceXmlDto.Address address = party.getAddress();
        checkRequired(address != null, xmlFileName, basePath + ".Adres", sellerTaxId, invoiceNumber, issues);
        if (address != null) {
            checkRequired(FieldCheck.notNullNorBlank(address.getCountryCode()), xmlFileName, basePath + ".Adres.KodKraju", sellerTaxId, invoiceNumber, issues);
            checkRequired(FieldCheck.notNullNorBlank(address.getAddressLine1()), xmlFileName, basePath + ".Adres.AdresL1", sellerTaxId, invoiceNumber, issues);
        }
    }

    /**
     * Checks if condition is true, otherwise adds technical issue
     */
    private void checkRequired(boolean condition,
                               String fileName,
                               String fieldPath,
                               String sellerTaxId,
                               String invoiceNumber,
                               List<ValidationIssue> issues) {
        // Converts any failed required-check into a technical issue object.
        if (!condition) {
            issues.add(new ValidationIssue(
                    fileName,
                    invoiceNumber,
                    sellerTaxId,
                    ValidationStage.TECHNICAL,
                    Severity.ERROR,
                    "TECH_MISSING_REQUIRED_FIELD",
                    fieldPath,
                    TechnicalIssueMessages.missingRequiredField(fieldPath)
            ));
        }
    }
}