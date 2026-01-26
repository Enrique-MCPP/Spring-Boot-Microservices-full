# 🏗️ Arquitectura del Sistema de Microservicios

> **📅 Actualizado**: 26 de Enero 2026
> **🎯 Estado**: Proyecto refactorizado con **DDD + CQRS + Circuit Breaker + Tests**
> **💻 Stack**: Spring Boot 4.0 · Java 21 · Kafka 4.1 · gRPC (stubs) · PostgreSQL · MongoDB

---

## ✅ CHECKLIST - Buenas Prácticas Implementadas

### 🧠 DDD (Domain-Driven Design)
- ✅ **Dominio rico** - Lógica de negocio en entidades, NO en services
- ✅ **Value Objects** - Money, Quantity, OrderNumber (inmutables)
- ✅ **Factory methods** - `Order.create()` protege invariantes
- ✅ **Sin modelo anémico** - Entidades con comportamiento real
- ✅ **Independiente de infra** - Dominio no depende de Spring/JPA

### 🔁 CQRS (Command Query Responsibility Segregation)
- ✅ **Commands** - `PlaceOrderCommand` (inmutable con Java records)
- ✅ **Handlers** - `PlaceOrderCommandHandler` orquesta casos de uso
- ✅ **Separación** - Commands cambian estado, Queries leen
- ✅ **@Transactional** - En capa de aplicación, NO en controllers

### 🛡️ Resiliencia y Observabilidad
- ✅ **Circuit Breaker** - `@CircuitBreaker` explícito con fallback
- ✅ **Retry** - Reintentos automáticos configurados
- ✅ **Timeout** - Límites de tiempo en operaciones externas
- ✅ **Tracing** - Zipkin para seguimiento distribuido

### 🧪 Testing
- ✅ **Tests sin Spring** - Dominio testeado aisladamente (21 tests ✅)
- ✅ **TDD-friendly** - Lógica de negocio fácil de testear
- ✅ **Rápidos** - Tests unitarios en milisegundos

### 🌐 Comunicación
- ✅ **REST** - Cliente → API Gateway (externo)
- ✅ **REST** - Order → Inventory (interno, usando WebClient)
- ✅ **Kafka** - Eventos asíncronos (OrderPlacedEvent)
- 🟡 **gRPC** - Preparado (protobuf + stubs), pendiente implementación completa

### 📁 Arquitectura de Capas
```
Controller (REST) → Command/Query → Domain → Repository
    ↓                    ↓              ↓          ↓
  HTTP              Orquesta       Lógica      Persistencia
  Solo recibe      casos de uso   de negocio   (JPA/Mongo)
```

---

## 📊 Diagrama de Arquitectura

```
                                    ┌─────────────────┐
                                    │   Keycloak      │
                                    │ (Autenticación) │
                                    └────────┬────────┘
                                             │ JWT
                                             ▼
┌──────────┐          ┌──────────────────────────────────┐
│  Cliente │  ────▶   │       API Gateway                │
│ (Postman)│          │   (Puerto 8080)                  │
└──────────┘          │   - Enrutamiento                 │
                      │   - Autenticación OAuth2         │
                      └────────┬─────────────────────────┘
                               │
                ┌──────────────┴──────────────┐
                │                             │
                ▼                             ▼
    ┌───────────────────┐         ┌───────────────────┐
    │  Discovery Server │         │   Zipkin          │
    │   (Eureka)        │◀────────│   (Tracing)       │
    │   Puerto 8761     │         │   Puerto 9411     │
    └─────────┬─────────┘         └───────────────────┘
              │
      ┌───────┴────────┬─────────────┬──────────────┐
      │                │             │              │
      ▼                ▼             ▼              ▼
┌──────────┐    ┌──────────┐  ┌──────────┐  ┌─────────────┐
│ Product  │    │  Order   │  │Inventory │  │Notification │
│ Service  │    │ Service  │  │ Service  │  │  Service    │
└────┬─────┘    └────┬─────┘  └────┬─────┘  └──────┬──────┘
     │               │             │                │
     ▼               ▼             ▼                │
┌─────────┐    ┌──────────┐  ┌──────────┐          │
│ MongoDB │    │PostgreSQL│  │PostgreSQL│          │
│  27017  │    │   5431   │  │   5432   │          │
└─────────┘    └──────────┘  └──────────┘          │
                     │                              │
                     └──────────▶ Kafka ◀───────────┘
                              (Puerto 9092)
```

