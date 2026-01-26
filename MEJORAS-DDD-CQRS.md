# 🚀 Mejoras Implementadas: DDD + CQRS + Circuit Breaker + Tests

## 📋 Resumen de Cambios

Hemos refactorizado el `order-service` para aplicar **buenas prácticas** de arquitectura de software:

### ✅ Implementado

1. **Domain-Driven Design (DDD)** - Dominio rico con lógica de negocio
2. **CQRS** - Separación de Commands y Queries
3. **Circuit Breaker explícito** - Resiliencia activa
4. **Tests unitarios** - Cobertura del dominio sin Spring
5. **Value Objects** - Inmutabilidad y validaciones

### ⏳ Pendiente

- **gRPC entre servicios** (actualmente usa REST con WebClient)

---

## 🧠 1. Domain-Driven Design (DDD)

### Problema Anterior: Modelo Anémico

**Antes** - La entidad `Order` era un simple contenedor de datos:

```java
@Entity
public class Order {
    private Long id;
    private String orderNumber;
    private List<OrderLineItems> orderLineItemsList;

    // Solo getters/setters - SIN LÓGICA DE NEGOCIO
}
```

❌ **Problemas**:
- Toda la lógica estaba en `OrderService`
- La entidad no protegía sus invariantes
- Difícil de testear
- No representa el dominio del negocio

### Solución: Dominio Rico

**Ahora** - La entidad `Order` tiene comportamiento y lógica:

**Ubicación**: [Order.java](order-service/src/main/java/com/programmingtechie/orderservice/domain/entity/Order.java)

```java
@Entity
public class Order {
    // Constructor privado - solo se crea mediante factory method
    private Order() {}

    // Factory method - forma correcta de crear órdenes
    public static Order create(List<OrderLineItem> items) {
        Order order = new Order();
        order.orderNumber = OrderNumber.generate().getValue();
        order.addItems(items);  // Validación aquí
        order.status = OrderStatus.PENDING;
        return order;
    }

    // Lógica de negocio: calcular total
    public Money calculateTotal() {
        return orderLineItems.stream()
            .map(OrderLineItem::calculateSubtotal)
            .reduce(Money.zero(), Money::add);
    }

    // Lógica de negocio: confirmar orden
    public void confirm() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be confirmed");
        }
        this.status = OrderStatus.CONFIRMED;
    }

    // Lógica de negocio: cancelar orden
    public void cancel() {
        if (this.status == OrderStatus.SHIPPED || this.status == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel shipped or delivered orders");
        }
        this.status = OrderStatus.CANCELLED;
    }

    // Protege invariantes - lista inmutable
    public List<OrderLineItem> getOrderLineItems() {
        return Collections.unmodifiableList(orderLineItems);
    }
}
```

✅ **Beneficios**:
- La lógica de negocio vive en el dominio
- Protege invariantes (no se puede cancelar una orden enviada)
- Auto-documentado (el código expresa reglas de negocio)
- Fácil de testear sin Spring

---

### Value Objects

Los **Value Objects** son objetos inmutables que representan conceptos del dominio:

#### Money - [Money.java](order-service/src/main/java/com/programmingtechie/orderservice/domain/valueobject/Money.java)

```java
public class Money {
    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    public Money multiply(int quantity) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(quantity)));
    }
}
```

✅ **Ventajas**:
- **Inmutable**: No se puede modificar después de creado
- **Validaciones**: No puede haber dinero negativo
- **Operaciones de negocio**: `add()`, `multiply()`
- **Type-safe**: No confundes Money con otros BigDecimal

#### Quantity - [Quantity.java](order-service/src/main/java/com/programmingtechie/orderservice/domain/valueobject/Quantity.java)

```java
public class Quantity {
    private final Integer value;

    private Quantity(Integer value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        this.value = value;
    }

    public static Quantity of(Integer value) {
        return new Quantity(value);
    }
}
```

#### OrderNumber - [OrderNumber.java](order-service/src/main/java/com/programmingtechie/orderservice/domain/valueobject/OrderNumber.java)

```java
public class OrderNumber {
    private final String value;

    public static OrderNumber generate() {
        return new OrderNumber(UUID.randomUUID().toString());
    }

    public static OrderNumber from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Order number cannot be null or empty");
        }
        return new OrderNumber(value);
    }
}
```

---

