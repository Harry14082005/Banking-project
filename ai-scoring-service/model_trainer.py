import os
import pandas as pd
import numpy as np
import pickle
from sklearn.ensemble import RandomForestClassifier, IsolationForest
from xgboost import XGBClassifier
from sklearn.metrics import classification_report, confusion_matrix, recall_score
from feature_engineering import build_features

def generate_dummy_data(n=1000):
    np.random.seed(42)
    return pd.DataFrame({
        'amt': np.random.exponential(50, n),
        'unix_time': np.linspace(1600000000, 1600000000 + 86400*30, n),
        'lat': 40.0 + np.random.randn(n),
        'long': -73.0 + np.random.randn(n),
        'merch_lat': 40.0 + np.random.randn(n),
        'merch_long': -73.0 + np.random.randn(n),
        'category': np.random.choice(['shopping_net', 'grocery', 'misc'], n),
        'merchant': np.random.choice(['M1', 'M2', 'M3'], n),
        'dob': '1980-01-01',
        'city_pop': np.random.randint(1000, 1000000, n),
        'is_fraud': np.random.binomial(1, 0.05, n)
    })

def main():
    print("Loading datasets fraudTrain.csv and fraudTest.csv...")
    try:
        # Expected manual dataset file locations
        train_df = pd.read_csv('fraudTrain.csv')
        test_df = pd.read_csv('fraudTest.csv')
    except Exception as e:
        print(f"Warning: Dataset not found ({e}).")
        print("Creating an in-memory synthetic dataset for training demonstration.")
        df = generate_dummy_data(5000)
        train_df = df.iloc[:4000].copy()
        test_df = df.iloc[4000:].copy()

    print("Step 1: Building features...")
    X_train, encoders = build_features(train_df, is_training=True)
    y_train = train_df['is_fraud']

    X_test = build_features(test_df, encoders=encoders, is_training=False)
    y_test = test_df['is_fraud']
    
    # Step 2: Train Models
    print("Training RandomForestClassifier...")
    rf_model = RandomForestClassifier(n_estimators=200, max_depth=10, class_weight="balanced", random_state=42)
    rf_model.fit(X_train, y_train)

    print("Training XGBClassifier...")
    xgb_model = XGBClassifier(
        n_estimators=300, 
        max_depth=6, 
        learning_rate=0.05, 
        subsample=0.8, 
        random_state=42, 
        eval_metric="logloss"
    )
    xgb_model.fit(X_train, y_train)

    print("Training IsolationForest...")
    iso_model = IsolationForest(contamination=0.002, random_state=42)
    iso_model.fit(X_train)

    # Evaluation on Test Set
    print("\n--- Evaluation on Test Set ---")
    rf_preds = rf_model.predict_proba(X_test)[:, 1]
    xgb_preds = xgb_model.predict_proba(X_test)[:, 1]
    
    # IsolationForest outputs -1 (anomaly) and 1 (normal)
    iso_preds_raw = iso_model.predict(X_test)
    iso_probs = np.where(iso_preds_raw == -1, 1.0, 0.0)
    
    # Ensemble voting rule applied:
    fraud_score = 0.4 * rf_preds + 0.4 * xgb_preds + 0.2 * iso_probs
    ensemble_pred = (fraud_score > 0.5).astype(int)

    print("\nClassification Report:")
    print(classification_report(y_test, ensemble_pred))
    
    print("Confusion Matrix:")
    print(confusion_matrix(y_test, ensemble_pred))
    
    print("\nFraud Detection Recall (Prioritized):")
    recall = recall_score(y_test, ensemble_pred)
    # Output optimized matching
    print(f"{recall:.4f}")

    # Save models
    print("\nSaving models to 'models/' directory...")
    os.makedirs('models', exist_ok=True)
    with open('models/rf_model.pkl', 'wb') as f:
        pickle.dump(rf_model, f)
    with open('models/xgb_model.pkl', 'wb') as f:
        pickle.dump(xgb_model, f)
    with open('models/iso_model.pkl', 'wb') as f:
        pickle.dump(iso_model, f)
    with open('models/encoders.pkl', 'wb') as f:
        pickle.dump(encoders, f)
        
    print("Models saved successfully. System ready for inference!")

if __name__ == '__main__':
    main()