---

## 🎯 Microservicios

### 1️⃣ **Product Service**
- **Puerto**: Aleatorio (registrado en Eureka)
- **Base de Datos**: MongoDB (puerto 27017)
- **Función**: Gestión del catálogo de productos
- **Endpoints**:
  - `POST /api/product` - Crear producto
  - `GET /api/product` - Listar productos

**Flujo**:
```
Cliente → API Gateway → Product Service → MongoDB
```

---

### 2️⃣ **Order Service** ⭐
- **Puerto**: Aleatorio (registrado en Eureka)
- **Base de Datos**: PostgreSQL (puerto 5431)
- **Función**: Gestión de pedidos
- **Endpoints**:
  - `POST /api/order` - Crear pedido

**Flujo de creación de pedido**:
```
1. Cliente envía pedido → API Gateway (valida JWT)
2. API Gateway → Order Service (con JWT)
3. Order Service:
   a. Captura el JWT del SecurityContext
   b. Llama a Inventory Service (con JWT) para verificar stock
   c. Si hay stock → Guarda pedido en PostgreSQL
   d. Envía evento a Kafka (OrderPlacedEvent)
4. Notification Service escucha Kafka y procesa notificación
```

**Código clave** ([OrderService.java](order-service/src/main/java/com/programmingtechie/orderservice/service/OrderService.java)):
```java
// Captura JWT antes de llamar a Inventory
var authentication = SecurityContextHolder.getContext().getAuthentication();
if (authentication instanceof JwtAuthenticationToken jwtAuth) {
    bearerToken = jwtAuth.getToken().getTokenValue();
}

// Llama a Inventory Service con el token
webClientBuilder.build().get()
    .uri("http://inventory-service/api/inventory?skuCodeList=" + skuCodesList)
    .header("Authorization", "Bearer " + token)
    .retrieve()
    .bodyToMono(InventoryResponse[].class)
    .block();

// Envía evento a Kafka
kafkaTemplate.send("notificationTopic", new OrderPlacedEvent(orderNumber));
```

---

### 3️⃣ **Inventory Service**
- **Puerto**: Aleatorio (registrado en Eureka)
- **Base de Datos**: PostgreSQL (puerto 5432)
- **Función**: Gestión de inventario
- **Endpoints**:
  - `GET /api/inventory?skuCodeList=xxx` - Verificar stock

**Flujo**:
```
Order Service → WebClient (con JWT) → Inventory Service → PostgreSQL
```

---

### 4️⃣ **Notification Service**
- **Puerto**: Aleatorio (registrado en Eureka)
- **Base de Datos**: Ninguna (solo consume eventos)
- **Función**: Procesar notificaciones de pedidos

**Flujo**:
```
Order Service → Kafka (topic: notificationTopic) → Notification Service
```

**Código clave** ([NotificationServiceApplication.java](notification-service/src/main/java/com/programming/techie/notificationservice/NotificationServiceApplication.java)):
```java
@KafkaListener(topics = "notificationTopic")
public void handleNotification(OrderPlacedEvent orderPlacedEvent) {
    log.info("Received Notification for Order - {}", orderPlacedEvent.getOrderNumber());
}
```

---

## 🔧 Componentes de Infraestructura

### 🌐 **API Gateway** (Puerto 8080)
- **Tecnología**: Spring Cloud Gateway
- **Función**:
  - Punto de entrada único para todos los servicios
  - Enrutamiento basado en paths
  - Validación de JWT con Keycloak
  - Load balancing con Eureka

**Configuración de rutas** ([application.properties](api-gateway/src/main/resources/application.properties)):
```properties
# Ruta a Product Service
spring.cloud.gateway.routes[0].id=product-service
spring.cloud.gateway.routes[0].uri=lb://product-service
spring.cloud.gateway.routes[0].predicates[0]=Path=/api/product/**

# Ruta a Order Service
spring.cloud.gateway.routes[1].id=order-service
spring.cloud.gateway.routes[1].uri=lb://order-service
spring.cloud.gateway.routes[1].predicates[0]=Path=/api/order/**

# Ruta a Inventory Service
spring.cloud.gateway.routes[2].id=inventory-service
spring.cloud.gateway.routes[2].uri=lb://inventory-service
spring.cloud.gateway.routes[2].predicates[0]=Path=/api/inventory/**
```

