"""
=============================================================
CONCURRENT BATCH TESTER + EVALUATION
=============================================================
Đọc fraudTest.csv → gửi ĐỒNG THỜI qua Spring Boot
→ polling kết quả → so sánh với is_fraud thực tế
→ in Precision / Recall / F1 / Confusion Matrix
=============================================================
"""

import pandas as pd
import requests
import time
import csv
from concurrent.futures import ThreadPoolExecutor, as_completed
from tqdm import tqdm
from collections import Counter
from sklearn.metrics import (
    classification_report, confusion_matrix,
    precision_score, recall_score, f1_score
)

# ============================================================
# CẤU HÌNH
# ============================================================
CSV_FILE        = "fraudTest.csv"
SPRING_BOOT_URL  = "http://localhost:8080/api/v1/payments"
# ✅ FIX LỖI 4: Thêm endpoint FraudCase để lấy detectionLayer và fraudPatterns
FRAUD_CASE_URL   = "http://localhost:8080/api/v1/fraud-cases"
LIMIT           = 2000
MAX_WORKERS     = 50
POLL_WAIT_INITIAL   = 3    # giây chờ ban đầu trước lần poll đầu tiên
POLL_RETRY_INTERVAL = 2    # giây chờ giữa mỗi lần retry
MAX_POLL_RETRIES    = 10   # tối đa chờ thêm 20s
REQUEST_TIMEOUT = 10

# ============================================================
# ĐỌC CSV — giữ cột is_fraud để evaluate sau
# ============================================================
print(f"📂 Đọc {CSV_FILE}...")
df = pd.read_csv(CSV_FILE, dtype={"cc_num": str})
if LIMIT:
    df = df.head(LIMIT)

# ✅ Lưu is_fraud gốc theo row_idx để join sau khi poll
ground_truth = df["is_fraud"].to_dict()  # {0: 0, 1: 0, 2: 1, ...}

fraud_count  = df["is_fraud"].sum()
normal_count = len(df) - fraud_count
print(f"✅ {len(df)} giao dịch | Fraud thực tế: {fraud_count} ({fraud_count/len(df)*100:.1f}%)"
      f" | Bình thường: {normal_count}")
print(f"   Workers: {MAX_WORKERS} | Initial wait: {POLL_WAIT_INITIAL}s | Max retries: {MAX_POLL_RETRIES}\n")

# ============================================================
# CHUYỂN CSV ROW → PAYLOAD
# ============================================================
def row_to_payload(row):
    """
    Chuyển một dòng CSV thành JSON payload gửi lên Spring Boot.

    ✅ FIX 5: Đổi key "merch_long" → "merch_long" vẫn đúng ở đây vì
    Payment.java có @JsonProperty("merch_long") map vào field merchLon.
    Tuy nhiên field "lon" (longitude của chủ thẻ) phải gửi đúng key "lon"
    vì Payment.java KHÔNG có @JsonProperty cho field lon — Jackson dùng
    tên field Java (lon) làm key JSON khi serialize/deserialize.

    Vấn đề cũ:
      "lon": float(row["long"])   ← gửi key "lon" nhưng lấy giá trị từ cột "long" của CSV ✅
      Thực ra key "lon" là đúng với Payment.java — nhưng cần xác nhận
      feature_engineering.py nhận key nào từ Spring Boot khi serialize ngược lại.

    Luồng dữ liệu đầy đủ:
      CSV cột "long"
        → payload key "lon"          (batch_tester gửi lên Spring Boot)
        → Payment.java field lon     (Spring Boot lưu DB, serialize Kafka)
        → JSON key "lon"             (Kafka message, vì không có @JsonProperty)
        → feature_engineering.py nhận "lon" → nhánh elif ['lat','lon','merch_lat','merch_long'] ✅

    Kết luận: key "lon" là ĐÚNG — đây là điểm cần giữ nguyên và document rõ.
    Key "merch_long" cũng ĐÚNG vì Payment.java có @JsonProperty("merch_long").
    """
    try:
        raw = str(row["cc_num"]).strip()
        try:
            cc_num = str(int(float(raw)))
        except:
            cc_num = raw.split(".")[0]
        return {
            "cc_num"    : cc_num,
            "amt"       : float(row["amt"]),
            "merchant"  : str(row.get("merchant", "unknown")),
            "category"  : str(row.get("category", "misc_net")),
            "location"  : str(row.get("city", "unknown")),
            # ✅ FIX 5: "lon" (không phải "long") — khớp với field lon trong Payment.java
            # Payment.java không có @JsonProperty cho lon → Jackson dùng tên field "lon" làm key
            # CSV Kaggle dùng cột "long" → lấy giá trị từ row["long"] nhưng gửi với key "lon"
            "lat"       : float(row["lat"])        if pd.notna(row.get("lat"))        else None,
            "lon"       : float(row["long"])       if pd.notna(row.get("long"))       else None,
            # "merch_lat" và "merch_long" đúng vì Payment.java có @JsonProperty cho cả 2
            "merch_lat" : float(row["merch_lat"])  if pd.notna(row.get("merch_lat"))  else None,
            "merch_long": float(row["merch_long"]) if pd.notna(row.get("merch_long")) else None,
            "unix_time" : int(row["unix_time"]),
            "city_pop"  : int(row["city_pop"])     if pd.notna(row.get("city_pop"))   else 0,
            "dob"       : str(row["dob"])          if pd.notna(row.get("dob"))        else None,
        }
    except:
        return None

