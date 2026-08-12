package bankingService.demo.adapter.in.web;

import bankingService.demo.domain.exception.DuplicateResourceException;
import bankingService.demo.domain.exception.IdempotencyConflictException;
import bankingService.demo.domain.exception.InsufficientFundsException;
import bankingService.demo.domain.exception.InvalidAccountStateException;
import bankingService.demo.domain.exception.InvalidPaginationException;
import bankingService.demo.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({DuplicateResourceException.class, InsufficientFundsException.class,
            InvalidAccountStateException.class, IdempotencyConflictException.class})
    public ProblemDetail handleDuplicate(RuntimeException ex) {
        log.warn("Rejected request due to a conflicting resource state: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidPaginationException.class)
    public ProblemDetail handleInvalidPagination(InvalidPaginationException ex) {
        log.warn("Rejected invalid pagination request: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The request could not be completed because it violates a data integrity constraint");
    }

    /**
     * Rethrown rather than handled so ExceptionTranslationFilter can decide between 401 and 403
     * based on whether the caller is anonymous. Without this the catch-all below would turn every
     * authorization failure into a 500.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public void handleAccessDenied(AccessDeniedException ex) throws AccessDeniedException {
        log.warn("Access denied: {}", ex.getMessage());
        throw ex;
    }

    @ExceptionHandler(AuthenticationException.class)
    public void handleAuthenticationFailure(AuthenticationException ex) throws AuthenticationException {
        log.warn("Authentication failed: {}", ex.getMessage());
        throw ex;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception while processing the request", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                     HttpHeaders headers,
                                                                     HttpStatusCode status,
                                                                     WebRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage()));

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Validation failed for one or more fields");
        problemDetail.setProperty("errors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }
}
