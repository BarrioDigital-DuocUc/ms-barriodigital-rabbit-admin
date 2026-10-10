# ms-barriodigital-rabbit-admin

Microservicio administrador de RabbitMQ para BarrioDigital.

Expone una API REST versionada que permite crear y eliminar colas, exchanges y bindings de forma controlada y validada.

El servicio se conecta al clúster RabbitMQ desplegado mediante la infraestructura del proyecto y encapsula toda la lógica administrativa en un servicio dedicado.

---

## Responsabilidad

Este microservicio permite administrar dinámicamente la topología RabbitMQ utilizada por BarrioDigital.

Operaciones disponibles:

- Crear queues.
- Eliminar queues.
- Crear exchanges.
- Eliminar exchanges.
- Crear bindings.
- Eliminar bindings.
- Validar parámetros antes de ejecutar operaciones sobre RabbitMQ.

No utiliza base de datos.

---

# Arquitectura

```text
Cliente / Postman
       │
       ▼
/api/v1/rabbit/...
       │
       ▼
RabbitAdminController
       │
       ▼
RabbitAdminService
       │
       ▼
Spring RabbitAdmin
       │
       ▼
RabbitMQ Cluster
   ┌───────┴───────┐
   ▼               ▼
rabbitmq-1     rabbitmq-2
```

La administración de RabbitMQ se encuentra separada de los controladores.

El controller únicamente recibe y valida solicitudes REST.

La lógica de creación y eliminación de recursos está encapsulada en:

```text
RabbitAdminService
```

---

# API REST

Base URL local:

```text
http://localhost:8086
```

Todas las APIs están versionadas mediante:

```text
/api/v1
```

---

## Queues

### Crear queue

```http
POST /api/v1/rabbit/queues
```

Ejemplo:

```json
{
  "name": "q.test.admin",
  "durable": true,
  "exclusive": false,
  "autoDelete": false
}
```

También permite configurar DLX:

```json
{
  "name": "q.example",
  "durable": true,
  "exclusive": false,
  "autoDelete": false,
  "deadLetterExchange": "cmd.dead.dlx",
  "deadLetterRoutingKey": "example.dlq"
}
```

Respuesta:

```json
{
  "operation": "CREATE",
  "resource": "QUEUE",
  "name": "q.test.admin",
  "message": "Cola creada correctamente"
}
```

Código HTTP:

```text
201 Created
```

---

### Eliminar queue

```http
DELETE /api/v1/rabbit/queues/{name}
```

Ejemplo:

```http
DELETE /api/v1/rabbit/queues/q.test.admin
```

Código HTTP esperado:

```text
200 OK
```

---

# Exchanges

## Crear exchange DIRECT

```http
POST /api/v1/rabbit/exchanges
```

```json
{
  "name": "test.admin.direct",
  "type": "DIRECT",
  "durable": true,
  "autoDelete": false
}
```

---

## Crear exchange TOPIC

```http
POST /api/v1/rabbit/exchanges
```

```json
{
  "name": "test.admin.topic",
  "type": "TOPIC",
  "durable": true,
  "autoDelete": false
}
```

Tipos soportados:

```text
DIRECT
TOPIC
```

Código HTTP esperado:

```text
201 Created
```

---

## Eliminar exchange

```http
DELETE /api/v1/rabbit/exchanges/{name}
```

Ejemplo:

```http
DELETE /api/v1/rabbit/exchanges/test.admin.direct
```

Código HTTP esperado:

```text
200 OK
```

---

# Bindings

## Crear binding

```http
POST /api/v1/rabbit/bindings
```

```json
{
  "exchange": "test.admin.direct",
  "queue": "q.test.admin",
  "routingKey": "admin.test"
}
```

Flujo resultante:

```text
test.admin.direct
        │
        │ admin.test
        ▼
q.test.admin
```

Código HTTP esperado:

```text
201 Created
```

---

## Eliminar binding

```http
DELETE /api/v1/rabbit/bindings
```

Body:

```json
{
  "exchange": "test.admin.direct",
  "queue": "q.test.admin",
  "routingKey": "admin.test"
}
```

Código HTTP esperado:

```text
200 OK
```

---

# Validaciones

La API utiliza DTOs con Jakarta Validation.

Se validan, entre otros:

- Nombres vacíos.
- Tamaño máximo.
- Caracteres inválidos.
- Tipo de exchange.
- Routing key.
- Configuraciones DLX incompletas.
- Combinaciones inválidas de queue durable/exclusive.

