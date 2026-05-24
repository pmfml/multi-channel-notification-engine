curl -i -X POST http://localhost:8081/api/v1/notifications \
-H "Content-Type: application/json" \
-d '{
  "recipient": "aaaaaaa@eeeee.com",
  "message": "Isso é um teste real enviado direto da minha aplicação para a AWS!!",
  "channel": "EMAIL",
  "metadata": {}
}'
