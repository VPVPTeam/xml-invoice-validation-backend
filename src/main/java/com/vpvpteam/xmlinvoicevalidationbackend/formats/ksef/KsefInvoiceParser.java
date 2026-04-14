package com.vpvpteam.xmlinvoicevalidationbackend.formats.ksef;

import com.vpvpteam.xmlinvoicevalidationbackend.formats.ksef.dto.KsefInvoiceXmlDto;
import org.springframework.stereotype.Component;
import tools.jackson.dataformat.xml.XmlMapper;
import java.io.InputStream;

/**
 * Parses raw KSeF XML input into KsefInvoiceXmlDto.
 */
@Component
public final class KsefInvoiceParser {
    private final XmlMapper xmlMapper;

    public KsefInvoiceParser() {
        this.xmlMapper = new XmlMapper();
    }

    /**
     * Parses XML stream into DTO object.
     */
    public KsefInvoiceXmlDto parse(InputStream xmlInput) {
        return xmlMapper.readValue(xmlInput, KsefInvoiceXmlDto.class);
    }
}