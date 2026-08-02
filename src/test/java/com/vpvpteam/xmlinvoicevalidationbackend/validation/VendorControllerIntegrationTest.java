package com.vpvpteam.xmlinvoicevalidationbackend.validation;

import com.vpvpteam.xmlinvoicevalidationbackend.AbstractIntegrationTest;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.VendorEntity;
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

class VendorControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String VENDORS_URL = "/api/vendors";
    private static final String TAX_ID = "5211146938";
    private static final String NAME = "Goodyear Polska Sp. z o.o.";
    private static final String UPDATED_NAME = "Goodyear Polska UPDATED";
    private static final String UNKNOWN_TAX_ID = "0000000000";

    @Autowired
    private VendorRepository vendorRepository;

    @Test
    void getVendor_withExistingTaxId_returnsVendor() throws Exception {
        saveVendor(TAX_ID, NAME);

        mockMvc.perform(get(VENDORS_URL + "/" + TAX_ID)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.taxId").value(TAX_ID))
                .andExpect(jsonPath("$.name").value(NAME));
    }

    @Test
    void getVendor_withoutToken_returnsUnauthorized() throws Exception {
        saveVendor(TAX_ID, NAME);

        mockMvc.perform(get(VENDORS_URL + "/" + TAX_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getVendor_withUnknownTaxId_returnsNotFound() throws Exception {
        mockMvc.perform(get(VENDORS_URL + "/" + UNKNOWN_TAX_ID)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Vendor not found: " + UNKNOWN_TAX_ID));
    }

    @Test
    void createVendor_asAdmin_returnsCreatedVendor() throws Exception {
        createVendor(adminToken(), TAX_ID, NAME)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.taxId").value(TAX_ID))
                .andExpect(jsonPath("$.name").value(NAME));
    }

    @Test
    void createVendor_withExistingTaxId_returnsConflict() throws Exception {
        saveVendor(TAX_ID, NAME);

        createVendor(adminToken(), TAX_ID, "Another Name")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Vendor already exists")))
                .andExpect(jsonPath("$.message", containsString(TAX_ID)));
    }

    @Test
    void createVendor_withBlankName_returnsBadRequest() throws Exception {
        createVendor(adminToken(), TAX_ID, "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("name: Vendor name must not be blank"));
    }

    @Test
    void createVendor_asUser_returnsForbidden() throws Exception {
        createVendor(userToken(), TAX_ID, NAME)
                .andExpect(status().isForbidden());
    }

    @Test
    void updateVendor_asAdmin_returnsUpdatedVendor() throws Exception {
        saveVendor(TAX_ID, NAME);

        updateVendor(adminToken(), TAX_ID, UPDATED_NAME)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taxId").value(TAX_ID))
                .andExpect(jsonPath("$.name").value(UPDATED_NAME));
    }

    @Test
    void updateVendor_withUnknownTaxId_returnsNotFound() throws Exception {
        updateVendor(adminToken(), UNKNOWN_TAX_ID, UPDATED_NAME)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Vendor not found: " + UNKNOWN_TAX_ID));
    }

    @Test
    void updateVendor_asUser_returnsForbidden() throws Exception {
        saveVendor(TAX_ID, NAME);

        updateVendor(userToken(), TAX_ID, UPDATED_NAME)
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteVendor_asAdmin_removesVendor() throws Exception {
        saveVendor(TAX_ID, NAME);

        mockMvc.perform(delete(VENDORS_URL + "/" + TAX_ID)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(VENDORS_URL + "/" + TAX_ID)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Vendor not found: " + TAX_ID));
    }

    @Test
    void deleteVendor_asUser_returnsForbidden() throws Exception {
        saveVendor(TAX_ID, NAME);

        mockMvc.perform(delete(VENDORS_URL + "/" + TAX_ID)
                        .header("Authorization", "Bearer " + userToken()))
                .andExpect(status().isForbidden());
    }

    private ResultActions createVendor(String token, String taxId, String name) throws Exception {
        return mockMvc.perform(post(VENDORS_URL)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(vendorJson(taxId, name)));
    }

    private ResultActions updateVendor(String token, String taxId, String name) throws Exception {
        return mockMvc.perform(put(VENDORS_URL + "/" + taxId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(vendorJson(taxId, name)));
    }

    private String vendorJson(String taxId, String name) {
        return """
                { "taxId": "%s", "name": "%s" }
                """.formatted(taxId, name);
    }

    private void saveVendor(String taxId, String name) {
        VendorEntity vendor = new VendorEntity();
        vendor.setTaxId(taxId);
        vendor.setName(name);

        vendorRepository.save(vendor);
    }
}