# ============================================================
# PHASE 1: GỬI ĐỒNG THỜI
# ============================================================
print(f"🚀 PHASE 1: Gửi {len(df)} GD đồng thời ({MAX_WORKERS} workers)...")

send_results = []

def send_one(task):
    idx, row = task
    payload = row_to_payload(row)
    if not payload:
        return {"row_idx": idx, "payment_id": None, "amt": 0,
                "category": "unknown", "error": "build_failed"}
    try:
        resp = requests.post(SPRING_BOOT_URL, json=payload, timeout=REQUEST_TIMEOUT)
        if resp.status_code == 201:
            body = resp.json()
            return {"row_idx": idx, "payment_id": body["id"],
                    "amt": payload["amt"], "category": payload["category"],
                    "cc_num": payload["cc_num"][-4:], "error": None}
        else:
            return {"row_idx": idx, "payment_id": None, "amt": payload["amt"],
                    "category": payload["category"], "cc_num": payload["cc_num"][-4:],
                    "error": f"HTTP {resp.status_code}"}
    except Exception as e:
        return {"row_idx": idx, "payment_id": None, "amt": payload.get("amt", 0),
                "category": payload.get("category", "?"),
                "cc_num": payload.get("cc_num", "????")[-4:],
                "error": str(e)[:40]}

wall_start = time.time()
with ThreadPoolExecutor(max_workers=MAX_WORKERS) as executor:
    futures = {executor.submit(send_one, t): t for t in df.iterrows()}
    with tqdm(as_completed(futures), total=len(futures), desc="📤 Gửi") as pbar:
        for future in pbar:
            r = future.result()
            send_results.append(r)
            pbar.set_postfix({
                "ok":   sum(1 for x in send_results if x["payment_id"]),
                "fail": sum(1 for x in send_results if x["error"])
            })

wall_send = time.time() - wall_start
send_ok   = [r for r in send_results if r["payment_id"]]
send_fail = [r for r in send_results if r["error"]]

print(f"\n✅ Gửi xong: {len(send_ok)} thành công | {len(send_fail)} lỗi")
print(f"   Thời gian: {wall_send:.2f}s | Throughput: {len(send_ok)/wall_send:.1f} GD/giây")

