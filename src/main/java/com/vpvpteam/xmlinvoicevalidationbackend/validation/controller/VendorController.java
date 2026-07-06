package com.vpvpteam.xmlinvoicevalidationbackend.validation.controller;

import com.vpvpteam.xmlinvoicevalidationbackend.exceptions.EntityNotFoundException;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.mapper.ValidationPersistenceMapper;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.Vendor;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.service.ValidationPersistenceService;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.service.ValidationQueryService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/api/vendors")
public final class VendorController {
    private final ValidationQueryService queryService;
    private final ValidationPersistenceService persistenceService;
    private final ValidationPersistenceMapper persistenceMapper;

    @GetMapping("/{taxId}")
    public Vendor getVendor(@PathVariable String taxId) {
        return persistenceMapper.toModel(
                queryService.getVendorByTaxId(taxId)
                        .orElseThrow(() -> new EntityNotFoundException("Vendor not found: " + taxId))
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Vendor createVendor(@Valid @RequestBody Vendor vendor) {
        return persistenceService.save(vendor);
    }

    @PutMapping("/{taxId}")
    public Vendor updateVendor(@PathVariable String taxId,
                               @Valid @RequestBody Vendor vendor) {
        vendor.setTaxId(taxId);
        return persistenceService.update(vendor);
    }

    @DeleteMapping("/{taxId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVendor(@PathVariable String taxId) {
        persistenceService.deleteVendor(taxId);
    }
}