**Flujo de una petición**:
```
1. Cliente → http://localhost:8080/api/order (con JWT en header)
2. API Gateway valida JWT con Keycloak
3. API Gateway busca servicio en Eureka (lb://order-service)
4. API Gateway hace load balancing y enruta a instancia disponible
5. Respuesta: Order Service → API Gateway → Cliente
```

---

### 🔍 **Discovery Server (Eureka)** (Puerto 8761)
- **Función**: Service Registry y Service Discovery
- **Dashboard**: http://localhost:8761

**Cómo funciona**:
```
1. Al arrancar, cada microservicio se registra en Eureka
2. Envía heartbeats cada 30 segundos
3. Otros servicios consultan Eureka para encontrar instancias
4. API Gateway usa Eureka para load balancing
```

**Logs de registro**:
```
DiscoveryClient_ORDER-SERVICE/order-service:xxx - registration status: 204
```

---

### 🔐 **Keycloak** (Puerto 8181)
- **Función**: Servidor de autenticación y autorización OAuth2/OpenID Connect
- **Realm**: `spring-boot-microservices-realm`

**Flujo de autenticación**:
```
1. Cliente solicita token: POST http://localhost:8181/realms/spring-boot-microservices-realm/protocol/openid-connect/token
   Body: username=user&password=pass&grant_type=password&client_id=spring-cloud-client
2. Keycloak responde con JWT access_token
3. Cliente usa token en header: Authorization: Bearer <token>
4. API Gateway valida token contra Keycloak
5. Si válido, permite acceso al microservicio
```

---

### 📊 **Zipkin** (Puerto 9411)
- **Función**: Distributed Tracing - Rastreo de peticiones entre servicios
- **Dashboard**: http://localhost:9411

**Qué traza**:
```
Cliente → API Gateway [trace-id] → Order Service [trace-id] → Inventory Service [trace-id]
```

En los logs verás:
```
[69775d5f0761118ff3390de7c700284f-f3390de7c700284f]
│                                     │
└─ Trace ID                           └─ Span ID
```

---

### 📨 **Kafka** (Puerto 9092)
- **Función**: Message Broker para comunicación asíncrona
- **Topics**: `notificationTopic`

**Flujo**:
```
Order Service (Producer) → Kafka Topic → Notification Service (Consumer)
```

**Configuración Producer** ([KafkaProducerConfig.java](order-service/src/main/java/com/programmingtechie/orderservice/config/KafkaProducerConfig.java)):
```java
new DefaultKafkaProducerFactory<>(
    props,
    new StringSerializer(),
    new JacksonJsonSerializer<OrderPlacedEvent>().noTypeInfo()
);
```

**Configuración Consumer** ([KafkaConsumerConfig.java](notification-service/src/main/java/com/programming/techie/notificationservice/config/KafkaConsumerConfig.java)):
```java
new DefaultKafkaConsumerFactory<>(
    props,
    new StringDeserializer(),
    new ErrorHandlingDeserializer<>(
        new JacksonJsonDeserializer<>(OrderPlacedEvent.class)
            .trustedPackages("*")
            .ignoreTypeHeaders()
    )
);
```

---

## 🔄 Flujo Completo: Crear un Pedido

### 1. **Obtener Token JWT**
```bash
POST http://localhost:8181/realms/spring-boot-microservices-realm/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

username=user&password=password&grant_type=password&client_id=spring-cloud-client&client_secret=xxx
```

**Respuesta**:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI...",
  "expires_in": 300,
  "token_type": "Bearer"
}
```

---

### 2. **Crear Pedido**
```bash
POST http://localhost:8080/api/order
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI...
Content-Type: application/json

{
  "orderLineItemsDtoList": [
    {
      "skuCode": "iphone_15",
      "price": 1200,
      "quantity": 1
    }
  ]
}
```

---

### 3. **Flujo Interno**

#### A. API Gateway
```
1. Recibe petición en puerto 8080
2. Extrae JWT del header Authorization
3. Valida JWT con Keycloak (issuer-uri)
4. Si válido, busca route para /api/order
5. Consulta Eureka: ¿dónde está order-service?
6. Eureka responde: http://127.0.0.1:63367
7. Gateway enruta petición (con JWT)
```

**Logs**:
```
Pattern "/api/order/**" matches against value "/api/order"
Route matched: order-service
LoadBalancerClientFilter url chosen: http://127.0.0.1:63367/api/order
```

---

#### B. Order Service
```java
// 1. Captura JWT del SecurityContext
var authentication = SecurityContextHolder.getContext().getAuthentication();
String bearerToken = jwtAuth.getToken().getTokenValue();

