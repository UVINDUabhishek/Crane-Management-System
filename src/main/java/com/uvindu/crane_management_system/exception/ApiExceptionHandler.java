package com.uvindu.crane_management_system.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Comparator;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    ProblemDetail business(BusinessException exception) {
        return ApiProblems.create(exception.getStatus(), exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(CraneAlreadyExistsException.class)
    ProblemDetail duplicateCrane(CraneAlreadyExistsException exception) {
        return ApiProblems.create(HttpStatus.CONFLICT, "DUPLICATE_CRANE", exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail dataConflict(DataIntegrityViolationException exception) {
        return ApiProblems.create(HttpStatus.CONFLICT, "DATA_CONFLICT", "Record conflicts with existing data");
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    ProblemDetail lockConflict(PessimisticLockingFailureException exception) {
        return ApiProblems.create(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION", "Concurrent change; retry the request");
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail authentication(AuthenticationException exception) {
        return ApiProblems.create(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                "Authentication is required or credentials are invalid");
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail forbidden(AccessDeniedException exception) {
        return ApiProblems.create(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access is denied");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ApiProblems.create(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed");
        problem.setProperty("errors", exception.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(org.springframework.validation.FieldError::getField))
                .map(error -> Map.of("field", error.getField(), "message", error.getDefaultMessage())).toList());
        return ResponseEntity.badRequest().body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        HttpStatus httpStatus = HttpStatus.valueOf(status.value());
        // Never echo parser messages, rejected values, Java types or persistence details.
        String code = switch (status.value()) {
            case 400 -> "INVALID_REQUEST";
            case 404 -> "RESOURCE_NOT_FOUND";
            case 409 -> "STATE_CONFLICT";
            default -> "HTTP_ERROR";
        };
        return new ResponseEntity<>(ApiProblems.create(httpStatus, code, httpStatus.getReasonPhrase()), headers, status);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception exception) {
        LOG.error("Unexpected API failure", exception);
        return ApiProblems.create(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred");
    }
}
