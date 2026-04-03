package com.vpvpteam.xmlinvoicevalidationbackend.validation.validators;

import com.vpvpteam.xmlinvoicevalidationbackend.canonical.CanonicalInvoice;
import com.vpvpteam.xmlinvoicevalidationbackend.formats.ksef.KsefInvoiceMapper;
import com.vpvpteam.xmlinvoicevalidationbackend.formats.ksef.dto.KsefInvoiceXmlDto;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.Severity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.ValidationStage;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationIssue;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class KsefTechnicalValidator implements TechnicalValidator {

    private final KsefInvoiceMapper mapper;

    public KsefTechnicalValidator(KsefInvoiceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public TechnicalValidationOutput validate(KsefInvoiceXmlDto dto) {
        List<ValidationIssue> issues = new ArrayList<>();

        String sellerTaxId = safeSellerTaxId(dto);
        String invoiceNumber = safeInvoiceNumber(dto);
        String invoiceId = sellerTaxId + "|" + invoiceNumber;

        checkRequired(dto != null, "Faktura", invoiceId, sellerTaxId, invoiceNumber, issues);

        if (dto != null) {
            validateParty(dto.getSeller(), "Podmiot1", invoiceId, sellerTaxId, invoiceNumber, issues);
            validateParty(dto.getBuyer(), "Podmiot2", invoiceId, sellerTaxId, invoiceNumber, issues);
            validateBody(dto.getInvoiceBody(), invoiceId, sellerTaxId, invoiceNumber, issues);
        }

        // Если уже есть ERROR — не идём в маппинг
        if (issues.stream().anyMatch(i -> i.getSeverity() == Severity.ERROR)) {
            return TechnicalValidationOutput.failure(issues);
        }

        // 2) Попытка создать CanonicalInvoice
        try {
            CanonicalInvoice canonicalInvoice = mapper.toCanonical(dto);
            return new TechnicalValidationOutput(canonicalInvoice, issues);
        } catch (Exception ex) {
            issues.add(ValidationIssue.buildIssue(
                    invoiceNumber,
                    sellerTaxId,
                    ValidationStage.TECHNICAL,
                    Severity.ERROR,
                    "canonicalInvoice",
                    "TECH_CANONICAL_MAPPING_FAILED",
                    // FIXME: Это message, нужно вставить подходящий message
                    "ERROR | " + sellerTaxId + " | " + invoiceNumber
                            + " | Technical validation failed: cannot create CanonicalInvoice. "
                            + ex.getClass().getSimpleName() + ": " + ex.getMessage()
            ));
            return TechnicalValidationOutput.failure(issues);
        }
    }

    private void validateBody(KsefInvoiceXmlDto.InvoiceBody body,
                              String invoiceId,
                              String sellerTaxId,
                              String invoiceNumber,
                              List<ValidationIssue> issues) {
        checkRequired(body != null, "Fa", invoiceId, sellerTaxId, invoiceNumber, issues);
        if (body == null) return;

        checkRequired(notBlank(body.getInvoiceNumber()), "Fa.P_2", invoiceId, sellerTaxId, invoiceNumber, issues);
        checkRequired(notBlank(body.getIssueDate()), "Fa.P_1", invoiceId, sellerTaxId, invoiceNumber, issues);
        checkRequired(notBlank(body.getSaleDate()), "Fa.P_6", invoiceId, sellerTaxId, invoiceNumber, issues);

        checkRequired(notBlank(body.getCurrencyCode()), "Fa.KodWaluty", invoiceId, sellerTaxId, invoiceNumber, issues);
        checkRequired(body.getTotalNet() != null, "Fa.P_13_1", invoiceId, sellerTaxId, invoiceNumber, issues);
        checkRequired(body.getTotalTax() != null, "Fa.P_14_1", invoiceId, sellerTaxId, invoiceNumber, issues);
        checkRequired(body.getTotalGross() != null, "Fa.P_15", invoiceId, sellerTaxId, invoiceNumber, issues);

        List<KsefInvoiceXmlDto.InvoiceLine> lines = body.getLines();
        checkRequired(lines != null && !lines.isEmpty(), "Fa.FaWiersz", invoiceId, sellerTaxId, invoiceNumber, issues);
        if (lines == null) return;

        // FIXME: String p = "Fa.FaWiersz[" + i + "]";
        for (int i = 0; i < lines.size(); i++) {
            KsefInvoiceXmlDto.InvoiceLine line = lines.get(i);
            String p = "Fa.FaWiersz[" + i + "]";

            checkRequired(line != null, p, invoiceId, sellerTaxId, invoiceNumber, issues);
            if (line == null) continue;

            checkRequired(line.getLineNumber() > 0, p + ".NrWierszaFa", invoiceId, sellerTaxId, invoiceNumber, issues);
            checkRequired(notBlank(line.getProductName()), p + ".P_7", invoiceId, sellerTaxId, invoiceNumber, issues);
            checkRequired(notBlank(line.getUnitOfMeasure()), p + ".P_8A", invoiceId, sellerTaxId, invoiceNumber, issues);
            checkRequired(line.getQuantity() != null, p + ".P_8B", invoiceId, sellerTaxId, invoiceNumber, issues);
            checkRequired(line.getUnitNetPrice() != null, p + ".P_9A", invoiceId, sellerTaxId, invoiceNumber, issues);
            checkRequired(line.getNetValue() != null, p + ".P_11", invoiceId, sellerTaxId, invoiceNumber, issues);
            checkRequired(line.getTaxRate() != null, p + ".P_12", invoiceId, sellerTaxId, invoiceNumber, issues);
        }
    }

    private void validateParty(KsefInvoiceXmlDto.Party party,
                               String basePath,
                               String invoiceId,
                               String sellerTaxId,
                               String invoiceNumber,
                               List<ValidationIssue> issues) {
        checkRequired(party != null, basePath, invoiceId, sellerTaxId, invoiceNumber, issues);
        if (party == null) return;

        KsefInvoiceXmlDto.IdentificationData id = party.getIdentificationData();
        checkRequired(id != null, basePath + ".DaneIdentyfikacyjne", invoiceId, sellerTaxId, invoiceNumber, issues);
        if (id != null) {
            checkRequired(notBlank(id.getTaxId()), basePath + ".DaneIdentyfikacyjne.NIP", invoiceId, sellerTaxId, invoiceNumber, issues);
            checkRequired(notBlank(id.getName()), basePath + ".DaneIdentyfikacyjne.Nazwa", invoiceId, sellerTaxId, invoiceNumber, issues);
        }

        KsefInvoiceXmlDto.Address address = party.getAddress();
        checkRequired(address != null, basePath + ".Adres", invoiceId, sellerTaxId, invoiceNumber, issues);
        if (address != null) {
            checkRequired(notBlank(address.getCountryCode()), basePath + ".Adres.KodKraju", invoiceId, sellerTaxId, invoiceNumber, issues);
            checkRequired(notBlank(address.getAddressLine1()), basePath + ".Adres.AdresL1", invoiceId, sellerTaxId, invoiceNumber, issues);
        }
    }

    private void checkRequired(boolean condition,
                               String fieldPath,
                               String invoiceId,
                               String sellerTaxId,
                               String invoiceNumber,
                               List<ValidationIssue> issues) {
        if (!condition) {
            issues.add(ValidationIssue.buildIssue(
                    invoiceNumber,
                    sellerTaxId,
                    ValidationStage.TECHNICAL,
                    Severity.ERROR,
                    "TECH_MISSING_REQUIRED_FIELD",
                    fieldPath,
                    // FIXME: Это message, нужно вставить подходящий message
                    "ERROR | " + sellerTaxId + " | " + invoiceNumber
                            + " | Technical validation failed: field '" + fieldPath + "' is missing or invalid."
            ));
        }
    }

    private String safeSellerTaxId(KsefInvoiceXmlDto dto) {
        try {
            String value = dto.getSeller().getIdentificationData().getTaxId();
            return notBlank(value) ? value : "UNKNOWN";
        } catch (Exception ex) {
            return "UNKNOWN";
        }
    }

    private String safeInvoiceNumber(KsefInvoiceXmlDto dto) {
        try {
            String value = dto.getInvoiceBody().getInvoiceNumber();
            return notBlank(value) ? value : "UNKNOWN";
        } catch (Exception ex) {
            return "UNKNOWN";
        }
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}