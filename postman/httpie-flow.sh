#!/usr/bin/env bash
# ShopFlow — fluxo da SAGA via HTTPie (caminho feliz + ponteiros para os outros desfechos).
# Pré-requisitos: stack no ar (docker compose up -d + os 6 serviços via bootRun), httpie e jq instalados.
# Variáveis de ambiente opcionais: GATEWAY (default :8080), KEYCLOAK (default :8088).
set -euo pipefail
GATEWAY=${GATEWAY:-http://localhost:8080}
KEYCLOAK=${KEYCLOAK:-http://localhost:8088}

# 1) Token de CUSTOMER (password grant)
TOKEN=$(http --form POST "$KEYCLOAK/realms/shopflow/protocol/openid-connect/token" \
  client_id=shopflow-gateway client_secret=shopflow-gateway-secret \
  grant_type=password username=customer password=customer | jq -r .access_token)

# 2) Cria o pedido do caminho feliz (2 x 400.00 = 800.00, abaixo do limite do PSP)
ORDER_ID=$(http POST "$GATEWAY/orders" "Authorization:Bearer $TOKEN" \
  customerId=3f8b1c2d-4e5f-6a7b-8c9d-0e1f2a3b4c5d \
  items:='[{"productId":"a1111111-1111-1111-1111-111111111111","quantity":2,"unitPrice":400.00}]' \
  | jq -r .id)
echo "order_id=$ORDER_ID"

# 3) Aguarda a saga (assíncrona) e consulta os três serviços
sleep 2
http GET "$GATEWAY/orders/$ORDER_ID"        "Authorization:Bearer $TOKEN"   # status: CONFIRMED
http GET "$GATEWAY/payments/$ORDER_ID"      "Authorization:Bearer $TOKEN"   # status: AUTHORIZED
http GET "$GATEWAY/notifications/$ORDER_ID" "Authorization:Bearer $TOKEN"   # ORDER_CREATED + ORDER_CONFIRMED

# --- Outros desfechos (rode trocando o corpo do POST /orders) ---
# Cancela (compensação):  items:='[{"productId":"a1111111-1111-1111-1111-111111111111","quantity":3,"unitPrice":400.00}]'  # total 1200 -> CANCELLED
# Rejeita (sem estoque):  items:='[{"productId":"a4444444-4444-4444-4444-444444444444","quantity":1,"unitPrice":400.00}]'  # -> REJECTED
#
# --- Estoque (exige role ADMIN: troque o usuário do token para manager/manager) ---
# ADMIN_TOKEN=$(http --form POST "$KEYCLOAK/realms/shopflow/protocol/openid-connect/token" \
#   client_id=shopflow-gateway client_secret=shopflow-gateway-secret \
#   grant_type=password username=manager password=manager | jq -r .access_token)
# http GET "$GATEWAY/stock" "Authorization:Bearer $ADMIN_TOKEN"
