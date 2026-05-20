package com.hethongtrongbanking.nienluancosonganh.service;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hethongtrongbanking.nienluancosonganh.dto.request.PaymentRequest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class DataSimulatorService {

    // Tao 5 luong chay // gion 5 thu ngan sieu thi
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    @Autowired
    private PaymentService paymentService;

    // Khi nao he thong khoi dong xong het, ket noi DB on dinh, hay tu dong goi cai
    // nay
    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void startSimulation() {
        log.info("Bat dau he thong gia lap giao dich...");

        // Chay ngam trong 1 luong rieng -> khong lam dung Spring boot
        executorService.submit(() -> {
            try {
                // Doc file csv tu resources
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        getClass().getClassLoader().getResourceAsStream("fraudTest.csv")));

                String line;
                boolean isFirstLine = true;

                while ((line = reader.readLine()) != null) {
                    if (isFirstLine) {
                        isFirstLine = false; // Bo qua dong tieu de trong csv
                        continue;
                    }

                    // Lay cac dong dulieu them cho luong xu ly
                    processTransactionAsync(line);

                    // Sleep nhe
                    Thread.sleep(50);
                }
            } catch (Exception e) {
                log.error("Loi doc file csv", e);
            }
        });
    }

    private void processTransactionAsync(String csvLine) {
        // Giao viec cho thu ngan xu ly
        executorService.submit(() -> {
            // tach chuoi csv + giao payment entity roi gui qua kafka
            // log.info("Dang quet the: {}", csvLine.substring(0, Math.min(50,
            // csvLine.length())) + "...");

            try {
                String[] data = csvLine.split(",");

                // Goi vao DTO
                PaymentRequest request = PaymentRequest.builder()
                        .transTime(java.time.LocalDateTime.parse(data[1],
                                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                        .ccNum(data[2])
                        .merchant(data[3])
                        .category(data[4])
                        .amount(Double.parseDouble(data[5]))
                        .firstName(data[6])
                        .lastName(data[7])
                        .city(data[10])
                        .lat(Double.parseDouble(data[13]))
                        .lon(Double.parseDouble(data[14]))
                        .merchantLat(Double.parseDouble(data[20]))
                        .merchantLon(Double.parseDouble(data[21]))
                        .build();

                // Quang DTO cho NVien Payment xu ly
                paymentService.processPayment(request);
            } catch (Exception e) {
                log.error("Loi khi tach chuoi CSV: {}", e.getMessage());
            }

        });
    }

}
