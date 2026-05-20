// package com.hethongtrongbanking.nienluancosonganh.service;

// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
// import com.fasterxml.jackson.databind.SerializationFeature;
// import com.hethongtrongbanking.nienluancosonganh.model.Payment;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.data.redis.core.StringRedisTemplate;
// import org.springframework.stereotype.Service;

// import java.time.Duration;
// import java.util.UUID;

// /**
// * IDEMPOTENCY SERVICE - Chong xu ly trung giao dich
// * Van de can giai quyet:
// * Client gui POST /payments → timeout → gui lai → He thong xu ly 2 lan → tru
// * tien 2 lan!
// *
// * Giai phap:
// * Moi request gan 1 Idempotency-Key duy nhat (UUID).
// * Key + ket qua duoc luu vao Redis voi TTL 24 gio.
// * Request tiep theo cung key → tra ve ket qua cu, khong xu ly lai.
// *
// *
// * Dung setIfAbsent() = Redis SET NX EX → atomic, 1 lenh duy nhat.
// * Chi 1 trong 2 thread SET duoc → thread kia nhan false → biet la duplicate.
// * Flow moi:
// * tryClaimKey(key) → true: request dau tien, xu ly binh thuong
// * tryClaimKey(key) → false: duplicate, tra ve ket qua cu
// * ================================================================
// */
// @Service
// @Slf4j
// public class IdempotencyService {

// private static final String KEY_PREFIX = "idempotency:";
// private static final String RESULT_PREFIX = "idempotency-result:";

// @Value("${idempotency.ttl-hours:24}")
// private long ttlHours;

// //
// @Autowired
// private StringRedisTemplate redisTemplate;

// private final ObjectMapper mapper = new ObjectMapper()
// // vi trong payment, cac bien cua LocalDateTime khong the chuyen doi duoc
// // nen can gan JavaTimeModule() vao
// .registerModule(new JavaTimeModule())
// // chuyen du lieu mang thanh kieu qte cho dep (ddMMyyyy) kieu vay
// .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

// // Tao Key
// public String resolveKey(String headerKey) {
// if (headerKey != null && !headerKey.isBlank()) {
// return headerKey.trim();
// }
// String generated = UUID.randomUUID().toString();
// log.debug("🔑 Tu sinh Idempotency-Key: {}", generated);
// return generated;
// }

// public boolean tryClaimKey(String key) {
// String redisKey = KEY_PREFIX + key;

// // Dung "CLAIMED" lam placeholder — ket qua thuc se luu o key khac sau khi xu
// ly
// // xong
// // setIfAbsent() se save neu la lan1 (true ), se thong bao loi neu ko la lan1
// // (false)
// Boolean wasAbsent = redisTemplate.opsForValue()
// .setIfAbsent(redisKey, "CLAIMED", Duration.ofHours(ttlHours));
// boolean isFirstTime = Boolean.TRUE.equals(wasAbsent);

// if (!isFirstTime) {
// log.warn("⚠️ Duplicate request | key={}", key);
// }
// return isFirstTime;
// }

// /**
// * Luu y: Controller phai goi theo thu tu:
// * 1. isDuplicate(key) → neu true: getCachedResult() roi return
// * 2. Xu ly payment
// * 3. saveResult(key, payment)
// *
// * Vi tryClaimKey() da SET key vao Redis o buoc 1,
// * saveResult() chi luu them ket qua vao key rieng.
// */
// public boolean isDuplicate(String key) {
// // Dao nguoc logic — tryClaimKey() tra true = CHUA co → xu ly moi
// // isDuplicate() can tra true = DA co → la duplicate
// return !tryClaimKey(key);
// }

// // Lay ket qua da duoc luu trong Redis
// public Payment getCachedResult(String key) {
// try {
// String json = redisTemplate.opsForValue().get(RESULT_PREFIX + key);
// if (json == null)
// return null;
// return mapper.readValue(json, Payment.class);
// } catch (Exception e) {
// log.error("❌ Khong doc duoc cache Redis | key={} | {}", key, e.getMessage());
// return null;
// }
// }

// /**
// * saveResult(): luu ket qua vao Redis sau khi xu ly thanh cong.
// *
// * Luu vao key rieng biet (RESULT_PREFIX) de phan biet:
// * KEY_PREFIX + key = "CLAIMED" (danh dau da xu ly, atomic)
// * RESULT_PREFIX + key = JSON cua Payment (de getCachedResult() doc)
// */
// public void saveResult(String key, Payment payment) {
// try {
// String json = mapper.writeValueAsString(payment);
// String redisKey = RESULT_PREFIX + key;
// redisTemplate.opsForValue().set(redisKey, json, Duration.ofHours(ttlHours));
// log.info("💾 Luu ket qua Redis | key={} | TTL={}h | paymentId={}",
// key, ttlHours, payment.getId());
// } catch (Exception e) {
// log.warn("⚠️ Khong luu duoc Redis | key={} | {}", key, e.getMessage());
// }
// }
// }