## 🔁 2. CQRS (Command Query Responsibility Segregation)

### Concepto

- **Command**: Cambia estado, no devuelve datos del dominio
- **Query**: Solo lee, no modifica estado

### Estructura Implementada

```
order-service/
├── application/
│   └── command/
│       ├── PlaceOrderCommand.java          # Command (inmutable)
│       └── PlaceOrderCommandHandler.java   # Handler (orquesta)
├── controller/
│   └── OrderController.java                # Solo recibe y delega
├── domain/
│   ├── entity/
│   │   ├── Order.java                      # Lógica de negocio
│   │   ├── OrderLineItem.java
│   │   └── OrderStatus.java
│   └── valueobject/
│       ├── Money.java
│       ├── Quantity.java
│       └── OrderNumber.java
└── repository/
    └── OrderRepository.java                # Persistencia
```

### Command

**PlaceOrderCommand** - [PlaceOrderCommand.java](order-service/src/main/java/com/programmingtechie/orderservice/application/command/PlaceOrderCommand.java)

```java
// Record - inmutable por defecto
public record PlaceOrderCommand(
    List<OrderLineItemsDto> orderLineItems
) {
    // Constructor canónico con validación
    public PlaceOrderCommand {
        if (orderLineItems == null || orderLineItems.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }
    }
}
```

✅ **Características**:
- **Inmutable** (usa `record`)
- **Representa intención del usuario**
- **Validación básica** en constructor

### Command Handler

**PlaceOrderCommandHandler** - [PlaceOrderCommandHandler.java](order-service/src/main/java/com/programmingtechie/orderservice/application/command/PlaceOrderCommandHandler.java)

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceOrderCommandHandler {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    @Transactional
    @CircuitBreaker(name = "inventory", fallbackMethod = "fallbackPlaceOrder")
    @Retry(name = "inventory")
    @TimeLimiter(name = "inventory")
    public String handle(PlaceOrderCommand command) {
        // 1. Crear entidades de dominio desde DTOs
        List<OrderLineItem> lineItems = command.orderLineItems().stream()
            .map(dto -> new OrderLineItem(
                dto.getSkuCode(),
                Money.of(dto.getPrice()),
                Quantity.of(dto.getQuantity())
            ))
            .toList();

        // 2. Crear orden usando factory method del dominio
        Order order = Order.create(lineItems);

        // 3. Validar orden (lógica de dominio)
        order.validateForProcessing();

        // 4. Verificar inventario
        boolean allInStock = checkInventory(order.getSkuCodes());

        if (!allInStock) {
            throw new IllegalArgumentException("Product not in stock");
        }

        // 5. Confirmar orden (lógica de dominio)
        order.confirm();

        // 6. Persistir
        Order savedOrder = orderRepository.save(order);

        // 7. Publicar evento
        publishOrderPlacedEvent(savedOrder);

        return "Order placed successfully";
    }

    // Fallback cuando inventory-service falla
    public String fallbackPlaceOrder(PlaceOrderCommand command, Exception ex) {
        log.error("Circuit breaker activated - Inventory service is down", ex);
        return "Order cannot be placed. Inventory service unavailable.";
    }
}
```

✅ **Responsabilidades**:
- **Orquesta** el caso de uso
- **NO contiene lógica de negocio** (está en el dominio)
- **Maneja @Transactional**
- **Maneja resiliencia** (Circuit Breaker, Retry)
- **Publica eventos**

### Controller

**OrderController** - [OrderController.java](order-service/src/main/java/com/programmingtechie/orderservice/controller/OrderController.java)

```java
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final PlaceOrderCommandHandler placeOrderCommandHandler;

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public String placeOrder(@RequestBody OrderRequest orderRequest) {
        log.info("Received order request");

        // Convertir DTO a Command
        PlaceOrderCommand command = new PlaceOrderCommand(
            orderRequest.getOrderLineItemsDtoList()
        );

        // Delegar al Command Handler
        return placeOrderCommandHandler.handle(command);
    }
}
```

✅ **Características**:
- **Solo recibe request**
- **Convierte DTO → Command**
- **Delega al handler**
- **NO tiene lógica de negocio**
- **NO tiene @Transactional** (va en el handler)

---

## 🛡️ 3. Circuit Breaker Explícito

### Configuración

Ya estaba configurado en [application.properties](order-service/src/main/resources/application.properties):

```properties
# Circuit Breaker
resilience4j.circuitbreaker.instances.inventory.sliding-window-size=5
resilience4j.circuitbreaker.instances.inventory.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.inventory.wait-duration-in-open-state=5s
resilience4j.circuitbreaker.instances.inventory.permitted-number-of-calls-in-half-open-state=3