// 2. Llama a Inventory Service (con JWT)
webClientBuilder.build().get()
    .uri("http://inventory-service/api/inventory?skuCodeList=[iphone_15]")
    .header("Authorization", "Bearer " + bearerToken)
    .retrieve()
    .bodyToMono(InventoryResponse[].class)
    .block();

// 3. Si hay stock, guarda pedido
orderRepository.save(order);

// 4. Envía evento a Kafka
kafkaTemplate.send("notificationTopic",
    new OrderPlacedEvent(order.getOrderNumber()));
```

**Logs**:
```
>>> JWT token captured successfully
>>> About to call inventory-service with URL: http://inventory-service/api/inventory?skuCodeList=[iphone_15]
>>> Authorization header added to WebClient request
>>> Received response from inventory-service: 1 items
>>> Order saved to database: 015addfc-375b-4d3e-b4f5-18e8eb249ecf
>>> Sending Kafka event to notificationTopic for order: 015addfc-375b-4d3e-b4f5-18e8eb249ecf
>>> Kafka event sent successfully
```

---

#### C. Inventory Service
```
1. Recibe petición de Order Service (con JWT)
2. API Gateway valida JWT
3. Consulta base de datos PostgreSQL
4. Verifica si hay stock para skuCode=iphone_15
5. Responde: [{"skuCode": "iphone_15", "inStock": true}]
```

---

#### D. Kafka + Notification Service
```
1. Order Service envía mensaje a topic "notificationTopic"
2. Kafka persiste mensaje
3. Notification Service (consumer) recibe mensaje
4. Procesa notificación
```

**Logs Notification Service**:
```
[Consumer clientId=consumer-notificationId-1] Subscribed to topic(s): notificationTopic
Successfully joined group with generation Generation{generationId=1}
partitions assigned: [notificationTopic-0]
Received Notification for Order - 015addfc-375b-4d3e-b4f5-18e8eb249ecf
```

---

## 🛠️ Tecnologías Usadas

### Backend
- **Spring Boot 4.0** (Java 21)
- **Spring Cloud** (Eureka, Gateway)
- **Spring Security OAuth2** (Resource Server)
- **Spring Data JPA** (PostgreSQL)
- **Spring Data MongoDB**
- **Spring Kafka 4.0** (JacksonJsonSerializer/Deserializer)
- **Spring WebFlux** (WebClient para llamadas entre servicios)

### Infraestructura
- **Keycloak 26.5.1** (Autenticación OAuth2)
- **Kafka 7.8.0** (Message Broker)
- **Eureka** (Service Discovery)
- **Zipkin 3.4** (Distributed Tracing)
- **PostgreSQL 16** (Bases de datos)
- **MongoDB 8.0** (Base de datos NoSQL)
- **Prometheus** (Métricas)

### Patrones de Diseño
- **API Gateway Pattern** (punto de entrada único)
- **Service Registry Pattern** (Eureka)
- **Circuit Breaker** (Resilience4j)
- **Event-Driven Architecture** (Kafka)
- **Distributed Tracing** (Zipkin)

---

## 🔐 Seguridad

### Autenticación y Autorización
```
1. Keycloak genera JWT tokens
2. API Gateway valida tokens en cada petición
3. Los microservices confían en el Gateway (no validan de nuevo)
4. JWT se propaga entre servicios mediante headers
```

### Configuración OAuth2 Resource Server
```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8181/realms/spring-boot-microservices-realm
```

### SecurityConfig
```java
@Bean
SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
    http
        .csrf(csrf -> csrf.disable())
        .authorizeExchange(exchanges -> exchanges
            .pathMatchers("/eureka/**", "/actuator/**").permitAll()
            .anyExchange().authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()));
    return http.build();
}
```

---

## 📈 Observabilidad

### 1. Logs
Cada servicio genera logs estructurados con:
- Trace ID (para seguir requests entre servicios)
- Información de negocio (orden creada, stock verificado, etc.)

### 2. Métricas (Prometheus)
```
management.endpoints.web.exposure.include=*
management.endpoint.health.show-details=always
```

Endpoints:
- `/actuator/prometheus` - Métricas en formato Prometheus
- `/actuator/health` - Estado del servicio

### 3. Tracing (Zipkin)
```properties
management.zipkin.tracing.endpoint=http://localhost:9411/api/v2/spans
management.tracing.sampling.probability=1.0
```

---

## 🚀 Resiliencia

### Circuit Breaker (Resilience4j)
```properties
resilience4j.circuitbreaker.instances.inventory.slidingWindowSize=5
resilience4j.circuitbreaker.instances.inventory.failureRateThreshold=50
resilience4j.circuitbreaker.instances.inventory.waitDurationInOpenState=5s
```

**Estados**:
- **CLOSED**: Todo funciona, peticiones pasan
- **OPEN**: Muchos fallos, peticiones bloqueadas
- **HALF_OPEN**: Prueba si servicio se recuperó

### Retry
```properties
resilience4j.retry.instances.inventory.max-attempts=3
resilience4j.retry.instances.inventory.wait-duration=5s
```

### Timeout
```properties
resilience4j.timelimiter.instances.inventory.timeout-duration=3s
```

---

## 📦 Bases de Datos

### PostgreSQL (Order Service - Puerto 5431)
```sql
-- Tabla: orders
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(255) UNIQUE NOT NULL
);

