package com.vpvpteam.xmlinvoicevalidationbackend.validation.validator;

import com.vpvpteam.xmlinvoicevalidationbackend.canonical.CanonicalInvoice;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;

@Component
public final class FieldValueExtractor {

    // Маппинг: field_path из БД → функция, которая достаёт значение из CanonicalInvoice.
    // Каждая запись говорит: "если field_path = X, вызови функцию Y на invoice".
    // Все значения приводим к String, потому что expected_value в БД — тоже String.
    private static final Map<String, Function<CanonicalInvoice, String>> EXTRACTORS = Map.ofEntries(

            // TODO: Вынести
            //  Header
            Map.entry("header.invoiceNumber",
                    inv -> inv.getHeader().getInvoiceNumber()),

            Map.entry("header.issueDate",
                    inv -> inv.getHeader().getIssueDate()),

            // Header.seller
            Map.entry("header.seller.taxId",
                    inv -> inv.getHeader().getSeller().getTaxId()),

            Map.entry("header.seller.name",
                    inv -> inv.getHeader().getSeller().getName()),

            Map.entry("header.seller.address.countryCode",
                    inv -> inv.getHeader().getSeller().getAddress().getCountryCode()),

            Map.entry("header.seller.address.addressLine1",
                    inv -> inv.getHeader().getSeller().getAddress().getAddressLine1()),

            // Header.buyer
            Map.entry("header.buyer.taxId",
                    inv -> inv.getHeader().getBuyer().getTaxId()),

            Map.entry("header.buyer.name",
                    inv -> inv.getHeader().getBuyer().getName()),

            Map.entry("header.buyer.address.countryCode",
                    inv -> inv.getHeader().getBuyer().getAddress().getCountryCode()),

            Map.entry("header.buyer.address.addressLine1",
                    inv -> inv.getHeader().getBuyer().getAddress().getAddressLine1()),

            // Totals
            Map.entry("totals.currencyCode",
                    inv -> inv.getTotals().getCurrencyCode()),

            Map.entry("totals.totalNet",
                    inv -> toString(inv.getTotals().getTotalNet())),

            Map.entry("totals.totalTax",
                    inv -> toString(inv.getTotals().getTotalTax())),

            Map.entry("totals.totalGross",
                    inv -> toString(inv.getTotals().getTotalGross()))
    );

    /**
     * Извлекает значение поля из invoice по field_path.
     * Возвращает null если путь не поддерживается или значение отсутствует.
     */
    public String extract(CanonicalInvoice invoice, String fieldPath) {
        // Ищем функцию-экстрактор по ключу fieldPath
        Function<CanonicalInvoice, String> extractor = EXTRACTORS.get(fieldPath);

        // Если fieldPath не найден в маппинге — путь не поддерживается
        if (extractor == null) {
            return null;
        }

        // TODO: Отхэндлить
        try {
            // Вызываем функцию. try/catch ловит NullPointerException,
            // если промежуточный объект null (например header.seller = null).
            return extractor.apply(invoice);
        } catch (NullPointerException e) {
            // Поле существует в маппинге, но объект по пути = null.
            // Например: правило на "header.seller.taxId", а seller = null.
            return null;
        }
    }

    /**
     * BigDecimal → String. Возвращает null если значение null.
     * toPlainString() — без научной нотации (не "1E+4", а "10000").
     */
    private static String toString(Object value) {
        return value == null ? null : value.toString();
    }
}