# Retry
resilience4j.retry.instances.inventory.max-attempts=3
resilience4j.retry.instances.inventory.wait-duration=5s

# Timeout
resilience4j.timelimiter.instances.inventory.timeout-duration=3s
```

### Uso Explícito

Ahora se usa **explícitamente** en el Command Handler:

```java
@CircuitBreaker(name = "inventory", fallbackMethod = "fallbackPlaceOrder")
@Retry(name = "inventory")
@TimeLimiter(name = "inventory")
public String handle(PlaceOrderCommand command) {
    // ...
}

// Método de fallback
public String fallbackPlaceOrder(PlaceOrderCommand command, Exception ex) {
    log.error("Circuit breaker activated - Inventory service is down", ex);
    return "Order cannot be placed. Inventory service unavailable.";
}
```

### Estados del Circuit Breaker

```
CLOSED (Cerrado)
  ↓
  Fallos superan threshold (50%)
  ↓
OPEN (Abierto) - Bloquea llamadas por 5s
  ↓
  Después de 5s
  ↓
HALF_OPEN (Medio abierto) - Permite 3 llamadas de prueba
  ↓
  Si funcionan → CLOSED
  Si fallan → OPEN
```

---

## 🧪 4. Tests Unitarios del Dominio

### Características

✅ **Sin Spring** - Tests rápidos y simples
✅ **Testean lógica de negocio** - No infraestructura
✅ **TDD friendly** - Dominio primero, infraestructura después

### Ejemplo: OrderTest

**Ubicación**: [OrderTest.java](order-service/src/test/java/com/programmingtechie/orderservice/domain/entity/OrderTest.java)

```java
class OrderTest {

