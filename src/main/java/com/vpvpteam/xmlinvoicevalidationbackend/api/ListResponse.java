package com.vpvpteam.xmlinvoicevalidationbackend.api;

import java.util.List;

public record ListResponse<T>(List<T> data, int count, String message) {
    public static <T> ListResponse<T> of(List<T> data) {
        if (data == null || data.isEmpty()) {
            return new ListResponse<>(List.of(), 0, "No data found for your request");
        }

        return new ListResponse<>(data, data.size(), null);
    }
}