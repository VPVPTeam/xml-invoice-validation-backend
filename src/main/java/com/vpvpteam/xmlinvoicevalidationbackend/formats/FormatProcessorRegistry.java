package com.vpvpteam.xmlinvoicevalidationbackend.formats;

import com.vpvpteam.xmlinvoicevalidationbackend.exceptions.UnsupportedFormatException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public final class FormatProcessorRegistry {
    private final Map<String, FormatProcessor> processors;

    public FormatProcessorRegistry(List<FormatProcessor> processorList) {
        this.processors = processorList.stream()
                .collect(Collectors.toMap(FormatProcessor::getFormatName, Function.identity()));
    }

    public FormatProcessor getProcessorOrThrow(String format) {
        FormatProcessor processor = processors.get(format);

        if (processor == null) {
            throw new UnsupportedFormatException("Unsupported format: " + format
                    + ". Supported: " + processors.keySet());
        }

        return processor;
    }

    public void ensureFormatSupported(String format) {
        getProcessorOrThrow(format);
    }
}