# ShopFlow — fluxo da SAGA (caminho feliz): pedido -> CONFIRMED, entrando pela borda (api-gateway :8080).
#
# Pré-requisitos (a stack precisa estar de pé):
#   docker compose up -d                         # infra (Kafka, PostgreSQL, Keycloak, ...)
#   ./gradlew :discovery-server:bootRun          # + order/inventory/payment/notification/api-gateway
#
# Gerar o GIF (rode a partir desta pasta para que a saída caia em doc/demo/flow.gif):
#   cd doc/demo && vhs flow.tape
#
# Requer: vhs (https://github.com/charmbracelet/vhs), curl e jq no PATH.

Output flow.gif

Require curl
Require jq

Set Shell bash
Set FontSize 16
Set Width 1280
Set Height 820
Set Padding 18
Set Theme "Dracula"
Set TypingSpeed 25ms

Type "# ShopFlow — caminho feliz: pedido -> CONFIRMED (via api-gateway :8080)"
Enter
Sleep 800ms

Type "# 1) Token de CUSTOMER no Keycloak (password grant)"
Enter
Type `TOKEN=$(curl -s -d "client_id=shopflow-gateway" -d "client_secret=shopflow-gateway-secret" -d "grant_type=password" -d "username=customer" -d "password=customer" http://localhost:8088/realms/shopflow/protocol/openid-connect/token | jq -r .access_token)`
Enter
Sleep 1500ms

Type "# 2) Cria o pedido (2 x 400.00 = 800.00, abaixo do limite do PSP)"
Enter
Type `ORDER=$(curl -s -X POST http://localhost:8080/orders -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"customerId":"3f8b1c2d-4e5f-6a7b-8c9d-0e1f2a3b4c5d","items":[{"productId":"a1111111-1111-1111-1111-111111111111","quantity":2,"unitPrice":400.00}]}' | jq -r .id)`
Enter
Sleep 1200ms
Type `echo "Pedido criado: $ORDER"`
Enter
Sleep 1500ms

Type "# 3) A saga roda em background: reserva estoque -> autoriza pagamento -> confirma..."
Enter
Sleep 2500ms

Type "# Estado do pedido:"
Enter
Type `curl -s http://localhost:8080/orders/$ORDER -H "Authorization: Bearer $TOKEN" | jq '{id, status, totalAmount}'`
Enter
Sleep 2000ms

Type "# Pagamento:"
Enter
Type `curl -s http://localhost:8080/payments/$ORDER -H "Authorization: Bearer $TOKEN" | jq '{orderId, amount, status}'`
Enter
Sleep 2000ms

Type "# Notificacoes registradas (tipos):"
Enter
Type `curl -s http://localhost:8080/notifications/$ORDER -H "Authorization: Bearer $TOKEN" | jq '[.[] | .type]'`
Enter
Sleep 3500ms
