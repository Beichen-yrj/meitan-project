package com.meitan.exception;

import com.meitan.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PythonServiceUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handlePythonServiceUnavailable(
            PythonServiceUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ApiResponse.error(HttpStatus.SERVICE_UNAVAILABLE.value(), exception.getMessage()));
    }
}