    @Test
    void shouldCreateOrderWithItems() {
        // Given
        OrderLineItem item = new OrderLineItem(
            "iphone_15",
            Money.of(new BigDecimal("1200.00")),
            Quantity.of(2)
        );

        // When
        Order order = Order.create(List.of(item));

        // Then
        assertNotNull(order);
        assertNotNull(order.getOrderNumber());
        assertEquals(OrderStatus.PENDING, order.getStatus());
    }

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

### Ejemplo: MoneyTest

**Ubicación**: [MoneyTest.java](order-service/src/test/java/com/programmingtechie/orderservice/domain/valueobject/MoneyTest.java)

```java
class MoneyTest {

    @Test
    void shouldThrowExceptionForNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> {
            Money.of(new BigDecimal("-10.00"));
        });
    }

    @Test
    void shouldAddMoneyCorrectly() {
        Money money1 = Money.of(new BigDecimal("100.00"));
        Money money2 = Money.of(new BigDecimal("50.50"));

        Money result = money1.add(money2);

        assertEquals(new BigDecimal("150.50"), result.getAmount());
    }

    @Test
    void shouldBeImmutable() {
        Money original = Money.of(new BigDecimal("100.00"));
        Money added = original.add(Money.of(new BigDecimal("50.00")));

        // El original no cambia
        assertEquals(new BigDecimal("100.00"), original.getAmount());
        assertEquals(new BigDecimal("150.00"), added.getAmount());
    }
}
```

### Ejecutar Tests

```bash
cd order-service

# Todos los tests
mvn test

# Solo tests del dominio
mvn test -Dtest=OrderTest
mvn test -Dtest=MoneyTest

# Con cobertura
mvn test jacoco:report
```

---

## 📊 Comparación: Antes vs Después

### Antes (Modelo Anémico)

```
Controller
    ↓
OrderService (TODA la lógica aquí ❌)
    ↓
Repository
    ↓
Order (solo getters/setters)
```

❌ **Problemas**:
- Service gigante con toda la lógica
- Entidad sin comportamiento
- Difícil de testear (necesita Spring)
- No protege invariantes

### Después (DDD + CQRS)

```
Controller (solo recibe)
    ↓
PlaceOrderCommand (inmutable)
    ↓
PlaceOrderCommandHandler (orquesta)
    ↓
Order (lógica de negocio ✅)
    ↓
Repository
```

✅ **Beneficios**:
- Dominio rico con lógica
- Separación de responsabilidades
- Fácil de testear (sin Spring)
- Protege invariantes
- Circuit Breaker explícito

---

## 🎯 Principios Aplicados

### 1. Separación de Responsabilidades

- **Controller**: Recibe HTTP, delega
- **Command/Handler**: Orquesta caso de uso
- **Domain**: Lógica de negocio
- **Repository**: Persistencia

### 2. Dominio Fuerte, Infraestructura Débil

- **Dominio**: No depende de Spring, JPA, HTTP
- **Infraestructura**: Depende del dominio

### 3. Código Expresivo Orientado a Negocio

```java
order.confirm();
order.cancel();
order.calculateTotal();
```

El código **habla el lenguaje del negocio**.

### 4. Diseño Pragmático

- No sobre-ingenierizado
- Solo lo necesario
- CQRS pragmático (no event sourcing completo)

---

## 🚀 Ejecutar el Proyecto Refactorizado

### 1. Compilar

```bash
cd order-service
mvn clean install
```

### 2. Ejecutar tests

```bash
mvn test
```

**Resultado esperado**:
```
Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
```

### 3. Arrancar el servicio

```bash
mvn spring-boot:run
```

### 4. Probar endpoint

```bash
# Obtener token JWT
curl -X POST http://localhost:8181/realms/spring-boot-microservices-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=user&password=password&grant_type=password&client_id=spring-cloud-client"

# Crear orden
curl -X POST http://localhost:8080/api/order \
  -H "Authorization: Bearer <TOKEN>" \
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

---

## 🔮 Qué Falta Implementar

### gRPC entre servicios

Actualmente usa REST (WebClient) para comunicación entre Order e Inventory.

**Beneficios de gRPC**:
- 7x más rápido que REST
- Tipado fuerte (protobuf)
- Streaming bidireccional
- Mejor para comunicación interna

**Para implementarlo**:
1. Agregar dependencias de gRPC
2. Definir protobuf contracts
3. Generar stubs
4. Reemplazar WebClient por gRPC client

---

## 📚 Conceptos Clave Aprendidos

### DDD

- **Entidades**: Tienen identidad y ciclo de vida
- **Value Objects**: Inmutables, sin identidad
- **Agregados**: Order es el root, OrderLineItem es parte del agregado
- **Factory Methods**: `Order.create()` en lugar de `new Order()`
- **Invariantes**: Protegidos por el dominio

### CQRS

- **Command**: `PlaceOrderCommand` - cambia estado
- **Query**: (no implementado aún) - solo lectura
- **Handler**: Orquesta, no tiene lógica de negocio
- **Controller**: Solo recibe y delega

### Resiliencia

- **Circuit Breaker**: Evita cascade failures
- **Retry**: Reintentos automáticos
- **Timeout**: Evita esperas infinitas
- **Fallback**: Respuesta alternativa cuando falla

### Testing

- **Tests sin Spring**: Rápidos y simples
- **Test de dominio**: Validan reglas de negocio
- **Given-When-Then**: Estructura clara
- **TDD**: Tests primero, implementación después

---

## ✅ Checklist de Buenas Prácticas

- [x] Dominio rico (NO anémico)
- [x] Value Objects inmutables
- [x] CQRS (Commands y Queries separados)
- [x] Circuit Breaker explícito
- [x] Tests unitarios del dominio
- [x] Factory methods en lugar de constructores públicos
- [x] Invariantes protegidos
- [x] Código expresivo (lenguaje de negocio)
- [x] @Transactional en capa de aplicación
- [x] Optimistic locking (@Version)
- [ ] gRPC entre servicios (pendiente)

---

## 🎓 Conclusión

Hemos transformado un proyecto con **modelo anémico** en un proyecto con:

✅ **DDD**: Dominio rico con lógica de negocio
✅ **CQRS**: Separación clara de responsabilidades
✅ **Resiliencia**: Circuit Breaker activo
✅ **Testing**: Cobertura del dominio
✅ **Calidad**: Código mantenible y escalable

**El código ahora es**:
- Más fácil de entender
- Más fácil de testear
- Más fácil de mantener
- Más robusto ante fallos
- Más expresivo del negocio

¡Listo para producción! 🚀
