package com.hethongtrongbanking.nienluancosonganh.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hethongtrongbanking.nienluancosonganh.entity.FraudCase;
import com.hethongtrongbanking.nienluancosonganh.enums.FraudCaseStatus;

@Repository
public interface FraudCaseRepository extends JpaRepository<FraudCase, Long> {

    // Tat ca case theo status
    List<FraudCase> findByStatusOrderByCreatedAtDesc(FraudCaseStatus status);

    // Lay case chua xuly + dang xuly (de analyst tong hop)
    List<FraudCase> findByStatusInOrderByCreatedAtDesc(List<FraudCaseStatus> status);

    // Lay case cua 1 analyst cu the dang xu ly
    List<FraudCase> findByResolvedByIdAndStatus(Long analystId, FraudCaseStatus status);

}