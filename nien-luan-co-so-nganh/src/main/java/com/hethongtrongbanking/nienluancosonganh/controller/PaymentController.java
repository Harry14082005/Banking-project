package com.hethongtrongbanking.nienluancosonganh.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hethongtrongbanking.nienluancosonganh.dto.request.PaymentRequest;
import com.hethongtrongbanking.nienluancosonganh.dto.response.PaymentResultResponse;
import com.hethongtrongbanking.nienluancosonganh.service.PaymentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResultResponse> createPayment(@RequestBody PaymentRequest request) {
        PaymentResultResponse response = paymentService.processPayment(request);
        return ResponseEntity.ok(response);
    }
}