package userService.demo.adapter.in.web.dto.common;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.util.List;

/** Stable pagination envelope for list endpoints, independent of Spring Data's own page types. */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        String sortBy,
        String sortDirection
) {

    public static <T> PageResponse<T> of(Page<T> page) {
        Sort.Order order = page.getSort().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Paged result is missing its sort order"));
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                order.getProperty(),
                order.getDirection().name().toLowerCase());
    }
}
