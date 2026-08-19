package com.vpvpteam.xmlinvoicevalidationbackend.validation.validator.technical;

import com.vpvpteam.xmlinvoicevalidationbackend.formats.ksef.KsefInvoiceMapper;
import com.vpvpteam.xmlinvoicevalidationbackend.formats.ksef.dto.KsefInvoiceXmlDto;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.Severity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.ValidationStage;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.message.RuleKeys;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.InvoiceId;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationIssue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class KsefTechnicalValidatorTest {

    private static final String FILE_NAME = "sample-invoice.xml";
    private static final String SELLER_TAX_ID = "5211146938";
    private static final String INVOICE_NUMBER = "5860135336";

    private final KsefTechnicalValidator validator = new KsefTechnicalValidator(new KsefInvoiceMapper());

    @Test
    void validate_withCompleteInvoice_buildsCanonicalInvoice() {
        KsefInvoiceXmlDto dto = validDto();

        TechnicalValidationOutput output = validator.validate(dto, FILE_NAME);

        assertThat(output.hasErrors()).isFalse();
        assertThat(output.getIssues()).isEmpty();
        assertThat(output.getCanonicalInvoice()).isNotNull();
        assertThat(output.getInvoiceId()).isEqualTo(new InvoiceId(SELLER_TAX_ID, INVOICE_NUMBER));
    }

    @Test
    void validate_withCompleteInvoice_copiesValuesIntoCanonicalModel() {
        KsefInvoiceXmlDto dto = validDto();

        TechnicalValidationOutput output = validator.validate(dto, FILE_NAME);

        assertThat(output.getCanonicalInvoice().getHeader().getInvoiceNumber()).isEqualTo(INVOICE_NUMBER);
        assertThat(output.getCanonicalInvoice().getHeader().getSeller().getTaxId()).isEqualTo(SELLER_TAX_ID);
        assertThat(output.getCanonicalInvoice().getTotals().getCurrencyCode()).isEqualTo("PLN");
        assertThat(output.getCanonicalInvoice().getLines()).hasSize(1);
    }

    @Test
    void validate_withNullDto_reportsMissingInvoice() {
        TechnicalValidationOutput output = validator.validate(null, FILE_NAME);

        assertThat(output.getCanonicalInvoice()).isNull();
        assertThat(output.getInvoiceId()).isEqualTo(InvoiceId.NONE);
        assertThat(output.getIssues()).hasSize(1);
        assertThat(output.getIssues().get(0).getFieldPath()).isEqualTo("Faktura");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("missingRequiredFieldCases")
    void validate_withMissingRequiredField_reportsThatField(String fieldPath, Consumer<KsefInvoiceXmlDto> breakField) {
        KsefInvoiceXmlDto dto = validDto();
        breakField.accept(dto);

        TechnicalValidationOutput output = validator.validate(dto, FILE_NAME);

        assertThat(output.hasErrors()).isTrue();
        assertThat(output.getCanonicalInvoice()).isNull();
        assertThat(output.getIssues()).hasSize(1);

        ValidationIssue issue = output.getIssues().get(0);
        assertThat(issue.getFieldPath()).isEqualTo(fieldPath);
        assertThat(issue.getRuleKey()).isEqualTo(RuleKeys.TECH_MISSING_REQUIRED_FIELD);
        assertThat(issue.getSeverity()).isEqualTo(Severity.ERROR);
        assertThat(issue.getStage()).isEqualTo(ValidationStage.TECHNICAL);
        assertThat(issue.getFileName()).isEqualTo(FILE_NAME);
    }

    static Stream<Arguments> missingRequiredFieldCases() {
        return Stream.of(
                brokenField("Podmiot1", dto -> dto.setSeller(null)),
                brokenField("Podmiot1.DaneIdentyfikacyjne", dto -> dto.getSeller().setIdentificationData(null)),
                brokenField("Podmiot1.DaneIdentyfikacyjne.NIP", dto -> dto.getSeller().getIdentificationData().setTaxId("   ")),
                brokenField("Podmiot1.DaneIdentyfikacyjne.Nazwa", dto -> dto.getSeller().getIdentificationData().setName(null)),
                brokenField("Podmiot1.Adres", dto -> dto.getSeller().setAddress(null)),
                brokenField("Podmiot1.Adres.KodKraju", dto -> dto.getSeller().getAddress().setCountryCode(null)),
                brokenField("Podmiot1.Adres.AdresL1", dto -> dto.getSeller().getAddress().setAddressLine1("")),

                brokenField("Podmiot2", dto -> dto.setBuyer(null)),
                brokenField("Podmiot2.DaneIdentyfikacyjne.NIP", dto -> dto.getBuyer().getIdentificationData().setTaxId(null)),
                brokenField("Podmiot2.Adres.AdresL1", dto -> dto.getBuyer().getAddress().setAddressLine1(null)),

                brokenField("Fa", dto -> dto.setInvoiceBody(null)),
                brokenField("Fa.P_2", dto -> dto.getInvoiceBody().setInvoiceNumber("  ")),
                brokenField("Fa.P_1", dto -> dto.getInvoiceBody().setIssueDate(null)),
                brokenField("Fa.KodWaluty", dto -> dto.getInvoiceBody().setCurrencyCode(null)),
                brokenField("Fa.P_13_1", dto -> dto.getInvoiceBody().setTotalNet(null)),
                brokenField("Fa.P_14_1", dto -> dto.getInvoiceBody().setTotalTax(null)),
                brokenField("Fa.P_15", dto -> dto.getInvoiceBody().setTotalGross(null)),

                brokenField("Fa.FaWiersz", dto -> dto.getInvoiceBody().setLines(null)),
                brokenField("Fa.FaWiersz", dto -> dto.getInvoiceBody().setLines(new ArrayList<>())),
                brokenField("Fa.FaWiersz[0]", dto -> dto.getInvoiceBody().getLines().set(0, null)),
                brokenField("Fa.FaWiersz[0].NrWierszaFa", dto -> firstLine(dto).setLineNumber(0)),
                brokenField("Fa.FaWiersz[0].P_7", dto -> firstLine(dto).setProductName("")),
                brokenField("Fa.FaWiersz[0].P_8A", dto -> firstLine(dto).setUnitOfMeasure(null)),
                brokenField("Fa.FaWiersz[0].P_8B", dto -> firstLine(dto).setQuantity(null)),
                brokenField("Fa.FaWiersz[0].P_9A", dto -> firstLine(dto).setUnitNetPrice(null)),
                brokenField("Fa.FaWiersz[0].P_11", dto -> firstLine(dto).setNetValue(null)),
                brokenField("Fa.FaWiersz[0].P_12", dto -> firstLine(dto).setTaxRate(null))
        );
    }

    @Test
    void validate_withSeveralMissingFields_reportsEachOfThem() {
        KsefInvoiceXmlDto dto = validDto();
        dto.getSeller().getIdentificationData().setTaxId(null);
        firstLine(dto).setTaxRate(null);

        TechnicalValidationOutput output = validator.validate(dto, FILE_NAME);

        assertThat(output.getIssues())
                .hasSize(2)
                .extracting(ValidationIssue::getFieldPath)
                .containsExactlyInAnyOrder("Podmiot1.DaneIdentyfikacyjne.NIP", "Fa.FaWiersz[0].P_12");
    }

    @Test
    void validate_withUnidentifiableInvoice_fallsBackToUnknownIdentity() {
        KsefInvoiceXmlDto dto = validDto();
        dto.setSeller(null);
        dto.getInvoiceBody().setInvoiceNumber(null);

        TechnicalValidationOutput output = validator.validate(dto, FILE_NAME);

        assertThat(output.getInvoiceId()).isEqualTo(new InvoiceId("UNKNOWN", "UNKNOWN"));
        assertThat(output.getIssues())
                .extracting(ValidationIssue::getSellerTaxId)
                .containsOnly("UNKNOWN");
    }

    @Test
    void validate_withSecondLineBroken_reportsIndexOfThatLine() {
        KsefInvoiceXmlDto dto = validDto();
        dto.getInvoiceBody().getLines().add(line());
        dto.getInvoiceBody().getLines().get(1).setProductName(null);

        TechnicalValidationOutput output = validator.validate(dto, FILE_NAME);

        assertThat(output.getIssues()).hasSize(1);
        assertThat(output.getIssues().get(0).getFieldPath()).isEqualTo("Fa.FaWiersz[1].P_7");
    }

    private static Arguments brokenField(String fieldPath, Consumer<KsefInvoiceXmlDto> breakField) {
        return Arguments.of(fieldPath, breakField);
    }

    private static KsefInvoiceXmlDto.InvoiceLine firstLine(KsefInvoiceXmlDto dto) {
        return dto.getInvoiceBody().getLines().get(0);
    }

    private static KsefInvoiceXmlDto validDto() {
        KsefInvoiceXmlDto dto = new KsefInvoiceXmlDto();

        dto.setSeller(party(SELLER_TAX_ID, "Vendor A Sp. z o.o."));
        dto.setBuyer(party("7390203825", "Vendor B Sp. z o.o."));
        dto.setInvoiceBody(invoiceBody());

        return dto;
    }

    private static KsefInvoiceXmlDto.Party party(String taxId, String name) {
        KsefInvoiceXmlDto.IdentificationData identificationData = new KsefInvoiceXmlDto.IdentificationData();
        identificationData.setTaxId(taxId);
        identificationData.setName(name);

        KsefInvoiceXmlDto.Address address = new KsefInvoiceXmlDto.Address();
        address.setCountryCode("PL");
        address.setAddressLine1("ul. Testowa 1");

        KsefInvoiceXmlDto.Party party = new KsefInvoiceXmlDto.Party();
        party.setIdentificationData(identificationData);
        party.setAddress(address);

        return party;
    }

    private static KsefInvoiceXmlDto.InvoiceBody invoiceBody() {
        KsefInvoiceXmlDto.InvoiceBody body = new KsefInvoiceXmlDto.InvoiceBody();

        body.setInvoiceNumber(INVOICE_NUMBER);
        body.setIssueDate("2025-03-14");
        body.setCurrencyCode("PLN");
        body.setTotalNet(new BigDecimal("2602.52"));
        body.setTotalTax(new BigDecimal("598.58"));
        body.setTotalGross(new BigDecimal("3201.10"));
        body.setLines(new ArrayList<>(List.of(line())));

        return body;
    }

    private static KsefInvoiceXmlDto.InvoiceLine line() {
        KsefInvoiceXmlDto.InvoiceLine line = new KsefInvoiceXmlDto.InvoiceLine();

        line.setLineNumber(1);
        line.setProductName("Test product");
        line.setUnitOfMeasure("szt.");
        line.setQuantity(new BigDecimal("4"));
        line.setUnitNetPrice(new BigDecimal("650.63"));
        line.setNetValue(new BigDecimal("2602.52"));
        line.setTaxRate(new BigDecimal("23"));

        return line;
    }
}