# ============================================================
# PHASE 2 + 3: SMART POLLING — tự động retry đến khi hết PENDING
# ============================================================
def poll_one(r):
    """Poll 1 GD, tra ve status moi nhat.

    FIX LOI 4: Lay detectionLayer va fraudPatterns tu FraudCase thay vi Payment.

    Van de cu:
      Payment object KHONG co detectionLayer / fraudPatterns.
      body.get("detectionLayer", "") luon tra "" → 2 cot trong CSV luon rong.

    Fix:
      Buoc 1: GET /api/v1/payments/{id}         → status, fraudType, reason
      Buoc 2: GET /api/v1/fraud-cases?status=.. → tim FraudCase theo transactionId
              → ghep detectionLayer + fraudPatterns vao ket qua
    """
    empty = {**r, "status": "POLL_FAIL", "fraud_type": "",
             "detection_layer": "", "fraud_patterns": "", "reason": ""}
    try:
        # Buoc 1: lay Payment
        resp = requests.get(f"{SPRING_BOOT_URL}/{r['payment_id']}", timeout=REQUEST_TIMEOUT)
        if resp.status_code != 200:
            return empty
        body = resp.json()

        result = {
            **r,
            "status"         : body.get("status", "UNKNOWN"),
            "fraud_type"     : body.get("fraudType", ""),
            "detection_layer": "",
            "fraud_patterns" : "",
            "reason"         : body.get("statusReason", "") or "",
        }

        # Buoc 2: chi goi FraudCase khi GD bi nghi ngo (tranh goi thua voi APPROVED)
        if result["status"] in ("BLOCKED", "UNDER_REVIEW"):
            try:
                case_resp = requests.get(
                    FRAUD_CASE_URL,
                    params={"status": result["status"]},
                    timeout=REQUEST_TIMEOUT
                )
                if case_resp.status_code == 200:
                    cases = case_resp.json()
                    matched = next(
                        (c for c in cases if c.get("transactionId") == r["payment_id"]),
                        None
                    )
                    if matched:
                        result["detection_layer"] = matched.get("detectionLayer", "")
                        result["fraud_patterns"]  = matched.get("fraudPatterns", "")
            except Exception:
                pass  # FraudCase khong lay duoc → de trong, khong crash

        return result

    except Exception:
        return empty

def poll_batch(targets, label="📥 Polling"):
    """Poll đồng thời danh sách GD, trả về list kết quả."""
    results = []
    with ThreadPoolExecutor(max_workers=MAX_WORKERS) as executor:
        futures = {executor.submit(poll_one, r): r for r in targets}
        with tqdm(as_completed(futures), total=len(futures), desc=label) as pbar:
            for future in pbar:
                results.append(future.result())
    return results

# ── Bước 1: chờ ban đầu rồi poll toàn bộ ────────────────────
print(f"\n⏳ PHASE 2+3: Chờ {POLL_WAIT_INITIAL}s rồi smart polling...")
for i in range(POLL_WAIT_INITIAL, 0, -1):
    print(f"   Còn {i}s...", end="\r")
    time.sleep(1)
print(f"   Poll lần 1 — {len(send_ok)} GD        ")

poll_map = {}
for r in poll_batch(send_ok, "📥 Poll lần 1"):
    poll_map[r["payment_id"]] = r

# ── Bước 2: retry loop cho các GD còn PENDING ────────────────
for retry in range(1, MAX_POLL_RETRIES + 1):
    pending_list = [r for r in poll_map.values() if r["status"] == "PENDING"]
    if not pending_list:
        print(f"   ✅ Hết PENDING sau {retry} lần poll!")
        break
    print(f"   ⏳ Retry {retry}/{MAX_POLL_RETRIES}: {len(pending_list)} GD còn PENDING"
          f" — chờ {POLL_RETRY_INTERVAL}s...")
    time.sleep(POLL_RETRY_INTERVAL)
    for r in poll_batch(pending_list, f"🔄 Retry {retry}"):
        poll_map[r["payment_id"]] = r
else:
    still = sum(1 for r in poll_map.values() if r["status"] == "PENDING")
    if still:
        print(f"   ⚠️  Vẫn còn {still} GD PENDING sau {MAX_POLL_RETRIES} lần retry"
              f" — tăng MAX_POLL_RETRIES nếu cần.")

