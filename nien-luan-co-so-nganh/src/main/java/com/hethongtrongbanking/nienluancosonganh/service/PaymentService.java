package com.hethongtrongbanking.nienluancosonganh.service;

import org.springframework.stereotype.Service;

import com.hethongtrongbanking.nienluancosonganh.dto.request.PaymentRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    // khai bao sau

    public void processPayment(PaymentRequest request) {
        log.info("Bank Processing {}: {} - amount : {}", request.getCcNum(), request.getAmount());

        // Code sau:
        // Kiem tra the co trong DB chua, chua thi tao moi
        // Chay luat lop 1
        // neu qua lop 1, day vao Kafka cho lop 2 xu ly

    }

}