-- Tabla: order_line_items
CREATE TABLE order_line_items (
    id BIGSERIAL PRIMARY KEY,
    sku_code VARCHAR(255) NOT NULL,
    price DECIMAL(19,2) NOT NULL,
    quantity INT NOT NULL,
    order_id BIGINT REFERENCES orders(id)
);
```

### PostgreSQL (Inventory Service - Puerto 5432)
```sql
-- Tabla: inventory
CREATE TABLE inventory (
    id BIGSERIAL PRIMARY KEY,
    sku_code VARCHAR(255) UNIQUE NOT NULL,
    quantity INT NOT NULL
);
```

### MongoDB (Product Service - Puerto 27017)
```javascript
// Colección: products
{
    "_id": ObjectId("..."),
    "name": "iPhone 15",
    "description": "iPhone 15 Pro Max",
    "price": 1200.00
}
```

---

## 🐳 Docker Compose

Todos los servicios de infraestructura corren en Docker:

```yaml
services:
  mongodb:
    image: mongo:8.0
    ports:
      - "27017:27017"

  postgres-order:
    image: postgres:16-alpine
    ports:
      - "5431:5432"

  postgres-inventory:
    image: postgres:16-alpine
    ports:
      - "5432:5432"

  kafka:
    image: confluentinc/cp-kafka:7.8.0
    ports:
      - "9092:9092"

  keycloak:
    image: quay.io/keycloak/keycloak:26.5.1
    ports:
      - "8181:8080"

  zipkin:
    image: openzipkin/zipkin:3.4
    ports:
      - "9411:9411"
```

---

## ✅ Verificación del Sistema

### Comprobar que todo funciona:

1. **Eureka Dashboard**: http://localhost:8761
   - Deberías ver: api-gateway, product-service, order-service, inventory-service, notification-service

2. **Zipkin Dashboard**: http://localhost:9411
   - Ver trazas de peticiones entre servicios

3. **Keycloak Admin**: http://localhost:8181
   - Admin: admin / admin

4. **Crear pedido completo**:
```bash
# 1. Obtener token
curl -X POST http://localhost:8181/realms/spring-boot-microservices-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=user&password=password&grant_type=password&client_id=spring-cloud-client"

# 2. Crear pedido
curl -X POST http://localhost:8080/api/order \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"orderLineItemsDtoList":[{"skuCode":"iphone_15","price":1200,"quantity":1}]}'
```

5. **Verificar logs**:
   - Order Service: "Order saved to database"
   - Notification Service: "Received Notification for Order"

---

## 🎓 Conceptos Clave Aprendidos

### 1. **Microservicios**
- Cada servicio es independiente
- Comunicación mediante REST (síncrona) y Kafka (asíncrona)
- Base de datos por servicio (database per service pattern)

### 2. **Service Discovery**
- Los servicios se registran en Eureka al arrancar
- No necesitas IPs/puertos hardcoded
- Load balancing automático

### 3. **API Gateway**
- Punto de entrada único
- Maneja autenticación centralmente
- Enrutamiento y load balancing

### 4. **Event-Driven Architecture**
- Desacopla servicios mediante eventos
- Order Service no necesita conocer Notification Service
- Escalable y resiliente

### 5. **Observabilidad**
- Logs estructurados
- Métricas con Prometheus
- Tracing distribuido con Zipkin

### 6. **Seguridad**
- OAuth2 + JWT
- Autenticación en API Gateway
- Propagación de tokens entre servicios

---

## 📚 Referencias

- [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)
- [Spring Kafka 4.0](https://docs.spring.io/spring-kafka/reference/kafka/serdes.html)
- [Netflix Eureka](https://spring.io/guides/gs/service-registration-and-discovery)
- [Resilience4j](https://resilience4j.readme.io/)
- [Keycloak](https://www.keycloak.org/documentation)

---

---

## 📊 COMPARACIÓN: Antes vs Después (DDD + CQRS)

### ❌ ANTES - Modelo Anémico (Anti-patrón)

```java
// ❌ Entidad sin comportamiento - solo getters/setters
@Entity
public class Order {
    private Long id;
    private String orderNumber;
    private List<OrderLineItems> orderLineItemsList;

