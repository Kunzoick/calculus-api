package com.zoick.calculusapi.exception;

import com.zoick.calculusapi.dto.response.CalcResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CalcResponse> handleValidation(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst()
                .orElse("Invalid request");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CalcResponse.fail("", "UNKNOWN", errorMessage));
    }
    @ExceptionHandler(InvalidExpressionException.class)
    public ResponseEntity<CalcResponse> handleInvalidExpression(InvalidExpressionException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CalcResponse.fail(ex.getExpression(), "UNKNOWN", ex.getMessage()));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CalcResponse> handleGeneral(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CalcResponse.fail("", "UNKNOWN", "An unexpected error occurred"));
    }
}
