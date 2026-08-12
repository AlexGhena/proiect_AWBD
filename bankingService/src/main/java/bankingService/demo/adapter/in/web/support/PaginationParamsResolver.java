package bankingService.demo.adapter.in.web.support;

import bankingService.demo.config.PaginationProperties;
import bankingService.demo.domain.exception.InvalidPaginationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Turns raw {@code page}/{@code size}/{@code sortBy}/{@code sortDirection} query parameters into a
 * validated {@link Pageable}, rejecting anything a repository would otherwise silently accept -
 * negative pages, oversized pages, or a sort field that was never whitelisted for that entity.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaginationParamsResolver {

    private final PaginationProperties paginationProperties;

    public Pageable resolve(Integer page, Integer size, String sortBy, String sortDirection,
                             Set<String> allowedSortFields, String defaultSortField) {
        int resolvedPage = page == null ? 0 : page;
        if (resolvedPage < 0) {
            throw new InvalidPaginationException("page must not be negative");
        }

        int resolvedSize = size == null ? paginationProperties.defaultSize() : size;
        if (resolvedSize <= 0) {
            throw new InvalidPaginationException("size must be greater than 0");
        }
        if (resolvedSize > paginationProperties.maxSize()) {
            throw new InvalidPaginationException("size must not exceed " + paginationProperties.maxSize());
        }

        String resolvedSortBy = (sortBy == null || sortBy.isBlank()) ? defaultSortField : sortBy;
        if (!allowedSortFields.contains(resolvedSortBy)) {
            throw new InvalidPaginationException(
                    "sortBy must be one of " + allowedSortFields + " but was '" + resolvedSortBy + "'");
        }

        String resolvedDirection = (sortDirection == null || sortDirection.isBlank()) ? "desc" : sortDirection;
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(resolvedDirection);
        } catch (IllegalArgumentException ex) {
            throw new InvalidPaginationException("sortDirection must be 'asc' or 'desc'");
        }

        log.debug("Resolved pagination request: page={}, size={}, sortBy={}, sortDirection={}",
                resolvedPage, resolvedSize, resolvedSortBy, direction);
        return PageRequest.of(resolvedPage, resolvedSize, Sort.by(direction, resolvedSortBy));
    }
}