    // Solo getters y setters - SIN LÓGICA
}

// ❌ TODO en el Service - Violación de Single Responsibility
@Service
public class OrderService {
    public String placeOrder(OrderRequest request) {
        // Validación aquí ❌
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        // Cálculo de total aquí ❌
        BigDecimal total = request.getItems().stream()
            .map(item -> item.getPrice().multiply(item.getQuantity()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Lógica de negocio aquí ❌
        if (inventoryService.checkStock(skuCodes)) {
            order.setStatus("CONFIRMED");
            repository.save(order);
        }

        // Kafka aquí
        kafkaTemplate.send("topic", event);

        return "Order placed";
    }
}

// ❌ Controller con @Transactional
@RestController
public class OrderController {

    @Transactional  // ❌ MAL UBICADO
    @PostMapping
    public String placeOrder(@RequestBody OrderRequest request) {
        return orderService.placeOrder(request);
    }
}
```

**Problemas**:
- Service gigante con toda la lógica
- Entidad anémica (solo datos, sin comportamiento)
- Difícil de testear (necesita Spring completo)
- No protege invariantes
- Violación de principios SOLID

---

### ✅ DESPUÉS - DDD + CQRS (Buenas Prácticas)

```java
// ✅ Dominio Rico - Lógica de negocio en la entidad
@Entity
public class Order {

    // Constructor privado - solo factory method
    private Order() {}

    // ✅ Factory method - protege construcción
    public static Order create(List<OrderLineItem> items) {
        Order order = new Order();
        order.orderNumber = OrderNumber.generate().getValue();
        order.addItems(items);  // Validación
        order.status = OrderStatus.PENDING;
        return order;
    }

    // ✅ Lógica de negocio en el dominio
    public Money calculateTotal() {
        return orderLineItems.stream()
            .map(OrderLineItem::calculateSubtotal)
            .reduce(Money.zero(), Money::add);
    }

    // ✅ Métodos de negocio que protegen invariantes
    public void confirm() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be confirmed");
        }
        this.status = OrderStatus.CONFIRMED;
    }

    public void cancel() {
        if (this.status == OrderStatus.SHIPPED || this.status == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel shipped/delivered orders");
        }
        this.status = OrderStatus.CANCELLED;
    }
}

// ✅ Command - Inmutable (Java record)
public record PlaceOrderCommand(
    List<OrderLineItemsDto> orderLineItems
) {
    public PlaceOrderCommand {
        if (orderLineItems == null || orderLineItems.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }
    }
}

// ✅ Handler - Orquesta, NO tiene lógica de negocio
@Service
@RequiredArgsConstructor
public class PlaceOrderCommandHandler {

    @Transactional  // ✅ BIEN - en capa de aplicación
    @CircuitBreaker(name = "inventory", fallbackMethod = "fallbackPlaceOrder")
    public String handle(PlaceOrderCommand command) {
        // 1. Crear dominio desde DTOs
        List<OrderLineItem> items = command.orderLineItems().stream()
            .map(dto -> new OrderLineItem(
                dto.getSkuCode(),
                Money.of(dto.getPrice()),
                Quantity.of(dto.getQuantity())
            ))
            .toList();

        // 2. Factory method del dominio
        Order order = Order.create(items);

        // 3. Validar (lógica de dominio)
        order.validateForProcessing();

        // 4. Verificar inventario (infraestructura)
        boolean inStock = checkInventory(order.getSkuCodes());

        if (!inStock) {
            throw new IllegalArgumentException("Product not in stock");
        }

        // 5. Confirmar (lógica de dominio)
        order.confirm();

        // 6. Persistir
        orderRepository.save(order);

        // 7. Publicar evento
        publishOrderPlacedEvent(order);

        return "Order placed successfully";
    }

