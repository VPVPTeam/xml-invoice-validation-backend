package com.vpvpteam.xmlinvoicevalidationbackend.validation.controller;

import com.vpvpteam.xmlinvoicevalidationbackend.api.exceptions.ApiBadRequestException;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.XmlFileData;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class XmlFileDataReaderTest {

    private static final String XML_CONTENT = "<Faktura></Faktura>";

    @Test
    void read_withNullList_returnsEmptyList() {
        assertThat(XmlFileDataReader.read(null)).isEmpty();
    }

    @Test
    void read_withEmptyList_returnsEmptyList() {
        assertThat(XmlFileDataReader.read(List.of())).isEmpty();
    }

    @Test
    void read_withSeveralFiles_keepsOrderAndNames() {
        List<XmlFileData> result = XmlFileDataReader.read(List.of(
                xmlFile("Goodyear.xml"),
                xmlFile("Michelin.xml")
        ));

        assertThat(result)
                .extracting(XmlFileData::fileName)
                .containsExactly("Goodyear.xml", "Michelin.xml");
    }

    @Test
    void read_withValidFile_keepsContent() throws IOException {
        List<XmlFileData> result = XmlFileDataReader.read(List.of(xmlFile("Goodyear.xml")));

        byte[] content = result.get(0).xmlInputStream().readAllBytes();

        assertThat(new String(content, StandardCharsets.UTF_8)).isEqualTo(XML_CONTENT);
    }

    @Test
    void read_withoutOriginalFileName_fallsBackToUnknown() {
        MultipartFile fileWithoutName =
                new MockMultipartFile("file", "  ", "application/xml", XML_CONTENT.getBytes(StandardCharsets.UTF_8));

        List<XmlFileData> result = XmlFileDataReader.read(List.of(fileWithoutName));

        assertThat(result.get(0).fileName()).isEqualTo("UNKNOWN");
    }

    @Test
    void read_withEmptyFile_throwsBadRequestNamingThatFile() {
        MultipartFile emptyFile = new MockMultipartFile("file", "empty.xml", "application/xml", new byte[0]);

        assertThatThrownBy(() -> XmlFileDataReader.read(List.of(emptyFile)))
                .isInstanceOf(ApiBadRequestException.class)
                .hasMessageContaining("Empty XML file")
                .hasMessageContaining("empty.xml");
    }

    @Test
    void read_withNullFileInList_throwsBadRequest() {
        List<MultipartFile> filesWithNull = new ArrayList<>(Arrays.asList(xmlFile("Goodyear.xml"), null));

        assertThatThrownBy(() -> XmlFileDataReader.read(filesWithNull))
                .isInstanceOf(ApiBadRequestException.class)
                .hasMessageContaining("XML file is null");
    }

    @Test
    void read_whenFileCannotBeRead_throwsBadRequestNamingThatFile() {
        MultipartFile unreadableFile = new MockMultipartFile(
                "file", "broken.xml", "application/xml", XML_CONTENT.getBytes(StandardCharsets.UTF_8)) {

            @Override
            public byte[] getBytes() throws IOException {
                throw new IOException("stream closed");
            }
        };

        assertThatThrownBy(() -> XmlFileDataReader.read(List.of(unreadableFile)))
                .isInstanceOf(ApiBadRequestException.class)
                .hasMessageContaining("Cannot read XML file")
                .hasMessageContaining("broken.xml");
    }

    private static MockMultipartFile xmlFile(String fileName) {
        return new MockMultipartFile("file", fileName, "application/xml", XML_CONTENT.getBytes(StandardCharsets.UTF_8));
    }
}