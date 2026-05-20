package com.hethongtrongbanking.nienluancosonganh.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

        @ExceptionHandler(BankingException.class)
        public ResponseEntity<Map<String, Object>> handleBankingException(BankingException ex) {
                // Thò tay vào túi của BankingException móc cái ErrorCode ra
                ErrorCode errorCode = ex.getErrorCode();

                log.warn("Lỗi Nghiệp Vụ [Mã {}]: {}", errorCode.getCode(), errorCode.getMessage());

                return ResponseEntity
                                .status(errorCode.getStatusCode())
                                .body(errorBody(
                                                errorCode.getStatusCode().value(), // Mã HTTP (VD: 404)
                                                errorCode.getCode(), // Mã Nội bộ (VD: 1101)
                                                errorCode.getMessage() // Lời nhắn tiếng Việt
                                ));
        }

        // Validation input tu frontend
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
                String errors = ex.getBindingResult().getFieldErrors().stream()
                                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                                .collect(Collectors.joining(", "));

                log.warn("Validation thất bại: {}", errors);

                // Ben Enum
                ErrorCode errorCode = ErrorCode.INVALID_INPUT;

                return ResponseEntity
                                .status(errorCode.getStatusCode())
                                .body(errorBody(
                                                errorCode.getStatusCode().value(),
                                                errorCode.getCode(),
                                                "Dữ liệu không hợp lệ: " + errors // Gắn chi tiết lỗi vào đuôi
                                ));
        }

        // Cac loi khong xac dinh
        @ExceptionHandler(Exception.class)
        public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
                log.error("Lỗi hệ thống: {}", ex.getMessage(), ex);

                // Chộp luôn cái mã 1000 (INTERNAL_SERVER_ERROR) ra xài
                ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

                return ResponseEntity
                                .status(errorCode.getStatusCode())
                                .body(errorBody(
                                                errorCode.getStatusCode().value(),
                                                errorCode.getCode(),
                                                errorCode.getMessage()));
        }

        // Helper: Dong goi JSON chuan gui ve frontend
        private Map<String, Object> errorBody(int httpStatus, int errorCode, String message) {
                return Map.of(
                                "timestamp", LocalDateTime.now().toString(),
                                "status", httpStatus, // Cho Browser/Postman đọc
                                "errorCode", errorCode, // Cho code React/Vue đọc để chia nhánh
                                "message", message // Cho người dùng (Customer) đọc
                );
        }
}
