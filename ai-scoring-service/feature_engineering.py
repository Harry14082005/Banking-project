import pandas as pd
import numpy as np

def haversine(lat1, lon1, lat2, lon2):
    R = 6371.0
    lat1, lon1, lat2, lon2 = map(np.radians, [lat1, lon1, lat2, lon2])
    dlat = lat2 - lat1
    dlon = lon2 - lon1
    a = np.sin(dlat/2)**2 + np.cos(lat1) * np.cos(lat2) * np.sin(dlon/2)**2
    c = 2 * np.arcsin(np.sqrt(a))
    return R * c


# ✅ FIX 1 + FIX 4: Hàm parse dob riêng, hỗ trợ đa format, trả về tuổi float
#
# Vấn đề cũ:
#   pd.to_datetime(df['dob'], errors='coerce') chỉ nhận ISO 8601 (yyyy-MM-dd).
#   Dataset Kaggle dùng M/d/yyyy (VD: "9/10/1958") → trả về NaT toàn bộ
#   → (dt - NaT) = NaT → fillna(30.0) che giấu lỗi thay vì fix
#   → feature 'age' = 30.0 cho ~100% dataset → vô nghĩa với model
#
# Giải pháp:
#   Thử lần lượt các format, format nào parse được dùng luôn.
#   Tính tuổi so với mốc tham chiếu REF_DATE = 2020-01-01 để khớp dataset Kaggle.
#   Khi inference (dt là timestamp giao dịch), dùng dt thay vì REF_DATE
#   để tránh NaT do timezone không khớp khi trừ 2 Series.

REF_DATE = pd.Timestamp('2020-01-01')  # Mốc cố định khớp với Kaggle fraudTrain/Test
DOB_FORMATS = ['%Y-%m-%d', '%m/%d/%Y', '%m/%d/%y', '%d/%m/%Y']


def _parse_dob_series(dob_series, ref):
    """
    Parse Series ngày sinh thử lần lượt các format.
    Trả về Series tuổi (float), NaN → 40.0.

    ref: có thể là pd.Timestamp (training) hoặc pd.Series[Timestamp] (inference).
    """
    # Khởi tạo kết quả với NaN
    parsed = pd.Series(pd.NaT, index=dob_series.index)

    for fmt in DOB_FORMATS:
        # Chỉ thử parse các dòng chưa parse được
        mask = parsed.isna()
        if not mask.any():
            break
        try:
            attempt = pd.to_datetime(dob_series[mask], format=fmt, errors='coerce')
            parsed[mask] = attempt
        except Exception:
            continue

    # Tính tuổi: (ref - dob) / 365.25
    # ref là Timestamp → broadcast tự động
    # ref là Series     → phép trừ element-wise, không có NaT do timezone
    age = (ref - parsed).dt.days / 365.25

    # Giá trị hợp lệ: 0 < tuổi < 120
    age = age.where((age > 0) & (age < 120), other=np.nan)
    return age.fillna(40.0)


def build_features(df, encoders=None, is_training=False):
    """
    Biến đổi dữ liệu giao dịch thành features cho mô hình ML.

    Tên cột đầu vào:
      Training (từ CSV)    : lat, long, merch_lat, merch_long
      Inference (từ Flink) : lat, lon,  merchLat,  merchLon
    """
    df_feat = pd.DataFrame(index=df.index)

    # ── Time features ────────────────────────────────────────────────
    if 'unix_time' in df.columns:
        dt = pd.to_datetime(df['unix_time'], unit='s')
    elif 'unixTime' in df.columns:
        dt = pd.to_datetime(df['unixTime'], unit='s')
    elif 'trans_date_trans_time' in df.columns:
        dt = pd.to_datetime(df['trans_date_trans_time'])
    else:
        dt = pd.Series(pd.Timestamp('now'), index=df.index)

    df_feat['hour']        = dt.dt.hour
    df_feat['day_of_week'] = dt.dt.dayofweek
    df_feat['is_weekend']  = df_feat['day_of_week'].isin([5, 6]).astype(int)
    df_feat['is_night']    = ((df_feat['hour'] >= 2) & (df_feat['hour'] <= 5)).astype(int)

    # ── Amount features ──────────────────────────────────────────────
    if 'amt' in df.columns:
        df_feat['amt'] = df['amt']
    elif 'amount' in df.columns:
        df_feat['amt'] = df['amount']
    else:
        df_feat['amt'] = 0.0
    df_feat['amt_log'] = np.log1p(df_feat['amt'])

    # ── Location / Distance features ─────────────────────────────────
    if 'distance' in df.columns:
        df_feat['distance_km'] = df['distance']
    elif all(c in df.columns for c in ['lat', 'lon', 'merch_lat', 'merch_long']):
        df_feat['distance_km'] = haversine(
            df['lat'].values, df['lon'].values,
            df['merch_lat'].values, df['merch_long'].values
        )
    elif all(c in df.columns for c in ['lat', 'long', 'merch_lat', 'merch_long']):
        df_feat['distance_km'] = haversine(
            df['lat'].values, df['long'].values,
            df['merch_lat'].values, df['merch_long'].values
        )
    elif all(c in df.columns for c in ['lat', 'lon', 'merchLat', 'merchLon']):
        df_feat['distance_km'] = haversine(
            df['lat'].values, df['lon'].values,
            df['merchLat'].values, df['merchLon'].values
        )
    else:
        df_feat['distance_km'] = 0.0

    df_feat['distance_log'] = np.log1p(df_feat['distance_km'])

    # ── Demographic features ─────────────────────────────────────────
    # ✅ FIX 1 + FIX 4: Dùng _parse_dob_series thay vì pd.to_datetime trực tiếp.
    #
    # Training  → ref = REF_DATE (Timestamp cố định, khớp Kaggle)
    # Inference → ref = dt (Series timestamp giao dịch, tránh NaT do timezone)
    if 'dob' in df.columns:
        ref = REF_DATE if is_training else dt
        df_feat['age'] = _parse_dob_series(df['dob'].astype(str), ref)
    else:
        df_feat['age'] = 40.0

    if 'city_pop' in df.columns:
        df_feat['city_pop_log'] = np.log1p(df['city_pop'])
    elif 'cityPop' in df.columns:
        df_feat['city_pop_log'] = np.log1p(df['cityPop'])
    else:
        df_feat['city_pop_log'] = 10.0

    # ── Categorical encoding ─────────────────────────────────────────
    categories = ['category', 'merchant']

    if is_training:
        from sklearn.preprocessing import LabelEncoder
        encoders = {}
        for col in categories:
            le = LabelEncoder()
            series = df[col].astype(str) if col in df.columns else pd.Series(['UNKNOWN'] * len(df))
            df_feat[col + '_encoded'] = le.fit_transform(series)

            classes = list(le.classes_)
            if 'UNKNOWN' not in classes:
                classes.append('UNKNOWN')
            le.classes_ = np.array(classes)
            encoders[col] = le

        return df_feat, encoders

    else:
        for col in categories:
            if encoders and col in encoders:
                le = encoders[col]
                if col in df.columns:
                    series = df[col].astype(str).values
                    mapping = {cls: idx for idx, cls in enumerate(le.classes_)}
                    unknown_idx = mapping.get('UNKNOWN', len(le.classes_) - 1)
                    df_feat[col + '_encoded'] = [mapping.get(x, unknown_idx) for x in series]
                else:
                    df_feat[col + '_encoded'] = len(le.classes_) - 1
            else:
                df_feat[col + '_encoded'] = 0
        return df_feat