curl -i -X POST http://localhost:8081/api/v1/notifications -H "Content-Type: application/json" -d '{
  "recipient": "user@example.com",
  "message": "Welcome to the jungle!",
  "channel": "EMAIL",
  "metadata": {}
}'