    // ✅ Fallback para resiliencia
    public String fallbackPlaceOrder(PlaceOrderCommand command, Exception ex) {
        log.error("Circuit breaker activated", ex);
        return "Service unavailable, please try later";
    }
}

// ✅ Controller Ligero - Solo recibe y delega
@RestController
@RequestMapping("/api/order")
public class OrderController {

    // Sin @Transactional ✅
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public String placeOrder(@RequestBody OrderRequest request) {
        // Convierte DTO → Command
        PlaceOrderCommand command = new PlaceOrderCommand(
            request.getOrderLineItemsDtoList()
        );

        // Delega al handler
        return placeOrderCommandHandler.handle(command);
    }
}
```

**Beneficios**:
- ✅ Lógica de negocio en el dominio
- ✅ Fácil de testear sin Spring
- ✅ Protege invariantes
- ✅ Código expresivo del negocio
- ✅ Single Responsibility
- ✅ Circuit Breaker para resiliencia

---

## 🧪 Tests - TDD con DDD

### Tests del Dominio (sin Spring)

```java
// ✅ Test rápido y simple - NO necesita Spring
class OrderTest {

    @Test
    void shouldCalculateTotalCorrectly() {
        // Given
        OrderLineItem item1 = new OrderLineItem(
            "iphone_15",
            Money.of(new BigDecimal("1200.00")),
            Quantity.of(2)  // 2400
        );

        OrderLineItem item2 = new OrderLineItem(
            "macbook_pro",
            Money.of(new BigDecimal("2500.00")),
            Quantity.of(1)  // 2500
        );

        Order order = Order.create(List.of(item1, item2));

        // When
        Money total = order.calculateTotal();

        // Then
        assertEquals(new BigDecimal("4900.00"), total.getAmount());
    }

    @Test
    void shouldNotCancelShippedOrder() {
        // Given
        OrderLineItem item = new OrderLineItem(
            "iphone_15",
            Money.of(new BigDecimal("1200.00")),
            Quantity.of(1)
        );
        Order order = Order.create(List.of(item));
        order.confirm();
        order.markAsShipped();

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            order.cancel();
        });
    }
}
```

**Resultados**:
```
Tests run: 21, Failures: 0, Errors: 0 ✅
Tiempo: < 1 segundo
```

---

## 🚀 Guía de Uso Completa

### 1. Arrancar Infraestructura Docker

```bash
# Verificar contenedores
docker ps

# Deberías ver:
# - kafka (puerto 9092)
# - postgres-order (puerto 5431)
# - postgres-inventory (puerto 5432)
# - keycloak (puerto 8181)
# - zipkin (puerto 9411)
# - mongodb (puerto 27017)
```

### 2. Compilar y Testear

```bash
cd order-service

# Compilar con gRPC stubs
mvn clean compile

# Ejecutar tests del dominio
mvn test

# Resultado esperado:
# Tests run: 21, Failures: 0, Errors: 0 ✅
```

### 3. Arrancar Microservicios

```bash
# Terminal 1 - Discovery Server
cd discovery-server && mvn spring-boot:run

# Terminal 2 - API Gateway
cd api-gateway && mvn spring-boot:run

# Terminal 3 - Product Service
cd product-service && mvn spring-boot:run

# Terminal 4 - Inventory Service
cd inventory-service && mvn spring-boot:run

# Terminal 5 - Order Service (REFACTORIZADO)
cd order-service && mvn spring-boot:run

# Terminal 6 - Notification Service
cd notification-service && mvn spring-boot:run
```

### 4. Obtener Token JWT

```bash
curl -X POST \
  'http://localhost:8181/realms/spring-boot-microservices-realm/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'username=user' \
  -d 'password=password' \
  -d 'grant_type=password' \
  -d 'client_id=spring-cloud-client' \
  -d 'client_secret=<tu-secret>'
```

Guarda el `access_token` de la respuesta.

### 5. Crear una Orden (Flow Completo)

```bash
curl -X POST http://localhost:8080/api/order \
  -H "Authorization: Bearer <TU_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "orderLineItemsDtoList": [
      {
        "skuCode": "iphone_15",
        "price": 1200.00,
        "quantity": 1
      }
    ]
  }'
