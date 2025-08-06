package com.mulya.employee.timesheet.exception;

import com.mulya.employee.timesheet.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatus status, WebRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ApiResponse<Object> errorResponse = ApiResponse.error(
                "Request failed",
                String.valueOf(HttpStatus.BAD_REQUEST.value()),
                detail);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }



    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatus status, WebRequest request) {
        String detail = ex.getMostSpecificCause() != null ?
                ex.getMostSpecificCause().getMessage() : ex.getMessage();

        ApiResponse<Object> errorResponse = ApiResponse.error(
                "Request failed",
                String.valueOf(HttpStatus.BAD_REQUEST.value()),
                detail);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @Hidden
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(ValidationException ex) {
        logger.error("Validation error: {}", ex.getErrors(), ex);

        String detail = ex.getErrors().entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("; "));
        ApiResponse<?> errorResponse = ApiResponse.error(
                "Validation Error",
                String.valueOf(HttpStatus.BAD_REQUEST.value()),
                detail);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @Hidden
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGeneralException(Exception ex, HttpServletRequest request) {
        logger.error("Unhandled exception: ", ex);

        ApiResponse<?> errorResponse = ApiResponse.error(
                "Internal Server Error",
                String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred");
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
