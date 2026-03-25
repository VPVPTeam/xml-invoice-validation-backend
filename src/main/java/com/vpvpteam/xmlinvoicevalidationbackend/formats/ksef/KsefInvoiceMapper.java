package com.vpvpteam.xmlinvoicevalidationbackend.formats.ksef;

import com.vpvpteam.xmlinvoicevalidationbackend.canonical.*;
import com.vpvpteam.xmlinvoicevalidationbackend.formats.ksef.dto.KsefInvoiceXmlDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KsefInvoiceMapper {
    // CanonicalInvoice creating
    public CanonicalInvoice toCanonical(KsefInvoiceXmlDto dto) {
        CanonicalInvoice invoice = new CanonicalInvoice();
        invoice.setHeader(mapHeader(dto));
        invoice.setLines(mapLines(dto.getInvoiceBody().getLines()));
        invoice.setTotals(mapTotals(dto.getInvoiceBody()));
        return invoice;
    }

    // InvoiceHeader mapping
    private InvoiceHeader mapHeader(KsefInvoiceXmlDto dto) {
        KsefInvoiceXmlDto.InvoiceBody body = dto.getInvoiceBody();
        InvoiceHeader header = new InvoiceHeader();
        header.setInvoiceNumber(body.getInvoiceNumber());
        header.setIssueDate(body.getIssueDate());
        header.setSaleDate(body.getSaleDate());
        header.setSeller(mapParty(dto.getSeller()));
        header.setBuyer(mapParty(dto.getBuyer()));
        header.setThirdParty(
                dto.getThirdParty() != null ? mapParty(dto.getThirdParty()) : null
        );
        return header;
    }

    // Party mapping
    private Party mapParty(KsefInvoiceXmlDto.Party dtoParty) {
        Party party = new Party();
        party.setTaxId(dtoParty.getIdentificationData().getTaxId());
        party.setName(dtoParty.getIdentificationData().getName());
        party.setAddress(mapAddress(dtoParty.getAddress()));
        return party;
    }

    // Address mapping
    private Address mapAddress(KsefInvoiceXmlDto.Address dtoAddress) {
        Address address = new Address();
        address.setCountryCode(dtoAddress.getCountryCode());
        address.setAddressLine1(dtoAddress.getAddressLine1());
        address.setAddressLine2(dtoAddress.getAddressLine2());
        return address;
    }

    // List<InvoiceLine> mapping
    private List<InvoiceLine> mapLines(List<KsefInvoiceXmlDto.InvoiceLine> dtoLines) {
        return dtoLines.stream()
                .map(this::mapLine)
                .toList();
    }

    //  InvoiceLine mapping
    private InvoiceLine mapLine(KsefInvoiceXmlDto.InvoiceLine dtoLine) {
        InvoiceLine line = new InvoiceLine();
        line.setLineNumber(dtoLine.getLineNumber());
        line.setProductName(dtoLine.getProductName());
        line.setUnitOfMeasure(dtoLine.getUnitOfMeasure());
        line.setQuantity(dtoLine.getQuantity());
        line.setUnitNetPrice(dtoLine.getUnitNetPrice());
        line.setNetValue(dtoLine.getNetValue());
        line.setTaxRate(dtoLine.getTaxRate());
        return line;
    }

    // InvoiceTotals mapping
    private InvoiceTotals mapTotals(KsefInvoiceXmlDto.InvoiceBody body) {
        InvoiceTotals totals = new InvoiceTotals();
        totals.setCurrencyCode(body.getCurrencyCode());
        totals.setTotalNet(body.getTotalNet());
        totals.setTotalTax(body.getTotalTax());
        totals.setTotalGross(body.getTotalGross());
        return totals;
    }
}