Ejemplo inválido:

```json
{
  "name": " ",
  "durable": true,
  "exclusive": false,
  "autoDelete": false
}
```

Respuesta:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Datos de entrada inválidos",
  "errors": {
    "name": [
      "El nombre de la cola es obligatorio",
      "El nombre de la cola contiene caracteres inválidos"
    ]
  }
}
```

Código HTTP:

```text
400 Bad Request
```

---

# Manejo global de errores

El manejo de excepciones está centralizado en:

```text
exception/GlobalExceptionHandler.java
```

Se manejan:

```text
MethodArgumentNotValidException
ConstraintViolationException
IllegalArgumentException
AmqpException
```

Las respuestas incluyen:

```text
timestamp
status
error
message
errors/detail
```

---

# RabbitAdminService

Toda la lógica administrativa está encapsulada en:

```text
service/RabbitAdminService.java
```

Responsabilidades:

```text
createQueue()
deleteQueue()

createExchange()
deleteExchange()

createBinding()
deleteBinding()
```

Los controladores no utilizan directamente las operaciones internas de RabbitMQ.

---

# RabbitMQ

Configuración local:

```properties
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=barrio
spring.rabbitmq.password=barrio123
```

Variables de entorno soportadas:

```text
SPRING_RABBITMQ_HOST
SPRING_RABBITMQ_PORT
SPRING_RABBITMQ_USERNAME
SPRING_RABBITMQ_PASSWORD
```

---

# Infraestructura RabbitMQ

La infraestructura se encuentra en:

```text
infra/mq
```

y está compuesta por dos nodos:

```text
rabbit@rabbitmq-1
rabbit@rabbitmq-2
```

Durante las pruebas se verificó mediante:

```powershell
docker exec bd-rabbitmq-1 rabbitmqctl cluster_status
```

que ambos nodos estaban presentes como:

```text
Disk Nodes
rabbit@rabbitmq-1
rabbit@rabbitmq-2

Running Nodes
rabbit@rabbitmq-1
rabbit@rabbitmq-2
```

La interfaz de administración está disponible localmente en:

```text
http://localhost:15672
```

---

# Ejecución local

Primero levantar RabbitMQ:

```powershell
cd C:\Users\marce\Desktop\BarrioDigital\infra\mq

docker compose up -d
```

Verificar:

```powershell
docker compose ps
```

Luego iniciar Rabbit Admin:

```powershell
cd C:\Users\marce\Desktop\BarrioDigital\ms-barriodigital-rabbit-admin

.\mvnw.cmd spring-boot:run
```

Servicio:

```text
http://localhost:8086
```

---

# Pruebas realizadas

Se verificó correctamente:

```text
POST /api/v1/rabbit/exchanges
→ 201 Created

POST /api/v1/rabbit/queues
→ 201 Created

POST /api/v1/rabbit/bindings
→ 201 Created

POST /api/v1/rabbit/queues con datos inválidos
→ 400 Bad Request

DELETE /api/v1/rabbit/bindings
→ operación correcta

DELETE /api/v1/rabbit/queues/{name}
→ operación correcta

DELETE /api/v1/rabbit/exchanges/{name}
→ operación correcta
```

Las operaciones fueron verificadas contra RabbitMQ real mediante RabbitMQ Management.

---

# Estructura principal

```text
src/main/java/cl/duoc/barriodigital/rabbitadmin
│
├── config
│   └── RabbitAdminConfig.java
│
├── controller
│   └── RabbitAdminController.java
│
├── dto
│   ├── QueueRequest.java
│   ├── ExchangeRequest.java
│   ├── BindingRequest.java
│   └── OperationResponse.java
│
├── service
│   └── RabbitAdminService.java
│
├── exception
│   └── GlobalExceptionHandler.java
│
└── MsBarriodigitalRabbitAdminApplication.java
```

---

# Tecnologías

- Java 17
- Spring Boot 4
- Spring Web MVC
- Spring AMQP
- Jakarta Validation
- RabbitMQ
- Maven
- Docker
- Docker Compose

---

# Relación con BarrioDigital

```text
ms-barriodigital-requests
        │
        │ productor
        ▼
     RabbitMQ
        │
        ▼
ms-barriodigital-notify
     consumidor


ms-barriodigital-rabbit-admin
        │
        │ administración
        ▼
     RabbitMQ
```

`ms-barriodigital-rabbit-admin` administra la topología del broker, mientras que Requests y Notify utilizan RabbitMQ para el procesamiento asíncrono de comandos.