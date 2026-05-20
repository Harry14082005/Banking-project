package com.hethongtrongbanking.nienluancosonganh.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum ErrorCode {

    // Nhom 1000: Loi he thong va chung
    INTERNAL_SERVER_ERROR(1000, "Lỗi hệ thống, vui lòng thử lại sau", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_INPUT(1001, "Dữ liệu đầu vào không hợp lệ", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1002, "Vui lòng đăng nhập", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1003, "Bạn không có quyền thực hiện thao tác này", HttpStatus.FORBIDDEN),

    // Nhom 1100: Failed business (payment)
    PAYMENT_NOT_FOUND(1101, "Không tìm thấy giao dịch", HttpStatus.NOT_FOUND),
    DUPLICATE_PAYMENT(1102, "Giao dịch trùng lặp đang được xử lý", HttpStatus.CONFLICT),
    LIMIT_EXCEEDED(1103, "Số tiền giao dịch vượt quá hạn mức cho phép", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_FUNDS(1104, "Tài khoản không đủ số dư", HttpStatus.BAD_REQUEST),

    // Nhom 1200: Loi gian lan (fraud case)
    FRAUD_CASE_NOT_FOUND(1201, "Không tìm thấy hồ sơ gian lận", HttpStatus.NOT_FOUND),
    ALREADY_RESOLVED(1202, "Hồ sơ gian lận này đã được xử lý", HttpStatus.CONFLICT), // <--- Của bạn đây!
    INVALID_STATUS_TRANSITION(1203, "Không thể chuyển đổi trạng thái hồ sơ theo cách này", HttpStatus.BAD_REQUEST),
    HIGH_RISK_BLOCKED(1204, "Giao dịch bị từ chối tự động do điểm rủi ro gian lận quá cao", HttpStatus.FORBIDDEN);

    private int code;
    private String message;
    private HttpStatusCode statusCode;
}
