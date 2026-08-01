package com.vpvpteam.xmlinvoicevalidationbackend.validation;

import com.jayway.jsonpath.JsonPath;
import com.vpvpteam.xmlinvoicevalidationbackend.AbstractIntegrationTest;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.BusinessRuleEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.VendorEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.RuleOperator;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.repository.BusinessRuleRepository;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.repository.VendorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ValidationIntegrationTest extends AbstractIntegrationTest {

    private static final String KSEF_FORMAT = "KSEF";
    private static final String UNKNOWN_BATCH_ID = "00000000-0000-0000-0000-000000000000";

    private static final InvoiceFixture GOODYEAR = new InvoiceFixture(
            "Goodyear.xml", "5211146938", "5860135336", "Goodyear Polska Sp. z o.o.");

    private static final InvoiceFixture GOODYEAR_DUPLICATE = new InvoiceFixture(
            "Goodyear - duplicate.xml", GOODYEAR.sellerTaxId(), GOODYEAR.invoiceNumber(), GOODYEAR.vendorName());

    private static final InvoiceFixture GOODYEAR_INVALID = new InvoiceFixture(
            "Goodyear - invalid.xml", "UNKNOWN", "38294723948", null);

    private static final InvoiceFixture MICHELIN = new InvoiceFixture(
            "Michelin.xml", "7390203825", "VD16004393", null);

    private static final InvoiceFixture NOKIAN = new InvoiceFixture(
            "Nokian.xml", "5213876026", "960152352", null);

    private static final InvoiceFixture SHELL = new InvoiceFixture(
            "Shell.xml", "5261009190", "9573462586", null);

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private BusinessRuleRepository businessRuleRepository;

    @Test
    void validate_withValidInvoice_returnsReportWithoutIssues() throws Exception {
        validate(GOODYEAR.fileName())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.issues").isEmpty())
                .andExpect(jsonPath("$.totalInvoices").value(1))
                .andExpect(jsonPath("$.validInvoices").value(1))
                .andExpect(jsonPath("$.invoicesWithIssues").value(0))
                .andExpect(jsonPath("$.duplicateInvoices").value(0))
                .andExpect(jsonPath("$.validationBatch.batchId").isNotEmpty())
                .andExpect(jsonPath("$.validationBatch.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.validationBatch.listOfVendorIds.length()").value(1))
                .andExpect(jsonPath("$.validationBatch.listOfVendorIds[0]").value(GOODYEAR.sellerTaxId()))
                .andExpect(jsonPath("$.validationBatch.listOfInvoiceIds.length()").value(1))
                .andExpect(jsonPath("$.validationBatch.listOfInvoiceIds[0]").value(GOODYEAR.invoiceId()));
    }

    @Test
    void validate_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(multipart("/api/validation/validate")
                        .file(invoiceFile(GOODYEAR.fileName()))
                        .param("format", KSEF_FORMAT))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validate_withTechnicallyInvalidInvoice_returnsErrorWithTechnicalIssues() throws Exception {
        validate(GOODYEAR_INVALID.fileName())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.issues.length()").value(2))
                .andExpect(jsonPath("$.issues[*].stage", everyItem(is("TECHNICAL"))))
                .andExpect(jsonPath("$.issues[*].severity", everyItem(is("ERROR"))))
                .andExpect(jsonPath("$.issues[*].ruleKey", everyItem(is("TECH_MISSING_REQUIRED_FIELD"))))
                .andExpect(jsonPath("$.issues[*].fileName", everyItem(is(GOODYEAR_INVALID.fileName()))))
                .andExpect(jsonPath("$.issues[*].fieldPath", containsInAnyOrder(
                        "Podmiot1.DaneIdentyfikacyjne.NIP",
                        "Fa.FaWiersz[0].P_12")))
                .andExpect(jsonPath("$.totalInvoices").value(1))
                .andExpect(jsonPath("$.validInvoices").value(0))
                .andExpect(jsonPath("$.invoicesWithIssues").value(1))
                .andExpect(jsonPath("$.duplicateInvoices").value(0))
                .andExpect(jsonPath("$.validationBatch.listOfInvoiceIds[0]")
                        .value(GOODYEAR_INVALID.invoiceId()));
    }

    @Test
    void validate_withViolatedBusinessRule_reportsOnlyViolatedRule() throws Exception {
        VendorEntity vendor = createVendor(GOODYEAR);
        createRule(vendor, "CURRENCY_MUST_BE_PLN", "totals.currencyCode", RuleOperator.EQUALS, "PLN");
        createRule(vendor, "TOTALS_LESS_THAN", "totals.totalGross", RuleOperator.LESS_THAN, "1000");

        validate(GOODYEAR.fileName())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WARNING"))
                .andExpect(jsonPath("$.issues.length()").value(1))
                .andExpect(jsonPath("$.issues[0].ruleKey").value("TOTALS_LESS_THAN"))
                .andExpect(jsonPath("$.issues[0].fieldPath").value("totals.totalGross"))
                .andExpect(jsonPath("$.issues[0].stage").value("BUSINESS"))
                .andExpect(jsonPath("$.issues[0].severity").value("WARNING"))
                .andExpect(jsonPath("$.issues[0].fileName").value(GOODYEAR.fileName()))
                .andExpect(jsonPath("$.issues[0].sellerTaxId").value(GOODYEAR.sellerTaxId()))
                .andExpect(jsonPath("$.issues[0].invoiceNumber").value(GOODYEAR.invoiceNumber()))
                .andExpect(jsonPath("$.issues[0].message", containsString("3201.10")))
                .andExpect(jsonPath("$.totalInvoices").value(1))
                .andExpect(jsonPath("$.validInvoices").value(0))
                .andExpect(jsonPath("$.invoicesWithIssues").value(1))
                .andExpect(jsonPath("$.duplicateInvoices").value(0));
    }

    @Test
    void validate_withMixedBatch_aggregatesResultsAcrossAllFiles() throws Exception {
        VendorEntity vendor = createVendor(GOODYEAR);
        createRule(vendor, "CURRENCY_MUST_BE_PLN", "totals.currencyCode", RuleOperator.EQUALS, "PLN");

        validate(GOODYEAR.fileName(), GOODYEAR_DUPLICATE.fileName(), GOODYEAR_INVALID.fileName(),
                MICHELIN.fileName(), NOKIAN.fileName(), SHELL.fileName())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.totalInvoices").value(5))
                .andExpect(jsonPath("$.validInvoices").value(4))
                .andExpect(jsonPath("$.invoicesWithIssues").value(1))
                .andExpect(jsonPath("$.duplicateInvoices").value(1))
                .andExpect(jsonPath("$.issues.length()").value(3))
                .andExpect(jsonPath("$.issues[*].ruleKey", containsInAnyOrder(
                        "TECH_MISSING_REQUIRED_FIELD",
                        "TECH_MISSING_REQUIRED_FIELD",
                        "DUPLICATE_IN_BATCH")))
                .andExpect(jsonPath("$.issues[*].stage", containsInAnyOrder(
                        "TECHNICAL", "TECHNICAL", "BUSINESS")))
                .andExpect(jsonPath("$.validationBatch.listOfInvoiceIds.length()").value(5))
                .andExpect(jsonPath("$.validationBatch.listOfInvoiceIds", containsInAnyOrder(
                        GOODYEAR.invoiceId(),
                        GOODYEAR_INVALID.invoiceId(),
                        MICHELIN.invoiceId(),
                        NOKIAN.invoiceId(),
                        SHELL.invoiceId())))
                .andExpect(jsonPath("$.validationBatch.listOfVendorIds.length()").value(5))
                .andExpect(jsonPath("$.validationBatch.listOfVendorIds", containsInAnyOrder(
                        GOODYEAR.sellerTaxId(),
                        GOODYEAR_INVALID.sellerTaxId(),
                        MICHELIN.sellerTaxId(),
                        NOKIAN.sellerTaxId(),
                        SHELL.sellerTaxId())));
    }

    @Test
    void validate_withUnsupportedFormat_returnsBadRequest() throws Exception {
        validateFiles("UNSUPPORTED_FORMAT", invoiceFile(GOODYEAR.fileName()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validate_withEmptyFile_returnsBadRequest() throws Exception {
        MockMultipartFile emptyFile =
                new MockMultipartFile("file", "empty.xml", "application/xml", new byte[0]);

        validateFiles(KSEF_FORMAT, emptyFile)
                .andExpect(status().isBadRequest());
    }

    @Test
    void validate_withMalformedXml_reportsParseErrorIssue() throws Exception {
        MockMultipartFile malformedFile = new MockMultipartFile("file", "broken.xml",
                "application/xml", "this is not xml".getBytes(StandardCharsets.UTF_8));

        validateFiles(KSEF_FORMAT, malformedFile)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.issues.length()").value(1))
                .andExpect(jsonPath("$.issues[0].ruleKey").value("TECH_XML_PARSE_ERROR"))
                .andExpect(jsonPath("$.issues[0].stage").value("TECHNICAL"))
                .andExpect(jsonPath("$.issues[0].severity").value("ERROR"))
                .andExpect(jsonPath("$.issues[0].fileName").value("broken.xml"));
    }

    @Test
    void getReport_withExistingBatch_returnsPersistedReport() throws Exception {
        String batchId = validateAndGetBatchId(GOODYEAR_INVALID.fileName());

        authorizedGet("/api/validation/report/" + batchId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validationBatch.batchId").value(batchId))
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.totalInvoices").value(1))
                .andExpect(jsonPath("$.validInvoices").value(0))
                .andExpect(jsonPath("$.invoicesWithIssues").value(1))
                .andExpect(jsonPath("$.issues.length()").value(2))
                .andExpect(jsonPath("$.issues[*].ruleKey", everyItem(is("TECH_MISSING_REQUIRED_FIELD"))))
                .andExpect(jsonPath("$.issues[*].fileName", everyItem(is(GOODYEAR_INVALID.fileName()))))
                .andExpect(jsonPath("$.validationBatch.listOfInvoiceIds[0]")
                        .value(GOODYEAR_INVALID.invoiceId()));
    }

    @Test
    void getReport_withUnknownBatch_returnsNotFound() throws Exception {
        authorizedGet("/api/validation/report/" + UNKNOWN_BATCH_ID)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Batch not found: " + UNKNOWN_BATCH_ID));
    }

    @Test
    void getBatchesByVendor_returnsBatchesContainingVendor() throws Exception {
        String batchId = validateAndGetBatchId(GOODYEAR.fileName());

        authorizedGet("/api/validation/batches/by-vendor?vendorId=" + GOODYEAR.sellerTaxId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].batchId").value(batchId))
                .andExpect(jsonPath("$.data[0].createdAt").isNotEmpty());
    }

    @Test
    void getBatchesByVendor_withUnknownVendor_returnsEmptyList() throws Exception {
        authorizedGet("/api/validation/batches/by-vendor?vendorId=0000000000")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message").value("No data found for your request"));
    }

    @Test
    void getIssuesByInvoice_returnsPersistedIssues() throws Exception {
        validate(GOODYEAR_INVALID.fileName()).andExpect(status().isOk());

        authorizedGet("/api/validation/issues/by-invoice"
                + "?sellerTaxId=" + GOODYEAR_INVALID.sellerTaxId()
                + "&invoiceNumber=" + GOODYEAR_INVALID.invoiceNumber())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[*].ruleKey", everyItem(is("TECH_MISSING_REQUIRED_FIELD"))))
                .andExpect(jsonPath("$.data[*].fileName", everyItem(is(GOODYEAR_INVALID.fileName()))))
                .andExpect(jsonPath("$.data[*].fieldPath", containsInAnyOrder(
                        "Podmiot1.DaneIdentyfikacyjne.NIP",
                        "Fa.FaWiersz[0].P_12")));
    }

    @Test
    void getBatchesByInvoice_returnsBatchesContainingInvoice() throws Exception {
        String batchId = validateAndGetBatchId(GOODYEAR.fileName());

        authorizedGet("/api/validation/batches/by-invoice"
                + "?sellerTaxId=" + GOODYEAR.sellerTaxId()
                + "&invoiceNumber=" + GOODYEAR.invoiceNumber())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].batchId").value(batchId));
    }

    private ResultActions validate(String... fileNames) throws Exception {
        MockMultipartFile[] files = new MockMultipartFile[fileNames.length];

        for (int i = 0; i < fileNames.length; i++) {
            files[i] = invoiceFile(fileNames[i]);
        }

        return validateFiles(KSEF_FORMAT, files);
    }

    private ResultActions validateFiles(String format, MockMultipartFile... files) throws Exception {
        var request = multipart("/api/validation/validate");

        for (MockMultipartFile file : files) {
            request.file(file);
        }

        return mockMvc.perform(request
                .param("format", format)
                .header("Authorization", "Bearer " + adminToken()));
    }

    private String validateAndGetBatchId(String... fileNames) throws Exception {
        String response = validate(fileNames)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonPath.read(response, "$.validationBatch.batchId");
    }

    private ResultActions authorizedGet(String url) throws Exception {
        return mockMvc.perform(get(url)
                .header("Authorization", "Bearer " + adminToken()));
    }

    private VendorEntity createVendor(InvoiceFixture invoice) {
        VendorEntity vendor = new VendorEntity();
        vendor.setTaxId(invoice.sellerTaxId());
        vendor.setName(invoice.vendorName());

        return vendorRepository.save(vendor);
    }

    private void createRule(VendorEntity vendor,
                            String ruleKey,
                            String fieldPath,
                            RuleOperator operator,
                            String expectedValue) {
        BusinessRuleEntity rule = new BusinessRuleEntity();
        rule.setVendor(vendor);
        rule.setRuleKey(ruleKey);
        rule.setFieldPath(fieldPath);
        rule.setOperator(operator);
        rule.setExpectedValue(expectedValue);
        rule.setCreatedBy("integration-test");

        businessRuleRepository.save(rule);
    }

    private MockMultipartFile invoiceFile(String fileName) throws Exception {
        String path = "/invoices/" + fileName;
        byte[] content = Objects.requireNonNull(
                        getClass().getResourceAsStream(path), "Test resource not found: " + path)
                .readAllBytes();

        return new MockMultipartFile("file", fileName, "application/xml", content);
    }

    private record InvoiceFixture(String fileName,
                                  String sellerTaxId,
                                  String invoiceNumber,
                                  String vendorName) {
        String invoiceId() {
            return sellerTaxId + "|" + invoiceNumber;
        }
    }
}