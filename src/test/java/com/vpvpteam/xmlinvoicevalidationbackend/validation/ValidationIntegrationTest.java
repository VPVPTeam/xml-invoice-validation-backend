package com.vpvpteam.xmlinvoicevalidationbackend.validation;

import com.vpvpteam.xmlinvoicevalidationbackend.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Objects;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;

class ValidationIntegrationTest extends AbstractIntegrationTest {
    private static final String GOODYEAR_SELLER_TAX_ID = "5211146938";
    private static final String GOODYEAR_INVOICE_NUMBER = "5860135336";

    @Test
    void validate_withValidInvoice_returnsReportWithoutIssues() throws Exception {
        String token = adminToken();
        MockMultipartFile file = invoiceFile("Goodyear.xml");

        mockMvc.perform(multipart("/api/validation/validate")
                        .file(file)
                        .param("format", "KSEF")
                        .header("Authorization", "Bearer " + token))
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
                .andExpect(jsonPath("$.validationBatch.listOfVendorIds[0]").value(GOODYEAR_SELLER_TAX_ID))
                .andExpect(jsonPath("$.validationBatch.listOfInvoiceIds.length()").value(1))
                .andExpect(jsonPath("$.validationBatch.listOfInvoiceIds[0]")
                        .value(GOODYEAR_SELLER_TAX_ID + "|" + GOODYEAR_INVOICE_NUMBER));
    }

    private static final String INVALID_INVOICE_FILE = "Goodyear - Copy (2).xml";

    @Test
    void validate_withTechnicallyInvalidInvoice_returnsErrorWithTechnicalIssues() throws Exception {
        String token = adminToken();
        MockMultipartFile file = invoiceFile(INVALID_INVOICE_FILE);

        mockMvc.perform(multipart("/api/validation/validate")
                        .file(file)
                        .param("format", "KSEF")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.issues.length()").value(2))
                .andExpect(jsonPath("$.issues[*].stage", everyItem(is("TECHNICAL"))))
                .andExpect(jsonPath("$.issues[*].severity", everyItem(is("ERROR"))))
                .andExpect(jsonPath("$.issues[*].ruleKey", everyItem(is("TECH_MISSING_REQUIRED_FIELD"))))
                .andExpect(jsonPath("$.issues[*].fileName", everyItem(is(INVALID_INVOICE_FILE))))
                .andExpect(jsonPath("$.issues[*].fieldPath", containsInAnyOrder(
                        "Podmiot1.DaneIdentyfikacyjne.NIP",
                        "Fa.FaWiersz[0].P_12")))
                .andExpect(jsonPath("$.totalInvoices").value(1))
                .andExpect(jsonPath("$.validInvoices").value(0))
                .andExpect(jsonPath("$.invoicesWithIssues").value(1))
                .andExpect(jsonPath("$.duplicateInvoices").value(0))
                .andExpect(jsonPath("$.validationBatch.listOfInvoiceIds[0]")
                        .value("UNKNOWN|38294723948"));
    }

    private MockMultipartFile invoiceFile(String fileName) throws Exception {
        String path = "/invoices/" + fileName;
        byte[] content = Objects.requireNonNull(
                        getClass().getResourceAsStream(path), "Test resource not found: " + path)
                .readAllBytes();

        return new MockMultipartFile("file", fileName, "application/xml", content);
    }
}