poll_results = list(poll_map.values())
print(f"   📊 Hoàn tất: {len(poll_results)} GD có kết quả\n")

# ============================================================
# BÁO CÁO HIỆU NĂNG
# ============================================================
total    = len(poll_results)
counts   = Counter(r["status"] for r in poll_results)
approved = counts.get("APPROVED", 0)
blocked  = counts.get("BLOCKED", 0)
review   = counts.get("UNDER_REVIEW", 0)
pending  = counts.get("PENDING", 0)
fail     = counts.get("POLL_FAIL", 0) + counts.get("UNKNOWN", 0)

print("\n" + "=" * 65)
print("📊 KẾT QUẢ — CONCURRENT FRAUD DETECTION TEST")
print("=" * 65)

print(f"\n⚡ HIỆU NĂNG ({MAX_WORKERS} users đồng thời):")
print(f"   Tổng GD gửi    : {len(df)}")
print(f"   Gửi thành công : {len(send_ok)}")
print(f"   Thời gian gửi  : {wall_send:.2f}s")
print(f"   Throughput     : {len(send_ok)/wall_send:.1f} GD/giây")

print(f"\n🎯 KẾT QUẢ PHÂN LOẠI ({total} GD):")
bar_a = "█" * int(approved/total*30) if total else ""
bar_b = "█" * int(blocked/total*30)  if total else ""
bar_r = "█" * int(review/total*30)   if total else ""
print(f"   ✅ APPROVED      {bar_a} {approved:>4} ({approved/total*100:.1f}%)")
print(f"   ⚠️  UNDER_REVIEW  {bar_r} {review:>4} ({review/total*100:.1f}%)")
print(f"   🚨 BLOCKED       {bar_b} {blocked:>4} ({blocked/total*100:.1f}%)")
if pending: print(f"   ⏳ PENDING (chưa xong) : {pending} → tăng MAX_POLL_RETRIES lên {MAX_POLL_RETRIES+5}")
if fail:    print(f"   ❌ Lỗi polling         : {fail}")

# ============================================================
# EVALUATION — SO SÁNH VỚI is_fraud THỰC TẾ
# ============================================================
evaluable = [r for r in poll_results
             if r["status"] not in ("POLL_FAIL", "UNKNOWN", "PENDING")]

if len(evaluable) > 0:
    y_true = [ground_truth[r["row_idx"]] for r in evaluable]
    y_pred = [1 if r["status"] in ("BLOCKED", "UNDER_REVIEW") else 0
              for r in evaluable]

    precision = precision_score(y_true, y_pred, zero_division=0)
    recall    = recall_score(y_true, y_pred, zero_division=0)
    f1        = f1_score(y_true, y_pred, zero_division=0)
    cm        = confusion_matrix(y_true, y_pred)

    tn, fp, fn, tp = cm.ravel() if cm.shape == (2, 2) else (0, 0, 0, 0)

    print(f"\n{'=' * 65}")
    print("🎯 ĐÁNH GIÁ ĐỘ CHÍNH XÁC (so với is_fraud thực tế)")
    print(f"{'=' * 65}")
    print(f"\n   Số GD evaluate được : {len(evaluable)}")
    print(f"   Fraud thực tế       : {sum(y_true)} ({sum(y_true)/len(y_true)*100:.1f}%)")
    print(f"   Hệ thống dự đoán    : {sum(y_pred)} ({sum(y_pred)/len(y_pred)*100:.1f}%)")

    print(f"\n   📐 CHỈ SỐ ĐÁNH GIÁ:")
    print(f"   Precision : {precision:.3f}  → {precision*100:.1f}% GD bị bắt là fraud thật")
    print(f"   Recall    : {recall:.3f}  → {recall*100:.1f}% fraud thật bị phát hiện")
    print(f"   F1 Score  : {f1:.3f}  → điểm tổng hợp")

    print(f"\n   📊 CONFUSION MATRIX:")
    print(f"                    Predict: Bình thường  |  Predict: Fraud")
    print(f"   Thực: Bình thường      {tn:>6}         |        {fp:>6}  ← Báo nhầm (FP)")
    print(f"   Thực: Fraud            {fn:>6}         |        {tp:>6}  ← Bắt đúng (TP)")
    print(f"\n   ✅ Bắt đúng fraud (TP)       : {tp}")
    print(f"   ❌ Bỏ sót fraud (FN)         : {fn}  ← nguy hiểm nhất!")
    print(f"   ⚠️  Báo nhầm bình thường (FP) : {fp}  ← analyst quá tải")
    print(f"   ✅ Cho qua đúng (TN)          : {tn}")

    print(f"\n{classification_report(y_true, y_pred, target_names=['Bình thường', 'Fraud'])}")

    if pending > 0:
        print(f"⚠️  Lưu ý: {pending} GD vẫn PENDING → tăng MAX_POLL_RETRIES lên {MAX_POLL_RETRIES+5}")
    if recall < 0.5:
        print(f"⚠️  Recall thấp ({recall:.2f}) → hệ thống đang bỏ sót nhiều fraud thực tế!")
    if precision < 0.3:
        print(f"⚠️  Precision thấp ({precision:.2f}) → quá nhiều false positive → analyst bị quá tải!")
