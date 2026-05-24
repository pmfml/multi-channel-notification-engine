curl -i -X POST http://localhost:8081/api/v1/notifications \
-H "Content-Type: application/json" \
-d '{
  "recipient": "+5511999999999",
  "message": "Teste de SMS direto do meu Spring Boot via AWS SNS!",
  "channel": "SMS",
  "metadata": {}
}'
