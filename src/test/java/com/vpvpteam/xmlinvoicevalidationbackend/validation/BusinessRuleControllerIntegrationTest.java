package com.vpvpteam.xmlinvoicevalidationbackend.validation;

import com.vpvpteam.xmlinvoicevalidationbackend.AbstractIntegrationTest;
import com.vpvpteam.xmlinvoicevalidationbackend.canonical.CanonicalFieldRegistry;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.BusinessRuleEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.VendorEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.RuleOperator;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.repository.BusinessRuleRepository;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.repository.VendorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BusinessRuleControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String RULES_URL = "/api/rules";
    private static final String BY_VENDOR_URL = RULES_URL + "/by-vendor?vendorTaxId=";

    private static final String VENDOR_TAX_ID = "5211146938";
    private static final String VENDOR_NAME = "Goodyear Polska Sp. z o.o.";

    private static final String RULE_KEY = "CURRENCY_MUST_BE_PLN";
    private static final String FIELD_PATH = "totals.currencyCode";
    private static final String EXPECTED_VALUE = "PLN";
    private static final String UPDATED_EXPECTED_VALUE = "EUR";
    private static final String CREATED_BY = "integration-test";

    private static final String OTHER_FIELD_PATH = "header.seller.address.countryCode";
    private static final String UNKNOWN_VENDOR_TAX_ID = "0000000000";
    private static final String UNKNOWN_RULE_KEY = "NO_SUCH_RULE";

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private BusinessRuleRepository businessRuleRepository;

    @Test
    void createRule_withValidData_returnsCreatedRule() throws Exception {
        saveVendor();

        createRule(VENDOR_TAX_ID, RULE_KEY, FIELD_PATH, RuleOperator.EQUALS, EXPECTED_VALUE)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.vendorTaxId").value(VENDOR_TAX_ID))
                .andExpect(jsonPath("$.ruleKey").value(RULE_KEY))
                .andExpect(jsonPath("$.fieldPath").value(FIELD_PATH))
                .andExpect(jsonPath("$.operator").value("EQUALS"))
                .andExpect(jsonPath("$.expectedValue").value(EXPECTED_VALUE))
                .andExpect(jsonPath("$.createdBy").value(CREATED_BY));
    }

    @Test
    void getRulesByVendor_returnsVendorRules() throws Exception {
        VendorEntity vendor = saveVendor();
        saveRule(vendor, RULE_KEY, FIELD_PATH);

        authorizedGet(BY_VENDOR_URL + VENDOR_TAX_ID)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].vendorTaxId").value(VENDOR_TAX_ID))
                .andExpect(jsonPath("$.data[0].ruleKey").value(RULE_KEY))
                .andExpect(jsonPath("$.data[0].fieldPath").value(FIELD_PATH))
                .andExpect(jsonPath("$.data[0].operator").value("EQUALS"))
                .andExpect(jsonPath("$.data[0].expectedValue").value(EXPECTED_VALUE));
    }

    @Test
    void getRulesByVendor_withUnknownVendor_returnsEmptyList() throws Exception {
        authorizedGet(BY_VENDOR_URL + UNKNOWN_VENDOR_TAX_ID)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message").value("No data found for your request"));
    }

    @Test
    void updateRule_withValidData_returnsUpdatedRule() throws Exception {
        VendorEntity vendor = saveVendor();
        saveRule(vendor, RULE_KEY, FIELD_PATH);

        updateRule(VENDOR_TAX_ID, RULE_KEY, FIELD_PATH, RuleOperator.EQUALS, UPDATED_EXPECTED_VALUE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendorTaxId").value(VENDOR_TAX_ID))
                .andExpect(jsonPath("$.ruleKey").value(RULE_KEY))
                .andExpect(jsonPath("$.expectedValue").value(UPDATED_EXPECTED_VALUE));
    }

    @Test
    void deleteRule_removesRule() throws Exception {
        VendorEntity vendor = saveVendor();
        saveRule(vendor, RULE_KEY, FIELD_PATH);

        mockMvc.perform(delete(RULES_URL + "?vendorTaxId=" + VENDOR_TAX_ID + "&ruleKey=" + RULE_KEY)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNoContent());

        authorizedGet(BY_VENDOR_URL + VENDOR_TAX_ID)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.message").value("No data found for your request"));
    }

    @Test
    void createRule_withUnknownVendor_returnsNotFound() throws Exception {
        createRule(UNKNOWN_VENDOR_TAX_ID, RULE_KEY, FIELD_PATH, RuleOperator.EQUALS, EXPECTED_VALUE)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Vendor not found: " + UNKNOWN_VENDOR_TAX_ID));
    }

    @Test
    void createRule_withExistingRuleKey_returnsConflict() throws Exception {
        VendorEntity vendor = saveVendor();
        saveRule(vendor, RULE_KEY, FIELD_PATH);

        createRule(VENDOR_TAX_ID, RULE_KEY, OTHER_FIELD_PATH, RuleOperator.EQUALS, "PL")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Rule with key '" + RULE_KEY + "' already exists for this vendor"));
    }

    @Test
    void createRule_withExistingFieldPath_returnsConflict() throws Exception {
        VendorEntity vendor = saveVendor();
        saveRule(vendor, RULE_KEY, FIELD_PATH);

        createRule(VENDOR_TAX_ID, "ANOTHER_RULE_KEY", FIELD_PATH, RuleOperator.EQUALS, EXPECTED_VALUE)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Rule for field path '" + FIELD_PATH + "' already exists for this vendor"));
    }

    @Test
    void createRule_withUnsupportedFieldPath_returnsBadRequest() throws Exception {
        saveVendor();

        ResultActions result = createRule(
                VENDOR_TAX_ID, RULE_KEY, "totals.currencyCooode", RuleOperator.EQUALS, EXPECTED_VALUE)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message",
                        containsString("Unsupported field path: 'totals.currencyCooode'")));

        for (String supportedPath : CanonicalFieldRegistry.getSupportedFieldPaths()) {
            result.andExpect(jsonPath("$.message", containsString(supportedPath)));
        }
    }

    @Test
    void createRule_withNonNumericValueForNumericOperator_returnsBadRequest() throws Exception {
        saveVendor();

        createRule(VENDOR_TAX_ID, RULE_KEY, "totals.totalGross", RuleOperator.GREATER_THAN, "NOT_A_NUMBER")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Invalid expected value 'NOT_A_NUMBER' for operator GREATER_THAN"));
    }

    @Test
    void createRule_withBlankRuleKey_returnsBadRequest() throws Exception {
        saveVendor();

        createRule(VENDOR_TAX_ID, "", FIELD_PATH, RuleOperator.EQUALS, EXPECTED_VALUE)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("ruleKey: Rule key must not be blank"));
    }

    @Test
    void updateRule_withUnknownRuleKey_returnsNotFound() throws Exception {
        saveVendor();

        updateRule(VENDOR_TAX_ID, UNKNOWN_RULE_KEY, FIELD_PATH, RuleOperator.EQUALS, EXPECTED_VALUE)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Rule not found: " + UNKNOWN_RULE_KEY));
    }

    @Test
    void getRulesByVendor_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(BY_VENDOR_URL + VENDOR_TAX_ID))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions createRule(String vendorTaxId,
                                     String ruleKey,
                                     String fieldPath,
                                     RuleOperator operator,
                                     String expectedValue) throws Exception {
        return mockMvc.perform(post(RULES_URL)
                .header("Authorization", "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(ruleJson(vendorTaxId, ruleKey, fieldPath, operator, expectedValue)));
    }

    private ResultActions updateRule(String vendorTaxId,
                                     String ruleKey,
                                     String fieldPath,
                                     RuleOperator operator,
                                     String expectedValue) throws Exception {
        return mockMvc.perform(put(RULES_URL + "/" + ruleKey)
                .header("Authorization", "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(ruleJson(vendorTaxId, ruleKey, fieldPath, operator, expectedValue)));
    }

    private String ruleJson(String vendorTaxId,
                            String ruleKey,
                            String fieldPath,
                            RuleOperator operator,
                            String expectedValue) {
        return """
                {
                  "vendorTaxId": "%s",
                  "ruleKey": "%s",
                  "fieldPath": "%s",
                  "operator": "%s",
                  "expectedValue": "%s",
                  "createdBy": "%s"
                }
                """.formatted(vendorTaxId, ruleKey, fieldPath, operator.name(), expectedValue, CREATED_BY);
    }

    private VendorEntity saveVendor() {
        VendorEntity vendor = new VendorEntity();
        vendor.setTaxId(VENDOR_TAX_ID);
        vendor.setName(VENDOR_NAME);

        return vendorRepository.save(vendor);
    }

    private void saveRule(VendorEntity vendor, String ruleKey, String fieldPath) {
        BusinessRuleEntity rule = new BusinessRuleEntity();
        rule.setVendor(vendor);
        rule.setRuleKey(ruleKey);
        rule.setFieldPath(fieldPath);
        rule.setOperator(RuleOperator.EQUALS);
        rule.setExpectedValue(EXPECTED_VALUE);
        rule.setCreatedBy(CREATED_BY);

        businessRuleRepository.save(rule);
    }
}