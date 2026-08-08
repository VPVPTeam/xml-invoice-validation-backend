package com.vpvpteam.xmlinvoicevalidationbackend.validation.controller;

import com.vpvpteam.xmlinvoicevalidationbackend.api.ListResponse;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.mapper.PersistenceMapper;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.BusinessRule;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.service.BusinessRulePersistenceService;
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
    private final BusinessRulePersistenceService ruleService;
    private final PersistenceMapper persistenceMapper;

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
    public BusinessRule createRule(@Valid @RequestBody BusinessRule rule) {
        return ruleService.save(rule);
    }

    @PutMapping("/{ruleKey}")
    public BusinessRule updateRule(@PathVariable String ruleKey,
                                   @Valid @RequestBody BusinessRule rule) {
        rule.setRuleKey(ruleKey);
        return ruleService.update(rule);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRule(@RequestParam String vendorTaxId,
                           @RequestParam String ruleKey) {
        ruleService.delete(vendorTaxId, ruleKey);
    }
}