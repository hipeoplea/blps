#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080/api}"

echo "1) Guest initiates service request"
REQ_JSON=$(curl -sS -X POST "${BASE_URL}/service-requests" \
  -H "Content-Type: application/json" \
  -d '{
    "guestId": "guest-100",
    "hostId": "host-900",
    "message": "Можно организовать ранний check-in с завтраком?"
  }')
echo "$REQ_JSON"
REQUEST_ID=$(echo "$REQ_JSON" | grep -o '"id":"[^"]*"' | head -n1 | cut -d'"' -f4)
if [[ -z "${REQUEST_ID:-}" ]]; then
  echo "Failed to extract service request id from response"
  exit 1
fi

echo "2) Host proposes terms"
curl -sS -X POST "${BASE_URL}/service-requests/${REQUEST_ID}/terms" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 45.00,
    "currency": "USD",
    "terms": "Ранний check-in в 10:00 и континентальный завтрак"
  }'
echo

echo "3) Host creates payment request"
PAYMENT_JSON=$(curl -sS -X POST "${BASE_URL}/service-requests/${REQUEST_ID}/payment-requests" \
  -H "Content-Type: application/json")
echo "$PAYMENT_JSON"
PAYMENT_ID=$(echo "$PAYMENT_JSON" | grep -o '"paymentRequest":{"id":"[^"]*"' | head -n1 | cut -d'"' -f6)
if [[ -z "${PAYMENT_ID:-}" ]]; then
  echo "Failed to extract payment request id from response"
  exit 1
fi

echo "4a) Guest pays"
curl -sS -X POST "${BASE_URL}/payment-requests/${PAYMENT_ID}/pay"
echo

echo "5) Read final request state"
curl -sS "${BASE_URL}/service-requests/${REQUEST_ID}"
echo

echo "Alternative branch 4b) Guest rejects"
echo "curl -X POST ${BASE_URL}/payment-requests/${PAYMENT_ID}/reject"
