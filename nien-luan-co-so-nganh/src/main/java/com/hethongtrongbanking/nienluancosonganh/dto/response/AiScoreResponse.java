package com.hethongtrongbanking.nienluancosonganh.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// khi python the field ma java chua khai bao se khong deserialize
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiScoreResponse {

    private Double riskScore;
    private String riskLevel;
    private String recommendation;
    private List<String> patternMatched;
    private List<String> reasons;
    private String modelVersion;
}
