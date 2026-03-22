package com.industrial.mdm.common.exception;

import com.industrial.mdm.common.api.ApiErrorResponse;
import com.industrial.mdm.common.api.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<String> details =
                exception.getBindingResult().getFieldErrors().stream()
                        .map(FieldError::getDefaultMessage)
                        .toList();
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_REQUEST,
                "request validation failed",
                details,
                request.getRequestURI());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception, HttpServletRequest request) {
        List<String> details =
                exception.getConstraintViolations().stream()
                        .map(violation -> violation.getMessage())
                        .toList();
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_REQUEST,
                "request validation failed",
                details,
                request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(HttpServletRequest request) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                ErrorCode.FORBIDDEN,
                "access denied",
                List.of(),
                request.getRequestURI());
    }

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiErrorResponse> handleBizException(
            BizException exception, HttpServletRequest request) {
        return buildResponse(
                resolveStatus(exception.getErrorCode()),
                exception.getErrorCode(),
                exception.getMessage(),
                List.of(),
                request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_ERROR,
                "internal server error",
                List.of(exception.getClass().getSimpleName()),
                request.getRequestURI());
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            ErrorCode errorCode,
            String message,
            List<String> details,
            String path) {
        ApiErrorResponse body =
                new ApiErrorResponse(
                        errorCode.name(),
                        message,
                        details,
                        MDC.get(RequestIdFilter.REQUEST_ID),
                        path,
                        OffsetDateTime.now());
        return ResponseEntity.status(status).body(body);
    }

    private HttpStatus resolveStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            case INVALID_REQUEST,
                            SMS_CODE_INVALID,
                            SMS_CODE_EXPIRED,
                            ENTERPRISE_PROFILE_INCOMPLETE,
                            PRODUCT_PROFILE_INCOMPLETE ->
                    HttpStatus.BAD_REQUEST;
            case INVALID_CREDENTIALS, REFRESH_TOKEN_INVALID, UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case DUPLICATE_ACCOUNT, STATE_CONFLICT -> HttpStatus.CONFLICT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
