package com.hethongtrongbanking.nienluancosonganh.service;

import org.springframework.stereotype.Service;

import com.hethongtrongbanking.nienluancosonganh.dto.request.PaymentRequest;
import com.hethongtrongbanking.nienluancosonganh.entity.CardInfo;
import com.hethongtrongbanking.nienluancosonganh.entity.User;
import com.hethongtrongbanking.nienluancosonganh.repository.CardInfoRepository;
import com.hethongtrongbanking.nienluancosonganh.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CardInfoService {

    private final CardInfoRepository cardInfoRepository;
    private final UserRepository userRepository;

    // Username user "KH demo" da co trong DB (register truoc hoac seed SQL)
    private static final String DEMO_CUSTOMER_USERNAME = "demo_customer";

    // Tim the theo ccNum, neu chua co -> tao CardInfo gan voi 1 User demo
    public CardInfo findOrCreateFromRequest(PaymentRequest req) {
        return cardInfoRepository.findById(req.getCcNum())
                .orElseGet(() -> createCardFromRequest(req));

    }

    private CardInfo createCardFromRequest(PaymentRequest req) {
        // CardInfo bat buoc co User
        User owner = userRepository.findByUsername(DEMO_CUSTOMER_USERNAME)
                .orElseThrow(() -> new RuntimeException(
                        "Chua cos user demo_customer trong DB. Hay register/seed truoc"));

        String fullName = (req.getFirstName() != null ? req.getFirstName() : "") + " "
                + (req.getLastName() != null ? req.getLastName() : "");

        CardInfo card = CardInfo.builder()
                .ccNum(req.getCcNum())
                .user(owner)
                .full_name(fullName.trim().isEmpty() ? "Demo Customer" : fullName.trim())
                // tranh trung
                .email(req.getCcNum() + "@demo.local")
                .phone("090000000000")
                .homeLat(req.getLat())
                .homeLon(req.getLon())
                .build();

        return cardInfoRepository.save(card);
    }

}