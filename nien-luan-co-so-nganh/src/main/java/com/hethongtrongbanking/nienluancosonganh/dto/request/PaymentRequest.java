package com.hethongtrongbanking.nienluancosonganh.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    private String ccNum; // so the
    private String merchant; // ten cua hang
    private String category; // danh muc
    private Double amount; // so tien
    private String firstName; // ten KH
    private String lastName; // ho KH
    private String city; // thanh pho
    private Double lat; // Vi do KH
    private Double lon; // kinh do KH
    private Double merchantLat; // Vi do cua hang
    private Double merchantLon; // Kinh do cua hang
    private LocalDateTime transTime; // Thoi gian giao dich

}