```

### 6. Verificar en Logs

**Order Service** debería mostrar:
```
INFO ... OrderController : Received order request with 1 items
INFO ... PlaceOrderCommandHandler : Handling PlaceOrderCommand with 1 items
INFO ... PlaceOrderCommandHandler : Checking inventory for SKUs: [iphone_15]
INFO ... PlaceOrderCommandHandler : JWT token extracted successfully
INFO ... PlaceOrderCommandHandler : Received 1 inventory responses
INFO ... PlaceOrderCommandHandler : Order saved: <uuid>
INFO ... PlaceOrderCommandHandler : Publishing OrderPlacedEvent
INFO ... PlaceOrderCommandHandler : OrderPlacedEvent published successfully
```

**Notification Service** debería mostrar:
```
INFO ... NotificationServiceApplication : Received Notification for Order - <uuid>
```

### 7. Ver Trazas en Zipkin

1. Ve a: http://localhost:9411
2. Service Name: `order-service`
3. Click **RUN QUERY**
4. Verás la traza completa:

```
api-gateway (50ms)
  └─ order-service (120ms)
       └─ inventory-service (30ms)
```

### 8. Ver Dashboard de Eureka

http://localhost:8761

Deberías ver registrados:
- api-gateway
- product-service
- order-service
- inventory-service
- notification-service

---

## 🎯 Para Entrevistas - Frase Resumen

> **"Aplico DDD y CQRS de forma pragmática: diseño un dominio rico donde la lógica de negocio vive en las entidades, no en los services. Implemento Commands inmutables con handlers que solo orquestan, dejando controllers finos que solo reciben y delegan. Para resiliencia, uso Circuit Breaker explícito con fallback. Todo el dominio está testeado sin Spring, facilitando TDD con tests rápidos y simples. Es un diseño pragmático, no académico, enfocado en código expresivo y mantenible."**

---

## 📁 Estructura de Archivos Clave

```
order-service/
├── application/command/
│   ├── PlaceOrderCommand.java           ✅ Command (inmutable)
│   └── PlaceOrderCommandHandler.java    ✅ Handler (orquesta + @Transactional)
├── controller/
│   └── OrderController.java             ✅ Controller ligero
├── domain/
│   ├── entity/
│   │   ├── Order.java                   ✅ Dominio rico
│   │   ├── OrderLineItem.java
│   │   └── OrderStatus.java
│   └── valueobject/
│       ├── Money.java                   ✅ Value Object inmutable
│       ├── Quantity.java
│       └── OrderNumber.java
├── proto/
│   └── inventory.proto                  🟡 gRPC preparado
└── test/
    ├── OrderTest.java                   ✅ 11 tests del dominio
    └── MoneyTest.java                   ✅ 10 tests de Value Object
```

---

## ⚠️ Lo que NO está implementado

- ❌ **gRPC Server/Client completo** - Solo protobuf + stubs generados
- ❌ **Queries (CQRS completo)** - Solo Commands implementados
- ❌ **Event Sourcing** - Kafka solo para notificaciones
- ❌ **Saga Pattern** - No hay transacciones distribuidas
- ❌ **BFF (Backend for Frontend)** - API Gateway directo

---

## 🏁 Conclusión Final

Este proyecto implementa un sistema de microservicios completo con:

### ✅ Arquitectura
- Comunicación síncrona (REST/WebClient)
- Comunicación asíncrona (Kafka)
- Service Discovery (Eureka)
- API Gateway
- Autenticación OAuth2 (Keycloak)
- Distributed Tracing (Zipkin)

### ✅ Buenas Prácticas (DDD + CQRS)
- **Dominio rico** - Lógica en entidades, NO en services
- **Value Objects** - Inmutables con validaciones
- **Commands** - Inmutables que representan intenciones
- **Handlers** - Orquestan sin lógica de negocio
- **Controllers** - Finos, solo reciben y delegan
- **@Transactional** - En capa de aplicación
- **Circuit Breaker** - Resiliencia explícita

### ✅ Testing
- **21 tests** del dominio sin Spring
- **Rápidos** (< 1 segundo)
- **TDD-friendly** - Lógica aislada

### ✅ Observabilidad
- Zipkin para tracing distribuido
- Prometheus para métricas
- Logs estructurados

**¡Arquitectura lista para producción con buenas prácticas profesionales!** 🚀

---

**Documentos relacionados**:
- [MEJORAS-DDD-CQRS.md](MEJORAS-DDD-CQRS.md) - Explicación detallada de las refactorizaciones
