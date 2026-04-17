# 🐰 Projeto RabbitMQ com Java - Estudo Básico

Este projeto foi criado com o objetivo de praticar e relembrar os conceitos fundamentais do RabbitMQ utilizando Java, com foco em comunicação assíncrona entre serviços através de filas.

---

## 📚 Objetivo

- Entender o funcionamento de filas (queues)
- Praticar envio e consumo de mensagens com Java
- Reforçar conceitos de mensageria
- Simular processamento assíncrono
- Consolidar base para sistemas distribuídos

---

## ⚙️ Tecnologias Utilizadas

- RabbitMQ
- Java
- Maven ou Gradle
- Docker / Docker Compose

---

## 🧠 Conceitos Aplicados

- Producer (produtor de mensagens)
- Consumer (consumidor de mensagens)
- Queue (fila)
- Exchange (opcional)
- Binding (opcional)
- Comunicação assíncrona

---

## 🐳 Subindo o RabbitMQ com Docker Compose

### docker-compose.yml

```yaml
version: "3.8"

services:
  rabbitmq:
    image: rabbitmq:3-management
    container_name: rabbitmq
    hostname: rabbitmq
    ports:
      - "5672:5672"     # Porta da aplicação
      - "15672:15672"   # Painel web
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    restart: unless-stopped

volumes:
  rabbitmq_data:

▶️ Executar
docker-compose up -d

🌐 Acessar painel do RabbitMQ
URL: http://localhost:15672
Usuário: guest
Senha: guest