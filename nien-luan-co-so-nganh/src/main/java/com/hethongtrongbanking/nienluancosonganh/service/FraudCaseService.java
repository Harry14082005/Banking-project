package com.hethongtrongbanking.nienluancosonganh.service;

import org.springframework.stereotype.Service;

import com.hethongtrongbanking.nienluancosonganh.entity.FraudCase;
import com.hethongtrongbanking.nienluancosonganh.entity.User;
import com.hethongtrongbanking.nienluancosonganh.enums.FraudCaseStatus;
import com.hethongtrongbanking.nienluancosonganh.exception.BankingException;
import com.hethongtrongbanking.nienluancosonganh.exception.ErrorCode;
import com.hethongtrongbanking.nienluancosonganh.repository.FraudCaseRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudCaseService {

    private final FraudCaseRepository fraudCaseRepository;

    // Lay ds case can xuly (OPEN hoac PENDING)
    public List<FraudCase> getPendingCases() {
        return fraudCaseRepository.findByStatusInOrderByCreatedAtDesc(
                List.of(FraudCaseStatus.OPEN, FraudCaseStatus.IN_PROGRESS));
    }

    // Analyst nhan case (OPEN, IN_PROGRESS)
    @Transactional
    public FraudCase claimCase(Long caseId, User analyst) {
        FraudCase fraudCase = findOrThrow(caseId);

        if (fraudCase.getStatus() != FraudCaseStatus.OPEN) {
            throw new BankingException(ErrorCode.INVALID_INPUT);
        }

        fraudCase.setStatus(FraudCaseStatus.IN_PROGRESS);
        fraudCase.setResolvedBy(analyst);

        log.info("Case {} claimed by analyst {}", caseId, analyst.getUsername());

        return fraudCaseRepository.save(fraudCase);

    }

    // Analyst tu choi vi GIAN LAN -> BLOCKED
    @Transactional
    public FraudCase rejectCase(Long caseId, User analyst, String note) {
        FraudCase fraudCase = findOrThrow(caseId);
        validateAnalystOwnership(fraudCase, analyst);

        fraudCase.setStatus(FraudCaseStatus.CLOSED);
        fraudCase.setAnalystNote(note);
        fraudCase.setResolvedAt(LocalDateTime.now());

        log.info("Case {} rejected by analyst {}", caseId, analyst.getUsername());

        return fraudCaseRepository.save(fraudCase);
    }

    // Analyst duyet giao dich hop le -> RESOLVED
    @Transactional
    public FraudCase approveCase(Long caseId, User analyst, String note) {
        FraudCase fraudCase = findOrThrow(caseId);
        validateAnalystOwnership(fraudCase, analyst);

        fraudCase.setStatus(FraudCaseStatus.RESOLVED);
        fraudCase.setAnalystNote(note);
        fraudCase.setResolvedAt(LocalDateTime.now());

        log.info("Case {} approved by analyst {}", caseId, analyst.getUsername());

        return fraudCaseRepository.save(fraudCase);
    }

    // Helper
    private FraudCase findOrThrow(Long caseId) {
        return fraudCaseRepository.findById(caseId)
                .orElseThrow(() -> new BankingException(ErrorCode.FRAUD_CASE_NOT_FOUND));
    }

    private void validateAnalystOwnership(FraudCase fraudCase, User analyst) {
        // Chi analyst dang xuly case moi duoc approve/reject
        // Co the bo neu muon ai xu ly cung duoc
        if (fraudCase.getResolvedBy() == null || !fraudCase.getResolvedBy().getId().equals(analyst.getId())) {
            throw new BankingException(ErrorCode.UNAUTHORIZED);
        }
    }

}
