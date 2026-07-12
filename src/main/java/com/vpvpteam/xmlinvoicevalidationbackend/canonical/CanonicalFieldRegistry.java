package com.vpvpteam.xmlinvoicevalidationbackend.canonical;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.Set;

public final class CanonicalFieldRegistry {

    private CanonicalFieldRegistry() {}

    public static final String HEADER_INVOICE_NUMBER = "header.invoiceNumber";
    public static final String HEADER_ISSUE_DATE = "header.issueDate";

    public static final String HEADER_SELLER_TAX_ID = "header.seller.taxId";
    public static final String HEADER_SELLER_NAME = "header.seller.name";
    public static final String HEADER_SELLER_ADDRESS_COUNTRY_CODE = "header.seller.address.countryCode";
    public static final String HEADER_SELLER_ADDRESS_LINE1 = "header.seller.address.addressLine1";

    public static final String HEADER_BUYER_TAX_ID = "header.buyer.taxId";
    public static final String HEADER_BUYER_NAME = "header.buyer.name";
    public static final String HEADER_BUYER_ADDRESS_COUNTRY_CODE = "header.buyer.address.countryCode";
    public static final String HEADER_BUYER_ADDRESS_LINE1 = "header.buyer.address.addressLine1";

    public static final String TOTALS_CURRENCY_CODE = "totals.currencyCode";
    public static final String TOTALS_TOTAL_NET = "totals.totalNet";
    public static final String TOTALS_TOTAL_TAX = "totals.totalTax";
    public static final String TOTALS_TOTAL_GROSS = "totals.totalGross";

    private static final Map<String, Function<CanonicalInvoice, String>> EXTRACTORS = buildExtractors();

    public static Function<CanonicalInvoice, String> getExtractor(String fieldPath) {
        return EXTRACTORS.get(fieldPath);
    }

    public static boolean isSupported(String fieldPath) {
        return EXTRACTORS.containsKey(fieldPath);
    }

    public static Set<String> getSupportedFieldPaths() {
        return EXTRACTORS.keySet();
    }

    private static Map<String, Function<CanonicalInvoice, String>> buildExtractors() {
        Map<String, Function<CanonicalInvoice, String>> map = new LinkedHashMap<>();
        registerHeaderFields(map);
        registerTotalsFields(map);
        registerOptionalFields(map);
        return Collections.unmodifiableMap(map);
    }

    private static void registerHeaderFields(Map<String, Function<CanonicalInvoice, String>> map) {
        map.put(HEADER_INVOICE_NUMBER, inv -> inv.getHeader().getInvoiceNumber());
        map.put(HEADER_ISSUE_DATE, inv -> inv.getHeader().getIssueDate());

        map.put(HEADER_SELLER_TAX_ID, inv -> inv.getHeader().getSeller().getTaxId());
        map.put(HEADER_SELLER_NAME, inv -> inv.getHeader().getSeller().getName());
        map.put(HEADER_SELLER_ADDRESS_COUNTRY_CODE, inv -> inv.getHeader().getSeller().getAddress().getCountryCode());
        map.put(HEADER_SELLER_ADDRESS_LINE1, inv -> inv.getHeader().getSeller().getAddress().getAddressLine1());

        map.put(HEADER_BUYER_TAX_ID, inv -> inv.getHeader().getBuyer().getTaxId());
        map.put(HEADER_BUYER_NAME, inv -> inv.getHeader().getBuyer().getName());
        map.put(HEADER_BUYER_ADDRESS_COUNTRY_CODE, inv -> inv.getHeader().getBuyer().getAddress().getCountryCode());
        map.put(HEADER_BUYER_ADDRESS_LINE1, inv -> inv.getHeader().getBuyer().getAddress().getAddressLine1());
    }

    private static void registerTotalsFields(Map<String, Function<CanonicalInvoice, String>> map) {
        map.put(TOTALS_CURRENCY_CODE, inv -> inv.getTotals().getCurrencyCode());
        map.put(TOTALS_TOTAL_NET, inv -> toString(inv.getTotals().getTotalNet()));
        map.put(TOTALS_TOTAL_TAX, inv -> toString(inv.getTotals().getTotalTax()));
        map.put(TOTALS_TOTAL_GROSS, inv -> toString(inv.getTotals().getTotalGross()));
    }

    private static void registerOptionalFields(Map<String, Function<CanonicalInvoice, String>> map) {
        // TODO: Добавить опциональные поля
    }

    private static String toString(Object value) {
        return value == null ? null : value.toString();
    }
}