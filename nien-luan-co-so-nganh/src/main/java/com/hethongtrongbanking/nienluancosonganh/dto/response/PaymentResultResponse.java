package com.hethongtrongbanking.nienluancosonganh.dto.response;

import lombok.Data;
import java.util.List;
import lombok.Builder;
@Data
@Builder
public class PaymentResultResponse {
    private Long paymentId;
    private String status;
    private String reason;
    private Double riskScore;
    private String riskLevel;
    private List<String> patterns;
    private String modelVersion;

}
