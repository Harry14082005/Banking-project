import json
import pandas as pd
from flask import Flask, request, jsonify
from ensemble_predictor import FraudEnsemblePredictor

app = Flask(__name__)

# Load predictor 1 lan khi khoi dong — khong load lai moi request
predictor = FraudEnsemblePredictor(models_dir='models')

def predict_transaction(transaction_dict):
    """
    Nhan dict hoac list dict chua thong tin giao dich.
    Tra ve ket qua theo format ma FraudTransactionConsumer mong doi:
      { riskScore, riskLevel, recommendation, patternMatched, reasons }
    """
    if isinstance(transaction_dict, list):
        df = pd.DataFrame(transaction_dict)
    else:
        df = pd.DataFrame([transaction_dict])

    result = predictor.predict(df)

    # ✅ FIX: Chuyen format tu { fraud_score, prediction }
    #         sang { riskScore, riskLevel, ... } khop voi FraudTransactionConsumer.java
    #         parseAiResponse() doc: riskScore, riskLevel, patternMatched
    def to_api_format(r):
        score = r.get("fraud_score", 0.0)

        # Nguong khop voi decideStatus() trong FraudTransactionConsumer.java:
        #   score >= 0.70 → BLOCKED
        #   score >= 0.35 → UNDER_REVIEW
        #   score <  0.35 → APPROVED
        if score >= 0.70:
            risk_level     = "HIGH"
            recommendation = "BLOCK"
        elif score >= 0.35:
            risk_level     = "MEDIUM"
            recommendation = "REVIEW"
        else:
            risk_level     = "LOW"
            recommendation = "APPROVE"

        return {
            "riskScore"      : round(score, 4),
            "riskLevel"      : risk_level,
            "recommendation" : recommendation,
            "patternMatched" : r.get("patterns", []),
            "reasons"        : r.get("reasons", []),
            "modelVersion"   : "ensemble-v2"
        }

    if isinstance(result, list):
        return [to_api_format(r) for r in result]
    return to_api_format(result)


# ✅ FIX 1: Doi endpoint /predict → /api/score
#   Ly do: FraudTransactionConsumer.java goi:
#     @Value("${ai.service.url:http://localhost:8000/api/score}")
#   Neu de /predict thi Spring Consumer se nhan 404 moi lan goi AI
@app.route('/api/score', methods=['POST'])
def score_endpoint():
    """Endpoint chinh — FraudTransactionConsumer.java goi endpoint nay."""
    data = request.get_json()
    if not data:
        return jsonify({"error": "No JSON payload provided."}), 400
    result = predict_transaction(data)
    return jsonify(result)


# Giu lai /predict de backward compatible voi batch_tester.py
@app.route('/predict', methods=['POST'])
def predict_endpoint():
    data = request.get_json()
    if not data:
        return jsonify({"error": "No JSON payload provided."}), 400
    return jsonify(predict_transaction(data))


@app.route('/health', methods=['GET'])
def health():
    return jsonify({
        "status"  : "ok" if predictor.is_loaded else "no_model",
        "models"  : ["RandomForest", "XGBoost", "IsolationForest"],
        "endpoint": "/api/score"
    })


if __name__ == '__main__':
    print("✅ Multi-AI Fraud Scoring Service khoi dong.")
    print("   Endpoint chinh : POST http://localhost:8000/api/score")
    print("   Health check   : GET  http://localhost:8000/health")
    # ✅ FIX 2: Doi port 5000 → 8000
    #   Ly do: FraudTransactionConsumer default url dung port 8000
    #   Neu de port 5000 → connection refused moi lan goi AI
    app.run(host='0.0.0.0', port=8000, debug=False)