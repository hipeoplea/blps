#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080/api}"

post_msg() {
  local chat_id="$1"
  local sender="$2"
  local message="$3"
  curl -sS -X POST "${BASE_URL}/chats/${chat_id}/messages" \
    -H "Content-Type: application/json" \
    -d "{\"sender\":\"${sender}\",\"message\":\"${message}\"}"
}

echo "1) Guest starts chat"
CHAT_JSON=$(curl -sS -X POST "${BASE_URL}/chats" \
  -H "Content-Type: application/json" \
  -d '{
    "guestId": "guest-100",
    "hostId": "host-900",
    "message": "Можно организовать ранний check-in с завтраком?"
  }')
echo "$CHAT_JSON"
CHAT_ID=$(echo "$CHAT_JSON" | grep -o '"id":"[^"]*"' | head -n1 | cut -d'"' -f4)
if [[ -z "${CHAT_ID:-}" ]]; then
  echo "Failed to extract chat id"
  exit 1
fi

echo "2) Chat dialog before payment request"
post_msg "${CHAT_ID}" "HOST" "Да, могу организовать ранний заезд и завтрак."
echo
post_msg "${CHAT_ID}" "GUEST" "Отлично, а какая итоговая стоимость?"
echo
post_msg "${CHAT_ID}" "HOST" "45 USD, включает заезд в 10:00 и континентальный завтрак."
echo
post_msg "${CHAT_ID}" "GUEST" "Подходит, создавайте заявку на оплату."
echo

echo "3) Host creates extra-service request"
REQ_AFTER_CREATE=$(curl -sS -X POST "${BASE_URL}/chats/${CHAT_ID}/extra-service/request" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Ранний check-in + завтрак",
    "description": "Заезд в 10:00 и континентальный завтрак",
    "amount": 45.00,
    "currency": "USD"
  }')
echo "$REQ_AFTER_CREATE"
EXTRA_REQUEST_ID=$(echo "$REQ_AFTER_CREATE" | grep -o '"extraServiceRequests":\[{"id":"[^"]*"' | head -n1 | cut -d'"' -f6)
if [[ -z "${EXTRA_REQUEST_ID:-}" ]]; then
  echo "Failed to extract extra service request id"
  exit 1
fi

echo "4a) Guest confirms payment; platform processing SUCCESS"
curl -sS -X POST "${BASE_URL}/chats/${CHAT_ID}/extra-service/requests/${EXTRA_REQUEST_ID}/pay" \
  -H "Content-Type: application/json" \
  -d '{"result":"SUCCESS"}'
echo

echo "5) Host marks service delivered"
curl -sS -X POST "${BASE_URL}/chats/${CHAT_ID}/extra-service/requests/${EXTRA_REQUEST_ID}/deliver"
echo

echo "6) Read final chat aggregate"
curl -sS "${BASE_URL}/chats/${CHAT_ID}"
echo

echo "Alternative branch 4b) Guest rejects payment"
echo "curl -X POST ${BASE_URL}/chats/${CHAT_ID}/extra-service/requests/${EXTRA_REQUEST_ID}/reject"

echo "Alternative branch 4c) Guest confirms, but processing FAILED"
echo "curl -X POST ${BASE_URL}/chats/${CHAT_ID}/extra-service/requests/${EXTRA_REQUEST_ID}/pay -H 'Content-Type: application/json' -d '{\"result\":\"FAILED\"}'"
