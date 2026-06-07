package com.vpvpteam.xmlinvoicevalidationbackend.validation.controller;

import com.vpvpteam.xmlinvoicevalidationbackend.api.ListResponse;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.mapper.ValidationPersistenceMapper;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.BusinessRule;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.service.ValidationPersistenceService;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.service.ValidationQueryService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/rules")
public final class BusinessRuleController {
    private final ValidationQueryService queryService;
    private final ValidationPersistenceService persistenceService;
    private final ValidationPersistenceMapper persistenceMapper;

    @GetMapping("/by-vendor")
    public ListResponse<BusinessRule> getRulesByVendor(@RequestParam String vendorTaxId) {
        List<BusinessRule> rules = queryService.getRulesByVendorTaxId(vendorTaxId)
                .stream()
                .map(persistenceMapper::toModel)
                .toList();

        return ListResponse.of(rules);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createRule(@Valid @RequestBody BusinessRule rule) {
        persistenceService.save(rule);
    }

    @PutMapping("/{ruleKey}")
    public void updateRule(@PathVariable String ruleKey,
                           @Valid @RequestBody BusinessRule rule) {
        rule.setRuleKey(ruleKey);
        persistenceService.update(rule);
    }

    @DeleteMapping
    public void deleteRule(@RequestParam String vendorTaxId,
                           @RequestParam String ruleKey) {
        persistenceService.delete(vendorTaxId, ruleKey);
    }
}