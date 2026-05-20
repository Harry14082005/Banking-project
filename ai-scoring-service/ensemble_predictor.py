import os
import pickle
import numpy as np
import pandas as pd
from feature_engineering import build_features


# ✅ FIX 3: Helper truy cập Series an toàn thay vì .get()
#
# Vấn đề cũ:
#   feat_row là pandas.Series, không phải dict.
#   feat_row.get('amt', 0) KHÔNG báo lỗi nhưng trả về 0 âm thầm
#   nếu key không tồn tại (Series.get() trả về None → float(None) = TypeError,
#   hoặc nếu key tồn tại nhưng value là NaN → float(NaN) = nan → so sánh sai).
#
# Giải pháp:
#   Dùng _get() kiểm tra .index trước, trả về default rõ ràng.
def _get(series, key, default=0.0):
    return float(series[key]) if key in series.index and pd.notna(series[key]) else default


class FraudEnsemblePredictor:
    def __init__(self, models_dir='models'):
        self.is_loaded = False
        try:
            with open(os.path.join(models_dir, 'rf_model.pkl'),  'rb') as f:
                self.rf_model  = pickle.load(f)
            with open(os.path.join(models_dir, 'xgb_model.pkl'), 'rb') as f:
                self.xgb_model = pickle.load(f)
            with open(os.path.join(models_dir, 'iso_model.pkl'), 'rb') as f:
                self.iso_model = pickle.load(f)
            with open(os.path.join(models_dir, 'encoders.pkl'),  'rb') as f:
                self.encoders  = pickle.load(f)
            self.is_loaded = True
            print("✅ Ensemble models loaded: RF + XGB + IsolationForest")
        except FileNotFoundError as e:
            print(f"Warning: Models not found ({e}). Run model_trainer.py first!")

    def predict(self, df):
        if not self.is_loaded:
            default_res = {
                "fraud_score" : 0.0,
                "prediction"  : "normal",
                "patterns"    : [],
                "reasons"     : []
            }
            return default_res if len(df) == 1 else [default_res] * len(df)

        X = build_features(df, encoders=self.encoders, is_training=False)

        rf_probs      = self.rf_model.predict_proba(X)[:, 1]
        xgb_probs     = self.xgb_model.predict_proba(X)[:, 1]
        iso_preds_raw = self.iso_model.predict(X)
        iso_probs     = np.where(iso_preds_raw == -1, 1.0, 0.0)

        fraud_scores = (0.4 * rf_probs) + (0.4 * xgb_probs) + (0.2 * iso_probs)

        results = []
        for i in range(len(df)):
            score = float(fraud_scores[i])
            patterns, reasons = self._detect_patterns(
                df.iloc[i], X.iloc[i], score,
                rf_probs[i], xgb_probs[i], iso_probs[i]
            )
            results.append({
                "fraud_score" : round(score, 4),
                "prediction"  : "fraud" if score > 0.5 else "normal",
                "patterns"    : patterns,
                "reasons"     : reasons
            })

        return results[0] if len(df) == 1 else results

    def _detect_patterns(self, row, feat_row, score, rf_prob, xgb_prob, iso_prob):
        patterns = []
        reasons  = []

        # ✅ FIX 3: Dùng _get() thay vì feat_row.get() / row.get()
        amt      = _get(feat_row, 'amt')
        dist_km  = _get(feat_row, 'distance_km')
        hour     = int(_get(feat_row, 'hour', 12))
        is_night = int(_get(feat_row, 'is_night', 0))

        # row là Series gốc từ df (dữ liệu thô), dùng _get để an toàn
        category = str(row['category']) if 'category' in row.index and pd.notna(row['category']) else ''

        # Pattern 1: Mua hàng online giá trị cao
        if category in ('shopping_net', 'misc_net') and amt > 300:
            patterns.append("HIGH_VALUE_ONLINE")
            reasons.append(f"Online cao: {category} ${amt:.0f}")

        # Pattern 2: Test thẻ bằng số tiền nhỏ (Card Testing)
        if 1 <= amt <= 30 and category in (
            'health_fitness', 'misc_net', 'shopping_net', 'personal_care', 'home'
        ):
            patterns.append("CARD_TESTING")
            reasons.append(f"Nghi test the: ${amt:.2f} / {category}")

        # Pattern 3: Giao dịch xa nhà bất thường
        if dist_km > 80:
            patterns.append("GEOGRAPHIC")
            reasons.append(f"Xa nha: {dist_km:.0f} km")

        # Pattern 4: Số tiền lớn bất thường
        if amt > 800:
            patterns.append("HIGH_AMOUNT")
            reasons.append(f"So tien lon: ${amt:,.0f}")

        # Pattern 5: Giao dịch đêm khuya
        if is_night:
            patterns.append("NIGHT_TRANSACTION")
            reasons.append(f"Dem khuya: {hour}h")

        # Pattern 6: IsolationForest đánh dấu là anomaly
        if iso_prob >= 1.0:
            patterns.append("ANOMALY_DETECTED")
            reasons.append(f"IsolationForest: bat thuong (RF={rf_prob:.2f}, XGB={xgb_prob:.2f})")

        return patterns, reasons