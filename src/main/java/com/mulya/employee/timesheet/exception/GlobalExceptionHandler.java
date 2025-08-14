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
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler{

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ====== Handle @Valid validation errors (e.g. request body validation) ======
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatus status,
                                                                  jakarta.servlet.http.HttpServletRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ApiResponse<Object> errorResponse = ApiResponse.error(
                "Request validation failed",
                String.valueOf(HttpStatus.BAD_REQUEST.value()),
                detail
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // ====== Handle JSON parse errors ======
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatus status,
                                                                  org.springframework.web.context.request.WebRequest request) {
        String detail = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();

        ApiResponse<Object> errorResponse = ApiResponse.error(
                "Malformed JSON request",
                String.valueOf(HttpStatus.BAD_REQUEST.value()),
                detail
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // ====== Handle ValidationException (custom) ======
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
                detail
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // ====== Handle Max Upload Size ======
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<?>> handleMaxSize(MaxUploadSizeExceededException ex) {
        logger.error("File size too large: ", ex);
        ApiResponse<?> errorResponse = ApiResponse.error(
                "File upload too large",
                String.valueOf(HttpStatus.PAYLOAD_TOO_LARGE.value()),
                "Uploaded file exceeds the maximum allowed size"
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.PAYLOAD_TOO_LARGE);
    }

    // ====== Handle Attachment Not Found ======
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleAttachmentNotFound(ResourceNotFoundException ex) {
        logger.error("Attachment not found: {}", ex.getMessage());
        ApiResponse<?> errorResponse = ApiResponse.error(
                "Attachment Not Found",
                String.valueOf(HttpStatus.NOT_FOUND.value()),
                ex.getMessage()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    // ====== Handle generic IllegalArgumentException (like "Timesheet not found") ======
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArgument(IllegalArgumentException ex) {
        logger.error("Bad request: {}", ex.getMessage());
        ApiResponse<?> errorResponse = ApiResponse.error(
                "Bad Request",
                String.valueOf(HttpStatus.BAD_REQUEST.value()),
                ex.getMessage()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // ====== Handle ALL other unhandled exceptions ======
    @Hidden
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGeneralException(Exception ex, HttpServletRequest request) {
        logger.error("Unhandled exception: ", ex);
        ApiResponse<?> errorResponse = ApiResponse.error(
                "Internal Server Error",
                String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred"
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
