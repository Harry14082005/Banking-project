package com.hethongtrongbanking.nienluancosonganh.service;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hethongtrongbanking.nienluancosonganh.dto.request.PaymentRequest;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class DataSimulatorService {

    // Tao 5 luong chay song song (giong 5 thu ngan sieu thi)
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    @Autowired
    private PaymentService paymentService;

    // Cờ hiệu: simulator có đang chạy không?
    private final AtomicBoolean running = new AtomicBoolean(false);
    // Bộ đếm tổng số giao dịch đã xử lý
    private final AtomicInteger processedCount = new AtomicInteger(0);

    public String startSimulation(int delayMs) {
        if (running.get()) {
            return "Simulator dang chay roi! Da xu ly " + processedCount.get() + " giao dich.";
        }

        running.set(true);
        processedCount.set(0);
        log.info("Bat dau he thong gia lap giao dich (delay={}ms)...", delayMs);

        // Chay ngam trong 1 luong rieng -> khong lam dung Spring Boot
        // submit() giao viec cho 1 Thread trong ThreadPool
        executorService.submit(() -> {
            try {
                InputStream is = getClass().getClassLoader().getResourceAsStream("fraudTest.csv");
                if (is == null) {
                    log.error("Khong tim thay file fraudTest.csv trong resources!");
                    running.set(false);
                    return;
                }

                // Boc lai de doc tung dong text
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line;
                boolean isFirstLine = true;

                while ((line = reader.readLine()) != null && running.get()) {
                    if (isFirstLine) {
                        isFirstLine = false; // Bo qua dong tieu de trong CSV
                        continue;
                    }

                    // Lay cac dong dulieu them cho luong xu ly
                    processTransactionAsync(line);
                    Thread.sleep(delayMs);
                }

                reader.close();
                log.info("Simulator hoan tat. Tong: {} giao dich.", processedCount.get());
            } catch (Exception e) {
                log.error("Loi doc file CSV", e);
            } finally {
                running.set(false);
            }
        });

        return "Simulator da bat dau! Delay: " + delayMs + "ms giua moi giao dich.";
    }

    // Dung simulator
    public String stopSimulation() {
        if (!running.get()) {
            return "Simulator khong dang chay.";
        }
        running.set(false);
        return "Simulator da dung. Tong xu ly: " + processedCount.get() + " giao dich.";
    }

    // Lay trang thai hien tai
    public String getStatus() {
        return running.get()
                ? "RUNNING - Da xu ly: " + processedCount.get() + " giao dich"
                : "STOPPED - Tong da xu ly: " + processedCount.get() + " giao dich";
    }

    private void processTransactionAsync(String csvLine) {
        // Giao viec cho thu ngan xu ly
        executorService.submit(() -> {
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
                processedCount.incrementAndGet(); // ????
            } catch (Exception e) {
                log.error("Loi khi tach chuoi CSV: {}", e.getMessage());
            }
        });
    }
}
