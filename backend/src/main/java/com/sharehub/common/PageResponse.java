package com.sharehub.common;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(
    List<T> items,
    int page,
    int pageSize,
    long total
) {
    public static <T> PageResponse<T> of(List<T> items, int page, int pageSize, long total) {
        return new PageResponse<>(items, page, pageSize, total);
    }

    public static <T> PageResponse<T> from(Page<T> pageResult) {
        return new PageResponse<>(
            pageResult.getContent(),
            pageResult.getNumber(),
            pageResult.getSize(),
            pageResult.getTotalElements()
        );
    }
}
