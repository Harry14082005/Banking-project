package com.hethongtrongbanking.nienluancosonganh.service;

import org.springframework.stereotype.Service;

import com.hethongtrongbanking.nienluancosonganh.dto.request.PaymentRequest;
import com.hethongtrongbanking.nienluancosonganh.dto.response.AiScoreResponse;
import com.hethongtrongbanking.nienluancosonganh.dto.response.PaymentResultResponse;
import com.hethongtrongbanking.nienluancosonganh.dto.response.ValidationResult;
import com.hethongtrongbanking.nienluancosonganh.entity.*;
import com.hethongtrongbanking.nienluancosonganh.enums.FraudCaseStatus;
import com.hethongtrongbanking.nienluancosonganh.enums.PaymentStatus;
import com.hethongtrongbanking.nienluancosonganh.repository.FraudCaseRepository;
import com.hethongtrongbanking.nienluancosonganh.repository.PaymentRepository;
import com.hethongtrongbanking.nienluancosonganh.repository.TransactionHistoryRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private static final double BLOCK_THRESHOLD = 0.70;
    private static final double REVIEW_THRESHOLD = 0.35;

    private final PaymentValidator paymentValidator;
    private final AiScoringClient aiScoringClient;
    private final PaymentRepository paymentRepository;
    private final CardInfoService cardInfoService;
    private final FraudCaseRepository fraudCaseRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;

    // khai bao sau

    public PaymentResultResponse processPayment(PaymentRequest request) {
        log.info("Bank Processing: {} - amount : {}", request.getCcNum(), request.getAmount());

        // ========== LAYER 1: Hard Rules ==========
        ValidationResult l1 = paymentValidator.validate(request);
        if (!l1.isAllowed()) {
            Payment saved = savePayment(
                    request, PaymentStatus.BLOCKED, l1.getReason(), l1.getFraudType());

            // Ghi lich su: PENDING -> BLOCKED
            saveTransactionHistory(saved, PaymentStatus.PENDING, PaymentStatus.BLOCKED, l1.getReason());

            // Tao FraudCase cho L1 blocked
            createFraudCase(saved, "LAYER_1", null, l1.getFraudType(), null, l1.getReason());

            return PaymentResultResponse.builder()
                    .paymentId(saved.getId())
                    .status("BLOCKED")
                    .reason(l1.getReason())
                    .build();
        }

        // ========== LAYER 2: AI Scoring ==========
        AiScoreResponse ai;
        try {
            ai = aiScoringClient.score(request);
        } catch (Exception e) {
            // AI service khong kha dung → van save Payment voi status PENDING
            log.warn("AI service unavailable, saving as PENDING: {}", e.getMessage());
            Payment saved = savePayment(request, PaymentStatus.PENDING, "AI service unavailable", null);
            saveTransactionHistory(saved, PaymentStatus.PENDING, PaymentStatus.PENDING, "AI service unavailable");

            return PaymentResultResponse.builder()
                    .paymentId(saved.getId())
                    .status("PENDING")
                    .reason("AI service unavailable - giao dich cho xu ly thu cong")
                    .build();
        }

        double score = ai.getRiskScore() != null ? ai.getRiskScore() : 0.0;

        // Quyet dinh trang thai theo diem AI
        PaymentStatus paymentStatus;
        if (score >= BLOCK_THRESHOLD) {
            paymentStatus = PaymentStatus.BLOCKED;
        } else if (score >= REVIEW_THRESHOLD) {
            paymentStatus = PaymentStatus.UNDER_REVIEW;
        } else {
            paymentStatus = PaymentStatus.APPROVED;
        }

        // Ghep ly do doc duoc
        String reason = (ai.getReasons() != null && !ai.getReasons().isEmpty())
                ? String.join("; ", ai.getReasons())
                : "AI scoring completed";

        String patterns = (ai.getPatternMatched() != null && !ai.getPatternMatched().isEmpty())
                ? String.join(", ", ai.getPatternMatched())
                : null;

        // ========== SAVE PAYMENT vao DB ==========
        Payment saved = savePayment(request, paymentStatus, reason,
                paymentStatus != PaymentStatus.APPROVED ? "AI_DETECTED" : null);

        // Ghi lich su: PENDING -> status cuoi
        saveTransactionHistory(saved, PaymentStatus.PENDING, paymentStatus, reason);

        // ========== TAO FRAUD CASE neu nghi ngo/block ==========
        if (paymentStatus == PaymentStatus.UNDER_REVIEW || paymentStatus == PaymentStatus.BLOCKED) {
            createFraudCase(saved, "LAYER_2", score, "AI_DETECTED", patterns, reason);
        }

        return PaymentResultResponse.builder()
                .paymentId(saved.getId())
                .status(paymentStatus.name())
                .reason(reason)
                .riskScore(ai.getRiskScore())
                .riskLevel(ai.getRiskLevel())
                .patterns(ai.getPatternMatched())
                .modelVersion(ai.getModelVersion())
                .build();
    }

    // HELPER METHODS
    private Payment savePayment(PaymentRequest req, PaymentStatus status,
            String statusReason, String fraudType) {

        CardInfo card = cardInfoService.findOrCreateFromRequest(req);

        Payment payment = Payment.builder()
                .cardInfo(card) // FKbat buoc
                .amt(BigDecimal.valueOf(req.getAmount()))
                .merchant(req.getMerchant())
                .category(req.getCategory())
                .lat(req.getLat())
                .lon(req.getLon())
                .merchLat(req.getMerchantLat())
                .merchLon(req.getMerchantLon())
                .status(status)
                .statusReason(statusReason)
                .fraudType(fraudType)
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Payment saved: id={}, status={}", saved.getId(), status);
        return saved;
    }

    private void saveTransactionHistory(Payment payment, PaymentStatus oldStatus,
            PaymentStatus newStatus, String reason) {
        TransactionHistory history = TransactionHistory.builder()
                .payment(payment)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .reason(reason)
                .build();
        transactionHistoryRepository.save(history);
    }

    private void createFraudCase(Payment payment, String detectionLayer,
            Double riskScore, String fraudType, String patternMatched, String reason) {
        FraudCase fraudCase = FraudCase.builder()
                .payment(payment)
                .detectionLayer(detectionLayer)
                .riskScore(riskScore)
                .fraudType(fraudType)
                .patternMatched(patternMatched)
                .reason(reason)
                .status(FraudCaseStatus.OPEN)
                .build();
        fraudCaseRepository.save(fraudCase);
        log.info("FraudCase created: paymentId={}, layer={}, score={}",
                payment.getId(), detectionLayer, riskScore);
    }
}