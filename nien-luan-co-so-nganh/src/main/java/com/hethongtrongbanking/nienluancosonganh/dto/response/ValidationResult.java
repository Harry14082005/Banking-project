package com.hethongtrongbanking.nienluancosonganh.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ValidationResult {

    private boolean allowed; // true -> qua L1
    private String reason; // Ly do hien thi (Postman/ Log)
    private String fraudType; // loi se luu tru vao db sau nay

    //Thong qua L1
    public static ValidationResult ok (){
        return ValidationResult.builder().allowed(true).build();
    }

    //Bi cam o L1
    public static ValidationResult fail(String reason, String fraudType){
        return ValidationResult.builder()   
            .allowed(false)
            .reason(reason)
            .fraudType(fraudType)
            .build();
    }

}
