package com.avsmc.procurement.shared.util;

import com.avsmc.procurement.shared.dto.PageResponse;
import org.springframework.data.domain.Page;

public class PaginationHelper {

    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