else:
    print("\n⚠️  Không có GD nào để evaluate (tất cả POLL_FAIL hoặc PENDING)")

# ============================================================
# CHI TIẾT BLOCKED / UNDER_REVIEW
# ============================================================
blocked_list = [r for r in poll_results if r["status"] == "BLOCKED"]
review_list  = [r for r in poll_results if r["status"] == "UNDER_REVIEW"]

if blocked_list:
    print(f"\n🚨 BLOCKED ({len(blocked_list)} GD):")
    for r in blocked_list[:10]:
        is_real = "✅ FRAUD THẬT" if ground_truth.get(r["row_idx"], 0) == 1 else "❌ Báo nhầm"
        print(f"   ID={r['payment_id']} | ****{r['cc_num']} | ${r['amt']:>8.2f}"
              f" | {r.get('fraud_type','?'):<22} | {is_real}")
    if len(blocked_list) > 10:
        print(f"   ... và {len(blocked_list)-10} GD khác")

if review_list:
    print(f"\n⚠️  UNDER_REVIEW ({len(review_list)} GD):")
    for r in review_list[:10]:
        is_real = "✅ FRAUD THẬT" if ground_truth.get(r["row_idx"], 0) == 1 else "❌ Báo nhầm"
        print(f"   ID={r['payment_id']} | ****{r['cc_num']} | ${r['amt']:>8.2f}"
              f" | {r.get('fraud_patterns',''):<30} | {is_real}")
    if len(review_list) > 10:
        print(f"   ... và {len(review_list)-10} GD khác")

# ============================================================
# LƯU CSV KẾT QUẢ ĐẦY ĐỦ
# ============================================================
out = f"results_{MAX_WORKERS}workers.csv"
with open(out, "w", newline="", encoding="utf-8") as f:
    fieldnames = [
        "payment_id", "cc_num", "amt", "category", "status",
        "fraud_type", "detection_layer", "fraud_patterns", "reason",
        "is_fraud_actual", "predicted_fraud", "correct",
    ]
    writer = csv.DictWriter(f, fieldnames=fieldnames)
    writer.writeheader()
    for r in poll_results:
        actual    = ground_truth.get(r["row_idx"], -1)
        predicted = 1 if r["status"] in ("BLOCKED", "UNDER_REVIEW") else 0
        row = {k: r.get(k, "") for k in fieldnames}
        row["is_fraud_actual"] = actual
        row["predicted_fraud"] = predicted
        row["correct"]         = "✅" if actual == predicted else "❌"
        writer.writerow(row)

print(f"\n💾 Đã lưu: {out} (có cột is_fraud_actual, predicted_fraud, correct)")
print("=" * 65)