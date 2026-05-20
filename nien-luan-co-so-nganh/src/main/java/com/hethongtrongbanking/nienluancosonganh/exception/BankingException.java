package com.hethongtrongbanking.nienluancosonganh.exception;

import org.springframework.http.HttpStatusCode;

public class BankingException extends RuntimeException {
    private final ErrorCode errorcode;

    public BankingException(ErrorCode errorcode) {
        this.errorcode = errorcode;
    }

    public ErrorCode getErrorCode() {
        return errorcode;
    }

}
