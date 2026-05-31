package com.hethongtrongbanking.nienluancosonganh.service;

import org.springframework.stereotype.Component;

import com.hethongtrongbanking.nienluancosonganh.dto.request.PaymentRequest;
import com.hethongtrongbanking.nienluancosonganh.dto.response.ValidationResult;
import com.hethongtrongbanking.nienluancosonganh.repository.PaymentRepository;

@Component
public class PaymentValidator {

    //Han muc (demo) - sau co the doc tu ban FRAUD_RULE
    private static final double HARD_AMOUNT_LIMIT = 10_000.0;

    public ValidationResult validate (PaymentRequest request){
        if (request == null){
            return ValidationResult.fail("Request null", "INVALID_REQUEST");
        }

        //So tien bat buoc > 0
        if (request.getAmount() == null || request.getAmount() <= 0){
            return ValidationResult.fail("So tien khong hop le", "INVALID_AMOUNT");
        }

        // Vuot han muc -> block ko can AI
        if (request.getAmount() > HARD_AMOUNT_LIMIT){
            return ValidationResult.fail("Vuot han muc giao dich", "AMOUNT_LIMIT");
        }

        // Thieu so the ->  khong xu ly
        if (isBlank(request.getCcNum())){
            return ValidationResult.fail("Thieu so the", "CARD_MISSING");
        }

        //Thieu dia diem giao dich
        if (isBlank(request.getMerchant())){
            return ValidationResult.fail("Thieu merchan", "MERCHANT_MISSING");
        }
        
        return ValidationResult.ok();
    }

    private boolean isBlank(String s){
        return s == null || s.trim().isEmpty();
    }

}
