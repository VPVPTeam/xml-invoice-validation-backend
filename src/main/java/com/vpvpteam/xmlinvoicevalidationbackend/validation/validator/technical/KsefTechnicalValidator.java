package com.vpvpteam.xmlinvoicevalidationbackend.validation.validator.technical;

import com.vpvpteam.xmlinvoicevalidationbackend.formats.ksef.KsefInvoiceMapper;
import com.vpvpteam.xmlinvoicevalidationbackend.formats.ksef.dto.KsefInvoiceXmlDto;
import com.vpvpteam.xmlinvoicevalidationbackend.util.FieldCheck;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.message.TechnicalIssueMessages;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.InvoiceId;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationIssue;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Technical validator for KSeF invoice DTO.
 * Checks required XML fields and then tries canonical mapping.
 */
@Component
public final class KsefTechnicalValidator implements TechnicalValidator<KsefInvoiceXmlDto> {
    private static final String CANONICAL_MAPPING_FAILED = "TECH_CANONICAL_MAPPING_FAILED";

    private final KsefInvoiceMapper mapper;

    public KsefTechnicalValidator(KsefInvoiceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public TechnicalValidationOutput validate(KsefInvoiceXmlDto dto, String xmlFileName) {
        if (dto == null) {
            TechnicalIssueCollector collector = new TechnicalIssueCollector(xmlFileName, InvoiceId.NONE);
            collector.addMissingField("Faktura");

            return TechnicalValidationOutput.failure(collector.issues());
        }

        InvoiceId invoiceId = new InvoiceId(dto.safeSellerTaxId(), dto.safeInvoiceNumber());
        TechnicalIssueCollector collector = new TechnicalIssueCollector(xmlFileName, invoiceId);

        validateParty(dto.getSeller(), "Podmiot1", collector);
        validateParty(dto.getBuyer(), "Podmiot2", collector);
        validateBody(dto.getInvoiceBody(), collector);

        // Mapper expects every required field to be present, so we stop before it.
        if (collector.hasErrors()) {
            return TechnicalValidationOutput.failure(collector.issues());
        }

        return mapToCanonical(dto, xmlFileName, invoiceId);
    }

    private TechnicalValidationOutput mapToCanonical(KsefInvoiceXmlDto dto, String xmlFileName, InvoiceId invoiceId) {
        try {
            return TechnicalValidationOutput.success(mapper.toCanonical(dto));
        } catch (Exception ex) {
            ValidationIssue issue = ValidationIssue.technicalError(
                    xmlFileName,
                    invoiceId,
                    CANONICAL_MAPPING_FAILED,
                    "canonicalInvoice",
                    TechnicalIssueMessages.canonicalMappingFailed(xmlFileName, ex)
            );

            return TechnicalValidationOutput.failure(List.of(issue));
        }
    }

    private void validateParty(KsefInvoiceXmlDto.Party party, String basePath, TechnicalIssueCollector collector) {
        collector.checkRequired(party != null, basePath);

        if (party == null) {
            return;
        }

        validateIdentificationData(party.getIdentificationData(), basePath, collector);
        validateAddress(party.getAddress(), basePath, collector);
    }

    private void validateIdentificationData(KsefInvoiceXmlDto.IdentificationData identificationData,
                                            String basePath,
                                            TechnicalIssueCollector collector) {
        collector.checkRequired(identificationData != null, basePath + ".DaneIdentyfikacyjne");

        if (identificationData == null) {
            return;
        }

        collector.checkRequired(FieldCheck.notNullNorBlank(identificationData.getTaxId()),
                basePath + ".DaneIdentyfikacyjne.NIP");
        collector.checkRequired(FieldCheck.notNullNorBlank(identificationData.getName()),
                basePath + ".DaneIdentyfikacyjne.Nazwa");
    }

    private void validateAddress(KsefInvoiceXmlDto.Address address, String basePath, TechnicalIssueCollector collector) {
        collector.checkRequired(address != null, basePath + ".Adres");

        if (address == null) {
            return;
        }

        collector.checkRequired(FieldCheck.notNullNorBlank(address.getCountryCode()),
                basePath + ".Adres.KodKraju");
        collector.checkRequired(FieldCheck.notNullNorBlank(address.getAddressLine1()),
                basePath + ".Adres.AdresL1");
    }

    private void validateBody(KsefInvoiceXmlDto.InvoiceBody body, TechnicalIssueCollector collector) {
        collector.checkRequired(body != null, "Fa");

        if (body == null) {
            return;
        }

        validateBodyHeader(body, collector);
        validateLines(body.getLines(), collector);
    }

    private void validateBodyHeader(KsefInvoiceXmlDto.InvoiceBody body, TechnicalIssueCollector collector) {
        collector.checkRequired(FieldCheck.notNullNorBlank(body.getInvoiceNumber()), "Fa.P_2");
        collector.checkRequired(FieldCheck.notNullNorBlank(body.getIssueDate()), "Fa.P_1");
        collector.checkRequired(FieldCheck.notNullNorBlank(body.getCurrencyCode()), "Fa.KodWaluty");
        collector.checkRequired(body.getTotalNet() != null, "Fa.P_13_1");
        collector.checkRequired(body.getTotalTax() != null, "Fa.P_14_1");
        collector.checkRequired(body.getTotalGross() != null, "Fa.P_15");
    }

    private void validateLines(List<KsefInvoiceXmlDto.InvoiceLine> lines, TechnicalIssueCollector collector) {
        collector.checkRequired(lines != null && !lines.isEmpty(), "Fa.FaWiersz");

        if (lines == null) {
            return;
        }

        for (int i = 0; i < lines.size(); i++) {
            validateLine(lines.get(i), "Fa.FaWiersz[" + i + "]", collector);
        }
    }

    private void validateLine(KsefInvoiceXmlDto.InvoiceLine line, String fieldPath, TechnicalIssueCollector collector) {
        collector.checkRequired(line != null, fieldPath);

        if (line == null) {
            return;
        }

        collector.checkRequired(line.getLineNumber() > 0, fieldPath + ".NrWierszaFa");
        collector.checkRequired(FieldCheck.notNullNorBlank(line.getProductName()), fieldPath + ".P_7");
        collector.checkRequired(FieldCheck.notNullNorBlank(line.getUnitOfMeasure()), fieldPath + ".P_8A");
        collector.checkRequired(line.getQuantity() != null, fieldPath + ".P_8B");
        collector.checkRequired(line.getUnitNetPrice() != null, fieldPath + ".P_9A");
        collector.checkRequired(line.getNetValue() != null, fieldPath + ".P_11");
        collector.checkRequired(line.getTaxRate() != null, fieldPath + ".P_12");
    }
}