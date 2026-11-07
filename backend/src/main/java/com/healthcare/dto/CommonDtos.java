package com.healthcare.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public final class CommonDtos {
    private CommonDtos() {}

    /** Stable pagination envelope (instead of serialising Spring's Page directly). */
    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        public static <T> PageResponse<T> of(Page<T> p) {
            return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(),
                    p.getTotalPages());
        }
    }
}
